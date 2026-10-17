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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("PlanViewCommandTest")
class PlanViewCommandTest {

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
    @DisplayName("renders indented process tree with glyphs, path header, and tokens")
    void shouldRenderIndentedProcessTree() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/process".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "plan_id": "fix-relay",
                                      "title": "Fix Relay Transport",
                                      "project": {
                                        "project_id": "pm-mcp-clients",
                                        "display_name": "pm-mcp-clients"
                                      },
                                      "epic": {
                                        "epic_id": "relay-hardening",
                                        "title": "relay-hardening"
                                      },
                                      "current_phase": "execute",
                                      "phase_state": "running",
                                      "duration_seconds": 7800,
                                      "pull_request_number": 214,
                                      "pull_request_url": "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                                      "tokens_so_far": {
                                        "total": 412000,
                                        "input": 380000,
                                        "output": 32000,
                                        "floor": false,
                                        "tasks_total": 7,
                                        "tasks_measured": 7,
                                        "none_yet": false,
                                        "not_measured": false
                                      },
                                      "nodes": [
                                        {
                                          "node_id": "init",
                                          "title": "phase init",
                                          "kind": "phase",
                                          "state": "done",
                                          "duration_seconds": 120,
                                          "sub_nodes": [],
                                          "leaf_tasks": []
                                        },
                                        {
                                          "node_id": "execute",
                                          "title": "phase execute",
                                          "kind": "phase",
                                          "state": "running",
                                          "duration_seconds": 1500,
                                          "sub_nodes": [
                                            {
                                              "node_id": "envelope-ready",
                                              "title": "envelope-ready",
                                              "kind": "state",
                                              "state": "running",
                                              "run_count": 3,
                                              "current_run": 3,
                                              "duration_seconds": 360,
                                              "sub_nodes": [],
                                              "leaf_tasks": [
                                                {
                                                  "task_id": "TASK-001",
                                                  "role": "implement",
                                                  "harness": "claude",
                                                  "model": "sonnet",
                                                  "attempt": 1,
                                                  "status": "done",
                                                  "duration_seconds": 600,
                                                  "tokens_so_far": 12000
                                                },
                                                {
                                                  "task_id": "TASK-003",
                                                  "role": "implement",
                                                  "harness": "claude",
                                                  "model": "sonnet",
                                                  "attempt": 1,
                                                  "status": "running",
                                                  "duration_seconds": 360,
                                                  "tokens_so_far": 48000
                                                }
                                              ]
                                            }
                                          ],
                                          "leaf_tasks": []
                                        },
                                        {
                                          "node_id": "finalize",
                                          "title": "phase finalize",
                                          "kind": "phase",
                                          "state": "planned",
                                          "duration_seconds": 0,
                                          "sub_nodes": [],
                                          "leaf_tasks": []
                                        }
                                      ],
                                      "current_position_path": ["execute", "envelope-ready", "TASK-003"]
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "fix-relay");
        var output = out.toString();

        assertAll(
                            () -> assertEquals(0, code),
                            // Path navigation header
                            () -> assertTrue(output.contains("pm-mcp-clients › relay-hardening › fix-relay")),
                            // Plan overview line
                            () -> assertTrue(output.contains("fix-relay · execute · running")),
                            () -> assertTrue(output.contains("#214")),
                            // Context commands
                            () -> assertTrue(output.contains("status --project pm-mcp-clients")),
                            // Tokens line
                            () -> assertTrue(output.contains("tokens so far")),
                            () -> assertTrue(output.contains("412 k")),
                            // Tree nodes with glyphs and labels
                            () -> assertTrue(output.contains("✓ done      phase init")),
                            () -> assertTrue(output.contains("▶ running   phase execute")),
                            () -> assertTrue(output.contains("○ planned   phase finalize")),
                            // Sub-nodes
                            () -> assertTrue(output.contains("envelope-ready")),
                            // Leaf tasks beneath node
                            () -> assertTrue(output.contains("TASK-001 (implement)")),
                            () -> assertTrue(output.contains("TASK-003 (implement)"))
        );
    }

    @Test
    @DisplayName("formats durations correctly for sub-minute, minute, and multi-hour intervals")
    void shouldFormatPlanDurationsCorrectly() {
        assertAll(
                            () -> assertEquals("0 s", PlanCommand.formatPlanDuration(0)),
                            () -> assertEquals("45 s", PlanCommand.formatPlanDuration(45)),
                            () -> assertEquals("2 min", PlanCommand.formatPlanDuration(120)),
                            () -> assertEquals("1 h 1 min", PlanCommand.formatPlanDuration(3660))
        );
    }

    @Test
    @DisplayName("renders pruned phases with · not in this plan")
    void shouldRenderPrunedPhases() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/docs-typo/process".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "plan_id": "docs-typo",
                                      "title": "Fix Documentation Typo",
                                      "project": {
                                        "project_id": "pm-mcp-clients"
                                      },
                                      "current_phase": "execute",
                                      "phase_state": "running",
                                      "duration_seconds": 180,
                                      "tokens_so_far": {
                                        "total": 12000,
                                        "input": 11000,
                                        "output": 1000,
                                        "floor": false,
                                        "tasks_total": 2,
                                        "tasks_measured": 2,
                                        "none_yet": false,
                                        "not_measured": false
                                      },
                                      "nodes": [
                                        {
                                          "node_id": "init",
                                          "title": "phase init",
                                          "kind": "phase",
                                          "state": "done",
                                          "duration_seconds": 60,
                                          "sub_nodes": [],
                                          "leaf_tasks": []
                                        },
                                        {
                                          "node_id": "refine",
                                          "title": "refine, outline, plan",
                                          "kind": "phase",
                                          "state": "not_in_plan",
                                          "prune_reason": "lane_choice",
                                          "duration_seconds": 0,
                                          "sub_nodes": [],
                                          "leaf_tasks": []
                                        }
                                      ],
                                      "current_position_path": ["init"]
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "docs-typo");
        var output = out.toString();

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("pm-mcp-clients › docs-typo")),
                            () -> assertTrue(output.contains("· not in this plan: refine, outline, plan (lane choice)"))
        );
    }

    @Test
    @DisplayName("renders worker task activity with model text, tools, and off-track warning")
    void shouldRenderWorkerTaskActivity() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/tasks/TASK-003/activity".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "task_id": "TASK-003",
                                      "role": "implement",
                                      "harness": "claude",
                                      "model": "sonnet",
                                      "attempt": 1,
                                      "duration_seconds": 360,
                                      "tokens_so_far": 48000,
                                      "entries": [
                                        {
                                          "timestamp": "14:02:11",
                                          "kind": "text",
                                          "content": "Reading the relay's transport before changing it."
                                        },
                                        {
                                          "timestamp": "14:02:14",
                                          "kind": "tool_call",
                                          "tool_name": "read",
                                          "tool_target": "pm-relay/src/main/java/Transport.java",
                                          "tool_failed": false
                                        },
                                        {
                                          "timestamp": "14:02:20",
                                          "kind": "tool_call",
                                          "tool_name": "bash",
                                          "tool_target": "./mvnw test",
                                          "tool_failed": true,
                                          "size": "4 KiB"
                                        },
                                        {
                                          "timestamp": "14:03:02",
                                          "kind": "file_changed",
                                          "files": ["pm-relay/src/main/java/Transport.java"]
                                        }
                                      ],
                                      "off_track": {
                                        "signal": "repeating",
                                        "action": "shown",
                                        "value": 5,
                                        "threshold": 5
                                      }
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "fix-relay", "--task", "TASK-003");
        var output = out.toString();

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("fix-relay · TASK-003")),
                            () -> assertTrue(output.contains("implement · claude sonnet · attempt 1")),
                            () -> assertTrue(output.contains("wrote      Reading the relay's transport")),
                            () -> assertTrue(output.contains("tool       read pm-relay/src/main/java/Transport.java")),
                            () -> assertTrue(output.contains("failed · 4 KiB")),
                            () -> assertTrue(output.contains("changed    pm-relay/src/main/java/Transport.java")),
                            () -> assertTrue(output.contains("⚠ off track  repeating (5 of 5): shown"))
        );
    }

    @Test
    @DisplayName("sanitizes untrusted terminal control sequences in activity log")
    void shouldSanitizeTerminalControlSequences() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/tasks/TASK-INJECT/activity".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "task_id": "TASK-INJECT",
                                      "role": "implement",
                                      "harness": "claude",
                                      "model": "sonnet",
                                      "attempt": 1,
                                      "duration_seconds": 60,
                                      "tokens_so_far": 1000,
                                      "entries": [
                                        {
                                          "timestamp": "14:00:00",
                                          "kind": "text",
                                          "content": "Malicious \\u001B[31mInjected\\u001B[0m text"
                                        }
                                      ]
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "fix-relay", "--task", "TASK-INJECT");
        var output = out.toString();

        assertEquals(0, code);
        assertFalse(output.contains("\u001B[31m"), "Control sequence must be stripped");
        assertTrue(output.contains("Malicious Injected text"));
    }

    @Test
    @DisplayName("renders epic process view with running and staged plans and review status")
    void shouldRenderEpicProcess() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/relay-hardening/process".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "epic_id": "relay-hardening",
                                      "project": "pm-mcp-clients",
                                      "title": "Relay Hardening",
                                      "state": "running",
                                      "running_plans": [
                                        {
                                          "plan_id": "fix-relay",
                                          "title": "Fix Relay Transport",
                                          "current_phase": "execute",
                                          "phase_state": "running",
                                          "tasks_done": 4,
                                          "tasks_total": 7,
                                          "running": true
                                        }
                                      ],
                                      "staged_plans": [
                                        {
                                          "plan_id": "relay-auth-v2",
                                          "title": "Relay Auth v2",
                                          "current_phase": "init",
                                          "phase_state": "staged",
                                          "tasks_done": 0,
                                          "tasks_total": 4,
                                          "running": false
                                        }
                                      ],
                                      "alongside_landings": ["remediation.ci-failure · done"],
                                      "review_ready": false
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "relay-hardening");
        var output = out.toString();

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("pm-mcp-clients › relay-hardening")),
                            () -> assertTrue(output.contains("relay-hardening · epic · running · 1 running · 1 staged")),
                            () -> assertTrue(output.contains("running plans:")),
                            () -> assertTrue(output.contains("▶ running   fix-relay")),
                            () -> assertTrue(output.contains("staged plans:")),
                            () -> assertTrue(output.contains("○ staged    relay-auth-v2")),
                            () -> assertTrue(output.contains("completion review: not ready")),
                            () -> assertTrue(output.contains("remediation.ci-failure · done"))
        );
    }

    @Test
    @DisplayName("renders process tree with leafJob, depth options, and tokens variations")
    void shouldRenderLeafJobAndDepthOptions() throws Exception {
        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/plans/build-plan/process") && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "plan_id": "build-plan",
                                      "title": "Build Plan",
                                      "project": {
                                        "project_id": "pm-mcp-clients"
                                      },
                                      "current_phase": "execute",
                                      "phase_state": "running",
                                      "duration_seconds": 90,
                                      "nodes": [
                                        {
                                          "node_id": "phase-build",
                                          "title": "Build Phase",
                                          "kind": "phase",
                                          "state": "running",
                                          "duration_seconds": 90,
                                          "sub_nodes": [],
                                          "leaf_tasks": [],
                                          "leaf_job": {
                                            "kind": "compile",
                                            "status": "running",
                                            "outcome": "success",
                                            "duration_seconds": 45,
                                            "waits_slot": true
                                          }
                                        }
                                      ]
                                    }
                                    """);
            } else if ("/api/v1/plans/non-existent/process".equals(request.target())) {
                response.send(404, null, "{\"error\": \"not_found\"}");
            } else if ("/api/v1/plans".equals(request.target())) {
                response.json(200, """
                                    [
                                      {
                                        "id": "build-plan",
                                        "phase": "execute",
                                        "status": "running"
                                      }
                                    ]
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code1 = execute("plan", "build-plan", "--depth", "1");
        var output1 = out.toString();
        assertEquals(0, code1);
        assertTrue(output1.contains("Build Phase"));

        out.getBuffer().setLength(0);
        int code2 = execute("plan", "build-plan", "--all");
        var output2 = out.toString();
        assertEquals(0, code2);
        assertTrue(output2.contains("job (compile)"));
        assertTrue(output2.contains("waits slot"));

        out.getBuffer().setLength(0);
        int code3 = execute("plan", "non-existent");
        var output3 = out.toString();
        assertEquals(1, code3);
        assertTrue(output3.contains("Plan non-existent not found."));

        out.getBuffer().setLength(0);
        int code4 = execute("plan");
        var output4 = out.toString();
        assertEquals(0, code4);
        assertTrue(output4.contains("Running Plans:"));
        assertTrue(output4.contains("build-plan"));
    }

    @Test
    @DisplayName("renders rich task activity entries including questions, files changed, and links")
    void shouldRenderRichTaskActivity() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/tasks/TASK-RICH/activity".equals(request.target())) {
                response.json(200, """
                                    {
                                      "task_id": "TASK-RICH",
                                      "role": "audit",
                                      "harness": "opencode",
                                      "model": "deepseek",
                                      "attempt": 2,
                                      "duration_seconds": 120,
                                      "tokens_so_far": 8500,
                                      "entries": [
                                        {
                                          "timestamp": "12:00:01",
                                          "kind": "tool",
                                          "tool_name": "pytest",
                                          "tool_target": "tests/",
                                          "tool_failed": true,
                                          "size": "12 KiB"
                                        },
                                        {
                                          "timestamp": "12:00:05",
                                          "kind": "file_changed",
                                          "files": ["pom.xml", "README.md"]
                                        },
                                        {
                                          "timestamp": "12:00:10",
                                          "kind": "questions_asked",
                                          "content": "Should we rebase?"
                                        },
                                        {
                                          "timestamp": "12:00:15",
                                          "kind": "link",
                                          "tool_target": "https://example.com/log"
                                        }
                                      ]
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "fix-relay", "--task", "TASK-RICH");
        var output = out.toString();

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("TASK-RICH")),
                            () -> assertTrue(output.contains("pytest tests/")),
                            () -> assertTrue(output.contains("failed · 12 KiB")),
                            () -> assertTrue(output.contains("changed    pom.xml, README.md")),
                            () -> assertTrue(output.contains("asked      Should we rebase?")),
                            () -> assertTrue(output.contains("link       https://example.com/log"))
        );
    }
}
