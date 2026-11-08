/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Every response is checked before the dialogue prints success, and the nouns of the result lines
 * come from the envelope's scope, blocks, project and plan, never from its subject text.
 */
@DisplayName("AnswerCommand checks responses and takes its nouns from the envelope")
class AnswerRefusalTest {

    private static final String REFUSED = "refused by the runtime";

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
        fixture.start((request, response) -> {
            if ("/api/v1/decisions/Q-001".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, planEnvelope());
            } else if ("/api/v1/decisions/Q-001/withdraw".equals(request.target())) {
                response.json(500, "{\"error\":\"withdraw blocked\"}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = run("en", "x\n\n".getBytes(StandardCharsets.UTF_8), "answer", "Q-001", "-i");

        assertNotEquals(0, code);
        assertTrue(err.toString().contains(REFUSED), err.toString());
    }

    @Test
    @DisplayName("an own-words answer the runtime refuses exits non-zero with the refusal")
    void refusedOwnWordsAnswer() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-001".equals(target) && "GET".equals(request.method())) {
                response.json(200, ownWordsEnvelope());
            } else if ("/api/v1/decisions/Q-001/interpret".equals(target)) {
                response.json(200, """
                                    {"interpretation_type":"option","read_back_text":"Rebase","understood":true}
                                    """);
            } else if ("/api/v1/decisions/Q-001/answer".equals(target)) {
                response.json(500, "{\"error\":\"answer blocked\"}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = run("en", "w\nrebase\n\n".getBytes(StandardCharsets.UTF_8), "answer", "Q-001", "-i");

        assertNotEquals(0, code);
        assertTrue(err.toString().contains(REFUSED), err.toString());
    }

    @Test
    @DisplayName("answer Q-999 later checks that the decision exists")
    void laterOnUnknownDecision() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        var code = run("en", new byte[0], "answer", "Q-999", "later");

        assertNotEquals(0, code);
        assertTrue(err.toString().contains(REFUSED), err.toString());
    }

    @Test
    @DisplayName("the German result lines select the noun of a plan, an epic and an entry")
    void germanNouns() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/decisions/Q-PLAN".equals(target)) {
                response.json(200, envelope("Q-PLAN", "plan", "plan", "plan storage-v2", "pm-mcp-clients"));
            } else if ("/api/v1/decisions/Q-EPIC".equals(target)) {
                response.json(200, envelope("Q-EPIC", "epic", "epic", "epic ui-improvements", "pm-mcp-clients"));
            } else if ("/api/v1/decisions/Q-ENTRY".equals(target)) {
                response.json(200, envelope("Q-ENTRY", "plan", "entry", "start in pm-mcp-clients", "pm-mcp-clients"));
            } else if (target.endsWith("/answer") || target.endsWith("/withdraw")) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        assertGerman("der Plan wartet darauf.", "answer", "Q-PLAN", "later");
        assertGerman("du hast den Plan gebeten, neu zu \u00fcberlegen.", "answer", "Q-PLAN", "x");
        assertGerman("das Epic wartet darauf.", "answer", "Q-EPIC", "later");
        assertGerman("du hast das Epic gebeten, neu zu \u00fcberlegen.", "answer", "Q-EPIC", "x");
        assertGerman("der Start in pm-mcp-clients wartet darauf.", "answer", "Q-ENTRY", "later");
        assertGerman("du hast den Start in pm-mcp-clients gebeten, neu zu \u00fcberlegen.", "answer", "Q-ENTRY", "x");
    }

    private void assertGerman(String expected, String... args) throws Exception {
        out = new StringWriter();
        err = new StringWriter();
        var code = run("de", new byte[0], args);
        assertTrue(code == 0, "exit " + code + ": " + err);
        assertTrue(out.toString().contains(expected), out.toString());
    }

    private int run(String language, byte[] input, String... args) throws IOException {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", language.equals("de") ? "de_DE.UTF-8" : "en_US.UTF-8");
        var terminalBytes = new ByteArrayOutputStream();
        InputStream inStream = new ByteArrayInputStream(input);
        var termProvider = (OperatorContext.TerminalProvider) () -> new DumbTerminal(inStream, terminalBytes);
        var context = new OperatorContext(environment, tempDir, true,
                            ClientEnvironment.capture(environment), termProvider, null);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
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
