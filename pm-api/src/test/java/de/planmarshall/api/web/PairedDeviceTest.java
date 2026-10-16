/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

class PairedDeviceTest {

    @Test
    void shouldRoundTripPairedDevice() throws Exception {
        var now = Instant.parse("2026-10-16T10:00:00Z");
        var device = new PairedDevice("dev-1", "Firefox on MBP", "loopback", now, now);

        var json = device.toJson();
        assertNotNull(json);

        var deserialized = PairedDevice.fromJson(json);
        assertEquals("dev-1", deserialized.deviceId());
        assertEquals("Firefox on MBP", deserialized.label());
        assertEquals("loopback", deserialized.pairedFrom());
        assertEquals(now, deserialized.createdAt());
        assertEquals(now, deserialized.lastUsedAt());
    }

    @Test
    void shouldDefaultPairedFromWhenBlankOrNull() {
        var now = Instant.now();
        var device = new PairedDevice("dev-2", "Safari", null, now, now);
        assertEquals("loopback", device.pairedFrom());

        var device2 = new PairedDevice("dev-3", "Chrome", "   ", now, now);
        assertEquals("loopback", device2.pairedFrom());
    }

    @Test
    void shouldFailOnMissingRequiredFields() {
        var now = Instant.now();
        assertThrows(NullPointerException.class, () -> new PairedDevice(null, "Label", "loopback", now, now));
        assertThrows(NullPointerException.class, () -> new PairedDevice("id", null, "loopback", now, now));
        assertThrows(NullPointerException.class, () -> new PairedDevice("id", "Label", "loopback", null, now));
        assertThrows(NullPointerException.class, () -> new PairedDevice("id", "Label", "loopback", now, null));

        assertThrows(JsonFormatException.class, () -> PairedDevice.fromJson("{\"device_id\":\"123\"}"));
    }

    @Test
    void shouldRoundTripDeviceList() throws Exception {
        var t1 = Instant.parse("2026-05-12T14:02:00Z");
        var t2 = Instant.parse("2026-05-20T08:30:00Z");

        var devices = List.of(
                new PairedDevice("dev-1", "Firefox on mbp-oliver", "loopback", t1, t1),
                new PairedDevice("dev-2", "iPad Oliver", "network", t2, t2)
        );

        var json = PairedDevice.writeList(devices);
        var parsedList = PairedDevice.parseList(json);

        assertEquals(2, parsedList.size());
        assertEquals("dev-1", parsedList.get(0).deviceId());
        assertEquals("Firefox on mbp-oliver", parsedList.get(0).label());
        assertEquals("loopback", parsedList.get(0).pairedFrom());
        assertEquals(t1, parsedList.get(0).createdAt());

        assertEquals("dev-2", parsedList.get(1).deviceId());
        assertEquals("iPad Oliver", parsedList.get(1).label());
        assertEquals("network", parsedList.get(1).pairedFrom());
        assertEquals(t2, parsedList.get(1).createdAt());
    }

    @Test
    void shouldRoundTripPairedDevicesResponse() throws Exception {
        var t1 = Instant.parse("2026-05-12T14:02:00Z");
        var devices = List.of(new PairedDevice("dev-1", "Firefox", "loopback", t1, t1));
        var response = new PairedDevicesResponse(devices, true);

        var json = response.toJson();
        var parsed = PairedDevicesResponse.fromJson(json);

        assertEquals(1, parsed.devices().size());
        assertEquals("dev-1", parsed.devices().getFirst().deviceId());
        assertTrue(parsed.lanListenerOn());

        // Also test parsing from a plain array
        var arrayJson = PairedDevice.writeList(devices);
        var parsedFromArray = PairedDevicesResponse.fromJson(arrayJson);
        assertEquals(1, parsedFromArray.devices().size());
        assertEquals("dev-1", parsedFromArray.devices().getFirst().deviceId());
    }

    @Test
    void shouldRoundTripEvents() throws Exception {
        // DeviceRenamedEvent
        var rename = new DeviceRenamedEvent("dev-1", "Work Laptop");
        assertEquals("device.renamed", DeviceRenamedEvent.EVENT_NAME);
        var renameJson = rename.toJson();
        var parsedRename = DeviceRenamedEvent.fromJson(renameJson);
        assertEquals("dev-1", parsedRename.deviceId());
        assertEquals("Work Laptop", parsedRename.label());

        // DeviceRevokedEvent
        var revoke = new DeviceRevokedEvent("dev-2");
        assertEquals("device.revoked", DeviceRevokedEvent.EVENT_NAME);
        var revokeJson = revoke.toJson();
        var parsedRevoke = DeviceRevokedEvent.fromJson(revokeJson);
        assertEquals("dev-2", parsedRevoke.deviceId());

        // StreamClosedEvent
        var closed = new StreamClosedEvent(StreamClosedEvent.REASON_DEVICE_REVOKED, "Revoked from terminal", "terminal", "14:10");
        assertEquals("stream.closed", StreamClosedEvent.EVENT_NAME);
        var closedJson = closed.toJson();
        var parsedClosed = StreamClosedEvent.fromJson(closedJson);
        assertEquals(StreamClosedEvent.REASON_DEVICE_REVOKED, parsedClosed.reason());
        assertEquals("Revoked from terminal", parsedClosed.details());
        assertEquals("terminal", parsedClosed.revokedBy());
        assertEquals("14:10", parsedClosed.revokedAt());

        var simpleClosed = new StreamClosedEvent(StreamClosedEvent.REASON_LISTENER_CLOSED, null);
        var parsedSimple = StreamClosedEvent.fromJson(simpleClosed.toJson());
        assertEquals(StreamClosedEvent.REASON_LISTENER_CLOSED, parsedSimple.reason());
        assertNull(parsedSimple.details());
    }

    @Test
    void shouldHandlePairedDeviceListParsingAndErrors() throws Exception {
        var now = Instant.now();
        var dev = new PairedDevice("d-1", "Test Dev", null, now, now);
        assertEquals("loopback", dev.pairedFrom());

        var listJson = PairedDevice.writeList(List.of(dev));
        var parsedList = PairedDevice.parseList(listJson);
        assertEquals(1, parsedList.size());
        assertEquals("d-1", parsedList.getFirst().deviceId());

        // camelCase fallback parsing
        var camelCaseJson = "{\"deviceId\":\"d-2\",\"label\":\"L2\",\"pairedFrom\":\"network\",\"createdAt\":\""
                + now + "\",\"lastUsedAt\":\"" + now + "\"}";
        var parsedCamel = PairedDevice.fromJson(camelCaseJson);
        assertEquals("d-2", parsedCamel.deviceId());
        assertEquals("L2", parsedCamel.label());
        assertEquals("network", parsedCamel.pairedFrom());

        // parse errors
        assertThrows(JsonFormatException.class, () -> PairedDevice.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> PairedDevice.fromJson("{}"));
        assertThrows(JsonFormatException.class, () -> PairedDevice.parseList("{}"));
        assertThrows(JsonFormatException.class, () -> DeviceRenamedEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> DeviceRenamedEvent.fromJson("{\"device_id\":\"1\"}"));
        assertThrows(JsonFormatException.class, () -> DeviceRevokedEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> DeviceRevokedEvent.fromJson("{}"));
        assertThrows(JsonFormatException.class, () -> StreamClosedEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> StreamClosedEvent.fromJson("{}"));
        assertThrows(JsonFormatException.class, () -> PairedDevicesResponse.fromJson("\"not json obj\""));
    }
}
