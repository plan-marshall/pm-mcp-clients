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
import de.planmarshall.api.report.DecisionRecord;
import de.planmarshall.api.report.DeliverableSummary;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReadReportsRequest;
import de.planmarshall.api.report.ReplanningRecord;
import de.planmarshall.api.report.ReportFrom;
import de.planmarshall.api.report.ReportItem;
import de.planmarshall.api.report.ReportKind;
import de.planmarshall.api.report.ReportsPage;
import de.planmarshall.api.report.TokenBreakdownItem;
import de.planmarshall.api.report.TokenComponentRecord;
import de.planmarshall.api.report.TokenUsageRecord;
import de.planmarshall.api.report.TokenWasteRecord;
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
    private final Object reportsLock = new Object();
    private final List<ReportItem> mockReports = new ArrayList<>();
    private final Map<String, PlanCompletionReport> completionReports = new ConcurrentHashMap<>();
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
        initDefaultReports();
        transitionTo(scenario.start());
    }

    /**
     * Resets the active scenario back to its start state.
     */
    public synchronized void reset() {
        initDefaultDevices();
        initDefaultReports();
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

        // 3b. Dynamic report routes
        if (handleReportRoutes(request, response, isWeb)) {
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

    private void initDefaultReports() {
        synchronized (reportsLock) {
            mockReports.clear();
            mockReports.add(new ReportItem(
                    "rep-0142",
                    new ReportFrom("pm-mcp-clients", "relay-hardening", "fix-relay"),
                    ReportKind.PLAN_ENDED,
                    "fix-relay ended: merged (#214)",
                    Map.of("plan", "fix-relay", "outcome", "merged (#214)"),
                    "3 of 3 deliverables · 2 h 40 min",
                    Instant.parse("2026-10-16T11:20:00Z"),
                    Instant.parse("2026-10-16T11:20:00Z"),
                    null, null, null,
                    "/api/v1/plans/fix-relay/completion"
            ));
            mockReports.add(new ReportItem(
                    "rep-0143",
                    new ReportFrom("pm-mcp-clients", "relay-hardening", "relay-token-store"),
                    ReportKind.PLAN_STARTED,
                    "relay-token-store started by relay-hardening (2 of 3 at once)",
                    Map.of("plan", "relay-token-store", "epic", "relay-hardening", "running", 2, "scope", 3, "by", "epic"),
                    null,
                    Instant.parse("2026-10-16T11:21:00Z"),
                    Instant.parse("2026-10-16T11:21:00Z"),
                    null, null, null,
                    "/api/v1/plans/relay-token-store"
            ));
            mockReports.add(new ReportItem(
                    "rep-0144",
                    new ReportFrom("pm-mcp-clients", "relay-hardening", "relay-docs"),
                    ReportKind.EPIC_WAITS_OVERLAP,
                    "relay-docs waits: overlap with relay-auth-v2 (2 files)",
                    Map.of("plan", "relay-docs", "other", "relay-auth-v2", "files", 2),
                    null,
                    Instant.parse("2026-10-16T11:21:00Z"),
                    Instant.parse("2026-10-16T11:21:00Z"),
                    null, null, null,
                    "/api/v1/epics/relay-hardening"
            ));
            mockReports.add(new ReportItem(
                    "rep-0139",
                    new ReportFrom("pm-mcp-clients", null, "docs-typo"),
                    ReportKind.PLAN_ENDED,
                    "docs-typo ended: no change",
                    Map.of("plan", "docs-typo", "outcome", "no change"),
                    null,
                    Instant.parse("2026-10-16T09:02:00Z"),
                    Instant.parse("2026-10-16T09:40:00Z"),
                    null, null, null,
                    "/api/v1/plans/docs-typo/completion"
            ));
            mockReports.add(new ReportItem(
                    "rep-0131",
                    new ReportFrom("pm-mcp-core", null, null),
                    ReportKind.PROJECT_ONBOARDED,
                    "pm-mcp-core is onboarded",
                    Map.of("project", "pm-mcp-core"),
                    null,
                    Instant.parse("2026-10-15T18:40:00Z"),
                    Instant.parse("2026-10-15T18:40:00Z"),
                    null, null, null,
                    "/api/v1/projects/pm-mcp-core"
            ));
            mockReports.add(new ReportItem(
                    "rep-0120",
                    new ReportFrom("pm-mcp-clients", "relay-hardening", "relay-auth-v2"),
                    ReportKind.PLAN_ENDED,
                    "relay-auth-v2 ended: merged (#210)",
                    Map.of("plan", "relay-auth-v2", "outcome", "merged (#210)"),
                    null,
                    Instant.parse("2026-10-14T10:00:00Z"),
                    Instant.parse("2026-10-14T10:00:00Z"),
                    null, null, null,
                    "/api/v1/plans/relay-auth-v2/completion"
            ));
            mockReports.add(new ReportItem(
                    "rep-0110",
                    new ReportFrom("pm-mcp-core", null, null),
                    ReportKind.SOURCE_FLAGGED,
                    "A source of codeberg-issues was flagged: issue #4711",
                    Map.of("item", "codeberg-issues", "source", "issue #4711"),
                    null,
                    Instant.parse("2026-10-13T08:00:00Z"),
                    Instant.parse("2026-10-13T08:00:00Z"),
                    null, null, null,
                    "/api/v1/sources/codeberg-issues"
            ));
        }

        completionReports.clear();
        var fixRelayDeliverables = List.of(
                new DeliverableSummary("cursor write order", "done"),
                new DeliverableSummary("a test per transport, websocket and SSE", "done"),
                new DeliverableSummary("fsync of the cursor file", "done")
        );
        var fixRelayReplanning = List.of(
                new ReplanningRecord("the base moved", "09:55", "TASK-5 added")
        );
        var fixRelayDecisions = List.of(
                new DecisionRecord("Q-003", "Rebase onto main", "rebase", "operator", "web", "iPad"),
                new DecisionRecord("Q-005", "Pick transport", "sse", "delegated", null, null)
        );
        var fixRelayComponents = new TokenComponentRecord(310000L, 62000L, 8000L, 29000L, 3000L);
        var fixRelayPhase = List.of(
                new TokenBreakdownItem("refine", 38000, 35000, 3000, 0, false, null, null),
                new TokenBreakdownItem("outline", 96000, 90000, 6000, 0, false, null, null),
                new TokenBreakdownItem("execute", 241000, 222000, 19000, 3, true, null, fixRelayComponents),
                new TokenBreakdownItem("finalize", 37000, 33000, 4000, 0, false, null, null)
        );
        var fixRelayRole = List.of(
                new TokenBreakdownItem("planner", 128000, 120000, 8000, 0, false, "claude:opus-5.5 · high", null),
                new TokenBreakdownItem("implementer", 270000, 247000, 23000, 0, false, "claude:sonnet-5.5 · medium", null),
                new TokenBreakdownItem("reviewer", 14000, 13000, 1000, 3, true, "codex:gpt-5 · low", null)
        );
        var fixRelayModel = List.of(
                new TokenBreakdownItem("claude:opus-5.5", 128000, 120000, 8000, 0, false, null, null),
                new TokenBreakdownItem("claude:sonnet-5.5", 270000, 247000, 23000, 0, false, null, null),
                new TokenBreakdownItem("codex:gpt-5", 14000, 13000, 1000, 3, true, null, null)
        );
        var fixRelayWaste = List.of(
                new TokenWasteRecord("no_product", 18000, 4, 2, "2 tasks"),
                new TokenWasteRecord("retries", 21000, 5, 2, "retryable 1 · stall 1"),
                new TokenWasteRecord("remediation", 13000, 4, 1, "returned with findings 1")
        );
        var fixRelayTokens = new TokenUsageRecord(
                412000, 380000, 32000, 9600, 6, 3, 38, 41, true,
                "their worker was terminated",
                fixRelayPhase, fixRelayRole, fixRelayModel, fixRelayWaste, 52000, 13
        );
        completionReports.put("fix-relay", new PlanCompletionReport(
                "fix-relay",
                "fix-relay",
                new ReportFrom("pm-mcp-clients", "relay-hardening", "fix-relay"),
                "merged",
                "the pull request #214 was merged into main",
                214,
                "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                "—",
                Instant.parse("2026-10-16T08:40:00Z"),
                Instant.parse("2026-10-16T11:20:00Z"),
                9600,
                1200,
                "you",
                fixRelayDeliverables,
                fixRelayReplanning,
                fixRelayDecisions,
                fixRelayTokens
        ));

        completionReports.put("docs-typo", new PlanCompletionReport(
                "docs-typo",
                "docs-typo",
                new ReportFrom("pm-mcp-clients", null, "docs-typo"),
                "no_change",
                "no changes produced",
                null,
                null,
                "—",
                Instant.parse("2026-10-16T08:40:00Z"),
                Instant.parse("2026-10-16T09:00:00Z"),
                1200,
                0,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        ));
    }

    /**
     * Helper to inject and broadcast a new report (for tests or simulations).
     *
     * @param item report item
     */
    public void addReportAndBroadcast(ReportItem item) {
        synchronized (reportsLock) {
            mockReports.add(0, item);
        }
        broadcastEvent("report.sent", Map.of("report", item.toMap()));
    }

    private boolean handleReportRoutes(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        var path = request.path();
        var method = request.method();

        if ("GET".equals(method) && ("/api/v1/reports".equals(path) || "/api/v1/reports/".equals(path))) {
            handleListReports(request, response);
            return true;
        }
        if ("POST".equals(method) && "/api/v1/reports/read".equals(path)) {
            handleMarkReportsRead(request, response, isWeb);
            return true;
        }
        if (path.startsWith("/api/v1/reports/")) {
            var sub = path.substring("/api/v1/reports/".length());
            if (sub.endsWith("/read") && "POST".equals(method)) {
                var reportId = sub.substring(0, sub.length() - "/read".length());
                handleMarkSingleReportRead(response, reportId, isWeb);
                return true;
            }
            if ("GET".equals(method) && !sub.contains("/")) {
                handleGetReport(response, sub);
                return true;
            }
        }
        if ("GET".equals(method) && path.startsWith("/api/v1/plans/") && path.endsWith("/completion")) {
            var sub = path.substring("/api/v1/plans/".length());
            var planId = sub.substring(0, sub.length() - "/completion".length());
            handleGetPlanCompletion(response, planId);
            return true;
        }
        return false;
    }

    private void handleListReports(MockHttpRequest request, MockHttpResponse response) throws IOException {
        var query = request.query() != null ? request.query() : "";
        boolean older = query.contains("older=true");
        String projectFilter = null;
        for (var part : query.split("&")) {
            if (part.startsWith("project=")) {
                projectFilter = part.substring("project=".length());
            }
        }
        if (".".equals(projectFilter)) {
            projectFilter = "pm-mcp-clients";
        }

        List<ReportItem> allUnread = new ArrayList<>();
        synchronized (reportsLock) {
            for (var rep : mockReports) {
                if (!rep.isRead()) {
                    if (projectFilter == null || projectFilter.isBlank() || projectFilter.equals(rep.from().projectId())) {
                        allUnread.add(rep);
                    }
                }
            }
        }

        allUnread.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));

        int pageSize = 5;
        List<ReportItem> paged;
        boolean hasOlder;
        int olderCount;

        if (!older) {
            if (allUnread.size() > pageSize) {
                paged = new ArrayList<>(allUnread.subList(0, pageSize));
                hasOlder = true;
                olderCount = allUnread.size() - pageSize;
            } else {
                paged = new ArrayList<>(allUnread);
                hasOlder = false;
                olderCount = 0;
            }
        } else {
            if (allUnread.size() > pageSize) {
                paged = new ArrayList<>(allUnread.subList(pageSize, allUnread.size()));
            } else {
                paged = List.of();
            }
            hasOlder = false;
            olderCount = 0;
        }

        Instant oldestUnread = null;
        if (!allUnread.isEmpty()) {
            oldestUnread = allUnread.getLast().createdAt();
        }

        var page = new ReportsPage(paged, allUnread.size(), oldestUnread, hasOlder, olderCount);
        response.json(200, page.toMap());
    }

    private void handleGetReport(MockHttpResponse response, String reportId) throws IOException {
        ReportItem target = null;
        synchronized (reportsLock) {
            for (var rep : mockReports) {
                if (rep.id().equals(reportId)) {
                    target = rep;
                    break;
                }
            }
        }
        if (target != null) {
            response.json(200, target.toMap());
        } else {
            response.json(404, Map.of(KEY_ERROR, "report_not_found", KEY_MESSAGE, "Report not found: " + reportId));
        }
    }

    private void handleMarkSingleReportRead(MockHttpResponse response, String reportId, boolean isWeb) throws IOException {
        ReportItem target = null;
        synchronized (reportsLock) {
            for (int i = 0; i < mockReports.size(); i++) {
                var rep = mockReports.get(i);
                if (rep.id().equals(reportId)) {
                    target = rep;
                    if (!rep.isRead()) {
                        var readItem = new ReportItem(
                                rep.id(), rep.from(), rep.kind(), rep.subject(), rep.subjectParams(),
                                rep.body(), rep.createdAt(), rep.sentAt(), Instant.now(),
                                isWeb ? "web" : "cli", isWeb ? "iPad" : "terminal", rep.detailLink()
                        );
                        mockReports.set(i, readItem);
                    }
                    break;
                }
            }
        }
        if (target == null) {
            response.json(404, Map.of(KEY_ERROR, "report_not_found", KEY_MESSAGE, "Report not found: " + reportId));
            return;
        }
        if (target.isRead()) {
            response.json(200, Map.of("count", 0, "marked_count", 0, KEY_STATUS, "already_read"));
            return;
        }
        broadcastEvent("report.read", Map.of(
                "report_id", target.id(),
                "read_at", Instant.now().toString(),
                "channel", isWeb ? "web" : "cli",
                "device", isWeb ? "iPad" : "terminal"
        ));
        response.json(200, Map.of("count", 1, "marked_count", 1, KEY_STATUS, "ok"));
    }

    private void handleMarkReportsRead(MockHttpRequest request, MockHttpResponse response, boolean isWeb) throws IOException {
        boolean all = false;
        List<String> ids = List.of();
        if (request.body().length > 0) {
            try {
                var req = ReadReportsRequest.fromJson(request.bodyText());
                all = req.all();
                ids = req.reportIds();
            } catch (IOException _) {
                // Ignore parse errors from malformed body
            }
        }
        var marked = new ArrayList<ReportItem>();
        synchronized (reportsLock) {
            for (int i = 0; i < mockReports.size(); i++) {
                var rep = mockReports.get(i);
                if (!rep.isRead() && (all || ids.contains(rep.id()))) {
                    var readItem = new ReportItem(
                            rep.id(), rep.from(), rep.kind(), rep.subject(), rep.subjectParams(),
                            rep.body(), rep.createdAt(), rep.sentAt(), Instant.now(),
                            isWeb ? "web" : "cli", isWeb ? "iPad" : "terminal", rep.detailLink()
                    );
                    mockReports.set(i, readItem);
                    marked.add(readItem);
                }
            }
        }
        for (var m : marked) {
            broadcastEvent("report.read", Map.of(
                    "report_id", m.id(),
                    "read_at", Instant.now().toString(),
                    "channel", isWeb ? "web" : "cli",
                    "device", isWeb ? "iPad" : "terminal"
            ));
        }
        response.json(200, Map.of("count", marked.size(), "marked_count", marked.size(), KEY_STATUS, "ok"));
    }

    private void handleGetPlanCompletion(MockHttpResponse response, String planId) throws IOException {
        if ("relay-token-store".equals(planId) || "running-plan".equals(planId)) {
            response.json(409, Map.of(
                    KEY_ERROR, "plan_running",
                    KEY_MESSAGE, planId + " is running; its completion report follows when it ends",
                    "plan", planId,
                    "state", "running"
            ));
            return;
        }
        var report = completionReports.get(planId);
        if (report != null) {
            response.json(200, report.toMap());
            return;
        }
        response.json(404, Map.of(KEY_ERROR, "plan_not_found", KEY_MESSAGE, "Completion report not found for plan: " + planId));
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
        if (route.status() < 200 || route.status() >= 300) {
            response.json(route.status(), route.representation());
            return;
        }
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
