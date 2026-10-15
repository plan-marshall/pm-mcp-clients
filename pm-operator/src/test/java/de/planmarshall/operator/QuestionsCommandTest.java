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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

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
                () -> assertTrue(output.contains("Open Questions")),
                () -> assertTrue(output.contains("Q-007")),
                () -> assertTrue(output.contains("fix-relay")),
                () -> assertTrue(output.contains("Resolve rebase conflict"))
        );
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
}
