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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the texts of the intent and outline design (design {@code intent-and-outline-texts.adoc}):
 * the area {@code intent} carries every key of the document in English and German, with the same
 * arguments, and the seven verdict words of the document's verdict table live in {@code common}.
 * <p>
 * The document is the specification, so the table below is its own key list. A key the document
 * carries and a pack lacks fails here, and so does a pack key the document does not name.
 */
@DisplayName("IntentCatalogueTest")
class IntentCatalogueTest {

    /** The document's key list: one row per key, as {@code key|English|German}. */
    private static final String TEXTS = """
            intent.tab.title|Intent|Absicht
            intent.plan.none|Refine has not written the intent yet.|Refine hat die Absicht noch nicht geschrieben.
            intent.epic.none|This epic has no intent record.|Dieses Epic hat keinen Absichtsdatensatz.
            intent.head.title|Intent \u00b7 {plan} \u00b7 revision {revision}|Absicht \u00b7 {plan} \u00b7 Revision {revision}
            intent.head.goal|Goal|Ziel
            intent.head.criteria|{criteria, plural, one {# criterion} other {# criteria}}|{criteria, plural, one {# Kriterium} other {# Kriterien}}
            intent.head.count|{count} {state}|{count} {state}
            intent.criteria.title|Criteria|Kriterien
            intent.criteria.satisfied_by|satisfied by {deliverables}|erf\u00fcllt durch {deliverables}
            intent.criteria.no_deliverable|no deliverable yet|noch kein Deliverable
            intent.criteria.reason|Reason: {reason}|Grund: {reason}
            intent.non_goals.title|Non-goals|Nicht-Ziele
            intent.choices.title|Choices|Festlegungen
            intent.decider.operator|you decided|du hast entschieden
            intent.decider.quorum|a quorum decided|ein Quorum hat entschieden
            intent.decider.role|the model's call|die Wahl des Modells
            intent.decider.fact|settled by a fact|durch einen Fakt gekl\u00e4rt
            intent.choice.options|options weighed: {options}|abgewogene Optionen: {options}
            intent.choice.question|answers {question}|beantwortet {question}
            intent.choice.adr|{relation, select, follows {follows {adr}} other {contradicts {adr}}}|{relation, select, follows {folgt {adr}} other {widerspricht {adr}}}
            intent.record.evidence|evidence: {evidence}|Beleg: {evidence}
            intent.record.changed|the record changed|der Datensatz hat sich ge\u00e4ndert
            intent.open_questions.title|Open questions|Offene Fragen
            intent.deferred.title|Deferred|Zur\u00fcckgestellt
            intent.deferred.reason|because: {reason}|weil: {reason}
            intent.builder.title|Builder check|Builder-Pr\u00fcfung
            intent.builder.sort.answered|answered by the record|vom Datensatz beantwortet
            intent.builder.sort.minor|the builder's call|die Wahl des Builders
            intent.builder.sort.operator|needs you|braucht dich
            intent.builder.recommended|recommended: {answer}|empfohlen: {answer}
            intent.builder.resolution.open|open|offen
            intent.builder.resolution.answered|answered: {answer}|beantwortet: {answer}
            intent.builder.resolution.deferred|deferred: the plan continues without it|zur\u00fcckgestellt: der Plan l\u00e4uft ohne sie weiter
            intent.builder.state.pending|questions wait|Fragen warten
            intent.builder.state.answered|resolved|gekl\u00e4rt
            intent.builder.state.not_run|not run: {reason}|nicht ausgef\u00fchrt: {reason}
            intent.outline.title|Outline|Outline
            intent.outline.deliverable|{number} \u00b7 {title}|{number} \u00b7 {title}
            intent.outline.files|Affected files|Betroffene Dateien
            intent.outline.file.read|read|gelesen
            intent.outline.file.write_new|new|neu
            intent.outline.file.write_replace|replaced|ersetzt
            intent.outline.file.write_conditional|changed on a condition|unter einer Bedingung ge\u00e4ndert
            intent.outline.file.delete|deleted|gel\u00f6scht
            intent.outline.depends_on|depends on {deliverables}|h\u00e4ngt ab von {deliverables}
            intent.outline.missing_deliverable|missing deliverable {number}|fehlendes Deliverable {number}
            intent.outline.satisfies|satisfies {criteria}|erf\u00fcllt {criteria}
            intent.outline.satisfies_none|satisfies no criterion|erf\u00fcllt kein Kriterium
            intent.outline.verification|Verification|Nachweis
            intent.outline.dependencies|Added dependencies|Hinzugef\u00fcgte Abh\u00e4ngigkeiten
            intent.outline.dependency|{coordinates} {version} \u00b7 added by {deliverable}: {justification}|{coordinates} {version} \u00b7 hinzugef\u00fcgt von {deliverable}: {justification}
            intent.epic.destination|Destination|Endziel
            intent.epic.done_when|Done when|Fertig, wenn
            intent.epic.not_specified|Not yet specified|Noch nicht festgelegt
            intent.epic.plans|Each plan's done-when|Fertig-wenn jedes Plans
            intent.epic.review|Completion review|Abschlusspr\u00fcfung
            intent.oversize.title|{role} \u00b7 over_size {current}x \u2192 {proposed}x|{role} \u00b7 over_size {current}x \u2192 {proposed}x
            intent.oversize.title_current|{role} \u00b7 over_size {current}x|{role} \u00b7 over_size {current}x
            intent.oversize.empty|No measured tasks of this role yet.|Noch keine gemessenen Tasks dieser Role.
            intent.oversize.bucket|{from}\u2013{to}x|{from}\u2013{to}x
            intent.oversize.bucket_open|{from}x and more|{from}x und mehr
            intent.oversize.tasks|{count, plural, one {# task} other {# tasks}}|{count, plural, one {# Task} other {# Tasks}}
            intent.oversize.flagged|{count} flagged|{count} markiert
            intent.oversize.share|{tasks, plural, one {# task} other {# tasks}} \u00b7 {flagged} flagged \u00b7 {percent} %|{tasks, plural, one {# Task} other {# Tasks}} \u00b7 {flagged} markiert \u00b7 {percent} %
            intent.oversize.direction|{direction, select, lower {{current}x \u2192 {proposed}x lowers the multiple and can flag more} other {{current}x \u2192 {proposed}x raises the multiple and can flag fewer}}|{direction, select, lower {{current}x \u2192 {proposed}x senkt den Faktor und kann mehr markieren} other {{current}x \u2192 {proposed}x hebt den Faktor und kann weniger markieren}}
            """;

