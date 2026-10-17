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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.entry.ConnectionStatus;
import de.planmarshall.api.entry.ListenerKind;
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
    @DisplayName("refuses issuing a pairing code and opening the network listener on a web listener as cli_only")
    void shouldRefuseCliOnlyActsOnWeb() throws Exception {
        try (var runtime = webRuntime()) {
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

                var res = client.send(HttpRequest.json("PUT", "/api/v1/web/listeners/lan", "{\"port\":7421}")
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("listener.changed", event.event());
                var data = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals(ListenerKind.LAN.wireValue(), JsonTree.string(data, "listener"));
                assertEquals(Boolean.TRUE, JsonTree.bool(data, "enabled"));
                assertNotNull(JsonTree.string(data, "fingerprint"));
            }
        }
    }

    @Test
    @DisplayName("broadcasts device.paired with its paired_from when a code is redeemed")
    void shouldBroadcastDevicePairedOnRedemption() throws Exception {
        try (var runtime = webRuntime()) {
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
    @DisplayName("refuses a credential on a web listener with loopback_only")
    void shouldRefuseCredentialOnWebWithLoopbackOnly() throws Exception {
        try (var runtime = webRuntime()) {
            var baseUrl = "http://127.0.0.1:" + runtime.webPort();

            var resp = put(baseUrl + "/api/v1/credentials/github", "{\"secret\":\"s3cr3t\"}");
            assertEquals(403, resp.statusCode());
            assertEquals("loopback_only", JsonTree.string(JsonTree.asObject(JsonTree.parse(resp.body())), "error"));
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
                assertEquals("listener.changed", changed.event());
                assertEquals(Boolean.FALSE,
                        JsonTree.bool(JsonTree.asObject(JsonTree.parse(changed.data())), "enabled"));

                var closed = sse.next();
                assertNotNull(closed);
                assertEquals("stream.closed", closed.event());
                assertEquals("listener_closed", JsonTree.string(JsonTree.asObject(JsonTree.parse(closed.data())), "reason"));
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

    private static MockRuntime webRuntime() throws Exception {
        return MockRuntime.start(MockRuntimeConfig.builder()
                .scenario(StandardScenarios.ENTRY)
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
