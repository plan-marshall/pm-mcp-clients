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

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        assertTrue(out.toString().contains("rep-404 is no report of this machine; reports lists the unread ones."));
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
    @DisplayName("reports open executes command when detail is present in json")
    void reportsOpenDelegatesToDetail() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/reports/rep-0142".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"id":"rep-0142","detail":"status"}
                                    """);
            } else if ("/api/v1/reports/rep-0142/read".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ok\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports", "open", "rep-0142");
        assertEquals(0, code);
    }

    @Test
    @DisplayName("reports open reports not found on 404")
    void reportsOpenNotFound() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("reports", "open", "rep-missing");
        assertEquals(1, code);
        assertTrue(out.toString().contains("rep-missing is no report of this machine; reports lists the unread ones."));
    }

    @Test
    @DisplayName("reports groups hierarchically by project, epic, and plan")
    void reportsHierarchicalGrouping() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/reports".equals(request.target())) {
                response.json(200, """
                                    {
                                      "reports": [
                                        {
                                          "id": "rep-0142",
                                          "from": "pm-mcp-clients > relay-hardening > fix-relay",
                                          "kind": "plan.ended",
                                          "subject": "Ended: merged (#214)",
                                          "body": "3 of 3 deliverables · 2 h 40 min",
                                          "detail": "plan report fix-relay",
                                          "created": "today 11:20"
                                        },
                                        {
                                          "id": "rep-0139",
                                          "from": "pm-mcp-clients > docs-typo",
                                          "kind": "plan.ended",
                                          "subject": "Ended: no change",
                                          "created": "today 09:02",
                                          "sent": "today 09:40"
                                        },
                                        {
                                          "id": "rep-0131",
                                          "from": "pm-mcp-core",
                                          "kind": "project.onboarded",
                                          "subject": "Onboarded",
                                          "created": "yesterday 18:40"
                                        }
                                      ],
                                      "unread_count": 3,
                                      "oldest_unread": "yesterday 18:40",
                                      "older_count": 0
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Reports · 3 unread")),
                            () -> assertTrue(output.contains("pm-mcp-clients")),
                            () -> assertTrue(output.contains("  relay-hardening")),
                            () -> assertTrue(output.contains("rep-0142")),
                            () -> assertTrue(output.contains("fix-relay")),
                            () -> assertTrue(output.contains("Ended: merged (#214)")),
                            () -> assertTrue(output.contains("reports open rep-0142")),
                            () -> assertTrue(output.contains("docs-typo")),
                            () -> assertTrue(output.contains("sent today 09:40")),
                            () -> assertTrue(output.contains("pm-mcp-core")),
                            () -> assertTrue(output.contains("rep-0131")),
                            () -> assertTrue(output.contains("reports read rep-0142 · reports read --all"))
        );
    }

    @Test
    @DisplayName("reports fails with error on 500")
    void reportsRefused() throws Exception {
        fixture.start((request, response) -> response.send(500, null, ""));

        int code = execute("reports");
        assertEquals(PmOperator.EXIT_FAILURE, code);
    }
}

