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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

@DisplayName("OverviewCommandTest (pm-operator status and plans)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class OverviewCommandTest {

    private static final String STATUS_JSON = """
                        {
                          "status": "ready",
                          "version": "0.9.1",
                          "listener": "unix",
                          "running_since": "08:14",
                          "workers_count": 2,
                          "build_slots_used": 1,
                          "build_slots_total": 2
                        }
                        """;

    private static final String PROJECTS_JSON = """
                        {
                          "projects": [
                            {
                              "project_id": "pm-mcp-clients",
                              "display_name": "pm-mcp-clients",
                              "active_plans_count": 3,
                              "active_epics_count": 1,
                              "needs_operator_count": 2,
                              "needs_operator_summary": "decision",
                              "setup_needs_operator": false
                            },
                            {
                              "project_id": "pm-mcp-core",
                              "display_name": "pm-mcp-core",
                              "active_plans_count": 2,
                              "active_epics_count": 0,
                              "needs_operator_count": 0,
                              "needs_operator_summary": null,
                              "setup_needs_operator": false
                            },
                            {
                              "project_id": "plan-marshall",
                              "display_name": "plan-marshall",
                              "active_plans_count": 0,
                              "active_epics_count": 1,
                              "needs_operator_count": 1,
                              "needs_operator_summary": "setup",
                              "setup_needs_operator": true
                            }
                          ]
                        }
                        """;

    private static final String DETAIL_CLIENTS_JSON = """
                        {
                          "project_id": "pm-mcp-clients",
                          "display_name": "pm-mcp-clients",
                          "plans": [
                            {
                              "plan_id": "pln-console-history",
                              "title": "Console history of answers",
                              "current_phase": "refine",
                              "phase_state": "waiting",
                              "needs_operator_kind": "decision",
                              "needs_operator_count": 1,
                              "phase_entered_at": "2026-10-16T15:26:00Z",
                              "tasks_done": 0,
                              "tasks_total": 0
                            },
                            {
                              "plan_id": "pln-overview-resource",
                              "title": "Overview resource of the runtime",
                              "current_phase": "execute",
                              "phase_state": "running",
                              "tasks_done": 7,
                              "tasks_total": 12,
                              "epic_id": "epc-client-work",
                              "epic_title": "client work",
                              "pull_request_number": 214,
                              "pull_request_url": "https://github.com/plan-marshall/pm-mcp-clients/pull/214"
                            },
                            {
                              "plan_id": "pln-message-catalogue",
                              "title": "Message catalogue",
                              "current_phase": "outline",
                              "phase_state": "pausing",
                              "epic_id": "epc-client-work",
                              "epic_title": "client work",
                              "off_track_signal": "repeating"
                            }
                          ],
                          "epics": [
                            {
                              "epic_id": "epc-client-work",
                              "title": "client work",
                              "running_plans_count": 2,
                              "staged_plans_count": 3,
                              "plans": [
                                {
                                  "plan_id": "pln-overview-resource",
                                  "title": "Overview resource of the runtime",
                                  "current_phase": "execute",
                                  "phase_state": "running",
                                  "tasks_done": 7,
                                  "tasks_total": 12
                                },
                                {
                                  "plan_id": "pln-message-catalogue",
                                  "title": "Message catalogue",
                                  "current_phase": "outline",
                                  "phase_state": "pausing",
                                  "off_track_signal": "repeating"
                                }
                              ]
                            }
                          ],
                          "alongside_landings": [
                            {
                              "unit": "remediation.ci-failure",
                              "state": "failed",
                              "kind": "unit",
                              "hold_kind": "landing_failed"
                            },
                            {
                              "state": "running",
                              "count": 2,
                              "kind": "removal"
                            }
                          ]
                        }
                        """;

    private static final String DETAIL_CORE_JSON = """
                        {
                          "project_id": "pm-mcp-core",
                          "display_name": "pm-mcp-core",
                          "plans": [
                            {
                              "plan_id": "pln-session-log",
                              "title": "Session log and generations",
                              "current_phase": "execute",
                              "phase_state": "running",
                              "pull_request_number": 198,
                              "pull_request_url": "https://github.com/plan-marshall/pm-mcp-core/pull/198"
                            },
                            {
                              "plan_id": "pln-profile-loader",
                              "title": "Profile loader",
                              "current_phase": "finalize",
                              "phase_state": "paused"
                            }
                          ],
                          "epics": [],
                          "alongside_landings": []
                        }
                        """;

    private static final String DETAIL_PLAN_MARSHALL_JSON = """
                        {
                          "project_id": "plan-marshall",
                          "display_name": "plan-marshall",
                          "plans": [],
                          "epics": [],
                          "alongside_landings": []
                        }
                        """;

    private static final String ENDED_CLIENTS_JSON = """
                        {
                          "project_id": "pm-mcp-clients",
                          "page": 1,
                          "total_pages": 2,
                          "has_newer": false,
                          "has_older": true,
                          "items": [
                            {
                              "item_id": "pln-relay-drops",
                              "kind": "plan",
                              "title": "Relay drops events on reconnect",
                              "outcome": "merged",
                              "ended_at": "2026-10-14T10:00:00Z",
                              "pull_request_number": 211,
                              "pull_request_url": "https://github.com/plan-marshall/pm-mcp-clients/pull/211"
                            },
                            {
                              "item_id": "pln-fix-loss",
                              "kind": "plan",
                              "title": "Fix the relay's event loss",
                              "outcome": "cancelled",
                              "reason": "the approach is replaced by plan relay-v2",
                              "ended_at": "2026-10-14T09:00:00Z"
                            },
                            {
                              "item_id": "epc-bootstrap",
                              "kind": "epic",
                              "title": "client bootstrap",
                              "terminal_reason": "completed",
                              "ended_at": "2026-10-02T10:00:00Z"
                            }
                          ]
                        }
                        """;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private String activeLocale = "en";

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
        activeLocale = "en";
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private int run(String... args) {
        var env = new HashMap<>(fixture.environment());
        if ("de".equals(activeLocale)) {
            env.put("LANG", "de_DE.UTF-8");
        } else {
            env.put("LANG", "en_US.UTF-8");
        }
        var context = new OperatorContext(
                            env,
                            fixture.base().getParent(),
                            true,
                            ClientEnvironment.capture(env),
                            null,
                            _ -> {
                            }
        );
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private void writeLiveRecord() throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
    }

    private FakeRuntimeServer setupRunningServer() throws IOException {
        writeLiveRecord();
        fixture.writeToken("test-token");
        return fixture.start((request, response) -> {
            var path = request.target();
            if ("/api/v1/status".equals(path) || path.startsWith("/api/v1/status?")) {
                response.json(200, STATUS_JSON);
            } else if ("/api/v1/projects".equals(path)) {
                response.json(200, PROJECTS_JSON);
            } else if ("/api/v1/projects/pm-mcp-clients".equals(path)) {
                response.json(200, DETAIL_CLIENTS_JSON);
            } else if ("/api/v1/projects/pm-mcp-core".equals(path)) {
                response.json(200, DETAIL_CORE_JSON);
            } else if ("/api/v1/projects/plan-marshall".equals(path)) {
                response.json(200, DETAIL_PLAN_MARSHALL_JSON);
            } else if (path.startsWith("/api/v1/projects/pm-mcp-clients/ended")) {
                if (path.contains("page=2")) {
                    response.json(200, "{\"project_id\":\"pm-mcp-clients\",\"page\":2,\"total_pages\":2,\"has_newer\":true,\"has_older\":false,\"items\":[{\"id\":\"pln-page-2\",\"kind\":\"plan\",\"title\":\"Page 2 plan\",\"outcome\":\"merged\",\"ended_at\":\"2026-10-01T10:00:00Z\"}]}");
                } else {
                    response.json(200, ENDED_CLIENTS_JSON);
                }
            } else if (path.startsWith("/api/v1/projects/pm-mcp-core/ended") || path.startsWith("/api/v1/projects/plan-marshall/ended")) {
                response.json(200, "{\"project_id\":\"other\",\"page\":1,\"total_pages\":1,\"has_newer\":false,\"has_older\":false,\"items\":[]}");
            } else {
                response.send(404, null, "{\"error\":\"not_found\"}");
            }
        });
    }

    @Test
    @DisplayName("status renders complete hierarchical tree across projects and plans")
    void shouldRenderOverviewTree() throws Exception {
        setupRunningServer();

        int code = run("status");
        assertEquals(0, code);

        var output = out.toString();
        // Runtime header
        assertTrue(output.contains("Runtime pm-mcpd 0.9.1, running since 08:14 · 2 workers · 1 of 2 build slots in use"));

        // Attention markers
        assertTrue(output.contains("! 2  pm-mcp-clients · 3 plans · 1 epic"));
        assertTrue(output.contains("pm-mcp-core · 2 plans"));
        assertTrue(output.contains("! 1  plan-marshall · setup needs you"));

        // Plans in pm-mcp-clients
        assertTrue(output.contains("! decision plan  Console history of answers"));
        assertTrue(output.contains("refine · waiting for you"));
        assertTrue(output.contains("plan  Overview resource of the runtime"));
        assertTrue(output.contains("in epic client work · 7 of 12 tasks"));
        assertTrue(output.contains("pull request #214 https://github.com/plan-marshall/pm-mcp-clients/pull/214"));

        // Off-track signal on Message catalogue
        assertTrue(output.contains("plan  Message catalogue"));
        assertTrue(output.contains("off track: repeating"));

        // Alongside landings
        assertTrue(output.contains("alongside"));
        assertTrue(output.contains("! landing failed   landing remediation.ci-failure"));
        assertTrue(output.contains("removing 2 moved documents from git"));
    }

    @Test
    @DisplayName("status --project filters output to single project")
    void shouldFilterByProject() throws Exception {
        setupRunningServer();

        int code = run("status", "--project", "pm-mcp-core");
        assertEquals(0, code);

        var output = out.toString();
        assertTrue(output.contains("pm-mcp-core · 2 plans"));
        assertTrue(output.contains("Session log and generations"));
        assertTrue(output.contains("pull request #198 https://github.com/plan-marshall/pm-mcp-core/pull/198"));

        assertFalse(output.contains("pm-mcp-clients"));
        assertFalse(output.contains("plan-marshall · setup needs you"));
    }

    @Test
    @DisplayName("status --by epic groups plans under parent epics")
    void shouldGroupByEpic() throws Exception {
        setupRunningServer();

        int code = run("status", "--project", "pm-mcp-clients", "--by", "epic");
        assertEquals(0, code);

        var output = out.toString();
        assertTrue(output.contains("epic  client work"));
        assertTrue(output.contains("2 running · 3 staged"));
        assertTrue(output.contains("├─"));
        assertTrue(output.contains("Overview resource of the runtime"));
        assertTrue(output.contains("└─"));
        assertTrue(output.contains("Message catalogue"));
        assertTrue(output.contains("without epic"));
        assertTrue(output.contains("Console history of answers"));
    }

    @Test
    @DisplayName("status --ended renders ended work tree with outcomes, PRs, and reasons")
    void shouldRenderEndedWorkTree() throws Exception {
        setupRunningServer();

        int code = run("status", "--ended", "--project", "pm-mcp-clients");
        assertEquals(0, code);

        var output = out.toString();
        assertTrue(output.contains("pm-mcp-clients · ended work"));
        assertTrue(output.contains("plan  Relay drops events on reconnect"));
        assertTrue(output.contains("merged"));
        assertTrue(output.contains("pull request #211 https://github.com/plan-marshall/pm-mcp-clients/pull/211"));
        assertTrue(output.contains("plan  Fix the relay's event loss"));
        assertTrue(output.contains("cancelled"));
        assertTrue(output.contains("reason: the approach is replaced by plan relay-v2"));
        assertTrue(output.contains("epic  client bootstrap"));
        assertTrue(output.contains("completed"));
        assertTrue(output.contains("Page 2 plan"));
    }

    @Test
    @DisplayName("status --ended without project iterates all projects")
    void shouldRenderAllProjectsEndedWork() throws Exception {
        setupRunningServer();

        int code = run("status", "--ended");
        assertEquals(0, code);

        var output = out.toString();
        assertTrue(output.contains("pm-mcp-clients · ended work"));
        assertTrue(output.contains("Page 2 plan"));
        assertFalse(output.contains("pm-mcp-core · ended work"));
    }

    @Test
    @DisplayName("status renders '-' placeholder when running_since is missing")
    void shouldRenderPlaceholderWhenRunningSinceMissing() throws Exception {
        writeLiveRecord();
        fixture.writeToken("test-token");
        fixture.start((request, response) -> {
            var path = request.target();
            if ("/api/v1/status".equals(path)) {
                response.json(200, "{\"status\":\"ready\",\"version\":\"0.9.1\",\"listener\":\"unix\",\"workers_count\":2}");
            } else if ("/api/v1/projects".equals(path)) {
                response.json(200, "{\"projects\":[]}");
            } else {
                response.send(404, null, "{\"error\":\"not_found\"}");
            }
        });

        int code = run("status");
        assertEquals(0, code);

        var output = out.toString();
        assertTrue(output.contains("running since -"));
    }

    @Test
    @DisplayName("plans command delegates to overview tree")
    void shouldExecutePlansCommand() throws Exception {
        setupRunningServer();

        int code = run("plans");
        assertEquals(0, code);

        var output = out.toString();
        assertTrue(output.contains("Runtime pm-mcpd 0.9.1"));
        assertTrue(output.contains("pm-mcp-clients"));
    }

    @Test
    @DisplayName("status renders German bundle texts when LANG=de")
    void shouldRenderGermanMessages() throws Exception {
        activeLocale = "de";
        setupRunningServer();

        int code = run("status");
        assertEquals(0, code);

        var output = out.toString();
        assertTrue(output.contains("3 Pläne · 1 Epic"));
        assertTrue(output.contains("belegt"));
    }

    @Test
    @DisplayName("status --json outputs JSON payload for status and project detail")
    void shouldRenderJsonStatusAndDetail() throws Exception {
        setupRunningServer();

        // 1. status --json
        int codeStatus = run("status", "--json");
        assertEquals(0, codeStatus);
        var jsonStatus = out.toString();
        assertTrue(jsonStatus.contains("\"status\""));
        assertTrue(jsonStatus.contains("\"ready\""));

        // 2. status --json --project pm-mcp-clients
        out.getBuffer().setLength(0);
        int codeDetail = run("status", "--json", "--project", "pm-mcp-clients");
        assertEquals(0, codeDetail);
        var jsonDetail = out.toString();
        assertTrue(jsonDetail.contains("\"project_id\""));
        assertTrue(jsonDetail.contains("\"pm-mcp-clients\""));
    }

    @Test
    @DisplayName("OutputSafety strips ANSI CSI, OSC sequences and control characters")
    void outputSafety() {
        assertEquals("", OutputSafety.stripControlSequences(null));
        assertEquals("", OutputSafety.stripControlSequences(""));
        assertEquals("Clean Text", OutputSafety.stripControlSequences("Clean Text"));
        assertEquals("Red Alert", OutputSafety.stripControlSequences("\u001B[31mRed Alert\u001B[0m"));
        assertEquals("Title Content", OutputSafety.stripControlSequences("\u001B]0;Window Title\u0007Title Content"));
        assertEquals("No\nLine\rBreaks", OutputSafety.stripControlSequences("No\nLine\rBreaks"));
    }
}
