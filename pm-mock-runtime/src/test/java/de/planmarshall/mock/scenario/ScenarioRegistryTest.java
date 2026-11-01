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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.mock.StandardScenarios;

@DisplayName("ScenarioRegistry")
@EnableGeneratorController
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
        var scenarioName = Generators.letterStrings(4, 12).next().toLowerCase();
        var stateName = Generators.letterStrings(4, 12).next().toLowerCase();
        var scenarioFile = tempDir.resolve(scenarioName + ".json");
        var json = """
                {
                  "format_version": 1,
                  "name": "%s",
                  "start": "%s",
                  "states": {
                    "%s": {
                      "representation": {
                        "scope": { "kind": "machine" },
                        "state": { "state_id": "%s", "kind": "operator_wait" },
                        "links": []
                      }
                    }
                  }
                }
                """.formatted(scenarioName, stateName, stateName, stateName);
        Files.writeString(scenarioFile, json);

        var scenario = registry.resolve(scenarioFile.toString());
        assertNotNull(scenario);
        assertEquals(scenarioName, scenario.name());
        assertTrue(scenario.states().containsKey(stateName));
    }

    @Test
    @DisplayName("throws IllegalArgumentException for unknown scenario")
    void throwsOnUnknownScenario() {
        var unknown = "absent-" + Generators.letterStrings(8, 16).next();
        var ex = assertThrows(IllegalArgumentException.class, () -> registry.resolve(unknown));
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

        var unknownState = Generators.letterStrings(4, 10).next();
        var refuses = assertThrows(IllegalArgumentException.class,
                () -> registry.resolve(StandardScenarios.RUNNING_WORK + "#" + unknownState));
        assertTrue(refuses.getMessage().contains(unknownState));
    }

    @Test
    @DisplayName("splits a composite reference into scenario and state")
    void splitsCompositeReference() {
        var scenarioName = Generators.letterStrings(4, 10).next();
        var stateName = Generators.letterStrings(4, 10).next();
        assertEquals(scenarioName, ScenarioRegistry.scenarioNameOf(scenarioName + "#" + stateName));
        assertEquals(stateName, ScenarioRegistry.stateNameOf(scenarioName + "#" + stateName));
        assertEquals(scenarioName, ScenarioRegistry.scenarioNameOf(scenarioName));
        assertNull(ScenarioRegistry.stateNameOf(scenarioName));
    }

    @Test
    @DisplayName("resolves a legacy alias only when the legacy file is absent")
    void resolvesLegacyAliasAsFallback() {
        var alias = "alias-" + Generators.letterStrings(4, 12).next().toLowerCase();
        registry.registerLegacyAlias(alias, StandardScenarios.PROJECTS_AND_START
                + "#projects.explorer");
        var aliased = registry.resolve(alias);
        assertEquals(StandardScenarios.PROJECTS_AND_START, aliased.name());
        assertEquals("projects.explorer", aliased.start());

        // A consolidated scenario resolves through its alias once the legacy file is gone.
        assertEquals(StandardScenarios.REPORTS_AND_SYSTEM,
                registry.resolve(StandardScenarios.REPORTS).name());
        assertEquals("reports.unread", registry.resolve(StandardScenarios.REPORTS).start());
        assertEquals("projects-and-start#projects.explorer",
                new ScenarioRegistry().legacyAlias(StandardScenarios.PROJECTS));
    }

    @Test
    @DisplayName("publishes the standard legacy alias table")
    void publishesStandardAliases() {
        var aliases = registry.legacyAliases();
        assertEquals("onboarding#harness.not-logged-in", aliases.get("onboarding-harness-not-logged-in"));
        assertEquals("projects-and-start#start.sources", aliases.get("start"));
        assertEquals("reports-and-system#reports.unread", aliases.get("reports"));
        assertEquals("reports-and-system#system.running", aliases.get("system"));
        assertNull(aliases.get("designer"), "a primary scenario needs no alias");
    }

    @Test
    @DisplayName("every standard legacy alias resolves to an existing state")
    void everyStandardAliasResolves() {
        for (var alias : registry.legacyAliases().entrySet()) {
            var scenario = registry.resolve(alias.getValue());
            assertEquals(ScenarioRegistry.stateNameOf(alias.getValue()), scenario.start(), alias.getKey());
        }
    }

    @Test
    @DisplayName("an alias does not overwrite the primary scenario's own start state")
    void aliasDoesNotOverwritePrimaryStart() {
        var alias = "ghost-" + Generators.letterStrings(4, 12).next().toLowerCase();
        registry.registerLegacyAlias(alias, StandardScenarios.PROJECTS_AND_START
                + "#start.sources");
        assertEquals("start.sources", registry.resolve(alias).start());

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
        var unknown = "unknown-" + Generators.letterStrings(8, 16).next();
        assertTrue(registry.find(StandardScenarios.STATUS_ONLY).isPresent());
        assertTrue(registry.find(unknown).isEmpty());
        assertTrue(registry.find(null).isEmpty());

        registry.clear();
        assertTrue(registry.find(StandardScenarios.STATUS_ONLY).isPresent());
    }
}
