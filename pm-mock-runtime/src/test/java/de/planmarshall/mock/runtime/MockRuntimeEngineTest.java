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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.LinkDefinition;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

@DisplayName("MockRuntimeEngine")
class MockRuntimeEngineTest {

    private ScenarioRegistry registry;
    private MockRuntimeEngine engine;

    @BeforeEach
    void setUp() {
        registry = new ScenarioRegistry();
        var initialScenario = registry.resolve(StandardScenarios.STATUS_ONLY);
        engine = new MockRuntimeEngine(registry, initialScenario, "test-token", "test-secret");
    }

    @AfterEach
    void tearDown() {
        engine.close();
    }

    private record TestResponse(int status, String body, Map<String, Object> json) {
    }

    private TestResponse send(String method, String target, Map<String, String> headers, byte[] body, boolean isWeb) throws IOException {
        var req = new MockHttpRequest(method, target, headers, body, System.nanoTime());
        var out = new ByteArrayOutputStream();
        var res = new MockHttpResponse(out);
        engine.dispatch(req, res, isWeb);

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
        assertEquals(StandardScenarios.STATUS_ONLY, engine.currentScenario().name());
        assertEquals("runtime.idle", engine.currentStateId());
        assertEquals("test-token", engine.runtimeToken());
        assertEquals("test-secret", engine.deviceSecret());
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
        assertEquals(StandardScenarios.STATUS_ONLY, res.json().get("scenario"));
        assertEquals("runtime.idle", res.json().get("state"));
        assertEquals("MOCKMOCK", res.json().get("pairing_code"));
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
        assertEquals(StandardScenarios.WEB_PAIRING, res1.json().get("scenario"));
        assertEquals("web.pair", engine.currentStateId());

        // 2. Switch via query parameter ?name=...
        var res2 = send("POST", "/mock/scenario?name=" + StandardScenarios.STATUS_ONLY, Map.of(), new byte[0], false);
        assertEquals(200, res2.status());
        assertEquals(StandardScenarios.STATUS_ONLY, res2.json().get("scenario"));

        // 3. Switch via query parameter ?scenario=...
        var res3 = send("POST", "/mock/scenario?scenario=" + StandardScenarios.WEB_PAIRING, Map.of(), new byte[0], false);
        assertEquals(200, res3.status());
        assertEquals(StandardScenarios.WEB_PAIRING, res3.json().get("scenario"));

        // 4. Missing scenario name -> 400
        var res4 = send("POST", "/mock/scenario", Map.of(), new byte[0], false);
        assertEquals(400, res4.status());
        assertEquals("missing_scenario_name", res4.json().get("error"));

        // 5. Unknown scenario name -> 404
        var res5 = send("POST", "/mock/scenario?name=unknown-scenario", Map.of(), new byte[0], false);
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

        var res2 = send("POST", "/mock/goto/non-existent-state", Map.of(), new byte[0], false);
        assertEquals(404, res2.status());
        assertEquals("state_not_found", res2.json().get("error"));
    }

    @Test
    @DisplayName("control endpoint: POST /mock/broadcast broadcasts event")
    void controlBroadcast() throws Exception {
        var body = "{\"event\":\"test-evt\",\"data\":{\"foo\":\"bar\"}}".getBytes(StandardCharsets.UTF_8);
        var res = send("POST", "/mock/broadcast", Map.of(), body, false);
        assertEquals(200, res.status());
        assertEquals("broadcasted", res.json().get("status"));
        assertEquals("test-evt", res.json().get("event"));
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
        assertEquals("test-secret", res1.json().get("device_secret"));
        assertEquals("mock-device-1", res1.json().get("device_id"));

        // 2. A fresh code in the query parameter (case and dash insensitive); the redeemed code is single use
        var pairings = send("POST", "/api/v1/web/pairings",
                Map.of("Authorization", "Bearer test-token"), new byte[0], false);
        assertEquals(201, pairings.status());
        var freshCode = String.valueOf(pairings.json().get("code"));
        var res2 = send("POST", "/pair?code=" + freshCode.toLowerCase().replace("-", ""),
                Map.of(), new byte[0], true);
        assertEquals(200, res2.status());
        assertEquals("test-secret", res2.json().get("device_secret"));

        // 3. Pairing with mismatched code -> 400
        var res3 = send("POST", "/pair?code=wrong", Map.of(), new byte[0], true);
        assertEquals(400, res3.status());
        assertEquals("pairing_invalid", res3.json().get("error"));
    }

