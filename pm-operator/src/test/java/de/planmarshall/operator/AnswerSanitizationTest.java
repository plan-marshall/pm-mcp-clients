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
import org.jline.terminal.impl.DumbTerminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Every text of a decision may come from a model or a repository and must reach the terminal only
 * after its control sequences and its invisible characters are removed
 * ({@code doc/design/shared/decisions.adoc#safety},
 * {@code doc/design/terminal-client/04-console.adoc#output-safety}). The dialogue, the
 * {@code --no-input} view, the waiver view and the direct answer are the four paths that print one;
 * the subject, the project and the plan are the three nouns taken from the envelope as they are.
 *
 * <p>Every assertion pairs the absence of the hostile characters with the presence of the text
 * they were hidden in, so a path that printed nothing at all could not pass.
 */
@DisplayName("AnswerCommand strips control sequences and invisible characters from every decision text")
@EnableGeneratorController
class AnswerSanitizationTest {

    private static final String ESC = "\u001B";
    private static final String ESC_JSON = "\\u001b";
    private static final String CSI = ESC_JSON + "[31m";
    private static final String OSC = ESC_JSON + "]0;owned\\u0007";
    private static final String C1_CSI = "\\u009b2J";
    private static final String C1_OSC = "\\u009d0;owned\\u009c";
    /** A right-to-left override and a left-to-right isolate, as JSON escapes. */
    private static final String BIDI = "\\u202e\\u2066";
    /** A zero-width space, the zero-width joiner, the word joiner and the byte order mark. */
    private static final String ZERO_WIDTH = "\\u200b\\u200d\\u2060\\ufeff";
    /** The tag characters U+E0041 and U+E007F, as their surrogate pairs. */
    private static final String TAG = "\\udb40\\udc41\\udb40\\udc7f";
    /** Every class of hostile text at once, as a JSON string fragment. */
    private static final String HOSTILE = CSI + OSC + C1_CSI + C1_OSC + BIDI + ZERO_WIDTH + TAG;

    /** The nouns the answer lines take from the envelope without a catalogue text of their own. */
    enum Noun {
        SUBJECT, PROJECT, PLAN
    }

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
    @DisplayName("the interactive dialogue prints the decision's text without one hostile character")
    void dialogueIsClean() throws Exception {
        var anchor = anchor();
        var terminal = new StringWriter();

        var code = runDialogue(anchor, terminal);

        assertEquals(0, code);
        assertNoControlSequences(out.toString());
        assertNoControlSequences(terminal.toString());
        var rendered = out.toString() + terminal;
        assertTrue(rendered.contains(anchor), () -> "the dialogue printed no text of the decision: " + rendered);
    }

    @Test
    @DisplayName("the --no-input view prints the decision's text without one hostile character")
    void noInputViewIsClean() throws Exception {
        var anchor = anchor();

        var code = runNoInput(anchor);

        assertEquals(3, code);
        assertNoControlSequences(out.toString());
        assertTrue(out.toString().contains(anchor), () -> "the view printed no text of the decision: " + out);
    }

    @Test
    @DisplayName("the waiver view prints its gate and reason without one hostile character")
    void waiverViewIsClean() throws Exception {
        var anchor = anchor();

        var code = runWaiverView(anchor);

        assertEquals(3, code);
        assertNoControlSequences(out.toString());
        assertTrue(out.toString().contains(anchor), () -> "the view printed no reason of the waiver: " + out);
    }

    @Test
    @DisplayName("the direct answer prints the decision's text without one hostile character")
    void directAnswerIsClean() throws Exception {
        var anchor = anchor();

        var code = runDirect(anchor);

        assertEquals(0, code);
        assertNoControlSequences(out.toString());
        assertTrue(out.toString().contains(anchor), () -> "the answer printed no text of the decision: " + out);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Noun.class)
    @DisplayName("each noun of the envelope is printed without one hostile character")
    void nounPrintsNoHostileCharacter(Noun noun) throws Exception {
        var anchor = anchor();

        var rendered = switch (noun) {
            case SUBJECT -> runSubject(anchor);
            case PROJECT -> runProject(anchor);
            case PLAN -> runPlan(anchor);
        };

        assertNoControlSequences(rendered);
        assertTrue(rendered.contains(anchor), () -> "the noun was not printed: " + rendered);
    }

    @Test
    @DisplayName("the direct answer in own words reaches the terminal only through the sink")
    void directWordsAreCleanedByTheSink() throws Exception {
        var anchor = anchor();
        var words = anchor + ESC + "[2J" + (char) 0x202E + (char) 0x200B + (char) 0x200D + (char) 0xFEFF
                            + new String(Character.toChars(0xE0041)) + ESC + "]52;c;owned" + (char) 0x07;
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, envelope("plan fix-relay", "plan", null, null, "Rebase"));
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("answer", "Q-001", "--words", words);

        assertEquals(0, code);
        assertNoControlSequences(out.toString());
        assertTrue(out.toString().contains(anchor), out::toString);
    }

    @Test
    @DisplayName("the sink turns a line feed and a tab of a text into a space, but keeps its own line end")
    void sinkCollapsesLineFeedAndTab() {
        var first = anchor();
        var second = anchor();
        var third = anchor();
        var target = new StringWriter();
        var sink = new SanitizingPrintWriter(new PrintWriter(target));

        sink.println(first + "\n" + second + "\t" + third);
        sink.flush();

        assertEquals(first + " " + second + " " + third + System.lineSeparator(), target.toString());
    }

    @Test
    @DisplayName("the sink drops the invisible characters of every class")
    void sinkDropsInvisibleCharacters() {
        var before = anchor();
        var after = anchor();
        var target = new StringWriter();
        var sink = new SanitizingPrintWriter(new PrintWriter(target));

        sink.print(before + (char) 0x202E + (char) 0x2066 + (char) 0x200D + (char) 0x2060
                            + new String(Character.toChars(0xE0041)) + after);
        sink.flush();

        assertEquals(before + after, target.toString());
    }

    @Test
    @DisplayName("a refusal carries the runtime's body only after it is cleaned")
    void refusalMessageIsCleaned() {
        var anchor = anchor();
        var body = anchor + ESC + "[31m" + (char) 0x202E + "\n" + new String(Character.toChars(0xE007F)) + (char) 0x009B;
        var response = new HttpResponse(409, "text/plain", body.getBytes(StandardCharsets.UTF_8));

        var message = new OperatorRefusedException("answer Q-001", response).getMessage();

        assertNoControlSequences(message);
        assertTrue(message.contains(anchor), message);
    }

    private int runDialogue(String anchor, StringWriter terminal) throws IOException {
        var interpreted = new int[]{0};
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, hostileEnvelope(anchor));
            } else if ("/api/v1/decisions/Q-001/interpret".equals(target)) {
                interpreted[0]++;
                if (interpreted[0] == 1) {
                    response.json(200, """
                                        {
                                          "interpretation_type": "unclear",
                                          "clarification_question": "Which %s branch?",
                                          "understood": false
                                        }
                                        """.formatted(HOSTILE));
                } else {
                    response.json(200, """
                                        {
                                          "interpretation_type": "option",
                                          "read_back_text": "Rebase %s now",
                                          "understood": true
                                        }
                                        """.formatted(HOSTILE));
                }
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
        return executeInteractive("?\n?r\nw\nmy words\nclarify reply\n\n", terminal, "answer", "Q-001", "-i");
    }

    private int runNoInput(String anchor) throws IOException {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-001".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, hostileEnvelope(anchor));
            } else {
                response.send(404, null, "");
            }
        });
        return execute("answer", "Q-001", "--no-input");
    }

    private int runWaiverView(String anchor) throws IOException {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/WR-1".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "id": "WR-1", "revision": 1, "title": "Waiver %s",
                                      "subject": "plan fix-relay", "scope": "plan", "blocks": "plan",
                                      "reversibility": "costly", "is_waiver": true,
                                      "waiver_gate": "coverage %s of 80%%",
                                      "waiver_reason": "%s generated %s files"
                                    }
                                    """.formatted(HOSTILE, HOSTILE, anchor, HOSTILE));
            } else {
                response.send(404, null, "");
            }
        });
        return execute("answer", "WR-1", "--no-input");
    }

    private int runDirect(String anchor) throws IOException {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, hostileEnvelope(anchor));
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
        return execute("answer", "Q-001", "1");
    }

    private String runSubject(String anchor) throws IOException {
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-001".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, envelope("plan " + anchor + HOSTILE, "plan", null, null, "Rebase"));
            } else {
                response.send(404, null, "");
            }
        });
        assertEquals(3, execute("answer", "Q-001", "--no-input"));
        return out.toString();
    }

    private String runProject(String anchor) throws IOException {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, envelope("setup", "setup", anchor + HOSTILE, null, "Rebase"));
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
        assertEquals(0, execute("answer", "Q-001", "later"));
        return out.toString();
    }

    private String runPlan(String anchor) throws IOException {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/WR-1".equals(target) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "id": "WR-1", "revision": 1, "title": "Waiver",
                                      "subject": "plan", "scope": "plan", "blocks": "plan",
                                      "reversibility": "costly", "is_waiver": true,
                                      "waiver_gate": "coverage", "waiver_reason": "generated files",
                                      "plan": "%s%s"
                                    }
                                    """.formatted(anchor, HOSTILE));
            } else if ("/api/v1/decisions/WR-1/answer".equals(target)) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
        assertEquals(0, execute("answer", "WR-1", "waive", "--reason", "covered elsewhere"));
        return out.toString();
    }

    private static String anchor() {
        return Generators.letterStrings(6, 12).next();
    }

    /**
     * Fails when the rendered text carries a control character other than the layout's line feed,
     * carriage return and tab, an escape, a bidi control, a zero-width character or a tag character.
     */
    private static void assertNoControlSequences(String rendered) {
        assertFalse(rendered.contains(ESC), rendered);
        assertFalse(rendered.indexOf(0x9B) >= 0 || rendered.indexOf(0x9D) >= 0,
                            () -> "a C1 control introducer reached the terminal: " + rendered);
        rendered.codePoints().forEach(codePoint -> {
            var layout = codePoint == '\n' || codePoint == '\r' || codePoint == '\t';
            var hostile = (Character.isISOControl(codePoint) && !layout)
                                || (codePoint >= 0x202A && codePoint <= 0x202E)
                                || (codePoint >= 0x2066 && codePoint <= 0x2069)
                                || (codePoint >= 0x200B && codePoint <= 0x200D)
                                || codePoint == 0x2060 || codePoint == 0xFEFF
                                || (codePoint >= 0xE0000 && codePoint <= 0xE007F);
            assertFalse(hostile, () -> "U+%04X reached the terminal: %s".formatted(codePoint, rendered));
        });
    }

    private static String envelope(String subject, String scope, @Nullable String project, @Nullable String plan,
                        String label) {
        var extra = new StringBuilder();
        if (project != null) {
            extra.append(", \"project\": \"").append(project).append('"');
        }
        if (plan != null) {
            extra.append(", \"plan\": \"").append(plan).append('"');
        }
        return """
                            {
                              "id": "Q-001", "revision": 1, "title": "Pick one",
                              "subject": "%s", "scope": "%s", "blocks": "plan",
                              "reversibility": "reversible", "description": "Choose"%s,
                              "parts": [
                                {
                                  "part_id": "part-1", "question": "Which strategy?",
                                  "options": [{"key": "1", "label": "%s"}, {"key": "2", "label": "Merge"}]
                                }
                              ]
                            }
                            """.formatted(subject, scope, extra, label);
    }

    private static String hostileEnvelope(String anchor) {
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
                                    {"key": "1", "label": "%s%s"},
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
                            """.formatted(HOSTILE, HOSTILE, HOSTILE, HOSTILE, HOSTILE, HOSTILE, HOSTILE, HOSTILE,
                            anchor, HOSTILE, HOSTILE, HOSTILE, HOSTILE, HOSTILE);
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
