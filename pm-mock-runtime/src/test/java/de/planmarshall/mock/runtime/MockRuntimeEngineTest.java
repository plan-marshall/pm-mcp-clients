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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.entry.RuntimeEntryResource;
import de.planmarshall.api.event.EventName;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.SseEvent;
import de.planmarshall.api.http.SseReader;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.system.RuntimeLifecycleState;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.EventDefinition;
import de.planmarshall.mock.scenario.LinkDefinition;
import de.planmarshall.mock.scenario.RouteDefinition;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioLoader;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

@DisplayName("MockRuntimeEngine")
@EnableGeneratorController
class MockRuntimeEngineTest {

    private ScenarioRegistry registry;
    private MockRuntimeEngine engine;
    private String runtimeToken;
    private String deviceSecret;
    private Map<String, String> auth;
    private String brokenName;
    private String brokenState;

    @BeforeEach
    void setUp() {
        registry = new ScenarioRegistry();
        runtimeToken = "rt-" + Generators.letterStrings(8, 16).next();
        deviceSecret = "ds-" + Generators.letterStrings(8, 16).next();
        auth = Map.of("Authorization", "Bearer " + runtimeToken);
        brokenName = Generators.letterStrings(4, 10).next().toLowerCase();
        brokenState = "s" + Generators.letterStrings(3, 6).next().toLowerCase();
        var initialScenario = registry.resolve(StandardScenarios.STATUS_ONLY);
        engine = new MockRuntimeEngine(registry, initialScenario, runtimeToken, deviceSecret);
    }

    @AfterEach
    void tearDown() {
        engine.close();
    }

    private record TestResponse(int status, String body, Map<String, Object> json) {
    }

    private TestResponse send(String method, String target, Map<String, String> headers, byte[] body, boolean isWeb) throws IOException {
        return send(engine, method, target, headers, body, isWeb);
    }

    private static TestResponse send(MockRuntimeEngine target, String method, String targetPath,
            Map<String, String> headers, byte[] body, boolean isWeb) throws IOException {
        var req = new MockHttpRequest(method, targetPath, headers, body, System.nanoTime());
        var out = new ByteArrayOutputStream();
        var res = new MockHttpResponse(out);
        target.dispatch(req, res, isWeb);

        var raw = out.toString(StandardCharsets.UTF_8);
        var status = parseStatus(raw);
        var bodyStr = parseBody(raw);
        Map<String, Object> json = null;
        if (!bodyStr.isBlank() && bodyStr.startsWith("{")) {
            try {
                json = JsonTree.asObject(JsonTree.parse(bodyStr));
            } catch (IOException | IllegalArgumentException _) {
                // Not valid JSON
            }
        }
        return new TestResponse(status, bodyStr, json);
    }

    private static int parseStatus(String raw) {
        var lines = raw.split("\r\n");
        if (lines.length > 0 && lines[0].startsWith("HTTP/1.1 ")) {
            var parts = lines[0].split(" ");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        }
        return -1;
    }

    private static String parseBody(String raw) {
        var split = raw.indexOf("\r\n\r\n");
        return split >= 0 ? raw.substring(split + 4) : "";
    }

    @Test
    @DisplayName("initializes state and handles getters")
    void initialization() {
        assertEquals(StandardScenarios.ONBOARDING, engine.currentScenario().name());
        assertEquals("status.only", engine.currentStateId());
        assertEquals(runtimeToken, engine.runtimeToken());
        assertEquals(deviceSecret, engine.deviceSecret());
    }

    @Test
    @DisplayName("a malformed state device fixture fails the start instead of falling back to defaults")
    void malformedStateDeviceFixtureFailsFast() {
        var representation = Map.<String, Object>of(
                "scope", Map.of("kind", "machine"),
                "state", Map.of("state_id", "start.state"),
                "facts", Map.of("runtime", Map.of("devices", "not-a-list")));
        var state = new StateDefinition(representation, Map.of(), List.of(), null);
        var deviceFixtureName = Generators.letterStrings(4, 10).next().toLowerCase();
        var scenario = new Scenario(1, "bad-" + deviceFixtureName, "Bad device fixture", "start.state", null,
                Map.of(), Map.of("start.state", state));

        assertThrows(IllegalStateException.class,
                () -> new MockRuntimeEngine(registry, scenario, runtimeToken, deviceSecret));
    }

    @Test
    @DisplayName("a malformed device entry in a state fixture fails the start")
    void malformedStateDeviceEntryFailsFast() {
        var representation = Map.<String, Object>of(
                "scope", Map.of("kind", "machine"),
                "state", Map.of("state_id", "start.state"),
                "facts", Map.of("runtime", Map.of("devices", List.of(Map.of("label", "no id")))));
        var state = new StateDefinition(representation, Map.of(), List.of(), null);
        var deviceEntryName = Generators.letterStrings(4, 10).next().toLowerCase();
        var scenario = new Scenario(1, "bad-" + deviceEntryName, "Bad device entry", "start.state", null,
                Map.of(), Map.of("start.state", state));

        assertThrows(IllegalStateException.class,
                () -> new MockRuntimeEngine(registry, scenario, runtimeToken, deviceSecret));
    }

    @Test
    @DisplayName("a preloaded state whose representation is no object fails the start, as the loader would refuse it")
    void preloadedStateWithoutAnObjectRepresentationFailsFast() {
        var state = new StateDefinition(List.of(Generators.letterStrings(4, 10).next()), Map.of(), List.of(), null);
        var name = Generators.letterStrings(4, 10).next().toLowerCase();
        var scenario = new Scenario(1, "bad-" + name, "No object representation", "start.state", null,
                Map.of(), Map.of("start.state", state));

        assertThrows(IllegalStateException.class,
                () -> new MockRuntimeEngine(registry, scenario, runtimeToken, deviceSecret));
    }

    @Test
    @DisplayName("the status-only case keeps its recorded status and drops the web object")
    void statusOnlyKeepsItsRecordedStatus() throws Exception {
        var res = send("GET", "/api/v1/status", Map.of("Authorization", "Bearer " + runtimeToken),
                new byte[0], false);
        assertEquals(200, res.status());
        assertEquals("ready", res.json().get("status"));
        assertEquals(Boolean.TRUE, res.json().get("onboarded"));
        assertNotNull(res.json().get("listener"));
        assertFalse(res.json().containsKey("web"));
        assertEquals("mock-machine", JsonTree.string(JsonTree.asObject(res.json().get("machine")), "name"));
    }

    @Test
    @DisplayName("refuses a start whose project segment is empty")
    void shouldRefuseStartWithBlankProject() throws Exception {
        // Arrange: the path names the start route but carries no project id.
        var request = JsonTree.write(Map.of("request", Generators.letterStrings(4, 12).next()));
        var body = request.getBytes(StandardCharsets.UTF_8);

        // Act
        var res = send("POST", "/api/v1/projects//plans/start", auth, body, false);

        // Assert
        assertEquals(404, res.status());
        assertEquals("project_not_enrolled", res.json().get("error"));
    }

    @Test
    @DisplayName("goToState transitions to valid state and rejects invalid state")
    void stateTransitions() {
        assertThrows(IllegalArgumentException.class, () -> engine.goToState("non-existent"));

        engine.switchScenario(StandardScenarios.ONBOARDING_HAPPY);
        assertEquals("onboard.mode", engine.currentStateId());

        engine.goToState("harness.choose");
        assertEquals("harness.choose", engine.currentStateId());

        engine.reset();
        assertEquals("onboard.mode", engine.currentStateId());
    }

    @Test
    @DisplayName("control endpoint: GET /mock/state returns scenario and state details")
    void controlState() throws Exception {
        var res = send("GET", "/mock/state", Map.of(), new byte[0], false);
        assertEquals(200, res.status());
        assertNotNull(res.json());
        assertEquals(StandardScenarios.ONBOARDING, res.json().get("scenario"));
        assertEquals("status.only", res.json().get("state"));
        assertEquals("KXQB-MRTN", res.json().get("pairing_code"));
    }

    @Test
    @DisplayName("control endpoint: POST /mock/reset resets state to scenario start")
    void controlReset() throws Exception {
        engine.switchScenario(StandardScenarios.ONBOARDING_HAPPY);
        engine.goToState("harness.choose");
        assertEquals("harness.choose", engine.currentStateId());

        var res = send("POST", "/mock/reset", Map.of(), new byte[0], false);
        assertEquals(200, res.status());
        assertEquals("reset", res.json().get("status"));
        assertEquals("onboard.mode", engine.currentStateId());
    }

    @Test
    @DisplayName("control endpoint: POST /mock/scenario switches active scenario by JSON body and query")
    void controlSwitchScenario() throws Exception {
        // 1. Switch via body
        var body = ("{\"scenario\":\"" + StandardScenarios.WEB_PAIRING + "\"}").getBytes(StandardCharsets.UTF_8);
        var res1 = send("POST", "/mock/scenario", Map.of(), body, false);
        assertEquals(200, res1.status());
        assertEquals("switched", res1.json().get("status"));
        assertEquals(StandardScenarios.ONBOARDING, res1.json().get("scenario"));
        assertEquals("web.pair", engine.currentStateId());
        // 2. Switch via query parameter ?name=...
        var res2 = send("POST", "/mock/scenario?name=" + StandardScenarios.STATUS_ONLY, Map.of(), new byte[0], false);
        assertEquals(200, res2.status());
        assertEquals(StandardScenarios.ONBOARDING, res2.json().get("scenario"));

        // 3. Switch via query parameter ?scenario=...
        var res3 = send("POST", "/mock/scenario?scenario=" + StandardScenarios.WEB_PAIRING, Map.of(), new byte[0], false);
        assertEquals(200, res3.status());
        assertEquals(StandardScenarios.ONBOARDING, res3.json().get("scenario"));

        // 4. Missing scenario name -> 400
        var res4 = send("POST", "/mock/scenario", Map.of(), new byte[0], false);
        assertEquals(400, res4.status());
        assertEquals("missing_scenario_name", res4.json().get("error"));

        // 5. Unknown scenario name -> 404
        var res5 = send("POST", "/mock/scenario?name=unknown-"
                + Generators.letterStrings(6, 12).next(), Map.of(), new byte[0], false);
        assertEquals(404, res5.status());
        assertEquals("scenario_not_found", res5.json().get("error"));
    }

    @Test
    @DisplayName("control endpoint: POST /mock/goto transitions state and validates existence")
    void controlGoto() throws Exception {
        engine.switchScenario(StandardScenarios.ONBOARDING_HAPPY);
        var res1 = send("POST", "/mock/goto/harness.choose", Map.of(), new byte[0], false);
        assertEquals(200, res1.status());
        assertEquals("transitioned", res1.json().get("status"));
        assertEquals("harness.choose", engine.currentStateId());

        var res2 = send("POST", "/mock/goto/" + Generators.letterStrings(6, 12).next(),
                Map.of(), new byte[0], false);
        assertEquals(404, res2.status());
        assertEquals("state_not_found", res2.json().get("error"));
    }

    @Test
    @DisplayName("control endpoint: POST /mock/broadcast broadcasts event")
    void controlBroadcast() throws Exception {
        var eventName = Generators.letterStrings(4, 10).next().toLowerCase();
        var body = ("{\"event\":\"" + eventName + "\",\"data\":{\""
                + Generators.letterStrings(4, 8).next() + "\":\""
                + Generators.letterStrings(4, 8).next() + "\"}}").getBytes(StandardCharsets.UTF_8);
        var res = send("POST", "/mock/broadcast", Map.of(), body, false);
        assertEquals(200, res.status());
        assertEquals("broadcasted", res.json().get("status"));
        assertEquals(eventName, res.json().get("event"));
    }

    @Test
    @DisplayName("the status reports what waits, by kind, and follows a broadcast answer")
    void statusWaitingCountsFollowBroadcasts() throws Exception {
        try (var showroom = new MockRuntimeEngine(registry, registry.resolve(StandardScenarios.FULLY_ONBOARDED),
                     runtimeToken, deviceSecret)) {
            var before = send(showroom, "GET", "/api/v1/status", auth, new byte[0], false);
            var waitingBefore = JsonTree.asObject(before.json().get("needs_operator"));
            assertEquals(3, JsonTree.integer(waitingBefore, "decision"));

            var answer = "{\"event\":\"decision.answered\",\"data\":{\"id\":\"Q-007\",\"answer\":\"Rebase onto main\","
                    + "\"channel\":\"web\",\"device\":\"iPad\"}}";
            send(showroom, "POST", "/mock/broadcast", Map.of(), answer.getBytes(StandardCharsets.UTF_8), false);

            var after = send(showroom, "GET", "/api/v1/status", auth, new byte[0], false);
            assertEquals(2, JsonTree.integer(JsonTree.asObject(after.json().get("needs_operator")), "decision"));
        }
    }

    @Test
    @DisplayName("a broadcast decision.opened joins the open decisions")
    void broadcastOpenedDecisionCounts() throws Exception {
        try (var showroom = new MockRuntimeEngine(registry, registry.resolve(StandardScenarios.FULLY_ONBOARDED),
                     runtimeToken, deviceSecret)) {
            var newDecision = "Q-" + Generators.integers(500, 999).next();
            var opened = "{\"event\":\"decision.opened\",\"data\":{\"decision\":{\"id\":\"" + newDecision + "\","
                    + "\"title\":\"" + Generators.letterStrings(4, 10).next()
                    + "\",\"subject\":\"plan fix-relay\"}}}";
            send(showroom, "POST", "/mock/broadcast", Map.of(), opened.getBytes(StandardCharsets.UTF_8), false);

            var after = send(showroom, "GET", "/api/v1/status", Map.of("Authorization", "Bearer " + runtimeToken),
                    new byte[0], false);
            assertEquals(4, JsonTree.integer(JsonTree.asObject(after.json().get("needs_operator")), "decision"));
        }
    }

