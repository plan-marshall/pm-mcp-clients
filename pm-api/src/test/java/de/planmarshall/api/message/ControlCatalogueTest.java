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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.control.HoldKind;
import de.planmarshall.api.control.PlanAction;

@DisplayName("ControlCatalogueTest")
class ControlCatalogueTest {

    private static final Pattern KEY_CELL = Pattern.compile("^\\|`(control\\.[a-z0-9_.]+)`$");
    private static final String TABLE_DELIMITER = "|===";

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
     * The keys of the text table of {@code doc/design/shared/work-actions-texts.adoc#texts}, read from
     * the repository: every key cell {@code |`control.…`} of the document's first table.
     */
    private static List<String> specificationKeys() throws IOException {
        var basedir = System.getProperty("basedir");
        var module = basedir != null ? Path.of(basedir) : Path.of("").toAbsolutePath();
        var document = module.resolve("../doc/design/shared/work-actions-texts.adoc").normalize();
        var keys = new ArrayList<String>();
        var inTable = false;
        for (var line : Files.readAllLines(document, StandardCharsets.UTF_8)) {
            if (TABLE_DELIMITER.equals(line.strip())) {
                if (inTable) {
                    break;
                }
                inTable = true;
            } else if (inTable) {
                var matcher = KEY_CELL.matcher(line.strip());
                if (matcher.matches()) {
                    keys.add(matcher.group(1));
                }
            }
        }
        return keys;
    }

    @Test
    @DisplayName("should contain every key of the work-actions text table in both packs")
    void shouldContainAllSpecificationKeys() throws Exception {
        var en = MessageCatalogue.getMessages("en", "control");
        var keys = specificationKeys();

        assertTrue(keys.size() > 50, "the text table of work-actions-texts.adoc must be read: " + keys);
        assertTrue(keys.contains("control.epic.next_invalid"), keys.toString());
        for (String key : keys) {
            assertTrue(MessageCatalogue.hasKey("en", key), "Key '" + key + "' must exist in the English pack");
            assertTrue(MessageCatalogue.hasKey("de", key), "Key '" + key + "' must exist in the German pack");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
        for (String key : List.of("common.conflict.changed", "common.conflict.apply_again", "common.conflict.keep")) {
            assertTrue(MessageCatalogue.hasKey("en", key), "Key '" + key + "' must exist in the English pack");
            assertTrue(MessageCatalogue.hasKey("de", key), "Key '" + key + "' must exist in the German pack");
        }
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
