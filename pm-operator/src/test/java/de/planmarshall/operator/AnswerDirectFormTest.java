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
 * {@code answer <id> [<option>]} as one shell command: the decision the request names is read
 * before anything is sent, and its id is the request's; the positional value is the decision's
 * option, and the acts are the same words in every language
 * ({@code doc/design/terminal-client/01-commands.adoc}). Nothing is sent for a value the request
 * does not offer.
 */
@DisplayName("AnswerCommand answers directly only the decision it was asked for")
@EnableGeneratorController
class AnswerDirectFormTest {

    private static final String REFUSED = "refused by the runtime";

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private final List<String> acts = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Nested
    @DisplayName("the id of the request is authoritative")
    class Identity {

        @ParameterizedTest(name = "answer Q-31 {0}")
        @ValueSource(strings = {"1", "later", "withdraw", "--no-input", "-i"})
        @DisplayName("a decision returned with another id is unreadable: the line is printed and nothing is sent")
        void otherIdIsRefused(String argument) throws Exception {
            var other = "Q-" + Generators.integers(100, 999).next();
            serve("Q-31", decision(other));

            var code = run("en", "1\n", "answer", "Q-31", argument);

            assertAll(
                                () -> assertEquals(PmOperator.EXIT_FAILURE, code),
                                () -> assertTrue(out.toString().contains(
                                                                        "Q-31 could not be read, so it is not known whether it has parts; nothing was sent."),
                                                    out.toString()),
                                () -> assertFalse((out.toString() + err).contains(other), out + " " + err),
                                () -> assertTrue(acts.isEmpty(), acts.toString()));
        }

        @Test
        @DisplayName("the German unreadable line names the id asked for")
        void otherIdIsRefusedInGerman() throws Exception {
            serve("Q-31", decision("Q-77"));

            var code = run("de", "", "answer", "Q-31");

            assertEquals(PmOperator.EXIT_FAILURE, code);
            assertTrue(out.toString().contains("Q-31 ließ sich nicht lesen, daher ist unbekannt, ob sie Teile hat"),
                                out.toString());
        }

        @ParameterizedTest(name = "answer Q-31 {0}")
        @ValueSource(strings = {"1", "later", "withdraw"})
        @DisplayName("a decision returned without an id is unreadable on every direct path and nothing is sent")
        void missingIdIsRefused(String argument) throws Exception {
            serve("Q-31", "{\"title\":\"" + Generators.letterStrings(4, 12).next() + "\"}");

            var code = run("en", "", "answer", "Q-31", argument);

            assertEquals(PmOperator.EXIT_FAILURE, code);
            assertTrue(out.toString().contains(
                                                    "Q-31 could not be read, so it is not known whether it has parts; nothing was sent."),
                                out.toString());
            assertFalse(err.toString().contains("Missing id"), err.toString());
            assertTrue(acts.isEmpty(), acts.toString());
        }

        @ParameterizedTest(name = "answer Q-31 {0}")
        @ValueSource(strings = {"1", "later", "withdraw"})
        @DisplayName("a decision the runtime does not return is its refusal, and nothing is sent")
        void unknownDecisionIsRefused(String argument) throws Exception {
            fixture.start((request, response) -> {
                if ("POST".equals(request.method())) {
                    acts.add(request.target());
                }
                response.send(404, null, "");
            });

            var code = run("en", "", "answer", "Q-31", argument);

            assertEquals(PmOperator.EXIT_FAILURE, code);
            assertTrue(err.toString().contains(REFUSED), err.toString());
            assertTrue(acts.isEmpty(), acts.toString());
        }

