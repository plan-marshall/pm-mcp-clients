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

@DisplayName("ProjectCommand and ProjectsCommand")
class ProjectCommandTest {

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

    private int executeWithInput(String input, String... args) throws IOException {
        var inStream = new java.io.ByteArrayInputStream(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var outStream = new java.io.ByteArrayOutputStream();
        var terminal = new org.jline.terminal.impl.DumbTerminal(inStream, outStream);
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()), () -> terminal, null);
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
        out.write(outStream.toString(java.nio.charset.StandardCharsets.UTF_8));
        return code;
    }

    @Test
    @DisplayName("project setup runs the state dialogue on the project's onboarding resource")
    void projectSetupRunsTheDialogue() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/projects/cui-parent/setup".equals(request.target())) {
                if (request.bodyText().contains("\"link\"")) {
                    response.json(200, """
                                        {"project_id":"cui-parent","state":"project.completed","step":10,
                                         "total_steps":10,"summary":"cui-parent is ready","links":{}}
                                        """);
                } else {
                    response.json(200, """
                                        {"project_id":"cui-parent","state":"project.identity","step":2,
                                         "total_steps":10,"id":"cui-parent","name":"cui-parent","pilot":false,
                                         "links":{"set":{"href":"/api/v1/projects/cui-parent/setup"}}}
                                        """);
                }
            } else {
                response.send(404, null, "");
            }
        });

        var code = executeWithInput("\n", "project", "setup", "cui-parent");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, output),
                            () -> assertTrue(output.contains("Id"), output),
                            () -> assertTrue(output.contains("cui-parent"), output),
                            () -> assertTrue(output.contains("The project is ready"), output));
    }

    @Test
    @DisplayName("projects lists enrolled projects with favourite indicators")
    void projectsList() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target())) {
                response.json(200, """
                                    [
                                      {"id":"pm-mcp-clients","state":"ready","favourite":true,"plans_count":3},
                                      {"id":"cui-http","state":"ready","favourite":false,"plans_count":0}
                                    ]
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("projects");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Projects")),
                            () -> assertTrue(output.contains("★ pm-mcp-clients")),
                            () -> assertTrue(output.contains("3 plans")),
                            () -> assertTrue(output.contains("cui-http"))
        );
    }

    @Test
    @DisplayName("projects renders full 4-part explorer view")
    void projectsFullExplorer() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target())) {
                response.json(200, """
                                    {
                                      "being_added": [
                                        {"checkout_path":"~/git/cui-parent","current_step":"Identity","step_number":2,"total_steps":10}
                                      ],
                                      "favourites": [
                                        {"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients","readiness":"ready","needs_operator_count":2,"active_plans_count":3,"active_epics_count":1,"last_active":"2026-10-17T10:04:00Z","is_favourite":true,"is_released":false}
                                      ],
                                      "recent": [
                                        {"project_id":"plan-marshall","display_name":"plan-marshall","readiness":"setup_needs","setup_needs_summary":"credential sonar","needs_operator_count":1,"active_plans_count":0,"active_epics_count":0,"last_active":"2026-10-16T10:00:00Z","is_favourite":false,"is_released":false}
                                      ],
                                      "all_projects": [
                                        {"project_id":"API-Sheriff","display_name":"API-Sheriff","readiness":"ready","needs_operator_count":0,"active_plans_count":0,"active_epics_count":0,"last_active":"2026-09-12T10:00:00Z","is_favourite":false,"is_released":false},
                                        {"project_id":"legacy-portal","display_name":"legacy-portal","readiness":"released","needs_operator_count":0,"active_plans_count":0,"active_epics_count":0,"last_active":"2026-08-17T10:00:00Z","is_favourite":false,"is_released":true}
                                      ]
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("projects");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Being added")),
                            () -> assertTrue(output.contains("~/git/cui-parent")),
                            () -> assertTrue(output.contains("Identity (2 of 10)")),
                            () -> assertTrue(output.contains("Favourites")),
                            () -> assertTrue(output.contains("★ pm-mcp-clients")),
                            () -> assertTrue(output.contains("needs you: 2")),
                            () -> assertTrue(output.contains("3 plans · 1 epic")),
                            () -> assertTrue(output.contains("Recent")),
                            () -> assertTrue(output.contains("plan-marshall")),
                            () -> assertTrue(output.contains("setup needs: credential sonar")),
                            () -> assertTrue(output.contains("All projects (by name)")),
                            () -> assertTrue(output.contains("API-Sheriff")),
                            () -> assertTrue(output.contains("legacy-portal")),
                            () -> assertTrue(output.contains("released"))
        );
    }

    @Test
    @DisplayName("projects filter narrows the list")
    void projectsFilter() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target())) {
                response.json(200, """
                                    {
                                      "favourites": [],
                                      "recent": [],
                                      "all_projects": [
                                        {"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients","readiness":"ready","needs_operator_count":0,"active_plans_count":0,"active_epics_count":0,"is_favourite":false,"is_released":false},
                                        {"project_id":"cui-http","display_name":"cui-http","readiness":"ready","needs_operator_count":0,"active_plans_count":0,"active_epics_count":0,"is_favourite":false,"is_released":false}
                                      ],
                                      "being_added": []
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("projects", "--filter", "http");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Matches for 'http'")),
                            () -> assertTrue(output.contains("cui-http")),
                            () -> assertFalse(output.contains("pm-mcp-clients"))
        );
    }

    @Test
    @DisplayName("project inspects single project")
    void projectInspect() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/pm-mcp-clients".equals(request.target())) {
                response.json(200, """
                                    {
                                      "project_id":"pm-mcp-clients",
                                      "display_name":"plan-marshall/pm-mcp-clients",
                                      "remote_url":"https://github.com/plan-marshall/pm-mcp-clients",
                                      "readiness":"ready",
                                      "needs_operator_count":2,
                                      "active_plans_count":3,
                                      "active_epics_count":1,
                                      "last_active":"2026-10-17T10:04:00Z",
                                      "is_favourite":true,
                                      "is_released":false
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "pm-mcp-clients");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Project:   plan-marshall/pm-mcp-clients (pm-mcp-clients)")),
                            () -> assertTrue(output.contains("Remote:    https://github.com/plan-marshall/pm-mcp-clients")),
                            () -> assertTrue(output.contains("Readiness: ready")),
                            () -> assertTrue(output.contains("Operator:  needs you: 2")),
                            () -> assertTrue(output.contains("Work:      3 plans · 1 epics")),
                            () -> assertTrue(output.contains("Favourite: yes (★)")),
                            () -> assertTrue(output.contains("Released:  no"))
        );
    }

    @Test
    @DisplayName("project inspect not found returns 1")
    void projectInspectNotFound() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("project", "nonexistent");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("Project nonexistent not found."))
        );
    }

    @Test
    @DisplayName("project add starts onboarding for a path")
    void projectAdd() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "/git/new-app");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Project onboarding started for /git/new-app"))
        );
    }

    @Test
    @DisplayName("project favourite toggles favourite status")
    void projectFavourite() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/my-app/favourite".equals(request.target())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "favourite", "my-app");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Project my-app favourite: on (★)"))
        );

        out.getBuffer().setLength(0);
        int code2 = execute("project", "favourite", "my-app", "--off");
        assertAll(
                            () -> assertEquals(0, code2),
                            () -> assertTrue(out.toString().contains("✓ Project my-app favourite: off"))
        );
    }

    @Test
    @DisplayName("project release marks project released")
    void projectRelease() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/old-app/release".equals(request.target())) {
                response.json(200, "{\"project_id\":\"old-app\",\"plans_ended_count\":1,\"epics_ended_count\":0,\"entries_deleted_count\":0}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "release", "old-app", "--reason", "archived", "--yes");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ old-app released"))
        );
    }

    @Test
    @DisplayName("project release requires reason when no-input")
    void projectReleaseRequiresReasonNoInput() throws Exception {
        fixture.start((request, response) -> response.send(200, null, "{}"));

        int code = execute("project", "release", "old-app", "--no-input");
        assertAll(
                            () -> assertEquals(3, code),
                            () -> assertTrue(err.toString().contains("Release reason required (--reason)"))
        );
    }

    @Test
    @DisplayName("project release handles landing pending refusal")
    void projectReleaseRefusal() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/old-app/release".equals(request.target())) {
                response.json(409, "{\"error\":\"release_landing_pending\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "release", "old-app", "--reason", "test", "--yes");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains("✗ old-app was not released: release landing pending"))
        );
    }

    @Test
    @DisplayName("project unenrol un-enrols project")
    void projectUnenrol() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/old-app/unenrol".equals(request.target())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "unenrol", "old-app", "--yes");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ old-app unenrolled"))
        );
    }

    @Test
    @DisplayName("project unenrol refused when work runs")
    void projectUnenrolRefusedWorkActive() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/busy-app/unenrol".equals(request.target())) {
                response.json(409, "{\"error\":\"scope_active\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "unenrol", "busy-app", "--yes");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains("✗ busy-app still runs work; unenrolling is refused while it does."))
        );
    }

    @Test
    @DisplayName("output safety strips control sequences")
    void outputSafetySanitizesUntrustedInput() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "\u001B[31m/malicious/path\u001B[0m");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Project onboarding started for /malicious/path")),
                            () -> assertFalse(out.toString().contains("\u001B[31m"))
        );
    }
}
