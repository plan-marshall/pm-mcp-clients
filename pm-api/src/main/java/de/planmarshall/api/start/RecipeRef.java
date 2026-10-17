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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A recipe offered in a project, with its parameters ({@code recipe.choose}, {@code start.recipe}).
 *
 * @param key          the recipe's key
 * @param name         the display name
 * @param description  what the recipe does in one line
 * @param origin       where the recipe comes from ({@code domain}, {@code machine} or {@code project})
 * @param takesRequest whether the recipe takes a request (its sources are asked too)
 * @param parameters   the parameters the server resolved
 */
public record RecipeRef(
        String key,
        String name,
        String description,
        String origin,
        boolean takesRequest,
        List<RecipeParameter> parameters
) {

    private static final String FIELD_KEY = "key";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_ORIGIN = "origin";
    private static final String FIELD_TAKES_REQUEST = "takes_request";
    private static final String FIELD_PARAMETERS = "parameters";

    public RecipeRef {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(name, "name");
        description = description == null ? "" : description;
        origin = origin == null || origin.isBlank() ? "machine" : origin;
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KEY, key);
        map.put(FIELD_NAME, name);
        map.put(FIELD_DESCRIPTION, description);
        map.put(FIELD_ORIGIN, origin);
        map.put(FIELD_TAKES_REQUEST, takesRequest);
        map.put(FIELD_PARAMETERS, parameters.stream().map(RecipeParameter::toMap).toList());
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
     * @return the parsed recipe
     * @throws IOException if a required field is missing
     */
    public static RecipeRef fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var key = JsonTree.string(map, FIELD_KEY);
        var name = JsonTree.string(map, FIELD_NAME);
        if (key == null || name == null) {
            throw new JsonFormatException("Missing required fields for RecipeRef: key or name");
        }
        var parameters = new ArrayList<RecipeParameter>();
        var rawParams = JsonTree.asArray(map.get(FIELD_PARAMETERS));
        if (rawParams != null) {
            for (var item : rawParams) {
                var obj = JsonTree.asObject(item);
                if (obj != null) {
                    parameters.add(RecipeParameter.fromMap(obj));
                }
            }
        }
        return new RecipeRef(key, name, JsonTree.string(map, FIELD_DESCRIPTION),
                JsonTree.string(map, FIELD_ORIGIN),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_TAKES_REQUEST)), parameters);
    }

    /**
     * @param json the JSON object string
     * @return the parsed recipe
     * @throws IOException if the value is not an object or a field is missing
     */
    public static RecipeRef fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RecipeRef");
        }
        return fromMap(obj);
    }
}
