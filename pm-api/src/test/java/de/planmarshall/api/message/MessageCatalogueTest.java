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

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageCatalogueTest {

    @Test
    void shouldExposeSupportedLocalesAndAreas() {
        var meta = MessageCatalogue.getAvailableLocalesAndAreas();
        assertNotNull(meta);
        assertTrue(meta.containsKey("locales"));
        assertTrue(meta.containsKey("areas"));
    }

    @Test
    void shouldLoadAllAreasForEnglishAndGerman() {
        var enAll = MessageCatalogue.getMessages("en", "all");
        var deAll = MessageCatalogue.getMessages("de", "all");

        assertFalse(enAll.isEmpty(), "English messages should not be empty");
        assertFalse(deAll.isEmpty(), "German messages should not be empty");

        // Completeness check required by module-structure.adoc:
        // A key missing in a pack fails the build, and a key in a pack and not in English fails it too.
        var enKeys = new TreeSet<>(enAll.keySet());
        var deKeys = new TreeSet<>(deAll.keySet());

        var missingInDe = new TreeSet<>(enKeys);
        missingInDe.removeAll(deKeys);

        var extraneousInDe = new TreeSet<>(deKeys);
        extraneousInDe.removeAll(enKeys);

        assertTrue(missingInDe.isEmpty(), "Keys missing in German translation: " + missingInDe);
        assertTrue(extraneousInDe.isEmpty(), "Keys in German but missing in English: " + extraneousInDe);
    }

    @Test
    void shouldVerifyAreaIsolation() {
        for (String area : MessageCatalogue.SUPPORTED_AREAS) {
            var enArea = MessageCatalogue.getMessages("en", area);
            var deArea = MessageCatalogue.getMessages("de", area);

            assertFalse(enArea.isEmpty(), "Area '" + area + "' must contain messages in en");
            assertFalse(deArea.isEmpty(), "Area '" + area + "' must contain messages in de");
            assertEquals(enArea.keySet(), deArea.keySet(), "Area '" + area + "' key sets must match between en and de");

            for (var entry : enArea.entrySet()) {
                assertFalse(entry.getValue().isBlank(), "Key '" + entry.getKey() + "' must not be blank in en");
            }
            for (var entry : deArea.entrySet()) {
                assertFalse(entry.getValue().isBlank(), "Key '" + entry.getKey() + "' must not be blank in de");
            }
        }
    }

    @Test
    void shouldFormatParameterizedMessages() {
        var formattedEn = MessageCatalogue.format("en", "web.pair.done.paired_as", Map.of("label", "Firefox"));
        assertEquals("Paired as Firefox", formattedEn);

        var formattedDe = MessageCatalogue.format("de", "web.pair.done.paired_as", Map.of("label", "Firefox"));
        assertEquals("Gekoppelt als Firefox", formattedDe);
    }

    @Test
    void shouldFallbackGracefullyForUnknownKeysAndLocales() {
        assertEquals("unknown.key", MessageCatalogue.format("en", "unknown.key", Map.of()));
        assertEquals("en", MessageCatalogue.normalizeLocale("fr"));
        assertEquals("de", MessageCatalogue.normalizeLocale("DE"));
    }
}
