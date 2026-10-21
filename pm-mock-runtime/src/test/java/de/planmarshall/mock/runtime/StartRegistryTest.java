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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.mock.scenario.RouteDefinition;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.StateDefinition;

@DisplayName("StartRegistry")
class StartRegistryTest {

    private static final String BASE_STATE = "running_work.active";
    private static final String START_STATE = "start.state";

    @Test
    @DisplayName("seeds the entry and the recipes from the start state merged over the base state")
    void seedsFromTheStateFixture() {
        var registry = new StartRegistry(_ -> {
        });
        registry.init(scenario(
                Map.of("start", Map.of("recipes", List.of(Map.of("key", "base-recipe", "name", "base-recipe")))),
                Map.of("start", Map.of("project_id", "p", "kind", "plan", "state", "start.sources",
                        "entry_id", "ent-test", "sources", List.of()))));

        var entry = registry.find("ent-test");
        assertNotNull(entry);
        assertEquals("p", entry.entry().projectId());
        assertEquals(1, registry.recipes("p").size());
        assertEquals("base-recipe", registry.recipes("p").getFirst().key());
    }

    @Test
    @DisplayName("falls back to the GET /api/v1/recipes route when the state names no recipe")
    void seedsRecipesFromTheRoute() {
        var registry = new StartRegistry(_ -> {
        });
        registry.init(scenario(Map.of(),
                Map.of("start", Map.of("project_id", "p", "kind", "plan", "state", "start.sources",
                        "entry_id", "ent-test")),
                Map.of("GET /api/v1/recipes", RouteDefinition.of(Map.of("recipes",
                        List.of(Map.of("key", "route-recipe", "name", "route-recipe")))))));

        assertNotNull(registry.find("ent-test"));
        assertEquals("route-recipe", registry.recipes("p").getFirst().key());
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
