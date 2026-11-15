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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * The document is the specification, so the test reads it rather than repeating it: a key the document
 * gains fails here until the packs carry it, and a key a pack carries that the document does not name
 * fails too.
 */
@DisplayName("IntentCatalogueTest")
class IntentCatalogueTest {

    /** The texts and verdict documents, from the module's own directory. */
    private static final Path DOCUMENT = Path.of("", "..", "doc", "design", "shared",
            "intent-and-outline-texts.adoc").toAbsolutePath().normalize();

    /** One key row of a table, as {@code |`intent.tab.title`}. */
    private static final Pattern KEY_ROW = Pattern.compile("^\\|`([a-z0-9_.]+)`$");

    /** One argument name of an ICU message, as {@code {name}} or the head of {@code {name, type, …}}. */
    private static final Pattern ARGUMENT = Pattern.compile("\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*[,}]");

    @Test
    @DisplayName("should carry exactly the document's keys in English and German")
    void shouldCarryExactlyTheDocumentKeys() throws Exception {
        var documented = documentTexts();
        var intentKeys = keyFamily(documented, "intent.");

        assertTrue(Files.isReadable(DOCUMENT), "The texts document must be readable: " + DOCUMENT);
        assertFalse(intentKeys.isEmpty(), "The document names the area's keys");
        assertEquals(intentKeys, new TreeSet<>(MessageCatalogue.getMessages("en", "intent").keySet()),
                "The English pack must carry exactly the document's keys");
        assertEquals(intentKeys, new TreeSet<>(MessageCatalogue.getMessages("de", "intent").keySet()),
                "The German pack must carry exactly the document's keys");
    }

    @Test
    @DisplayName("should translate every key as the document writes it, in both languages")
    void shouldTranslateEveryKeyAsTheDocumentWritesIt() throws Exception {
        var en = MessageCatalogue.getMessages("en", "intent");
        var de = MessageCatalogue.getMessages("de", "intent");
        var mismatches = new TreeSet<String>();

        documentTexts().forEach((key, texts) -> {
            if (!key.startsWith("intent.")) {
                return;
            }
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
    void shouldAgreeOnEveryArgument() throws Exception {
        var mismatches = new TreeSet<String>();

        documentTexts().forEach((key, texts) -> {
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
    @DisplayName("should carry the document's verdict words in common, in both languages")
    void shouldCarryTheSevenVerdictWords() throws Exception {
        var en = MessageCatalogue.getMessages("en", "common");
        var de = MessageCatalogue.getMessages("de", "common");
        var words = new LinkedHashMap<String, String[]>();
        documentTexts().forEach((key, texts) -> {
            if (key.startsWith("state.criterion.") || key.startsWith("state.check.")) {
                words.put(key, texts);
            }
        });

        assertEquals(7, words.size(), "The document names seven verdict words");
        var mismatches = new TreeSet<String>();
        words.forEach((key, texts) -> {
            assertNotNull(en.get(key), "English verdict word of " + key);
            assertNotNull(de.get(key), "German verdict word of " + key);
            if (!texts[0].equals(en.get(key)) || !texts[1].equals(de.get(key))) {
                mismatches.add(key + ": en <" + en.get(key) + ">, de <" + de.get(key) + ">");
            }
        });
        assertTrue(mismatches.isEmpty(), "The verdict words must be as the document writes them: " + mismatches);
    }

    /**
     * @return every table row of the document, by key, the English at index 0 and the German at 1
     * @throws IOException if the document cannot be read
     */
    private static Map<String, String[]> documentTexts() throws IOException {
        var lines = Files.readAllLines(DOCUMENT, StandardCharsets.UTF_8);
        var rows = new LinkedHashMap<String, String[]>();
        for (var index = 0; index < lines.size(); index++) {
            Matcher keyRow = KEY_ROW.matcher(lines.get(index).strip());
            if (keyRow.matches()) {
                rows.put(keyRow.group(1), new String[]{valueAfter(lines, index + 1),
                        valueAfter(lines, index + 2)});
            }
        }
        return rows;
    }

    /**
     * @param lines  the document's lines
     * @param from   the line after the key row
     * @return the next table cell's text, in either language
     */
    private static String valueAfter(List<String> lines, int from) {
        for (var index = from; index < lines.size(); index++) {
            var line = lines.get(index).strip();
            if (line.startsWith("|") && !"|===".equals(line)) {
                return line.substring(1).strip();
            }
        }
        return "";
    }

    /**
     * @param rows   the document's rows
     * @param prefix the key prefix of one family
     * @return that family's keys, sorted
     */
    private static TreeSet<String> keyFamily(Map<String, String[]> rows, String prefix) {
        var keys = new TreeSet<String>();
        rows.keySet().forEach(key -> {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        });
        return keys;
    }

    /**
     * @param template an ICU message
     * @return the argument names it reads, in the order they appear
     */
    private static TreeSet<String> argumentsOf(String template) {
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
