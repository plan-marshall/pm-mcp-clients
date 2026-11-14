/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The keys of the answer dialogue follow the interface language, and an input the prompt does not
 * offer is answered with one line and the same prompt, never taken as another answer
 * ({@code doc/design/terminal-client/03-dialogues.adoc#keys}: {@code decision.answer} English
 * {@code w m h x l}, German {@code w n t x s}; a waiver request English {@code y d l}, German
 * {@code w a s}; back {@code b}/{@code z}; leave {@code q}/{@code v}; the read-back English
 * {@code e o}, German {@code b o}). Every scripted input ends, so a dialogue that waits for more
 * reads the end of its input and leaves.
 */
@DisplayName("AnswerCommand takes the keys its prompts offer, in the interface language")
@EnableGeneratorController
class AnswerKeysTest {

    private static final String NOT_OFFERED = "Please choose a valid option or action.";
    private static final String NOT_OFFERED_DE = "Bitte wähle eine gültige Option oder Aktion.";

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private StringWriter terminalText;
    private final List<String> acts = new CopyOnWriteArrayList<>();
    private String reading;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        out = new StringWriter();
        err = new StringWriter();
        terminalText = new StringWriter();
        reading = Generators.letterStrings(6, 16).next();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Nested
    @DisplayName("a waiver request")
    class Waiver {

        private String reason;

        @BeforeEach
        void serveWaiver() throws IOException {
            reason = Generators.letterStrings(8, 24).next();
            serve("WR-7", """
                                {"id": "WR-7", "is_waiver": true, "subject": "plan session-log", "plan": "session-log",
                                 "waiver_gate": "coverage:pm-api", "waiver_reason": "generated classes"}
                                """);
        }

        @ParameterizedTest(name = "{0}: {1} sends {2}")
        @CsvSource({"en, y, waive", "en, d, decline", "de, w, waive", "de, a, decline"})
        @DisplayName("the act keys of the language send the act with the reason")
        void actKeysOfTheLanguage(String locale, String key, String act) {
            var code = run(locale, key + "\n" + reason + "\n", "answer", "WR-7");

            assertEquals(0, code, err.toString());
            assertEquals(1, acts.size(), acts.toString());
            var body = body(0);
            assertEquals(act, body.get("action"), acts.toString());
            assertEquals(reason, body.get("reason"), acts.toString());
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource({"en, w", "en, a", "en, waive", "en, decline", "en, later", "en, v", "de, y", "de, d", "de, g",
                            "de, l", "de, später", "de, q"})
        @DisplayName("a key the prompt does not offer is answered with the keys it offers and sends nothing")
        void keyNotOffered(String locale, String key) {
            var code = run(locale, key + "\n", "answer", "WR-7");

            var expected = "de".equals(locale) ? "Gib w, a oder s ein." : "Type y, d or l.";
            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertTrue(out.toString().contains(expected), out.toString()),
                                () -> assertTrue(acts.isEmpty(), acts.toString()),
                                () -> assertFalse(out.toString().contains("Waived"), out.toString()),
                                () -> assertFalse(out.toString().contains("gewährt"), out.toString()));
        }

        @Test
        @DisplayName("the German prompt names the German keys")
        void germanPromptNamesItsKeys() {
            run("de", "s\n", "answer", "WR-7");

            assertTrue(terminalText.toString().contains("w) gewähren, mit Grund   a) ablehnen, mit Grund   s) später"),
                                terminalText.toString());
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource({"en, q, Left WR-7. Nothing was sent.", "de, v, Verlassen: WR-7. Es wurde nichts gesendet."})
        @DisplayName("the leave key of the language leaves and sends nothing")
        void leaveKeyOfTheLanguage(String locale, String key, String expected) {
            var code = run(locale, key + "\n", "answer", "WR-7");

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains(expected), out.toString());
            assertTrue(acts.isEmpty(), acts.toString());
        }
    }

    @Nested
    @DisplayName("the answer of a decision")
    class Answer {

        @BeforeEach
        void serveDecision() throws IOException {
            serve("Q-21", twoParts("reversible"));
        }

        @Test
        @DisplayName("the German note key is n, and m is not offered")
        void germanNoteKey() {
            var note = Generators.letterStrings(6, 20).next();

            var code = run("de", "m\nn\n" + note + "\n1\n1\n\n", "answer", "Q-21");

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains(NOT_OFFERED_DE), out.toString());
            assertTrue(terminalText.toString().contains("n) Notiz hinzufügen"), terminalText.toString());
            assertTrue(terminalText.toString().contains("n) ✎ Notiz"), terminalText.toString());
            assertEquals(1, acts.size(), acts.toString());
            assertEquals(note, body(0).get("note"), acts.toString());
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource({"en, z", "en, back", "de, b", "de, zurück"})
        @DisplayName("a back key of the other language, or the word, is not offered at the second part")
        void backKeyNotOffered(String locale, String key) {
            var code = run(locale, "2\n" + key + "\n1\n\n", "answer", "Q-21");

            var parts = answers();
            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertTrue(out.toString().contains("de".equals(locale) ? NOT_OFFERED_DE : NOT_OFFERED),
                                                    out.toString()),
                                () -> assertEquals("2", parts.get("1"), acts.toString()),
                                () -> assertEquals("1", parts.get("2"), acts.toString()));
        }

        @Test
        @DisplayName("the German back key z returns to the first part")
        void germanBackKey() {
            var code = run("de", "2\nz\n1\n2\n\n", "answer", "Q-21");

            var parts = answers();
            assertEquals(0, code, err.toString());
            assertEquals("1", parts.get("1"), acts.toString());
            assertEquals("2", parts.get("2"), acts.toString());
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource({"en, later", "en, none fits", "en, s", "en, v", "de, l", "de, nichts passt", "de, q"})
        @DisplayName("the words of a key, and the keys of the other language, answer nothing and send nothing")
        void wordsAndForeignKeysAreNotOffered(String locale, String input) {
            var code = run(locale, input + "\n", "answer", "Q-21");

            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertTrue(out.toString().contains("de".equals(locale) ? NOT_OFFERED_DE : NOT_OFFERED),
                                                    out.toString()),
                                () -> assertFalse(out.toString().contains("stays open"), out.toString()),
                                () -> assertFalse(out.toString().contains("bleibt offen"), out.toString()),
                                () -> assertTrue(acts.isEmpty(), acts.toString()));
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource({"en, l, Q-21 stays open", "de, s, Q-21 bleibt offen", "en, q, Left Q-21",
                            "de, v, Verlassen: Q-21"})
        @DisplayName("later and leave take the key of the language and send nothing")
        void laterAndLeaveKeys(String locale, String key, String expected) {
            var code = run(locale, key + "\n", "answer", "Q-21");

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains(expected), out.toString());
            assertTrue(acts.isEmpty(), acts.toString());
        }
    }

    @Nested
    @DisplayName("the confirmations")
    class Confirmations {

        @Test
        @DisplayName("decision.confirm takes Enter, shows the evidence on ?, and answers a not offered key")
        void costlyConfirm() throws Exception {
            serve("Q-22", single("Q-22", "costly", "{\"file\": \"Relay.java\"}"));

            var code = run("en", "1\ny\n?\n\n", "answer", "Q-22");

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains(NOT_OFFERED), out.toString());
            assertTrue(out.toString().contains("Evidence details: Relay.java"), out.toString());
            assertEquals(1, acts.size(), acts.toString());
            assertEquals("1", body(0).get("option"), acts.toString());
        }

        @Test
        @DisplayName("back at decision.confirm asks the part again, and the new choice is sent")
        void costlyConfirmBack() throws Exception {
            serve("Q-22", single("Q-22", "costly", "null"));

            var code = run("en", "1\nb\n2\n\n", "answer", "Q-22");

            assertEquals(0, code, err.toString());
            assertEquals(1, acts.size(), acts.toString());
            assertEquals("2", body(0).get("option"), acts.toString());
        }

        @Test
        @DisplayName("leave at decision.confirm sends nothing")
        void costlyConfirmLeave() throws Exception {
            serve("Q-22", single("Q-22", "irreversible", "null"));

            var code = run("de", "1\nv\n", "answer", "Q-22");

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains("Verlassen: Q-22"), out.toString());
            assertTrue(acts.isEmpty(), acts.toString());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"y", "j", "send"})
        @DisplayName("none fits is sent on Enter alone; another input is not offered")
        void noneFitsConfirm(String input) throws Exception {
            serve("Q-22", single("Q-22", "reversible", "null"));

            var code = run("en", "x\n" + input + "\n\n", "answer", "Q-22");

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains(NOT_OFFERED), out.toString());
            assertTrue(out.toString().contains("you asked the plan to think again"), out.toString());
            assertEquals(1, acts.size(), acts.toString());
            assertTrue(acts.get(0).startsWith("/api/v1/decisions/Q-22/withdraw"), acts.toString());
        }

        @Test
        @DisplayName("back at the none-fits confirmation returns to the answer and withdraws nothing")
        void noneFitsBack() throws Exception {
            serve("Q-22", single("Q-22", "reversible", "null"));

            var code = run("de", "x\nz\ns\n", "answer", "Q-22");

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains("Q-22 bleibt offen"), out.toString());
            assertTrue(acts.isEmpty(), acts.toString());
        }
    }

    @Nested
    @DisplayName("the read-back of own words")
    class ReadBack {

        @BeforeEach
        void serveDecision() throws IOException {
            serve("Q-23", single("Q-23", "reversible", "null"));
        }

        @Test
        @DisplayName("y is not offered at the read-back, ? repeats what was understood, and Enter records the words")
        void enterConfirms() {
            var words = Generators.letterStrings(6, 20).next();

            var code = run("en", "w\n" + words + "\ny\n?\n\n", "answer", "Q-23");

            var understood = "I understood: " + reading;
            var rendered = out.toString();
            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertTrue(rendered.contains(NOT_OFFERED), rendered),
                                () -> assertTrue(rendered.indexOf(understood) < rendered.lastIndexOf(understood), rendered),
                                () -> assertEquals(1, acts.size(), acts.toString()),
                                () -> assertEquals(reading, body(0).get("words"), acts.toString()));
        }

        @Test
        @DisplayName("o chooses an option instead: the words are not recorded and the option is sent")
        void chooseAnOptionInstead() {
            var code = run("en", "w\n" + Generators.letterStrings(6, 20).next() + "\no\n2\n", "answer", "Q-23");

            assertEquals(0, code, err.toString());
            assertEquals(1, acts.size(), acts.toString());
            assertEquals("2", body(0).get("option"), acts.toString());
            assertFalse(acts.get(0).contains("\"words\""), acts.toString());
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource({"en, e", "de, b"})
        @DisplayName("the edit key of the language asks for the words again")
        void editKeyOfTheLanguage(String locale, String key) {
            var first = Generators.letterStrings(6, 20).next();
            var second = Generators.letterStrings(6, 20).next();

            var code = run(locale, "w\n" + first + "\n" + key + "\n" + second + "\n\n", "answer", "Q-23");

            assertEquals(0, code, err.toString());
            assertEquals(1, acts.size(), acts.toString());
            assertEquals(reading, body(0).get("words"), acts.toString());
        }
    }

    private void serve(String id, String envelope) throws IOException {
        fixture.start((request, response) -> {
            var target = request.target();
            var decision = "/api/v1/decisions/" + id;
            if (decision.equals(target) && "GET".equals(request.method())) {
                response.json(200, envelope);
            } else if ((decision + "/interpret").equals(target)) {
                response.json(200, JsonTree.write(Map.of("interpretation_type", "option",
                                    "read_back_text", reading, "understood", true)));
            } else if (target.startsWith(decision + "/") && "POST".equals(request.method())) {
                acts.add(target + " " + request.bodyText());
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
    }

    private Map<String, Object> body(int index) {
        var act = acts.get(index);
        try {
            var body = JsonTree.asObject(JsonTree.parse(act.substring(act.indexOf(' ') + 1)));
            assertNotNull(body, act);
            return body;
        } catch (IOException e) {
            throw new UncheckedIOException(act, e);
        }
    }

    private Map<String, Object> answers() {
        assertEquals(1, acts.size(), acts.toString());
        var parts = JsonTree.asObject(body(0).get("part_answers"));
        assertNotNull(parts, acts.toString());
        return parts;
    }

    private static String twoParts(String reversibility) {
        return """
                            {"id": "Q-21", "title": "Store", "subject": "plan storage-v2", "scope": "plan",
                             "blocks": "plan", "reversibility": "%s", "plan": "storage-v2",
                             "parts": [
                               {"part_id": "1", "question": "Where?", "allow_note": true,
                                "options": [{"key": "1", "label": "A file"}, {"key": "2", "label": "A database"}]},
                               {"part_id": "2", "question": "How?",
                                "options": [{"key": "1", "label": "At start"}, {"key": "2", "label": "By a verb"}]}
                             ]}
                            """.formatted(reversibility);
    }

    private static String single(String id, String reversibility, String evidence) {
        return """
                            {"id": "%s", "title": "Endpoints", "subject": "plan drop-api", "scope": "plan",
                             "blocks": "plan", "reversibility": "%s", "plan": "drop-api", "evidence": %s,
                             "parts": [
                               {"part_id": "1", "question": "Delete them?", "allow_own_words": true,
                                "options": [{"key": "1", "label": "Delete"}, {"key": "2", "label": "Keep"}]}
                             ]}
                            """.formatted(id, reversibility, evidence);
    }

    private int run(String locale, String input, String... args) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de".equals(locale) ? "de_DE.UTF-8" : "en_US.UTF-8");
        var terminalBytes = new ByteArrayOutputStream();
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var context = new OperatorContext(environment, tempDir, true, ClientEnvironment.capture(environment),
                            () -> new DumbTerminal(inStream, terminalBytes), null);
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
        terminalText.write(terminalBytes.toString(StandardCharsets.UTF_8));
        return code;
    }
}
