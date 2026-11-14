/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.designer.UnitPlacingResult;
import de.planmarshall.api.designer.WorkflowFinding;
import de.planmarshall.api.designer.WorkflowUnit;
import de.planmarshall.api.designer.WorkflowValidationRequest;
import de.planmarshall.api.designer.WorkflowValidationResult;
import de.planmarshall.api.designer.WorkflowValidationResult.Stage;
import de.planmarshall.mock.scenario.RouteDefinition;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.StateDefinition;

@DisplayName("DesignerRegistry")
@EnableGeneratorController
class DesignerRegistryTest {

    private static final String BASE_STATE = "designer.ready";

    private final DesignerRegistry registry = new DesignerRegistry();

    private static String lowerLetters() {
        return Generators.letterStrings(4, 10).next().toLowerCase(Locale.ROOT);
    }

    private static String header(String name) {
        return "workflow " + name + " version 1 format 1";
    }

    private static WorkflowValidationResult validate(String text, String project) {
        return new DesignerRegistry().validate(new WorkflowValidationRequest(text, null, null, project, null));
    }

    private static List<String> statuses(WorkflowValidationResult result) {
        return result.stages().stream().map(Stage::status).toList();
    }

    @Nested
    @DisplayName("Validating a unit")
    class Validating {

        @Test
        @DisplayName("fails the parse at the first written line when it is no header")
        void failsTheParse() {
            var text = "\n\n" + lowerLetters();

            var result = validate(text, null);

            assertFalse(result.valid());
            assertEquals(List.of(Stage.STATUS_FAILED, Stage.STATUS_NOT_RUN, Stage.STATUS_NOT_RUN, Stage.STATUS_NOT_RUN),
                    statuses(result));
            assertEquals("Rule 1", result.findings().getFirst().rule());
            assertEquals(3, result.findings().getFirst().line(), "the header is looked for past blank lines");
        }

        @Test
        @DisplayName("fails the static rules for a missing frontmatter, a plan-marshall author and a goto")
        void failsTheStaticRules() {
            var text = String.join("\n", header(lowerLetters()), "author: plan-marshall", "goto");

            var result = validate(text, null);

            assertFalse(result.valid());
            assertEquals(Stage.STATUS_PASSED, result.stages().getFirst().status());
            assertEquals(Stage.STATUS_FAILED, result.stages().get(1).status());
            assertEquals(List.of("frontmatter.missing", "frontmatter.author", "Rule 6"),
                    result.findings().stream().map(WorkflowFinding::rule).toList());
        }

        @Test
        @DisplayName("checks the compiled invariants of the unit the header names")
        void failsTheInvariants() {
            var text = String.join("\n", header("phase-6-finalize"), "---", "name: Finalize", "---",
                    "state finalize.review");

            var result = new DesignerRegistry().validate(
                    new WorkflowValidationRequest(text, null, lowerLetters(), null, null));

            assertFalse(result.valid());
            assertEquals(Stage.STATUS_FAILED, result.stages().get(2).status());
            assertEquals("INV-FIN-1", result.findings().getFirst().rule());
            assertEquals(5, result.findings().getFirst().line());
        }

        @Test
        @DisplayName("accepts a unit and composes it only where a project is named or enrolled")
        void acceptsAUnit() {
            var displayName = lowerLetters();
            var text = String.join("\n", header(lowerLetters()), "---", "name: " + displayName,
                    "entry start Begins", "entry", "exit done", "---", "state a", "call x", "ok -> done");

            var machine = validate(text, null);
            var project = validate(text, lowerLetters());

            assertTrue(machine.valid());
            assertEquals(Stage.STATUS_NOT_RUN, machine.stages().getLast().status(),
                    "no project to compose for");
            assertNull(machine.composed());
            assertEquals(displayName, machine.structure().displayName());
            assertEquals(List.of("start Begins", ""), machine.structure().entries());
            assertEquals(List.of("done"), machine.structure().exits());
            assertEquals(1, machine.structure().states().size());
            assertTrue(project.valid());
            assertEquals(List.of(Stage.STATUS_PASSED, Stage.STATUS_PASSED, Stage.STATUS_PASSED, Stage.STATUS_PASSED),
                    statuses(project));
            assertNotNull(project.composed());
        }

        @Test
        @DisplayName("reads a frontmatter that is never closed to the end of the text")
        void readsAnUnclosedFrontmatter() {
            var displayName = lowerLetters();
            var text = String.join("\n", header(lowerLetters()), "---", "name: " + displayName);

            var result = validate(text, null);

            assertEquals(displayName, result.structure().displayName());
        }
    }

    @Nested
    @DisplayName("Placing and removing a unit")
    class Placing {

        @Test
        @DisplayName("places a unit at the machine level, replacing one of the same name")
        void placesAtTheMachineLevel() {
            var name = lowerLetters();

            registry.place(name, DesignerRegistry.MACHINE, header(name), lowerLetters(), "1.0");
            var placed = registry.place(name, DesignerRegistry.MACHINE, header(name), lowerLetters(), "1.0");

            assertEquals(List.of(UnitPlacingResult.STEP_WRITTEN), placed.steps());
            assertEquals("sha256:machine-r3", registry.digest(DesignerRegistry.MACHINE), "each placing is a revision");
            assertEquals(1, registry.machineUnits().size(), "the second placing replaced the first");
            assertNull(registry.machineUnits().getFirst().frontmatter(), "a unit without frontmatter has none");
            assertEquals(WorkflowUnit.LEVEL_MACHINE, registry.machineUnits().getFirst().levelInForce());
        }

