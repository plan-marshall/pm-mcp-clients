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

import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.web.PairedDevice;

@DisplayName("PairedDevicesScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PairedDevicesScenarioTest {

    @Test
    @DisplayName("loads paired-devices scenario and serves initial device list")
    void shouldLoadScenarioAndListDevices() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PAIRED_DEVICES)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.ONBOARDING, runtime.currentScenarioName());
            assertEquals("entry.device-lifecycle", runtime.currentStateName());

            var res = client.send(HttpRequest.get("/api/v1/web/devices").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var devices = PairedDevice.parseList(new String(res.body()));
            assertEquals(3, devices.size());

            assertEquals("dev-1", devices.get(0).deviceId());
            assertEquals("Firefox on mbp-oliver", devices.get(0).label());
            assertEquals("loopback", devices.get(0).pairedFrom());

            assertEquals("dev-2", devices.get(1).deviceId());
            assertEquals("Safari on iPad", devices.get(1).label());
            assertEquals("lan", devices.get(1).pairedFrom());

            assertEquals("dev-3", devices.get(2).deviceId());
            assertEquals("Chrome on build-box", devices.get(2).label());
            assertEquals("loopback", devices.get(2).pairedFrom());
        }
    }

    @Test
    @DisplayName("renames device via PATCH and broadcasts device.renamed event")
    void shouldRenameDevice() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PAIRED_DEVICES)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var renameBody = JsonTree.write(Map.of("label", "iPad Oliver"));
                var res = client.send(HttpRequest.json("PATCH", "/api/v1/web/devices/dev-2", renameBody)
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("device.renamed", event.event());
                var eventData = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("dev-2", JsonTree.string(eventData, "device_id"));
                assertEquals("iPad Oliver", JsonTree.string(eventData, "label"));

                // Verify updated in list
                var listRes = client.send(HttpRequest.get("/api/v1/web/devices").withHeader("Authorization", auth));
                var devices = PairedDevice.parseList(new String(listRes.body()));
                assertEquals("iPad Oliver", devices.get(1).label());
            }
        }
    }

    @Test
    @DisplayName("revokes device via DELETE and broadcasts device.revoked and stream.closed")
    void shouldRevokeDevice() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PAIRED_DEVICES)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var res = client.send(new HttpRequest("DELETE", "/api/v1/web/devices/dev-3",
                        Map.of("Authorization", auth), null));
                assertEquals(200, res.status());

                var revokedEvent = sse.next();
                assertNotNull(revokedEvent);
                assertEquals("device.revoked", revokedEvent.event());

                var closedEvent = sse.next();
                assertNotNull(closedEvent);
                assertEquals("stream.closed", closedEvent.event());
                var closedData = JsonTree.asObject(JsonTree.parse(closedEvent.data()));
                assertEquals("device_revoked", JsonTree.string(closedData, "reason"));

                // Verify device is gone from list
                var listRes = client.send(HttpRequest.get("/api/v1/web/devices").withHeader("Authorization", auth));
                var devices = PairedDevice.parseList(new String(listRes.body()));
                assertEquals(2, devices.size());
                assertFalse(devices.stream().anyMatch(d -> "dev-3".equals(d.deviceId())));
            }
        }
    }

    @Test
    @DisplayName("turns network listener off via DELETE /api/v1/web/listeners/lan")
    void shouldTurnLanListenerOff() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PAIRED_DEVICES)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var statusBefore = client.send(HttpRequest.get("/api/v1/status").withHeader("Authorization", auth));
            var webBefore = JsonTree.object(JsonTree.asObject(JsonTree.parse(statusBefore.body())), "web");
            assertEquals(Boolean.TRUE, webBefore.get("lan"));

            var res = client.send(new HttpRequest("DELETE", "/api/v1/web/listeners/lan",
                    Map.of("Authorization", auth), null));
            assertEquals(200, res.status());

            var statusAfter = client.send(HttpRequest.get("/api/v1/status").withHeader("Authorization", auth));
            var webAfter = JsonTree.object(JsonTree.asObject(JsonTree.parse(statusAfter.body())), "web");
            assertEquals(Boolean.FALSE, webAfter.get("lan"));
        }
    }

    @Test
    @DisplayName("returns 404 when renaming non-existent device")
    void shouldReturn404WhenRenamingNonExistentDevice() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PAIRED_DEVICES)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var renameBody = JsonTree.write(Map.of("label", "Unknown"));
            var res = client.send(HttpRequest.json("PATCH", "/api/v1/web/devices/dev-unknown", renameBody)
                    .withHeader("Authorization", auth));
            assertEquals(404, res.status());
        }
    }

    @Test
    @DisplayName("returns 404 when revoking non-existent device")
    void shouldReturn404WhenRevokingNonExistentDevice() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PAIRED_DEVICES)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(new HttpRequest("DELETE", "/api/v1/web/devices/dev-unknown",
                    Map.of("Authorization", auth), null));
            assertEquals(404, res.status());
        }
    }

    @Test
    @DisplayName("updates network listener via PUT /api/v1/web")
    void shouldUpdateLanListenerViaPut() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PAIRED_DEVICES)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var body = JsonTree.write(Map.of("lan", false));
            var res = client.send(HttpRequest.json("PUT", "/api/v1/web", body).withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var status = client.send(HttpRequest.get("/api/v1/status").withHeader("Authorization", auth));
            var web = JsonTree.object(JsonTree.asObject(JsonTree.parse(status.body())), "web");
            assertEquals(Boolean.FALSE, web.get("lan"));
        }
    }
}