    @Test
    @DisplayName("web authentication: protected paths require Bearer deviceSecret")
    void webProtectedAuthentication() throws Exception {
        // Missing Authorization -> 401
        var res1 = send("GET", "/api/v1/status", Map.of(), new byte[0], true);
        assertEquals(401, res1.status());
        assertEquals("unauthorized", res1.json().get("error"));

        // Wrong Authorization -> 401
        var res2 = send("GET", "/api/v1/status", Map.of("Authorization", "Bearer wrong"), new byte[0], true);
        assertEquals(401, res2.status());

        // Correct Authorization -> accepted
        var res3 = send("GET", "/api/v1/status", Map.of("Authorization", "Bearer test-secret"), new byte[0], true);
        assertEquals(200, res3.status());
    }

    @Test
    @DisplayName("unix authentication: requires runtime token or job token on /mcp")
    void unixAuthentication() throws Exception {
        // Missing Authorization -> 401
        var res1 = send("GET", "/api/v1/status", Map.of(), new byte[0], false);
        assertEquals(401, res1.status());
        assertEquals("unauthorized", res1.json().get("error"));

        // Valid runtime token -> 200
        var res2 = send("GET", "/api/v1/status", Map.of("Authorization", "Bearer test-token"), new byte[0], false);
        assertEquals(200, res2.status());

        // Job token on /mcp path -> accepted
        var res3 = send("GET", "/mcp/job-status", Map.of("PM-MCP-Job-Token", "some-job-token"), new byte[0], false);
        // Returns 404 mock_no_route because path has no route, but authentication passed (not 401)
        assertEquals(404, res3.status());
        assertEquals("mock_no_route", res3.json().get("error"));
    }

    @Test
    @DisplayName("handles mock_no_route for unmatched requests with offered_links")
    void mockNoRoute() throws Exception {
        var res = send("GET", "/api/v1/unmatched-path", Map.of("Authorization", "Bearer test-token"), new byte[0], false);
        assertEquals(404, res.status());
        assertNotNull(res.json());
        assertEquals("mock_no_route", res.json().get("error"));
        assertEquals("runtime.idle", res.json().get("state"));
        assertNotNull(res.json().get("offered_links"));
    }

    @Test
    @DisplayName("matches state link, advances state, and returns next representation")
    void stateLinkTransition() throws Exception {
        engine.switchScenario(StandardScenarios.ONBOARDING_HAPPY);
        assertEquals("onboard.mode", engine.currentStateId());

        // Send POST to /api/v1/operator/onboard/begin with begin link
        var res = send("POST", "/api/v1/operator/onboard/begin", Map.of("Authorization", "Bearer test-token"), new byte[0], false);
        assertEquals(200, res.status());
        assertEquals("language.choose", engine.currentStateId());
    }

    @Test
    @DisplayName("handles link with refusal status >= 400")
    void errorLinkRefusal() throws Exception {
        var errorLink = new LinkDefinition("start", "POST", "/api/v1/fail", 0, 409, "locked_keychain");
        var stateWithRefusal = new StateDefinition(
                Map.of("message", "blocked"),
                Map.of("fail", errorLink),
                List.of(),
                null
        );
        var testScenario = new Scenario(1, "test-refusal", "Refusal test", "start", null,
                Map.of(), Map.of("start", stateWithRefusal));

        registry.register(testScenario);
        engine.switchScenario("test-refusal");

        var res = send("POST", "/api/v1/fail", Map.of("Authorization", "Bearer test-token"), new byte[0], false);
        assertEquals(409, res.status());
        assertEquals("locked_keychain", res.json().get("error"));
        assertEquals("start", res.json().get("state"));
        assertEquals("fail", res.json().get("link"));
    }

