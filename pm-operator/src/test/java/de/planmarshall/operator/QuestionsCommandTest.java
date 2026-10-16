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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.jline.terminal.impl.DumbTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.planmarshall.api.testsupport.RuntimeFixture;

@DisplayName("QuestionsCommand and AnswerCommand")
class QuestionsCommandTest {

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

    private int execute(String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private int executeInteractive(String input, String... args) {
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();
        OperatorContext.TerminalProvider termProvider = () -> new DumbTerminal(inStream, outStream);
        var context = new OperatorContext(fixture.environment(), tempDir, true, ClientEnvironment.capture(fixture.environment()), termProvider, null);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    @Test
    @DisplayName("questions lists open decisions")
    void questionsList() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                                [
                                                  {"id":"Q-007","plan":"fix-relay","title":"Resolve rebase conflict"}
                                                ]
                                                """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("questions");
        var output = out.toString();
        assertAll(
                                        () -> assertEquals(0, code),
                                        () -> assertTrue(output.contains("Open Questions · 1 pending")),
                                        () -> assertTrue(output.contains("Q-007")),
                                        () -> assertTrue(output.contains("fix-relay")),
                                        () -> assertTrue(output.contains("Resolve rebase conflict"))
        );
    }

    @Test
    @DisplayName("questions reports no open questions when list is empty")
    void questionsEmpty() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions".equals(request.target())) {
                response.json(200, "[]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("questions");
        assertEquals(0, code);
        assertTrue(out.toString().contains("No open questions."));
    }

    @Test
    @DisplayName("questions falls back to /api/v1/questions on 404")
    void questionsFallback() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions".equals(request.target())) {
                response.send(404, null, "");
            } else if ("/api/v1/questions".equals(request.target())) {
                response.json(200, "[{\"id\":\"Q-001\",\"scope\":\"proj-1\",\"question\":\"Merge now?\"}]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("questions");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Q-001"));
        assertTrue(out.toString().contains("Merge now?"));
    }

    @Test
    @DisplayName("questions reports error on 500")
    void questionsError() throws Exception {
        fixture.start((request, response) -> response.json(500, "{\"code\":\"error\"}"));

        int code = execute("questions");
        assertEquals(PmOperator.EXIT_FAILURE, code);
    }

    @Test
    @DisplayName("answer <id> <option> answers a decision")
    void answerWithOption() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-007/answer".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("answer", "Q-007", "2");
        assertAll(
                                        () -> assertEquals(0, code),
                                        () -> assertTrue(out.toString().contains("✓ Answered Q-007 with option 2."))
        );
    }

    @Test
    @DisplayName("answer <id> --words answers in own words")
    void answerWithWords() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-007/answer".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("answer", "Q-007", "--words", "rebase onto origin/main");
        assertAll(
                                        () -> assertEquals(0, code),
                                        () -> assertTrue(out.toString().contains("✓ Answered Q-007 with option rebase onto origin/main."))
        );
    }

    @Test
    @DisplayName("answer <id> without option displays question details")
    void answerShowDetails() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-007".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "{\"title\":\"Choose strategy\",\"options\":[\"rebase\",\"merge\"]}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("answer", "Q-007");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Choose strategy"));
    }

    @Test
    @DisplayName("answer <id> without option reports not found when 404")
    void answerNotFound() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("answer", "Q-999");
        assertEquals(1, code);
        assertTrue(out.toString().contains("Question Q-999 not found."));
    }

    @Test
    @DisplayName("answer <id> with --no-input and no option returns code 3")
    void answerNoInputWithoutOption() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-007".equals(request.target())) {
                response.json(200, "{\"title\":\"Question\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("answer", "Q-007", "--no-input");
        assertEquals(3, code);
        assertTrue(out.toString().contains("Choice or option required (--no-input specified)."));
    }

    @Test
    @DisplayName("answer falls back to /api/v1/questions/<id>/answers on 404")
    void answerFallbackEndpoint() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-008/answer".equals(request.target())) {
                response.send(404, null, "");
            } else if ("/api/v1/questions/Q-008/answers".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("answer", "Q-008", "yes", "--note", "proceeding");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Answered Q-008 with option yes."));
    }

    @Test
    @DisplayName("answer reports refusal when both endpoints fail")
    void answerRefused() throws Exception {
        fixture.start((request, response) -> response.send(500, null, ""));

        int code = execute("answer", "Q-008", "yes");
        assertEquals(PmOperator.EXIT_FAILURE, code);
    }

    @Test
    @DisplayName("answer waiver without reason returns code 3")
    void answerWaiverWithoutReason() throws Exception {
        int code = execute("answer", "WR-2", "waive");
        assertEquals(3, code);
        assertTrue(out.toString().contains("reason is required"));
    }

    @Test
    @DisplayName("answer waiver with reason succeeds")
    void answerWaiverWithReason() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/WR-2/answer".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("answer", "WR-2", "waive", "--reason", "hotfix on prod");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Answered WR-2 with option waive."));
    }

    @Test
    @DisplayName("answer later leaves decision open")
    void answerLater() throws Exception {
        int code = execute("answer", "Q-007", "later");
        assertEquals(0, code);
        assertTrue(out.toString().contains("stays open"));
    }

    @Test
    @DisplayName("answer withdraw invokes think-again endpoint")
    void answerWithdraw() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-007/withdraw".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("answer", "Q-007", "withdraw");
        assertEquals(0, code);
        assertTrue(out.toString().contains("you asked to think again"));
    }

    @Test
    @DisplayName("interactive decision answering takes recommended option on Enter")
    void answerInteractiveTakeRecommended() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, """
                                                {
                                                  "id": "Q-001",
                                                  "revision": 1,
                                                  "title": "Select merge strategy",
                                                  "subject": "fix-relay",
                                                  "scope": "plan",
                                                  "asking_phase": "refine",
                                                  "asking_role": "outline",
                                                  "blocks": "plan",
                                                  "reversibility": "reversible",
                                                  "description": "Preserve clean history",
                                                  "evidence": {"diff": "+10 -2"},
                                                  "parts": [
                                                    {
                                                      "part_id": "part-1",
                                                      "question": "How should branch be updated?",
                                                      "answer_type": "one_option",
                                                      "recommendation": "rebase",
                                                      "recommending_role": "outline",
                                                      "recommendation_rationale": "Keeps clean git log",
                                                      "options": [
                                                        {
                                                          "key": "rebase",
                                                          "label": "Rebase onto main",
                                                          "quorum_support": {
                                                            "votes_count": 2,
                                                            "total_votes": 3,
                                                            "models_count": 2,
                                                            "band": "clear_majority",
                                                            "dissent_summary": "Merge keeps commits"
                                                          }
                                                        },
                                                        {
                                                          "key": "merge",
                                                          "label": "Merge main into branch"
                                                        }
                                                      ]
                                                    }
                                                  ]
                                                }
                                                """);
            } else if ("/api/v1/decisions/Q-001/answer".equals(target) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeInteractive("\n", "answer", "Q-001", "-i");
        assertEquals(0, code);
        var output = out.toString();
        assertTrue(output.contains("Question Q-001"));
        assertTrue(output.contains("Rebase onto main"));
        assertTrue(output.contains("2 of 3 votes · 2 models"));
        assertTrue(output.contains("Against it (quorum-b): Merge keeps commits"));
        assertTrue(output.contains("✓ Q-001 answered: Rebase onto main"));
    }

    @Test
    @DisplayName("interactive decision answering with note and other option")
    void answerInteractiveWithNoteAndChoice() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, """
                                                {
                                                  "id": "Q-001",
                                                  "revision": 1,
                                                  "title": "Select merge strategy",
                                                  "subject": "fix-relay",
                                                  "scope": "plan",
                                                  "asking_phase": "refine",
                                                  "blocks": "plan",
                                                  "reversibility": "costly",
                                                  "parts": [
                                                    {
                                                      "part_id": "part-1",
                                                      "question": "Choose strategy",
                                                      "answer_type": "one_option",
                                                      "options": [
                                                        {"key": "rebase", "label": "Rebase"},
                                                        {"key": "merge", "label": "Merge", "consequences": "Creates merge commit"}
                                                      ]
                                                    }
                                                  ]
                                                }
                                                """);
            } else if ("/api/v1/decisions/Q-001/answer".equals(target) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        // m -> enter note -> merge -> confirm Enter
        int code = executeInteractive("m\nmy note text\nmerge\n\n", "answer", "Q-001", "-i");
        assertEquals(0, code);
        var output = out.toString();
        assertTrue(output.contains("cannot be undone"));
        assertTrue(output.contains("✓ Q-001 answered: Merge (with your note)"));
    }

    @Test
    @DisplayName("interactive decision with none-fits and later")
    void answerInteractiveNoneFitsAndLater() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, """
                                                {
                                                  "id": "Q-001",
                                                  "revision": 1,
                                                  "title": "Select merge strategy",
                                                  "subject": "plan fix-relay",
                                                  "scope": "plan",
                                                  "blocks": "plan",
                                                  "reversibility": "reversible",
                                                  "parts": []
                                                }
                                                """);
            } else if ("/api/v1/decisions/Q-001/withdraw".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int codeLater = executeInteractive("l\n", "answer", "Q-001", "-i");
        assertEquals(0, codeLater);
        assertTrue(out.toString().contains("stays open"));

        out = new StringWriter();
        int codeNoneFits = executeInteractive("x\n\n", "answer", "Q-001", "-i");
        assertEquals(0, codeNoneFits);
        assertTrue(out.toString().contains("you asked the plan to think again"));
    }

    @Test
    @DisplayName("interactive decision with own words interpretation and read-back")
    void answerInteractiveOwnWordsFlow() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, """
                                                {
                                                  "id": "Q-001",
                                                  "revision": 1,
                                                  "title": "Select merge strategy",
                                                  "subject": "fix-relay",
                                                  "scope": "plan",
                                                  "blocks": "plan",
                                                  "reversibility": "reversible",
                                                  "parts": [
                                                    {
                                                      "part_id": "part-1",
                                                      "question": "How?",
                                                      "answer_type": "one_option",
                                                      "options": [{"key": "rebase", "label": "Rebase"}]
                                                    }
                                                  ]
                                                }
                                                """);
            } else if ("/api/v1/decisions/Q-001/interpret".equals(target)) {
                response.json(200, """
                                                {
                                                  "words": "Use rebase please",
                                                  "read_back_text": "Rebase onto main",
                                                  "interpretation_type": "option",
                                                  "option": "rebase",
                                                  "understood": true
                                                }
                                                """);
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        // w -> words -> confirm Enter
        int code = executeInteractive("w\nUse rebase please\n\n", "answer", "Q-001", "-i");
        assertEquals(0, code);
        var output = out.toString();
        assertTrue(output.contains("I understood: Rebase onto main"));
        assertTrue(output.contains("✓ Q-001 answered: Rebase onto main"));
    }

    @Test
    @DisplayName("interactive own words with clarification question")
    void answerInteractiveOwnWordsClarification() throws Exception {
        var count = new int[]{0};
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, """
                                                {
                                                  "id": "Q-001",
                                                  "revision": 1,
                                                  "title": "Select merge strategy",
                                                  "subject": "fix-relay",
                                                  "scope": "plan",
                                                  "blocks": "plan",
                                                  "reversibility": "reversible",
                                                  "parts": []
                                                }
                                                """);
            } else if ("/api/v1/decisions/Q-001/interpret".equals(target)) {
                count[0]++;
                if (count[0] == 1) {
                    response.json(200, """
                                                    {
                                                      "words": "unclear words",
                                                      "interpretation_type": "unclear",
                                                      "clarification_question": "Which branch should be target?",
                                                      "understood": false
                                                    }
                                                    """);
                } else {
                    response.json(200, """
                                                    {
                                                      "words": "unclear words. main branch",
                                                      "read_back_text": "Rebase onto main branch",
                                                      "interpretation_type": "option",
                                                      "understood": true
                                                    }
                                                    """);
                }
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        // w -> unclear words -> reply to question -> confirm
        int code = executeInteractive("w\nunclear words\nmain branch\n\n", "answer", "Q-001", "-i");
        assertEquals(0, code);
        var output = out.toString();
        assertTrue(output.contains("interpret asks: Which branch should be target?"));
        assertTrue(output.contains("I understood: Rebase onto main branch"));
    }

    @Test
    @DisplayName("interactive waiver answering: waive, decline, and later")
    void answerInteractiveWaiver() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/WR-1".equals(target) && "GET".equals(request.method())) {
                response.json(200, """
                                                {
                                                  "id": "WR-1",
                                                  "revision": 1,
                                                  "title": "Coverage waiver",
                                                  "subject": "plan fix-relay",
                                                  "scope": "plan",
                                                  "is_waiver": true,
                                                  "waiver_gate": "coverage >= 80%",
                                                  "waiver_reason": "generated files",
                                                  "evidence": {"coverage": "76%"}
                                                }
                                                """);
            } else if ("/api/v1/decisions/WR-1/answer".equals(target) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        // 1. Later
        int codeLater = executeInteractive("l\n", "answer", "WR-1", "-i");
        assertEquals(0, codeLater);
        assertTrue(out.toString().contains("stays open"));

        // 2. Waive with evidence check (? then empty reason loop then valid reason)
        out = new StringWriter();
        int codeWaive = executeInteractive("?\ny\n\nApproved temporary\n", "answer", "WR-1", "-i");
        assertEquals(0, codeWaive);
        var waiveOut = out.toString();
        assertTrue(waiveOut.contains("Evidence:"));
        assertTrue(waiveOut.contains("Waived coverage >= 80% for fix-relay."));

        // 3. Decline
        out = new StringWriter();
        int codeDecline = executeInteractive("d\nNot allowed\n", "answer", "WR-1", "-i");
        assertEquals(0, codeDecline);
        assertTrue(out.toString().contains("Declined WR-1"));
    }

    @Test
    @DisplayName("questions command with project and plan filter, waivers header, and non-map items")
    void questionsComprehensive() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if (target.startsWith("/api/v1/decisions")) {
                if (target.contains("project=my-proj")) {
                    response.json(200, """
                                                    [
                                                      {"id": "WR-5", "is_waiver": true, "subject": "my-plan", "waiver_gate": "pitest", "title": "Pitest waiver"}
                                                    ]
                                                    """);
                } else {
                    response.json(200, """
                                                    [
                                                      "String item raw",
                                                      {"id": "Q-009", "subject": "start in my-proj", "asking_phase": "entry", "title": "Allow network"}
                                                    ]
                                                    """);
                }
            } else {
                response.send(404, null, "");
            }
        });

        // Filtered project (only waivers)
        int codeWaivers = execute("questions", "--project", "my-proj", "--plan", "my-plan");
        assertEquals(0, codeWaivers);
        var wOut = out.toString();
        assertTrue(wOut.contains("Waiver Requests · 1 pending"));
        assertTrue(wOut.contains("WR-5"));

        // Non-waiver and plain string item
        out = new StringWriter();
        int codeMix = execute("questions");
        assertEquals(0, codeMix);
        var mixOut = out.toString();
        assertTrue(mixOut.contains("Open Questions · 2 pending"));
        assertTrue(mixOut.contains("String item raw"));
        assertTrue(mixOut.contains("Q-009"));
    }

    @Test
    @DisplayName("non-interactive answer command displays waiver details")
    void answerNonInteractiveWaiverDetails() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/WR-1".equals(target) && "GET".equals(request.method())) {
                response.json(200, """
                                                {
                                                  "id": "WR-1",
                                                  "revision": 1,
                                                  "title": "Coverage waiver",
                                                  "subject": "plan fix-relay",
                                                  "scope": "plan",
                                                  "is_waiver": true,
                                                  "waiver_gate": "coverage >= 80%",
                                                  "waiver_reason": "generated files"
                                                }
                                                """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("answer", "WR-1");
        assertEquals(0, code);
        var output = out.toString();
        assertTrue(output.contains("Waiver request WR-1"));
        assertTrue(output.contains("coverage >= 80%"));
        assertTrue(output.contains("generated files"));
    }

    @Test
    @DisplayName("interactive decision prompt handles evidence query and invalid option")
    void answerInteractivePromptEvidenceAndInvalid() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, """
                                                {
                                                  "id": "Q-001",
                                                  "revision": 1,
                                                  "title": "Select merge strategy",
                                                  "subject": "fix-relay",
                                                  "scope": "plan",
                                                  "blocks": "plan",
                                                  "reversibility": "reversible",
                                                  "evidence": {"diff": "+10 -2"},
                                                  "parts": [
                                                    {
                                                      "part_id": "part-1",
                                                      "question": "How?",
                                                      "answer_type": "one_option",
                                                      "options": [{"key": "rebase", "label": "Rebase"}]
                                                    }
                                                  ]
                                                }
                                                """);
            } else if ("/api/v1/decisions/Q-001/answer".equals(target) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        // ? -> invalid -> rebase
        int code = executeInteractive("?\ninvalid\nrebase\n", "answer", "Q-001", "-i");
        assertEquals(0, code);
        var output = out.toString();
        assertTrue(output.contains("Evidence details:"));
        assertTrue(output.contains("Please choose a valid option or action."));
        assertTrue(output.contains("✓ Q-001 answered: Rebase"));
    }

    @Test
    @DisplayName("cli action flags decline and withdraw")
    void answerCliActionDeclineAndWithdraw() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/WR-1/answer".equals(target) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else if ("/api/v1/decisions/Q-001/withdraw".equals(target) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int codeDecline = execute("answer", "WR-1", "--action", "decline", "--reason", "Not approved");
        assertEquals(0, codeDecline);
        assertTrue(out.toString().contains("Answered WR-1 with option decline"));

        out = new StringWriter();
        int codeWithdraw = execute("answer", "Q-001", "--action", "withdraw");
        assertEquals(0, codeWithdraw);
        assertTrue(out.toString().contains("you asked to think again"));
    }
}
