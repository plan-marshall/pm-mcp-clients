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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@EnableGeneratorController
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

    private int executeGerman(String... args) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de_DE.UTF-8");
        environment.remove("LC_ALL");
        environment.remove("LC_MESSAGES");
        var context = new OperatorContext(environment, tempDir, true, ClientEnvironment.capture(environment));
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private int executeWithInput(String input, String... args) throws IOException {
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();
        var terminal = new DumbTerminal(inStream, outStream);
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()), () -> terminal, null);
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
        out.write(outStream.toString(StandardCharsets.UTF_8));
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
                            () -> assertTrue(output.contains("cui-parent is ready"), output));
    }

    @Test
    @DisplayName("a project setup that ends without its final state leaves with 3, not 0")
    void projectSetupWithoutItsFinalStateNeedsInput() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/cui-parent/setup".equals(request.target())) {
                // A state with no link and no final state: the run ends where it stood.
                response.json(200, """
                                    {"project_id":"cui-parent","state":"project.identity","step":2,
                                     "total_steps":10,"id":"cui-parent","name":"cui-parent","pilot":false,
                                     "links":{}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        var code = executeWithInput("", "project", "setup", "cui-parent");

        assertEquals(ProjectCommand.EXIT_NEEDS_INPUT, code, out.toString());
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
    @DisplayName("project <id> renders its texts in a German client locale")
    void projectInspectGerman() throws Exception {
        var projectId = Generators.letterStrings(4, 12).next();
        fixture.start((request, response) -> {
            if (("/api/v1/projects/" + projectId).equals(request.target())) {
                response.json(200, "{\"project_id\":\"" + projectId + "\",\"display_name\":\"" + projectId
                                    + "\",\"readiness\":\"ready\",\"needs_operator_count\":0,"
                                    + "\"active_plans_count\":0,\"active_epics_count\":0,"
                                    + "\"is_favourite\":false,\"is_released\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeGerman("project", projectId);
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(output.contains("Projekt:      " + projectId + " (" + projectId + ")"),
                                                output),
                            () -> assertTrue(output.contains("Bereitschaft: bereit"), output),
                            () -> assertTrue(output.contains("Favorit:      nein"), output),
                            () -> assertTrue(output.contains("Abgegeben:    ja"), output)
        );
    }

    @Test
    @DisplayName("project <id> not found is reported in a German client locale")
    void projectInspectNotFoundGerman() throws Exception {
        var projectId = Generators.letterStrings(4, 12).next();
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = executeGerman("project", projectId);
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("Projekt " + projectId + " nicht gefunden."),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("project favourite confirms in a German client locale")
    void projectFavouriteGerman() throws Exception {
        var projectId = Generators.letterStrings(4, 12).next();
        fixture.start((request, response) -> {
            if (("/api/v1/projects/" + projectId + "/favourite").equals(request.target())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeGerman("project", "favourite", projectId);
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(out.toString().contains("✓ Projekt " + projectId + " Favorit: an (★)"),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("interactive project add reaches identity state and asks")
    void projectAddInteractiveReachesIdentity() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var server = fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, """
                                    {"status":"started","onboarding_id":"onb-cui-parent","checkout_path":"/git/cui-parent",
                                     "continue_link":"/api/v1/entries/ent-cui-parent/continue",
                                     "current_step":"Identity","step_number":2,"total_steps":10}
                                    """);
            } else if ("/api/v1/entries/ent-cui-parent".equals(request.target())) {
                response.json(200, """
                                    {"state":"project.identity","step":2,
                                     "total_steps":10,"id":"cui-parent","name":"cui-parent","pilot":false,
                                     "links":{"set":{"href":"/api/v1/entries/ent-cui-parent/links/set","method":"POST"}}}
                                    """);
            } else if ("/api/v1/entries/ent-cui-parent/links/set".equals(request.target())) {
                response.json(200, """
                                    {"state":"project.completed","step":10,
                                     "total_steps":10,"summary":"cui-parent is ready","links":{}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeWithInput("\n", "project", "add", "/git/cui-parent");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(output.contains("Id"), output),
                            () -> assertTrue(output.contains("cui-parent"), output),
                            () -> assertTrue(output.contains("The project is ready"), output),
                            () -> assertTrue(server.requests().stream().noneMatch(r -> "/api/v1/enrol".equals(r.target())),
                                                "no POST /api/v1/enrol is sent outside the onboarding's own enrol step")
        );
    }

    @Test
    @DisplayName("project add --no-input without --pilot exits 3 and names --pilot")
    void projectAddNoInputWithoutPilotExitsNeedsInput() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, """
                                    {"status":"started","onboarding_id":"onb-cui-parent","checkout_path":"/git/cui-parent",
                                     "project_id":"cui-parent","current_step":"Identity","step_number":2,"total_steps":10}
                                    """);
            } else if ("/api/v1/projects/cui-parent/setup".equals(request.target())) {
                response.json(200, """
                                    {"project_id":"cui-parent","state":"project.identity","step":2,
                                     "total_steps":10,"id":"cui-parent","name":"cui-parent","pilot":false,
                                     "links":{"set":{"href":"/api/v1/projects/cui-parent/setup","method":"POST"}}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "/git/cui-parent", "--no-input");
        assertAll(
                            () -> assertEquals(ProjectCommand.EXIT_NEEDS_INPUT, code, out.toString()),
                            () -> assertTrue(err.toString().contains("--pilot"), err.toString())
        );
    }

    @Test
    @DisplayName("project add with every answer given runs through to enrolment without a prompt")
    void projectAddWithAllAnswersRunsWithoutPrompt() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var server = fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, """
                                    {"status":"started","onboarding_id":"onb-cui-parent","checkout_path":"/git/cui-parent",
                                     "continue_link":"/api/v1/entries/ent-cui-parent/continue",
                                     "current_step":"Identity","step_number":2,"total_steps":10}
                                    """);
            } else if ("/api/v1/entries/ent-cui-parent".equals(request.target())) {
                response.json(200, """
                                    {"state":"project.identity","step":2,
                                     "total_steps":10,"id":"cui-parent","name":"cui-parent","pilot":true,
                                     "links":{"set":{"href":"/api/v1/entries/ent-cui-parent/links/set","method":"POST"}}}
                                    """);
            } else if ("/api/v1/entries/ent-cui-parent/links/set".equals(request.target())) {
                response.json(200, """
                                    {"state":"project.completed","step":10,
                                     "total_steps":10,"summary":"cui-parent is ready","links":{}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "/git/cui-parent", "--pilot");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(output.contains("The project is ready"), output),
                            () -> assertTrue(server.requests().stream().noneMatch(r -> "/api/v1/enrol".equals(r.target())),
                                                "no POST /api/v1/enrol is sent outside the onboarding's own enrol step")
        );
    }

    @Test
    @DisplayName("no POST /api/v1/enrol is sent outside the onboarding's own enrol step")
    void projectAddNeverSendsDirectEnrol() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var server = fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, """
                                    {"status":"started","onboarding_id":"onb-cui-parent","checkout_path":"/git/cui-parent",
                                     "continue_link":"/api/v1/entries/ent-cui-parent/continue",
                                     "current_step":"Identity","step_number":2,"total_steps":10}
                                    """);
            } else if ("/api/v1/entries/ent-cui-parent".equals(request.target())) {
                response.json(200, """
                                    {"state":"project.completed","step":10,
                                     "total_steps":10,"summary":"cui-parent is ready","links":{}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "/git/cui-parent", "--pilot");
        assertEquals(0, code, err.toString());
        assertTrue(server.requests().stream().noneMatch(r -> "/api/v1/enrol".equals(r.target())),
                            "no POST /api/v1/enrol is sent");
    }

    @Test
    @DisplayName("project add reports the root conflict of another enrolment")
    void projectAddRootConflict() throws Exception {
        var path = "/git/" + Generators.letterStrings(4, 12).next();
        fixture.start((request, response) -> response.json(409,
                            JsonTree.write(Map.of("error", "enrolment_root_conflict"))));

        int code = execute("project", "add", path, "--pilot");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("✗ Another enrolment already names " + path),
                                                out.toString()));
    }

    @Test
    @DisplayName("project add reports a project store that names another checkout root")
    void projectAddStoreRootMismatch() throws Exception {
        fixture.start((request, response) -> response.json(409,
                            JsonTree.write(Map.of("reason", "project_store_root_mismatch"))));

        int code = execute("project", "add", "/git/" + Generators.letterStrings(4, 12).next());

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("names a different checkout root"), out.toString()));
    }

    @Test
    @DisplayName("project add reports any other refusal with its status")
    void projectAddOtherRefusal() throws Exception {
        fixture.start((request, response) -> response.json(400, "{\"error\":\"invalid_arguments\"}"));

        int code = execute("project", "add", "/git/" + Generators.letterStrings(4, 12).next());

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains("HTTP 400"), err.toString()));
    }

    @Test
    @DisplayName("project add continues a run that has only an entry in its continue link")
    void projectAddContinuesTheEntryOfAnUnenrolledRun() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, """
                                    {"status":"started","checkout_path":"/git/cui-parent",
                                     "continue_link":"/api/v1/entries/ent-cui/continue"}
                                    """);
            } else if ("/api/v1/entries/ent-cui".equals(request.target())) {
                response.json(200, """
                                    {"state":"project.completed","step":10,"total_steps":10,
                                     "summary":"cui-parent is ready","links":{}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "/git/cui-parent", "--pilot");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(out.toString().contains("The project is ready"), out.toString()),
                            () -> assertTrue(server.requests().stream()
                                                                    .anyMatch(r -> "/api/v1/entries/ent-cui".equals(r.target())),
                                                "the run is read at its entry"));
    }

    @Test
    @DisplayName("project add reports a start that names no entry as unreadable")
    void projectAddWithoutAnEntryReportsUnreadable() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, "{\"status\":\"started\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "/git/cui-parent", "--pilot");

        assertAll(
                            () -> assertEquals(1, code, out.toString()),
                            () -> assertTrue(err.toString().contains("could not be read"), err.toString())
        );
    }

    @Test
    @DisplayName("project add reports a checkout without a directory name as unreadable")
    void projectAddWithARootPathReportsUnreadable() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, "{\"status\":\"started\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "/", "--pilot");

        assertAll(
                            () -> assertEquals(1, code, out.toString()),
                            () -> assertTrue(err.toString().contains("could not be read"), err.toString()));
    }

    @Test
    @DisplayName("project add reports a run whose setup cannot be read")
    void projectAddReportsAnUnreadableSetup() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, "{\"status\":\"started\",\"project_id\":\"cui-parent\"}");
            } else {
                response.send(500, null, "");
            }
        });

        int code = execute("project", "add", "/git/cui-parent", "--pilot");

        assertAll(
                            () -> assertEquals(1, code, out.toString()),
                            () -> assertTrue(err.toString().contains("cui-parent could not be read"), err.toString()));
    }

    @Test
    @DisplayName("project add does not treat a server error of the start as a refusal of the operator")
    void projectAddServerErrorOnStartFails() throws Exception {
        fixture.start((request, response) -> response.send(503, null, ""));

        int code = execute("project", "add", "/git/cui-parent", "--pilot");

        assertNotEquals(0, code, out.toString());
    }

    @Test
    @DisplayName("project add --no-input at a later choice state exits 3, prints the catalogue and closes the entry")
    void projectAddNoInputAtALaterStateClosesTheEntry() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, """
                                    {"status":"started","checkout_path":"/git/cui-parent",
                                     "continue_link":"/api/v1/entries/ent-cui-parent/continue"}
                                    """);
            } else if ("/api/v1/entries/ent-cui-parent".equals(request.target())) {
                response.json(200, """
                                    {"state":"project.consent","step":1,"total_steps":10,
                                     "links":{"yes":{"href":"/api/v1/entries/ent-cui-parent/links/yes"}}}
                                    """);
            } else if (request.target().endsWith("/abandon")) {
                response.json(200, "{\"status\":\"abandoned\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "/git/cui-parent", "--no-input", "--pilot");

        assertAll(
                            () -> assertEquals(ProjectCommand.EXIT_NEEDS_INPUT, code, out.toString()),
                            () -> assertTrue(out.toString().contains(
                                                "The project setup needs"), out.toString()),
                            () -> assertTrue(out.toString().contains("Look at the checkout"), out.toString()),
                            () -> assertTrue(server.requests().stream()
                                                                    .anyMatch(r -> "DELETE".equals(r.method())
                                                                                        && r.target().endsWith("/abandon")),
                                                () -> "the entry is closed: " + server.requests())
        );
    }

    @Test
    @DisplayName("project add sends the contract's body: the absolute checkout path and the build environment")
    void projectAddSendsTheContractBody() throws Exception {
        var posted = new AtomicReference<String>("");
        fixture.start((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                posted.set(request.bodyText());
                response.json(201, """
                                    {"status":"started","checkout_path":"/git/cui-parent",
                                     "continue_link":"/api/v1/entries/ent-cui-parent/continue"}
                                    """);
            } else if ("/api/v1/entries/ent-cui-parent".equals(request.target())) {
                response.json(200, """
                                    {"state":"project.completed","step":10,"total_steps":10,
                                     "summary":"cui-parent is ready","links":{}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });
        var checkout = Generators.letterStrings(4, 12).next().toLowerCase(Locale.ROOT);

        int code = execute("project", "add", checkout, "--pilot", "--no-input");

        var body = posted.get();
        var parsed = JsonTree.asObject(JsonTree.parse(body));
        var buildEnvironment = parsed != null && parsed.get("build_environment") instanceof Map<?, ?> env
                            ? env : Map.of();
        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertNotNull(parsed, body),
                            () -> assertTrue(buildEnvironment.containsKey("PM_MCP_BASE"), body),
                            () -> assertEquals(Path.of(checkout).toAbsolutePath().toString(),
                                                String.valueOf(parsed.get("path")), body),
                            () -> assertFalse(parsed.containsKey("pilot"), body)
        );
    }

    @Test
    @DisplayName("a release without --yes prints the block and asks again on anything but y or n")
    void releaseWithoutYesPrintsTheBlockAndReAsks() throws Exception {
        var releases = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("/api/v1/projects/cui-parent".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"project_id":"cui-parent","display_name":"cui-parent","readiness":"ready",
                                     "active_plans_count":3,"active_epics_count":1}
                                    """);
            } else if (request.target().endsWith("/release")) {
                releases.incrementAndGet();
                response.json(200, "{\"status\":\"released\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeWithInput("the gates move\nmaybe\ny\n", "project", "release", "cui-parent");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertEquals(1, releases.get(), "the release is sent once"),
                            () -> assertTrue(output.contains("What ends"), output),
                            () -> assertTrue(output.contains("3 plans, each ended as released and archived; 1 epic"), output),
                            () -> assertTrue(output.contains("What stays"), output),
                            () -> assertTrue(output.contains("Afterwards"), output),
                            () -> assertTrue(output.contains("Type y or n."), output),
                            () -> assertTrue(output.contains("Reason"), output)
        );
    }

    @Test
    @DisplayName("a release with nothing running names no What ends, and ? explains")
    void releaseWithNothingRunningExplains() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/cui-parent".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"project_id":"cui-parent","display_name":"cui-parent","readiness":"ready",
                                     "active_plans_count":0,"active_epics_count":0}
                                    """);
            } else if (request.target().endsWith("/release")) {
                response.json(200, "{\"status\":\"released\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeWithInput("why\n?\ny\n", "project", "release", "cui-parent");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertFalse(output.contains("What ends"), output),
                            () -> assertTrue(output.contains("What stays"), output),
                            () -> assertTrue(output.contains("Releasing ends every plan and epic"), output));
    }

    @Test
    @DisplayName("an interactive unenrol prints what ends and what stays before it asks")
    void unenrolPrintsItsBlock() throws Exception {
        var server = fixture.start((request, response) -> {
            if (request.target().endsWith("/unenrol")) {
                response.json(200, "{\"status\":\"unenrolled\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeWithInput("y\n", "project", "unenrol", "legacy-portal");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(output.contains("Unenrol legacy-portal?"), output),
                            () -> assertTrue(output.contains("What ends"), output),
                            () -> assertTrue(output.contains("Afterwards"), output),
                            () -> assertTrue(server.requests().stream().anyMatch(r -> r.target().endsWith("/unenrol")),
                                                server.requests().toString()));
    }

    @Test
    @DisplayName("project unenrol treats a closed input as a declined confirmation and sends nothing")
    void projectUnenrolWithClosedInputSendsNothing() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, "{\"status\":\"unenrolled\"}"));

        int code = executeWithInput("", "project", "unenrol", "my-app");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(server.requests().stream()
                                                                    .noneMatch(r -> r.target().endsWith("/unenrol")),
                                                "nothing is sent"));
    }

    @Test
    @DisplayName("project release treats a closed input as a declined confirmation and sends nothing")
    void projectReleaseWithClosedInputSendsNothing() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, "{\"status\":\"released\"}"));

        int code = executeWithInput("", "project", "release", "my-app");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(server.requests().stream()
                                                                    .noneMatch(r -> r.target().endsWith("/release")),
                                                "nothing is sent"));
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
    @DisplayName("project release without --yes prompts confirmation; declining sends nothing")
    void projectReleaseWithoutYesConfirmationAskedDecliningSendsNothing() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, "{}"));

        int code = executeWithInput("n\n", "project", "release", "old-app", "--reason", "my-reason");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(server.requests().stream()
                                                                    .noneMatch(request -> request.target().endsWith("/release")),
                                                "no release is sent when declined: " + server.requests()),
                            () -> assertTrue(out.toString().contains("Release the project?"), out.toString())
        );
    }

    @Test
    @DisplayName("project release interactive without --reason asks reason and records it")
    void projectReleaseInteractiveWithoutReasonAsksReason() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/projects/old-app/release".equals(request.target())) {
                response.json(200, "{\"project_id\":\"old-app\",\"plans_ended_count\":1,\"epics_ended_count\":0,\"entries_deleted_count\":0}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = executeWithInput("my custom reason\ny\n", "project", "release", "old-app");
        var releases = server.requests().stream().filter(request -> request.target().endsWith("/release")).toList();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertEquals(1, releases.size(), server.requests().toString()),
                            () -> assertTrue(releases.getFirst().bodyText().contains("my custom reason"),
                                                releases.getFirst().bodyText()),
                            () -> assertTrue(out.toString().contains("✓ old-app released"), out.toString())
        );
    }

    @Test
    @DisplayName("project release --no-input without --yes stops with exit 3 and sends nothing")
    void projectReleaseNoInputWithoutYesExitsNeedsInput() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, "{}"));

        int code = execute("project", "release", "old-app", "--reason", "my-reason", "--no-input");
        assertAll(
                            () -> assertEquals(3, code),
                            () -> assertTrue(err.toString().contains("Release confirmation required (--yes)"), err.toString()),
                            () -> assertTrue(server.requests().isEmpty(), "no request may be sent")
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
    @DisplayName("project unenrol without --yes prompts confirmation; declining sends nothing")
    void projectUnenrolWithoutYesConfirmationAskedDecliningSendsNothing() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, "{}"));

        int code = executeWithInput("n\n", "project", "unenrol", "old-app");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(server.requests().isEmpty(), "no request may be sent when declined"),
                            () -> assertTrue(out.toString().contains("Unenrol the project?"), out.toString())
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
    @DisplayName("project unenrol --no-input without --yes stops with exit 3 and sends nothing")
    void projectUnenrolNeedsConfirmation() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, "{}"));

        int code = execute("project", "unenrol", Generators.letterStrings(4, 12).next(), "--no-input");

        assertAll(
                            () -> assertEquals(3, code),
                            () -> assertTrue(err.toString().contains("Unenrol confirmation required (--yes)"),
                                                err.toString()),
                            () -> assertTrue(server.requests().isEmpty(), "no request may be sent"));
    }

    @Test
    @DisplayName("output safety strips control sequences")
    void outputSafetySanitizesUntrustedInput() throws Exception {
        fixture.start((request, response) -> response.json(409,
                            JsonTree.write(Map.of("error", "enrolment_root_conflict"))));

        int code = execute("project", "add", "\u001B[31m/malicious/path\u001B[0m");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("/malicious/path"), out.toString()),
                            () -> assertFalse(out.toString().contains("\u001B[31m"))
        );
    }
}
