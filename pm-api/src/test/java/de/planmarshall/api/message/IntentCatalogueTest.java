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

import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the texts of the intent and outline area: the area {@code intent} carries the same keys in
 * English and German, and the seven verdict words live in {@code common}. The catalogue is the source
 * of the texts (ADR 002), so the expectations are stated here as literals; the test reads no document.
 */
@DisplayName("IntentCatalogueTest")
class IntentCatalogueTest {

    @Test
    @DisplayName("the intent area carries equal, non-empty key sets in English and German")
    void shouldKeepTheIntentAreaKeySetsEqual() {
        var en = MessageCatalogue.getMessages("en", "intent");
        var de = MessageCatalogue.getMessages("de", "intent");

        assertAll(
                () -> assertFalse(en.isEmpty(), "The English intent pack must carry its texts"),
                () -> assertFalse(de.isEmpty(), "The German intent pack must carry its texts"),
                () -> assertEquals(new TreeSet<>(en.keySet()), new TreeSet<>(de.keySet()),
                        "The English and German intent packs must carry the same keys"));
    }

    @Test
    @DisplayName("common carries the seven verdict words in both languages")
    void shouldCarryTheSevenVerdictWords() {
        var en = MessageCatalogue.getMessages("en", "common");
        var de = MessageCatalogue.getMessages("de", "common");

        assertAll(
                () -> assertEquals("met", en.get("state.criterion.met")),
                () -> assertEquals("erf\u00fcllt", de.get("state.criterion.met")),
                () -> assertEquals("not met", en.get("state.criterion.not_met")),
                () -> assertEquals("nicht erf\u00fcllt", de.get("state.criterion.not_met")),
                () -> assertEquals("not verified", en.get("state.criterion.not_verified")),
                () -> assertEquals("nicht verifiziert", de.get("state.criterion.not_verified")),
                () -> assertEquals("not reviewed yet", en.get("state.criterion.not_reviewed")),
                () -> assertEquals("noch nicht bewertet", de.get("state.criterion.not_reviewed")),
                () -> assertEquals("pending", en.get("state.check.pending")),
                () -> assertEquals("ausstehend", de.get("state.check.pending")),
                () -> assertEquals("met", en.get("state.check.met")),
                () -> assertEquals("erf\u00fcllt", de.get("state.check.met")),
                () -> assertEquals("not met", en.get("state.check.not_met")),
                () -> assertEquals("nicht erf\u00fcllt", de.get("state.check.not_met")));
    }

    @Test
    @DisplayName("should format the head, the counts, and the criteria texts")
    void shouldFormatHeadCountsAndCriteria() {
        assertEquals("Intent \u00b7 fix-relay \u00b7 revision 3",
                format("en", "intent.head.title", Map.of("plan", "fix-relay", "revision", 3)));
        assertEquals("Absicht \u00b7 fix-relay \u00b7 Revision 3",
                format("de", "intent.head.title", Map.of("plan", "fix-relay", "revision", 3)));
        assertEquals("1 criterion", format("en", "intent.head.criteria", Map.of("criteria", 1)));
        assertEquals("4 criteria", format("en", "intent.head.criteria", Map.of("criteria", 4)));
        assertEquals("1 Kriterium", format("de", "intent.head.criteria", Map.of("criteria", 1)));
        assertEquals("2 Kriterien", format("de", "intent.head.criteria", Map.of("criteria", 2)));
        assertEquals("2 met", format("en", "intent.head.count", Map.of("count", 2, "state", "met")));
        assertEquals("satisfied by 1, 2",
                format("en", "intent.criteria.satisfied_by", Map.of("deliverables", "1, 2")));
        assertEquals("erf\u00fcllt durch 1, 2",
                format("de", "intent.criteria.satisfied_by", Map.of("deliverables", "1, 2")));
        assertEquals("Reason: it failed",
                format("en", "intent.criteria.reason", Map.of("reason", "it failed")));
        assertEquals("no deliverable yet", format("en", "intent.criteria.no_deliverable", Map.of()));
    }

