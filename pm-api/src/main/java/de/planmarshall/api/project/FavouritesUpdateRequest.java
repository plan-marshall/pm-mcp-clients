/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Request payload to update favourite projects ordering (UC-PRJ-2).
 *
 * @param projectIds ordered list of project IDs
 */
public record FavouritesUpdateRequest(List<String> projectIds) {

    public static final String FIELD_PROJECT_IDS = "project_ids";

    public FavouritesUpdateRequest {
        projectIds = projectIds != null ? List.copyOf(projectIds) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_IDS, new ArrayList<>(projectIds));
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON tree map.
     *
     * @param map input map
     * @return parsed request
     * @throws IOException on format failure
     */
    public static FavouritesUpdateRequest fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var list = new ArrayList<String>();
        var array = JsonTree.asArray(map.get(FIELD_PROJECT_IDS));
        if (array == null) {
            array = JsonTree.asArray(map.get("favourites"));
        }
        if (array != null) {
            for (var item : array) {
                if (item instanceof String s) {
                    list.add(s);
                }
            }
        }
        return new FavouritesUpdateRequest(list);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed request
     * @throws IOException on format failure
     */
    public static FavouritesUpdateRequest fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for FavouritesUpdateRequest");
    }
}
