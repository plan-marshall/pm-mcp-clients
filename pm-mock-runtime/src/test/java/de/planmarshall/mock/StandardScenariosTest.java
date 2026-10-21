/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.interaction.OperatorStateResource;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.scenario.ScenarioRegistry;

@DisplayName("StandardScenarios")
class StandardScenariosTest {

    @Test
    @DisplayName("defines all standard scenario constants correctly")
    void shouldDefineStandardConstants() throws Exception {
        assertEquals("status-only", StandardScenarios.STATUS_ONLY);
        assertEquals("onboarding-happy", StandardScenarios.ONBOARDING_HAPPY);
        assertEquals("onboarding-harness-not-logged-in", StandardScenarios.ONBOARDING_HARNESS_NOT_LOGGED_IN);
        assertEquals("web-pairing", StandardScenarios.WEB_PAIRING);
        assertEquals("entry", StandardScenarios.ENTRY);
        assertEquals("paired-devices", StandardScenarios.PAIRED_DEVICES);
        assertEquals("running-work", StandardScenarios.RUNNING_WORK);
        assertEquals("decisions", StandardScenarios.DECISIONS);
        assertEquals("system", StandardScenarios.SYSTEM);
        assertEquals("projects", StandardScenarios.PROJECTS);
        assertEquals("plan-view", StandardScenarios.PLAN_VIEW);
        assertEquals("control", StandardScenarios.CONTROL);
        assertEquals("designer", StandardScenarios.DESIGNER);
        assertEquals("fully-onboarded", StandardScenarios.FULLY_ONBOARDED);

        // ALL lists exactly the scenario constants declared here. Reading them by reflection keeps
        // the test correct when a scenario is added, so it does not break on a sibling change.
        var expected = new TreeSet<String>();
        for (var field : StandardScenarios.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                expected.add((String) field.get(null));
            }
        }
        assertEquals(expected, new TreeSet<>(StandardScenarios.ALL));
    }

    @Test
    @DisplayName("defines exactly the six canonical primary scenarios")
    void shouldDefineSixPrimaryScenarios() {
        assertEquals(6, StandardScenarios.PRIMARY_SCENARIOS.size());
        assertEquals(StandardScenarios.PRIMARY_SCENARIOS, List.of("onboarding", "running-work", "projects-and-start",
                "reports-and-system", "designer", "fully-onboarded"));
        assertEquals("projects-and-start", StandardScenarios.PROJECTS_AND_START);
        assertEquals("reports-and-system", StandardScenarios.REPORTS_AND_SYSTEM);
        assertEquals("fully-onboarded", StandardScenarios.FULLY_ONBOARDED);
    }

    @Test
    @DisplayName("the primary list is the one resource the launcher reads too")
    void primaryListComesFromTheSharedResource() throws Exception {
        try (var in = getClass().getResourceAsStream("/scenarios/primary.txt")) {
            assertNotNull(in);
            try (var reader = new BufferedReader(
                         new InputStreamReader(in, StandardCharsets.UTF_8))) {
                var names = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();
                assertEquals(StandardScenarios.PRIMARY_SCENARIOS, names);
            }
        }
    }

    @Test
    @DisplayName("every primary scenario has a delivered resource file")
    void primaryScenariosHaveResources() {
        var scenarios = Path.of("src", "main", "resources", "scenarios");
        if (!Files.isDirectory(scenarios)) {
            scenarios = Path.of("pm-mock-runtime", "src", "main", "resources", "scenarios");
        }
        for (var name : StandardScenarios.PRIMARY_SCENARIOS) {
            assertTrue(Files.isRegularFile(scenarios.resolve(name + ".json")), name + ".json is missing");
        }
    }

    @Test
    @DisplayName("delivers exactly the canonical scenario files and no stray case file")
    void canonicalScenarioFilesAreExactlyThePrimarySet() throws Exception {
        var scenarios = scenariosDirectory();
        TreeSet<String> delivered;
        try (var files = Files.list(scenarios)) {
            delivered = files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        var expected = StandardScenarios.PRIMARY_SCENARIOS.stream()
                .map(name -> name + ".json")
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(expected, delivered);
    }

    @Test
    @DisplayName("every state of every canonical scenario decodes as an OperatorStateResource")
    void everyCanonicalStateDecodesAsAnOperatorState() throws Exception {
        var registry = new ScenarioRegistry();
        for (var name : StandardScenarios.PRIMARY_SCENARIOS) {
            var scenario = registry.resolve(name);
            for (var entry : scenario.states().entrySet()) {
                var representation = JsonTree.asObject(entry.getValue().representation());
                var state = OperatorStateResource.fromMap(representation);

                assertEquals(entry.getKey(), state.stateId(), name + " state");
            }
        }
    }

    private static Path scenariosDirectory() {
        var scenarios = Path.of("src", "main", "resources", "scenarios");
        return Files.isDirectory(scenarios)
                ? scenarios
                : Path.of("pm-mock-runtime", "src", "main", "resources", "scenarios");
    }

    @Test
    @DisplayName("resolves every control scenario constant to the consolidated running-work scenario")
    void controlScenariosResolveIntoRunningWork() throws Exception {
        var constants = new TreeSet<String>();
        for (var field : StandardScenarios.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                var value = (String) field.get(null);
                if (value.startsWith("control")) {
                    constants.add(value);
                }
            }
        }

        var scenarios = Path.of("src", "main", "resources", "scenarios");
        if (!Files.isDirectory(scenarios)) {
            scenarios = Path.of("pm-mock-runtime", "src", "main", "resources", "scenarios");
        }
        var resources = new TreeSet<String>();
        try (var files = Files.list(scenarios)) {
            files.filter(path -> path.getFileName().toString().startsWith("control"))
                    .forEach(path -> resources.add(path.getFileName().toString().replace(".json", "")));
        }
        assertEquals(Set.of(), resources, "the control case files are consolidated into running-work.json");

        var registry = new ScenarioRegistry();
        for (var name : constants) {
            var scenario = registry.resolve(name);
            assertEquals(StandardScenarios.RUNNING_WORK, scenario.name(), name);
        }
    }

    @Test
    @DisplayName("utility class constructor throws UnsupportedOperationException")
    void shouldRefuseInstantiation() throws Exception {
        var constructor = StandardScenarios.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, ex.getCause());
    }
}
