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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ReportsCommand")
@EnableGeneratorController
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
        var reportId = "rep-" + Generators.integers(1000, 9999).next();
        fixture.start((request, response) -> {
            if ("/api/v1/reports".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    [
                                      {"id":"%s","subject":"fix-relay","summary":"Ended: merged (#214)"}
                                    ]
                                    """.formatted(reportId));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Reports · 1 unread")),
                            () -> assertTrue(output.contains(reportId)),
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
    @DisplayName("reports read --all reads the list first, marks the ids it read, and says nothing about older pages when there are none")
    void reportsReadAll() throws Exception {
        var firstId = "rep-" + Generators.integers(1000, 9999).next();
        var secondId = "rep-" + Generators.integers(1000, 9999).next();
        var thirdId = "rep-" + Generators.integers(1000, 9999).next();
        var unreadCount = Generators.integers(3, 9).next();
        var markedCount = Generators.integers(1, 9).next();
        var server = fixture.start((request, response) -> {
            if ("/api/v1/reports".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "items": [
                                        {"id":"%s","subject":"fix-relay"},
                                        {"id":"%s","subject":"docs-typo"},
                                        {"id":"%s","subject":"Onboarded"}
                                      ],
                                      "unread_count": %d,
                                      "has_older": false,
                                      "older_count": 0
                                    }
                                    """.formatted(firstId, secondId, thirdId, unreadCount));
            } else if ("/api/v1/reports/read".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"count\":" + markedCount + "}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports", "read", "--all");

        var requests = server.requests();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertEquals(2, requests.size(), "one list read and one ids write"),
                            () -> assertEquals("GET", requests.get(0).method()),
                            () -> assertEquals("/api/v1/reports", requests.get(0).target()),
                            () -> assertEquals("POST", requests.get(1).method()),
                            () -> assertEquals("/api/v1/reports/read", requests.get(1).target()),
                            () -> assertTrue(requests.get(0).receivedNanos() <= requests.get(1).receivedNanos(),
                                                "the list read precedes the ids write"),
                            () -> assertTrue(requests.get(1).bodyText().contains(
                                                                    "\"report_ids\":[\"" + firstId + "\",\"" + secondId + "\",\"" + thirdId + "\"]"),
                                                requests.get(1).bodyText()),
                            () -> assertFalse(requests.get(1).bodyText().contains("\"all\""),
                                                requests.get(1).bodyText()),
                            () -> assertTrue(out.toString().contains("✓ " + markedCount + " report"),
                                                out.toString()),
                            () -> assertFalse(out.toString().contains("older unread reports remain"),
                                                "a list with no second page prints no older-page hint: " + out)
        );
    }

    @Test
    @DisplayName("reports read --all says that older unread reports remain when the list has a second page")
    void reportsReadAllWithOlderPage() throws Exception {
        var firstId = "rep-" + Generators.integers(1000, 4999).next();
        var secondId = "rep-" + Generators.integers(5000, 9999).next();
        var markedCount = Generators.integers(2, 9).next();
        var olderCount = Generators.integers(2, 9).next();
        var unreadCount = markedCount + olderCount;
        var server = fixture.start((request, response) -> {
            if ("/api/v1/reports".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "items": [
                                        {"id":"%s","subject":"fix-relay"},
                                        {"id":"%s","subject":"docs-typo"}
                                      ],
                                      "unread_count": %d,
                                      "has_older": true,
                                      "older_count": %d
                                    }
                                    """.formatted(firstId, secondId, unreadCount, olderCount));
            } else if ("/api/v1/reports/read".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"count\":" + markedCount + "}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports", "read", "--all");

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertEquals(2, server.requests().size(), "one list read and one ids write"),
                            () -> assertTrue(out.toString().contains("✓ " + markedCount + " reports marked read."),
                                                out.toString()),
                            () -> assertTrue(out.toString().contains(
                                                                    olderCount + " older unread reports remain. Run reports to see the unread list."),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("reports read --all prints the older-reports notice in the client's language")
    void reportsReadAllWithOlderPageInGerman() throws Exception {
        var firstId = "rep-" + Generators.integers(1000, 4999).next();
        var secondId = "rep-" + Generators.integers(5000, 9999).next();
        var markedCount = Generators.integers(2, 9).next();
        var olderCount = Generators.integers(2, 9).next();
        var unreadCount = markedCount + olderCount;
        var server = fixture.start((request, response) -> {
            if ("/api/v1/reports".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "items": [
                                        {"id":"%s","subject":"fix-relay"},
                                        {"id":"%s","subject":"docs-typo"}
                                      ],
                                      "unread_count": %d,
                                      "has_older": true,
                                      "older_count": %d
                                    }
                                    """.formatted(firstId, secondId, unreadCount, olderCount));
            } else if ("/api/v1/reports/read".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"count\":" + markedCount + "}");
            } else {
                response.send(404, null, "");
            }
        });
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de_DE.UTF-8");
        var context = new OperatorContext(environment, tempDir, true);

        int code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err),
                            "reports", "read", "--all");

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertEquals(2, server.requests().size(), "one list read and one ids write"),
                            () -> assertTrue(out.toString().contains(
                                                                    olderCount + " ältere ungelesene Berichte bleiben."
                                                                                        + " Führe reports aus, um die ungelesene Liste zu sehen."),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("reports read --all prints nothing read and sends no read on an empty unread list")
    void reportsReadAllWithoutUnreadReports() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/reports".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "{\"items\":[],\"unread_count\":0,\"has_older\":false,\"older_count\":0}");
            } else {
                response.send(500, null, "");
            }
        });

        int code = execute("reports", "read", "--all");

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("0 reports marked read.")),
                            () -> assertEquals(1, server.requests().size(), "only the list read may be sent"),
                            () -> assertEquals("/api/v1/reports", server.requests().getFirst().target())
        );
    }

    @Test
    @DisplayName("reports read without IDs or --all prints its local refusal on stderr and returns 1")
    void reportsReadNoIds() {
        int code = execute("reports", "read");
        assertEquals(1, code);
        assertTrue(err.toString().contains("✗ Give the report ids, or --all."), err.toString());
        assertFalse(out.toString().contains("Give the report ids"), out.toString());
    }

    @Test
    @DisplayName("reports read with specific IDs marks them read")
    void reportsReadSpecificIds() throws Exception {
        var presentId = "rep-" + Generators.integers(1000, 4999).next();
        var missingId = "rep-" + Generators.integers(5000, 9999).next();
        fixture.start((request, response) -> {
            if (("/api/v1/reports/" + presentId + "/read").equals(request.target())) {
                response.send(200, null, "");
            } else if (("/api/v1/reports/" + missingId + "/read").equals(request.target())) {
                response.send(404, null, "");
            } else {
                response.send(500, null, "");
            }
        });

        int code = execute("reports", "read", presentId, missingId);
        assertEquals(0, code);
        assertTrue(out.toString().contains(missingId + " is no report of this machine; reports lists the unread ones."));
        assertTrue(out.toString().contains("✓ 1 report marked read."), out.toString());
    }

    @Test
    @DisplayName("reports open prints report detail and marks read")
    void reportsOpen() throws Exception {
        var reportId = "rep-" + Generators.integers(1000, 9999).next();
        fixture.start((request, response) -> {
            if (("/api/v1/reports/" + reportId).equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "Report Detail: " + reportId + " fix-relay merged");
            } else if (("/api/v1/reports/" + reportId + "/read").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports", "open", reportId);
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("Report Detail: " + reportId + " fix-relay merged"))
        );
    }

    @Test
    @DisplayName("reports open executes command when detail is present in json")
    void reportsOpenDelegatesToDetail() throws Exception {
        var reportId = "rep-" + Generators.integers(1000, 9999).next();
        fixture.start((request, response) -> {
            if (("/api/v1/reports/" + reportId).equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"id":"%s","detail":"status"}
                                    """.formatted(reportId));
            } else if (("/api/v1/reports/" + reportId + "/read").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ok\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports", "open", reportId);
        assertEquals(0, code);
    }

    @Test
    @DisplayName("reports open reports not found on 404")
    void reportsOpenNotFound() throws Exception {
        var missingId = "rep-" + Generators.integers(1000, 9999).next();
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("reports", "open", missingId);
        assertEquals(1, code);
        var line = missingId + " is no report of this machine; reports lists the unread ones.";
        assertTrue(err.toString().contains(line), err.toString());
        assertFalse(out.toString().contains(line), out.toString());
    }

    @Test
    @DisplayName("reports groups hierarchically by project, epic, and plan")
    void reportsHierarchicalGrouping() throws Exception {
        var firstId = "rep-" + Generators.integers(1000, 3999).next();
        var secondId = "rep-" + Generators.integers(4000, 6999).next();
        var thirdId = "rep-" + Generators.integers(7000, 9999).next();
        var unreadCount = Generators.integers(3, 9).next();
        fixture.start((request, response) -> {
            if ("/api/v1/reports".equals(request.target())) {
                response.json(200, """
                                    {
                                      "reports": [
                                        {
                                          "id": "%s",
                                          "from": "pm-mcp-clients > relay-hardening > fix-relay",
                                          "kind": "plan.ended",
                                          "subject": "Ended: merged (#214)",
                                          "body": "3 of 3 deliverables · 2 h 40 min",
                                          "detail": "plan report fix-relay",
                                          "created": "today 11:20"
                                        },
                                        {
                                          "id": "%s",
                                          "from": "pm-mcp-clients > docs-typo",
                                          "kind": "plan.ended",
                                          "subject": "Ended: no change",
                                          "created": "today 09:02",
                                          "sent": "today 09:40"
                                        },
                                        {
                                          "id": "%s",
                                          "from": "pm-mcp-core",
                                          "kind": "project.onboarded",
                                          "subject": "Onboarded",
                                          "created": "yesterday 18:40"
                                        }
                                      ],
                                      "unread_count": %d,
                                      "oldest_unread": "yesterday 18:40",
                                      "older_count": 0
                                    }
                                    """.formatted(firstId, secondId, thirdId, unreadCount));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("reports");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Reports · " + unreadCount + " unread")),
                            () -> assertTrue(output.contains("pm-mcp-clients")),
                            () -> assertTrue(output.contains("  relay-hardening")),
                            () -> assertTrue(output.contains(firstId)),
                            () -> assertTrue(output.contains("fix-relay")),
                            () -> assertTrue(output.contains("Ended: merged (#214)")),
                            () -> assertTrue(output.contains("reports open " + firstId)),
                            () -> assertTrue(output.contains("docs-typo")),
                            () -> assertTrue(output.contains("sent today 09:40")),
                            () -> assertTrue(output.contains("pm-mcp-core")),
                            () -> assertTrue(output.contains(thirdId)),
                            () -> assertTrue(output.contains("reports read " + firstId + " · reports read --all"))
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

