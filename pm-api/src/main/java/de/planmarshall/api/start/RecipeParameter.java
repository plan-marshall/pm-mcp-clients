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

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One parameter of a recipe, with the options the server resolved and its proposal ({@code
 * recipe.parameters}, {@code start.recipe.proposed}).
 *
 * @param name      the parameter's name
 * @param value     the proposed value, or {@code null} when the parameter has no option
 * @param options   the options the server resolved
 * @param proposed  whether the value is the server's proposal
 * @param available whether the recipe can run with this parameter
 * @param reason    why the parameter has no option, or {@code null}
 */
public record RecipeParameter(
        String name,
        @Nullable String value,
        List<String> options,
        boolean proposed,
        boolean available,
        @Nullable String reason
) {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_OPTIONS = "options";
    private static final String FIELD_PROPOSED = "proposed";
    private static final String FIELD_AVAILABLE = "available";
    private static final String FIELD_REASON = "reason";

    public RecipeParameter {
        Objects.requireNonNull(name, "name");
        options = options == null ? List.of() : List.copyOf(options);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_NAME, name);
        if (value != null) {
            map.put(FIELD_VALUE, value);
        }
        map.put(FIELD_OPTIONS, options);
        map.put(FIELD_PROPOSED, proposed);
        map.put(FIELD_AVAILABLE, available);
        if (reason != null) {
            map.put(FIELD_REASON, reason);
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
     * @return the parsed parameter
     * @throws IOException if the name is missing
     */
    public static RecipeParameter fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var name = JsonTree.string(map, FIELD_NAME);
        if (name == null) {
            throw new JsonFormatException("Missing required field for RecipeParameter: name");
        }
        var options = new ArrayList<String>();
        var raw = JsonTree.asArray(map.get(FIELD_OPTIONS));
        if (raw != null) {
            for (var item : raw) {
                if (item != null) {
                    options.add(String.valueOf(item));
                }
            }
        }
        return new RecipeParameter(name, JsonTree.string(map, FIELD_VALUE), options,
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_PROPOSED)),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_AVAILABLE)), JsonTree.string(map, FIELD_REASON));
    }

    /**
     * @param json the JSON object string
     * @return the parsed parameter
     * @throws IOException if the value is not an object or the name is missing
     */
    public static RecipeParameter fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RecipeParameter");
        }
        return fromMap(obj);
    }
}
