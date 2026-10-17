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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("StartCommand")
@EnabledOnOs({OS.MAC, OS.LINUX})
class StartCommandTest {

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private int run(String... args) {
        var context = new OperatorContext(fixture.environment(), fixture.base().getParent());
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private FakeRuntimeServer runtime(FakeRuntimeServer.Handler handler) throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        return fixture.start(handler);
    }

    private static String startedResult(String id, String kind, String title) {
        return "{\"id\":\"" + id + "\",\"kind\":\"" + kind + "\",\"title\":\"" + title
                            + "\",\"state\":\"started\",\"follow_href\":\"/api/v1/plans/" + id + "\"}";
    }

    @Test
    @DisplayName("start without subcommands prints usage")
    void startUsage() {
        assertEquals(0, run("start"));
        assertTrue(out.toString().contains("Usage: pm-operator start"));
    }

    @Test
    @DisplayName("start plan posts the request and reports the created plan")
    void startPlan() throws Exception {
        var server = runtime((request, response) -> {
            if ("/api/v1/projects/my-proj/plans/start".equals(request.target())
                                && "POST".equals(request.method())) {
                response.send(201, "application/json",
                                    startedResult("codeberg-provider-issues", "plan", "Codeberg provider for issues"));
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, run("start", "plan", "my-proj", "--prompt", "Add a Codeberg provider",
                            "--doc", "doc/concepts/forgejo.adoc", "--issue", "4711", "--title",
                            "Codeberg provider for issues"));
        assertTrue(out.toString().contains("✓ codeberg-provider-issues has started"), out.toString());

        var recorded = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(recorded.bodyText().contains("\"project_id\":\"my-proj\""));
        assertTrue(recorded.bodyText().contains("\"objective\":\"Add a Codeberg provider\""));
        assertTrue(recorded.bodyText().contains("\"documents\":[\"doc/concepts/forgejo.adoc\"]"));
        assertTrue(recorded.bodyText().contains("\"issue\":\"4711\""));
    }

    @Test
    @DisplayName("start plan defaults the project to default when omitted")
    void startPlanDefaultProject() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/projects/default/plans/start".equals(request.target())) {
                response.send(201, "application/json", startedResult("new-work", "plan", "New work"));
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, run("start", "plan", "--prompt", "something"));
        assertTrue(out.toString().contains("✓ new-work has started"));
    }

    @Test
    @DisplayName("start plan carries the recipe, its parameters, and its id derivation")
    void startPlanWithRecipe() throws Exception {
        var server = runtime((request, response) -> {
            if ("/api/v1/projects/pm-mcp-clients/plans/start".equals(request.target())) {
                response.send(201, "application/json",
                                    startedResult("recipe-audit-2026-10-12-14-20-00", "plan", "audit"));
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, run("start", "plan", "pm-mcp-clients", "--recipe", "audit", "--param",
                            "modules=all 9"));
        assertTrue(out.toString().contains("✓ recipe-audit-2026-10-12-14-20-00 has started"));

        var recorded = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(recorded.bodyText().contains("\"recipe_id\":\"audit\""));
        assertTrue(recorded.bodyText().contains("\"parameters\":{\"modules\":\"all 9\"}"));
    }

    @Test
    @DisplayName("start epic posts the request and reports the created epic")
    void startEpic() throws Exception {
        var server = runtime((request, response) -> {
            if ("/api/v1/projects/pm-mcp-clients/epics/start".equals(request.target())) {
                response.send(201, "application/json",
                                    startedResult("relay-hardening", "epic", "Relay hardening"));
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, run("start", "epic", "pm-mcp-clients", "--prompt", "Relay hardening",
                            "--no-auto"));
        assertTrue(out.toString().contains("✓ relay-hardening has started"));

        var recorded = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(recorded.bodyText().contains("\"auto_start\":false"));
    }

    @Test
    @DisplayName("start recipe requires the recipe key")
    void startRecipeRequiresKey() {
        assertEquals(1, run("start", "recipe"));
        assertTrue(out.toString().contains("Recipe key required."));
    }

    @Test
    @DisplayName("start recipe posts the recipe plan and reports its id")
    void startRecipe() throws Exception {
        var server = runtime((request, response) -> {
            if ("/api/v1/projects/pm-mcp-clients/plans/start".equals(request.target())) {
                response.send(201, "application/json",
                                    startedResult("recipe-coverage-2026-10-12-14-05-00", "plan", "Coverage"));
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, run("start", "recipe", "coverage", "--project", "pm-mcp-clients", "--param",
                            "modules=all 9"));
        assertTrue(out.toString().contains("✓ recipe-coverage-2026-10-12-14-05-00 has started"));

        var recorded = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(recorded.bodyText().contains("\"recipe_id\":\"coverage\""));
    }

    @Test
    @DisplayName("start plan reports a refusal on failure")
    void startPlanRefused() throws Exception {
        runtime((request, response) -> response.json(409,
                            "{\"error\":\"start_refused\",\"reason\":\"project_not_ready\"}"));

        assertEquals(PmOperator.EXIT_FAILURE, run("start", "plan", "plan-marshall", "--prompt", "x"));
        assertTrue(err.toString().contains("start_refused"), err.toString());
    }
}