    @Test
    @DisplayName("a broadcast waiver request counts under its own kind")
    void broadcastWaiverCountsSeparately() throws Exception {
        try (var showroom = new MockRuntimeEngine(registry, registry.resolve(StandardScenarios.FULLY_ONBOARDED),
                     runtimeToken, deviceSecret)) {
            var waiver = "{\"event\":\"decision.opened\",\"data\":{\"decision\":{\"id\":\"WR-2\","
                    + "\"title\":\"Waiver\",\"subject\":\"plan fix-relay\",\"is_waiver\":true,"
                    + "\"waiver_gate\":\"sonar\"}}}";
            send(showroom, "POST", "/mock/broadcast", Map.of(), waiver.getBytes(StandardCharsets.UTF_8), false);

            var after = send(showroom, "GET", "/api/v1/status", Map.of("Authorization", "Bearer " + runtimeToken),
                    new byte[0], false);
            var waiting = JsonTree.asObject(after.json().get("needs_operator"));
            assertNotNull(waiting);
            assertEquals(1, JsonTree.integer(waiting, "waiver"),
                    "a waiver counts under the contract's kind waiver: " + waiting);
            assertFalse(waiting.containsKey("waiver_request"),
                    "the mock serves one list of kinds, never the specification's spelling beside it: " + waiting);
        }
    }

    @Test
    @DisplayName("every kind the status counts is a kind of the contract's closed list")
    void statusCountsOnlyContractKinds() throws Exception {
        try (var showroom = new MockRuntimeEngine(registry, registry.resolve(StandardScenarios.FULLY_ONBOARDED),
                     runtimeToken, deviceSecret)) {
            var waiver = "{\"event\":\"decision.opened\",\"data\":{\"decision\":{\"id\":\"WR-"
                    + Generators.integers(100, 999).next() + "\",\"title\":\"Waiver\",\"subject\":\"plan fix-relay\","
                    + "\"is_waiver\":true,\"waiver_gate\":\"sonar\"}}}";
            send(showroom, "POST", "/mock/broadcast", Map.of(), waiver.getBytes(StandardCharsets.UTF_8), false);
            var status = send(showroom, "GET", "/api/v1/status", auth, new byte[0], false);
            var waiting = JsonTree.asObject(status.json().get("needs_operator"));
            assertNotNull(waiting);
            for (var kind : waiting.keySet()) {
                assertEquals(kind, NeedsOperatorKind.fromWireOrOther(kind).value(),
                        "the status names " + kind + " outside the contract's kinds: " + waiting);
            }
        }
    }

    @Test
    @DisplayName("control endpoint: unknown path returns 404")
    void controlUnknown() throws Exception {
        var res = send("GET", "/mock/unknown-endpoint", Map.of(), new byte[0], false);
        assertEquals(404, res.status());
        assertEquals("unknown_control_endpoint", res.json().get("error"));
    }

    @Test
    @DisplayName("web authentication: pairing endpoint succeeds with valid code and rejects invalid code")
    void webPairingAuthentication() throws Exception {
        engine.switchScenario(StandardScenarios.WEB_PAIRING);

        // 1. Pairing with matching code in JSON body
        var body = "{\"code\":\"KXQB-MRTN\"}".getBytes(StandardCharsets.UTF_8);
        var res1 = send("POST", "/api/v1/pair", Map.of(), body, true);
        assertEquals(200, res1.status());
        assertEquals(deviceSecret, res1.json().get("device_secret"));
        assertEquals("mock-device-1", res1.json().get("device_id"));

        // 2. A fresh code in the query parameter (case and dash insensitive); the redeemed code is single use
        var pairings = send("POST", "/api/v1/web/pairings",
                Map.of("Authorization", "Bearer " + runtimeToken), new byte[0], false);
        assertEquals(201, pairings.status());
        var freshCode = String.valueOf(pairings.json().get("code"));
        var res2 = send("POST", "/pair?code=" + freshCode.toLowerCase().replace("-", ""),
                Map.of(), new byte[0], true);
        assertEquals(200, res2.status());
        assertEquals(deviceSecret, res2.json().get("device_secret"));

        // 3. Pairing with mismatched code -> 400
        var res3 = send("POST", "/pair?code=wrong", Map.of(), new byte[0], true);
        assertEquals(400, res3.status());
        assertEquals("pairing_invalid", res3.json().get("error"));
    }

    @Test
    @DisplayName("control endpoint: POST /mock/pairing returns the open code or issues a fresh one")
    void controlFreshPairingCode() throws Exception {
        engine.switchScenario(StandardScenarios.WEB_PAIRING);

        // 1. The scenario's code is consumed by a redemption.
        var consumedBody = "{\"code\":\"KXQB-MRTN\"}".getBytes(StandardCharsets.UTF_8);
        assertEquals(200, send("POST", "/api/v1/pair", Map.of(), consumedBody, true).status());

        // 2. The development loop asks for the replacement; repeated calls keep the open code.
        var issued = send("POST", "/mock/pairing", Map.of(), new byte[0], false);
        assertEquals(201, issued.status());
        var freshCode = String.valueOf(issued.json().get("code"));
        assertFalse(freshCode.isEmpty());
        assertNotEquals("KXQB-MRTN", freshCode, "a consumed code must never be reissued");
        var again = send("POST", "/mock/pairing", Map.of(), new byte[0], false);
        assertEquals(freshCode, again.json().get("code"));

        // 3. The fresh code pairs; the consumed one stays refused.
        var freshBody = ("{\"code\":\"" + freshCode + "\"}").getBytes(StandardCharsets.UTF_8);
        assertEquals(200, send("POST", "/api/v1/pair", Map.of(), freshBody, true).status());
        var refused = send("POST", "/api/v1/pair", Map.of(), consumedBody, true);
        assertEquals(400, refused.status());
        assertEquals("pairing_invalid", refused.json().get("error"));
    }

    @Test
    @DisplayName("web authentication: protected paths require Bearer deviceSecret")
    void webProtectedAuthentication() throws Exception {
        // Missing Authorization -> 401
        var res1 = send("GET", "/api/v1/status", Map.of(), new byte[0], true);
        assertEquals(401, res1.status());
        assertFalse(res1.json().containsKey("error"), "a 401 carries no code: " + res1.body());

        // Wrong Authorization -> 401
        var res2 = send("GET", "/api/v1/status", Map.of("Authorization",
                "Bearer " + deviceSecret + "x"), new byte[0], true);
        assertEquals(401, res2.status());

        // Correct Authorization -> accepted
        var res3 = send("GET", "/api/v1/status", Map.of("Authorization", "Bearer " + deviceSecret), new byte[0], true);
        assertEquals(200, res3.status());

        // The runtime token is a local credential, never a web one -> 401
        var res4 = send("GET", "/api/v1/status", Map.of("Authorization", "Bearer " + runtimeToken), new byte[0], true);
        assertEquals(401, res4.status());
    }

    @Test
    @DisplayName("unix authentication: requires runtime token or job token on /mcp")
    void unixAuthentication() throws Exception {
        // Missing Authorization -> 401
        var res1 = send("GET", "/api/v1/status", Map.of(), new byte[0], false);
        assertEquals(401, res1.status());
        assertFalse(res1.json().containsKey("error"), "a 401 carries no code: " + res1.body());

        // Valid runtime token -> 200
        var res2 = send("GET", "/api/v1/status", Map.of("Authorization", "Bearer " + runtimeToken), new byte[0], false);
        assertEquals(200, res2.status());

        // Job token on /mcp path -> accepted
        var res3 = send("GET", "/mcp/job-status", Map.of("PM-MCP-Job-Token",
                Generators.letterStrings(8, 16).next()), new byte[0], false);
        // Returns 404 mock_no_route because path has no route, but authentication passed (not 401)
        assertEquals(404, res3.status());
        assertEquals("mock_no_route", res3.json().get("error"));
    }

    @Test
    @DisplayName("handles mock_no_route for unmatched requests with offered_links")
    void mockNoRoute() throws Exception {
        var res = send("GET", "/api/v1/unmatched-path", auth, new byte[0], false);
        assertEquals(404, res.status());
        assertNotNull(res.json());
        assertEquals("mock_no_route", res.json().get("error"));
        assertEquals("status.only", res.json().get("state"));
        assertNotNull(res.json().get("offered_links"));
    }

    @Test
    @DisplayName("an unoffered domain endpoint answers mock_no_route naming the state")
    void unofferedDomainIsNoRoute() throws Exception {
        var system = send("GET", "/api/v1/system", auth, new byte[0], false);
        assertEquals(404, system.status());
        assertEquals("mock_no_route", system.json().get("error"));
        assertEquals("status.only", system.json().get("state"));

        var decisions = send("GET", "/api/v1/decisions", auth, new byte[0], false);
        assertEquals(404, decisions.status());
        assertEquals("mock_no_route", decisions.json().get("error"));

        // The domain a scenario declares is served.
        engine.switchScenario(StandardScenarios.SYSTEM);
        assertEquals(200, send("GET", "/api/v1/system", auth, new byte[0], false).status());
    }

    @Test
    @DisplayName("every stateful handler family is classified as a live domain path")
    void liveDomainPrefixesCoverHandlers() {
        var representatives = List.of(
                "/api/v1/reports",
                "/api/v1/plans/p-1/process",
                "/api/v1/epics/e-1/decomposition",
                "/api/v1/entries/ent-1",
                "/api/v1/projects/p-1",
                "/api/v1/project-onboardings",
                "/api/v1/explorer",
                "/api/v1/overview",
                "/api/v1/find",
                "/api/v1/machine/needs-operator",
                "/api/v1/workflows",
                "/api/v1/decisions",
                "/api/v1/questions",
                "/api/v1/system",
                "/api/v1/operator/runtime/stop",
                "/api/v1/runtime/stop",
                "/api/v1/recipes",
                "/api/v1/intake/draft",
                "/api/v1/onboarding/auto",
                "/api/v1/web",
                "/api/v1/web/listeners/lan",
                "/api/v1/status/connection",
                "/api/v1/enrol",
                "/api/v1/credentials/github",
                "/api/v1/machine/decisions",
                "/api/v1/machine/settings",
                "/api/v1/config");
        for (var path : representatives) {
            assertTrue(MockRuntimeEngine.isLiveDomainPath(path), path + " must be a live domain path");
        }
        // The operator link convention is scenario-first, never a live domain.
        assertFalse(MockRuntimeEngine.isLiveDomainPath("/api/v1/machine/links/yes"));
        assertFalse(MockRuntimeEngine.isLiveDomainPath("/api/v1/status"));
    }

    @Test
    @DisplayName("a scenario static route outside the live domains wins over the domain defaults")
    void scenarioRouteWinsOutsideLiveDomains() throws Exception {
        var scenarioName = Generators.letterStrings(4, 10).next().toLowerCase();
        var value = Generators.letterStrings(4, 10).next();
        var state = new StateDefinition(Map.of("message", "start"), Map.of(), List.of(), null);
        var testScenario = new Scenario(1, scenarioName, "Route precedence", "start", null,
                Map.of("GET /api/v1/custom/thing", new RouteDefinition(
                        Map.of("value", value), 200)),
                Map.of("start", state));
        registry.register(testScenario);
        engine.switchScenario(scenarioName);

        var res = send("GET", "/api/v1/custom/thing", Map.of("Authorization", "Bearer " + runtimeToken),
                new byte[0], false);
        assertEquals(200, res.status());
        assertEquals(value, res.json().get("value"));
    }

    @Test
    @DisplayName("matches state link, advances state, and returns next representation")
    void stateLinkTransition() throws Exception {
        engine.switchScenario(StandardScenarios.ONBOARDING_HAPPY);
        assertEquals("onboard.mode", engine.currentStateId());

        // Send POST to /api/v1/onboarding/language with the begin link
        var res = send("POST", "/api/v1/onboarding/language", Map.of("Authorization", "Bearer " + runtimeToken), new byte[0], false);
        assertEquals(200, res.status());
        assertEquals("language.choose", engine.currentStateId());
    }

