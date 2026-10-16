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

class DevicesCatalogueTest {

    @Test
    void shouldLoadDevicesAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "devices");
        var de = MessageCatalogue.getMessages("de", "devices");

        assertFalse(en.isEmpty(), "English devices messages must not be empty");
        assertFalse(de.isEmpty(), "German devices messages must not be empty");

        var enKeys = new TreeSet<>(en.keySet());
        var deKeys = new TreeSet<>(de.keySet());

        assertEquals(enKeys, deKeys, "Keys in English and German devices message packs must match exactly");
    }

    @Test
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "devices");

        String[] requiredKeys = {
                "devices.list.title",
                "devices.list.subtitle",
                "devices.list.paired",
                "devices.list.last_used",
                "devices.list.from",
                "devices.list.this_one",
                "devices.list.terminal_note",
                "devices.list.empty",
                "devices.list.listener",
                "devices.act.rename",
                "devices.act.label",
                "devices.act.renamed",
                "devices.act.revoke",
                "devices.act.revoke_others",
                "devices.act.listener_off",
                "devices.revoke.title",
                "devices.revoke.explain",
                "devices.revoke.this_one",
                "devices.revoke.others_title",
                "devices.revoke.keep",
                "devices.revoke.action",
                "devices.revoke.done",
                "devices.revoke.all_title",
                "devices.listener_off.confirm_title",
                "devices.listener_off.confirm_explain",
                "devices.listener_off.done",
                "devices.revoke.others_done",
                "devices.revoked.title",
                "devices.revoked.reason",
                "devices.revoked.how_back",
                "devices.revoked.pair"
        };

        for (String key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in devices.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    void shouldFormatParameterizedDeviceMessages() {
        var renamedEn = MessageCatalogue.format("en", "devices.act.renamed", Map.of("label", "My iPad"));
        assertEquals("Renamed: My iPad", renamedEn);

        var renamedDe = MessageCatalogue.format("de", "devices.act.renamed", Map.of("label", "Mein iPad"));
        assertEquals("Umbenannt: Mein iPad", renamedDe);

        var revokeTitle = MessageCatalogue.format("en", "devices.revoke.title", Map.of("label", "Chrome"));
        assertEquals("Revoke Chrome?", revokeTitle);
    }
}
