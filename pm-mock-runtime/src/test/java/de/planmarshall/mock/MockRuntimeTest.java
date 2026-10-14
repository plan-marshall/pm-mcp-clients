/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

@DisplayName("MockRuntime")
@EnabledOnOs({OS.MAC, OS.LINUX})
class MockRuntimeTest {

    @Test
    @DisplayName("initializes filesystem layout with strict POSIX permissions and cleans up on close")
    void lifecycleAndFilesystem() throws Exception {
        var runtime = MockRuntime.start("status-only");
        var base = runtime.base();
        var paths = runtime.paths();

        try (runtime) {
            assertTrue(Files.exists(base), "base must exist");
            assertTrue(Files.exists(paths.runDir()), "run dir must exist");
            assertTrue(Files.exists(paths.stateDir()), "state dir must exist");
            assertTrue(Files.exists(paths.runtimeToken()), "runtime.token must exist");
            assertTrue(Files.exists(paths.runtimeRecord()), "runtime.json must exist");
            assertTrue(Files.exists(paths.socket()), "socket must exist");

            // Verify permissions
            assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(paths.runDir())));
            assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(paths.stateDir())));
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(paths.runtimeToken())));
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(paths.runtimeRecord())));

            assertEquals("status-only", runtime.currentScenarioName());
            assertEquals("runtime.idle", runtime.currentStateName());
            assertEquals(runtime.base().toString(), runtime.environment().get(MachinePaths.ENV_BASE));
        }

        // Base directory and parent should be deleted
        assertFalse(Files.exists(base), "base directory must be cleaned up on close");
    }

    @Test
    @DisplayName("enforces Bearer token authentication on Unix domain socket")
    void unixAuthentication() throws Exception {
        try (var runtime = MockRuntime.start("status-only")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());

            // 1. Unauthenticated request -> 401
            var unauthResponse = client.send(HttpRequest.get("/api/v1/status"));
            assertEquals(401, unauthResponse.status());
            var unauthBody = JsonTree.asObject(JsonTree.parse(unauthResponse.body()));
            assertEquals("unauthorized", JsonTree.string(unauthBody, "error"));

            // 2. Request with invalid token -> 401
            var badResponse = client.send(HttpRequest.get("/api/v1/status")
                    .withHeader("Authorization", "Bearer invalid-token"));
            assertEquals(401, badResponse.status());

            // 3. Request with valid token -> 200
            var validResponse = client.send(HttpRequest.get("/api/v1/status")
                    .withHeader("Authorization", "Bearer " + runtime.token()));
            assertEquals(200, validResponse.status());
            var body = JsonTree.asObject(JsonTree.parse(validResponse.body()));
            assertEquals("ready", JsonTree.string(body, "status"));
        }
    }

    @Test
    @DisplayName("handles hypermedia state transitions and reports mock_no_route on unmatched links")
    void hypermediaTransitions() throws Exception {
        try (var runtime = MockRuntime.start("onboarding-harness-not-logged-in")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals("harness.choose", runtime.currentStateName());

            // 1. Initial representation on static /api/v1/status route
            var statusRes = client.send(HttpRequest.get("/api/v1/status").withHeader("Authorization", auth));
            assertEquals(200, statusRes.status());
            var statusJson = JsonTree.asObject(JsonTree.parse(statusRes.body()));
            assertEquals("onboarding", JsonTree.string(statusJson, "status"));

            // 2. Follow offered link "check": POST /api/v1/operator/onboard/harness/check
            var checkRes = client.send(HttpRequest.json("POST", "/api/v1/operator/onboard/harness/check", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(200, checkRes.status());
            var checkJson = JsonTree.asObject(JsonTree.parse(checkRes.body()));
            assertEquals("harness.probe", JsonTree.string(checkJson, "state"));
            assertEquals("harness.probe", runtime.currentStateName());

            // 3. Try to POST /api/v1/operator/onboard/harness/check again -> 404 mock_no_route because harness.probe has no links!
            var secondCheck = client.send(HttpRequest.json("POST", "/api/v1/operator/onboard/harness/check", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(404, secondCheck.status());
            var errorJson = JsonTree.asObject(JsonTree.parse(secondCheck.body()));
            assertEquals("mock_no_route", JsonTree.string(errorJson, "error"));
            assertEquals("harness.probe", JsonTree.string(errorJson, "state"));
            var offered = JsonTree.asArray(errorJson.get("offered_links"));
            assertNotNull(offered);
            assertTrue(offered.isEmpty());
        }
    }

    @Test
    @DisplayName("supports name-based scenario switching programmatically and via /mock/scenario")
    void scenarioSwitching() throws Exception {
        try (var runtime = MockRuntime.start("onboarding-happy")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());

            assertEquals("onboarding-happy", runtime.currentScenarioName());
            assertEquals("onboard.mode", runtime.currentStateName());

            // Programmatic switch to status-only
            runtime.switchScenario("status-only");
            assertEquals("status-only", runtime.currentScenarioName());
            assertEquals("runtime.idle", runtime.currentStateName());

            // Control endpoint: GET /mock/state
            var stateRes = client.send(HttpRequest.get("/mock/state"));
            assertEquals(200, stateRes.status());
            var stateJson = JsonTree.asObject(JsonTree.parse(stateRes.body()));
            assertEquals("status-only", JsonTree.string(stateJson, "scenario"));
            assertEquals("runtime.idle", JsonTree.string(stateJson, "state"));

            // Verify scenario switching control endpoint
            var switchRes = client.send(HttpRequest.json("POST", "/mock/scenario", "{\"scenario\":\"web-pairing\"}"));
            assertEquals(200, switchRes.status());
            var switchJson = JsonTree.asObject(JsonTree.parse(switchRes.body()));
            assertEquals("switched", JsonTree.string(switchJson, "status"));
            assertEquals("web-pairing", JsonTree.string(switchJson, "scenario"));
            assertEquals("web.pair", runtime.currentStateName());

            // Verify jump to state control endpoint
            var gotoPost = client.send(HttpRequest.json("POST", "/mock/goto/web.paired", "{}"));
            assertEquals(200, gotoPost.status());
            assertEquals("web.paired", runtime.currentStateName());

            // Verify reset control endpoint
            var resetRes = client.send(HttpRequest.json("POST", "/mock/reset", "{}"));
            assertEquals(200, resetRes.status());
            assertEquals("web.pair", runtime.currentStateName());
        }
    }

    @Test
    @DisplayName("handles auto-transitions after configured delay")
    void autoTransitions() throws Exception {
        try (var runtime = MockRuntime.start("onboarding-happy")) {
            assertEquals("onboard.mode", runtime.currentStateName());

            // Jump to state 'harness.probe' which has auto-transition to 'harness.confirm' after 250ms
            runtime.goToState("harness.probe");
            assertEquals("harness.probe", runtime.currentStateName());

            // Wait for auto-transition to complete
            var deadline = System.currentTimeMillis() + 2000;
            while (!"harness.confirm".equals(runtime.currentStateName()) && System.currentTimeMillis() < deadline) {
                LockSupport.parkNanos(10_000_000L);
            }
            assertEquals("harness.confirm", runtime.currentStateName());
        }
    }

    @Test
    @DisplayName("delivers scheduled Server-Sent Events to subscribers")
    void serverSentEvents() throws Exception {
        try (var runtime = MockRuntime.start("onboarding-happy")) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events")
                         .withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                // Trigger entry into state 'harness.probe' which schedules event 'check' after 100ms
                runtime.goToState("harness.probe");

                var event = sse.next();
                assertNotNull(event, "Must receive scheduled event");
                assertEquals("check", event.event());
                var payload = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("Claude Code", JsonTree.string(payload, "harness"));
                assertEquals("pass", JsonTree.string(payload, "status"));
            }
        }
    }

    @Test
    @DisplayName("supports web pairing and device secret authentication over loopback TCP")
    void webPairingAndTcp() throws Exception {
        var config = MockRuntimeConfig.builder()
                .scenario("web-pairing")
                .webPort(0) // Ephemeral TCP port
                .build();

        try (var runtime = MockRuntime.start(config)) {
            assertTrue(runtime.webPort() > 0, "Loopback TCP port should be allocated");
            var baseUrl = "http://127.0.0.1:" + runtime.webPort();
            var httpClient = HttpClient.newHttpClient();

            // 1. Attempt unauthenticated access on web port -> 401
            var unauthReq = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/status"))
                    .GET()
                    .build();
            var unauthRes = httpClient.send(unauthReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(401, unauthRes.statusCode());

            // 2. Attempt pairing with wrong code -> 400
            var badPairReq = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/pair"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"code\":\"wrong-code\"}"))
                    .build();
            var badPairRes = httpClient.send(badPairReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(400, badPairRes.statusCode());

            // 3. Successful pairing with code 'KXQB-MRTN' -> 200 and device_secret
            var pairReq = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/pair"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"code\":\"KXQB-MRTN\"}"))
                    .build();
            var pairRes = httpClient.send(pairReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, pairRes.statusCode());
            var pairJson = JsonTree.asObject(JsonTree.parse(pairRes.body()));
            var deviceSecret = JsonTree.string(pairJson, "device_secret");
            assertNotNull(deviceSecret);
            assertEquals(runtime.deviceSecret(), deviceSecret);

            // 4. Access protected endpoint with device secret Bearer auth -> 200
            var authReq = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/status"))
                    .header("Authorization", "Bearer " + deviceSecret)
                    .GET()
                    .build();
            var authRes = httpClient.send(authReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, authRes.statusCode());
        }
    }

    @Test
    @DisplayName("programmatic facade methods provide full runtime introspection and control")
    void programmaticFacade() throws Exception {
        var tempBase = Files.createTempDirectory(Path.of("/tmp"), "pm-mb");
        try {
            var config = MockRuntimeConfig.builder()
                    .scenario("status-only")
                    .base(tempBase)
                    .cleanupBaseOnClose(false)
                    .build();

            try (var runtime = MockRuntime.start(config)) {
                assertEquals(tempBase, runtime.base());
                assertNotNull(runtime.paths());
                assertEquals(runtime.paths().socket(), runtime.socketPath());
                assertNotNull(runtime.token());
                assertNotNull(runtime.deviceSecret());
                assertEquals(-1, runtime.webPort());
                assertTrue(runtime.environment().containsKey(MachinePaths.ENV_BASE));

                assertEquals("status-only", runtime.currentScenarioName());
                assertEquals("runtime.idle", runtime.currentStateName());
                assertNotNull(runtime.currentScenario());

                runtime.goToState("runtime.idle");
                assertEquals("runtime.idle", runtime.currentStateName());

                runtime.switchScenario("onboarding-happy");
                assertEquals("onboarding-happy", runtime.currentScenarioName());
                assertEquals("onboard.mode", runtime.currentStateName());

                runtime.reset();
                assertEquals("onboard.mode", runtime.currentStateName());

                var awaited = runtime.awaitRequest(Duration.ofMillis(10));
                assertTrue(awaited == null || awaited.receivedNanos() > 0);
            }
        } finally {
            try (var walk = Files.walk(tempBase)) {
                for (var p : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    @Test
    @DisplayName("starts successfully with in-memory programmatic Scenario and custom ScenarioRegistry")
    void startWithProgrammaticScenario() throws Exception {
        var states = Map.of("initial", new StateDefinition(
                Map.of("custom_key", "custom_val"),
                Map.of(),
                List.of(),
                null
        ));
        var dynamicScenario = new Scenario(1, "synthetic-scenario", "Synthetic", "initial", null, Map.of(), states);
        var registry = new ScenarioRegistry();
        var config = MockRuntimeConfig.builder()
                .registry(registry)
                .initialScenario(dynamicScenario)
                .build();

        try (var runtime = MockRuntime.start(config)) {
            assertEquals("synthetic-scenario", runtime.currentScenarioName());
            assertEquals("initial", runtime.currentStateName());
            assertNotNull(runtime.currentScenario());
        }
    }
}
