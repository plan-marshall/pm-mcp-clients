/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

@DisplayName("Entry record contracts")
class EntryRecordsTest {

    @Nested
    @DisplayName("ListenerKind")
    class ListenerKindTest {

        @Test
        @DisplayName("is the closed list of the two listeners and resolves its wire values")
        void closedListAndWireValues() {
            var wireValues = Stream.of(ListenerKind.values())
                    .map(ListenerKind::wireValue)
                    .collect(Collectors.toCollection(TreeSet::new));

            assertEquals(Set.of("loopback", "lan"), wireValues);
            assertEquals(ListenerKind.LOOPBACK, ListenerKind.fromWire("loopback"));
            assertEquals(ListenerKind.LAN, ListenerKind.fromWire("LAN"));
        }

        @Test
        @DisplayName("refuses a value that names no listener")
        void refusesUnknownValue() {
            assertThrows(IllegalArgumentException.class, () -> ListenerKind.fromWire("bogus"));
        }
    }

    @Test
    @DisplayName("PairingToken round-trips and keeps its expiry")
    void pairingTokenRoundTrips() throws Exception {
        var token = new PairingToken("KXQB-MRTN", "http://127.0.0.1:7420/pair#code=KXQB-MRTN", 600);

        var roundTrip = PairingToken.fromJson(token.toJson());

        assertEquals(token, roundTrip);
    }

    @Test
    @DisplayName("PairingToken defaults the expiry when it is absent")
    void pairingTokenExpiryDefault() throws Exception {
        var parsed = PairingToken.fromJson("{\"code\":\"KXQB-MRTN\",\"url\":\"http://127.0.0.1:7420/pair#code=KXQB-MRTN\"}");

        assertEquals(PairingToken.DEFAULT_LIFETIME_SECONDS, parsed.expiresInSeconds());
    }

    @Test
    @DisplayName("PairingRedemptionRequest round-trips and omits an absent label")
    void pairingRedemptionRequestRoundTrips() throws Exception {
        var full = new PairingRedemptionRequest("KXQB-MRTN", "Firefox on mbp-oliver", "web");

        assertEquals(full, PairingRedemptionRequest.fromJson(full.toJson()));

        var minimal = new PairingRedemptionRequest("KXQB-MRTN", null, null);
        assertFalse(minimal.toMap().containsKey("label"));
        var parsed = PairingRedemptionRequest.fromJson(minimal.toJson());
        assertNull(parsed.label());
        assertNull(parsed.clientType());
    }

    @Test
    @DisplayName("PairingRedemptionResponse round-trips with its paired_from origin")
    void pairingRedemptionResponseRoundTrips() throws Exception {
        var response = new PairingRedemptionResponse("pmw_secret", "dev-1", "Firefox on mbp-oliver", "lan");

        assertEquals(response, PairingRedemptionResponse.fromJson(response.toJson()));
    }

    @Test
    @DisplayName("LanListenerConfig round-trips with its fingerprint")
    void lanListenerConfigRoundTrips() throws Exception {
        var config = new LanListenerConfig(true, 7421, "mbp-oliver.local", "4F:1A:9C");

        assertEquals(config, LanListenerConfig.fromJson(config.toJson()));
    }

    @Test
    @DisplayName("ConnectionStatus round-trips with both listeners and the device count")
    void connectionStatusRoundTrips() throws Exception {
        var status = new ConnectionStatus("http://127.0.0.1:7420", true, true,
                "https://mbp-oliver.local:7421", "4F:1A:9C", 2, true);

        assertEquals(status, ConnectionStatus.fromJson(status.toJson()));
    }

    @Test
    @DisplayName("DevicePairedEvent round-trips with its paired_from origin")
    void devicePairedEventRoundTrips() throws Exception {
        var event = new DevicePairedEvent("dev-1", "Firefox on mbp-oliver", "loopback");

        assertEquals("device.paired", DevicePairedEvent.EVENT_NAME);
        assertEquals(event, DevicePairedEvent.fromJson(event.toJson()));
    }

    @Test
    @DisplayName("ListenerChangedEvent round-trips with its listener kind")
    void listenerChangedEventRoundTrips() throws Exception {
        var event = new ListenerChangedEvent(ListenerKind.LAN, true, "https://mbp-oliver.local:7421", "4F:1A:9C");

        assertEquals("listener.changed", ListenerChangedEvent.EVENT_NAME);
        var roundTrip = ListenerChangedEvent.fromJson(event.toJson());
        assertEquals(event, roundTrip);
        assertEquals("lan", roundTrip.toMap().get("listener"));
        assertTrue(roundTrip.enabled());
    }

    @Nested
    @DisplayName("Negative parsing")
    class NegativeParsing {

        @Test
        @DisplayName("refuses a JSON value that is no object")
        void refusesNonObjectJson() {
            assertThrows(JsonFormatException.class, () -> PairingToken.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PairingRedemptionRequest.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PairingRedemptionResponse.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> LanListenerConfig.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> ConnectionStatus.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> DevicePairedEvent.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> ListenerChangedEvent.fromJson("[]"));
        }

        @Test
        @DisplayName("refuses an object without the required fields")
        void refusesMissingFields() {
            assertThrows(JsonFormatException.class, () -> PairingToken.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PairingRedemptionRequest.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PairingRedemptionResponse.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> LanListenerConfig.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> DevicePairedEvent.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> ListenerChangedEvent.fromJson("{}"));
        }

        @Test
        @DisplayName("defaults the optional connection fields")
        void defaultsOptionalConnectionFields() throws Exception {
            var status = ConnectionStatus.fromJson("{}");

            assertEquals(0, status.pairedDevicesCount());
            assertFalse(status.loopbackOpen());
            assertFalse(status.lanEnabled());
        }
    }
}
