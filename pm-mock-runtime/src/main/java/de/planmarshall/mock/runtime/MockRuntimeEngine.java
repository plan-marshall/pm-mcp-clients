/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.http.MockEventStream;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.LinkDefinition;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

/**
 * The core state machine and request dispatcher for the mock runtime.
 */
public final class MockRuntimeEngine implements AutoCloseable {

    private final ScenarioRegistry registry;
    private final String runtimeToken;
    private final String deviceSecret;
    private final ScheduledExecutorService scheduler;
    private final Set<MockEventStream> activeStreams = ConcurrentHashMap.newKeySet();
    private final List<ScheduledFuture<?>> pendingTasks = new ArrayList<>();

    private volatile Scenario currentScenario;
    private volatile String currentStateId;
    private volatile Instant stateEnteredInstant;
    private volatile boolean closed;

    /**
     * @param registry     the scenario registry
     * @param scenario     the initial scenario
     * @param runtimeToken the accepted runtime token on the Unix domain socket
     * @param deviceSecret the accepted device secret on the web port
     */
    public MockRuntimeEngine(ScenarioRegistry registry, Scenario scenario, String runtimeToken, String deviceSecret) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.runtimeToken = Objects.requireNonNull(runtimeToken, "runtimeToken");
        this.deviceSecret = Objects.requireNonNull(deviceSecret, "deviceSecret");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("mock-engine-scheduler").factory());
        setScenario(scenario);
    }

    /**
     * Switches the active scenario by name.
     *
     * @param nameOrPath scenario name or path
     * @return the newly activated scenario
     */
    public synchronized Scenario switchScenario(String nameOrPath) {
        var scenario = registry.resolve(nameOrPath);
        setScenario(scenario);
        return scenario;
    }

    private synchronized void setScenario(Scenario scenario) {
        this.currentScenario = scenario;
        transitionTo(scenario.start(), false);
    }

    /**
     * Resets the active scenario back to its start state.
     */
    public synchronized void reset() {
        transitionTo(currentScenario.start(), false);
    }

    /**
     * Jumps directly to a state within the current scenario.
     *
     * @param stateId the state identifier
     * @throws IllegalArgumentException if the state does not exist in the current scenario
     */
    public synchronized void goToState(String stateId) {
        if (!currentScenario.states().containsKey(stateId)) {
            throw new IllegalArgumentException("State '" + stateId + "' does not exist in scenario '" + currentScenario.name() + "'");
        }
        transitionTo(stateId, false);
    }

    private synchronized void transitionTo(String nextStateId, boolean fromAuto) {
        if (closed) {
            return;
        }
        cancelPendingTasks();
        this.currentStateId = nextStateId;
        this.stateEnteredInstant = Instant.now();

        var stateDef = currentScenario.state(nextStateId);
        if (stateDef == null) {
            return;
        }

        // Schedule events
        for (var eventDef : stateDef.events()) {
            var task = scheduler.schedule(() -> broadcastEvent(eventDef.event(), eventDef.data()),
                    eventDef.afterMs(), TimeUnit.MILLISECONDS);
            pendingTasks.add(task);
        }

        // Schedule auto transition
        if (stateDef.auto() != null) {
            var auto = stateDef.auto();
            var task = scheduler.schedule(() -> {
                synchronized (this) {
                    if (Objects.equals(this.currentStateId, nextStateId)) {
                        transitionTo(auto.next(), true);
                    }
                }
            }, auto.afterMs(), TimeUnit.MILLISECONDS);
            pendingTasks.add(task);
        }
    }

    private synchronized void cancelPendingTasks() {
        for (var task : pendingTasks) {
            task.cancel(false);
        }
        pendingTasks.clear();
    }

    private void broadcastEvent(String event, Object data) {
        var iterator = activeStreams.iterator();
        while (iterator.hasNext()) {
            var stream = iterator.next();
            try {
                stream.sendJson(event, data);
            } catch (IOException e) {
                iterator.remove();
            }
        }
    }

    /**
     * Dispatches an incoming request.
     *
     * @param request  the request
     * @param response the response
     * @param isWeb    {@code true} if arriving on the web listener, {@code false} if on the Unix socket
     * @throws IOException on I/O error
     */
    public void dispatch(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();

        // 1. Control endpoints under /mock/
        if (path.startsWith("/mock/")) {
            handleControlRequest(request, response);
            return;
        }

        // 2. Authentication check
        if (isWeb) {
            if (path.equals("/api/v1/pair") || path.equals("/pair")) {
                handlePairing(request, response);
                return;
            }
            var auth = request.header("Authorization");
            if (auth == null || !auth.equals("Bearer " + deviceSecret)) {
                response.json(401, Map.of("error", "unauthorized", "message", "Invalid or missing device secret"));
                return;
            }
        } else {
            // Local Unix socket
            var auth = request.header("Authorization");
            if (auth == null || !auth.equals("Bearer " + runtimeToken)) {
                // Check if it presents a job token on /mcp
                var jobToken = request.header("PM-MCP-Job-Token");
                if (jobToken == null || !path.startsWith("/mcp")) {
                    response.json(401, Map.of("error", "unauthorized", "message", "Invalid or missing runtime token"));
                    return;
                }
            }
        }

        // 3. SSE event stream subscription
        if (request.method().equals("GET") && (path.equals("/api/v1/events") || path.endsWith("/events"))) {
            handleEventStream(request, response);
            return;
        }

        // 4. Match against current state links
        StateDefinition state;
        String activeStateId;
        synchronized (this) {
            activeStateId = this.currentStateId;
            state = currentScenario.state(activeStateId);
        }

        if (state != null) {
            var matched = findMatchingLink(request, state);
            if (matched != null) {
                var linkName = matched.getKey();
                var linkDef = matched.getValue();

                if (linkDef.delayMs() > 0) {
                    try {
                        Thread.sleep(linkDef.delayMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (linkDef.status() >= 400) {
                    var errorPayload = Map.of(
                            "error", linkDef.error() != null ? linkDef.error() : "rejected",
                            "state", activeStateId,
                            "link", linkName
                    );
                    response.json(linkDef.status(), errorPayload);
                    return;
                }

                // Advance state machine
                synchronized (this) {
                    transitionTo(linkDef.next(), false);
                    state = currentScenario.state(this.currentStateId);
                }

                var nextRep = state != null ? state.representation() : Map.of();
                response.json(linkDef.status(), nextRep);
                return;
            }
        }

        // 5. Match against static routes
        var route = currentScenario.route(request.method(), path);
        if (route != null) {
            response.json(route.status(), route.representation());
            return;
        }

        // 6. No match: answer mock_no_route
        var offeredLinks = state != null ? new ArrayList<>(state.links().keySet()) : List.<String>of();
        var errorBody = new LinkedHashMap<String, Object>();
        errorBody.put("error", "mock_no_route");
        errorBody.put("message", "No route matched for " + request.method() + " " + path + " in state '" + activeStateId + "'");
        errorBody.put("state", activeStateId);
        errorBody.put("offered_links", offeredLinks);

        response.json(404, errorBody);
    }

    private Map.Entry<String, LinkDefinition> findMatchingLink(MockHttpRequest request, StateDefinition state) {
        var method = request.method();
        var path = request.path();

        for (var entry : state.links().entrySet()) {
            var linkName = entry.getKey();
            var linkDef = entry.getValue();

            // Method check
            if (!method.equalsIgnoreCase(linkDef.method())) {
                continue;
            }

            // Check 1: Explicit href matches path
            if (linkDef.href() != null && linkDef.href().equals(path)) {
                return entry;
            }

            // Check 2: Path ends with /<linkName> (e.g. /api/v1/operator/check or /api/v1/probe)
            if (path.endsWith("/" + linkName)) {
                return entry;
            }

            // Check 3: Check inside representation links/forms if available
            if (state.representation() instanceof Map<?, ?> repMap) {
                var linksObj = repMap.get("links");
                if (linksObj instanceof Map<?, ?> repLinks && repLinks.containsKey(linkName)) {
                    var targetObj = repLinks.get(linkName);
                    if (targetObj instanceof Map<?, ?> targetMap && path.equals(targetMap.get("href"))) {
                        return entry;
                    }
                    if (path.equals(String.valueOf(targetObj))) {
                        return entry;
                    }
                }
                var halLinksObj = repMap.get("_links");
                if (halLinksObj instanceof Map<?, ?> halLinks && halLinks.containsKey(linkName)) {
                    var targetObj = halLinks.get(linkName);
                    if (targetObj instanceof Map<?, ?> targetMap && path.equals(targetMap.get("href"))) {
                        return entry;
                    }
                }
            }
        }
        return null;
    }

    private void handlePairing(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String code = null;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    code = JsonTree.string(parsed, "code");
                }
            } catch (Exception ignored) {
            }
        }
        if (code == null && request.query() != null) {
            for (var part : request.query().split("&")) {
                if (part.startsWith("code=")) {
                    code = part.substring(5);
                    break;
                }
            }
        }

        var expectedCode = currentScenario.pairingCode();
        if (code != null && code.replace("-", "").equalsIgnoreCase(expectedCode.replace("-", ""))) {
            var body = Map.of(
                    "device_secret", deviceSecret,
                    "device_id", "mock-device-1",
                    "label", "Mock Browser"
            );
            response.json(200, body);
        } else {
            response.json(400, Map.of("error", "invalid_code", "message", "The pairing code does not match"));
        }
    }

    private void handleEventStream(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var stream = response.events(200);
        activeStreams.add(stream);
        try {
            stream.sendComment("connected to mock runtime");
        } catch (IOException e) {
            activeStreams.remove(stream);
            return;
        }

        // Keep the stream alive until client disconnects or closed
        while (!closed && !stream.isClosed()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        activeStreams.remove(stream);
    }

    private void handleControlRequest(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if (path.equals("/mock/state") && request.method().equals("GET")) {
            StateDefinition state;
            String stateId;
            String scenarioName;
            synchronized (this) {
                stateId = this.currentStateId;
                scenarioName = this.currentScenario.name();
                state = currentScenario.state(stateId);
            }
            var offered = state != null ? new ArrayList<>(state.links().keySet()) : List.<String>of();
            response.json(200, Map.of(
                    "scenario", scenarioName,
                    "state", stateId,
                    "offered_links", offered
            ));
            return;
        }

        if (path.equals("/mock/reset") && request.method().equals("POST")) {
            reset();
            response.json(200, Map.of(
                    "status", "reset",
                    "scenario", currentScenario.name(),
                    "state", currentStateId
            ));
            return;
        }

        if (path.equals("/mock/scenario") && request.method().equals("POST")) {
            String scenarioName = null;
            if (request.body().length > 0) {
                try {
                    var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                    if (parsed != null) {
                        scenarioName = JsonTree.string(parsed, "scenario");
                    }
                } catch (Exception ignored) {
                }
            }
            if (scenarioName == null && request.query() != null) {
                for (var part : request.query().split("&")) {
                    if (part.startsWith("name=") || part.startsWith("scenario=")) {
                        scenarioName = part.substring(part.indexOf('=') + 1);
                        break;
                    }
                }
            }
            if (scenarioName == null || scenarioName.isBlank()) {
                response.json(400, Map.of("error", "missing_scenario_name"));
                return;
            }
            try {
                switchScenario(scenarioName);
                response.json(200, Map.of(
                        "status", "switched",
                        "scenario", currentScenario.name(),
                        "state", currentStateId
                ));
            } catch (Exception e) {
                response.json(404, Map.of("error", "scenario_not_found", "message", e.getMessage()));
            }
            return;
        }

        if (path.startsWith("/mock/goto/") && request.method().equals("POST")) {
            var targetState = path.substring("/mock/goto/".length());
            try {
                goToState(targetState);
                response.json(200, Map.of(
                        "status", "transitioned",
                        "scenario", currentScenario.name(),
                        "state", currentStateId
                ));
            } catch (IllegalArgumentException e) {
                response.json(404, Map.of("error", "state_not_found", "message", e.getMessage()));
            }
            return;
        }

        response.json(404, Map.of("error", "unknown_control_endpoint"));
    }

    /** @return the current scenario */
    public Scenario currentScenario() {
        return currentScenario;
    }

    /** @return the current state id */
    public String currentStateId() {
        return currentStateId;
    }

    /** @return the accepted runtime token */
    public String runtimeToken() {
        return runtimeToken;
    }

    /** @return the accepted device secret */
    public String deviceSecret() {
        return deviceSecret;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            cancelPendingTasks();
            scheduler.shutdownNow();
            for (var stream : activeStreams) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
            activeStreams.clear();
        }
    }
}