    /** The verdict words of the document's verdict table: {@code key|English|German}. */
    private static final String VERDICTS = """
            state.criterion.met|met|erf\u00fcllt
            state.criterion.not_met|not met|nicht erf\u00fcllt
            state.criterion.not_verified|not verified|nicht verifiziert
            state.criterion.not_reviewed|not reviewed yet|noch nicht bewertet
            state.check.pending|pending|ausstehend
            state.check.met|met|erf\u00fcllt
            state.check.not_met|not met|nicht erf\u00fcllt
            """;

    /** One argument name of an ICU message, as {@code {name}} or the head of {@code {name, type, …}}. */
    private static final Pattern ARGUMENT = Pattern.compile("\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*[,}]");

    @Test
    @DisplayName("should carry exactly the documents' keys in English and German")
    void shouldCarryExactlyTheDocumentKeys() {
        var expected = new TreeSet<>(parse(TEXTS).keySet());

        assertEquals(65, expected.size(), "The texts document names 65 intent keys");
        assertEquals(expected, new TreeSet<>(MessageCatalogue.getMessages("en", "intent").keySet()),
                "The English pack must carry exactly the document's keys");
        assertEquals(expected, new TreeSet<>(MessageCatalogue.getMessages("de", "intent").keySet()),
                "The German pack must carry exactly the document's keys");
        assertFalse(MessageCatalogue.getMessages("en", "intent").isEmpty(),
                "The area intent must be served, not silently unsupported");
    }

    @Test
    @DisplayName("should translate every key as the document writes it, in both languages")
    void shouldTranslateEveryKeyAsTheDocumentWritesIt() {
        var en = MessageCatalogue.getMessages("en", "intent");
        var de = MessageCatalogue.getMessages("de", "intent");
        var mismatches = new TreeSet<String>();

        parse(TEXTS).forEach((key, texts) -> {
            if (!texts[0].equals(en.get(key))) {
                mismatches.add("en " + key + ": expected <" + texts[0] + "> but was <" + en.get(key) + ">");
            }
            if (!texts[1].equals(de.get(key))) {
                mismatches.add("de " + key + ": expected <" + texts[1] + "> but was <" + de.get(key) + ">");
            }
        });

        assertTrue(mismatches.isEmpty(), "Every text must be the document's own: " + mismatches);
    }

