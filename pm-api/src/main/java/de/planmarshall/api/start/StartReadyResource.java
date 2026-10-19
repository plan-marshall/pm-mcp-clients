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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * What a start's ready step shows (State Catalogue, {@code start.ready} and
 * {@code start-epic.ready}; design <em>Starting Work</em>, Ready): what the work starts from, the
 * title proposed from the sources, the derived id, and, for an epic, the values in force from the
 * project, shown and never asked.
 * <p>
 * The plan id of a recipe's plan and of a plan from a pull request is derived and not changeable
 * ({@link #idEditable()} is {@code false}). A recipe passes the ready step too, with its recipe and
 * its parameters, and asks no landing. An epic's ready step shows the project's
 * {@code epic.auto_start} as the value in force with its origin, beside the
 * {@code parallelization_scope} and {@code overlap_max_files} in force; none of them is ever asked at
 * the start.
 *
 * @param title                the title proposed from the sources, or {@code null}
 * @param planId               the id proposed for the plan or the epic
 * @param idEditable           whether the operator may change the id
 * @param idFromTitle          whether the id is the one derived from the title
 * @param recipe               the recipe a plan runs, or {@code null}
 * @param parameters           the recipe's resolved parameters, empty for a plan from sources
 * @param landing              the project's landing in force, or {@code null} where the entry does not show it
 * @param landingAsked         whether the start asks the landing (always {@code false})
 * @param epicAutoStart        the project's {@code epic.auto_start} in force, or {@code null} for a plan
 * @param epicAutoStartOrigin  where the value comes from, or {@code null} for a plan
 * @param parallelizationScope the epic's {@code parallelization_scope} in force, or {@code null}
 * @param overlapMaxFiles      the epic's {@code overlap_max_files} in force, or {@code null}
 */
public record StartReadyResource(
        @Nullable String title,
        String planId,
        boolean idEditable,
        boolean idFromTitle,
        @Nullable RecipeRef recipe,
        List<RecipeParameter> parameters,
        @Nullable String landing,
        boolean landingAsked,
        @Nullable Boolean epicAutoStart,
        @Nullable ConfigOrigin epicAutoStartOrigin,
        @Nullable Integer parallelizationScope,
        @Nullable Integer overlapMaxFiles
) {

    public static final String FIELD_TITLE = "title";
    public static final String FIELD_PLAN_ID = "plan_id";
    public static final String FIELD_ID_EDITABLE = "id_editable";
    public static final String FIELD_ID_FROM_TITLE = "id_from_title";
    public static final String FIELD_RECIPE = "recipe";
    public static final String FIELD_PARAMETERS = "parameters";
    public static final String FIELD_LANDING = "landing";
    public static final String FIELD_LANDING_ASKED = "landing_asked";
    public static final String FIELD_EPIC_AUTO_START = "epic_auto_start";
    public static final String FIELD_EPIC_AUTO_START_ORIGIN = "epic_auto_start_origin";
    public static final String FIELD_PARALLELIZATION_SCOPE = "parallelization_scope";
    public static final String FIELD_OVERLAP_MAX_FILES = "overlap_max_files";

    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_TITLE, FIELD_PLAN_ID, FIELD_ID_EDITABLE, FIELD_ID_FROM_TITLE, FIELD_RECIPE, FIELD_PARAMETERS,
            FIELD_LANDING, FIELD_LANDING_ASKED, FIELD_EPIC_AUTO_START, FIELD_EPIC_AUTO_START_ORIGIN,
            FIELD_PARALLELIZATION_SCOPE, FIELD_OVERLAP_MAX_FILES);

    public StartReadyResource {
        Objects.requireNonNull(planId, FIELD_PLAN_ID);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        if (landingAsked) {
            throw new IllegalArgumentException("The start never asks the landing");
        }
        if (recipe != null && !recipe.parameters().isEmpty() && parameters.isEmpty()) {
            throw new IllegalArgumentException("A recipe's ready step shows its resolved parameters");
        }
        if (epicAutoStart != null && epicAutoStartOrigin == null) {
            throw new IllegalArgumentException("The epic's automatic mode is shown with its origin");
        }
        if (parallelizationScope != null && parallelizationScope < 1) {
            throw new IllegalArgumentException("parallelization_scope is at least 1: " + parallelizationScope);
        }
        if (overlapMaxFiles != null && overlapMaxFiles < 0) {
            throw new IllegalArgumentException("overlap_max_files cannot be negative: " + overlapMaxFiles);
        }
    }

    /**
     * @param title   the proposed title
     * @param planId  the derived id
     * @param recipe  the recipe, or {@code null}
     * @return the ready step of a plan from sources or of a recipe
     */
    public static StartReadyResource ofPlan(@Nullable String title, String planId, @Nullable RecipeRef recipe) {
        return new StartReadyResource(title, planId, recipe == null, recipe == null, recipe,
                recipe != null ? recipe.parameters() : List.of(), null, false, null, null, null, null);
    }

    /**
     * @param title    the proposed title
     * @param epicId   the derived epic id
     * @param autoStart the project's {@code epic.auto_start} in force
     * @param origin   where the value comes from
     * @param scope    the {@code parallelization_scope} in force
     * @param overlap  the {@code overlap_max_files} in force
     * @return the ready step of an epic
     */
    public static StartReadyResource ofEpic(@Nullable String title, String epicId, boolean autoStart,
                                            ConfigOrigin origin, int scope, int overlap) {
        return new StartReadyResource(title, epicId, true, true, null, List.of(), null, false, autoStart,
                origin, scope, overlap);
    }

    /**
     * @return {@code true} if the entry runs a recipe
     */
    public boolean recipeBound() {
        return recipe != null;
    }

    /**
     * @return {@code true} if any parameter of the recipe has no option, so the recipe cannot run
     */
    public boolean recipeBlocked() {
        return parameters.stream().anyMatch(parameter -> !parameter.available());
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (title != null) {
            map.put(FIELD_TITLE, title);
        }
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_ID_EDITABLE, idEditable);
        map.put(FIELD_ID_FROM_TITLE, idFromTitle);
        if (recipe != null) {
            map.put(FIELD_RECIPE, recipe.toMap());
        }
        map.put(FIELD_PARAMETERS, parameters.stream().map(RecipeParameter::toMap).toList());
        if (landing != null) {
            map.put(FIELD_LANDING, landing);
        }
        map.put(FIELD_LANDING_ASKED, landingAsked);
        if (epicAutoStart != null) {
            map.put(FIELD_EPIC_AUTO_START, epicAutoStart);
        }
        if (epicAutoStartOrigin != null) {
            map.put(FIELD_EPIC_AUTO_START_ORIGIN, epicAutoStartOrigin.wire());
        }
        if (parallelizationScope != null) {
            map.put(FIELD_PARALLELIZATION_SCOPE, parallelizationScope);
        }
        if (overlapMaxFiles != null) {
            map.put(FIELD_OVERLAP_MAX_FILES, overlapMaxFiles);
        }
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this ready step to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed ready step
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static StartReadyResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "StartReadyResource");
        var planId = JsonTree.string(map, FIELD_PLAN_ID);
        if (planId == null) {
            throw new JsonFormatException("Missing required field for StartReadyResource: plan_id");
        }
        var recipeMap = JsonTree.objectOrNull(map, FIELD_RECIPE, "StartReadyResource");
        var parameters = new ArrayList<RecipeParameter>();
        for (var object : StartJson.readObjects(map, FIELD_PARAMETERS)) {
            parameters.add(RecipeParameter.fromMap(object));
        }
        var originWire = JsonTree.string(map, FIELD_EPIC_AUTO_START_ORIGIN);
        var origin = originWire != null ? ConfigOrigin.fromWireOrNull(originWire) : null;
        if (originWire != null && origin == null) {
            throw new JsonFormatException("Unknown " + FIELD_EPIC_AUTO_START_ORIGIN + " in StartReadyResource: "
                    + originWire);
        }
        try {
            return new StartReadyResource(JsonTree.string(map, FIELD_TITLE), planId,
                    StartJson.bool(map, FIELD_ID_EDITABLE), StartJson.bool(map, FIELD_ID_FROM_TITLE),
                    recipeMap != null ? RecipeRef.fromMap(recipeMap) : null, parameters,
                    JsonTree.string(map, FIELD_LANDING), StartJson.bool(map, FIELD_LANDING_ASKED),
                    JsonTree.bool(map, FIELD_EPIC_AUTO_START), origin,
                    StartJson.optInteger(map, FIELD_PARALLELIZATION_SCOPE),
                    StartJson.optInteger(map, FIELD_OVERLAP_MAX_FILES));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid StartReadyResource: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed ready step
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static StartReadyResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for StartReadyResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one ready step from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed ready step
     * @throws IOException on format failure
     */
    public static StartReadyResource readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for StartReadyResource");
        }
        return fromMap(map);
    }
}
