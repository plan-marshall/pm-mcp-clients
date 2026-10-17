/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class EntryCatalogueTest {

    @Test
    void shouldLoadEntryAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "entry");
        var de = MessageCatalogue.getMessages("de", "entry");

        assertFalse(en.isEmpty(), "English entry messages must not be empty");
        assertFalse(de.isEmpty(), "German entry messages must not be empty");

        var enKeys = new TreeSet<>(en.keySet());
        var deKeys = new TreeSet<>(de.keySet());

        assertEquals(enKeys, deKeys, "Keys in English and German entry message packs must match exactly");
    }

    @Test
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "entry");

        var requiredKeys = new String[]{
                "entry.connection.loopback",
                "entry.connection.loopback.detail",
                "entry.connection.lan",
                "entry.connection.lan.detail",
                "entry.connection.devices",
                "entry.connection.lan_off.title",
                "entry.connection.lan_off.reason",
                "entry.connection.lan_off.unknown",
                "entry.lan.title",
                "entry.lan.state",
                "entry.lan.state_on",
                "entry.lan.state_off",
                "entry.lan.fingerprint",
                "entry.lan.fingerprint_hint",
                "entry.lan.turn_off",
                "entry.lan.keep",
                "entry.lan.off_done",
                "entry.credentials.loopback_only_title",
                "entry.credentials.loopback_only",
                "entry.credentials.field",
                "entry.credentials.save",
                "entry.credentials.stored",
                "entry.listener.lan_open",
                "entry.listener.fingerprint"
        };

        for (var key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in entry.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    void shouldBePartOfTheSupportedAreas() {
        assertTrue(MessageCatalogue.SUPPORTED_AREAS.contains("entry"),
                "The entry area must be registered so both clients can load it");
    }

    @Test
    void shouldFormatParameterizedEntryMessages() {
        var lanOff = MessageCatalogue.format("en", "entry.connection.lan_off.reason",
                Map.of("time", "14:32", "command", "pm-operator web lan"));
        assertEquals("Its network access was turned off at 14:32. "
                + "This browser stays paired: it works again once the network access is on "
                + "(pm-operator web lan).", lanOff);

        var loopbackOnly = MessageCatalogue.format("en", "entry.credentials.loopback_only",
                Map.of("command", "pm-operator credentials set github"));
        assertTrue(loopbackOnly.contains("pm-operator credentials set github"));

        var devices = MessageCatalogue.format("en", "entry.connection.devices", Map.of("count", 1));
        assertEquals("1 paired device", devices);
    }
}