    @Test
    @DisplayName("should agree on the arguments of every key, one language with the other")
    void shouldAgreeOnEveryArgument() {
        var mismatches = new TreeSet<String>();

        parse(TEXTS).forEach((key, texts) -> {
            var english = argumentsOf(texts[0]);
            var german = argumentsOf(texts[1]);
            if (!english.equals(german)) {
                mismatches.add(key + ": en reads " + english + ", de reads " + german);
            }
        });

        assertTrue(mismatches.isEmpty(), "Both languages must carry the same arguments: " + mismatches);
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
        var lower = "fixed interval, exponential backoff";

        assertEquals("options weighed: " + lower,
                format("en", "intent.choice.options", Map.of("options", lower)));
        assertEquals("abgewogene Optionen: " + lower,
                format("de", "intent.choice.options", Map.of("options", lower)));
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
    @DisplayName("should format the outline's deliverables, files, dependencies, and the missing ones")
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
        assertEquals("de.cuioss:cui-http 2.1.0 \u00b7 added by 1: the retry adapter",
                format("en", "intent.outline.dependency", Map.of("coordinates", "de.cuioss:cui-http",
                        "version", "2.1.0", "deliverable", 1, "justification", "the retry adapter")));
    }

    @Test
    @DisplayName("should format the epic texts and the over_size distribution without recomputing them")
    void shouldFormatTheEpicAndOverSizeTexts() {
        assertEquals("implement \u00b7 over_size 3x \u2192 2x",
                format("en", "intent.oversize.title", Map.of("role", "implement", "current", 3, "proposed", 2)));
        assertEquals("implement \u00b7 over_size 3x",
                format("en", "intent.oversize.title_current", Map.of("role", "implement", "current", 3)));
        assertEquals("0\u20131x", format("en", "intent.oversize.bucket", Map.of("from", 0, "to", 1)));
        assertEquals("3x and more", format("en", "intent.oversize.bucket_open", Map.of("from", 3)));
        assertEquals("1 task", format("en", "intent.oversize.tasks", Map.of("count", 1)));
        assertEquals("4 tasks", format("en", "intent.oversize.tasks", Map.of("count", 4)));
        assertEquals("7 flagged", format("en", "intent.oversize.flagged", Map.of("count", 7)));
        assertEquals("20 tasks \u00b7 7 flagged \u00b7 35 %",
                format("en", "intent.oversize.share", Map.of("tasks", 20, "flagged", 7, "percent", 35)));
        assertEquals("20 Tasks \u00b7 7 markiert \u00b7 35 %",
                format("de", "intent.oversize.share", Map.of("tasks", 20, "flagged", 7, "percent", 35)));
        assertEquals("3x \u2192 2x lowers the multiple and can flag more",
                format("en", "intent.oversize.direction",
                        Map.of("direction", "lower", "current", 3, "proposed", 2)));
        assertEquals("3x \u2192 4x raises the multiple and can flag fewer",
                format("en", "intent.oversize.direction",
                        Map.of("direction", "higher", "current", 3, "proposed", 4)));
        assertEquals("Destination", format("en", "intent.epic.destination", Map.of()));
        assertEquals("Endziel", format("de", "intent.epic.destination", Map.of()));
        assertEquals("Fertig-wenn jedes Plans", format("de", "intent.epic.plans", Map.of()));
    }

    @Test
    @DisplayName("should carry the seven verdict words of the document in common, in both languages")
    void shouldCarryTheSevenVerdictWords() {
        var en = MessageCatalogue.getMessages("en", "common");
        var de = MessageCatalogue.getMessages("de", "common");
        var mismatches = new TreeSet<String>();

        parse(VERDICTS).forEach((key, texts) -> {
            assertEquals(texts[0], en.get(key), "English verdict word of " + key);
            assertEquals(texts[1], de.get(key), "German verdict word of " + key);
            if (!texts[0].equals(en.get(key)) || !texts[1].equals(de.get(key))) {
                mismatches.add(key);
            }
        });

        assertTrue(mismatches.isEmpty(), "The verdict words must be as the document writes them: " + mismatches);
        assertEquals(7, parse(VERDICTS).size(), "The document names seven verdict words");
        assertEquals(Set.of("state.criterion.met", "state.criterion.not_met", "state.criterion.not_verified",
                        "state.criterion.not_reviewed", "state.check.pending", "state.check.met",
                        "state.check.not_met"), parse(VERDICTS).keySet(),
                "The verdict family is state.criterion.* and state.check.*");
    }

    /**
     * @param table a table of {@code key|English|German} rows
     * @return the rows by key, the English at index 0 and the German at index 1
     */
    private static Map<String, String[]> parse(String table) {
        var rows = new LinkedHashMap<String, String[]>();
        for (var line : table.strip().split("\n")) {
            var parts = line.strip().split("\\|", -1);
            assertEquals(3, parts.length, "A row carries its key and both languages: " + line);
            rows.put(parts[0], new String[]{parts[1], parts[2]});
        }
        return rows;
    }

    /**
     * @param template an ICU message
     * @return the argument names it reads, in the order they appear
     */
    private static Set<String> argumentsOf(String template) {
        var names = new TreeSet<String>();
        Matcher matcher = ARGUMENT.matcher(template);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
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
