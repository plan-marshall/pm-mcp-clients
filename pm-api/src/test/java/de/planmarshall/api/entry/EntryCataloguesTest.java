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

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.event.StreamClosedEvent;
import de.planmarshall.api.resource.ApiErrorCode;

/**
 * Compares the closed catalogues of the entry with the specification, verbatim, so a member added
 * to or removed from a catalogue fails here before it reaches a client.
 */
@DisplayName("Entry catalogues against the specification")
class EntryCataloguesTest {

    @Test
    @DisplayName("the listener kinds are the two of 03-web-server.adoc#two-listeners")
    void listenerKinds() {
        assertEquals(List.of("loopback", "lan"), ListenerKind.wireValues());
    }

    @Test
    @DisplayName("the listener states are the three of 06-event-catalogue.adoc#listener.changed")
    void listenerStates() {
        assertEquals(List.of("open", "closed", "port_occupied"), ListenerState.wireValues());
        assertEquals(ListenerState.OPEN, ListenerState.fromWire("OPEN"));
        assertEquals(ListenerState.PORT_OCCUPIED, ListenerState.fromWire("port_occupied"));
        assertNull(ListenerState.fromWireOrNull("maybe"));
        assertThrows(IllegalArgumentException.class, () -> ListenerState.fromWire("maybe"));
    }

    @Test
    @DisplayName("the stream.closed reasons are the five of 06-event-catalogue.adoc#stream.closed")
    void streamCloseReasons() {
        assertEquals(List.of("device_revoked", "device_expired", "listener_closed",
                        "runtime_stopping", "scope_ended"),
                StreamClosedEvent.wireValues());
        assertEquals(StreamClosedEvent.DEVICE_REVOKED, StreamClosedEvent.fromWire("device_revoked"));
        assertEquals(StreamClosedEvent.DEVICE_EXPIRED, StreamClosedEvent.fromWire("device_expired"));
        assertEquals(StreamClosedEvent.LISTENER_CLOSED, StreamClosedEvent.fromWire("listener_closed"));
        assertNull(StreamClosedEvent.fromWireOrNull("maybe"));
        assertThrows(IllegalArgumentException.class, () -> StreamClosedEvent.fromWire("maybe"));
    }

    @Test
    @DisplayName("the pairing code is eight characters of the alphabet BCDFGHJKLMNPQRSTVWXZ")
    void pairingCodeGrammar() {
        assertEquals(8, PairingCode.LENGTH);
        assertEquals("BCDFGHJKLMNPQRSTVWXZ", PairingCode.ALPHABET);
        assertFalse(PairingCode.ALPHABET.contains("A"));
        assertFalse(PairingCode.ALPHABET.contains("E"));
        assertFalse(PairingCode.ALPHABET.contains("I"));
        assertFalse(PairingCode.ALPHABET.contains("O"));
        assertFalse(PairingCode.ALPHABET.contains("U"));
        assertTrue(PairingCode.isValid("BCDFGHJK"));
        assertFalse(PairingCode.isValid("BCDFGHJKA"));
        assertEquals("BCDF-GHJK", new PairingCode("bcdfghjk").display());
    }

    @Test
    @DisplayName("the entry's refusals are the codes and statuses of 01-processes-and-transport.adoc#local-api")
    void entryErrorCodes() {
        assertEquals(403, ApiErrorCode.CLI_ONLY.status());
        assertEquals("cli_only", ApiErrorCode.CLI_ONLY.wire());
        assertEquals(403, ApiErrorCode.LOOPBACK_ONLY.status());
        assertEquals("loopback_only", ApiErrorCode.LOOPBACK_ONLY.wire());
        assertEquals(409, ApiErrorCode.WEB_LISTENER_CONFLICT.status());
        assertEquals("web_listener_conflict", ApiErrorCode.WEB_LISTENER_CONFLICT.wire());
        assertEquals(400, ApiErrorCode.PAIRING_INVALID.status());
        assertEquals("pairing_invalid", ApiErrorCode.PAIRING_INVALID.wire());
        assertEquals(404, ApiErrorCode.TARGET_NOT_FOUND.status());
        assertEquals("target_not_found", ApiErrorCode.TARGET_NOT_FOUND.wire());
    }

    @Test
    @DisplayName("the credential entry statuses and stores are the closed lists of #web-credentials")
    void credentialCatalogue() {
        assertEquals(List.of("present", "not_found", "stored"),
                WebCredentialResource.Status.wireValues());
        assertEquals(List.of("keychain", "secret-service", "file"),
                WebCredentialResource.Store.wireValues());
        assertNull(WebCredentialResource.Status.fromWireOrNull("maybe"));
        assertNull(WebCredentialResource.Store.fromWireOrNull("vault"));
    }
}
