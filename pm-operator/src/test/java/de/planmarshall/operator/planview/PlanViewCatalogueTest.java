/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.planview;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.process.NodeState;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("plan view catalogue and process-contract states")
class PlanViewCatalogueTest {

    private static final List<String> TEXT_KEYS = List.of(
                        "operator.plan.keys.open",
                        "operator.plan.keys.close",
                        "operator.plan.keys.details",
                        "operator.plan.keys.where",
                        "operator.plan.keys.all",
                        "operator.plan.keys.not_in_plan",
                        "operator.plan.keys.actions",
                        "operator.plan.keys.project",
                        "operator.plan.keys.epic",
                        "operator.plan.keys.completion",
                        "operator.plan.keys.tokens",
                        "operator.plan.header.node",
                        "operator.plan.header.node_epic",
                        "operator.plan.header.ended",
                        "operator.plan.header.holds",
                        "operator.plan.tree.context",
                        "operator.plan.tree.context_epic",
                        "operator.plan.tree.ended",
                        "operator.plan.tree.sub_workflow",
                        "operator.plan.tokens.split",
                        "operator.plan.tokens.coverage",
                        "operator.plan.tokens.components",
                        "operator.plan.tokens.by_phase",
                        "operator.plan.tokens.by_role",
                        "operator.plan.activity.subheader",
                        "operator.plan.activity.off_track_banner",
                        "operator.plan.completion.title");

    @Test
    @DisplayName("every text of the terminal plan view exists in English and German")
    void textsExistInBothLanguages() {
        for (var key : TEXT_KEYS) {
            assertTrue(MessageCatalogue.hasKey(key), key);
            var en = MessageCatalogue.format("en", key, Map.of());
            var de = MessageCatalogue.format("de", key, Map.of());
            assertNotEquals(en, key, key);
            assertNotEquals(de, key, key);
        }
    }

    @Test
    @DisplayName("the plan-view scenario's process states are the closed list of the contract")
    void scenarioStatesAreClosed() throws Exception {
        var json = resource("/scenarios/running-work.json");
        var root = JsonTree.asObject(JsonTree.parse(json));
        assertNotNull(root);
        var routes = JsonTree.asObject(root.get("routes"));
        assertNotNull(routes);
        var states = new ArrayList<String>();
        for (var entry : routes.entrySet()) {
            if (!entry.getKey().contains("/process")) {
                continue;
            }
            var route = JsonTree.asObject(entry.getValue());
            var representation = route != null ? JsonTree.asObject(route.get("representation")) : null;
            if (representation != null && representation.get("nodes") instanceof List<?> nodes) {
                collectStates(nodes, states);
            }
        }
        var closed = Arrays.stream(NodeState.values()).map(NodeState::value).toList();
        assertFalse(states.isEmpty(), "the scenario must carry process nodes");
        for (var state : states) {
            assertTrue(closed.contains(state), "unknown state in the scenario: " + state);
        }
    }

    private static void collectStates(List<?> nodes, List<String> states) throws IOException {
        for (var item : nodes) {
            var node = JsonTree.asObject(item);
            if (node == null) {
                continue;
            }
            var state = JsonTree.string(node, "state");
            if (state != null) {
                states.add(state);
            }
            if (node.get("sub_nodes") instanceof List<?> subs) {
                collectStates(subs, states);
            }
        }
    }

    private static String resource(String name) throws IOException {
        try (InputStream stream = PlanViewCatalogueTest.class.getResourceAsStream(name)) {
            assertNotNull(stream, "missing resource " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