    @Test
    @DisplayName("should format a choice, its options, its question, its ADR relation, and its evidence")
    void shouldFormatChoices() {
        var options = "fixed interval, exponential backoff";

        assertEquals("options weighed: " + options,
                format("en", "intent.choice.options", Map.of("options", options)));
        assertEquals("abgewogene Optionen: " + options,
                format("de", "intent.choice.options", Map.of("options", options)));
        assertEquals("answers Q-007", format("en", "intent.choice.question", Map.of("question", "Q-007")));
        assertEquals("follows ADR-0001",
                format("en", "intent.choice.adr", Map.of("relation", "follows", "adr", "ADR-0001")));
        assertEquals("contradicts ADR-0002",
                format("en", "intent.choice.adr", Map.of("relation", "contradicts", "adr", "ADR-0002")));
        assertEquals("folgt ADR-0001",
                format("de", "intent.choice.adr", Map.of("relation", "follows", "adr", "ADR-0001")));
        assertEquals("widerspricht ADR-0002",
                format("de", "intent.choice.adr", Map.of("relation", "contradicts", "adr", "ADR-0002")));
        assertEquals("evidence: pm://plans/fix-relay/report",
                format("en", "intent.record.evidence", Map.of("evidence", "pm://plans/fix-relay/report")));
        assertEquals("because: no endpoint is in scope",
                format("en", "intent.deferred.reason", Map.of("reason", "no endpoint is in scope")));
        assertEquals("weil: kein Endpunkt im Umfang liegt",
                format("de", "intent.deferred.reason", Map.of("reason", "kein Endpunkt im Umfang liegt")));
    }

    @Test
    @DisplayName("should format the builder check's sorts, its recommendation, its states, and its reason")
    void shouldFormatTheBuilderCheck() {
        assertEquals("needs you", format("en", "intent.builder.sort.operator", Map.of()));
        assertEquals("braucht dich", format("de", "intent.builder.sort.operator", Map.of()));
        assertEquals("the builder's call", format("en", "intent.builder.sort.minor", Map.of()));
        assertEquals("answered by the record", format("en", "intent.builder.sort.answered", Map.of()));
        assertEquals("recommended: 30 seconds",
                format("en", "intent.builder.recommended", Map.of("answer", "30 seconds")));
        assertEquals("answered: AC-002",
                format("en", "intent.builder.resolution.answered", Map.of("answer", "AC-002")));
        assertEquals("deferred: the plan continues without it",
                format("en", "intent.builder.resolution.deferred", Map.of()));
        assertEquals("not run: the builder check task was lost",
                format("en", "intent.builder.state.not_run",
                        Map.of("reason", "the builder check task was lost")));
        assertEquals("nicht ausgef\u00fchrt: die Builder-Pr\u00fcfung ging verloren",
                format("de", "intent.builder.state.not_run",
                        Map.of("reason", "die Builder-Pr\u00fcfung ging verloren")));
    }

    @Test
    @DisplayName("should format the outline's deliverables, files, depends-on, and the missing ones")
    void shouldFormatTheOutline() {
        assertEquals("1 \u00b7 Reconnect the relay",
                format("en", "intent.outline.deliverable", Map.of("number", 1, "title", "Reconnect the relay")));
        assertEquals("read", format("en", "intent.outline.file.read", Map.of()));
        assertEquals("changed on a condition",
                format("en", "intent.outline.file.write_conditional", Map.of()));
        assertEquals("gel\u00f6scht", format("de", "intent.outline.file.delete", Map.of()));
        assertEquals("depends on 1, 2",
                format("en", "intent.outline.depends_on", Map.of("deliverables", "1, 2")));
        assertEquals("missing deliverable 7",
                format("en", "intent.outline.missing_deliverable", Map.of("number", 7)));
        assertEquals("satisfies AC-001, AC-004",
                format("en", "intent.outline.satisfies", Map.of("criteria", "AC-001, AC-004")));
        assertEquals("satisfies no criterion", format("en", "intent.outline.satisfies_none", Map.of()));
    }

    @Test
    @DisplayName("should format the epic texts without recomputing them")
    void shouldFormatTheEpicTexts() {
        assertEquals("Destination", format("en", "intent.epic.destination", Map.of()));
        assertEquals("Endziel", format("de", "intent.epic.destination", Map.of()));
        assertEquals("Fertig-wenn jedes Plans", format("de", "intent.epic.plans", Map.of()));
    }

    /**
     * @param locale the locale
     * @param key    the message key
     * @param params the arguments
     * @return the formatted text
     */
    private static String format(String locale, String key, Map<String, ?> params) {
        return MessageCatalogue.format(locale, key, params);
    }
}
