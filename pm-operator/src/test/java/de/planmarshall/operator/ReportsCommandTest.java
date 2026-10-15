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

@DisplayName("ReportsCommand")
class ReportsCommandTest {

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
    @DisplayName("reports lists unread reports")
    void reportsList() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/reports".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                        [
                          {"id":"rep-0142","subject":"fix-relay","summary":"Ended: merged (#214)"}
                        ]
                        """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports");
        var output = out.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("Reports · 1 unread")),
                () -> assertTrue(output.contains("rep-0142")),
                () -> assertTrue(output.contains("fix-relay"))
        );
    }

    @Test
    @DisplayName("reports read --all marks all read")
    void reportsReadAll() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/reports/read".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"count\":3}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports", "read", "--all");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ 3 reports marked read."))
        );
    }

    @Test
    @DisplayName("reports open prints report detail and marks read")
    void reportsOpen() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/reports/rep-0142".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "Report Detail: rep-0142 fix-relay merged");
            } else if ("/api/v1/reports/rep-0142/read".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports", "open", "rep-0142");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("Report Detail: rep-0142 fix-relay merged"))
        );
    }
}
