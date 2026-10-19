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

import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

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


    @Test
    @DisplayName("recipe without subcommands prints usage")
    void recipeUsage() {
        assertEquals(0, run("recipe"));
        assertTrue(out.toString().contains("Usage: pm-operator recipe"));
    }

    @Test
    @DisplayName("recipe run without a key and --no-input names the recipe state")
    void runRequiresKey() throws Exception {
        var outcome = StartTestSupport.run(StandardScenarios.START_SOURCES, null,
                            "recipe", "run", "--project", "pm-mcp-clients", "--no-input");
        assertAll(
                            () -> assertEquals(StartDialogue.EXIT_MISSING_INPUT, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("start.recipe needs"), outcome.rendered()));
    }

    @Test
    @DisplayName("recipe run resolves its parameters and starts")
    void runSuccess() throws Exception {
        var outcome = StartTestSupport.run(StandardScenarios.START_SOURCES, null,
                            "recipe", "run", "code-review", "--project", "pm-mcp-clients",
                            "--title", "Review Auth", "--param", "depth=deep", "--no-input");
        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("code-review"), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    @Test
    @DisplayName("recipe run with an explicit project starts the recipe")
    void runDefaultProject() throws Exception {
        var outcome = StartTestSupport.run(StandardScenarios.START_SOURCES, null,
                            "recipe", "run", "code-review", "--project", "pm-mcp-clients", "--no-input");
        assertEquals(0, outcome.code(), outcome.rendered());
        assertTrue(outcome.rendered().contains("has started"), outcome.rendered());
    }

    @Test
    @DisplayName("recipe run reports the refusal of an unknown recipe")
    void runRefused() throws Exception {
        var outcome = StartTestSupport.run(StandardScenarios.START_SOURCES, null,
                            "recipe", "run", "missing-recipe", "--project", "pm-mcp-clients", "--no-input");
        assertEquals(PmOperator.EXIT_FAILURE, outcome.code(), outcome.rendered());
        assertTrue(outcome.rendered().contains("No recipe with the key missing-recipe"), outcome.rendered());
    }
}
