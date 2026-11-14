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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Every response is checked before the dialogue prints success, and the nouns of the result lines
 * come from the envelope's scope, blocks, project and plan, never from its subject text.
 */
@DisplayName("AnswerCommand checks responses and takes its nouns from the envelope")
@EnableGeneratorController
class AnswerRefusalTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

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

    @Test
    @DisplayName("a withdraw the runtime refuses exits non-zero with the refusal")
    void refusedWithdraw() throws Exception {
        var failure = text();
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-001".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, planEnvelope());
            } else if ("/api/v1/decisions/Q-001/withdraw".equals(request.target())) {
                response.json(500, errorBody(failure));
            } else {
                response.send(404, null, "");
            }
        });

        var code = run("en", "x\n\n".getBytes(StandardCharsets.UTF_8), "answer", "Q-001", "-i");

        assertNotEquals(0, code);
        assertTrue(err.toString().contains("The runtime refused"), err.toString());
    }

    @Test
    @DisplayName("an own-words answer the runtime refuses exits non-zero with the refusal")
    void refusedOwnWordsAnswer() throws Exception {
        var words = text();
        var failure = text();
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, ownWordsEnvelope());
            } else if ("/api/v1/decisions/Q-001/interpret".equals(target)) {
                response.json(200, """
                                    {"interpretation_type":"option","read_back_text":"%s","understood":true}
                                    """.formatted(words));
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(500, errorBody(failure));
            } else {
                response.send(404, null, "");
            }
        });

        var code = run("en", ("w\n" + words + "\n\n").getBytes(StandardCharsets.UTF_8), "answer", "Q-001", "-i");

        assertNotEquals(0, code);
        assertTrue(err.toString().contains("The runtime refused"), err.toString());
    }

    @Test
    @DisplayName("answer Q-999 later checks that the decision exists")
    void laterOnUnknownDecision() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        var code = run("en", new byte[0], "answer", "Q-999", "later");

        assertEquals(1, code);
        assertTrue(err.toString().contains("This entry does not exist"), err.toString());
    }

    @Test
    @DisplayName("the German result lines select the noun of a plan, an epic and an entry")
    void germanNouns() throws Exception {
        var project = text();
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-PLAN".equals(target)) {
                response.json(200, envelope("Q-PLAN", "plan", "plan", "plan " + text(), project));
            } else if ("/api/v1/decisions/Q-EPIC".equals(target)) {
                response.json(200, envelope("Q-EPIC", "epic", "epic", "epic " + text(), project));
            } else if ("/api/v1/decisions/Q-ENTRY".equals(target)) {
                response.json(200, envelope("Q-ENTRY", "plan", "entry", "start in " + project, project));
            } else if (target.endsWith("/answer") || target.endsWith("/withdraw")) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        assertGerman("der Plan wartet darauf.", "answer", "Q-PLAN", "later");
        assertGerman("du hast den Plan gebeten, neu zu \u00fcberlegen.", "answer", "Q-PLAN", "withdraw");
        assertGerman("das Epic wartet darauf.", "answer", "Q-EPIC", "later");
        assertGerman("du hast das Epic gebeten, neu zu \u00fcberlegen.", "answer", "Q-EPIC", "withdraw");
        assertGerman("der Start in " + project + " wartet darauf.", "answer", "Q-ENTRY", "later");
        assertGerman("du hast den Start in " + project + " gebeten, neu zu \u00fcberlegen.", "answer", "Q-ENTRY", "withdraw");
    }

    @Test
    @DisplayName("the dissent names the voter whose reason is the summary, not the first voter")
    void dissentByMatchingSummary() throws Exception {
        var firstVoter = text();
        var dissenter = text();
        var firstReason = text();
        var summary = text();
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-001".equals(request.target())) {
                response.json(200, dissentEnvelope(firstVoter, firstReason, dissenter, summary));
            } else {
                response.send(404, null, "");
            }
        });

        var code = run("en", new byte[0], "answer", "Q-001", "--no-input");

        assertEquals(3, code);
        assertTrue(out.toString().contains("Against it (" + dissenter + "): " + summary), out.toString());
        assertFalse(out.toString().contains("Against it (" + firstVoter + ")"), out.toString());
    }

    @Test
    @DisplayName("a supporting vote with the summary's reason is never named as the dissenter")
    void dissentNeverNamesSupporter() throws Exception {
        var supporter = text();
        var dissenter = text();
        var summary = text();
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-001".equals(request.target())) {
                response.json(200, sharedReasonEnvelope(supporter, dissenter, summary));
            } else {
                response.send(404, null, "");
            }
        });

        var code = run("en", new byte[0], "answer", "Q-001", "--no-input");

        assertEquals(3, code);
        assertTrue(out.toString().contains("Against it (" + dissenter + "): " + summary), out.toString());
        assertFalse(out.toString().contains("Against it (" + supporter + ")"), out.toString());
    }

    @Test
    @DisplayName("with --plan a 404 on the answer sends nothing to the unscoped path")
    void scopedNotFoundStaysScoped() throws Exception {
        var planId = text();
        var targets = new ArrayList<String>();
        fixture.start((request, response) -> {
            var target = request.target();
            targets.add(request.method() + " " + target);
            if (("/api/v1/plans/" + planId + "/decisions/Q-001").equals(target) && "GET".equals(request.method())) {
                response.json(200, planEnvelope());
            } else {
                response.send(404, null, "");
            }
        });

        var code = run("en", new byte[0], "answer", "Q-001", "1", "--plan", planId);

        assertEquals(1, code);
        assertTrue(err.toString().contains("This entry does not exist"), err.toString());
        assertTrue(targets.stream().noneMatch(t -> t.contains("/api/v1/questions/")), targets.toString());
    }

    private static final String UNREADABLE = "Q-001 could not be read, so it is not known whether it has parts;"
                        + " nothing was sent. Answer it in the dialogue.";

    @ParameterizedTest(name = "a {0} on the first read of {1} is a failed read, not a decision")
    @CsvSource({"500, answer Q-001 --no-input", "503, answer Q-001 --no-input", "500, answer Q-001 1",
                        "502, answer Q-001 later"})
    @DisplayName("a failed first read prints the unreadable line and exits 1, with no status on screen")
    void failedFirstReadIsUnreadable(int status, String command) throws Exception {
        var failure = text();
        var posted = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(status, errorBody(failure));
            } else {
                posted.add(request.target());
                response.json(200, "{\"ok\":true}");
            }
        });

        var code = run("en", new byte[0], command.split(" "));

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains(UNREADABLE), out.toString()),
                            () -> assertFalse((out.toString() + err).contains("HTTP " + status), out + " " + err),
                            () -> assertTrue(posted.isEmpty(), posted.toString()));
    }

    @ParameterizedTest(name = "a first read that never reaches an answer, for {0}")
    @ValueSource(strings = {"answer Q-001 --no-input", "answer Q-001 1", "answer Q-001 later"})
    @DisplayName("a transport failure on the first read is as unreadable as a refused one: the line, exit 1, nothing sent")
    void transportFailureOnTheFirstReadIsUnreadable(String command) throws Exception {
        var posted = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.raw("this is no http response\r\n\r\n");
            } else {
                posted.add(request.target());
                response.json(200, "{\"ok\":true}");
            }
        });

        var code = run("en", new byte[0], command.split(" "));

        assertAll(
                            () -> assertEquals(1, code, err.toString()),
                            () -> assertTrue(out.toString().contains(UNREADABLE), out.toString()),
                            () -> assertTrue(posted.isEmpty(), posted.toString()));
    }

    @ParameterizedTest(name = "{0} is no decision of Q-001: {2}")
    @CsvSource(delimiter = '|', value = {
                        "another id | --no-input | OTHER",
                        "another id | 1 | OTHER",
                        "a body without an id | --no-input | NOID",
                        "a body that is no JSON | 1 | NOJSON"})
    @DisplayName("a first read that returns no decision of the id asked for prints the unreadable line and exits 1")
    void mismatchedFirstReadIsUnreadable(String name, String argument, String kind) throws Exception {
        var other = "Q-" + Generators.integers(100, 999).next();
        var body = switch (kind) {
            case "OTHER" -> envelope(other, "plan", "plan", "plan " + text(), text());
            case "NOID" -> envelope("Q-001", "plan", "plan", "plan " + text(), text()).replace("\"id\": \"Q-001\",", "");
            default -> "not json " + text();
        };
        var posted = new ArrayList<String>();
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, body);
            } else {
                posted.add(request.target());
                response.json(200, "{\"ok\":true}");
            }
        });

        var code = run("en", new byte[0], "answer", "Q-001", argument);

        assertAll(
                            () -> assertEquals(1, code, name),
                            () -> assertTrue(out.toString().contains(UNREADABLE), out.toString()),
                            () -> assertFalse(out.toString().contains(other + " for"), out.toString()),
                            () -> assertTrue(posted.isEmpty(), posted.toString()));
    }

    @Test
    @DisplayName("an option key of ten digits is shown, never parsed into an exception")
    void tenDigitOptionKey() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-001".equals(request.target())) {
                response.json(200, envelope("Q-001", "plan", "plan", "plan storage-v2", "pm-mcp-clients")
                                    .replace("{\"key\": \"1\", \"label\": \"Yes\"}",
                                                        "{\"key\": \"9999999999\", \"label\": \"Yes\"}, {\"key\": \"2\", \"label\": \"No\"}"));
            } else {
                response.send(404, null, "");
            }
        });

        var code = run("en", new byte[0], "answer", "Q-001", "--no-input");

        assertEquals(3, code, err.toString());
        assertTrue(out.toString().contains("Choose: 9999999999, 2"), out.toString());
    }

    private void assertGerman(String expected, String... args) {
        out = new StringWriter();
        err = new StringWriter();
        var code = run("de", new byte[0], args);
        assertEquals(0, code, "exit " + code + ": " + err);
        assertTrue(out.toString().contains(expected), out.toString());
    }

    private int run(String language, byte[] input, String... args) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de".equals(language) ? "de_DE.UTF-8" : "en_US.UTF-8");
        var terminalBytes = new ByteArrayOutputStream();
        InputStream inStream = new ByteArrayInputStream(input);
        var termProvider = (OperatorContext.TerminalProvider) () -> new DumbTerminal(inStream, terminalBytes);
        var context = new OperatorContext(environment, tempDir, true,
                            ClientEnvironment.capture(environment), termProvider, null);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    /** A word of letters only: it is safe in a path segment, a JSON string and a catalogue argument. */
    private static String text() {
        return Generators.letterStrings(4, 12).next();
    }

    private static String errorBody(String failure) {
        return "{\"error\":\"%s\"}".formatted(failure);
    }

    private static String dissentEnvelope(String firstVoter, String firstReason, String dissenter, String summary) {
        return """
                            {
                              "id": "Q-001", "revision": 1, "title": "Pick a strategy", "subject": "plan fix-relay",
                              "scope": "plan", "blocks": "plan", "reversibility": "reversible",
                              "parts": [{
                                "part_id": "1", "question": "Which strategy?", "recommendation": "1",
                                "recommending_role": "implement", "recommendation_rationale": "Keeps history linear",
                                "options": [
                                  {"key": "1", "label": "Rebase",
                                   "quorum_support": {"band": "clear_majority", "votes_count": 2, "total_votes": 4,
                                     "models_count": 2, "dissent_summary": "%s"}},
                                  {"key": "2", "label": "Merge",
                                   "quorum_support": {"band": "clear_majority", "votes_count": 2, "total_votes": 4,
                                     "models_count": 2,
                                     "votes": [{"role": "%s", "model": "gpt-4o", "reason": "%s"},
                                               {"role": "%s", "model": "gpt-4o", "reason": "%s"}]}}
                                ]
                              }]
                            }
                            """.formatted(summary, firstVoter, firstReason, dissenter, summary);
    }

    /**
     * The recommended option carries the summary and a supporting vote whose reason is the summary,
     * listed before the dissenting vote of the other option, which gives the same reason.
     */
    private static String sharedReasonEnvelope(String supporter, String dissenter, String summary) {
        return """
                            {
                              "id": "Q-001", "revision": 1, "title": "Pick a strategy", "subject": "plan fix-relay",
                              "scope": "plan", "blocks": "plan", "reversibility": "reversible",
                              "parts": [{
                                "part_id": "1", "question": "Which strategy?", "recommendation": "1",
                                "recommending_role": "implement", "recommendation_rationale": "Keeps history linear",
                                "options": [
                                  {"key": "1", "label": "Rebase",
                                   "quorum_support": {"band": "clear_majority", "votes_count": 2, "total_votes": 3,
                                     "models_count": 2, "dissent_summary": "%s",
                                     "votes": [{"role": "%s", "model": "gpt-4o", "reason": "%s"}]}},
                                  {"key": "2", "label": "Merge",
                                   "quorum_support": {"band": "clear_majority", "votes_count": 1, "total_votes": 3,
                                     "models_count": 1,
                                     "votes": [{"role": "%s", "model": "gpt-4o", "reason": "%s"}]}}
                                ]
                              }]
                            }
                            """.formatted(summary, supporter, summary, dissenter, summary);
    }

    private static String planEnvelope() {
        return envelope("Q-001", "plan", "plan", "plan storage-v2", "pm-mcp-clients");
    }

    private static String ownWordsEnvelope() {
        return """
                            {
                              "id": "Q-001", "revision": 1, "title": "Pick", "subject": "plan fix-relay",
                              "scope": "plan", "blocks": "plan", "reversibility": "reversible",
                              "plan": "fix-relay", "project": "pm-mcp-clients",
                              "parts": [{
                                "part_id": "1", "question": "Which?", "allow_own_words": true,
                                "options": [{"key": "1", "label": "Rebase"}]
                              }]
                            }
                            """;
    }

    private static String envelope(String id, String scope, String blocks, String subject, String project) {
        return """
                            {
                              "id": "%s", "revision": 1, "title": "Pick", "subject": "%s",
                              "scope": "%s", "blocks": "%s", "reversibility": "reversible",
                              "plan": "storage-v2", "project": "%s",
                              "parts": [{"part_id": "1", "question": "Which?", "options": [{"key": "1", "label": "Yes"}]}]
                            }
                            """.formatted(id, subject, scope, blocks, project);
    }
}