    @Test
    @DisplayName("handles link with execution delay")
    void delayedLink() throws Exception {
        var delayedLink = new LinkDefinition("next", "POST", "/api/v1/delayed", 20, 200, null);
        var startState = new StateDefinition(
                Map.of("message", "start"),
                Map.of("delayed", delayedLink),
                List.of(),
                null
        );
        var nextState = new StateDefinition(Map.of("message", "done"), Map.of(), List.of(), null);
        var testScenario = new Scenario(1, "test-delay", "Delay test", "start", null,
                Map.of(), Map.of("start", startState, "next", nextState));

        registry.register(testScenario);
        engine.switchScenario("test-delay");

        var startNanos = System.nanoTime();
        var res = send("POST", "/api/v1/delayed", Map.of("Authorization", "Bearer test-token"), new byte[0], false);
        var elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertEquals(200, res.status());
        assertTrue(elapsedMs >= 15, "Expected elapsed time to reflect delay: " + elapsedMs + "ms");
        assertEquals("next", engine.currentStateId());
    }

    @Test
    @DisplayName("matches static routes from scenario")
    void staticRouteMatching() throws Exception {
        engine.switchScenario(StandardScenarios.ONBOARDING_HARNESS_NOT_LOGGED_IN);
        assertEquals("harness.choose", engine.currentStateId());

        // GET /api/v1/status is a static route in this scenario
        var res = send("GET", "/api/v1/status", Map.of("Authorization", "Bearer test-token"), new byte[0], false);
        assertEquals(200, res.status());
        assertNotNull(res.json());
        // State remains harness.choose
        assertEquals("harness.choose", engine.currentStateId());
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
    }

    @Test
    @DisplayName("manages paired devices and web listener routes")
    void webDevicesRoutes() throws Exception {
        var auth = Map.of("Authorization", "Bearer test-token");

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
        var renameBody = "{\"label\":\"Primary Laptop\"}".getBytes(StandardCharsets.UTF_8);
        var renameRes = send("PATCH", "/api/v1/web/devices/dev-1", auth, renameBody, false);
        assertEquals(200, renameRes.status());
        assertEquals("Primary Laptop", renameRes.json().get("label"));

        // 4. PATCH /api/v1/web/devices/1 (by index)
        var indexRename = "{\"label\":\"Index Laptop\"}".getBytes(StandardCharsets.UTF_8);
        var indexRes = send("PATCH", "/api/v1/web/devices/1", auth, indexRename, false);
        assertEquals(200, indexRes.status());

        // 5. PATCH with blank label -> 400
        var blankBody = "{\"label\":\"\"}".getBytes(StandardCharsets.UTF_8);
        var blankRes = send("PATCH", "/api/v1/web/devices/dev-1", auth, blankBody, false);
        assertEquals(400, blankRes.status());
        assertEquals("invalid_label", blankRes.json().get("error"));

        // 6. PATCH non-existent -> 404
        var notFoundRes = send("PATCH", "/api/v1/web/devices/non-existent", auth, renameBody, false);
        assertEquals(404, notFoundRes.status());
        assertEquals("device_not_found", notFoundRes.json().get("error"));

        // 7. DELETE /api/v1/web/devices/dev-2 (single)
        var delRes = send("DELETE", "/api/v1/web/devices/dev-2", auth, new byte[0], false);
        assertEquals(200, delRes.status());
        assertEquals("revoked", delRes.json().get("status"));

        // 8. DELETE non-existent single -> 404
        var delNotFound = send("DELETE", "/api/v1/web/devices/non-existent", auth, new byte[0], false);
        assertEquals(404, delNotFound.status());

        // 9. DELETE /api/v1/web/devices?others (on web)
        var webAuth = Map.of("Authorization", "Bearer test-secret");
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
}
