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
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.web.PairedDevice;
import de.planmarshall.mock.http.MockEventStream;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.LinkDefinition;
import de.planmarshall.mock.scenario.RouteDefinition;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

/**
 * The core state machine and request dispatcher for the mock runtime.
 */
@SuppressWarnings("java:S1075")
public final class MockRuntimeEngine implements AutoCloseable {

    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_STATE = "state";
    private static final String KEY_SCENARIO = "scenario";
    private static final String KEY_STATUS = "status";
    private static final String KEY_LOOPBACK = "loopback";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_LABEL = "label";
    private static final String KEY_MOCK_DEVICE_1 = "mock-device-1";
    private static final String PATH_DEVICES = "/api/v1/web/devices";
    private static final String PATH_DEVICES_SLASH = "/api/v1/web/devices/";
    private static final String METHOD_DELETE = "DELETE";
    private static final String EVENT_STREAM_CLOSED = "stream.closed";
    private static final String KEY_REASON = "reason";
    private static final String KEY_DETAILS = "details";

    private final ScenarioRegistry registry;
    private final String runtimeToken;
    private final String deviceSecret;
    private final ScheduledExecutorService scheduler;
    private final Set<MockEventStream> activeStreams = ConcurrentHashMap.newKeySet();
    private final List<ScheduledFuture<?>> pendingTasks = new ArrayList<>();

