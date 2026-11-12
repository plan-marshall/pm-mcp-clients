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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.mock.scenario.RouteDefinition;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.StateDefinition;

@DisplayName("StartRegistry")
@EnableGeneratorController
class StartRegistryTest {

    private static final String BASE_STATE = "running_work.active";
    private static final String START_STATE = "start.state";

    @Test
    @DisplayName("seeds the entry and the recipes from the start state merged over the base state")
    void seedsFromTheStateFixture() {
        var projectId = Generators.letterStrings(3, 8).next().toLowerCase();
        var entryId = "ent-" + Generators.letterStrings(4, 10).next().toLowerCase();
        var recipeKey = "recipe-" + Generators.letterStrings(3, 8).next().toLowerCase();
        var registry = new StartRegistry(_ -> {
        });
        registry.init(scenario(
                Map.of("start", Map.of("recipes", List.of(Map.of("key", recipeKey, "name", recipeKey)))),
                Map.of("start", Map.of("project_id", projectId, "kind", "plan", "state", "start.sources",
                        "entry_id", entryId, "sources", List.of()))), START_STATE);

        var entry = registry.find(entryId);
        assertNotNull(entry);
        assertEquals(projectId, entry.entry().projectId());
        assertEquals(1, registry.recipes(projectId).size());
        assertEquals(recipeKey, registry.recipes(projectId).getFirst().key());
    }

    @Test
    @DisplayName("falls back to the GET /api/v1/recipes route when the state names no recipe")
    void seedsRecipesFromTheRoute() {
        var projectId = Generators.letterStrings(3, 8).next().toLowerCase();
        var entryId = "ent-" + Generators.letterStrings(4, 10).next().toLowerCase();
        var recipeKey = "recipe-" + Generators.letterStrings(3, 8).next().toLowerCase();
        var registry = new StartRegistry(_ -> {
        });
        registry.init(scenario(Map.of(),
                Map.of("start", Map.of("project_id", projectId, "kind", "plan", "state", "start.sources",
                        "entry_id", entryId)),
                Map.of("GET /api/v1/recipes", RouteDefinition.of(Map.of("recipes",
                        List.of(Map.of("key", recipeKey, "name", recipeKey)))))), START_STATE);

        assertNotNull(registry.find(entryId));
        assertEquals(recipeKey, registry.recipes(projectId).getFirst().key());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SourceKind.class, names = {"ISSUE", "PULL_REQUEST", "LESSON", "LOCAL_CHANGES"})
    @DisplayName("adds a source the state scripts no outcome for as the kind's default source")
    void addsTheDefaultSource(SourceKind kind) throws Exception {
        var entryId = seedPlanEntry();

        var added = registry.act(entryId, "add_" + kind.wireValue(), Map.of());

        assertNotNull(added);
        var sources = added.entry().sources();
        assertEquals(1, sources.size());
        assertEquals(kind, sources.getFirst().kind());
        assertFalse(sources.getFirst().reference().isBlank(), "the default source names what it brings in");
    }

    @Test
    @DisplayName("refuses a source kind no start carries with act_not_available")
    void refusesAnUnknownSourceKind() throws Exception {
        var entryId = seedPlanEntry();
        var unknownKind = Generators.letterStrings(4, 10).next().toLowerCase();

        var refused = assertThrows(StartRegistry.ActRefused.class,
                () -> registry.act(entryId, "add_" + unknownKind, Map.of()));

        assertEquals("act_not_available", refused.error());
        assertTrue(registry.find(entryId).entry().sources().isEmpty(), "a refused add adds nothing");
    }

    @Test
    @DisplayName("asks how documents are brought in, then moves them as the operator chooses")
    void documentsModeAskedThenMoved() throws Exception {
        var entryId = seedPlanEntry();

        var asked = registry.act(entryId, "add_documents", Map.of());
        assertNotNull(asked);
        assertEquals(SourceState.CHECKING, asked.entry().sources().getFirst().state());

        var moved = registry.act(entryId, "documents_mode:move", Map.of());

        assertNotNull(moved);
        assertEquals(SourceState.MOVED, moved.entry().sources().getFirst().state());
    }

    @Test
    @DisplayName("refuses a documents mode that is neither copy nor move with invalid_arguments")
    void refusesAnUnknownDocumentsMode() throws Exception {
        var entryId = seedPlanEntry();
        registry.act(entryId, "add_documents", Map.of());
        var unknownMode = Generators.letterStrings(4, 10).next().toLowerCase();

        var refused = assertThrows(StartRegistry.ActRefused.class,
                () -> registry.act(entryId, "documents_mode:" + unknownMode, Map.of()));

        assertEquals("invalid_arguments", refused.error());
        assertEquals(SourceState.CHECKING, registry.find(entryId).entry().sources().getFirst().state(),
                "a refused mode leaves the document waiting for the operator's choice");
    }

    /** The registry the act tests drive, seeded by {@link #seedPlanEntry()}. */
    private StartRegistry registry;

    /**
     * Seeds a plan's entry in the sources step with no source and no scripted add outcome.
     *
     * @return the entry's id
     */
    private String seedPlanEntry() {
        var projectId = Generators.letterStrings(3, 8).next().toLowerCase();
        var entryId = "ent-" + Generators.letterStrings(4, 10).next().toLowerCase();
        registry = new StartRegistry(_ -> {
        });
        registry.init(scenario(Map.of(),
                Map.of("start", Map.of("project_id", projectId, "kind", "plan", "state", "start.sources",
                        "entry_id", entryId, "sources", List.of()))), START_STATE);
        return entryId;
    }

    private static Scenario scenario(Map<String, Object> baseFixture, Map<String, Object> startFixture) {
        return scenario(baseFixture, startFixture, Map.of());
    }

    private static Scenario scenario(Map<String, Object> baseFixture, Map<String, Object> startFixture,
            Map<String, RouteDefinition> routes) {
        var states = new LinkedHashMap<String, StateDefinition>();
        if (!baseFixture.isEmpty()) {
            states.put(BASE_STATE, state(BASE_STATE, baseFixture));
        }
        if (!startFixture.isEmpty()) {
            states.put(START_STATE, state(START_STATE, startFixture));
        }
        return new Scenario(1, "registry-test", "StartRegistry test", START_STATE, "CODE", routes,
                states);
    }

    private static StateDefinition state(String stateId, Map<String, Object> fixture) {
        var representation = new LinkedHashMap<String, Object>();
        representation.put("scope", Map.of("kind", "workspace", "project_id", "p"));
        representation.put("state", Map.of("state_id", stateId, "kind", "operator_wait"));
        representation.put("facts", Map.of("runtime", fixture));
        return new StateDefinition(representation, Map.of(), List.of(), null);
    }
}
