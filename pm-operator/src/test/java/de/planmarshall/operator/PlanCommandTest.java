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
import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    @DisplayName("plan add starts a new plan from its sources")
    void planAdd() throws Exception {
        var outcome = StartTestSupport.run(StandardScenarios.START_SOURCES, null,
                            "plan", "add", "--project", "pm-mcp-clients", "--title", "Add relay feature",
                            "--id", "relay-feature", "--description", "relays messages",
                            "--document", "doc.md", "--issue", "42", "--copy", "--no-input");
        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("relay-feature has started"),
                                                outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("relays messages"), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("copied"), outcome.rendered()));
    }

    @Test
    @DisplayName("plan add with project opens the entry under that project")
    void planAddWithProject() throws Exception {
        var outcome = StartTestSupport.run(StandardScenarios.START_SOURCES, null,
                            "plan", "add", "--project", "pm-mcp-clients", "--title", "Project Plan",
                            "--description", "A project plan", "--no-input");
        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("project-plan has started"),
                                                outcome.rendered()));
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

        int codeRunning = execute("plan", "report", "p-running");
        assertAll(
                            () -> assertEquals(1, codeRunning),
                            () -> assertTrue(out.toString().contains("p-running is running; its completion report follows when it ends"))
        );
    }

    @Test
    @DisplayName("plan report renders structured completion report, tokens, and components")
    void planReportStructured() throws Exception {
        var jsonReport = """
                            {
                              "plan": "fix-relay",
                              "outcome": "merged",
                              "outcome_description": "merged: the pull request #214 was merged into main",
                              "pull_request": "214",
                              "pull_request_url": "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                              "ended": "today 11:20",
                              "from": "pm-mcp-clients > relay-hardening > fix-relay",
                              "reason": "—",
                              "ran": "2 h 40 min, 08:40 to 11:20 · paused 20 min by you",
                              "run_duration_seconds": 9600,
                              "deliverables": [
                                {"title": "cursor write order", "state": "done"},
                                {"title": "a test per transport, websocket and SSE", "state": "done"},
                                {"title": "fsync of the cursor file", "state": "done"}
                              ],
                              "replanning": [
                                {"trigger": "the base moved", "time": "09:55", "change": "TASK-5 added"}
                              ],
                              "decisions": [
                                {"id": "Q-003", "text": "Rebase onto main", "by": "you, the web app"},
                                {"id": "Q-005", "text": "", "by": "the plan"}
                              ],
                              "tokens": {
                                "total": 412000,
                                "input": 380000,
                                "output": 32000,
                                "floor": true,
                                "coverage": {
                                  "total_tasks": 41,
                                  "unmeasured": 3,
                                  "reason": "their worker was terminated",
                                  "workers": 6,
                                  "models": 3
                                },
                                "by_phase": [
                                  {
                                    "name": "refine",
                                    "total": 38000,
                                    "input": 35000,
                                    "output": 3000,
                                    "floor": false,
                                    "unmeasured": 0,
                                    "components": {
                                      "input": 25000,
                                      "cache_read": 10000,
                                      "cache_write": 0,
                                      "output": 2500,
                                      "reasoning": 500
                                    }
                                  }
                                ],
                                "by_role": [
                                  {
                                    "name": "planner",
                                    "harness_model": "claude:opus-5.5",
                                    "effort": "high",
                                    "total": 128000,
                                    "input": 120000,
                                    "output": 8000,
                                    "floor": false,
                                    "unmeasured": 0,
                                    "components": {
                                      "input": 100000,
                                      "cache_read": 20000,
                                      "cache_write": 0,
                                      "output": 7000,
                                      "reasoning": 1000
                                    }
                                  }
                                ],
                                "by_model": [
                                  {
                                    "name": "claude:opus-5.5",
                                    "total": 128000,
                                    "input": 120000,
                                    "output": 8000,
                                    "floor": false,
                                    "unmeasured": 0
                                  }
                                ],
                                "waste": {
                                  "total": 52000,
                                  "share_percent": 13,
                                  "no_product_tokens": 18000,
                                  "no_product_tasks": 2,
                                  "retries_tokens": 21000,
                                  "retries_causes": ["retryable 1", "stall 1"],
                                  "remediation_tokens": 13000,
                                  "remediation_causes": ["returned with findings 1"]
                                }
                              }
                            }
                            """;

        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/completion".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, jsonReport);
            } else {
                response.send(404, null, "");
            }
        });

        // 1. Basic report
        int code = execute("plan", "report", "fix-relay");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("fix-relay · completion report · merged · #214 · today 11:20")),
                            () -> assertTrue(output.contains("in pm-mcp-clients › relay-hardening")),
                            () -> assertTrue(output.contains("3 of 3 done")),
                            () -> assertTrue(output.contains("cursor write order")),
                            () -> assertTrue(output.contains("after the base moved (09:55): TASK-5 added")),
                            () -> assertTrue(output.contains("plan report fix-relay --tokens"))
        );

        // 2. With --tokens
        out = new StringWriter();
        int codeTokens = execute("plan", "report", "fix-relay", "--tokens");
        var outputTokens = out.toString();
        assertAll(
                            () -> assertEquals(0, codeTokens),
                            () -> assertTrue(outputTokens.contains("by phase")),
                            () -> assertTrue(outputTokens.contains("refine")),
                            () -> assertTrue(outputTokens.contains("by role")),
                            () -> assertTrue(outputTokens.contains("planner")),
                            () -> assertTrue(outputTokens.contains("claude:opus-5.5")),
                            () -> assertTrue(outputTokens.contains("waste")),
                            () -> assertTrue(outputTokens.contains("13 % of measured")),
                            () -> assertTrue(outputTokens.contains("raw components: plan report fix-relay --tokens --components"))
        );

        // 3. With --tokens --components
        out = new StringWriter();
        int codeComp = execute("plan", "report", "fix-relay", "--tokens", "--components");
        var outputComp = out.toString();
        assertAll(
                            () -> assertEquals(0, codeComp),
                            () -> assertTrue(outputComp.contains("input")),
                            () -> assertTrue(outputComp.contains("cache read")),
                            () -> assertTrue(outputComp.contains("output")),
                            () -> assertTrue(outputComp.contains("reasoning"))
        );
    }

    @Test
    @DisplayName("plan report reports running plan with 409 conflict")
    void planReportRunning() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/running-plan/completion".equals(request.target())) {
                response.json(409, """
                                    {"error":"plan_running","state":"running"}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "report", "running-plan");
        assertEquals(1, code);
        assertTrue(out.toString().contains("running-plan is running"));
    }

    @Test
    @DisplayName("plan report reports 404 when plan not found")
    void planReportNotFound() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("plan", "report", "non-existent");
        assertEquals(1, code);
        assertTrue(out.toString().contains("Plan non-existent not found."));
    }

    @Test
    @DisplayName("plan report falls back to raw text for non-JSON 200")
    void planReportRawText() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/plain-plan/completion".equals(request.target())) {
                response.send(200, "text/plain", "Raw completion report text");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "report", "plain-plan");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Raw completion report text"));
    }
}

