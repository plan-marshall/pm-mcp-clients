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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Every text of a decision may come from a model or a repository and must reach the terminal only
 * after its control sequences are removed ({@code doc/design/shared/decisions.adoc#safety}). The
 * dialogue, the {@code --no-input} view and the direct answer are the three paths that print one.
 */
@DisplayName("AnswerCommand strips control sequences from every decision text")
class AnswerSanitizationTest {

    private static final String ESC = "\u001B";
    private static final String ESC_JSON = "\\u001b";
    private static final String CSI = ESC_JSON + "[31m";
    private static final String OSC = ESC_JSON + "]0;owned\\u0007";
    private static final String C1_CSI = "\\u009b2J";
    private static final String C1_OSC = "\\u009d0;owned\\u009c";

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
    @DisplayName("the interactive dialogue shows no control sequence of the envelope")
    void dialogueIsClean() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, hostileEnvelope());
            } else if ("/api/v1/decisions/Q-001/interpret".equals(target)) {
                response.json(200, """
                                    {
                                      "interpretation_type": "unclear",
                                      "clarification_question": "Which %s branch%s?",
                                      "understood": false
                                    }
                                    """.formatted(CSI, C1_CSI));
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        var terminal = new StringWriter();
        var code = executeInteractive("?\n?r\nw\nmy words\nclarify reply\n\n", terminal, "answer", "Q-001", "-i");

        assertEquals(0, code);
        assertNoControlSequences(out.toString());
        assertNoControlSequences(terminal.toString());
    }

    @Test
    @DisplayName("the --no-input view shows no control sequence of the envelope")
    void noInputViewIsClean() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-001".equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, hostileEnvelope());
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("answer", "Q-001", "--no-input");

        assertEquals(3, code);
        assertNoControlSequences(out.toString());
    }

    @Test
    @DisplayName("the direct answer shows no control sequence of the envelope")
    void directAnswerIsClean() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, hostileEnvelope());
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("answer", "Q-001", "1");

        assertEquals(0, code);
        assertNoControlSequences(out.toString());
    }

    private static void assertNoControlSequences(String rendered) {
        for (int i = 0; i < rendered.length(); i++) {
            var c = rendered.charAt(i);
            var legalLayout = c == '\n' || c == '\r' || c == '\t';
            assertFalse(Character.isISOControl(c) && !legalLayout,
                                () -> "control character U+%04X reached the terminal: %s".formatted((int) c, rendered));
        }
        assertFalse(rendered.contains(ESC), rendered);
    }

    private static String hostileEnvelope() {
        return """
                            {
                              "id": "Q-001",
                              "revision": 1,
                              "title": "Pick one",
                              "subject": "plan fix-relay%s",
                              "scope": "plan",
                              "asking_phase": "phase-5%s",
                              "blocks": "plan",
                              "reversibility": "reversible",
                              "description": "Choose",
                              "evidence": {
                                "file": "src/Main%s.java",
                                "lines": "1%s",
                                "summary": "mismatch%s"
                              },
                              "parts": [
                                {
                                  "part_id": "part-1",
                                  "question": "Which strategy%s?",
                                  "recommendation": "1",
                                  "recommending_role": "implement%s",
                                  "recommendation_rationale": "Keeps history linear%s",
                                  "allow_own_words": true,
                                  "allow_note": true,
                                  "options": [
                                    {"key": "1", "label": "Rebase%s"},
                                    {
                                      "key": "2",
                                      "label": "Merge%s",
                                      "quorum_support": {
                                        "band": "split",
                                        "votes_count": 1,
                                        "total_votes": 3,
                                        "models_count": 1,
                                        "dissent_summary": "context lost%s",
                                        "votes": [{"role": "reviewer%s", "reason": "why%s"}]
                                      }
                                    }
                                  ]
                                }
                              ]
                            }
                            """.formatted(CSI + OSC + C1_CSI, CSI, CSI, C1_OSC, OSC, CSI, OSC + C1_CSI,
                            C1_OSC, CSI, OSC + C1_CSI, CSI, OSC, CSI + C1_OSC);
    }

    private int execute(String... args) {
        return run(new ByteArrayInputStream(new byte[0]), new StringWriter(), args);
    }

    private int executeInteractive(String input, StringWriter terminalText, String... args) {
        return run(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), terminalText, args);
    }

    private int run(InputStream inStream, StringWriter terminalText, String... args) {
        var terminalBytes = new ByteArrayOutputStream();
        var termProvider = (OperatorContext.TerminalProvider) () -> new DumbTerminal(inStream, terminalBytes);
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()), termProvider, null);
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
        terminalText.write(terminalBytes.toString(StandardCharsets.UTF_8));
        return code;
    }
}
