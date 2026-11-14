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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.List;
import java.util.function.UnaryOperator;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The decision dialogue asks every part with its own question, posts one answer per part, keeps
 * nothing preselected where the votes are split or the decision is costly, and offers own words and
 * a note only where the part allows them ({@code doc/design/shared/decisions.adoc}).
 */
@DisplayName("AnswerCommand asks every part and offers its keys")
@EnableGeneratorController
class AnswerMultipartTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private StringWriter terminalText;
    private final List<String> postedBodies = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        out = new StringWriter();
        err = new StringWriter();
        terminalText = new StringWriter();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Test
    @DisplayName("the three parts of Q-012 are asked and posted in one answer")
    void threePartsAreAskedAndAnswered() throws Exception {
        serve("Q-012", threePartEnvelope(), true);

        var code = run("1\n2\n1\n\n", "answer", "Q-012", "-i");

        assertEquals(0, code);
        assertTrue(out.toString().contains("[part 1 of 3] Where does the data live?"), out.toString());
        assertTrue(out.toString().contains("[part 2 of 3] How is it migrated?"), out.toString());
        assertTrue(out.toString().contains("[part 3 of 3] Which format?"), out.toString());
        assertTrue(out.toString().contains("Q-012 answered."), out.toString());
        assertTrue(out.toString().contains("Follows: One file per plan is written"), out.toString());

        assertEquals(1, postedBodies.size(), postedBodies.toString());
        var body = JsonTree.asObject(JsonTree.parse(postedBodies.get(0)));
        assertNotNull(body);
        var answers = JsonTree.asObject(body.get("part_answers"));
        assertNotNull(answers, postedBodies.get(0));
        assertEquals(3, answers.size(), postedBodies.get(0));
        assertEquals("1", answers.get("1"));
        assertEquals("2", answers.get("2"));
        assertEquals("1", answers.get("3"));
    }

    @Test
    @DisplayName("back keeps the earlier answer and Enter re-confirms it")
    void backKeepsEarlierAnswer() throws Exception {
        serve("Q-012", threePartEnvelope(), true);

        var code = run("2\nb\n\n2\n1\n\n", "answer", "Q-012", "-i");

        assertEquals(0, code);
        var body = JsonTree.asObject(JsonTree.parse(postedBodies.get(0)));
        var answers = JsonTree.asObject(body.get("part_answers"));
        assertEquals("2", answers.get("1"), postedBodies.get(0));
        assertEquals("2", answers.get("2"), postedBodies.get(0));
        assertEquals("1", answers.get("3"), postedBodies.get(0));
    }

    @Test
    @DisplayName("a direct answer on a multi-part decision refuses with exit 3")
    void directAnswerOnMultipartRefuses() throws Exception {
        serve("Q-012", threePartEnvelope(), true);

        var code = run(new byte[0], "answer", "Q-012", "2");

        assertEquals(3, code);
        assertTrue(out.toString().contains("has 3 parts"), out.toString());
        assertFalse(out.toString().contains("answered"), out.toString());
        assertTrue(postedBodies.isEmpty(), postedBodies.toString());
    }

    @Test
    @DisplayName("a costly decision hides the recommendation until ?r")
    void costlyRecommendationHiddenUntilRequested() throws Exception {
        serve("Q-001", costlyEnvelope(), false);

        var code = run("?r\n1\n\n", "answer", "Q-001", "-i");

        assertEquals(0, code);
        var rendered = out.toString();
        var hint = rendered.indexOf("one step away");
        var recommendation = rendered.indexOf("Recommended by implement");
        assertTrue(hint >= 0, rendered);
        assertTrue(recommendation > hint, rendered);
        assertFalse(rendered.lines()
                            .filter(line -> line.contains("1) Delete them"))
                            .anyMatch(line -> line.contains("←")), rendered);
        assertTrue(rendered.contains("costly to undo"), rendered);
        assertFalse(rendered.contains("cannot be undone"), rendered);
    }

    @Test
    @DisplayName("split votes preselect nothing: Enter alone does not answer")
    void splitVotesPreselectNothing() throws Exception {
        serve("Q-001", splitEnvelope(), true);

        var code = run("\n1\n", "answer", "Q-001", "-i");

        assertEquals(0, code);
        var rendered = out.toString();
        assertTrue(terminalText.toString().contains("Choose:"), terminalText.toString());
        assertTrue(rendered.contains("Please choose a valid option"), rendered);
        assertTrue(rendered.contains("Q-001 answered: Rebase"), rendered);
        assertEquals(1, postedBodies.size(), postedBodies.toString());
        assertFalse(postedBodies.get(0).contains("\"option\":\"2\""), postedBodies.get(0));
    }

    @Test
    @DisplayName("a part that allows no own words offers no w")
    void ownWordsNotOfferedWhenDisallowed() throws Exception {
        serve("Q-001", plainEnvelope(false, false), false);

        var code = run("1\n", "answer", "Q-001", "-i");

        assertEquals(0, code);
        assertFalse(terminalText.toString().contains("w) in my own words"), terminalText.toString());
        assertFalse(terminalText.toString().contains("m) add a note"), terminalText.toString());
        assertTrue(out.toString().contains("Q-001 answered"), out.toString());
    }

    @Test
    @DisplayName("?2 shows the votes on option 2 with their model and reason")
    void voteViewShowsVotesOnOption() throws Exception {
        serve("Q-001", splitEnvelope(), false);

        var code = run("?2\n1\n", "answer", "Q-001", "-i");

        assertEquals(0, code);
        var rendered = out.toString();
        assertTrue(rendered.contains("Votes for 2: Merge"), rendered);
        assertTrue(rendered.contains("reviewer-a"), rendered);
        assertTrue(rendered.contains("preserves context"), rendered);
        assertTrue(rendered.contains("model's own estimate"), rendered);
    }

    @Test
    @DisplayName("the chat key is not advertised while the terminal has no decision chat")
    void chatKeyNotAdvertised() throws Exception {
        serve("Q-001", plainEnvelope(true, true), false);

        var code = run("h\n1\n", "answer", "Q-001", "-i");

        assertEquals(0, code);
        assertFalse(terminalText.toString().contains("h) chat"), terminalText.toString());
        assertTrue(out.toString().contains("Please choose a valid option"), out.toString());
        assertTrue(out.toString().contains("Q-001 answered"), out.toString());
    }

    @Test
    @DisplayName("a direct key on a decision that could not be read is refused and nothing is sent")
    void directAnswerOnUnreadableDecisionRefuses() throws Exception {
        var failure = Generators.letterStrings(4, 20).next();
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(500, "{\"error\":\"%s\"}".formatted(failure));
            } else {
                postedBodies.add(request.bodyText());
                response.json(200, "{\"ok\":true}");
            }
        });

        var code = run(new byte[0], "answer", "Q-012", "2");

        assertEquals(1, code);
        assertTrue(out.toString().contains(
                                                "Q-012 could not be read, so it is not known whether it has parts; nothing was sent."),
                            out.toString());
        assertFalse((out.toString() + err).contains("HTTP 500"), out + " " + err);
        assertTrue(postedBodies.isEmpty(), postedBodies.toString());
    }

    @Test
    @DisplayName("the confirmation of a costly decision of several parts names the stake")
    void costlyMultipartConfirmationTitle() throws Exception {
        serve("Q-012", threePartEnvelope().replace("\"reversibility\": \"reversible\"", "\"reversibility\": \"costly\""), true);

        var code = run("1\n2\n1\n\n", "answer", "Q-012", "-i");

        assertEquals(0, code);
        assertTrue(out.toString().contains("Your decision · Q-012 · costly to undo"), out.toString());
        assertEquals(1, postedBodies.size(), postedBodies.toString());
    }

    @Test
    @DisplayName("unclear own words with no question are asked again, never posted")
    void unclearWithoutQuestionAsksAgain() throws Exception {
        var unclearWords = words();
        var clearWords = words();
        var reading = words();
        var interpreted = new ArrayList<String>();
        serveOwnWords(interpreted, words -> words.equals(unclearWords)
                            ? "{\"interpretation_type\":\"unclear\"}"
                            : readBackJson(reading));

        var code = run("w\n" + unclearWords + "\n" + clearWords + "\n\n", "answer", "Q-001", "-i");

        assertEquals(0, code);
        assertEquals(List.of(unclearWords, clearWords), interpreted);
        assertTrue(out.toString().contains("could not be read clearly enough"), out.toString());
        assertEquals(1, postedBodies.size(), postedBodies.toString());
        var body = JsonTree.asObject(JsonTree.parse(postedBodies.get(0)));
        assertEquals(reading, body.get("words"), postedBodies.get(0));
    }

    @Test
    @DisplayName("an unknown input at the read-back is asked again, the reading kept")
    void readBackTypoAsksAgain() throws Exception {
        var reading = words();
        var interpreted = new ArrayList<String>();
        serveOwnWords(interpreted, words -> readBackJson(reading));

        var code = run("w\n" + words() + "\nzz\n\n", "answer", "Q-001", "-i");

        assertEquals(0, code);
        assertTrue(out.toString().contains("Please choose a valid option or action."), out.toString());
        assertEquals(1, postedBodies.size(), postedBodies.toString());
        var body = JsonTree.asObject(JsonTree.parse(postedBodies.get(0)));
        assertEquals(reading, body.get("words"), postedBodies.get(0));
    }

    @Test
    @DisplayName("a clarified reading without read-back text keeps the operator's reply in the answer")
    void clarifiedReplyKeptWithoutReadBackText() throws Exception {
        var unclearWords = words();
        var question = words();
        var reply = words();
        var interpreted = new ArrayList<String>();
        serveOwnWords(interpreted, words -> words.equals(unclearWords)
                            ? "{\"interpretation_type\":\"unclear\",\"clarification_question\":\"%s\"}".formatted(question)
                            : "{\"interpretation_type\":\"own_option\"}");

        var code = run("w\n" + unclearWords + "\n" + reply + "\n\n", "answer", "Q-001", "-i");

        assertEquals(0, code, err.toString());
        assertEquals(List.of(unclearWords, unclearWords + ". " + reply), interpreted);
        assertEquals(1, postedBodies.size(), postedBodies.toString());
        var body = JsonTree.asObject(JsonTree.parse(postedBodies.get(0)));
        var posted = String.valueOf(body.get("words"));
        assertTrue(posted.contains(reply), postedBodies.get(0));
        assertEquals(unclearWords + ". " + reply, posted, postedBodies.get(0));
    }

    /** Free words an operator types: letters only, so no word is read as a key of the dialogue. */
    private static String words() {
        return Generators.letterStrings(4, 10).next() + " " + Generators.letterStrings(4, 10).next();
    }

    private static String readBackJson(String reading) {
        return "{\"interpretation_type\":\"own_option\",\"read_back_text\":\"%s\"}".formatted(reading);
    }

    @Test
    @DisplayName("the end of the input at a prompt prints the Left line and exits 0")
    void endOfInputLeaves() throws Exception {
        serve("Q-012", threePartEnvelope(), true);

        var code = run("1\n", "answer", "Q-012", "-i");

        assertEquals(0, code);
        assertTrue(out.toString().contains("Left Q-012. Nothing was sent."), out.toString());
        assertTrue(postedBodies.isEmpty(), postedBodies.toString());
    }

    private void serveOwnWords(List<String> interpreted, UnaryOperator<String> interpretation)
                        throws IOException {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, plainEnvelope(true, false));
            } else if ("/api/v1/decisions/Q-001/interpret".equals(target)) {
                var words = String.valueOf(JsonTree.asObject(JsonTree.parse(request.bodyText())).get("words"));
                interpreted.add(words);
                response.json(200, interpretation.apply(words));
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                postedBodies.add(request.bodyText());
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
    }

    private void serve(String id, String envelope, boolean recordAnswer) throws IOException {
        fixture.start((request, response) -> {
            var target = request.target();
            if (("/api/v1/decisions/" + id).equals(target) && "GET".equals(request.method())) {
                response.json(200, envelope);
            } else if (("/api/v1/decisions/" + id + "/answer").equals(target)) {
                if (recordAnswer) {
                    postedBodies.add(request.bodyText());
                }
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
    }

    private static String threePartEnvelope() {
        return """
                            {
                              "id": "Q-012", "revision": 1, "title": "How should the new store be introduced?",
                              "subject": "plan storage-v2", "scope": "plan", "asking_phase": "outline",
                              "blocks": "plan", "reversibility": "reversible",
                              "parts": [
                                {"part_id": "1", "question": "Where does the data live?", "recommendation": "1",
                                 "recommending_role": "outline", "recommendation_rationale": "Simple isolation.",
                                 "allow_own_words": true,
                                 "options": [{"key": "1", "label": "A file per plan", "consequences": "One file per plan is written"},
                              {"key": "2", "label": "One database for the machine"}]},
                                {"part_id": "2", "question": "How is it migrated?", "recommendation": "1",
                                 "recommending_role": "outline", "recommendation_rationale": "Automated transition.",
                                 "dependency": "depends on part 1: A file per plan", "allow_own_words": true,
                                 "options": [{"key": "1", "label": "At the first start of the new release"}, {"key": "2", "label": "By a verb the operator runs"}]},
                                {"part_id": "3", "question": "Which format?", "recommendation": "1",
                                 "recommending_role": "outline", "recommendation_rationale": "JSON lines.",
                                 "allow_own_words": true,
                                 "options": [{"key": "1", "label": "JSON lines"}, {"key": "2", "label": "SQLite per plan"}]}
                              ]
                            }
                            """;
    }

    private static String costlyEnvelope() {
        return """
                            {
                              "id": "Q-001", "revision": 1, "title": "Delete the endpoints", "subject": "plan drop-legacy-api",
                              "scope": "plan", "blocks": "plan", "reversibility": "costly",
                              "parts": [{
                                "part_id": "1", "question": "Delete them?", "recommendation": "1",
                                "recommending_role": "implement", "recommendation_rationale": "Keeps the API clean.",
                                "allow_own_words": true, "allow_note": true,
                                "options": [
                                  {"key": "1", "label": "Delete them", "consequences": "Old clients break"},
                                  {"key": "2", "label": "Keep them"}
                                ]
                              }]
                            }
                            """;
    }

    private static String splitEnvelope() {
        return """
                            {
                              "id": "Q-001", "revision": 1, "title": "Pick a strategy", "subject": "plan fix-relay",
                              "scope": "plan", "blocks": "plan", "reversibility": "reversible",
                              "parts": [{
                                "part_id": "1", "question": "Which strategy?", "recommendation": "1",
                                "recommending_role": "implement", "recommendation_rationale": "Keeps history linear",
                                "allow_own_words": true, "allow_note": true,
                                "options": [
                                  {"key": "1", "label": "Rebase",
                                   "quorum_support": {"band": "split", "votes_count": 1, "total_votes": 3, "models_count": 1}},
                                  {"key": "2", "label": "Merge",
                                   "quorum_support": {"band": "split", "votes_count": 1, "total_votes": 3, "models_count": 1,
                                     "dissent_summary": "context lost",
                                     "votes": [{"role": "reviewer-a", "model": "gpt-4o", "confidence_percent": 70,
                                                "reason": "preserves context"}]}}
                                ]
                              }]
                            }
                            """;
    }

    private static String plainEnvelope(boolean ownWords, boolean note) {
        return """
                            {
                              "id": "Q-001", "revision": 1, "title": "Pick a strategy", "subject": "plan fix-relay",
                              "scope": "plan", "blocks": "plan", "reversibility": "reversible",
                              "parts": [{
                                "part_id": "1", "question": "Which strategy?", "recommendation": "1",
                                "recommending_role": "implement", "recommendation_rationale": "Keeps history linear",
                                "allow_own_words": %s, "allow_note": %s,
                                "options": [{"key": "1", "label": "Rebase"}, {"key": "2", "label": "Merge"}]
                              }]
                            }
                            """.formatted(ownWords, note);
    }

    private int run(String input, String... args) {
        return run(input.getBytes(StandardCharsets.UTF_8), args);
    }

    private int run(byte[] input, String... args) {
        var terminalBytes = new ByteArrayOutputStream();
        InputStream inStream = new ByteArrayInputStream(input);
        var termProvider = (OperatorContext.TerminalProvider) () -> new DumbTerminal(inStream, terminalBytes);
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()), termProvider, null);
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
        terminalText.write(terminalBytes.toString(StandardCharsets.UTF_8));
        return code;
    }
}
