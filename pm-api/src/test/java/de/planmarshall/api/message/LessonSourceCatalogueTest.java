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

@DisplayName("Lesson source message catalogue")
class LessonSourceCatalogueTest {

    @Test
    @DisplayName("loads the lesson_source area in English and German with identical keys")
    void shouldLoadLessonSourceAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "lesson_source");
        var de = MessageCatalogue.getMessages("de", "lesson_source");

        assertFalse(en.isEmpty(), "English lesson_source messages must not be empty");
        assertFalse(de.isEmpty(), "German lesson_source messages must not be empty");
        assertEquals(new TreeSet<>(en.keySet()), new TreeSet<>(de.keySet()),
                "Keys in English and German lesson_source message packs must match exactly");
    }

    @Test
    @DisplayName("contains the specification keys of the choice, premise, claim and retirement")
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "lesson_source");

        var requiredKeys = new String[]{
                "lesson_source.choose.title",
                "lesson_source.choose.empty",
                "lesson_source.choose.newest_first",
                "lesson_source.list.recorded",
                "lesson_source.list.recorded_by",
                "lesson_source.list.cites",
                "lesson_source.premise.holds",
                "lesson_source.premise.broken",
                "lesson_source.premise.unavailable",
                "lesson_source.check.title",
                "lesson_source.check.holds",
                "lesson_source.check.broken",
                "lesson_source.check.unavailable",
                "lesson_source.evidence.file",
                "lesson_source.evidence.symbol",
                "lesson_source.evidence.line",
                "lesson_source.broken.title",
                "lesson_source.broken.choice",
                "lesson_source.broken.use_anyway",
                "lesson_source.broken.use_anyway_hint",
                "lesson_source.broken.retire",
                "lesson_source.broken.retire_hint",
                "lesson_source.broken.remove",
                "lesson_source.broken.remove_hint",
                "lesson_source.claimed",
                "lesson_source.refused",
                "lesson_source.refused.claimed_elsewhere",
                "lesson_source.refused.retired",
                "lesson_source.refused.not_found",
                "lesson_source.refused.premise",
                "lesson_source.retired",
                "lesson_source.unchanged",
                "lesson_source.not_found",
                "lesson_source.field.lesson",
                "lesson_source.field.use_anyway",
                "lesson_source.action.lesson_required",
                "lesson_source.retire.reason",
                "lesson_source.modal.title",
                "lesson_source.picker.title",
                "lesson_source.picker.empty",
                "lesson_source.picker.select",
                "lesson_source.picker.checking",
                "lesson_source.premise.title",
                "lesson_source.premise.explain",
                "lesson_source.premise.evidence_title",
                "lesson_source.premise.use_anyway",
                "lesson_source.premise.retire",
                "lesson_source.premise.cancel",
                "lesson_source.premise.nothing_changed",
                "lesson_source.error"
        };

        for (var key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in lesson_source.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("is part of the supported areas so both clients can load it")
    void shouldBePartOfTheSupportedAreas() {
        assertTrue(MessageCatalogue.SUPPORTED_AREAS.contains("lesson_source"),
                "The lesson_source area must be registered so both clients can load it");
    }

    @Test
    @DisplayName("formats the parameterized and pluralized lesson source messages")
    void shouldFormatParameterizedLessonSourceMessages() {
        assertEquals("cites 3 files or symbols",
                MessageCatalogue.format("en", "lesson_source.list.cites", Map.of("count", 3)));
        assertEquals("cites 1 file or symbol",
                MessageCatalogue.format("en", "lesson_source.list.cites", Map.of("count", 1)));

        assertEquals("les-0007 is claimed by fix-relay",
                MessageCatalogue.format("en", "lesson_source.claimed",
                        Map.of("lesson", "les-0007", "plan", "fix-relay")));

        assertEquals("les-0007 is retired",
                MessageCatalogue.format("en", "lesson_source.retired", Map.of("lesson", "les-0007")));

        var evidence = MessageCatalogue.format("en", "lesson_source.evidence.line",
                Map.of("kind", "file", "name", "src/Old.java", "path", "src/Old.java", "commit", "none"));
        assertTrue(evidence.contains("src/Old.java"), evidence);

        assertEquals("les-0007: die Voraussetzung ist gebrochen.",
                MessageCatalogue.format("de", "lesson_source.check.broken", Map.of("lesson", "les-0007")));
    }
}
