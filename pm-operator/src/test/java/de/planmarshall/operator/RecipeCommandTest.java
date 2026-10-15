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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

@DisplayName("RecipeCommand")
@EnabledOnOs({OS.MAC, OS.LINUX})
class RecipeCommandTest {

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

    @Test
    @DisplayName("recipe without subcommands prints usage")
    void recipeUsage() {
        assertEquals(0, run("recipe"));
        assertTrue(out.toString().contains("Usage: pm-operator recipe"));
    }

    @Test
    @DisplayName("recipe run requires recipe key")
    void runRequiresKey() {
        assertEquals(1, run("recipe", "run"));
        assertTrue(out.toString().contains("Recipe key required."));
    }

    @Test
    @DisplayName("recipe run sends post request with options and succeeds")
    void runSuccess() throws Exception {
        var server = runtime((request, response) -> {
            if ("/api/v1/projects/my-proj/recipes/code-review/run".equals(request.target())
                    && "POST".equals(request.method())) {
                response.send(201, null, "{\"plan_id\":\"plan-42\"}");
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, run("recipe", "run", "code-review", "--project", "my-proj",
                "--title", "Review Auth", "--param", "depth=deep", "--source", "src/"));
        assertTrue(out.toString().contains("✓ Recipe code-review started"));

        var recorded = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(recorded.bodyText().contains("\"title\":\"Review Auth\""));
        assertTrue(recorded.bodyText().contains("\"params\":{\"depth\":\"deep\"}"));
        assertTrue(recorded.bodyText().contains("\"sources\":[\"src/\"]"));
    }

    @Test
    @DisplayName("recipe run defaults project to default when omitted")
    void runDefaultProject() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/projects/default/recipes/cleanup/run".equals(request.target())) {
                response.send(200, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, run("recipe", "run", "cleanup"));
        assertTrue(out.toString().contains("✓ Recipe cleanup started"));
    }

    @Test
    @DisplayName("recipe run reports refusal on failure")
    void runRefused() throws Exception {
        runtime((request, response) -> response.json(404, "{\"code\":\"recipe_not_found\"}"));

        assertEquals(PmOperator.EXIT_FAILURE, run("recipe", "run", "missing-recipe"));
        assertTrue(err.toString().contains("recipe_not_found"));
    }
}
