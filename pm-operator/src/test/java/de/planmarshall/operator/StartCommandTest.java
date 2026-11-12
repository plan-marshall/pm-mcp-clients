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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.operator.transcript.TerminalTestDriver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The acts of the folded verb {@code start} through their design verbs: a plan starts with
 * {@code plan add}, an epic with {@code epic add}, and a recipe with {@code recipe run}
 * (01-commands.adoc#command-tree, 18-starting.adoc), each against the start entry of the mock
 * runtime.
 */
@EnableGeneratorController
@DisplayName("Starting work through plan add, epic add and recipe run")
class StartCommandTest {

    private static final String PROJECT = "pm-mcp-clients";

    @Test
    @DisplayName("plan add starts a plan from its description and title without asking")
    void planAddStartsAPlan() throws Exception {
        var description = Generators.letterStrings(8, 24).next();
        var title = Generators.letterStrings(8, 24).next();

        var outcome = run(StandardScenarios.START_SOURCES,
                            "plan", "add", "--project", PROJECT, "--description", description,
                            "--title", title, "--no-input");

        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains(description), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    @Test
    @DisplayName("plan add without a source stops at the sources with exit 3")
    void planAddWithoutASourceStops() throws Exception {
        var outcome = run(StandardScenarios.START_SOURCES, "plan", "add", "--project", PROJECT, "--no-input");

        assertAll(
                            () -> assertEquals(StartDialogue.EXIT_MISSING_INPUT, outcome.code()),
                            () -> assertTrue(outcome.rendered().contains("start.sources"), outcome.rendered()));
    }

    @Test
    @DisplayName("epic add starts an epic from its title and description")
    void epicAddStartsAnEpic() throws Exception {
        var description = Generators.letterStrings(8, 24).next();

        var outcome = run(StandardScenarios.START_EPIC,
                            "epic", "add", "--project", PROJECT, "--title", "Relay hardening",
                            "--description", description, "--no-input");

        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("relay-hardening has started"),
                                                outcome.rendered()));
    }

    @Test
    @DisplayName("recipe run starts a recipe with its parameter")
    void recipeRunStartsARecipe() throws Exception {
        var outcome = run(StandardScenarios.START_SOURCES,
                            "recipe", "run", "code-review", "--project", PROJECT, "--param", "depth=quick",
                            "--no-input");

        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("code-review"), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    @Test
    @DisplayName("plan add reports a refused start with the action that fixes it and exit 1")
    void planAddRefused() throws Exception {
        var outcome = run(StandardScenarios.START_REFUSED,
                            "plan", "add", "--project", PROJECT, "--entry", "ent-3f9a2c41", "--no-input");

        assertAll(
                            () -> assertEquals(1, outcome.code()),
                            () -> assertTrue(outcome.rendered().contains("is not ready"), outcome.rendered()));
    }

    private static Outcome run(String scenario, String... args) throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder().scenario(scenario)
                .timing(MockTiming.ORDERED).build());
             var driver = TerminalTestDriver.create()) {
            recordLiveRuntime(runtime);
            var environment = new HashMap<>(runtime.environment());
            environment.put("LANG", "en_US.UTF-8");
            environment.put("USER", "operator");
            driver.project(environment, runtime.base().getParent());
            driver.endOfInput();
            var code = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(), args);
            return new Outcome(code, driver.render());
        }
    }

    private static void recordLiveRuntime(MockRuntime runtime) throws IOException {
        var self = ProcessHandle.current();
        var start = self.info().startInstant().orElseThrow();
        Files.createDirectories(runtime.paths().stateDir());
        Files.writeString(runtime.paths().runtimeRecord(),
                            "{\"pid\":" + self.pid() + ",\"start_instant\":\"" + start + "\"}",
                            StandardCharsets.UTF_8);
    }

    private record Outcome(int code, String rendered) {
    }
}
