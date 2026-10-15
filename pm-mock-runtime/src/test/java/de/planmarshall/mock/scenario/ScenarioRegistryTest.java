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
        assertEquals(StandardScenarios.STATUS_ONLY, scenario.name());
    }

    @Test
    @DisplayName("resolves scenario with .json suffix")
    void resolvesWithJsonSuffix() {
        var scenario = registry.resolve(StandardScenarios.ONBOARDING_HAPPY + ".json");
        assertNotNull(scenario);
        assertEquals(StandardScenarios.ONBOARDING_HAPPY, scenario.name());
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
    @DisplayName("finds scenario safely returning Optional and handles clear")
    void findAndClear() {
        assertTrue(registry.find(StandardScenarios.STATUS_ONLY).isPresent());
        assertTrue(registry.find("unknown-scenario").isEmpty());
        assertTrue(registry.find(null).isEmpty());

        registry.clear();
        assertTrue(registry.find(StandardScenarios.STATUS_ONLY).isPresent());
    }
}
