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

@DisplayName("PlanCommand")
class PlanCommandTest {

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
    @DisplayName("plan list lists running plans")
    void planList() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                        [
                          {"id":"p-001","phase":"execute","title":"Implement CLI"},
                          {"id":"p-002","phase":"init","title":"Setup Docs"}
                        ]
                        """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan");
        var output = out.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("Running Plans:")),
                () -> assertTrue(output.contains("p-001")),
                () -> assertTrue(output.contains("Implement CLI")),
                () -> assertTrue(output.contains("p-002"))
        );
    }

    @Test
    @DisplayName("plan list reports no running plans when empty")
    void planListEmpty() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans".equals(request.target())) {
                response.json(200, "[]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan");
        assertEquals(0, code);
        assertTrue(out.toString().contains("No running plans."));
    }

    @Test
    @DisplayName("plan <id> views plan details")
    void planDetail() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-001".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "{\"id\":\"p-001\",\"phase\":\"execute\",\"state\":\"running\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "p-001");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("p-001"))
        );
    }

    @Test
    @DisplayName("plan <id> reports not found on 404")
    void planDetailNotFound() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("plan", "p-missing");
        assertEquals(1, code);
        assertTrue(out.toString().contains("Plan p-missing not found."));
    }

    @Test
    @DisplayName("plan add starts a new plan with options")
    void planAdd() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/plans".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "add", "--title", "Add relay feature", "--id", "relay-feature",
                "--description", "relays messages", "--document", "doc.md", "--issue", "42",
                "--lesson", "L-001", "--local-changes", "--pull-request", "12", "--copy");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ relay-feature has started"))
        );

        var req = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(req.bodyText().contains("\"documents_mode\":\"copy\""));
        assertTrue(req.bodyText().contains("\"local_changes\":true"));
        assertTrue(req.bodyText().contains("\"issues\":[\"42\"]"));
    }

    @Test
    @DisplayName("plan add with project routes to project entries")
    void planAddWithProject() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/projects/my-proj/entries".equals(request.target()) && "POST".equals(request.method())) {
                response.send(200, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "add", "--project", "my-proj", "--title", "Project Plan", "--move");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Project Plan has started"));
    }

    @Test
    @DisplayName("plan pause pauses running plan")
    void planPause() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-001/pause".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "pause", "p-001", "--now");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ p-001 paused"))
        );
    }

    @Test
    @DisplayName("plan pause with after-task or after-phase")
    void planPauseAfter() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/plans/p-001/pause".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "pause", "p-001", "--after-task");
        assertEquals(0, code);
        var req = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(req.bodyText().contains("\"point\":\"after_task\""));
    }

    @Test
    @DisplayName("plan start resumes paused plan")
    void planStart() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-001/start".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "start", "p-001");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ p-001 started"))
        );
    }

    @Test
    @DisplayName("plan cancel cancels a plan with reason")
    void planCancel() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-001/cancel".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "cancel", "p-001", "--reason", "Outdated approach");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ p-001 cancelled and archived"))
        );
    }

    @Test
    @DisplayName("plan abandon abandons a plan")
    void planAbandon() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-001/abandon".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "abandon", "p-001", "--reason", "No longer required");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ p-001 abandoned and archived"))
        );
    }

    @Test
    @DisplayName("plan chat communicates with plan")
    void planChat() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-001/chat".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"reply\":\"hello plan\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "chat", "p-001", "--message", "hello plan");
        assertEquals(0, code);
        assertTrue(out.toString().contains("hello plan"));
    }

    @Test
    @DisplayName("plan report renders completion report or refusal when running")
    void planReport() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-001/completion".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "Completion Report: p-001 SUCCESS (3 tasks, 25m)");
            } else if ("/api/v1/plans/p-running/completion".equals(request.target())) {
                response.send(409, null, "plan_running");
            } else {
                response.send(404, null, "");
            }
        });

        int codeSuccess = execute("plan", "report", "p-001");
        assertAll(
                () -> assertEquals(0, codeSuccess),
                () -> assertTrue(out.toString().contains("Completion Report: p-001"))
        );

        out = new StringWriter();
        int codeRunning = execute("plan", "report", "p-running");
        assertAll(
                () -> assertEquals(1, codeRunning),
                () -> assertTrue(out.toString().contains("p-running is running; its completion report follows when it ends"))
        );
    }
}
