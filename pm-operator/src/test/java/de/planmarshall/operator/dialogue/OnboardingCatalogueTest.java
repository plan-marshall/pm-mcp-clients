/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.dialogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The catalogue guard of the state dialogue ({@code CLI-UC-S-06}): every state the onboarding
 * scenario can reach from its start, and every link such a state offers, carries its text in both
 * locales. A state or a link without text is a raw identifier on the screen, so the guard fails
 * rather than letting the dialogue fall back to one.
 */
@DisplayName("Onboarding catalogue completeness")
class OnboardingCatalogueTest {

    private static final String SCENARIO = "/scenarios/onboarding.json";
    private static final String START = "onboard.mode";
    private static final List<String> LOCALES = List.of("en", "de");

    @Test
    @DisplayName("every state the scenario reaches carries a title in both locales")
    void everyReachableStateHasATitle() throws Exception {
        var missing = new ArrayList<String>();
        for (var stateId : reachableStates()) {
            var key = titleKey(stateId);
            for (var locale : LOCALES) {
                if (!MessageCatalogue.hasKey(locale, key)) {
                    missing.add(locale + ": " + key);
                }
            }
        }
        assertEquals(List.of(), missing, "states the dialogue may show without catalogue text");
    }

    @Test
    @DisplayName("every link a reachable state offers carries a label in both locales")
    void everyLinkHasALabel() throws Exception {
        var missing = new ArrayList<String>();
        for (var entry : reachableLinks().entrySet()) {
            for (var linkId : entry.getValue()) {
                for (var locale : LOCALES) {
                    if (!MessageCatalogue.hasKey(locale, "onboard.link." + linkId)
                                        && !MessageCatalogue.hasKey(locale, "common.consent." + linkId)) {
                        missing.add(locale + ": " + entry.getKey() + " -> " + linkId);
                    }
                }
            }
        }
        assertEquals(List.of(), missing, "links the dialogue may offer without catalogue text");
    }

    @Test
    @DisplayName("the traversal reads the scenario it guards")
    void traversalReadsTheScenario() throws Exception {
        var states = reachableStates();
        assertTrue(states.size() > 20, "the traversal found only " + states);
        assertTrue(states.contains(START), states.toString());

        var text = scenarioText();
        assertNotNull(text);
    }

    private static String titleKey(String stateId) {
        return "onboard." + StateDialogue.normalise(stateId) + ".title";
    }

    /**
     * @return the state ids the onboarding dialogue can reach from the scenario's start, following
     *         the links of every state and the automatic transitions
     */
    private static Set<String> reachableStates() throws IOException {
        var states = scenarioStates();
        var visited = new LinkedHashSet<String>();
        var queue = new ArrayDeque<String>();
        queue.add(START);
        while (!queue.isEmpty()) {
            var stateId = queue.poll();
            if (!visited.add(stateId)) {
                continue;
            }
            var state = JsonTree.asObject(states.get(stateId));
            if (state == null) {
                continue;
            }
            queue.addAll(nextOf(state));
        }
        return visited;
    }

    /**
     * @return the link ids each reachable state offers, by state id
     */
    private static Map<String, Set<String>> reachableLinks() throws IOException {
        var states = scenarioStates();
        var links = new LinkedHashMap<String, Set<String>>();
        for (var stateId : reachableStates()) {
            var state = JsonTree.asObject(states.get(stateId));
            if (state == null) {
                continue;
            }
            var offered = JsonTree.object(state, "links");
            links.put(stateId, offered == null ? Set.of() : new LinkedHashSet<>(offered.keySet()));
        }
        return links;
    }

    private static List<String> nextOf(Map<String, Object> state) {
        var next = new ArrayList<String>();
        var links = JsonTree.object(state, "links");
        if (links != null) {
            for (var entry : links.entrySet()) {
                var link = JsonTree.asObject(entry.getValue());
                var target = link != null ? JsonTree.string(link, "next") : null;
                if (target != null) {
                    next.add(target);
                }
            }
        }
        var auto = JsonTree.asObject(state.get("auto"));
        var automatic = auto != null ? JsonTree.string(auto, "next") : null;
        if (automatic != null) {
            next.add(automatic);
        }
        return next;
    }

    private static Map<String, Object> scenarioStates() throws IOException {
        var root = JsonTree.asObject(JsonTree.parse(scenarioText()));
        var states = root != null ? JsonTree.object(root, "states") : null;
        assertNotNull(states, "the scenario carries no states");
        return states;
    }

    private static String scenarioText() throws IOException {
        try (var in = OnboardingCatalogueTest.class.getResourceAsStream(SCENARIO)) {
            assertNotNull(in, "the scenario " + SCENARIO + " is not on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
