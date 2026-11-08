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
import java.util.Map;
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
        assertTrue(rendered.contains("cannot be undone"), rendered);
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
                    "options": [{"key": "1", "label": "A file per plan"}, {"key": "2", "label": "One database for the machine"}]},
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
                 "scope": "plan", "blocks": "plan", "reversibility": "irreversible",
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

    private int run(String input, String... args) throws IOException {
        return run(input.getBytes(StandardCharsets.UTF_8), args);
    }

    private int run(byte[] input, String... args) throws IOException {
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
