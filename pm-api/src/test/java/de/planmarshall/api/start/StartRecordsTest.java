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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;

@DisplayName("Start record contracts")
@EnableGeneratorController
class StartRecordsTest {

    @Nested
    @DisplayName("SourceKind")
    class SourceKindTest {

        @Test
        @DisplayName("is the closed list of the six source kinds and resolves its wire values")
        void closedListAndWireValues() {
            var wireValues = Stream.of(SourceKind.values())
                    .map(SourceKind::wireValue)
                    .collect(Collectors.toCollection(TreeSet::new));

            assertEquals(Set.of("description", "documents", "issue", "lesson", "local_changes",
                    "pull_request"), wireValues);
            assertEquals(SourceKind.DOCUMENTS, SourceKind.fromWire("documents"));
            assertEquals(SourceKind.LOCAL_CHANGES, SourceKind.fromWire("LOCAL_CHANGES"));
        }

        @Test
        @DisplayName("refuses a value that names no source kind")
        void refusesUnknownValue() {
            assertThrows(IllegalArgumentException.class, () -> SourceKind.fromWire("bogus"));
            assertThrows(IllegalArgumentException.class, () -> SourceKind.fromWire(null));
        }
    }

    @Test
    @DisplayName("SourceInput round-trips and omits an unresolved title")
    void sourceInputRoundTrips() throws Exception {
        var title = Generators.nonEmptyStrings().next();
        var full = new SourceInput(SourceKind.ISSUE, "https://codeberg.org/forgejo/forgejo/issues/4711",
                title);
        assertEquals(full, SourceInput.fromJson(full.toJson()));

        var unresolved = new SourceInput(SourceKind.DOCUMENTS, "doc/concepts/forgejo.adoc", null);
        assertFalse(unresolved.toMap().containsKey("title"));
        assertNull(SourceInput.fromJson(unresolved.toJson()).title());
    }

    @Test
    @DisplayName("StartPlanRequest round-trips with its recipe parameters and documents")
    void startPlanRequestRoundTrips() throws Exception {
        var objective = Generators.nonEmptyStrings().next();
        var title = Generators.nonEmptyStrings().next();
        var request = new StartPlanRequest("pm-mcp-clients", objective, "code-review", Map.of("depth", "deep"),
                List.of("doc/concepts/forgejo.adoc"), "4711", title);

        var roundTrip = StartPlanRequest.fromJson(request.toJson());

        assertEquals(request, roundTrip);
    }

    @Test
    @DisplayName("StartPlanRequest defaults its collections and omits absent fields")
    void startPlanRequestDefaults() throws Exception {
        var minimal = new StartPlanRequest("pm-mcp-clients", null, null, null, null, null, null);

        assertTrue(minimal.parameters().isEmpty());
        assertTrue(minimal.documents().isEmpty());
        var map = minimal.toMap();
        assertFalse(map.containsKey("objective"));
        assertFalse(map.containsKey("recipe_id"));
        assertFalse(map.containsKey("issue"));

        assertEquals(minimal, StartPlanRequest.fromJson(minimal.toJson()));
    }

    @Test
    @DisplayName("StartEpicRequest round-trips with its auto-start flag")
    void startEpicRequestRoundTrips() throws Exception {
        var objective = Generators.nonEmptyStrings().next();
        var request = new StartEpicRequest("pm-mcp-clients", objective, Boolean.FALSE, objective);

        var roundTrip = StartEpicRequest.fromJson(request.toJson());

        assertEquals(request, roundTrip);
        assertEquals(Boolean.FALSE, roundTrip.toMap().get("auto_start"));
    }

    @Test
    @DisplayName("PlanStartResult round-trips and defaults the state to started")
    void planStartResultRoundTrips() throws Exception {
        var title = Generators.nonEmptyStrings().next();
        var result = new PlanStartResult("codeberg-provider-issues", PlanStartResult.KIND_PLAN, title, null,
                "/api/v1/plans/codeberg-provider-issues");

        var roundTrip = PlanStartResult.fromJson(result.toJson());

        assertEquals(result.id(), roundTrip.id());
        assertEquals(PlanStartResult.STATE_STARTED, roundTrip.state());
        assertEquals("/api/v1/plans/codeberg-provider-issues", roundTrip.followHref());
    }

    @Test
    @DisplayName("RecipeParameter round-trips with its resolved options and reason")
    void recipeParameterRoundTrips() throws Exception {
        var parameter = new RecipeParameter("modules", "all 9", List.of("all 9", "pm-api"), true, true,
                null);
        assertEquals(parameter, RecipeParameter.fromJson(parameter.toJson()));

        var reason = Generators.nonEmptyStrings().next();
        var blocked = new RecipeParameter("modules", null, List.of(), false, false, reason);
        assertEquals(blocked, RecipeParameter.fromJson(blocked.toJson()));
    }

    @Test
    @DisplayName("RecipeRef round-trips with its parameters and takes-request flag")
    void recipeRefRoundTrips() throws Exception {
        var description = Generators.nonEmptyStrings().next();
        var recipe = new RecipeRef("audit", "audit", description, "project", true,
                List.of(new RecipeParameter("modules", "all 9", List.of("all 9"), true, true, null)));

        var roundTrip = RecipeRef.fromJson(recipe.toJson());

        assertEquals(recipe, roundTrip);
        assertTrue(roundTrip.takesRequest());
    }

    @Test
    @DisplayName("IntakeDraft round-trips with its sources and creation time")
    void intakeDraftRoundTrips() throws Exception {
        var objective = Generators.nonEmptyStrings().next();
        var createdAt = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var draft = new IntakeDraft("draft-1", "pm-mcp-clients", objective,
                List.of(new SourceInput(SourceKind.ISSUE, "4711", null)), createdAt);

        var roundTrip = IntakeDraft.fromJson(draft.toJson());

        assertEquals(draft, roundTrip);
    }

    @Nested
    @DisplayName("Negative parsing")
    class NegativeParsing {

        @Test
        @DisplayName("refuses a JSON value that is no object")
        void refusesNonObjectJson() {
            assertThrows(JsonFormatException.class, () -> SourceInput.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> StartPlanRequest.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> StartEpicRequest.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PlanStartResult.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> RecipeParameter.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> RecipeRef.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> IntakeDraft.fromJson("[]"));
        }

        @Test
        @DisplayName("refuses an object without the required fields")
        void refusesMissingFields() {
            assertThrows(JsonFormatException.class, () -> SourceInput.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> StartPlanRequest.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> StartEpicRequest.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PlanStartResult.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> RecipeParameter.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> RecipeRef.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> IntakeDraft.fromJson("{}"));
        }
    }
}
