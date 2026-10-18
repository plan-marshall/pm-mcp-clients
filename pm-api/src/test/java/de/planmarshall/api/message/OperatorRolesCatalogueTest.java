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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Operator roles message catalogue")
class OperatorRolesCatalogueTest {

    @Test
    @DisplayName("loads the operator area in English and German with exact key parity")
    void shouldLoadOperatorArea() {
        var en = MessageCatalogue.getMessages("en", "operator");
        var de = MessageCatalogue.getMessages("de", "operator");

        assertFalse(en.isEmpty(), "English operator messages must not be empty");
        assertFalse(de.isEmpty(), "German operator messages must not be empty");
        assertEquals(new TreeSet<>(en.keySet()), new TreeSet<>(de.keySet()),
                "Keys in English and German operator packs must match exactly");
    }

    @Test
    @DisplayName("carries every role key the verbs render")
    void shouldCarryRoleKeys() {
        var en = MessageCatalogue.getMessages("en", "operator");
        var required = new String[]{
                "operator.roles.empty",
                "operator.roles.list.title",
                "operator.roles.header.role",
                "operator.roles.header.set",
                "operator.roles.header.class",
                "operator.roles.header.form",
                "operator.roles.header.harness",
                "operator.roles.header.model",
                "operator.roles.header.effort",
                "operator.roles.header.qualification",
                "operator.roles.header.guard",
                "operator.roles.guard.held",
                "operator.roles.guard.none",
                "operator.roles.failed",
                "operator.roles.show.title",
                "operator.roles.show.description",
                "operator.roles.show.class",
                "operator.roles.show.effective",
                "operator.roles.show.tools",
                "operator.roles.show.narrowed",
                "operator.roles.show.qualification",
                "operator.roles.show.reason",
                "operator.roles.show.criteria",
                "operator.roles.show.parameters",
                "operator.roles.show.not_found",
                "operator.roles.show.failed",
                "operator.roles.qualify.title",
                "operator.roles.qualify.configuration",
                "operator.roles.qualify.topics",
                "operator.roles.qualify.items",
                "operator.roles.qualify.expected",
                "operator.roles.qualify.confirm",
                "operator.roles.qualify.confirm_required",
                "operator.roles.qualify.declined",
                "operator.roles.qualify.done",
                "operator.roles.qualify.not_found",
                "operator.roles.qualify.failed",
                "operator.roles.qualify.refused",
                "operator.roles.qualify.measurements",
                "operator.roles.qualify.record",
                "operator.roles.qualify.fresh",
                "operator.roles.qualify.warm",
                "operator.roles.qualify.gap",
                "operator.roles.qualify.qualified_fresh",
                "operator.roles.qualify.qualified_warm"
        };
        for (var key : required) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in operator.en.properties");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("formats parameterized role messages in both languages")
    void shouldFormatRoleMessages() {
        assertEquals("Roles (7)", MessageCatalogue.format("en", "operator.roles.list.title",
                Map.of("count", 7)));
        assertEquals("Rollen (7)", MessageCatalogue.format("de", "operator.roles.list.title",
                Map.of("count", 7)));
        assertEquals("Qualification: qualified", MessageCatalogue.format("en",
                "operator.roles.show.qualification", Map.of("state", "qualified")));
        assertEquals("Qualifikation: qualifiziert", MessageCatalogue.format("de",
                "operator.roles.show.qualification", Map.of("state", "qualifiziert")));
    }
}
