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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.control.HoldKind;
import de.planmarshall.api.control.PlanAction;

@DisplayName("ControlCatalogueTest")
class ControlCatalogueTest {

    @Test
    @DisplayName("should load the control area in English and German with identical key sets")
    void shouldLoadControlArea() {
        var en = MessageCatalogue.getMessages("en", "control");
        var de = MessageCatalogue.getMessages("de", "control");

        assertFalse(en.isEmpty(), "English control messages must not be empty");
        assertFalse(de.isEmpty(), "German control messages must not be empty");
        assertEquals(new TreeSet<>(en.keySet()), new TreeSet<>(de.keySet()),
                "Keys in English and German control message packs must match exactly");
    }

    /**
     * The keys of {@code doc/design/shared/work-actions-texts.adoc}, read from the repository: every
     * key cell of both of its tables, the {@code control} keys of {@code #texts} and the
     * {@code common.conflict.*} keys of {@code #texts-conflict}, which that document defines.
     */
    private static List<String> specificationKeys() throws IOException {
        return SpecificationTables.keyCells("shared/work-actions-texts.adoc");
    }

    @Test
    @DisplayName("should contain every key of the work-actions text tables in both packs")
    void shouldContainAllSpecificationKeys() throws Exception {
        var keys = specificationKeys();
        var control = keys.stream().filter(key -> key.startsWith("control.")).toList();
        var common = keys.stream().filter(key -> key.startsWith("common.")).toList();

        assertTrue(control.size() > 50, "the control table of work-actions-texts.adoc must be read: " + control);
        assertTrue(common.size() >= 5, "the common.conflict table of work-actions-texts.adoc must be read: " + common);
        assertEquals(keys.size(), control.size() + common.size(), "a key of another area in the tables: " + keys);
        assertTrue(control.contains("control.epic.next_invalid"), control.toString());
        assertTrue(common.contains("common.conflict.no_input"), common.toString());
        var missing = new ArrayList<String>();
        for (var locale : List.of("en", "de")) {
            var messages = MessageCatalogue.getMessages(locale, "all");
            for (String key : keys) {
                var value = messages.get(key);
                if (value == null || value.isBlank()) {
                    missing.add(locale + ":" + key);
                }
            }
        }
        assertTrue(missing.isEmpty(), "keys of work-actions-texts.adoc missing or blank in a pack: " + missing);
    }

    @Test
    @DisplayName("should name both answers of the raise question in English and German")
    void shouldPinTheInvalidRaiseAnswer() {
        var params = Map.of("raise", "r", "leave", "n");

        assertEquals("Type r to raise the scope or n to leave it.",
                MessageCatalogue.format("en", "control.epic.next_invalid", params));
        assertEquals("Tippe r, um den Umfang zu erhöhen, oder n, um es zu lassen.",
                MessageCatalogue.format("de", "control.epic.next_invalid", params));
    }

    @Test
    @DisplayName("should assert every plan action and hold kind has a key in both packs")
    void shouldHaveKeysForEveryEnumValue() {
        for (var action : PlanAction.values()) {
            var key = "control.plan." + action.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for action: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for action: " + key);
        }
        for (var kind : HoldKind.values()) {
            var key = "control.hold.kind." + kind.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for hold kind: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for hold kind: " + key);
        }
    }

    @Test
    @DisplayName("should format the unknown scope text word for word in both languages")
    void shouldFormatTheUnknownScopeText() {
        var params = Map.of("epic", "relay-hardening", "scope", 3, "plan", "relay-token-store");

        assertEquals("How many plans run is not known; relay-hardening may run 3 at once, and the next "
                + "plan, relay-token-store, waits for a free place.",
                MessageCatalogue.format("en", "control.epic.scope_unknown", params));
        assertEquals("Wie viele Pl\u00e4ne laufen, ist nicht bekannt; relay-hardening darf 3 gleichzeitig "
                + "ausf\u00fchren, und der n\u00e4chste Plan, relay-token-store, wartet auf einen freien Platz.",
                MessageCatalogue.format("de", "control.epic.scope_unknown", params));
        assertEquals("Type r to raise the scope or n to leave it.",
                MessageCatalogue.format("en", "control.epic.next_invalid",
                        Map.of("raise", "r", "leave", "n")));
    }

    @Test
    @DisplayName("should format parameterized control messages in both languages")
    void shouldFormatParameterizedMessages() {
        assertEquals("Pause fix-relay?", MessageCatalogue.format("en", "control.pause.title",
                Map.of("plan", "fix-relay")));
        assertEquals("fix-relay anhalten?", MessageCatalogue.format("de", "control.pause.title",
                Map.of("plan", "fix-relay")));

        assertEquals("pausing after the task", MessageCatalogue.format("en", "control.plan.state.pausing",
                Map.of("point", "task")));
        assertEquals("h\u00e4lt nach der Phase an", MessageCatalogue.format("de", "control.plan.state.pausing",
                Map.of("point", "phase")));

        assertEquals("a condition", MessageCatalogue.format("en", "control.hold.kind.condition", Map.of()));
        assertEquals("eine Integrit\u00e4tspause", MessageCatalogue.format("de", "control.hold.kind.integrity_pause",
                Map.of()));

        assertEquals("Applies where fix-relay next reads it; the plan's version becomes 8.",
                MessageCatalogue.format("en", "control.settings.applies",
                        Map.of("plan", "fix-relay", "version", 8)));
    }
}