    private final AtomicReference<Scenario> currentScenario = new AtomicReference<>();
    private final AtomicReference<String> currentStateId = new AtomicReference<>();
    private final Object devicesLock = new Object();
    private final List<PairedDevice> pairedDevices = new ArrayList<>();
    private volatile boolean lanListenerOn = true;
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
        initDefaultDevices();
        transitionTo(scenario.start());
    }

    /**
     * Resets the active scenario back to its start state.
     */
    public synchronized void reset() {
        initDefaultDevices();
        transitionTo(currentScenario.get().start());
    }

    private void initDefaultDevices() {
        synchronized (devicesLock) {
            pairedDevices.clear();
            pairedDevices.add(new PairedDevice("dev-1", "Firefox on mbp-oliver", KEY_LOOPBACK,
                    Instant.parse("2026-05-12T10:00:00Z"), Instant.parse("2026-10-16T14:02:00Z")));
            pairedDevices.add(new PairedDevice("dev-2", "Safari on iPad", "network",
                    Instant.parse("2026-05-20T10:00:00Z"), Instant.parse("2026-10-13T10:00:00Z")));
            pairedDevices.add(new PairedDevice("dev-3", "Chrome on build-box", KEY_LOOPBACK,
                    Instant.parse("2026-06-02T10:00:00Z"), Instant.parse("2026-09-08T10:00:00Z")));
            lanListenerOn = true;
        }
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
            } catch (IOException _) {
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

        // 3. Dynamic device and web routes
        if (handleDeviceRoutes(request, response, isWeb)) {
            return;
        }

        // 3a. Dynamic project overview routes
        if (handleProjectOverviewRoutes(request, response)) {
            return;
        }

        // 4. SSE event stream subscription
        if ("GET".equals(request.method()) && ("/api/v1/events".equals(path) || path.endsWith("/events"))) {
            handleEventStream(response);
            return;
        }

        // 5. Match against current state links
        if (handleStateLink(request, response)) {
            return;
        }

        // 6. Match against static routes
        var route = currentScenario.get().route(request.method(), path);
        if (route != null) {
            handleRouteWithDynamicStatus(route, path, response);
            return;
        }

        // 7. No match: answer mock_no_route
        handleNoRoute(request, response);
    }

    private boolean isAuthorized(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        if ("/api/v1/messages".equals(path) || path.startsWith("/api/v1/messages/")) {
            handleMessages(request, response);
            return false;
        }

        return isWeb ? authorizeWeb(request, response) : authorizeLocal(request, response);
    }

    private boolean authorizeWeb(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if ("/api/v1/pair".equals(path) || "/pair".equals(path)) {
            handlePairing(request, response);
            return false;
        }
        var auth = request.header("Authorization");
        if (auth == null || !auth.equals("Bearer " + deviceSecret)) {
            response.json(401, Map.of(KEY_ERROR, "unauthorized", KEY_MESSAGE, "Invalid or missing device secret"));
            return false;
        }
        return true;
    }

    private boolean authorizeLocal(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var auth = request.header("Authorization");
        if (auth == null || !auth.equals("Bearer " + runtimeToken)) {
            var jobToken = request.header("PM-MCP-Job-Token");
            if (jobToken == null || !request.path().startsWith("/mcp")) {
                response.json(401, Map.of(KEY_ERROR, "unauthorized", KEY_MESSAGE, "Invalid or missing runtime token"));
                return false;
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
            } catch (InterruptedException _) {
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

    private static String extractPairingCode(MockHttpRequest request) {
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    var code = JsonTree.string(parsed, "code");
                    if (code != null) {
                        return code;
                    }
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore parse errors from invalid body
            }
        }
        if (request.query() != null) {
            for (var part : request.query().split("&")) {
                if (part.startsWith("code=")) {
                    return part.substring(5);
                }
            }
        }
        return null;
    }

    private void handlePairing(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var code = extractPairingCode(request);
        var expectedCode = currentScenario.get().pairingCode();
        if (code != null && code.replace("-", "").equalsIgnoreCase(expectedCode.replace("-", ""))) {
            var body = Map.of(
                    "device_secret", deviceSecret,
                    KEY_DEVICE_ID, KEY_MOCK_DEVICE_1,
                    KEY_LABEL, "Mock Browser"
            );
            synchronized (devicesLock) {
                boolean found = false;
                for (var d : pairedDevices) {
                    if (KEY_MOCK_DEVICE_1.equals(d.deviceId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    pairedDevices.add(new PairedDevice(KEY_MOCK_DEVICE_1, "Mock Browser", KEY_LOOPBACK, Instant.now(), Instant.now()));
                }
            }
            response.json(200, body);
        } else {
            response.json(400, Map.of(KEY_ERROR, "invalid_code", KEY_MESSAGE, "The pairing code does not match"));
        }
    }

    private void handleCreatePairing(MockHttpResponse response) throws IOException {
        var code = currentScenario.get().pairingCode();
        if (code == null || code.isBlank()) {
            code = "KXQB-MRTN";
        }
        var url = "http://127.0.0.1:7420/pair#code=" + code;
        response.json(201, Map.of("code", code, "url", url, "valid_until", "10m"));
    }

    private void handleListDevices(MockHttpResponse response) throws IOException {
        var list = new ArrayList<Map<String, Object>>();
        synchronized (devicesLock) {
            for (var dev : pairedDevices) {
                list.add(dev.toMap());
            }
        }
        response.json(200, list);
    }

    private void handleRenameDevice(MockHttpRequest request, MockHttpResponse response, String path) throws IOException {
        var sub = path.substring(PATH_DEVICES_SLASH.length());
        if (sub.endsWith("/rename")) {
            sub = sub.substring(0, sub.length() - "/rename".length());
        }
        var targetId = sub;
        String newLabel = null;
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    newLabel = JsonTree.string(parsed, KEY_LABEL);
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore parse errors from invalid body
            }
        }
        if (newLabel == null || newLabel.isBlank()) {
            response.json(400, Map.of(KEY_ERROR, "invalid_label", KEY_MESSAGE, "Device label must not be blank"));
            return;
        }

        PairedDevice updated = null;
        synchronized (devicesLock) {
            for (int i = 0; i < pairedDevices.size(); i++) {
                var dev = pairedDevices.get(i);
                if (dev.deviceId().equals(targetId) || String.valueOf(i + 1).equals(targetId)) {
                    updated = new PairedDevice(dev.deviceId(), newLabel, dev.pairedFrom(), dev.createdAt(), Instant.now());
                    pairedDevices.set(i, updated);
                    break;
                }
            }
        }

        if (updated != null) {
            broadcastEvent("device.renamed", Map.of(KEY_DEVICE_ID, updated.deviceId(), KEY_LABEL, updated.label()));
            response.json(200, updated.toMap());
        } else {
            response.json(404, Map.of(KEY_ERROR, "device_not_found", KEY_MESSAGE, "Device not found: " + targetId));
        }
    }

    private void handleRevokeSingleDevice(MockHttpResponse response, String path, boolean isWeb) throws IOException {
        var targetId = path.substring(PATH_DEVICES_SLASH.length());
        PairedDevice removed = null;
        synchronized (devicesLock) {
            for (int i = 0; i < pairedDevices.size(); i++) {
                var dev = pairedDevices.get(i);
                if (dev.deviceId().equals(targetId) || String.valueOf(i + 1).equals(targetId)) {
                    removed = pairedDevices.remove(i);
                    break;
                }
            }
        }

        if (removed != null) {
            broadcastEvent("device.revoked", Map.of(KEY_DEVICE_ID, removed.deviceId()));
            broadcastEvent(EVENT_STREAM_CLOSED, Map.of(
                    KEY_REASON, "device_revoked",
                    KEY_DETAILS, "Revoked",
                    "revoked_by", isWeb ? "another device" : "the terminal client",
                    "revoked_at", "today at 14:10"
            ));
            response.json(200, Map.of(KEY_STATUS, "revoked", KEY_DEVICE_ID, removed.deviceId(), KEY_LABEL, removed.label()));
        } else {
            response.json(404, Map.of(KEY_ERROR, "device_not_found", KEY_MESSAGE, "Device not found: " + targetId));
        }
    }

    private void handleRevokeAllDevices(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var query = request.query() != null ? request.query().toLowerCase() : "";
        boolean isOthers = query.contains("others");

        var removedList = new ArrayList<PairedDevice>();
        PairedDevice kept = null;
        synchronized (devicesLock) {
            if (isOthers && isWeb) {
                if (!pairedDevices.isEmpty()) {
                    kept = pairedDevices.getFirst();
                    for (int i = pairedDevices.size() - 1; i >= 1; i--) {
                        removedList.add(pairedDevices.remove(i));
                    }
                }
            } else {
                removedList.addAll(pairedDevices);
                pairedDevices.clear();
            }
        }

        for (var dev : removedList) {
            broadcastEvent("device.revoked", Map.of(KEY_DEVICE_ID, dev.deviceId()));
            broadcastEvent(EVENT_STREAM_CLOSED, Map.of(
                    KEY_REASON, "device_revoked",
                    KEY_DETAILS, "Revoked",
                    "revoked_by", isWeb ? "another device" : "the terminal client",
                    "revoked_at", "today at 14:10"
            ));
        }

        var resp = new LinkedHashMap<String, Object>();
        resp.put(KEY_STATUS, "revoked");
        resp.put("revoked_count", removedList.size());
        resp.put("remaining_count", pairedDevices.size());
        if (kept != null) {
            resp.put("kept_label", kept.label());
        }
        response.json(200, resp);
    }

    private void handleCloseLanListener(MockHttpResponse response) throws IOException {
        lanListenerOn = false;
        broadcastEvent(EVENT_STREAM_CLOSED, Map.of(
                KEY_REASON, "listener_closed",
                KEY_DETAILS, "The network listener was turned off"
        ));
        response.json(200, Map.of("lan", false, KEY_STATUS, "listener_off"));
    }

    private void handleUpdateWebConfig(MockHttpRequest request, MockHttpResponse response) throws IOException {
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null && parsed.containsKey("lan")) {
                    lanListenerOn = Boolean.TRUE.equals(parsed.get("lan"));
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
                // Ignore body parsing failures and retain existing listener state
            }
        }
        var resp = new LinkedHashMap<String, Object>();
        resp.put("enabled", true);
        resp.put("lan", lanListenerOn);
        resp.put("port", 7420);
        resp.put("open", true);
        response.json(200, resp);
    }

    private boolean handleDeviceRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        return switch (request.method()) {
            case "GET" -> handleGetDeviceRoutes(path, response);
            case "POST" -> handlePostDeviceRoutes(request, response, path);
            case "PATCH", "PUT" -> handlePutDeviceRoutes(request, response, path);
            case METHOD_DELETE -> handleDeleteDeviceRoutes(request, response, path, isWeb);
            default -> false;
        };
    }

    private boolean handleGetDeviceRoutes(String path, MockHttpResponse response) throws IOException {
        if (PATH_DEVICES.equals(path)) {
            handleListDevices(response);
            return true;
        }
        if ("/api/v1/web".equals(path)) {
            var resp = new LinkedHashMap<String, Object>();
            resp.put("enabled", true);
            resp.put("lan", lanListenerOn);
            resp.put("port", 7420);
            resp.put("open", true);
            response.json(200, resp);
            return true;
        }
        return false;
    }

    private boolean handlePostDeviceRoutes(MockHttpRequest request, MockHttpResponse response, String path) throws IOException {
        if ("/api/v1/web/pairings".equals(path)) {
            handleCreatePairing(response);
            return true;
        }
        if (path.startsWith(PATH_DEVICES_SLASH)) {
            handleRenameDevice(request, response, path);
            return true;
        }
        return false;
    }

    private boolean handlePutDeviceRoutes(MockHttpRequest request, MockHttpResponse response, String path) throws IOException {
        if (path.startsWith(PATH_DEVICES_SLASH)) {
            handleRenameDevice(request, response, path);
            return true;
        }
        if ("/api/v1/web".equals(path)) {
            handleUpdateWebConfig(request, response);
            return true;
        }
        return false;
    }

    private boolean handleDeleteDeviceRoutes(MockHttpRequest request, MockHttpResponse response, String path, boolean isWeb) throws IOException {
        if (path.startsWith(PATH_DEVICES_SLASH)) {
            handleRevokeSingleDevice(response, path, isWeb);
            return true;
        }
        if (PATH_DEVICES.equals(path)) {
            handleRevokeAllDevices(request, response, isWeb);
            return true;
        }
        if ("/api/v1/web/listeners/lan".equals(path)) {
            handleCloseLanListener(response);
            return true;
        }
        return false;
    }

    private void handleRouteWithDynamicStatus(RouteDefinition route, String path, MockHttpResponse response) throws IOException {
        var rep = route.representation();
        if ("/api/v1/status".equals(path) && rep instanceof Map<?, ?> map && map.containsKey("web")) {
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                if ("web".equals(entry.getKey()) && entry.getValue() instanceof Map<?, ?> webMap) {
                    var webCopy = new LinkedHashMap<String, Object>();
                    for (var w : webMap.entrySet()) {
                        webCopy.put(String.valueOf(w.getKey()), w.getValue());
                    }
                    webCopy.put("lan", lanListenerOn);
                    copy.put("web", webCopy);
                } else {
                    copy.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            response.json(route.status(), copy);
            return;
        }
        if ("/api/v1/projects".equals(path) && rep instanceof Map<?, ?> map && map.get("projects") instanceof List<?> list) {
            var sorted = new ArrayList<>(list);
            sorted.sort((a, b) -> {
                if (a instanceof Map<?, ?> ma && b instanceof Map<?, ?> mb) {
                    var countA = JsonTree.integer(JsonTree.asObject(ma), "needs_operator_count");
                    boolean needA = (countA != null && countA > 0) || Boolean.TRUE.equals(ma.get("setup_needs_operator"));
                    var countB = JsonTree.integer(JsonTree.asObject(mb), "needs_operator_count");
                    boolean needB = (countB != null && countB > 0) || Boolean.TRUE.equals(mb.get("setup_needs_operator"));
                    if (needA != needB) {
                        return needA ? -1 : 1;
                    }
                }
                return 0;
            });
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            copy.put("projects", sorted);
            response.json(route.status(), copy);
            return;
        }
        response.json(route.status(), rep);
    }

    private boolean handleProjectOverviewRoutes(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var path = request.path();
        if ("GET".equals(request.method()) && path.startsWith("/api/v1/projects/")) {
            var route = currentScenario.get().route("GET", path);
            if (route != null) {
                if (path.endsWith("/ended")) {
                    handleEndedPageQuery(route, request.query() != null ? request.query() : "page=1", response);
                    return true;
                }
                return false;
            }
            if (path.endsWith("/ended")) {
                var sub = path.substring("/api/v1/projects/".length());
                var prj = sub.substring(0, sub.length() - "/ended".length());
                response.json(200, Map.of(
                        "project_id", prj,
                        "page", 1,
                        "total_pages", 1,
                        "has_newer", false,
                        "has_older", false,
                        "items", List.of()
                ));
                return true;
            }
            var prj = path.substring("/api/v1/projects/".length());
            if (!prj.contains("/")) {
                response.json(200, Map.of(
                        "project_id", prj,
                        "display_name", prj,
                        "plans", List.of(),
                        "epics", List.of(),
                        "alongside_landings", List.of()
                ));
                return true;
            }
        }
        return false;
    }

    private void handleEndedPageQuery(RouteDefinition route, String query, MockHttpResponse response) throws IOException {
        int page = 1;
        for (var part : query.split("&")) {
            if (part.startsWith("page=")) {
                try {
                    page = Integer.parseInt(part.substring(5));
                } catch (NumberFormatException _) {
                    page = 1;
                }
            }
        }
        var rep = route.representation();
        if (rep instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            var totalNum = JsonTree.number(JsonTree.asObject(map), "total_pages");
            int total = totalNum != null ? totalNum.intValue() : 1;
            copy.put("page", page);
            copy.put("has_newer", page > 1);
            copy.put("has_older", page < total);
            if (map.get("pages") instanceof Map<?, ?> pagesMap) {
                var pageItems = pagesMap.get(String.valueOf(page));
                if (pageItems instanceof List<?> list) {
                    copy.put("items", list);
                } else if (page == 1 && map.get("items") instanceof List<?> list) {
                    copy.put("items", list);
                } else {
                    copy.put("items", List.of());
                }
            }
            copy.remove("pages");
            response.json(route.status(), copy);
            return;
        }
        response.json(route.status(), rep);
    }

    private void handleMessages(MockHttpRequest request, MockHttpResponse response) throws IOException {
        if (!"GET".equalsIgnoreCase(request.method())) {
            response.json(405, Map.of(KEY_ERROR, "method_not_allowed", KEY_MESSAGE, "Only GET is supported for messages"));
            return;
        }

        var path = request.path();
        if ("/api/v1/messages".equals(path)) {
            response.json(200, MessageCatalogue.getAvailableLocalesAndAreas());
            return;
        }

        var sub = path.substring("/api/v1/messages/".length());
        var parts = sub.split("/");
        if (parts.length > 2) {
            response.json(400, Map.of(KEY_ERROR, "bad_request", KEY_MESSAGE, "Unexpected path segments"));
            return;
        }
        var locale = parts[0];
        var area = parts.length > 1 ? parts[1] : "all";

        var messages = MessageCatalogue.getMessages(locale, area);
        response.json(200, messages);
    }

    private void handleEventStream(MockHttpResponse response) throws IOException {
        var stream = response.events(200);
        activeStreams.add(stream);
        try {
            stream.sendComment("connected to mock runtime");
        } catch (IOException _) {
            activeStreams.remove(stream);
            return;
        }

        // Keep the stream alive until client disconnects or closed
        while (!closed && !stream.isClosed()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException _) {
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
            } catch (IOException | RuntimeException _) {
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
        String event = KEY_MESSAGE;
        Object data = Map.of();
        if (request.body().length > 0) {
            try {
                var parsed = JsonTree.asObject(JsonTree.parse(request.body()));
                if (parsed != null) {
                    event = Objects.requireNonNullElse(JsonTree.string(parsed, "event"), KEY_MESSAGE);
                    data = Objects.requireNonNullElse(parsed.get("data"), Map.of());
                }
                // cui-rewrite:disable InvalidExceptionUsageRecipe
            } catch (IOException | RuntimeException _) {
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

    /** @return active paired devices */
    public List<PairedDevice> pairedDevices() {
        synchronized (devicesLock) {
            return List.copyOf(pairedDevices);
        }
    }

    /** @return whether LAN listener is active */
    public boolean lanListenerOn() {
        return lanListenerOn;
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
                } catch (IOException _) {
                    // Ignore error when closing active stream during engine shutdown
                }
            }
            activeStreams.clear();
        }
    }
}
