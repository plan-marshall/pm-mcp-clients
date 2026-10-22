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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The request that opens a plan's entry under its project (UC-STA-1).
 * <p>
 * The opening request is the only one that names the project; the entry is addressed by its plan
 * id from then on.
 *
 * @param projectId  the enrolled project
 * @param objective  the operator's words for the work, or {@code null}
 * @param recipeId   the recipe the plan runs, or {@code null} for a plan from sources
 * @param parameters the recipe's resolved parameter values (key to value)
 * @param documents  the paths or directories brought in as documents
 * @param issue      the issue reference (a number or a URL), or {@code null}
 * @param title      the proposed title, or {@code null} to derive it from the sources
 */
public record StartPlanRequest(
        String projectId,
        @Nullable String objective,
        @Nullable String recipeId,
        Map<String, String> parameters,
        List<String> documents,
        @Nullable String issue,
        @Nullable String title
) {

    private static final String FIELD_PROJECT_ID = "project_id";
    private static final String FIELD_OBJECTIVE = "objective";
    private static final String FIELD_RECIPE_ID = "recipe_id";
    private static final String FIELD_PARAMETERS = "parameters";
    private static final String FIELD_DOCUMENTS = "documents";
    private static final String FIELD_ISSUE = "issue";
    private static final String FIELD_TITLE = "title";

    public StartPlanRequest {
        Objects.requireNonNull(projectId, "projectId");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        if (objective != null) {
            map.put(FIELD_OBJECTIVE, objective);
        }
        if (recipeId != null) {
            map.put(FIELD_RECIPE_ID, recipeId);
        }
        if (!parameters.isEmpty()) {
            map.put(FIELD_PARAMETERS, parameters);
        }
        if (!documents.isEmpty()) {
            map.put(FIELD_DOCUMENTS, documents);
        }
        if (issue != null) {
            map.put(FIELD_ISSUE, issue);
        }
        if (title != null) {
            map.put(FIELD_TITLE, title);
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
     * @param map the input map
     * @return the parsed request
     * @throws IOException if the project id is missing
     */
    public static StartPlanRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            throw new JsonFormatException("Missing required field for StartPlanRequest: project_id");
        }
        return new StartPlanRequest(projectId, JsonTree.string(map, FIELD_OBJECTIVE),
                JsonTree.string(map, FIELD_RECIPE_ID), readParameters(map), readDocuments(map),
                JsonTree.string(map, FIELD_ISSUE), JsonTree.string(map, FIELD_TITLE));
    }

    /**
     * @param json the JSON object string
     * @return the parsed request
     * @throws IOException if the value is not an object or a field is missing
     */
    public static StartPlanRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for StartPlanRequest");
        }
        return fromMap(obj);
    }

    private static Map<String, String> readParameters(Map<String, Object> map) {
        var raw = JsonTree.object(map, FIELD_PARAMETERS);
        if (raw == null) {
            return Map.of();
        }
        var result = new LinkedHashMap<String, String>();
        for (var entry : raw.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private static List<String> readDocuments(Map<String, Object> map) {
        var raw = JsonTree.asArray(map.get(FIELD_DOCUMENTS));
        if (raw == null) {
            return List.of();
        }
        return raw.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }
}
