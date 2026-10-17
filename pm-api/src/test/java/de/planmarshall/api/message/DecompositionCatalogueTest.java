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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Decomposition message catalogue")
class DecompositionCatalogueTest {

    @Test
    @DisplayName("loads the decomposition area in English and German with identical keys")
    void shouldLoadDecompositionAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "decomposition");
        var de = MessageCatalogue.getMessages("de", "decomposition");

        assertFalse(en.isEmpty(), "English decomposition messages must not be empty");
        assertFalse(de.isEmpty(), "German decomposition messages must not be empty");
        assertEquals(new TreeSet<>(en.keySet()), new TreeSet<>(de.keySet()),
                "Keys in English and German decomposition message packs must match exactly");
    }

    @Test
    @DisplayName("contains the specification keys of the decomposition")
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "decomposition");

        var requiredKeys = new String[]{
                "decomposition.title",
                "decomposition.drafting",
                "decomposition.plans",
                "decomposition.workstreams",
                "decomposition.keep",
                "decomposition.edit",
                "decomposition.drop",
                "decomposition.add",
                "decomposition.open_plan",
                "decomposition.chat",
                "decomposition.instruct.title",
                "decomposition.instruct.prompt",
                "decomposition.read_back.title",
                "decomposition.accept",
                "decomposition.instruct_again",
                "decomposition.discard",
                "decomposition.draft_again",
                "decomposition.agree",
                "decomposition.checks_fail",
                "decomposition.checks_pass",
                "decomposition.committed",
                "decomposition.recommitted",
                "decomposition.then",
                "decomposition.overlap",
                "decomposition.change",
                "decomposition.locked",
                "decomposition.objective",
                "decomposition.deliverables",
                "decomposition.surface",
                "decomposition.depends_on",
                "decomposition.none",
                "decomposition.workstream_label",
                "decomposition.refused",
                "decomposition.plan_not_found",
                "decomposition.committed_follow",
                "decomposition.left",
                "decomposition.error"
        };

        for (var key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in decomposition.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("is part of the supported areas so both clients can load it")
    void shouldBePartOfTheSupportedAreas() {
        assertTrue(MessageCatalogue.SUPPORTED_AREAS.contains("decomposition"),
                "The decomposition area must be registered so both clients can load it");
    }

    @Test
    @DisplayName("formats the parameterized and pluralized decomposition messages")
    void shouldFormatParameterizedDecompositionMessages() {
        assertEquals("drafting WS-2",
                MessageCatalogue.format("en", "decomposition.drafting", Map.of("workstream", "WS-2")));
        assertEquals("2 plans",
                MessageCatalogue.format("en", "decomposition.plans", Map.of("count", 2)));
        assertEquals("1 plan",
                MessageCatalogue.format("en", "decomposition.plans", Map.of("count", 1)));

        assertEquals("relay-hardening: 2 workstreams, 4 plans committed",
                MessageCatalogue.format("en", "decomposition.committed",
                        Map.of("epic", "relay-hardening", "workstreams", 2, "plans", 4)));
        assertEquals("relay-hardening: 3 workstreams, 1 changed, 1 new, 1 dropped",
                MessageCatalogue.format("en", "decomposition.recommitted",
                        Map.of("epic", "relay-hardening", "workstreams", 3, "changed", 1, "new", 1, "dropped", 1)));

        assertEquals("changed",
                MessageCatalogue.format("en", "decomposition.change", Map.of("change", "changed")));
        assertEquals("1 check fails",
                MessageCatalogue.format("en", "decomposition.checks_fail", Map.of("count", 1)));

        assertEquals("can start now: relay-cursor-order; it starts none by itself",
                MessageCatalogue.format("en", "decomposition.then",
                        Map.of("mode", "manual", "plans", "relay-cursor-order")));

        assertEquals("relay-hardening \u00b7 Zerlegung",
                MessageCatalogue.format("de", "decomposition.title", Map.of("epic", "relay-hardening")));
    }
}
