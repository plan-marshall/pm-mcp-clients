/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The ready step of a start: what a plan's and a recipe's ready step offers, the invariants the record
 * holds, and the wire shapes its codec refuses.
 */
@DisplayName("StartReadyResource")
@EnableGeneratorController
class StartReadyResourceTest {

    private static RecipeParameter parameter() {
        var value = Generators.nonBlankStrings().next();
        return new RecipeParameter(Generators.nonBlankStrings().next(), value, List.of(value), true, true, null);
    }

    private static RecipeRef recipe(List<RecipeParameter> parameters) {
        return new RecipeRef(Generators.nonBlankStrings().next(), Generators.nonBlankStrings().next(), null, null,
                false, parameters);
    }

    @Nested
    @DisplayName("ofPlan")
    class OfPlan {

        @Test
        @DisplayName("should let the operator change an id derived from the title of a plan from sources")
        void shouldOfferAnEditableIdWithoutRecipe() {
            var planId = Generators.nonBlankStrings().next();

            var ready = StartReadyResource.ofPlan(null, planId, null);

            assertAll(
                    () -> assertTrue(ready.idEditable()),
                    () -> assertTrue(ready.idFromTitle()),
                    () -> assertFalse(ready.recipeBound()),
                    () -> assertTrue(ready.parameters().isEmpty()),
                    () -> assertFalse(ready.toMap().containsKey(StartReadyResource.FIELD_TITLE)),
                    () -> assertNull(ready.landing()),
                    () -> assertNull(ready.epicAutoStart()));
        }

        @Test
        @DisplayName("should fix the id of a recipe's plan and show the recipe's resolved parameters")
        void shouldFixTheIdOfARecipePlan() {
            var parameters = List.of(parameter(), parameter());
            var recipe = recipe(parameters);
            var title = Generators.nonBlankStrings().next();

            var ready = StartReadyResource.ofPlan(title, Generators.nonBlankStrings().next(), recipe);

            assertAll(
                    () -> assertFalse(ready.idEditable()),
                    () -> assertFalse(ready.idFromTitle()),
                    () -> assertTrue(ready.recipeBound()),
                    () -> assertFalse(ready.recipeBlocked()),
                    () -> assertEquals(parameters, ready.parameters()),
                    () -> assertEquals(ready, StartReadyResource.fromJson(ready.toJson())));
        }
    }

    @Test
    @DisplayName("should replace absent parameters of a constructed ready step by an empty list")
    void shouldReplaceAbsentParameters() {
        var ready = new StartReadyResource(null, Generators.nonBlankStrings().next(), true, true, null, null, null,
                false, null, null, null, null);

        assertEquals(List.of(), ready.parameters());
    }

    static Stream<Arguments> brokenInvariants() {
        var withParameters = recipe(List.of(parameter()));
        return Stream.of(
                Arguments.of("a recipe's ready step without its parameters", withParameters, null,
                        Generators.integers(0, 50).next()),
                Arguments.of("a parallelization_scope below 1", null, Generators.integers(-50, 0).next(),
                        Generators.integers(0, 50).next()),
                Arguments.of("a negative overlap_max_files", null, Generators.integers(1, 50).next(),
                        Generators.integers(-50, -1).next()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenInvariants")
    @DisplayName("should refuse a ready step that breaks an invariant")
    void shouldRefuseABrokenInvariant(String violation, RecipeRef recipe, Integer scope, Integer overlap) {
        var planId = Generators.nonBlankStrings().next();

        assertThrows(IllegalArgumentException.class, () -> new StartReadyResource(null, planId, false, false, recipe,
                List.of(), null, false, null, null, scope, overlap), violation);
    }

    static Stream<Arguments> malformedMembers() {
        var planId = Generators.nonBlankStrings().next();
        return Stream.of(
                Arguments.of("no plan_id", Map.of(StartReadyResource.FIELD_TITLE, planId)),
                Arguments.of("an unknown origin", Map.of(StartReadyResource.FIELD_PLAN_ID, planId,
                        StartReadyResource.FIELD_EPIC_AUTO_START_ORIGIN, "not-a-level")),
                Arguments.of("a landing asked", Map.of(StartReadyResource.FIELD_PLAN_ID, planId,
                        StartReadyResource.FIELD_LANDING_ASKED, true)),
                Arguments.of("parameters that are no array", Map.of(StartReadyResource.FIELD_PLAN_ID, planId,
                        StartReadyResource.FIELD_PARAMETERS, planId)),
                Arguments.of("a parameter that is no object", Map.of(StartReadyResource.FIELD_PLAN_ID, planId,
                        StartReadyResource.FIELD_PARAMETERS, List.of(planId))),
                Arguments.of("a parameter without a name", Map.of(StartReadyResource.FIELD_PLAN_ID, planId,
                        StartReadyResource.FIELD_PARAMETERS, List.of(Map.of("value", planId)))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedMembers")
    @DisplayName("should refuse a ready step whose members are malformed")
    void shouldRefuseMalformedMembers(String violation, Map<String, Object> members) {
        var map = new LinkedHashMap<String, Object>(members);

        assertThrows(JsonFormatException.class, () -> StartReadyResource.fromMap(map), violation);
    }

    @Test
    @DisplayName("should read the epic's values in force with their origin")
    void shouldReadTheEpicValuesInForce() throws Exception {
        var origin = Generators.enumValues(ConfigOrigin.class).next();
        var ready = StartReadyResource.ofEpic(null, Generators.nonBlankStrings().next(),
                Generators.booleans().next(), origin, Generators.integers(1, 9).next(),
                Generators.integers(0, 9).next());

        var parsed = StartReadyResource.fromMap(JsonTree.asObject(JsonTree.parse(ready.toJson())));

        assertAll(
                () -> assertEquals(ready, parsed),
                () -> assertEquals(origin, parsed.epicAutoStartOrigin()));
    }

    @Test
    @DisplayName("should refuse a streamed value that is no object")
    void shouldRefuseAStreamedValueThatIsNoObject() throws Exception {
        var text = JsonTree.write(List.of(Generators.nonBlankStrings().next()));

        try (var parser = new JsonFactory().createParser(text)) {
            assertThrows(JsonFormatException.class, () -> StartReadyResource.readFrom(parser));
        }
    }
}
