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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import de.planmarshall.api.runtime.Authenticator;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

@DisplayName("MockRuntime")
@EnabledOnOs({OS.MAC, OS.LINUX})
class MockRuntimeTest {

    @Test
    @DisplayName("initializes filesystem layout with strict POSIX permissions and cleans up on close")
    void lifecycleAndFilesystem() throws Exception {
        var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY);
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

            assertEquals(StandardScenarios.ONBOARDING, runtime.currentScenarioName());
            assertEquals("status.only", runtime.currentStateName());
            assertEquals(runtime.base().toString(), runtime.environment().get(MachinePaths.ENV_BASE));
        }

        // Base directory and parent should be deleted
        assertFalse(Files.exists(base), "base directory must be cleaned up on close");
    }

    @Test
    @DisplayName("enforces Bearer token authentication on Unix domain socket")
    void unixAuthentication() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
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
        try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_HARNESS_NOT_LOGGED_IN)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals("harness.not-logged-in", runtime.currentStateName());

            // 1. Initial representation on static /api/v1/status route
            var statusRes = client.send(HttpRequest.get("/api/v1/status").withHeader("Authorization", auth));
            assertEquals(200, statusRes.status());
            var statusJson = JsonTree.asObject(JsonTree.parse(statusRes.body()));
            assertEquals("onboarding", JsonTree.string(statusJson, "status"));

            // 2. Follow offered link "check": POST /api/v1/onboarding/harness/check
            var checkRes = client.send(HttpRequest.json("POST", "/api/v1/onboarding/harness/check", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(200, checkRes.status());
            var checkJson = JsonTree.asObject(JsonTree.parse(checkRes.body()));
            assertEquals("harness.probe",
                    JsonTree.string(JsonTree.object(checkJson, "state"), "state_id"));
            assertEquals("harness.probe", runtime.currentStateName());

            // 3. The link the left state offered is refused act_not_available in harness.probe
            var secondCheck = client.send(HttpRequest.json("POST", "/api/v1/onboarding/harness/check", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(409, secondCheck.status());
            var errorJson = JsonTree.asObject(JsonTree.parse(secondCheck.body()));
            assertEquals("act_not_available", JsonTree.string(errorJson, "error"));
            assertEquals("harness.probe", JsonTree.string(errorJson, "state"));
        }
    }

    @Test
    @DisplayName("supports name-based scenario switching programmatically and via /mock/scenario")
    void scenarioSwitching() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_HAPPY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());

            assertEquals(StandardScenarios.ONBOARDING, runtime.currentScenarioName());
            assertEquals("onboard.mode", runtime.currentStateName());

            // Programmatic switch to status-only
            runtime.switchScenario(StandardScenarios.STATUS_ONLY);
            assertEquals(StandardScenarios.ONBOARDING, runtime.currentScenarioName());
            assertEquals("status.only", runtime.currentStateName());
            assertEquals("KXQB-MRTN", runtime.pairingCode());

            // Control endpoint: GET /mock/state
            var stateRes = client.send(HttpRequest.get("/mock/state"));
            assertEquals(200, stateRes.status());
            var stateJson = JsonTree.asObject(JsonTree.parse(stateRes.body()));
            assertEquals(StandardScenarios.ONBOARDING, JsonTree.string(stateJson, "scenario"));
            assertEquals("status.only", JsonTree.string(stateJson, "state"));
            assertEquals("KXQB-MRTN", JsonTree.string(stateJson, "pairing_code"));

            // Verify scenario switching control endpoint
            var switchRes = client.send(HttpRequest.json("POST", "/mock/scenario", "{\"scenario\":\"" + StandardScenarios.WEB_PAIRING + "\"}"));
            assertEquals(200, switchRes.status());
            var switchJson = JsonTree.asObject(JsonTree.parse(switchRes.body()));
            assertEquals("switched", JsonTree.string(switchJson, "status"));
            assertEquals(StandardScenarios.ONBOARDING, JsonTree.string(switchJson, "scenario"));
            assertEquals("web.pair", runtime.currentStateName());
            assertEquals("KXQB-MRTN", runtime.pairingCode());

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
        try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_HAPPY)) {
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
        try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_HAPPY)) {
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
                assertEquals("check.result", event.event());
                var payload = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("version", JsonTree.string(payload, "check_id"));
                assertEquals("pass", JsonTree.string(payload, "result"));
            }
        }
    }

    @Test
    @DisplayName("supports web pairing and device secret authentication over loopback TCP")
    void webPairingAndTcp() throws Exception {
        var config = MockRuntimeConfig.builder()
                .scenario(StandardScenarios.WEB_PAIRING)
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
    @DisplayName("keeps the web device secret across restarts on a stable base")
    void deviceSecretPersistsAcrossRestarts() throws Exception {
        var tempBase = Files.createTempDirectory(Path.of(System.getProperty("java.io.tmpdir")), "pms");
        try {
            String firstSecret;
            var config = MockRuntimeConfig.builder()
                    .scenario(StandardScenarios.WEB_PAIRING)
                    .base(tempBase)
                    .cleanupBaseOnClose(false)
                    .webPort(0)
                    .build();
            try (var runtime = MockRuntime.start(config)) {
                firstSecret = runtime.deviceSecret();
                var secretFile = tempBase.resolve("state").resolve(MockRuntime.DEVICE_SECRET_FILE);
                assertTrue(Files.exists(secretFile), "the device secret must be stored on a stable base");
                assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(secretFile)));
            }

            try (var restarted = MockRuntime.start(config)) {
                assertEquals(firstSecret, restarted.deviceSecret(), "a restart must reuse the stored secret");

                // A browser that paired before the restart keeps its access with that secret.
                var authReq = java.net.http.HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + restarted.webPort() + "/api/v1/status"))
                        .header("Authorization", "Bearer " + firstSecret)
                        .GET()
                        .build();
                var authRes = HttpClient.newHttpClient().send(authReq, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, authRes.statusCode());
            }
        } finally {
            deleteTree(tempBase);
        }
    }

    @Test
    @DisplayName("an explicitly configured device secret overrides the stored one")
    void explicitDeviceSecretOverridesStoredOne() throws Exception {
        var tempBase = Files.createTempDirectory(Path.of(System.getProperty("java.io.tmpdir")), "pme");
        try {
            String storedSecret;
            try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                         .scenario(StandardScenarios.WEB_PAIRING)
                         .base(tempBase)
                         .cleanupBaseOnClose(false)
                         .webPort(0)
                         .build())) {
                storedSecret = runtime.deviceSecret();
            }

            try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                         .scenario(StandardScenarios.WEB_PAIRING)
                         .base(tempBase)
                         .cleanupBaseOnClose(false)
                         .webPort(0)
                         .deviceSecret("pmw_explicit_override")
                         .build())) {
                assertEquals("pmw_explicit_override", runtime.deviceSecret());
                assertNotEquals(storedSecret, runtime.deviceSecret());

                var authReq = java.net.http.HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + runtime.webPort() + "/api/v1/status"))
                        .header("Authorization", "Bearer pmw_explicit_override")
                        .GET()
                        .build();
                var authRes = HttpClient.newHttpClient().send(authReq, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, authRes.statusCode());
            }
        } finally {
            deleteTree(tempBase);
        }
    }

    @Test
    @DisplayName("regenerates an empty stored device secret")
    void emptyStoredSecretIsRegenerated() throws Exception {
        var tempBase = Files.createTempDirectory(Path.of(System.getProperty("java.io.tmpdir")), "pmz");
        try {
            try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                    .scenario(StandardScenarios.WEB_PAIRING)
                    .base(tempBase)
                    .cleanupBaseOnClose(false)
                    .build())) {
                assertFalse(runtime.deviceSecret().isBlank());
            }
            var secretFile = tempBase.resolve("state").resolve(MockRuntime.DEVICE_SECRET_FILE);
            Files.writeString(secretFile, "  \n");

            try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                    .scenario(StandardScenarios.WEB_PAIRING)
                    .base(tempBase)
                    .cleanupBaseOnClose(false)
                    .build())) {
                assertFalse(runtime.deviceSecret().isBlank());
                assertEquals(runtime.deviceSecret(), Files.readString(secretFile).trim());
                assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(secretFile)));
            }
        } finally {
            deleteTree(tempBase);
        }
    }

    /**
     * Deletes a temporary base with everything below it.
     *
     * @param directory the directory to remove
     */
    private static void deleteTree(Path directory) {        if (!Files.exists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            for (var path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException _) {
            // Best effort: the operating system reclaims the temporary tree eventually.
        }
    }

    @Test
    @DisplayName("programmatic facade methods provide full runtime introspection and control")
    void programmaticFacade() throws Exception {
        var tempBase = Files.createTempDirectory(Path.of("/tmp"), "pm-mb");
        try {
            var config = MockRuntimeConfig.builder()
                    .scenario(StandardScenarios.STATUS_ONLY)
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

                assertEquals(StandardScenarios.ONBOARDING, runtime.currentScenarioName());
                assertEquals("status.only", runtime.currentStateName());
                assertNotNull(runtime.currentScenario());

                runtime.goToState("status.only");
                assertEquals("status.only", runtime.currentStateName());

                runtime.switchScenario(StandardScenarios.ONBOARDING_HAPPY);
                assertEquals(StandardScenarios.ONBOARDING, runtime.currentScenarioName());
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

    @Test
    @DisplayName("creates configured RuntimeClient instances targeting the Unix domain socket")
    void clientCreation() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
            var clientDefault = runtime.client();
            assertNotNull(clientDefault);
            assertTrue(clientDefault.isLive());

            var clientWithHeaders = runtime.client(Map.of("X-Trace", "123"));
            assertNotNull(clientWithHeaders);
            assertTrue(clientWithHeaders.isLive());

            var field = RuntimeClient.class.getDeclaredField("authenticator");
            field.setAccessible(true);
            var auth = (Authenticator) field.get(clientWithHeaders);
            assertNotNull(auth.credential());
            assertFalse(auth.renew());
        }
    }
}