        @Test
        @DisplayName("a failed read of the decision is unreadable, with no status shown, and is not read as a decision")
        void refusedReadIsTheRefusal() throws Exception {
            fixture.start((request, response) -> response.json(500, "{\"error\":\"internal_fault\"}"));

            var code = run("en", "", "answer", "Q-31");

            assertEquals(PmOperator.EXIT_FAILURE, code);
            assertTrue(out.toString().contains(
                                                    "Q-31 could not be read, so it is not known whether it has parts; nothing was sent."),
                                out.toString());
            assertFalse((out.toString() + err).contains("HTTP 500"), out + " " + err);
            assertFalse(err.toString().contains("Missing id"), err.toString());
        }
    }

    @Nested
    @DisplayName("the positional value is the decision's option")
    class Values {

        @ParameterizedTest(name = "option {0}")
        @ValueSource(strings = {"l", "x", "d", "y", "s"})
        @DisplayName("an option keyed like a dialogue key is answered as that option")
        void optionKeyedLikeADialogueKey(String key) throws Exception {
            serve("Q-32", decisionWithOption("Q-32", key));

            var code = run("de", "", "answer", "Q-32", key);

            assertEquals(0, code, err.toString());
            assertEquals(1, acts.size(), acts.toString());
            assertTrue(acts.get(0).startsWith("/api/v1/decisions/Q-32/answer"), acts.toString());
            var body = body(0);
            assertEquals(key, body.get("option"), acts.toString());
            assertFalse(body.containsKey("action"), acts.toString());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"l", "s", "x"})
        @DisplayName("a dialogue key is no act of the command line: it is sent as the option it names")
        void dialogueKeyIsNoAct(String key) throws Exception {
            serve("Q-33", decision("Q-33"));

            var code = run("de", "", "answer", "Q-33", key);

            assertEquals(0, code, err.toString());
            assertEquals(1, acts.size(), acts.toString());
            assertTrue(acts.get(0).startsWith("/api/v1/decisions/Q-33/answer"), acts.toString());
            assertEquals(key, body(0).get("option"), acts.toString());
            assertFalse(out.toString().contains("bleibt offen"), out.toString());
        }

