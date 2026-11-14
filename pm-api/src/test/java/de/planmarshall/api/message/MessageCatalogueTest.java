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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

@EnableGeneratorController
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

    @Test
    void shouldReturnEmptyMapForUnsupportedAreaWithoutCaching() {
        var result = MessageCatalogue.getMessages("en", "invalid_area_attack");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNameTheShownReportsInTheMarkAllTitle() {
        var en = MessageCatalogue.getMessages("en", "reports");
        var de = MessageCatalogue.getMessages("de", "reports");

        assertAll(
                () -> assertEquals(
                        "Mark every shown report read without a filter, only the matching ones with one;"
                                + " older reports stay unread",
                        en.get("report.action.mark_all_read.title")),
                () -> assertEquals(
                        "Markiere ohne Filter jeden gezeigten Bericht als gelesen, mit Filter nur die passenden;"
                                + " ältere Berichte bleiben ungelesen",
                        de.get("report.action.mark_all_read.title")));
    }

    @Test
    void shouldNameTheThemesInGermanAsTheDesignDoes() {
        var de = MessageCatalogue.getMessages("de", "common");

        assertAll(
                () -> assertEquals("Helles Theme", de.get("dev.theme.light")),
                () -> assertEquals("Dunkles Theme", de.get("dev.theme.dark")));
    }

    @Test
    @DisplayName("Names a link that opens a new tab and the pull request chip in both languages")
    void shouldCarryTheLinkAndPullRequestChipTexts() {
        var number = String.valueOf(Generators.integers(1, 999).next());

        assertAll(
                () -> assertEquals("opens in a new tab",
                        MessageCatalogue.getMessages("en", "common").get("common.link.new_tab")),
                () -> assertEquals("öffnet in einem neuen Tab",
                        MessageCatalogue.getMessages("de", "common").get("common.link.new_tab")),
                () -> assertEquals("PR #" + number,
                        MessageCatalogue.format("en", "common.pull_request.chip", Map.of("number", number))),
                () -> assertEquals("PR #" + number,
                        MessageCatalogue.format("de", "common.pull_request.chip", Map.of("number", number))));
    }

    @ParameterizedTest(name = "common.kind.{0} is ''{1}'' and ''{2}''")
    @DisplayName("Carries the seven kind words of texts-and-languages.adoc, kinds")
    @CsvSource({
            "project, project, Projekt",
            "epic, epic, Epic",
            "plan, plan, Plan",
            "side_job, side job, Nebenarbeit",
            "decision, decision, Entscheidung",
            "report, report, Bericht",
            "machine, machine, Maschine"
    })
    void shouldCarryTheKindWords(String kind, String english, String german) {
        var key = "common.kind." + kind;

        assertAll(
                () -> assertEquals(english, MessageCatalogue.getMessages("en", "common").get(key)),
                () -> assertEquals(german, MessageCatalogue.getMessages("de", "common").get(key)));
    }

    @ParameterizedTest(name = "phase.name.{0} is ''{0}'' in both languages")
    @DisplayName("Carries the six phase words of texts-and-languages.adoc, phases")
    @ValueSource(strings = {"init", "refine", "outline", "plan", "execute", "finalize"})
    void shouldCarryThePhaseWords(String phase) {
        var key = "phase.name." + phase;

        assertAll(
                () -> assertEquals(phase, MessageCatalogue.getMessages("en", "common").get(key)),
                () -> assertEquals(phase, MessageCatalogue.getMessages("de", "common").get(key)));
    }

    @ParameterizedTest(name = "role.name.{0} is ''{0}'' in both languages")
    @DisplayName("Carries the role words of texts-and-languages.adoc, phases and roles")
    @ValueSource(strings = {"implement", "architecture", "outline", "enrich", "decompose", "settings"})
    void shouldCarryTheRoleWords(String role) {
        var key = "role.name." + role;

        assertAll(
                () -> assertEquals(role, MessageCatalogue.getMessages("en", "common").get(key)),
                () -> assertEquals(role, MessageCatalogue.getMessages("de", "common").get(key)));
    }

    @Test
    @DisplayName("Carries the three marker words of components-tags.adoc, markers")
    void shouldCarryTheMarkerWords() {
        var en = MessageCatalogue.getMessages("en", "common");
        var de = MessageCatalogue.getMessages("de", "common");

        assertAll(
                () -> assertEquals("recommended", en.get("common.marker.recommended")),
                () -> assertEquals("empfohlen", de.get("common.marker.recommended")),
                () -> assertEquals("default", en.get("common.marker.default")),
                () -> assertEquals("Standard", de.get("common.marker.default")),
                () -> assertEquals("this one", en.get("common.marker.this_one")),
                () -> assertEquals("dieses Gerät", de.get("common.marker.this_one")));
    }

    @Test
    @DisplayName("no message pack has duplicate keys")
    void shouldHaveNoDuplicateKeysInAnyMessagePack() throws Exception {
        var duplicates = new ArrayList<String>();
        for (String area : MessageCatalogue.SUPPORTED_AREAS) {
            for (String locale : List.of("en", "de")) {
                var path = "messages/" + area + "." + locale + ".properties";
                try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
                    assertNotNull(stream, "Missing resource: " + path);
                    try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        var seen = new HashSet<String>();
                        String line;
                        int lineNo = 0;
                        while ((line = reader.readLine()) != null) {
                            lineNo++;
                            var trimmed = line.trim();
                            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                                continue;
                            }
                            int separatorIdx = -1;
                            for (int i = 0; i < trimmed.length(); i++) {
                                char c = trimmed.charAt(i);
                                if ((c == '=' || c == ':') && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                                    separatorIdx = i;
                                    break;
                                }
                            }
                            if (separatorIdx > 0) {
                                var key = trimmed.substring(0, separatorIdx).trim();
                                if (!seen.add(key)) {
                                    duplicates.add(path + ":" + lineNo + " duplicate key: " + key);
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(duplicates.isEmpty(), "Duplicate keys found in message packs:\n" + String.join("\n", duplicates));
    }
}