        @Test
        @DisplayName("places a unit in a project by a short landing")
        void placesInAProject() {
            var name = lowerLetters();
            var project = lowerLetters();
            var text = String.join("\n", header(name), "---", "name: " + name, "---");

            var before = registry.digest(project);

            var placed = registry.place(name, project, text, lowerLetters(), "1.0");

            assertEquals(List.of(UnitPlacingResult.STEP_WRITTEN, UnitPlacingResult.STEP_COMMITTED,
                    UnitPlacingResult.STEP_LANDED), placed.steps());
            assertNotEquals(before, registry.digest(project), "placing in a project the scenario did not seed changes its digest, so the app reloads");
            assertEquals(registry.digest(project), registry.projectCatalogue(project).digest());
            assertNotNull(registry.findUnit(project, name));
            assertNotNull(registry.findUnit(project, name).frontmatter());
        }

        @Test
        @DisplayName("removes a unit, and a removal in a project without units changes nothing else")
        void removes() {
            var name = lowerLetters();
            registry.place(name, DesignerRegistry.MACHINE, header(name), lowerLetters(), "1.0");
            var unknownProject = lowerLetters();

            registry.remove(name, DesignerRegistry.MACHINE);
            var removed = registry.remove(name, unknownProject);

            assertTrue(registry.machineUnits().isEmpty());
            assertEquals(UnitPlacingResult.STATUS_LANDED, removed.status());
            assertNull(registry.findUnit(unknownProject, name));
        }
    }

    @Nested
    @DisplayName("Seeding from the scenario")
    class Seeding {

        @Test
        @DisplayName("holds nothing without a scenario")
        void withoutAScenario() {
            registry.init(null);

            assertTrue(registry.enrolledProjects().isEmpty());
            assertTrue(registry.machineUnits().isEmpty());
            assertEquals("0.0.0", registry.machineCatalogue().runtimeVersion());
        }

        @Test
        @DisplayName("seeds from the routes, and the state's designer fixture replaces what it names")
        void stateFixtureReplacesTheRoutes() {
            var routeProject = lowerLetters();
            var fixtureProject = lowerLetters() + "-fixture";
            var routeUnit = lowerLetters();
            var machineUnit = lowerLetters();
            var fixtureUnit = lowerLetters();
            var routes = new LinkedHashMap<String, RouteDefinition>();
            routes.put("GET /api/v1/workflows", RouteDefinition.of(List.of(Map.of("name", routeUnit))));
            routes.put("GET /api/v1/workflows/catalogue", RouteDefinition.of(Map.of("version", " ")));
            routes.put("GET /api/v1/projects/" + routeProject + "/workflows",
                    RouteDefinition.of(Map.of("units", List.of(Map.of("name", routeUnit)))));
            routes.put("GET /api/v1/projects/" + routeProject + "/nested/workflows",
                    RouteDefinition.of(Map.of("units", List.of())));
            var designer = Map.<String, Object>of(
                    "machine_units", List.of(Map.of("name", machineUnit)),
                    "machine_blocks", "no list",
                    "project_units", Map.of(fixtureProject, List.of(Map.of("name", fixtureUnit))),
                    "project_blocks", Map.of(fixtureProject, List.of(Map.of("kind", "keyword", "name", "state"))));

            registry.init(scenario(routes, designer));

            assertEquals("0.0.0", registry.machineCatalogue().runtimeVersion(), "a blank version is no version");
            assertEquals(List.of(machineUnit), registry.machineUnits().stream().map(WorkflowUnit::name).toList());
            assertTrue(registry.machineCatalogue().blocks().isEmpty(), "a block fixture that is no list holds none");
            assertEquals(List.of(fixtureUnit),
                    registry.projectUnits(fixtureProject).stream().map(WorkflowUnit::name).toList());
            assertEquals(1, registry.projectCatalogue(fixtureProject).blocks().size());
            assertTrue(registry.projectUnits(routeProject).isEmpty(), "the fixture's projects replace the routes'");
            assertEquals(Set.of(routeProject), registry.enrolledProjects(),
                    "the enrolled projects are those of the routes, a nested path names none");
        }

        private static Scenario scenario(Map<String, RouteDefinition> routes, Map<String, Object> designer) {
            var representation = new LinkedHashMap<String, Object>();
            representation.put("scope", Map.of("kind", "machine"));
            representation.put("state", Map.of("state_id", BASE_STATE, "kind", "operator_wait"));
            representation.put("facts", Map.of("runtime", Map.of("designer", designer)));
            var states = new LinkedHashMap<String, StateDefinition>();
            states.put(BASE_STATE, new StateDefinition(representation, Map.of(), List.of(), null));
            return new Scenario(1, "designer-test", "DesignerRegistry test", BASE_STATE, "CODE", routes, states);
        }
    }
}