        @Test
        @DisplayName("later is the same word in German and leaves the decision open")
        void laterInGerman() throws Exception {
            serve("Q-33", decision("Q-33"));

            var code = run("de", "", "answer", "Q-33", "later");

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains("Q-33 bleibt offen"), out.toString());
            assertEquals("later", body(0).get("action"), acts.toString());
        }

        @ParameterizedTest(name = "--action {0}")
        @ValueSource(strings = {"waive", "decline", "bogus"})
        @DisplayName("an act a decision does not offer is answered with one line, exit 3, and nothing is sent")
        void actNotOfferedOnADecision(String act) throws Exception {
            serve("Q-33", decision("Q-33"));

            var code = run("en", "", "answer", "Q-33", "--action", act, "--reason",
                                Generators.letterStrings(4, 12).next());

            assertEquals(3, code, err.toString());
            assertTrue(out.toString().contains("Please choose a valid option or action."), out.toString());
            assertTrue(acts.isEmpty(), acts.toString());
        }

        @Test
        @DisplayName("--action answer without an option or words has nothing to send")
        void answerWithoutAChoice() throws Exception {
            serve("Q-33", decision("Q-33"));

            var code = run("en", "", "answer", "Q-33", "--action", "answer");

            assertEquals(3, code, err.toString());
            assertTrue(out.toString().contains("Please choose a valid option or action."), out.toString());
            assertTrue(acts.isEmpty(), acts.toString());
        }

        @Test
        @DisplayName("--action answer with an option answers it with the option's label")
        void answerWithAnOption() throws Exception {
            serve("Q-33", decision("Q-33"));

            var code = run("en", "", "answer", "Q-33", "2", "--action", "answer");

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains("Q-33 answered: Keep"), out.toString());
            assertEquals("2", body(0).get("option"), acts.toString());
        }
    }

    @Nested
    @DisplayName("a waiver request answered directly")
    class Waiver {

        @ParameterizedTest(name = "{0}: answer WR-9 {1}")
        @CsvSource({"en, y", "en, d", "de, w", "de, a", "en, 1", "en, withdraw"})
        @DisplayName("a value that is no act of a waiver names the three acts and sends nothing")
        void valueNotOffered(String locale, String value) throws Exception {
            serve("WR-9", waiver());

            var code = run(locale, "", "answer", "WR-9", value, "--reason", Generators.letterStrings(4, 12).next());

            var expected = "de".equals(locale) ? "Gib waive, decline oder later ein." : "Type waive, decline or later.";
            assertAll(
                                () -> assertEquals(3, code, err.toString()),
                                () -> assertTrue(out.toString().contains(expected), out.toString()),
                                () -> assertTrue(acts.isEmpty(), acts.toString()));
        }

        @Test
        @DisplayName("own words do not answer a waiver request")
        void ownWordsNotOffered() throws Exception {
            serve("WR-9", waiver());

            var code = run("en", "", "answer", "WR-9", "--words", Generators.letterStrings(4, 12).next());

            assertEquals(3, code, err.toString());
            assertTrue(out.toString().contains("Type waive, decline or later."), out.toString());
            assertTrue(acts.isEmpty(), acts.toString());
        }

        @Test
        @DisplayName("decline in German declines with the reason, and names the plan of the request")
        void declineInGerman() throws Exception {
            serve("WR-9", waiver());
            var reason = Generators.letterStrings(6, 20).next();

            var code = run("de", "", "answer", "WR-9", "decline", "--reason", reason);

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains("WR-9 abgelehnt"), out.toString());
            var body = body(0);
            assertEquals("decline", body.get("action"), acts.toString());
            assertEquals(reason, body.get("reason"), acts.toString());
        }

        @Test
        @DisplayName("waive names the plan the request belongs to, not a fallback")
        void waiveNamesThePlan() throws Exception {
            serve("WR-9", waiver());

            var code = run("en", "", "answer", "WR-9", "--action", "WAIVE", "--reason",
                                Generators.letterStrings(6, 20).next());

            assertEquals(0, code, err.toString());
            assertTrue(out.toString().contains("for session-log."), out.toString());
            assertEquals("waive", body(0).get("action"), acts.toString());
        }
    }

    private void serve(String id, String envelope) throws IOException {
        fixture.start((request, response) -> {
            var target = request.target();
            var decision = "/api/v1/decisions/" + id;
            if (decision.equals(target) && "GET".equals(request.method())) {
                response.json(200, envelope);
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

    private static String decision(String id) {
        return """
                            {"id": "%s", "title": "Strategy", "subject": "plan fix-relay", "scope": "plan",
                             "blocks": "plan", "reversibility": "reversible", "plan": "fix-relay",
                             "parts": [{"part_id": "1", "question": "Which?",
                               "options": [{"key": "1", "label": "Rebase"}, {"key": "2", "label": "Keep"}]}]}
                            """.formatted(id);
    }

    private static String decisionWithOption(String id, String key) {
        return """
                            {"id": "%s", "title": "Strategy", "subject": "plan fix-relay", "scope": "plan",
                             "blocks": "plan", "reversibility": "reversible", "plan": "fix-relay",
                             "parts": [{"part_id": "1", "question": "Which?",
                               "options": [{"key": "1", "label": "Rebase"}, {"key": "%s", "label": "Keyed"}]}]}
                            """.formatted(id, key);
    }

    private static String waiver() {
        return """
                            {"id": "WR-9", "is_waiver": true, "subject": "plan session-log", "plan": "session-log",
                             "waiver_gate": "coverage:pm-api"}
                            """;
    }

    private int run(String locale, String input, String... args) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de".equals(locale) ? "de_DE.UTF-8" : "en_US.UTF-8");
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var context = new OperatorContext(environment, tempDir, true, ClientEnvironment.capture(environment),
                            () -> new DumbTerminal(inStream, new ByteArrayOutputStream()), null);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }
}
