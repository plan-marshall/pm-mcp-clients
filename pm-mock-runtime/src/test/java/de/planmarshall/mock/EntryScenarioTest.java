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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.planmarshall.api.entry.ConnectionStatus;
import de.planmarshall.api.entry.DeviceListResource;
import de.planmarshall.api.entry.DeviceRenamedEvent;
import de.planmarshall.api.entry.DeviceRevokedEvent;
import de.planmarshall.api.entry.ListenerChangedEvent;
import de.planmarshall.api.entry.ListenerKind;
import de.planmarshall.api.entry.ListenerResource;
import de.planmarshall.api.entry.ListenerState;
import de.planmarshall.api.entry.PairingResource;
import de.planmarshall.api.entry.RuntimeEntryResource;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;

@DisplayName("EntryScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class EntryScenarioTest {

    private static final String DEVICE_SECRET = "test-secret";

    @Test
    @DisplayName("loads the entry scenario and registers it among the standard scenarios")
    void shouldLoadEntryScenario() throws Exception {
        assertTrue(StandardScenarios.ALL.contains(StandardScenarios.ENTRY));

        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY)) {
            assertEquals(StandardScenarios.ENTRY, runtime.currentScenarioName());
            assertEquals("entry.pair", runtime.currentStateName());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entryCases")
    @DisplayName("loads every entry case scenario and serves its runtime status")
    void shouldLoadEveryCaseScenario(String scenario) throws Exception {
        try (var runtime = MockRuntime.start(scenario)) {
            assertEquals(scenario, runtime.currentScenarioName());

            var client = new UnixSocketHttpClient(runtime.socketPath());
            var res = client.send(HttpRequest.get("/api/v1/status")
                    .withHeader("Authorization", "Bearer " + runtime.token()));
            assertEquals(200, res.status());
            var entry = RuntimeEntryResource.fromJson(new String(res.body(), StandardCharsets.UTF_8));
            assertNotNull(entry.onboarding());
            assertEquals(2, entry.listeners().size());
        }
    }

    @Test
    @DisplayName("serves the connection status with both listeners and the device count")
    void shouldServeConnectionStatus() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/status/connection").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var status = ConnectionStatus.fromJson(new String(res.body(), StandardCharsets.UTF_8));
            assertTrue(status.loopbackOpen());
            assertEquals("http://127.0.0.1:7420", status.loopbackUrl());
            assertTrue(status.lanEnabled());
            assertNotNull(status.lanFingerprint());
            assertEquals(3, status.pairedDevicesCount());
            assertFalse(status.onboarded(), "the entry scenario shows a machine not onboarded yet");
        }
    }

    @Test
    @DisplayName("reports the onboarding progress and both listeners in the runtime status")
    void shouldReportOnboardingProgressAndListeners() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY_ONBOARDING)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());

            var res = client.send(HttpRequest.get("/api/v1/status")
                    .withHeader("Authorization", "Bearer " + runtime.token()));
            var entry = RuntimeEntryResource.fromJson(new String(res.body(), StandardCharsets.UTF_8));

            assertFalse(entry.onboarded());
            assertEquals("harness.choose", entry.onboarding().state());
            assertEquals(2, entry.onboarding().step());
            assertEquals(7, entry.onboarding().steps());
            assertEquals("mock-machine", entry.host());
            assertEquals(ListenerKind.LOOPBACK, entry.listeners().getFirst().listener());
            assertEquals(ListenerState.OPEN, entry.listeners().getFirst().state());
            assertEquals(ListenerKind.LAN, entry.listeners().get(1).listener());
            assertEquals(ListenerState.CLOSED, entry.listeners().get(1).state());
            assertEquals(7421, entry.listeners().get(1).port());
        }
    }

    @Test
    @DisplayName("shows the onboarding not started on the un-onboarded machine")
    void shouldShowOnboardingNotStarted() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY_UNONBOARDED)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());

            var res = client.send(HttpRequest.get("/api/v1/status")
                    .withHeader("Authorization", "Bearer " + runtime.token()));
            var entry = RuntimeEntryResource.fromJson(new String(res.body(), StandardCharsets.UTF_8));

            assertEquals("onboard.none", entry.onboarding().state());
            assertEquals(0, entry.onboarding().step());
            assertFalse(entry.onboarded());
            assertEquals(0, entry.pairedDevicesCount());
            assertEquals(ListenerState.CLOSED, lanListener(entry).state());
        }
    }

    @Test
    @DisplayName("refuses issuing a pairing code and opening the network listener on a web listener as cli_only")
    void shouldRefuseCliOnlyActsOnWeb() throws Exception {
        try (var runtime = webRuntime(StandardScenarios.ENTRY)) {
            var baseUrl = "http://127.0.0.1:" + runtime.webPort();

            var pairingResp = post(baseUrl + "/api/v1/web/pairings", "{}");
            assertEquals(403, pairingResp.statusCode());
            assertEquals("cli_only", JsonTree.string(JsonTree.asObject(JsonTree.parse(pairingResp.body())), "error"));

            var lanResp = put(baseUrl + "/api/v1/web/listeners/lan", "{}");
            assertEquals(403, lanResp.statusCode());
            assertEquals("cli_only", JsonTree.string(JsonTree.asObject(JsonTree.parse(lanResp.body())), "error"));
        }
    }

    @Test
    @DisplayName("opens the network listener over the local socket, broadcasts listener.changed, and refuses a colliding port")
    void shouldOpenLanListener() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var conflict = client.send(HttpRequest.json("PUT", "/api/v1/web/listeners/lan", "{\"port\":7420}")
                        .withHeader("Authorization", auth));
                assertEquals(409, conflict.status());
                assertEquals("web_listener_conflict",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(conflict.body())), "error"));

                var occupied = sse.next();
                assertNotNull(occupied);
                assertEquals(ListenerChangedEvent.EVENT_NAME, occupied.event());
                var occupiedEvent = ListenerChangedEvent.fromJson(occupied.data());
                assertEquals(ListenerKind.LAN, occupiedEvent.listener());
                assertEquals(ListenerState.PORT_OCCUPIED, occupiedEvent.state());
                assertEquals(7420, occupiedEvent.port());

                var res = client.send(HttpRequest.json("PUT", "/api/v1/web/listeners/lan", "{\"port\":7421}")
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());

                var changed = sse.next();
                assertNotNull(changed);
                assertEquals(ListenerChangedEvent.EVENT_NAME, changed.event());
                var opened = ListenerChangedEvent.fromJson(changed.data());
                assertEquals(ListenerKind.LAN, opened.listener());
                assertEquals(ListenerState.OPEN, opened.state());
                assertEquals(7421, opened.port());
            }
        }
    }

    @Test
    @DisplayName("broadcasts device.paired with its paired_from when a code is redeemed")
    void shouldBroadcastDevicePairedOnRedemption() throws Exception {
        try (var runtime = webRuntime(StandardScenarios.ENTRY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();
            var baseUrl = "http://127.0.0.1:" + runtime.webPort();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var pairResp = post(baseUrl + "/api/v1/pair", "{\"code\":\"KXQB-MRTN\"}");
                assertEquals(200, pairResp.statusCode());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("device.paired", event.event());
                var data = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("mock-device-1", JsonTree.string(data, "device_id"));
                assertEquals("loopback", JsonTree.string(data, "paired_from"));
            }
        }
    }

    @Test
    @DisplayName("issues a fresh code after a redemption and refuses the consumed one")
    void shouldIssueFreshCodeAfterRedemption() throws Exception {
        try (var runtime = webRuntime(StandardScenarios.ENTRY_FRESH_CODE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();
            var baseUrl = "http://127.0.0.1:" + runtime.webPort();

            var first = client.send(HttpRequest.json("POST", "/api/v1/web/pairings", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(201, first.status());
            var firstCode = PairingResource.fromJson(new String(first.body(), StandardCharsets.UTF_8));
            assertEquals("KXQB-MRTN", firstCode.code().display());

            assertEquals(200, post(baseUrl + "/api/v1/pair", "{\"code\":\"KXQB-MRTN\"}").statusCode());

            // The control state names the code that is accepted, not the scenario's now-consumed one.
            var consumedState = JsonTree.asObject(JsonTree.parse(
                    new String(client.send(HttpRequest.get("/mock/state")).body(), StandardCharsets.UTF_8)));
            assertEquals("", JsonTree.string(consumedState, "pairing_code"));

            var second = client.send(HttpRequest.json("POST", "/api/v1/web/pairings", "{}")
                    .withHeader("Authorization", auth));
            var fresh = PairingResource.fromJson(new String(second.body(), StandardCharsets.UTF_8));
            assertNotEquals(firstCode.code(), fresh.code());

            var issuedState = JsonTree.asObject(JsonTree.parse(
                    new String(client.send(HttpRequest.get("/mock/state")).body(), StandardCharsets.UTF_8)));
            assertEquals(fresh.code().value(), JsonTree.string(issuedState, "pairing_code"));

            var consumed = post(baseUrl + "/api/v1/pair", "{\"code\":\"KXQB-MRTN\"}");
            assertEquals(400, consumed.statusCode());
            assertEquals("pairing_invalid",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(consumed.body())), "error"));
        }
    }

    @Test
    @DisplayName("refuses opening the network listener on an occupied port and closes it in the status")
    void shouldRefuseOccupiedLanPort() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY_LAN_CONFLICT)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.json("PUT", "/api/v1/web/listeners/lan", "{\"port\":7421}")
                    .withHeader("Authorization", auth));
            assertEquals(409, res.status());
            assertEquals("web_listener_conflict",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(res.body())), "error"));

            var status = client.send(HttpRequest.get("/api/v1/status").withHeader("Authorization", auth));
            var entry = RuntimeEntryResource.fromJson(new String(status.body(), StandardCharsets.UTF_8));
            assertEquals(ListenerState.CLOSED, lanListener(entry).state());
        }
    }

    @Test
    @DisplayName("refuses a credential on the network listener with loopback_only")
    void shouldRefuseCredentialOnNetworkListener() throws Exception {
        try (var runtime = webRuntime(StandardScenarios.ENTRY_LAN_CREDENTIALS)) {
            var baseUrl = "http://127.0.0.1:" + runtime.webPort();

            var resp = put(baseUrl + "/api/v1/credentials/github", "{\"secret\":\"s3cr3t\"}");
            assertEquals(403, resp.statusCode());
            assertEquals("loopback_only", JsonTree.string(JsonTree.asObject(JsonTree.parse(resp.body())), "error"));
        }
    }

    @Test
    @DisplayName("accepts a credential on the loopback listener")
    void shouldAcceptCredentialOnLoopbackListener() throws Exception {
        try (var runtime = webRuntime(StandardScenarios.ENTRY)) {
            var baseUrl = "http://127.0.0.1:" + runtime.webPort();

            var resp = put(baseUrl + "/api/v1/credentials/github", "{\"secret\":\"s3cr3t\"}");
            assertEquals(200, resp.statusCode());
            assertEquals("github", JsonTree.string(JsonTree.asObject(JsonTree.parse(resp.body())), "credential_key"));
        }
    }

    @Test
    @DisplayName("broadcasts listener.changed when the network listener is turned off")
    void shouldBroadcastListenerChangedOnClose() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var res = client.send(new HttpRequest("DELETE", "/api/v1/web/listeners/lan",
                        Map.of("Authorization", auth), null));
                assertEquals(200, res.status());

                var changed = sse.next();
                assertNotNull(changed);
                assertEquals(ListenerChangedEvent.EVENT_NAME, changed.event());
                var closed = ListenerChangedEvent.fromJson(changed.data());
                assertEquals(ListenerState.CLOSED, closed.state());
                assertEquals(7421, closed.port());

                var terminated = sse.next();
                assertNotNull(terminated);
                assertEquals("stream.closed", terminated.event());
                assertEquals("listener_closed",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(terminated.data())), "reason"));
            }
        }
    }

    @Test
    @DisplayName("lists the paired devices with their paired_by")
    void shouldListPairedDevicesWithPairedBy() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY_DEVICE_LIFECYCLE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/web/devices").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            var devices = DeviceListResource.fromJson(new String(res.body(), StandardCharsets.UTF_8));

            assertEquals(3, devices.size());
            assertEquals("open", devices.devices().getFirst().pairedBy());
            assertEquals("lan", devices.devices().get(1).pairedFrom());
            assertEquals("entry", devices.devices().get(1).pairedBy());
        }
    }

    @Test
    @DisplayName("broadcasts device.renamed with its paired_from and device.revoked with stream.closed")
    void shouldManageDeviceLifecycle() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY_DEVICE_LIFECYCLE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var renameResp = client.send(HttpRequest.json("PATCH", "/api/v1/web/devices/dev-2",
                        JsonTree.write(Map.of("label", "iPad Oliver"))).withHeader("Authorization", auth));
                assertEquals(200, renameResp.status());

                var renamed = sse.next();
                assertNotNull(renamed);
                assertEquals(DeviceRenamedEvent.EVENT_NAME, renamed.event());
                var renamedEvent = DeviceRenamedEvent.fromJson(renamed.data());
                assertEquals("dev-2", renamedEvent.deviceId());
                assertEquals("iPad Oliver", renamedEvent.label());
                assertEquals("lan", renamedEvent.pairedFrom());

                var revokeResp = client.send(new HttpRequest("DELETE", "/api/v1/web/devices/dev-3",
                        Map.of("Authorization", auth), null));
                assertEquals(200, revokeResp.status());

                var revoked = sse.next();
                assertNotNull(revoked);
                assertEquals(DeviceRevokedEvent.EVENT_NAME, revoked.event());
                var revokedEvent = DeviceRevokedEvent.fromJson(revoked.data());
                assertEquals("dev-3", revokedEvent.deviceId());
                assertEquals("Chrome on build-box", revokedEvent.label());

                var terminated = sse.next();
                assertNotNull(terminated);
                assertEquals("stream.closed", terminated.event());
                assertEquals("device_revoked",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(terminated.data())), "reason"));
            }
        }
    }

    @Test
    @DisplayName("ends a revoked device's stream with stream.closed and device_revoked")
    void shouldEndRevokedDeviceStream() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY_STREAM_CLOSE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                var sse = exchange.events();

                var revokeResp = client.send(new HttpRequest("DELETE", "/api/v1/web/devices/dev-2",
                        Map.of("Authorization", auth), null));
                assertEquals(200, revokeResp.status());

                assertEquals(DeviceRevokedEvent.EVENT_NAME, sse.next().event());
                var terminated = sse.next();
                assertEquals("stream.closed", terminated.event());
                var data = JsonTree.asObject(JsonTree.parse(terminated.data()));
                assertEquals("device_revoked", JsonTree.string(data, "reason"));
                assertEquals("the terminal client", JsonTree.string(data, "revoked_by"));
            }
        }
    }

    @Test
    @DisplayName("stores a credential over the local socket")
    void shouldStoreCredentialLocally() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.json("PUT", "/api/v1/credentials/github", "{\"secret\":\"s\"}")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());
            assertEquals("github",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(res.body())), "credential_key"));
        }
    }

    @Test
    @DisplayName("reports the network listener closed in the connection status")
    void shouldReportLanClosed() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            client.send(new HttpRequest("DELETE", "/api/v1/web/listeners/lan", Map.of("Authorization", auth), null));

            var res = client.send(HttpRequest.get("/api/v1/status/connection").withHeader("Authorization", auth));
            var status = ConnectionStatus.fromJson(new String(res.body(), StandardCharsets.UTF_8));
            assertFalse(status.lanEnabled());
            assertNull(status.lanFingerprint());
        }
    }

    @Test
    @DisplayName("turns the network listener on again through PUT /api/v1/web")
    void shouldTurnLanOnThroughWebConfig() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ENTRY)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            client.send(new HttpRequest("DELETE", "/api/v1/web/listeners/lan", Map.of("Authorization", auth), null));
            var res = client.send(HttpRequest.json("PUT", "/api/v1/web", "{\"lan\":true}")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var statusRes = client.send(HttpRequest.get("/api/v1/status/connection").withHeader("Authorization", auth));
            var status = ConnectionStatus.fromJson(new String(statusRes.body(), StandardCharsets.UTF_8));
            assertTrue(status.lanEnabled());
        }
    }

    private static List<String> entryCases() {
        return StandardScenarios.ENTRY_CASES;
    }

    private static ListenerResource lanListener(RuntimeEntryResource entry) {
        return entry.listeners().stream()
                .filter(listener -> listener.listener() == ListenerKind.LAN)
                .findFirst()
                .orElseThrow();
    }

    private static MockRuntime webRuntime(String scenario) throws Exception {
        return MockRuntime.start(MockRuntimeConfig.builder()
                .scenario(scenario)
                .webPort(0)
                .runtimeToken("test-token")
                .deviceSecret(DEVICE_SECRET)
                .build());
    }

    private static HttpResponse<String> post(String url, String body) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + DEVICE_SECRET)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> put(String url, String body) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + DEVICE_SECRET)
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
