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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;

@EnableGeneratorController
class PairedDeviceTest {

    @Test
    void shouldRoundTripPairedDevice() throws Exception {
        var now = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var deviceId = Generators.nonEmptyStrings().next();
        var label = Generators.nonEmptyStrings().next();
        var device = new PairedDevice(deviceId, label, "loopback", now, now);

        var json = device.toJson();
        assertNotNull(json);

        var deserialized = PairedDevice.fromJson(json);
        assertEquals(deviceId, deserialized.deviceId());
        assertEquals(label, deserialized.label());
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
        var t1 = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var t2 = Instant.ofEpochMilli(Generators.integers(1_000_001, 2_000_000).next());
        var firstId = Generators.nonEmptyStrings().next();
        var firstLabel = Generators.nonEmptyStrings().next();
        var secondId = Generators.nonEmptyStrings().next();
        var secondLabel = Generators.nonEmptyStrings().next();

        var devices = List.of(
                new PairedDevice(firstId, firstLabel, "loopback", t1, t1),
                new PairedDevice(secondId, secondLabel, "network", t2, t2)
        );

        var json = PairedDevice.writeList(devices);
        var parsedList = PairedDevice.parseList(json);

        assertEquals(2, parsedList.size());
        assertEquals(firstId, parsedList.get(0).deviceId());
        assertEquals(firstLabel, parsedList.get(0).label());
        assertEquals("loopback", parsedList.get(0).pairedFrom());
        assertEquals(t1, parsedList.get(0).createdAt());

        assertEquals(secondId, parsedList.get(1).deviceId());
        assertEquals(secondLabel, parsedList.get(1).label());
        assertEquals("network", parsedList.get(1).pairedFrom());
        assertEquals(t2, parsedList.get(1).createdAt());
    }

    @Test
    void shouldRoundTripPairedDevicesResponse() throws Exception {
        var t1 = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var deviceId = Generators.nonEmptyStrings().next();
        var devices = List.of(new PairedDevice(deviceId, Generators.nonEmptyStrings().next(), "loopback", t1, t1));
        var response = new PairedDevicesResponse(devices, true);

        var json = response.toJson();
        var parsed = PairedDevicesResponse.fromJson(json);

        assertEquals(1, parsed.devices().size());
        assertEquals(deviceId, parsed.devices().getFirst().deviceId());
        assertTrue(parsed.lanListenerOn());

        // Also test parsing from a plain array
        var arrayJson = PairedDevice.writeList(devices);
        var parsedFromArray = PairedDevicesResponse.fromJson(arrayJson);
        assertEquals(1, parsedFromArray.devices().size());
        assertEquals(deviceId, parsedFromArray.devices().getFirst().deviceId());
    }

    @Test
    void shouldRoundTripEvents() throws Exception {
        // DeviceRenamedEvent
        var renamedId = Generators.nonEmptyStrings().next();
        var renamedLabel = Generators.nonEmptyStrings().next();
        var rename = new DeviceRenamedEvent(renamedId, renamedLabel);
        assertEquals("device.renamed", DeviceRenamedEvent.EVENT_NAME);
        var renameJson = rename.toJson();
        var parsedRename = DeviceRenamedEvent.fromJson(renameJson);
        assertEquals(renamedId, parsedRename.deviceId());
        assertEquals(renamedLabel, parsedRename.label());

        // DeviceRevokedEvent
        var revokedId = Generators.nonEmptyStrings().next();
        var revoke = new DeviceRevokedEvent(revokedId);
        assertEquals("device.revoked", DeviceRevokedEvent.EVENT_NAME);
        var revokeJson = revoke.toJson();
        var parsedRevoke = DeviceRevokedEvent.fromJson(revokeJson);
        assertEquals(revokedId, parsedRevoke.deviceId());

        // StreamClosedEvent
        var details = Generators.nonEmptyStrings().next();
        var closed = new StreamClosedEvent(StreamClosedEvent.REASON_DEVICE_REVOKED, details, "terminal", "14:10");
        assertEquals("stream.closed", StreamClosedEvent.EVENT_NAME);
        var closedJson = closed.toJson();
        var parsedClosed = StreamClosedEvent.fromJson(closedJson);
        assertEquals(StreamClosedEvent.REASON_DEVICE_REVOKED, parsedClosed.reason());
        assertEquals(details, parsedClosed.details());
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
        var deviceId = Generators.nonEmptyStrings().next();
        var dev = new PairedDevice(deviceId, "Test Dev", null, now, now);
        assertEquals("loopback", dev.pairedFrom());

        var listJson = PairedDevice.writeList(List.of(dev));
        var parsedList = PairedDevice.parseList(listJson);
        assertEquals(1, parsedList.size());
        assertEquals(deviceId, parsedList.getFirst().deviceId());

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
