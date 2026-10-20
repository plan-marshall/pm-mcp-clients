/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.planmarshall.mock.StandardScenarios;

@DisplayName("ScenarioRegistry")
class ScenarioRegistryTest {

    private ScenarioRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ScenarioRegistry();
    }

    @Test
    @DisplayName("resolves scenario by bare name from classpath")
    void resolvesByBareName() {
        var scenario = registry.resolve(StandardScenarios.STATUS_ONLY);
        assertNotNull(scenario);
        assertEquals(StandardScenarios.ONBOARDING, scenario.name());
        assertEquals("status.only", scenario.start());
    }

    @Test
    @DisplayName("resolves scenario with .json suffix")
    void resolvesWithJsonSuffix() {
        var scenario = registry.resolve(StandardScenarios.ONBOARDING_HAPPY + ".json");
        assertNotNull(scenario);
        assertEquals(StandardScenarios.ONBOARDING, scenario.name());
        assertEquals("onboard.mode", scenario.start());
    }

    @Test
    @DisplayName("caches resolved scenarios")
    void cachesScenarios() {
        var first = registry.resolve(StandardScenarios.STATUS_ONLY);
        var second = registry.resolve(StandardScenarios.STATUS_ONLY);
        assertSame(first, second);
    }

    @Test
    @DisplayName("resolves scenario from external filesystem path")
    void resolvesFromFilePath(@TempDir Path tempDir) throws Exception {
        var scenarioFile = tempDir.resolve("custom-scenario.json");
        var json = """
                {
                  "format_version": 1,
                  "name": "custom-scenario",
                  "start": "init",
                  "states": {
                    "init": {
                      "representation": { "hello": "world" }
                    }
                  }
                }
                """;
        Files.writeString(scenarioFile, json);

        var scenario = registry.resolve(scenarioFile.toString());
        assertNotNull(scenario);
        assertEquals("custom-scenario", scenario.name());
        assertTrue(scenario.states().containsKey("init"));
    }

    @Test
    @DisplayName("throws IllegalArgumentException for unknown scenario")
    void throwsOnUnknownScenario() {
        var ex = assertThrows(IllegalArgumentException.class, () -> registry.resolve("non-existent-scenario"));
        assertTrue(ex.getMessage().contains("Scenario not found"));
    }

    @Test
    @DisplayName("rejects null or blank scenario name")
    void rejectsNullOrBlank() {
        assertThrows(NullPointerException.class, () -> registry.resolve(null));
        assertThrows(IllegalArgumentException.class, () -> registry.resolve("   "));
    }

    @Test
    @DisplayName("resolves the composite name#state notation to the named start state")
    void resolvesCompositeReference() {
        var scenario = registry.resolve(StandardScenarios.RUNNING_WORK + "#running_work.active");
        assertEquals(StandardScenarios.RUNNING_WORK, scenario.name());
        assertEquals("running_work.active", scenario.start());

        var refuses = assertThrows(IllegalArgumentException.class,
                () -> registry.resolve(StandardScenarios.RUNNING_WORK + "#no.such.state"));
        assertTrue(refuses.getMessage().contains("no.such.state"));
    }

    @Test
    @DisplayName("splits a composite reference into scenario and state")
    void splitsCompositeReference() {
        assertEquals("running-work", ScenarioRegistry.scenarioNameOf("running-work#plan.paused"));
        assertEquals("plan.paused", ScenarioRegistry.stateNameOf("running-work#plan.paused"));
        assertEquals("running-work", ScenarioRegistry.scenarioNameOf("running-work"));
        assertNull(ScenarioRegistry.stateNameOf("running-work"));
    }

    @Test
    @DisplayName("resolves a legacy alias only when the legacy file is absent")
    void resolvesLegacyAliasAsFallback() {
        registry.registerLegacyAlias("ghost-legacy", StandardScenarios.PROJECTS_AND_START
                + "#projects.explorer");
        var aliased = registry.resolve("ghost-legacy");
        assertEquals(StandardScenarios.PROJECTS_AND_START, aliased.name());
        assertEquals("projects.explorer", aliased.start());

        // A legacy file that still exists wins over its alias, so migration can delete file by file.
        registry.registerLegacyAlias(StandardScenarios.PROJECTS, "designer#designer.ready");
        assertEquals(StandardScenarios.PROJECTS, registry.resolve(StandardScenarios.PROJECTS).name());
        assertEquals("projects-and-start#projects.explorer",
                new ScenarioRegistry().legacyAlias(StandardScenarios.PROJECTS));
    }

    @Test
    @DisplayName("publishes the standard legacy alias table")
    void publishesStandardAliases() {
        var aliases = registry.legacyAliases();
        assertEquals("onboarding#harness.not-logged-in", aliases.get("onboarding-harness-not-logged-in"));
        assertEquals("projects-and-start#start.sources", aliases.get("start"));
        assertEquals("reports-and-system#reports.list", aliases.get("reports"));
        assertNull(aliases.get("designer"), "a primary scenario needs no alias");
    }

    @Test
    @DisplayName("an alias does not overwrite the primary scenario's own start state")
    void aliasDoesNotOverwritePrimaryStart() {
        registry.registerLegacyAlias("ghost-start", StandardScenarios.PROJECTS_AND_START
                + "#start.sources");
        assertEquals("start.sources", registry.resolve("ghost-start").start());

        var primary = registry.resolve(StandardScenarios.PROJECTS_AND_START);
        assertEquals("projects.explorer", primary.start(),
                "the alias must be cached under its own name only");
    }

    @Test
    @DisplayName("refuses a self-referencing alias")
    void refusesSelfAlias() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.registerLegacyAlias("designer", "designer#designer.ready"));
    }

    @Test
    @DisplayName("finds scenario safely returning Optional and handles clear")
    void findAndClear() {
        assertTrue(registry.find(StandardScenarios.STATUS_ONLY).isPresent());
        assertTrue(registry.find("unknown-scenario").isEmpty());
        assertTrue(registry.find(null).isEmpty());

        registry.clear();
        assertTrue(registry.find(StandardScenarios.STATUS_ONLY).isPresent());
    }
}
