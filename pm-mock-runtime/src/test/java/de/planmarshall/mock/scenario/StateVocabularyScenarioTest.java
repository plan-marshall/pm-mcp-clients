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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.mock.StandardScenarios;

/**
 * Guards that every workflow unit the primary scenarios serve has a plain name in the shared
 * display vocabulary, in both locales.
 * <p>
 * This lives here rather than in {@code pm-api} because the served unit ids are a property of the
 * scenarios: a scenario that renames or adds a unit is what invalidates a catalogue key, and only
 * this module can read them. The contract's own ids are guarded in {@code pm-api} by
 * {@code StateWordsCompletenessTest}.
 */
@DisplayName("State vocabulary against the scenarios")
class StateVocabularyScenarioTest {

    private static final List<String> LOCALES = MessageCatalogue.SUPPORTED_LOCALES.stream().sorted().toList();

    static Stream<String> primaryScenarioResources() {
        return StandardScenarios.PRIMARY_SCENARIOS.stream();
    }

    static Stream<String> scenariosServingUnits() {
        return Stream.of(StandardScenarios.DESIGNER, StandardScenarios.FULLY_ONBOARDED);
    }

    @ParameterizedTest
    @MethodSource("primaryScenarioResources")
    @DisplayName("should have a plain name for every workflow unit the scenario serves")
    void shouldHavePlainNameForEveryWorkflowUnitTheScenarioServes(String scenarioName) throws Exception {
        var resourcePath = "/scenarios/" + scenarioName + ".json";
        Set<String> unitIds;
        try (var in = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(in, "Resource must exist: " + resourcePath);
            unitIds = unitIdsOf(ScenarioLoader.load(in, resourcePath));
        }

        // A scenario that serves no workflow unit at all has nothing to guard; the ones that do
        // (the designer and the showroom) are what this keeps in step with the catalogue.

        var missing = new TreeSet<String>();
        for (var locale : LOCALES) {
            for (var id : unitIds) {
                var key = "unit." + id;
                if (!MessageCatalogue.hasKey(locale, key)) {
                    missing.add(locale + ":" + key);
                }
            }
        }

        assertTrue(missing.isEmpty(),
                "Every unit of " + scenarioName + " needs a plain name in every locale: " + missing);
    }

    @ParameterizedTest
    @MethodSource("scenariosServingUnits")
    @DisplayName("should serve workflow units where the catalogue of units is shown")
    void shouldServeWorkflowUnitsWhereTheCatalogueIsShown(String scenarioName) throws Exception {
        var resourcePath = "/scenarios/" + scenarioName + ".json";
        try (var in = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(in, "Resource must exist: " + resourcePath);
            var ids = unitIdsOf(ScenarioLoader.load(in, resourcePath));
            assertFalse(ids.isEmpty(), "Scenario should serve workflow units: " + scenarioName);
        }
    }

    /**
     * Collects the ids of the workflow units a scenario serves: every {@code name} of a served
     * object that carries a {@code frontmatter}, which is what distinguishes a unit from a plan,
     * a report or any other named resource.
     *
     * @param scenario the loaded scenario
     * @return the unit ids, sorted
     */
    private static Set<String> unitIdsOf(Scenario scenario) {
        var ids = new TreeSet<String>();
        scenario.states().values().forEach(state -> collect(state.representation(), ids));
        scenario.routes().values().forEach(route -> collect(route.representation(), ids));
        return ids;
    }

    /**
     * @param representation a hypermedia payload or one of its parts
     * @param ids            the set the unit ids are collected into
     */
    private static void collect(Object representation, Set<String> ids) {
        if (representation instanceof Map<?, ?> map) {
            if (map.get("name") instanceof String name && map.get("frontmatter") instanceof Map<?, ?>) {
                ids.add(name);
            }
            map.values().forEach(value -> collect(value, ids));
        } else if (representation instanceof List<?> list) {
            list.forEach(value -> collect(value, ids));
        }
    }
}
