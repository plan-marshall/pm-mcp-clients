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
import java.time.Duration;

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
    @DisplayName("reports reports nothing new when unread list is empty")
    void reportsEmpty() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/reports".equals(request.target())) {
                response.json(200, "[]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Nothing new. Reports you have read are not shown any more."));
    }

    @Test
    @DisplayName("reports queries with project and older options")
    void reportsFilteredAndOlder() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().contains("/api/v1/reports")) {
                response.json(200, "[]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports", "--project", "my-project", "--older");
        assertEquals(0, code);
        var req = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(req.target().contains("project=my-project"));
        assertTrue(req.target().contains("older=true"));
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
    @DisplayName("reports read without IDs or --all prints hint and returns 1")
    void reportsReadNoIds() {
        int code = execute("reports", "read");
        assertEquals(1, code);
        assertTrue(out.toString().contains("Specify report IDs or --all."));
    }

    @Test
    @DisplayName("reports read with specific IDs marks them read")
    void reportsReadSpecificIds() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/reports/rep-001/read".equals(request.target())) {
                response.send(200, null, "");
            } else if ("/api/v1/reports/rep-404/read".equals(request.target())) {
                response.send(404, null, "");
            } else {
                response.send(500, null, "");
            }
        });

        int code = execute("reports", "read", "rep-001", "rep-404");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Report rep-404 not found."));
        assertTrue(out.toString().contains("✓ 1 reports marked read."));
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

    @Test
    @DisplayName("reports open reports not found on 404")
    void reportsOpenNotFound() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("reports", "open", "rep-missing");
        assertEquals(1, code);
        assertTrue(out.toString().contains("Report rep-missing not found."));
    }

    @Test
    @DisplayName("reports fails with error on 500")
    void reportsRefused() throws Exception {
        fixture.start((request, response) -> response.send(500, null, ""));

        int code = execute("reports");
        assertEquals(PmOperator.EXIT_FAILURE, code);
    }
}
