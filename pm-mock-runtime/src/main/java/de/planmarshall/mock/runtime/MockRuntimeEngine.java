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
import java.util.ArrayList;
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
import java.util.concurrent.atomic.AtomicReference;

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

    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_STATE = "state";
    private static final String KEY_SCENARIO = "scenario";
    private static final String KEY_STATUS = "status";

    private final ScenarioRegistry registry;
    private final String runtimeToken;
    private final String deviceSecret;
    private final ScheduledExecutorService scheduler;
    private final Set<MockEventStream> activeStreams = ConcurrentHashMap.newKeySet();
    private final List<ScheduledFuture<?>> pendingTasks = new ArrayList<>();

    private final AtomicReference<Scenario> currentScenario = new AtomicReference<>();
    private final AtomicReference<String> currentStateId = new AtomicReference<>();
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
        this.currentScenario.set(scenario);
        transitionTo(scenario.start());
    }

    /**
     * Resets the active scenario back to its start state.
     */
    public synchronized void reset() {
        transitionTo(currentScenario.get().start());
    }

    /**
     * Jumps directly to a state within the current scenario.
     *
     * @param stateId the state identifier
     * @throws IllegalArgumentException if the state does not exist in the current scenario
     */
    public synchronized void goToState(String stateId) {
        var scenario = currentScenario.get();
        if (!scenario.states().containsKey(stateId)) {
            throw new IllegalArgumentException("State '" + stateId + "' does not exist in scenario '" + scenario.name() + "'");
        }
        transitionTo(stateId);
    }

    private synchronized void transitionTo(String nextStateId) {
        if (closed) {
            return;
        }
        cancelPendingTasks();
        this.currentStateId.set(nextStateId);

        var stateDef = currentScenario.get().state(nextStateId);
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
                    if (Objects.equals(this.currentStateId.get(), nextStateId)) {
                        transitionTo(auto.next());
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

    /**
     * Broadcasts an event to all open SSE connections.
     *
     * @param event the event type
     * @param data  the event payload
     */
    public void broadcast(String event, Object data) {
        broadcastEvent(event, data);
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
        if (!isAuthorized(request, response, isWeb)) {
            return;
        }

        // 3. SSE event stream subscription
        if ("GET".equals(request.method()) && ("/api/v1/events".equals(path) || path.endsWith("/events"))) {
            handleEventStream(response);
            return;
        }

        // 4. Match against current state links
        if (handleStateLink(request, response)) {
            return;
        }

        // 5. Match against static routes
        var route = currentScenario.get().route(request.method(), path);
        if (route != null) {
            response.json(route.status(), route.representation());
            return;
        }

        // 6. No match: answer mock_no_route
        handleNoRoute(request, response);
    }

    private boolean isAuthorized(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        if (isWeb) {
            if ("/api/v1/pair".equals(path) || "/pair".equals(path)) {
                handlePairing(request, response);
                return false;
            }
            var auth = request.header("Authorization");
            if (auth == null || !auth.equals("Bearer " + deviceSecret)) {
                response.json(401, Map.of(KEY_ERROR, "unauthorized", KEY_MESSAGE, "Invalid or missing device secret"));
                return false;
            }
        } else {
            var auth = request.header("Authorization");
            if (auth == null || !auth.equals("Bearer " + runtimeToken)) {
                var jobToken = request.header("PM-MCP-Job-Token");
                if (jobToken == null || !path.startsWith("/mcp")) {
                    response.json(401, Map.of(KEY_ERROR, "unauthorized", KEY_MESSAGE, "Invalid or missing runtime token"));
                    return false;
                }
            }
        }
        return true;
    }

    private boolean handleStateLink(MockHttpRequest request, MockHttpResponse response) throws IOException {
        StateDefinition state;
        String activeStateId;
        synchronized (this) {
            activeStateId = this.currentStateId.get();
            state = currentScenario.get().state(activeStateId);
        }

        if (state == null) {
            return false;
        }

        var matched = findMatchingLink(request, state);
        if (matched == null) {
            return false;
        }

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
                    KEY_ERROR, linkDef.error() != null ? linkDef.error() : "rejected",
                    KEY_STATE, activeStateId,
                    "link", linkName
            );
            response.json(linkDef.status(), errorPayload);
            return true;
        }

        synchronized (this) {
            transitionTo(linkDef.next());
            state = currentScenario.get().state(this.currentStateId.get());
        }

        var nextRep = state != null ? state.representation() : Map.of();
        response.json(linkDef.status(), nextRep);
        return true;
    }

    private void handleNoRoute(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String activeStateId = this.currentStateId.get();
        var state = currentScenario.get().state(activeStateId);
        var offeredLinks = state != null ? new ArrayList<>(state.links().keySet()) : List.<String>of();
        var errorBody = new LinkedHashMap<String, Object>();
        errorBody.put(KEY_ERROR, "mock_no_route");
        errorBody.put(KEY_MESSAGE, "No route matched for " + request.method() + " " + request.path() + " in state '" + activeStateId + "'");
        errorBody.put(KEY_STATE, activeStateId);
        errorBody.put("offered_links", offeredLinks);

        response.json(404, errorBody);
    }

    private Map.Entry<String, LinkDefinition> findMatchingLink(MockHttpRequest request, StateDefinition state) {
        var method = request.method();
        var path = request.path();

        for (var entry : state.links().entrySet()) {
            var linkName = entry.getKey();
            var linkDef = entry.getValue();

            if (!method.equalsIgnoreCase(linkDef.method())) {
                continue;
            }
            if (matchesLinkTarget(linkDef, linkName, path, state)) {
                return entry;
            }
        }
        return null;
    }

    private boolean matchesLinkTarget(LinkDefinition linkDef, String linkName, String path, StateDefinition state) {
        if (linkDef.href() != null && linkDef.href().equals(path)) {
            return true;
        }
        if (path.endsWith("/" + linkName)) {
            return true;
        }
        return matchesRepresentationLink(state, linkName, path);
    }

    private boolean matchesRepresentationLink(StateDefinition state, String linkName, String path) {
        if (!(state.representation() instanceof Map<?, ?> repMap)) {
            return false;
        }
        if (checkLinkGroup(repMap.get("links"), linkName, path)) {
            return true;
        }
        return checkLinkGroup(repMap.get("_links"), linkName, path);
    }

    private boolean checkLinkGroup(Object groupObj, String linkName, String path) {
        if (!(groupObj instanceof Map<?, ?> links) || !links.containsKey(linkName)) {
            return false;
        }
        var targetObj = links.get(linkName);
        if (targetObj instanceof Map<?, ?> targetMap && path.equals(targetMap.get("href"))) {
            return true;
        }
        return path.equals(String.valueOf(targetObj));
    }

    private void handlePairing(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String code = null;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    code = JsonTree.string(parsed, "code");
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException ignored) {
                // Ignore parse errors from invalid body
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

        var expectedCode = currentScenario.get().pairingCode();
        if (code != null && code.replace("-", "").equalsIgnoreCase(expectedCode.replace("-", ""))) {
            var body = Map.of(
                    "device_secret", deviceSecret,
                    "device_id", "mock-device-1",
                    "label", "Mock Browser"
            );
            response.json(200, body);
        } else {
            response.json(400, Map.of(KEY_ERROR, "invalid_code", KEY_MESSAGE, "The pairing code does not match"));
        }
    }

    private void handleEventStream(MockHttpResponse response) throws IOException {
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
                Thread.currentThread().interrupt();
                break;
            }
        }
        activeStreams.remove(stream);
    }

    private void handleControlRequest(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if ("/mock/state".equals(path) && "GET".equals(request.method())) {
            handleControlState(response);
            return;
        }
        if ("/mock/reset".equals(path) && "POST".equals(request.method())) {
            handleControlReset(response);
            return;
        }
        if ("/mock/scenario".equals(path) && "POST".equals(request.method())) {
            handleControlScenario(request, response);
            return;
        }
        if (path.startsWith("/mock/goto/") && "POST".equals(request.method())) {
            handleControlGoto(path.substring("/mock/goto/".length()), response);
            return;
        }
        if ("/mock/broadcast".equals(path) && "POST".equals(request.method())) {
            handleControlBroadcast(request, response);
            return;
        }
        response.json(404, Map.of(KEY_ERROR, "unknown_control_endpoint"));
    }

    private void handleControlState(MockHttpResponse response) throws IOException {
        StateDefinition state;
        String stateId;
        String scenarioName;
        synchronized (this) {
            stateId = this.currentStateId.get();
            scenarioName = this.currentScenario.get().name();
            state = currentScenario.get().state(stateId);
        }
        var offered = state != null ? new ArrayList<>(state.links().keySet()) : List.<String>of();
        response.json(200, Map.of(
                KEY_SCENARIO, scenarioName,
                KEY_STATE, stateId,
                "offered_links", offered
        ));
    }

    private void handleControlReset(MockHttpResponse response) throws IOException {
        reset();
        response.json(200, Map.of(
                KEY_STATUS, "reset",
                KEY_SCENARIO, currentScenario.get().name(),
                KEY_STATE, currentStateId.get()
        ));
    }

    private void handleControlScenario(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String scenarioName = extractScenarioName(request);
        if (scenarioName == null || scenarioName.isBlank()) {
            response.json(400, Map.of(KEY_ERROR, "missing_scenario_name"));
            return;
        }
        try {
            switchScenario(scenarioName);
            response.json(200, Map.of(
                    KEY_STATUS, "switched",
                    KEY_SCENARIO, currentScenario.get().name(),
                    KEY_STATE, currentStateId.get()
            ));
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (RuntimeException e) {
            response.json(404, Map.of(KEY_ERROR, "scenario_not_found", KEY_MESSAGE, e.getMessage()));
        }
    }

    private String extractScenarioName(MockHttpRequest request) {
        String scenarioName = null;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    scenarioName = JsonTree.string(parsed, KEY_SCENARIO);
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException ignored) {
                // Ignore parse errors when extracting scenario name from body
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
        return scenarioName;
    }

    private void handleControlGoto(String targetState, MockHttpResponse response) throws IOException {
        try {
            goToState(targetState);
            response.json(200, Map.of(
                    KEY_STATUS, "transitioned",
                    KEY_SCENARIO, currentScenario.get().name(),
                    KEY_STATE, currentStateId.get()
            ));
        } catch (IllegalArgumentException e) {
            response.json(404, Map.of(KEY_ERROR, "state_not_found", KEY_MESSAGE, e.getMessage()));
        }
    }

    private void handleControlBroadcast(MockHttpRequest request, MockHttpResponse response) throws IOException {
        String event = "message";
        Object data = Map.of();
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    event = Objects.requireNonNullElse(JsonTree.string(parsed, "event"), "message");
                    data = Objects.requireNonNullElse(parsed.get("data"), Map.of());
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException ignored) {
                // Ignore parse errors from broadcast body
            }
        }
        broadcastEvent(event, data);
        response.json(200, Map.of(KEY_STATUS, "broadcasted", "event", event));
    }

    /** @return the current scenario */
    public Scenario currentScenario() {
        return currentScenario.get();
    }

    /** @return the current state id */
    public String currentStateId() {
        return currentStateId.get();
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
                    // Ignore error when closing active stream during engine shutdown
                }
            }
            activeStreams.clear();
        }
    }
}