    @Test
    @DisplayName("handles the flat plan control routes the operator verbs post to")
    void flatPlanControlRoutes() throws Exception {
        engine.switchScenario(StandardScenarios.FULLY_ONBOARDED);

        var paused = send("POST", "/api/v1/plans/fix-relay/pause", auth,
                "{\"point\":\"now\"}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, paused.status());
        assertEquals("pausing", paused.json().get("state"));
        assertEquals("now", paused.json().get("pausing_point"));

        var started = send("POST", "/api/v1/plans/fix-relay/start", auth, new byte[0], false);
        assertEquals(200, started.status());
        assertEquals("running", started.json().get("state"));

        var cancelled = send("POST", "/api/v1/plans/fix-relay/cancel", auth,
                "{\"reason\":\"replaced\",\"close_pr\":true}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, cancelled.status());
        assertEquals("ended", cancelled.json().get("state"));
    }

    @Test
    @DisplayName("the flat plan control routes refuse an unknown plan and a bare verb")
    void flatPlanControlRoutesRefuse() throws Exception {
        engine.switchScenario(StandardScenarios.FULLY_ONBOARDED);

        var unknown = send("POST", "/api/v1/plans/no-such-plan/pause", auth, new byte[0], false);
        assertEquals(404, unknown.status());
        assertEquals("scope_not_found", unknown.json().get("error"));

        var notAnAction = send("POST", "/api/v1/plans/fix-relay/archive", auth, new byte[0], false);
        assertEquals(404, notAnAction.status());
    }

    @Test
    @DisplayName("handles link with refusal status >= 400")
    void errorLinkRefusal() throws Exception {
        var refusalScenario = Generators.letterStrings(4, 10).next().toLowerCase();
        var errorCode = Generators.letterStrings(4, 10).next().toLowerCase();
        var errorLink = new LinkDefinition("start", "POST", "/api/v1/fail", 0, 409, errorCode);
        var stateWithRefusal = new StateDefinition(
                Map.of("message", "blocked"),
                Map.of("fail", errorLink),
                List.of(),
                null
        );
        var testScenario = new Scenario(1, refusalScenario, "Refusal test", "start", null,
                Map.of(), Map.of("start", stateWithRefusal));

        registry.register(testScenario);
        engine.switchScenario(refusalScenario);

        var res = send("POST", "/api/v1/fail", Map.of("Authorization", "Bearer " + runtimeToken), new byte[0], false);
        assertEquals(409, res.status());
        assertEquals(errorCode, res.json().get("error"));
        assertEquals("start", res.json().get("state"));
        assertEquals("fail", res.json().get("link"));
    }

    @Test
    @DisplayName("handles link with execution delay")
    void delayedLink() throws Exception {
        var delayScenario = Generators.letterStrings(4, 10).next().toLowerCase();
        var nextStateId = "s" + Generators.letterStrings(3, 6).next().toLowerCase();
        var delayMs = Generators.integers(20, 40).next().intValue();
        var delayedLink = new LinkDefinition(nextStateId, "POST", "/api/v1/delayed", delayMs, 200, null);
        var startState = new StateDefinition(
                Map.of("message", "start"),
                Map.of("delayed", delayedLink),
                List.of(),
                null
        );
        var nextState = new StateDefinition(Map.of("message", "done"), Map.of(), List.of(), null);
        var testScenario = new Scenario(1, delayScenario, "Delay test", "start", null,
                Map.of(), Map.of("start", startState, nextStateId, nextState));

        registry.register(testScenario);
        engine.switchScenario(delayScenario);

        var startNanos = System.nanoTime();
        var res = send("POST", "/api/v1/delayed", Map.of("Authorization", "Bearer " + runtimeToken), new byte[0], false);
        var elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertEquals(200, res.status());
        assertTrue(elapsedMs >= delayMs - 5, "Expected elapsed time to reflect delay: " + elapsedMs + "ms");
        assertEquals(nextStateId, engine.currentStateId());
    }

    @Test
    @DisplayName("matches static routes from scenario")
    void staticRouteMatching() throws Exception {
        engine.switchScenario(StandardScenarios.ONBOARDING_HARNESS_NOT_LOGGED_IN);
        assertEquals("harness.not-logged-in", engine.currentStateId());

        // GET /api/v1/status is a static route in this scenario
        var res = send("GET", "/api/v1/status", Map.of("Authorization", "Bearer " + runtimeToken), new byte[0], false);
        assertEquals(200, res.status());
        assertNotNull(res.json());
        // State remains harness.not-logged-in
        assertEquals("harness.not-logged-in", engine.currentStateId());
    }

    @Test
    @DisplayName("broadcast sends event to active streams")
    void broadcast() {
        engine.broadcast("custom-evt", Map.of("hello", "world"));
        // Does not throw with zero streams
        assertNotNull(engine);
    }

    @Test
    @DisplayName("serves message catalogue metadata without authorization")
    void messageCatalogueMetadata() throws Exception {
        var res = send("GET", "/api/v1/messages", Map.of(), new byte[0], false);
        assertEquals(200, res.status());
        assertNotNull(res.json());
        assertTrue(res.json().containsKey("locales"));
        assertTrue(res.json().containsKey("areas"));
    }

    @Test
    @DisplayName("serves area messages and merged all messages")
    void areaMessages() throws Exception {
        var enRes = send("GET", "/api/v1/messages/en/common", Map.of(), new byte[0], true);
        assertEquals(200, enRes.status());
        assertNotNull(enRes.json());
        assertTrue(enRes.json().containsKey("web.common.copy"));

        var deAllRes = send("GET", "/api/v1/messages/de/all", Map.of(), new byte[0], false);
        assertEquals(200, deAllRes.status());
        assertNotNull(deAllRes.json());
        assertTrue(deAllRes.json().containsKey("web.common.copy"));

        var emptyRes = send("GET", "/api/v1/messages/en/nonexistent", Map.of(), new byte[0], false);
        assertEquals(200, emptyRes.status());
        assertNotNull(emptyRes.json());
        assertTrue(emptyRes.json().isEmpty());
    }

    @Test
    @DisplayName("rejects non-GET or malformed message paths")
    void messagePathValidation() throws Exception {
        var postRes = send("POST", "/api/v1/messages", Map.of(), new byte[0], false);
        assertEquals(405, postRes.status());

        var malformedRes = send("GET", "/api/v1/messages/en/common/extra/segments", Map.of(), new byte[0], false);
        assertEquals(400, malformedRes.status());
        assertEquals("invalid_arguments", malformedRes.json().get("error"),
                "a malformed path answers in the catalogue's codes");
    }

    @Test
    @DisplayName("manages paired devices and web listener routes")
    void webDevicesRoutes() throws Exception {

        // 1. POST /api/v1/web/pairings
        var pairRes = send("POST", "/api/v1/web/pairings", auth, new byte[0], false);
        assertEquals(201, pairRes.status());
        assertNotNull(pairRes.json().get("code"));
        assertTrue(pairRes.json().get("url").toString().contains("code="));

        // 2. GET /api/v1/web/devices
        var listRes = send("GET", "/api/v1/web/devices", auth, new byte[0], false);
        assertEquals(200, listRes.status());
        assertTrue(listRes.body().contains("Firefox on mbp-oliver"));

        // 3. PATCH /api/v1/web/devices/dev-1 (valid label)
        var renameLabel = Generators.letterStrings(4, 12).next();
        var renameBody = ("{\"label\":\"" + renameLabel + "\"}").getBytes(StandardCharsets.UTF_8);
        var renameRes = send("PATCH", "/api/v1/web/devices/dev-1", auth, renameBody, false);
        assertEquals(200, renameRes.status());
        assertEquals(renameLabel, renameRes.json().get("label"));

        // 4. PATCH /api/v1/web/devices/1 (by index)
        var indexLabel = Generators.letterStrings(4, 12).next();
        var indexRename = ("{\"label\":\"" + indexLabel + "\"}").getBytes(StandardCharsets.UTF_8);
        var indexRes = send("PATCH", "/api/v1/web/devices/1", auth, indexRename, false);
        assertEquals(200, indexRes.status());

        // 5. PATCH with blank label -> 400
        var blankBody = "{\"label\":\"\"}".getBytes(StandardCharsets.UTF_8);
        var blankRes = send("PATCH", "/api/v1/web/devices/dev-1", auth, blankBody, false);
        assertEquals(400, blankRes.status());
        assertEquals("invalid_arguments", blankRes.json().get("error"));

        // 6. PATCH non-existent -> 404
        var notFoundRes = send("PATCH", "/api/v1/web/devices/non-existent", auth, renameBody, false);
        assertEquals(404, notFoundRes.status());
        assertEquals("target_not_found", notFoundRes.json().get("error"));

        // 7. DELETE /api/v1/web/devices/dev-2 (single)
        var delRes = send("DELETE", "/api/v1/web/devices/dev-2", auth, new byte[0], false);
        assertEquals(200, delRes.status());
        assertEquals("revoked", delRes.json().get("status"));

        // 8. DELETE non-existent single -> 404
        var delNotFound = send("DELETE", "/api/v1/web/devices/non-existent", auth, new byte[0], false);
        assertEquals(404, delNotFound.status());

        // 9. DELETE /api/v1/web/devices?others (on web)
        var webAuth = Map.of("Authorization", "Bearer " + deviceSecret);
        var othersRes = send("DELETE", "/api/v1/web/devices?scope=others", webAuth, new byte[0], true);
        assertEquals(200, othersRes.status());
        assertEquals("revoked", othersRes.json().get("status"));

        // 10. DELETE /api/v1/web/devices (revoke all)
        var allRes = send("DELETE", "/api/v1/web/devices", auth, new byte[0], false);
        assertEquals(200, allRes.status());
        assertEquals("0", String.valueOf(allRes.json().get("remaining_count")));

        // 11. DELETE /api/v1/web/listeners/lan
        var lanOff = send("DELETE", "/api/v1/web/listeners/lan", auth, new byte[0], false);
        assertEquals(200, lanOff.status());
        assertEquals(Boolean.FALSE, lanOff.json().get("lan"));

        // 12. PUT /api/v1/web (restore lan)
        var putBody = "{\"lan\":true}".getBytes(StandardCharsets.UTF_8);
        var putRes = send("PUT", "/api/v1/web", auth, putBody, false);
        assertEquals(200, putRes.status());
        assertEquals(Boolean.TRUE, putRes.json().get("lan"));
    }

    @Test
    @DisplayName("refuses a runtime fixture whose member has the wrong type instead of reading it as absent")
    void refusesWrongTypedFixture() {
        var scenario = ScenarioLoader.load(brokenFixtureJson(), brokenName);
        var failure = assertThrows(IllegalStateException.class,
                () -> new MockRuntimeEngine(registry, scenario, runtimeToken, deviceSecret));
        assertTrue(failure.getMessage().contains("facts.runtime.control.holds"), failure.getMessage());
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(delimiter = '|', value = {
            "{ \"plans\": [ { \"state\": \"running\" } ] }|facts.runtime.control.plans names a plan without plan_id",
            "{ \"epics\": [ { \"epic_id\": \" \" } ] }|facts.runtime.control.epics names an epic without epic_id",
            "{ \"epics\": [ { \"epic_id\": \"e-1\", \"reviews\": [] } ] }|facts.runtime.control.epics.e-1.reviews must be an object",
            "{ \"epics\": [ { \"epic_id\": \"e-1\", \"reviews\": { \"plan\": 1 } } ] }|facts.runtime.control.epics.e-1.reviews.plan must be an object"})
    @DisplayName("refuses a control fixture that names work without its id or a review that is no object")
    void refusesAnIncompleteControlFixture(String control, String expected) {
        var scenario = ScenarioLoader.load(controlFixtureJson(control), brokenName);

        var failure = assertThrows(IllegalStateException.class,
                () -> new MockRuntimeEngine(registry, scenario, runtimeToken, deviceSecret));

        assertTrue(failure.getMessage().contains(expected), failure.getMessage());
    }

    @Test
    @DisplayName("a failed switch leaves the active scenario, its state, and its fixtures untouched")
    void failedSwitchKeepsTheActiveScenario() throws Exception {
        var running = registry.resolve(StandardScenarios.RUNNING_WORK);
        try (var live = new MockRuntimeEngine(registry, running, runtimeToken, deviceSecret)) {
            var beforeState = live.currentStateId();
            registry.register(ScenarioLoader.load(brokenFixtureJson(),
                    brokenName));

            var failure = assertThrows(IllegalStateException.class,
                    () -> live.switchScenario(brokenName));
            assertTrue(failure.getMessage().contains("facts.runtime.control.holds"), failure.getMessage());

            assertEquals(StandardScenarios.RUNNING_WORK, live.currentScenario().name());
            assertEquals(beforeState, live.currentStateId());

            var actions = send(live, "GET", "/api/v1/plans/fix-relay/actions",
                    Map.of("Authorization", "Bearer " + runtimeToken), new byte[0], false);
            assertEquals(200, actions.status());
            assertEquals("relay-hardening", actions.json().get("epic_id"));
        }
    }

    @Test
    @DisplayName("records the operator acts since the last reset and serves them at /mock/acts")
    void actLogRecordsActsAndResetEmptiesIt() throws Exception {
        engine.switchScenario(StandardScenarios.FULLY_ONBOARDED);

        send("POST", "/api/v1/plans/fix-relay/pause", auth,
                "{\"point\":\"after_task\"}".getBytes(StandardCharsets.UTF_8), false);
        send("POST", "/api/v1/projects/pm-mcp-clients/favourite", auth,
                "{\"favourite\":true}".getBytes(StandardCharsets.UTF_8), false);

        var acts = send("GET", "/mock/acts", Map.of(), new byte[0], false);
        assertEquals(200, acts.status());
        var listed = acts.json().get("acts");
        assertInstanceOf(List.class, listed, String.valueOf(listed));
        var list = (List<?>) listed;
        assertEquals(2, list.size(), String.valueOf(list));
        var first = JsonTree.asObject(list.getFirst());
        var second = JsonTree.asObject(list.get(1));
        assertNotNull(first);
        assertNotNull(second);
        assertEquals("POST", first.get("method"));
        assertEquals("/api/v1/plans/fix-relay/pause", first.get("path"));
        assertEquals("showroom.active", first.get("state"));
        assertEquals("POST", second.get("method"));
        assertEquals("/api/v1/projects/pm-mcp-clients/favourite", second.get("path"));

        var reset = send("POST", "/mock/reset", Map.of(), new byte[0], false);
        assertEquals(200, reset.status());
        var emptied = send("GET", "/mock/acts", Map.of(), new byte[0], false);
        assertTrue(emptied.json().get("acts") instanceof List<?> after && after.isEmpty(),
                String.valueOf(emptied.json()));

        send("POST", "/mcp", auth, "{}".getBytes(StandardCharsets.UTF_8), false);
        assertTrue(send("GET", "/mock/acts", Map.of(), new byte[0], false).json().get("acts")
                instanceof List<?> mcpActs && mcpActs.isEmpty(),
                "an MCP call is no operator act");
    }

    private String brokenFixtureJson() {
        return controlFixtureJson("{ \"holds\": { \"hold_id\": \"H-1\" } }");
    }

    private String controlFixtureJson(String control) {
        return """
                {
                  "format_version": 1,
                  "name": "%1$s",
                  "start": "%2$s",
                  "states": {
                    "%2$s": {
                      "representation": {
                        "scope": { "kind": "machine" },
                        "state": { "state_id": "%2$s" },
                        "facts": {
                          "runtime": {
                            "control": %3$s
                          }
                        },
                        "links": []
                      }
                    }
                  }
                }
                """.formatted(brokenName, brokenState, control);
    }

    @Test
    @DisplayName("a project's setup serves its identity and advances to completed")
    void projectSetupServesItsState() throws Exception {
        engine.switchScenario(StandardScenarios.PROJECTS_AND_START);

        var identity = send("GET", "/api/v1/projects/cui-parent/setup", auth, new byte[0], false);
        var advanced = send("POST", "/api/v1/projects/cui-parent/setup", auth,
                "{\"link\":\"set\"}".getBytes(StandardCharsets.UTF_8), false);
        var again = send("GET", "/api/v1/projects/cui-parent/setup", auth, new byte[0], false);

        assertAll(
                () -> assertEquals(200, identity.status()),
                () -> assertEquals("project.identity", JsonTree.string(identity.json(), "state")),
                () -> assertEquals("cui-parent", JsonTree.string(identity.json(), "id")),
                () -> assertEquals(Boolean.FALSE, JsonTree.bool(identity.json(), "pilot")),
                () -> assertEquals(200, advanced.status()),
                () -> assertEquals("project.completed", JsonTree.string(advanced.json(), "state")),
                () -> assertEquals("project.completed", JsonTree.string(again.json(), "state")));
    }

    @Test
    @DisplayName("a project setup id that arrives percent-encoded is decoded")
    void projectSetupDecodesTheId() throws Exception {
        engine.switchScenario(StandardScenarios.PROJECTS_AND_START);

        var res = send("GET", "/api/v1/projects/new%20service/setup", auth, new byte[0], false);

        assertAll(
                () -> assertEquals(200, res.status()),
                () -> assertEquals("new service", JsonTree.string(res.json(), "id")));
    }

    @Test
    @DisplayName("setWebPort updates webPort reflected in system and connection endpoints")
    void webPortReflectedInSystemAndConnection() throws Exception {
        engine.switchScenario(StandardScenarios.FULLY_ONBOARDED);
        var webPort = Generators.integers(1024, 65535).next().intValue();
        engine.setWebPort(webPort);

        var connRes = send("GET", "/api/v1/status/connection", Map.of("Authorization", "Bearer " + deviceSecret), new byte[0], true);
        assertEquals(200, connRes.status());
        assertEquals("http://127.0.0.1:" + webPort, JsonTree.string(connRes.json(), "loopback_url"));

        var webRes = send("GET", "/api/v1/web", Map.of("Authorization", "Bearer " + deviceSecret), new byte[0], true);
        assertEquals(200, webRes.status());
        assertEquals(Integer.valueOf(webPort), JsonTree.integer(webRes.json(), "port"));

        var sysRes = send("GET", "/api/v1/system", Map.of("Authorization", "Bearer " + runtimeToken), new byte[0], false);
        assertEquals(200, sysRes.status());
        var runtime = JsonTree.asObject(sysRes.json().get("runtime"));
        assertNotNull(runtime);
        assertEquals("127.0.0.1:" + webPort, JsonTree.string(runtime, "loopback_listener"));
    }

    @Test
    @DisplayName("after a reset and after a scenario switch the started plans store is cleared")
    void resetAndScenarioSwitchClearStartedPlans() throws Exception {
        engine.switchScenario(StandardScenarios.PROJECTS_AND_START);
        var startRes = send("POST", "/api/v1/projects/pm-mcp-clients/plans/start", auth,
                "{\"title\":\"Test Plan A\"}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(201, startRes.status());
        var planId = JsonTree.string(startRes.json(), "id");
        assertNotNull(planId);

        var processRes = send("GET", "/api/v1/plans/" + planId + "/process", auth, new byte[0], false);
        assertEquals(200, processRes.status());

        engine.reset();

        var processAfterReset = send("GET", "/api/v1/plans/" + planId + "/process", auth, new byte[0], false);
        assertEquals(404, processAfterReset.status());

        var startRes2 = send("POST", "/api/v1/projects/pm-mcp-clients/plans/start", auth,
                "{\"title\":\"Test Plan B\"}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(201, startRes2.status());
        var planId2 = JsonTree.string(startRes2.json(), "id");
        assertNotNull(planId2);

        var processRes2 = send("GET", "/api/v1/plans/" + planId2 + "/process", auth, new byte[0], false);
        assertEquals(200, processRes2.status());

        engine.switchScenario(StandardScenarios.STATUS_ONLY);
        var processAfterSwitch = send("GET", "/api/v1/plans/" + planId2 + "/process", auth, new byte[0], false);
        assertEquals(404, processAfterSwitch.status());
    }

    @Test
    @DisplayName("after a reset and after a scenario switch the project setup states store is cleared")
    void resetAndScenarioSwitchClearProjectSetupStates() throws Exception {
        engine.switchScenario(StandardScenarios.PROJECTS_AND_START);

        var advanced = send("POST", "/api/v1/projects/cui-parent/setup", auth,
                "{\"link\":\"set\"}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, advanced.status());
        assertEquals("project.completed", JsonTree.string(advanced.json(), "state"));

        engine.reset();

        var stateAfterReset = send("GET", "/api/v1/projects/cui-parent/setup", auth, new byte[0], false);
        assertEquals(200, stateAfterReset.status());
        assertEquals("project.identity", JsonTree.string(stateAfterReset.json(), "state"));

        var advancedAgain = send("POST", "/api/v1/projects/cui-parent/setup", auth,
                "{\"link\":\"set\"}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, advancedAgain.status());
        assertEquals("project.completed", JsonTree.string(advancedAgain.json(), "state"));

        engine.switchScenario(StandardScenarios.STATUS_ONLY);
        engine.switchScenario(StandardScenarios.PROJECTS_AND_START);

        var stateAfterSwitch = send("GET", "/api/v1/projects/cui-parent/setup", auth, new byte[0], false);
        assertEquals(200, stateAfterSwitch.status());
        assertEquals("project.identity", JsonTree.string(stateAfterSwitch.json(), "state"));
    }

    @Test
    @DisplayName("a plan made through the create act of a start entry answers /process with 200")
    void planCreatedThroughStartEntryCreateActAnswersProcess() throws Exception {
        engine.switchScenario(StandardScenarios.PROJECTS_AND_START);

        var openRes = send("POST", "/api/v1/projects/pm-mcp-clients/entries", auth,
                "{\"kind\":\"plan\",\"title\":\"Create act test plan\",\"recipe_key\":\"lesson-cleanup\"}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(201, openRes.status());
        var openStart = JsonTree.asObject(openRes.json().get("start"));
        assertNotNull(openStart);
        var openEntry = JsonTree.asObject(openStart.get("entry"));
        assertNotNull(openEntry);
        var entryId = JsonTree.string(openEntry, "entry_id");
        assertNotNull(entryId);

        var createRes = send("POST", "/api/v1/entries/" + entryId + "/links/create", auth,
                "{\"title\":\"My Created Plan\"}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, createRes.status());
        var startMap = JsonTree.asObject(createRes.json().get("start"));
        assertNotNull(startMap);
        var entryMap = JsonTree.asObject(startMap.get("entry"));
        assertNotNull(entryMap);
        var planId = JsonTree.string(entryMap, "plan_id");
        assertNotNull(planId);

        var processRes = send("GET", "/api/v1/plans/" + planId + "/process", auth, new byte[0], false);
        assertEquals(200, processRes.status());
        assertEquals(planId, JsonTree.string(processRes.json(), "plan_id"));
    }

    @Test
    @DisplayName("a scenario with a malformed start-state fact is refused with the right error and leaves the engine exactly as it was")
    void scenarioWithMalformedStartStateFactIsRefused() throws Exception {
        var running = registry.resolve(StandardScenarios.RUNNING_WORK);
        try (var live = new MockRuntimeEngine(registry, running, runtimeToken, deviceSecret)) {
            var beforeScenario = live.currentScenario().name();
            var beforeState = live.currentStateId();
            // An act before the switch: the refused switch must leave it recorded.
            var paused = send(live, "POST", "/api/v1/plans/fix-relay/pause", auth,
                    "{\"point\":\"now\"}".getBytes(StandardCharsets.UTF_8), false);
            assertEquals(200, paused.status(), paused.body());

            var brokenJobScenarioJson = """
                    {
                      "format_version": 1,
                      "name": "broken-job-scenario",
                      "start": "start_broken",
                      "states": {
                        "start_broken": {
                          "representation": {
                            "scope": { "kind": "machine" },
                            "state": { "state_id": "start_broken" },
                            "facts": {
                              "job": { "invalid": true }
                            },
                            "links": []
                          }
                        }
                      }
                    }
                    """;
            registry.register(ScenarioLoader.load(brokenJobScenarioJson, "broken-job-scenario"));

            var failure = assertThrows(IllegalStateException.class,
                    () -> live.switchScenario("broken-job-scenario"));
            assertTrue(failure.getMessage().contains("facts.job"), failure.getMessage());

            assertEquals(beforeScenario, live.currentScenario().name());
            assertEquals(beforeState, live.currentStateId());

            var switchRes = send(live, "POST", "/mock/scenario?name=broken-job-scenario", Map.of(), new byte[0], false);
            assertEquals(500, switchRes.status());
            assertEquals("scenario_fixture_invalid", switchRes.json().get("error"));

            var acts = send(live, "GET", "/mock/acts", Map.of(), new byte[0], false);
            assertTrue(acts.json().get("acts") instanceof List<?> recorded && recorded.size() == 1,
                    "the act before the refused switch is still recorded: " + acts.json());
            var actions = send(live, "GET", "/api/v1/plans/fix-relay/actions", auth, new byte[0], false);
            assertEquals(200, actions.status(), actions.body());
        }
    }

    @Test
    @DisplayName("a non-object job or durations fact is rejected, not skipped")
    void nonObjectJobOrDurationsFactIsRejected() throws Exception {
        var running = registry.resolve(StandardScenarios.RUNNING_WORK);
        try (var live = new MockRuntimeEngine(registry, running, runtimeToken, deviceSecret)) {
            var nonObjectFactsScenarioJson = """
                    {
                      "format_version": 1,
                      "name": "non-object-facts-scenario",
                      "start": "valid_start",
                      "states": {
                        "valid_start": {
                          "representation": {
                            "scope": { "kind": "machine" },
                            "state": { "state_id": "valid_start" },
                            "facts": {},
                            "links": []
                          }
                        },
                        "state_with_array_job": {
                          "representation": {
                            "scope": { "kind": "machine" },
                            "state": { "state_id": "state_with_array_job" },
                            "facts": {
                              "job": []
                            },
                            "links": []
                          }
                        },
                        "state_with_array_durations": {
                          "representation": {
                            "scope": { "kind": "machine" },
                            "state": { "state_id": "state_with_array_durations" },
                            "facts": {
                              "durations": []
                            },
                            "links": []
                          }
                        }
                      }
                    }
                    """;
            registry.register(ScenarioLoader.load(nonObjectFactsScenarioJson, "non-object-facts-scenario"));
            live.switchScenario("non-object-facts-scenario");

            var failureJob = assertThrows(IllegalStateException.class,
                    () -> live.goToState("state_with_array_job"));
            assertTrue(failureJob.getMessage().contains("facts.job must be an object"), failureJob.getMessage());

            var gotoJobRes = send(live, "POST", "/mock/goto/state_with_array_job", Map.of(), new byte[0], false);
            assertEquals(500, gotoJobRes.status());
            assertEquals("state_fixture_invalid", gotoJobRes.json().get("error"));

            var failureDurations = assertThrows(IllegalStateException.class,
                    () -> live.goToState("state_with_array_durations"));
            assertTrue(failureDurations.getMessage().contains("facts.durations must be an object"), failureDurations.getMessage());

            var gotoDurationsRes = send(live, "POST", "/mock/goto/state_with_array_durations", Map.of(), new byte[0], false);
            assertEquals(500, gotoDurationsRes.status());
            assertEquals("state_fixture_invalid", gotoDurationsRes.json().get("error"));
        }
    }

    @Test
    @DisplayName("a repeated cleanup delete returns the adjusted freed_bytes")
    void repeatedCleanupDeleteReturnsAdjustedFreedBytes() throws Exception {
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        var firstDelete = send("POST", "/api/v1/system/cleanup/cln-0012/delete", auth,
                "{\"unchecked_ids\":[\"token-usage\"]}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, firstDelete.status());
        assertEquals("deleted", firstDelete.json().get("status"));
        assertEquals(1872127584L, JsonTree.number(firstDelete.json(), "freed_bytes").longValue());

        var secondDelete = send("POST", "/api/v1/system/cleanup/cln-0012/delete", auth,
                new byte[0], false);
        assertEquals(200, secondDelete.status());
        assertEquals("deleted", secondDelete.json().get("status"));
        assertEquals(1872127584L, JsonTree.number(secondDelete.json(), "freed_bytes").longValue());
    }

    @Test
    @DisplayName("the system snapshot reflects the live cleanup job after delete")
    void systemSnapshotReflectsLiveCleanupJob() throws Exception {
        engine.switchScenario(StandardScenarios.SYSTEM);
        assertEquals("system.running", engine.currentStateId());

        var beforeRes = send("GET", "/api/v1/system", auth, new byte[0], false);
        assertEquals(200, beforeRes.status());
        var beforeCleanup = JsonTree.asObject(beforeRes.json().get("cleanup"));
        assertNotNull(beforeCleanup);
        assertEquals(12, JsonTree.integer(beforeCleanup, "plans_count"));

        var deleteRes = send("POST", "/api/v1/system/cleanup/cln-0012/delete", auth,
                new byte[0], false);
        assertEquals(200, deleteRes.status());

        var afterDeleteRes = send("GET", "/api/v1/system", auth, new byte[0], false);
        assertEquals(200, afterDeleteRes.status());
        var afterCleanup = JsonTree.asObject(afterDeleteRes.json().get("cleanup"));
        assertNotNull(afterCleanup);
        assertEquals(0, JsonTree.integer(afterCleanup, "plans_count"));
    }

    @Test
    @DisplayName("a concurrent find during an enrol does not throw")
    void concurrentFindDuringEnrolDoesNotThrow() throws Exception {
        engine.switchScenario(StandardScenarios.PROJECTS_AND_START);
        var running = new AtomicBoolean(true);
        var errors = new CopyOnWriteArrayList<Throwable>();
        var completedFinds = new AtomicInteger();
        var enrols = Generators.integers(30, 60).next();
        var projectPrefix = Generators.letterStrings(4, 8).next().toLowerCase();

        var findThread = new Thread(() -> {
            // At least one find runs, so the count below never passes on a thread that never ran.
            do {
                try {
                    var found = send("GET", "/api/v1/find?q=pm", auth, new byte[0], false);
                    if (found.status() == 200) {
                        completedFinds.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.add(e);
                    break;
                }
            } while (running.get());
        });

        findThread.start();
        for (int i = 0; i < enrols; i++) {
            send("POST", "/api/v1/projects/" + projectPrefix + "-" + i + "/enrol", auth,
                    ("{\"path\":\"/path/to/" + projectPrefix + i + "\"}").getBytes(StandardCharsets.UTF_8), false);
        }
        running.set(false);
        findThread.join(5000);

        assertFalse(findThread.isAlive(), "the find thread ends once the enrols are done");
        assertTrue(errors.isEmpty(), "find must not throw concurrently: " + errors);
        assertTrue(completedFinds.get() > 0, "the finds must have completed while the enrols ran");
    }

    @Test
    @DisplayName("completing project setup makes the enrolled project ready in the explorer read")
    void completingProjectSetupMakesEnrolledProjectReady() throws Exception {
        engine.switchScenario(StandardScenarios.PROJECTS_AND_START);

        var explorerBefore = send("GET", "/api/v1/explorer", auth, new byte[0], false);
        assertEquals(200, explorerBefore.status());
        var allBefore = JsonTree.asArray(explorerBefore.json().get("all_projects"));
        assertNotNull(allBefore);
        var targetBefore = allBefore.stream()
                .map(JsonTree::asObject)
                .filter(p -> "plan-marshall".equals(JsonTree.string(p, "project_id")))
                .findFirst()
                .orElseThrow();
        assertEquals("setup_needs", JsonTree.string(targetBefore, "readiness"));

        var advanced = send("POST", "/api/v1/projects/plan-marshall/setup", auth,
                "{\"link\":\"set\"}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, advanced.status());
        assertEquals("project.completed", JsonTree.string(advanced.json(), "state"));

        var explorerAfter = send("GET", "/api/v1/explorer", auth, new byte[0], false);
        assertEquals(200, explorerAfter.status());
        var allAfter = JsonTree.asArray(explorerAfter.json().get("all_projects"));
        assertNotNull(allAfter);
        var targetAfter = allAfter.stream()
                .map(JsonTree::asObject)
                .filter(p -> "plan-marshall".equals(JsonTree.string(p, "project_id")))
                .findFirst()
                .orElseThrow();
        assertEquals("ready", JsonTree.string(targetAfter, "readiness"));
    }

    @Test
    @DisplayName("a link other than set does not complete the identity step")
    void linkOtherThanSetDoesNotCompleteSetup() throws Exception {
        engine.switchScenario(StandardScenarios.PROJECTS_AND_START);

        var nonSetLink = send("POST", "/api/v1/projects/cui-parent/setup", auth,
                "{\"link\":\"other\"}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, nonSetLink.status());
        assertEquals("project.identity", JsonTree.string(nonSetLink.json(), "state"));
    }

    @Test
    @DisplayName("a state effect that carries a question member and an outer id is applied")
    void stateEffectWithQuestionMemberAndOuterIdIsApplied() throws Exception {
        engine.switchScenario(StandardScenarios.RUNNING_WORK);

        var broadcastPayload = """
                {
                  "event": "decision.opened",
                  "data": {
                    "id": "Q-999",
                    "question": {
                      "revision": 1,
                      "title": "Custom Question Title",
                      "header": "Custom Header",
                      "subject": "plan custom-plan",
                      "scope": "plan",
                      "asking_phase": "execute",
                      "asking_role": "implement",
                      "blocks": "plan",
                      "reversibility": "reversible",
                      "description": "Custom question description.",
                      "parts": []
                    }
                  }
                }
                """;
        var broadcastRes = send("POST", "/mock/broadcast", Map.of(),
                broadcastPayload.getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, broadcastRes.status());

        var getRes = send("GET", "/api/v1/decisions", auth, new byte[0], false);
        assertEquals(200, getRes.status());
        var list = JsonTree.asArray(JsonTree.parse(getRes.body()));
        assertNotNull(list);
        var dec = list.stream()
                .map(JsonTree::asObject)
                .filter(d -> "Q-999".equals(JsonTree.string(d, "id")))
                .findFirst()
                .orElseThrow();
        assertEquals("Custom Question Title", JsonTree.string(dec, "title"));
        assertEquals("plan custom-plan", JsonTree.string(dec, "subject"));
        assertEquals("Custom question description.", JsonTree.string(dec, "description"));
    }

    @Test
    @DisplayName("a stop holding the system lock while a reset holds the engine monitor, and a stop racing resets, do not deadlock")
    void stopRacingResetDoesNotDeadlock() throws Exception {
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(STATE_SYSTEM_RUNNING);
        var rounds = Generators.integers(150, 300).next();
        var drain = drainBody(THEN_EXIT);

        // The one interleaving a lock-order inversion deadlocks on, forced rather than hoped for.
        assertEquals(200, forceStopHoldingSystemLockAgainstReset(drain));

        // Then the free race of many stops, withdrawals and resets.
        var acceptedStops = new AtomicInteger();
        var start = new CountDownLatch(1);
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Future<?> stops = executor.submit(() -> {
                start.await();
                for (var round = 0; round < rounds; round++) {
                    if (send("POST", PATH_STOP, auth, drain, false).status() == 200) {
                        acceptedStops.incrementAndGet();
                    }
                    send("DELETE", PATH_STOP, auth, new byte[0], false);
                }
                return null;
            });
            Future<?> resets = executor.submit(() -> {
                start.await();
                for (var round = 0; round < rounds; round++) {
                    engine.reset();
                    engine.goToState(STATE_SYSTEM_RUNNING);
                }
                return null;
            });
            start.countDown();

            awaitBounded(stops, "the stops");
            awaitBounded(resets, "the resets");
        } finally {
            executor.shutdownNow();
        }

        assertTrue(acceptedStops.get() > 0, "a stop must have reached the drain while the resets ran");
        var afterwards = send("POST", PATH_STOP, auth, drain, false);
        assertEquals(200, afterwards.status(), afterwards.body());
        assertEquals(RuntimeLifecycleState.DRAINING, engine.lifecycleState());
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    @DisplayName("a stale runtime.draining event is rejected by its generation")
    void staleDrainingEventIsRejectedByItsGeneration() throws Exception {
        var items = drainItems(Generators.integers(2, 4).next());
        var stateId = drainStateId();
        registerSystemState(stateId, drainingFacts(items, THEN_EXIT), List.of());
        var staleMarker = markerEvent();
        var freshMarker = markerEvent();
        var latches = Map.of(staleMarker, new CountDownLatch(1), freshMarker, new CountDownLatch(1));

        try (var runtime = startRuntime()) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var bearer = Map.of("Authorization", "Bearer " + runtime.token());
            try (var exchange = client.open(HttpRequest.get("/api/v1/events")
                         .withHeader("Authorization", "Bearer " + runtime.token()))) {
                assertTrue(exchange.isEventStream());
                var names = new CopyOnWriteArrayList<String>();
                readEvents(exchange.events(), names, latches);
                var live = runtime.engine();
                live.goToState(stateId);

                // Arrange a drain event of an earlier drain: its generation is not the running one.
                var staleItem = Generators.letterStrings(5, 10).next();
                var staleGeneration = Generators.integers(1_000_000, 2_000_000).next();
                live.broadcast(EVENT_DRAINING, Map.of(
                        "lifecycle_state", "draining",
                        "remaining_items", List.of(staleItem),
                        "remaining_work_count", 1,
                        "drain_generation", staleGeneration));
                live.broadcast(staleMarker, Map.of());
                assertTrue(latches.get(staleMarker).await(5, TimeUnit.SECONDS), "the boundary arrives: " + names);

                assertFalse(names.subList(0, names.indexOf(staleMarker)).contains(EVENT_DRAINING),
                        "a stale drain event must reach no client: " + names);
                assertEquals(items, remainingItems(live, bearer), "a stale drain event must not change the status");

                // Control: an event of the running drain, which names no generation, does arrive.
                var freshItems = List.of(items.getLast());
                live.broadcast(EVENT_DRAINING, Map.of(
                        "lifecycle_state", "draining",
                        "remaining_items", freshItems,
                        "remaining_work_count", 1));
                live.broadcast(freshMarker, Map.of());
                assertTrue(latches.get(freshMarker).await(5, TimeUnit.SECONDS), "the boundary arrives: " + names);
                assertTrue(names.subList(names.indexOf(staleMarker), names.indexOf(freshMarker))
                        .contains(EVENT_DRAINING), "an event of the running drain reaches the client: " + names);
                assertEquals(freshItems, remainingItems(live, bearer));
            }
        }
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    @DisplayName("a state's own scheduled event survives a drain start")
    void stateScheduledEventSurvivesDrainStart() throws Exception {
        var stateId = drainStateId();
        var marker = markerEvent();
        var afterMs = Generators.integers(0, 20).next();
        registerSystemState(stateId, Map.of(), List.of(new EventDefinition(afterMs, marker, Map.of())));
        var latches = Map.of(marker, new CountDownLatch(1));
        // The engine's scheduler is held until the drain has started, so the state's event is due
        // only after it, without waiting for a delay.
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        var release = holdScheduler(scheduler);

        try (var runtime = startRuntime(scheduler)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var bearer = "Bearer " + runtime.token();
            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", bearer))) {
                assertTrue(exchange.isEventStream());
                var names = new CopyOnWriteArrayList<String>();
                readEvents(exchange.events(), names, latches);
                runtime.engine().goToState(stateId);

                var stop = client.send(HttpRequest.post(PATH_STOP).withHeader("Authorization", bearer)
                        .withBody(new String(drainBody(THEN_EXIT), StandardCharsets.UTF_8)));
                assertEquals(200, stop.status(), stop.bodyText());
                release.countDown();

                assertTrue(latches.get(marker).await(5, TimeUnit.SECONDS),
                        "the state's own event must still arrive after the drain started: " + names);
                assertTrue(names.indexOf(EVENT_DRAINING) >= 0 && names.indexOf(EVENT_DRAINING) < names.indexOf(marker),
                        "the drain started before the state's event was due: " + names);
            }
        } finally {
            // a start that failed before the engine took the scheduler must not leave it held
            release.countDown();
            scheduler.shutdownNow();
        }
    }

    @Test
    @DisplayName("a four-item drain leaves draining on its last step, for stopped when it ends in an exit")
    void fourItemDrainEndsStoppedOnItsLastStep() throws Exception {
        var items = drainItems(4);
        var stateId = drainStateId();
        registerSystemState(stateId, drainingFacts(items, THEN_EXIT), List.of());
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(stateId);
        assertEquals(items, remainingItems(engine, auth));

        for (var step = 1; step < items.size(); step++) {
            engine.shrinkDrain();
            assertEquals(RuntimeLifecycleState.DRAINING, engine.lifecycleState(), "step " + step + " still drains");
            assertEquals(items.subList(step, items.size()), remainingItems(engine, auth),
                    "step " + step + " ends the first item it still runs");
        }

        engine.shrinkDrain();

        var status = send("GET", "/api/v1/status", auth, new byte[0], false);
        assertAll(
                () -> assertEquals(RuntimeLifecycleState.STOPPED, engine.lifecycleState()),
                () -> assertEquals("stopped", status.json().get("lifecycle_state")),
                () -> assertTrue(remainingItems(engine, auth).isEmpty(), "an ended drain runs nothing"));
    }

    @Test
    @DisplayName("a drain that ends in a restart is running again after its last step")
    void drainEndingInRestartRunsAgain() throws Exception {
        var items = drainItems(2);
        var stateId = drainStateId();
        registerSystemState(stateId, drainingFacts(items, "restart"), List.of());
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(stateId);

        engine.shrinkDrain();
        assertEquals(RuntimeLifecycleState.DRAINING, engine.lifecycleState(), "one item left still drains");
        assertEquals(List.of(items.getLast()), remainingItems(engine, auth));

        engine.shrinkDrain();
        assertEquals(RuntimeLifecycleState.RUNNING, engine.lifecycleState());
        assertTrue(remainingItems(engine, auth).isEmpty(), "an ended drain runs nothing");
    }

    @Test
    @DisplayName("a malformed facts.kept leaves the previous scenario untouched")
    void malformedKeptFactLeavesThePreviousScenario() throws Exception {
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(STATE_SYSTEM_RUNNING);
        // An act before the switch: the refused switch must keep what it changed.
        var kept = send("PUT", "/api/v1/system/cleanup/kept", auth,
                "{\"item_ids\":[\"token-usage\",\"relay-auth\"]}".getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, kept.status(), kept.body());
        var beforeKept = send("GET", "/api/v1/system/cleanup/kept", auth, new byte[0], false).body();

        var brokenScenario = "kept-" + brokenName;
        registry.register(ScenarioLoader.load("""
                {
                  "format_version": 1,
                  "name": "%1$s",
                  "start": "%2$s",
                  "states": {
                    "%2$s": {
                      "representation": {
                        "scope": { "kind": "machine" },
                        "state": { "state_id": "%2$s" },
                        "facts": { "kept": [ "%3$s" ] },
                        "links": []
                      }
                    }
                  }
                }
                """.formatted(brokenScenario, brokenState, Generators.letterStrings(4, 10).next()), brokenScenario));

        var failure = assertThrows(IllegalStateException.class, () -> engine.switchScenario(brokenScenario));

        var afterKept = send("GET", "/api/v1/system/cleanup/kept", auth, new byte[0], false);
        assertAll(
                () -> assertTrue(failure.getMessage().contains("facts.kept"), failure.getMessage()),
                () -> assertEquals(StandardScenarios.REPORTS_AND_SYSTEM, engine.currentScenario().name()),
                () -> assertEquals(STATE_SYSTEM_RUNNING, engine.currentStateId()),
                () -> assertEquals(200, afterKept.status(), afterKept.body()),
                () -> assertEquals(beforeKept, afterKept.body(), "the kept items of the act survive the refused switch"));
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    @DisplayName("a decision opened through /mock/broadcast is closed for the clients by a jump to a state without it")
    void broadcastDecisionIsClosedByJump() throws Exception {
        var decisionId = "Q-" + Generators.integers(1000, 9999).next();
        var latches = Map.of(EVENT_DECISION_CLOSED, new CountDownLatch(1));

        try (var runtime = startRuntime()) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            try (var exchange = client.open(HttpRequest.get("/api/v1/events")
                         .withHeader("Authorization", "Bearer " + runtime.token()))) {
                assertTrue(exchange.isEventStream());
                var events = new CopyOnWriteArrayList<SseEvent>();
                readFullEvents(exchange.events(), events, latches);
                var live = runtime.engine();
                var opened = send(live, "POST", "/mock/broadcast", Map.of(),
                        decisionOpenedBody(decisionId).getBytes(StandardCharsets.UTF_8), false);
                assertEquals(200, opened.status(), opened.body());

                live.goToState(STATE_SYSTEM_RUNNING);

                assertTrue(latches.get(EVENT_DECISION_CLOSED).await(5, TimeUnit.SECONDS),
                        "the jump must close the broadcast decision: " + events);
                var closed = events.stream().filter(event -> EVENT_DECISION_CLOSED.equals(event.event()))
                        .map(event -> JsonTree.asObject(parseJson(event.data())))
                        .findFirst().orElseThrow();
                var closedText = JsonTree.write(closed);
                // The envelope carries the payload either flat or under data, as the catalogue shapes it.
                assertAll(
                        () -> assertTrue(closedText.contains("\"" + decisionId + "\""), closedText),
                        () -> assertTrue(closedText.contains("\"withdrawn\""), closedText),
                        () -> assertTrue(closedText.contains("\"state_left\""), closedText));
            }
        }
    }

    @Test
    @DisplayName("a drain event racing a withdrawal never leaves a running runtime with drain items")
    void drainEventRacingWithdrawalRestoresNoItem() throws Exception {
        var items = drainItems(Generators.integers(2, 4).next());
        var stateId = drainStateId();
        registerSystemState(stateId, drainingFacts(items, THEN_EXIT), List.of());
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        var rounds = Generators.integers(60, 120).next();
        var lateItem = Generators.letterStrings(5, 10).next();

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            for (var round = 0; round < rounds; round++) {
                engine.goToState(stateId);
                var start = new CountDownLatch(1);
                Future<?> event = executor.submit(() -> {
                    start.await();
                    engine.broadcast(EVENT_DRAINING, Map.of(
                            "lifecycle_state", "draining",
                            "remaining_items", List.of(lateItem),
                            "remaining_work_count", 1));
                    return null;
                });
                Future<?> withdraw = executor.submit(() -> {
                    start.await();
                    return send("DELETE", PATH_STOP, auth, new byte[0], false).status();
                });
                start.countDown();
                awaitBounded(event, "the drain event");
                awaitBounded(withdraw, "the withdrawal");

                assertEquals(RuntimeLifecycleState.RUNNING, engine.lifecycleState(), "round " + round);
                assertTrue(remainingItems(engine, auth).isEmpty(),
                        "round " + round + ": a withdrawn drain runs nothing, whatever event raced it");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    @DisplayName("after a jump to a running state a reconnecting stream replays no drain frame but every other event")
    void jumpDropsBufferedDrainFrames() throws Exception {
        var items = drainItems(Generators.integers(2, 4).next());
        var stateId = drainStateId();
        registerSystemState(stateId, drainingFacts(items, THEN_EXIT), List.of());
        var anchor = markerEvent();
        var unrelated = markerEvent();
        var boundary = markerEvent();
        var latches = Map.of(unrelated, new CountDownLatch(1));
        var replayLatches = Map.of(boundary, new CountDownLatch(1));

        try (var runtime = startRuntime()) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var bearer = "Bearer " + runtime.token();
            var live = runtime.engine();
            String anchorId;
            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", bearer))) {
                var events = new CopyOnWriteArrayList<SseEvent>();
                readFullEvents(exchange.events(), events, latches);
                live.goToState(stateId);
                live.broadcast(anchor, Map.of());
                live.broadcast(EVENT_DRAINING, Map.of(
                        "lifecycle_state", "draining",
                        "remaining_items", items,
                        "remaining_work_count", items.size()));
                live.broadcast(unrelated, Map.of());
                assertTrue(latches.get(unrelated).await(5, TimeUnit.SECONDS), "the events arrive: " + events);
                anchorId = events.stream().filter(event -> anchor.equals(event.event())).findFirst()
                        .orElseThrow().id();
                assertTrue(events.stream().anyMatch(event -> EVENT_DRAINING.equals(event.event())),
                        "the drain frame was delivered and buffered: " + events);
            }

            live.goToState(STATE_SYSTEM_RUNNING);

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", bearer)
                         .withHeader("Last-Event-ID", anchorId))) {
                var replayed = new CopyOnWriteArrayList<SseEvent>();
                readFullEvents(exchange.events(), replayed, replayLatches);
                live.broadcast(boundary, Map.of());
                assertTrue(replayLatches.get(boundary).await(5, TimeUnit.SECONDS), "the boundary arrives: " + replayed);
                var names = replayed.stream().map(SseEvent::event).toList();
                var beforeBoundary = names.subList(0, names.indexOf(boundary));
                assertAll(
                        () -> assertTrue(beforeBoundary.contains(unrelated),
                                "an unrelated buffered event is still replayed: " + names),
                        () -> assertFalse(beforeBoundary.contains(EVENT_DRAINING),
                                "a drain of the state left must not be replayed: " + names));
            }
        }
    }

    @Test
    @DisplayName("a drain event that leaves no item ends the drain through the lifecycle transition")
    void emptyDrainEventEndsTheDrain() {
        var items = drainItems(Generators.integers(2, 4).next());
        var stateId = drainStateId();
        registerSystemState(stateId, drainingFacts(items, THEN_EXIT), List.of());
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(stateId);
        assertEquals(RuntimeLifecycleState.DRAINING, engine.lifecycleState());

        engine.broadcast(EVENT_DRAINING, Map.of(
                "lifecycle_state", "draining",
                "remaining_items", List.of(),
                "remaining_work_count", 0));

        assertAll(
                () -> assertEquals(RuntimeLifecycleState.STOPPED, engine.lifecycleState(),
                        "a drain with nothing left ends as its last step does"),
                () -> assertTrue(remainingItems(engine, auth).isEmpty()));
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    @DisplayName("a scenario drain event of a withdrawn drain does not overwrite the items of the replacement drain")
    void scheduledDrainEventOfWithdrawnDrainIsDropped() throws Exception {
        var items = drainItems(Generators.integers(2, 4).next());
        var staleItem = Generators.letterStrings(5, 10).next() + "-stale";
        var stateId = drainStateId();
        var marker = markerEvent();
        var afterMs = Generators.integers(0, 20).next();
        registerSystemState(stateId, drainingFacts(items, THEN_EXIT), List.of(
                new EventDefinition(afterMs, EVENT_DRAINING, Map.of(
                        "lifecycle_state", "draining",
                        "remaining_items", List.of(staleItem),
                        "remaining_work_count", 1)),
                new EventDefinition(afterMs + 1, marker, Map.of())));
        var latches = Map.of(marker, new CountDownLatch(1));
        // The engine's scheduler is held until the drain is withdrawn and replaced, so the stale
        // event is due only after it, without waiting for a delay.
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        var release = holdScheduler(scheduler);

        try (var runtime = startRuntime(scheduler)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var bearer = Map.of("Authorization", "Bearer " + runtime.token());
            try (var exchange = client.open(HttpRequest.get("/api/v1/events")
                         .withHeader("Authorization", "Bearer " + runtime.token()))) {
                var names = new CopyOnWriteArrayList<String>();
                readEvents(exchange.events(), names, latches);
                var live = runtime.engine();
                live.goToState(stateId);

                assertEquals(200, send(live, "DELETE", PATH_STOP, bearer, new byte[0], false).status());
                var restarted = send(live, "POST", PATH_STOP, bearer, drainBody(THEN_EXIT), false);
                assertEquals(200, restarted.status(), restarted.body());
                release.countDown();

                assertTrue(latches.get(marker).await(5, TimeUnit.SECONDS), "the state's events are due: " + names);
                assertFalse(remainingItems(live, bearer).contains(staleItem),
                        "the withdrawn drain's event must not overwrite the replacement drain");
            }
        } finally {
            // a start that failed before the engine took the scheduler must not leave it held
            release.countDown();
            scheduler.shutdownNow();
        }
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    @DisplayName("under ordered timing a reset keeps the start state's events: they follow the reset and replay after it")
    void orderedStartEventsSurviveReset() throws Exception {
        assertStartEventsFollowTheReset(MockRuntimeEngine::reset);
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    @DisplayName("under ordered timing a scenario switch keeps the start state's events: they follow the reset and replay after it")
    void orderedStartEventsSurviveSwitch() throws Exception {
        assertStartEventsFollowTheReset(live -> live.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM));
    }

    @Test
    @DisplayName("refuses two seeded chats about one subject instead of keeping the later one")
    void refusesTwoChatsAboutOneSubject() throws Exception {
        var first = firstSeededChat();
        var twin = new ChatResource("C-" + Generators.letterStrings(4, 8).next(), first.subject(), first.role(),
                first.scope(), first.state(), first.context(), first.proposal(), first.turns(), first.before());

        assertChatSeedsRefused(twin, first.subject().id());
    }

    @Test
    @DisplayName("refuses two seeded chats with one id instead of keeping both under one id")
    void refusesTwoChatsWithOneId() throws Exception {
        var first = firstSeededChat();
        var other = new ChatResource(first.id(),
                new ChatSubject(first.subject().kind(), Generators.letterStrings(4, 10).next().toLowerCase()),
                first.role(), first.scope(), first.state(), first.context(), first.proposal(), first.turns(),
                first.before());

        assertChatSeedsRefused(other, first.id());
    }

    @Test
    @DisplayName("refuses an ended plan and an ended epic with one id instead of keeping the later one")
    void refusesEndedWorkNamedTwice() {
        var before = engine.currentScenario().name();
        var reference = StandardScenarios.PROJECTS_AND_START + "#start.follow-up";
        var scenario = registry.resolve(reference);
        var ended = new ArrayList<Object>(Objects.requireNonNull(JsonTree.asArray(
                member(runtimeFixture(scenario, scenario.start()), "start").get("ended_work"))));
        var twin = new LinkedHashMap<String, Object>(Objects.requireNonNull(JsonTree.asObject(ended.getFirst())));
        var subjectId = String.valueOf(twin.get("subject_id"));
        twin.put("subject_kind", "plan".equals(twin.get("subject_kind")) ? "epic" : "plan");
        ended.add(twin);
        registry.register(withStartRuntimeFixture(reference, runtime -> {
            var start = member(runtime, "start");
            start.put("ended_work", ended);
            runtime.put("start", start);
        }));

        var failure = assertThrows(IllegalStateException.class, () -> engine.switchScenario(brokenName));

        assertAll(
                () -> assertTrue(failure.getMessage().contains("facts.runtime.start.ended_work"), failure.getMessage()),
                () -> assertTrue(failure.getMessage().contains(subjectId), failure.getMessage()),
                () -> assertEquals(before, engine.currentScenario().name(), "the refused switch changes nothing"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"without subject_id", "without subject_kind", "with an unrecognized subject_kind",
            "with a non-object item"})
    @DisplayName("refuses a non-object ended item or one without subject id or known kind, at start and at a jump")
    void refusesMalformedEndedWork(String defect) {
        var before = engine.currentScenario().name();
        var reference = StandardScenarios.PROJECTS_AND_START + "#start.follow-up";
        var scenario = registry.resolve(reference);
        var ended = new ArrayList<Object>(Objects.requireNonNull(JsonTree.asArray(
                member(runtimeFixture(scenario, scenario.start()), "start").get("ended_work"))));
        ended.set(0, malformedEndedItem(defect, Objects.requireNonNull(JsonTree.asObject(ended.getFirst()))));
        var broken = withStartRuntimeFixture(reference, runtime -> {
            var start = member(runtime, "start");
            start.put("ended_work", ended);
            runtime.put("start", start);
        });
        registry.register(broken);
        var jumpName = "jump-" + brokenName;
        registry.register(new Scenario(broken.formatVersion(), jumpName, broken.description(),
                registry.resolve(StandardScenarios.PROJECTS_AND_START).start(), broken.pairingCode(), broken.routes(),
                broken.states()));

        var atStart = assertThrows(IllegalStateException.class, () -> engine.switchScenario(brokenName));
        var unchanged = engine.currentScenario().name();
        engine.switchScenario(jumpName);
        var jumpTarget = broken.start();
        var atJump = assertThrows(IllegalStateException.class, () -> engine.goToState(jumpTarget));

        var expected = "Malformed facts.runtime.start.ended_work: each item needs a subject_id and a recognized "
                + "subject_kind";
        assertAll(
                () -> assertEquals(expected, atStart.getMessage()),
                () -> assertEquals(expected, atJump.getMessage()),
                () -> assertEquals(before, unchanged, "the refused switch changes nothing"));
    }

    /**
     * @param defect   the defect the ended item carries, as named by {@link #refusesMalformedEndedWork(String)}
     * @param original a well-formed ended item
     * @return the ended item with the defect: a copy without a member, with an unknown kind, or no object
     */
    private static Object malformedEndedItem(String defect, Map<String, Object> original) {
        var malformed = new LinkedHashMap<String, Object>(original);
        switch (defect) {
            case "without subject_id" -> malformed.remove("subject_id");
            case "without subject_kind" -> malformed.remove("subject_kind");
            case "with a non-object item" -> {
                return Generators.integers().next();
            }
            default -> malformed.put("subject_kind", "unknown-" + Generators.letterStrings(4, 10).next());
        }
        return malformed;
    }

    /**
     * Starts a runtime under ordered timing whose start state plays one event, opens the runtime's
     * stream, restarts the scenario and proves the event arrives after the restart's {@code reset} and
     * is replayed to a client that reconnects with the id of that {@code reset}.
     *
     * @param restart what restarts the scenario: a reset or a scenario switch
     */
    private void assertStartEventsFollowTheReset(Consumer<MockRuntimeEngine> restart) throws Exception {
        var stateId = "start." + Generators.letterStrings(4, 8).next().toLowerCase();
        var marker = markerEvent();
        registerSystemStart(stateId, List.of(new EventDefinition(Generators.integers(0, 500).next(), marker, Map.of())));
        var reset = EventName.RESET.wire();
        var latches = Map.of(marker, new CountDownLatch(1));

        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                     .registry(registry)
                     .scenario(StandardScenarios.REPORTS_AND_SYSTEM)
                     .timing(MockTiming.ORDERED)
                     .build())) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var bearer = "Bearer " + runtime.token();
            String resetId;
            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", bearer))) {
                var events = new CopyOnWriteArrayList<SseEvent>();
                readFullEvents(exchange.events(), events, latches);

                restart.accept(runtime.engine());

                assertTrue(latches.get(marker).await(5, TimeUnit.SECONDS), "the start state's event arrives: " + events);
                var names = events.stream().map(SseEvent::event).toList();
                assertTrue(names.contains(reset) && names.indexOf(reset) < names.indexOf(marker),
                        "the reset precedes the start state's events: " + names);
                resetId = events.get(names.indexOf(reset)).id();
            }

            var replayLatches = Map.of(marker, new CountDownLatch(1));
            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", bearer)
                         .withHeader("Last-Event-ID", resetId))) {
                var replayed = new CopyOnWriteArrayList<SseEvent>();
                readFullEvents(exchange.events(), replayed, replayLatches);
                assertTrue(replayLatches.get(marker).await(5, TimeUnit.SECONDS),
                        "a client that reconnects after the reset replays the start state's events: " + replayed);
            }
        }
    }

    /**
     * @return the first chat the state {@code control.chat} seeds, a chat about the plan
     *         {@code fix-relay}
     */
    private ChatResource firstSeededChat() throws IOException {
        var chats = seededChats();
        assertFalse(chats.isEmpty(), "the state seeds a chat");
        return ChatResource.fromMap(Objects.requireNonNull(JsonTree.asObject(chats.getFirst())));
    }

    /**
     * @return the chats the state {@code control.chat} seeds: its own, or those of the scenario's base
     *         state {@code running_work.active} when it declares none
     */
    private List<Object> seededChats() {
        var scenario = registry.resolve(CHAT_STATE);
        var own = JsonTree.asArray(member(runtimeFixture(scenario, scenario.start()), "control").get("chats"));
        var chats = own != null ? own
                : JsonTree.asArray(member(runtimeFixture(scenario, "running_work.active"), "control").get("chats"));
        return chats == null ? List.of() : chats;
    }

    /**
     * @return a copy of the runtime fixture of a state's own operator facts
     */
    private static Map<String, Object> runtimeFixture(Scenario scenario, String stateId) {
        var representation = Objects.requireNonNull(JsonTree.asObject(
                Objects.requireNonNull(scenario.state(stateId)).representation()));
        return member(member(representation, "facts"), "runtime");
    }

    /**
     * Copies a scenario as one named {@link #brokenName} whose start state's runtime fixture is
     * changed: the start state's own fixture, not the scenario's base one.
     *
     * @param reference the scenario, with the state to start in after {@code #}
     * @param change    changes the copy of the start state's runtime fixture in place
     * @return the changed scenario
     */
    private Scenario withStartRuntimeFixture(String reference, Consumer<Map<String, Object>> change) {
        var base = registry.resolve(reference);
        var state = Objects.requireNonNull(base.state(base.start()));
        var representation = new LinkedHashMap<String, Object>(
                Objects.requireNonNull(JsonTree.asObject(state.representation())));
        var facts = member(representation, "facts");
        var runtime = member(facts, "runtime");
        change.accept(runtime);
        facts.put("runtime", runtime);
        representation.put("facts", facts);
        var states = new LinkedHashMap<>(base.states());
        states.put(base.start(), new StateDefinition(representation, state.links(), state.events(), state.auto()));
        return new Scenario(base.formatVersion(), brokenName, base.description(), base.start(), base.pairingCode(),
                base.routes(), states);
    }

    /**
     * @return a copy of the object member of a map, empty when the map has none
     */
    private static Map<String, Object> member(Map<String, Object> map, String key) {
        var copy = new LinkedHashMap<String, Object>();
        if (map.get(key) instanceof Map<?, ?> value) {
            value.forEach((name, item) -> copy.put(String.valueOf(name), item));
        }
        return copy;
    }

    /**
     * Registers the reports-and-system scenario starting in one more state: the state
     * {@code system.running} with the given scheduled events.
     *
     * @param stateId the new start state
     * @param events  the state's own scheduled events
     */
    private void registerSystemStart(String stateId, List<EventDefinition> events) {
        var base = registry.resolve(StandardScenarios.REPORTS_AND_SYSTEM);
        var running = Objects.requireNonNull(base.state(STATE_SYSTEM_RUNNING));
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(running.representation(), running.links(), events, null));
        registry.register(new Scenario(base.formatVersion(), base.name(), base.description(), stateId,
                base.pairingCode(), base.routes(), states));
    }

    /**
     * Registers the state {@code control.chat} with one more seeded chat as a scenario of its own and
     * proves that switching to it is refused with the fixture path and the duplicate named, and changes
     * nothing.
     *
     * @param extra    the chat added to the state's own
     * @param expected what the refusal names besides the fixture path
     */
    private void assertChatSeedsRefused(ChatResource extra, String expected) {
        var before = engine.currentScenario().name();
        var chats = new ArrayList<Object>(seededChats());
        chats.add(extra.toMap());
        registry.register(withStartRuntimeFixture(CHAT_STATE, runtime -> {
            var control = member(runtime, "control");
            control.put("chats", chats);
            runtime.put("control", control);
        }));

        var failure = assertThrows(IllegalStateException.class, () -> engine.switchScenario(brokenName));

        assertAll(
                () -> assertTrue(failure.getMessage().contains("facts.runtime.control.chats"), failure.getMessage()),
                () -> assertTrue(failure.getMessage().contains(expected), failure.getMessage()),
                () -> assertEquals(before, engine.currentScenario().name(), "the refused switch changes nothing"));
    }

    @Test
    @DisplayName("a repeated stop during a drain of a signal keeps the drain non-withdrawable")
    void repeatedStopKeepsSignalDrainNonWithdrawable() throws Exception {
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(STATE_DRAINING_SIGNAL);

        var repeated = send("POST", PATH_STOP, auth, drainBody(THEN_EXIT), false);
        var withdraw = send("DELETE", PATH_STOP, auth, new byte[0], false);

        assertAll(
                () -> assertEquals(200, repeated.status(), repeated.body()),
                () -> assertEquals(Boolean.FALSE, repeated.json().get("can_withdraw")),
                () -> assertEquals(409, withdraw.status(), withdraw.body()),
                () -> assertEquals("not_withdrawable", withdraw.json().get("error")),
                () -> assertEquals(RuntimeLifecycleState.DRAINING, engine.lifecycleState()));
    }

    @Test
    @DisplayName("a cleanup delete after an exit drain ended the runtime is rejected and the job stays open")
    void cleanupDeleteAfterExitDrainIsRejected() throws Exception {
        var stateId = drainStateId();
        registerSystemState(stateId, drainingFacts(drainItems(1), THEN_EXIT), List.of());
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(stateId);
        engine.shrinkDrain();
        assertEquals(RuntimeLifecycleState.STOPPED, engine.lifecycleState());

        var delete = send("POST", "/api/v1/system/cleanup/cln-0012/delete", auth, new byte[0], false);
        var cleanup = send("GET", "/api/v1/system/cleanup", auth, new byte[0], false);

        assertAll(
                () -> assertEquals(409, delete.status(), delete.body()),
                // A stopped runtime does not offer the deletion: the catalogue's act_not_available.
                () -> assertEquals("act_not_available", delete.json().get("error")),
                () -> assertEquals(200, cleanup.status(), cleanup.body()),
                () -> assertNotEquals(Boolean.FALSE, cleanup.json().get("open"), "the job stays open"));
    }

    @Test
    @DisplayName("the connection status reports onboarded as the served status says, not the state's runtime fixture")
    void connectionStatusAgreesWithTheServedStatus() throws Exception {
        // reports-and-system declares its status in the route (machine.onboarded true); the state's
        // runtime fixture is applied to the status in the onboarding scenario only.
        var stateId = drainStateId();
        var running = Objects.requireNonNull(registry.resolve(StandardScenarios.REPORTS_AND_SYSTEM)
                .state(STATE_SYSTEM_RUNNING));
        var runtimeFixture = new LinkedHashMap<String, Object>(Objects.requireNonNull(JsonTree.object(
                JsonTree.object(JsonTree.asObject(running.representation()), "facts"), "runtime")));
        runtimeFixture.put("onboarded", false);
        registerSystemState(stateId, Map.of("runtime", runtimeFixture), List.of());
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(stateId);

        var status = send("GET", "/api/v1/status", auth, new byte[0], false);
        var connection = send("GET", "/api/v1/status/connection", auth, new byte[0], false);

        assertEquals(200, status.status(), status.body());
        assertEquals(200, connection.status(), connection.body());
        var served = RuntimeEntryResource.fromMap(status.json()).onboarded();
        assertTrue(served, "the route declares an onboarded machine");
        assertEquals(served, connection.json().get("onboarded"),
                "the connection status and the status a client reads agree");
    }

    @Test
    @DisplayName("a stop now in a state that offers no act stops the runtime and leaves the scenario where it is")
    void stopNowInAStateWithoutActsStaysInTheState() throws Exception {
        var stateId = drainStateId();
        var base = registry.resolve(StandardScenarios.REPORTS_AND_SYSTEM);
        var running = Objects.requireNonNull(base.state(STATE_SYSTEM_RUNNING));
        var representation = new LinkedHashMap<String, Object>(
                Objects.requireNonNull(JsonTree.asObject(running.representation())));
        representation.remove("links");
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, Map.of(), List.of(), null));
        registry.register(new Scenario(base.formatVersion(), base.name(), base.description(), base.start(),
                base.pairingCode(), base.routes(), states));
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(stateId);

        var stop = send("POST", PATH_STOP, auth,
                JsonTree.write(Map.of("when", "now")).getBytes(StandardCharsets.UTF_8), false);

        assertAll(
                () -> assertEquals(200, stop.status(), stop.body()),
                () -> assertEquals("stopping", stop.json().get("state")),
                () -> assertEquals(RuntimeLifecycleState.STOPPING, engine.lifecycleState()),
                () -> assertEquals(stateId, engine.currentStateId(), "no act names a state to move to"));
    }

    @Test
    @DisplayName("a drain asked on the web names the web app, and a repeated drain keeps who started it")
    void repeatedDrainKeepsWhoStartedIt() throws Exception {
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(STATE_SYSTEM_RUNNING);
        var web = Map.of("Authorization", "Bearer " + deviceSecret);

        var first = send("POST", PATH_STOP, web, drainBody(THEN_EXIT), true);
        var repeated = send("POST", PATH_STOP, auth, drainBody(THEN_EXIT), false);

        assertAll(
                () -> assertEquals(200, first.status(), first.body()),
                () -> assertEquals("the web app", first.json().get("draining_by")),
                () -> assertEquals(200, repeated.status(), repeated.body()),
                () -> assertEquals("the web app", repeated.json().get("draining_by"),
                        "the console repeating the drain does not take it over"),
                () -> assertEquals(Boolean.TRUE, repeated.json().get("can_withdraw")));
    }

    @Test
    @DisplayName("a stop that names no when drains, and a drain that asks for a restart runs again after its last item")
    void stopWithoutWhenDrainsAndRestartsAfterItsLastItem() throws Exception {
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(STATE_SYSTEM_RUNNING);

        var stop = send("POST", PATH_STOP, auth,
                JsonTree.write(Map.of("then", "restart")).getBytes(StandardCharsets.UTF_8), false);
        assertEquals(200, stop.status(), stop.body());
        assertEquals("draining", stop.json().get("state"), "the default of when is drain");
        var items = remainingItems(engine, auth).size();
        assertTrue(items > 0, "a drain runs at least one item");

        for (var step = 1; step < items; step++) {
            engine.shrinkDrain();
        }
        assertEquals(RuntimeLifecycleState.DRAINING, engine.lifecycleState(), "one item left still drains");
        engine.shrinkDrain();

        assertEquals(RuntimeLifecycleState.RUNNING, engine.lifecycleState(), "a restart runs again");
    }

    @Test
    @DisplayName("a cleanup delete on the web keeps the items left out as kept by the web app and ignores an id the job does not list")
    void webCleanupDeleteKeepsTheItemsLeftOut() throws Exception {
        engine.switchScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        engine.goToState(STATE_SYSTEM_RUNNING);
        var unknown = "gone-" + Generators.letterStrings(4, 8).next().toLowerCase();
        var body = JsonTree.write(Map.of("unchecked_ids", List.of("token-usage", unknown)))
                .getBytes(StandardCharsets.UTF_8);

        var delete = send("POST", "/api/v1/system/cleanup/cln-0012/delete",
                Map.of("Authorization", "Bearer " + deviceSecret), body, true);
        var kept = send("GET", "/api/v1/system/cleanup/kept", auth, new byte[0], false);

        assertEquals(200, delete.status(), delete.body());
        assertEquals(1872127584L, JsonTree.number(delete.json(), "freed_bytes").longValue(),
                "the item left out is not freed, the unknown id frees nothing");
        assertEquals(200, kept.status(), kept.body());
        var keptItems = Objects.requireNonNull(JsonTree.asArray(JsonTree.parse(kept.body()))).stream()
                .map(JsonTree::asObject).toList();
        assertEquals(1, keptItems.stream()
                .filter(item -> "token-usage".equals(item.get("id")) && "the web app".equals(item.get("who")))
                .count(), "the item left out is kept, by the web app: " + keptItems);
        assertTrue(keptItems.stream().noneMatch(item -> unknown.equals(item.get("id"))),
                "an id the job does not list is not kept: " + keptItems);
    }

    @Test
    @DisplayName("a project's detail orders its epics when it lists no plans, and a project sub-resource is served as declared")
    void projectDetailWithEpicsOnlyIsOrdered() throws Exception {
        var scenarioName = Generators.letterStrings(4, 10).next().toLowerCase();
        var projectId = Generators.letterStrings(4, 10).next().toLowerCase();
        var note = Generators.letterStrings(4, 10).next();
        var ended = Map.<String, Object>of("epic_id", "ended-" + projectId, "phase_state", "ended");
        var quiet = Map.<String, Object>of("epic_id", "quiet-" + projectId,
                "running_since", "2026-03-01T10:00:00Z");
        var waiting = Map.<String, Object>of("epic_id", "waiting-" + projectId,
                "running_since", "2026-01-01T10:00:00Z", "needs_operator_count", 1);
        var state = new StateDefinition(Map.of("message", "start"), Map.of(), List.of(), null);
        registry.register(new Scenario(1, scenarioName, "Project detail order", "start", null,
                Map.of("GET /api/v1/projects/" + projectId, new RouteDefinition(
                                Map.of("project_id", projectId, "epics", List.of(ended, quiet, waiting)), 200),
                        "GET /api/v1/projects/" + projectId + "/notes", new RouteDefinition(
                                Map.of("note", note), 200)),
                Map.of("start", state)));
        engine.switchScenario(scenarioName);

        var detail = send("GET", "/api/v1/projects/" + projectId, auth, new byte[0], false);
        var notes = send("GET", "/api/v1/projects/" + projectId + "/notes", auth, new byte[0], false);

        assertEquals(200, detail.status(), detail.body());
        var epics = Objects.requireNonNull(JsonTree.asArray(detail.json().get("epics"))).stream()
                .map(epic -> JsonTree.string(JsonTree.asObject(epic), "epic_id")).toList();
        assertEquals(List.of("waiting-" + projectId, "quiet-" + projectId, "ended-" + projectId), epics,
                "what needs the operator first, an ended epic last");
        assertEquals(List.of(), detail.json().get("plans"), "the detail always lists its plans");
        assertEquals(200, notes.status(), notes.body());
        assertEquals(Map.of("note", note), notes.json(), "a sub-resource is not reshaped into a detail");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"recycle", "grant", "add_credential", "raise_budget", "give_up_removal"})
    @DisplayName("a hold resolved by one of its releasing acts is cleared and its plan goes on")
    void releasingActClearsTheHold(String act) throws Exception {
        var holdId = "H-" + Generators.integers(2000, 9999).next();
        var stateId = "control." + Generators.letterStrings(4, 8).next().toLowerCase();
        var base = registry.resolve(StandardScenarios.RUNNING_WORK);
        var holds = Objects.requireNonNull(base.state("control.holds"));
        var representation = new LinkedHashMap<String, Object>(
                Objects.requireNonNull(JsonTree.asObject(holds.representation())));
        var facts = new LinkedHashMap<String, Object>(Objects.requireNonNull(JsonTree.object(representation, "facts")));
        var runtimeFixture = new LinkedHashMap<String, Object>(Objects.requireNonNull(JsonTree.object(facts, "runtime")));
        var control = new LinkedHashMap<String, Object>(Objects.requireNonNull(JsonTree.object(runtimeFixture, "control")));
        control.put("holds", List.of(Map.of(
                "hold_id", holdId,
                "subject_id", "fix-relay",
                "subject_kind", "plan",
                "kind", "condition",
                "cause", "waits for the operator",
                "attempts", 0,
                "acts", List.of(act, "end_plan"))));
        runtimeFixture.put("control", control);
        facts.put("runtime", runtimeFixture);
        representation.put("facts", facts);
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, holds.links(), List.of(), null));
        registry.register(new Scenario(base.formatVersion(), base.name(), base.description(), base.start(),
                base.pairingCode(), base.routes(), states));
        engine.switchScenario(StandardScenarios.RUNNING_WORK);
        engine.goToState(stateId);
        var path = "/api/v1/plans/fix-relay/holds/" + holdId + "/resolve";
        var body = JsonTree.write(Map.of("act", act)).getBytes(StandardCharsets.UTF_8);

        var resolved = send("POST", path, auth, body, false);
        var again = send("POST", path, auth, body, false);

        assertAll(
                () -> assertEquals(200, resolved.status(), resolved.body()),
                () -> assertEquals(Boolean.TRUE, resolved.json().get("released"), act + " releases the hold"),
                () -> assertEquals(404, again.status(), "a released hold is gone: " + again.body()),
                () -> assertEquals("target_not_found", again.json().get("error")));
    }

    private static final String PATH_STOP = "/api/v1/operator/runtime/stop";
    private static final String STATE_SYSTEM_RUNNING = "system.running";
    /** The state that seeds the chats, among them the chat about the plan {@code fix-relay}. */
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String STATE_DRAINING_SIGNAL = "system.draining-signal";
    private static final String EVENT_DECISION_CLOSED = "decision.closed";
    private static final String EVENT_DRAINING = "runtime.draining";
    private static final String THEN_EXIT = "exit";

    /**
     * Forces the interleaving a lock-order inversion between a stop and a reset deadlocks on: the stop
     * holds {@code systemLock} while its answer waits on a slow client, a reset takes the engine
     * monitor and waits on {@code systemLock}, then the client reads on. Under the single lock order
     * the stop needs nothing beyond {@code systemLock} after that point and both end; a stop that takes
     * the engine monitor after {@code systemLock}, as the drain's step scheduling did before the single
     * lock order, never ends. Both run on platform threads so their lock waits can be observed.
     *
     * @param drain the body of the stop
     * @return the status the stop answered
     */
    private int forceStopHoldingSystemLockAgainstReset(byte[] drain) throws Exception {
        var subject = engine;
        var client = new SlowClient();
        var request = new MockHttpRequest("POST", PATH_STOP, auth, drain, System.nanoTime());
        var stop = new FutureTask<>(() -> {
            subject.dispatch(request, new MockHttpResponse(client), false);
            return parseStatus(client.text());
        });
        var stopThread = Thread.ofPlatform().daemon().name("stop").start(stop);
        assertTrue(client.awaitWriting(), "the stop must reach its answer");

        var reset = new FutureTask<>(() -> {
            subject.reset();
            subject.goToState(STATE_SYSTEM_RUNNING);
            return null;
        });
        var resetThread = Thread.ofPlatform().daemon().name("reset").start(reset);
        awaitBlockedBy(resetThread, stopThread);
        client.readOn();

        if (!endsWithinBound(stop) || !endsWithinBound(reset)) {
            // A deadlocked engine holds its monitor for good and cannot be closed; tear down a fresh one.
            engine = new MockRuntimeEngine(registry, registry.resolve(StandardScenarios.STATUS_ONLY),
                    runtimeToken, deviceSecret);
            fail("the stop and the reset did not end within the bound: they deadlocked");
        }
        return stop.get();
    }

    /**
     * Waits until {@code waiter} is blocked on a lock {@code owner} holds.
     */
    private static void awaitBlockedBy(Thread waiter, Thread owner) {
        var threads = ManagementFactory.getThreadMXBean();
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (true) {
            var info = threads.getThreadInfo(waiter.threadId());
            if (info != null && info.getThreadState() == Thread.State.BLOCKED
                    && info.getLockOwnerId() == owner.threadId()) {
                return;
            }
            if (System.nanoTime() > deadline) {
                fail(waiter.getName() + " never waited on a lock " + owner.getName() + " holds");
            }
            Thread.onSpinWait();
        }
    }

    private static boolean endsWithinBound(Future<?> task) throws Exception {
        try {
            task.get(20, TimeUnit.SECONDS);
            return true;
        } catch (TimeoutException _) {
            return false;
        }
    }

    /**
     * A client that takes an answer only once the test lets it read on, as a slow client's socket
     * does: the writer waits inside its first write until then.
     */
    private static final class SlowClient extends OutputStream {

        private final ByteArrayOutputStream received = new ByteArrayOutputStream();
        private final CountDownLatch writing = new CountDownLatch(1);
        private final CountDownLatch reading = new CountDownLatch(1);

        @Override
        public void write(int value) throws IOException {
            write(new byte[]{(byte) value}, 0, 1);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            writing.countDown();
            try {
                if (!reading.await(20, TimeUnit.SECONDS)) {
                    throw new InterruptedIOException("the client never read on");
                }
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("interrupted while the client waited to read");
            }
            received.write(bytes, offset, length);
        }

        boolean awaitWriting() throws InterruptedException {
            return writing.await(20, TimeUnit.SECONDS);
        }

        void readOn() {
            reading.countDown();
        }

        String text() {
            return received.toString(StandardCharsets.UTF_8);
        }
    }

    private static void awaitBounded(Future<?> task, String what) throws Exception {
        try {
            task.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException _) {
            fail(what + " did not end within the bound: the stop and the reset deadlocked");
        }
    }

    private static byte[] drainBody(String then) {
        return JsonTree.write(Map.of("when", "drain", "then", then)).getBytes(StandardCharsets.UTF_8);
    }

    private static List<String> drainItems(int count) {
        var items = new ArrayList<String>();
        for (var index = 0; index < count; index++) {
            items.add(Generators.letterStrings(4, 10).next() + "-" + index);
        }
        return List.copyOf(items);
    }

    private static String drainStateId() {
        return "drain." + Generators.letterStrings(4, 8).next().toLowerCase();
    }

    private static String markerEvent() {
        return "mock.marker." + Generators.letterStrings(4, 8).next().toLowerCase();
    }

    private static Map<String, Object> drainingFacts(List<String> items, String then) {
        return Map.of(
                "lifecycle_state", "draining",
                "draining_by", "the console",
                "can_withdraw_stop", true,
                "then", then,
                "blockers", items.stream().map(item -> Map.of("name", item)).toList());
    }

    /**
     * Registers the reports-and-system scenario with one more state: the facts of
     * {@code system.running} overlaid with the given facts, and the given scheduled events.
     *
     * @param stateId the new state
     * @param facts   the facts overlaid on those of {@code system.running}
     * @param events  the state's own scheduled events
     */
    private void registerSystemState(String stateId, Map<String, Object> facts, List<EventDefinition> events) {
        var base = registry.resolve(StandardScenarios.REPORTS_AND_SYSTEM);
        var running = Objects.requireNonNull(base.state(STATE_SYSTEM_RUNNING));
        var representation = new LinkedHashMap<String, Object>(
                Objects.requireNonNull(JsonTree.asObject(running.representation())));
        var mergedFacts = new LinkedHashMap<String, Object>();
        var baseFacts = JsonTree.asObject(representation.get("facts"));
        if (baseFacts != null) {
            mergedFacts.putAll(baseFacts);
        }
        mergedFacts.putAll(facts);
        representation.put("facts", mergedFacts);
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, running.links(), events, null));
        registry.register(new Scenario(base.formatVersion(), base.name(), base.description(), base.start(),
                base.pairingCode(), base.routes(), states));
    }

    private MockRuntime startRuntime() throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder()
                .registry(registry)
                .scenario(StandardScenarios.REPORTS_AND_SYSTEM)
                .build());
    }

    private MockRuntime startRuntime(ScheduledExecutorService scheduler) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder()
                .registry(registry)
                .scenario(StandardScenarios.REPORTS_AND_SYSTEM)
                .scheduler(scheduler)
                .build());
    }

    /**
     * Holds the one thread of {@code scheduler}: every task scheduled after this call runs only once
     * the returned latch is counted down, whatever its delay.
     */
    private static CountDownLatch holdScheduler(ScheduledExecutorService scheduler) {
        var release = new CountDownLatch(1);
        scheduler.submit(() -> {
            release.await();
            return null;
        });
        return release;
    }

    private static List<Object> remainingItems(MockRuntimeEngine target, Map<String, String> headers)
            throws IOException {
        var status = send(target, "GET", "/api/v1/status", headers, new byte[0], false);
        assertEquals(200, status.status(), status.body());
        var items = JsonTree.asArray(status.json().get("drain_remaining_items"));
        return items == null ? List.of() : items;
    }

    private static String decisionOpenedBody(String decisionId) {
        return """
                {
                  "event": "decision.opened",
                  "data": {
                    "id": "%s",
                    "question": {
                      "revision": 1,
                      "title": "Broadcast question",
                      "header": "Broadcast",
                      "subject": "plan broadcast-plan",
                      "scope": "plan",
                      "asking_phase": "execute",
                      "asking_role": "implement",
                      "blocks": "plan",
                      "reversibility": "reversible",
                      "description": "A question opened through the control endpoint.",
                      "parts": []
                    }
                  }
                }
                """.formatted(decisionId);
    }

    private static Object parseJson(String text) {
        try {
            return JsonTree.parse(text);
        } catch (IOException e) {
            throw new AssertionError("not JSON: " + text, e);
        }
    }

    /**
     * Reads the events of a stream on a daemon thread, opening each latch when its event arrives.
     *
     * @param sse     the stream
     * @param events  receives the events in arrival order
     * @param latches the latch per awaited event name
     */
    private static void readFullEvents(SseReader sse, List<SseEvent> events, Map<String, CountDownLatch> latches) {
        var reader = new Thread(() -> {
            try {
                SseEvent event;
                while ((event = sse.next()) != null) {
                    events.add(event);
                    var latch = latches.get(String.valueOf(event.event()));
                    if (latch != null) {
                        latch.countDown();
                    }
                }
            } catch (IOException _) {
                // the stream closed
            }
        });
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * Reads the event names of a stream on a daemon thread, opening each latch when its event arrives.
     *
     * @param sse     the stream
     * @param names   receives the event names in arrival order
     * @param latches the latch per awaited event name
     */
    private static void readEvents(SseReader sse, List<String> names, Map<String, CountDownLatch> latches) {
        var reader = new Thread(() -> {
            try {
                SseEvent event;
                while ((event = sse.next()) != null) {
                    var name = String.valueOf(event.event());
                    names.add(name);
                    var latch = latches.get(name);
                    if (latch != null) {
                        latch.countDown();
                    }
                }
            } catch (IOException _) {
                // the stream closed
            }
        });
        reader.setDaemon(true);
        reader.start();
    }
}
