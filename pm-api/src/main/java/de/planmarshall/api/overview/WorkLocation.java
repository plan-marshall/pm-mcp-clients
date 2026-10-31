/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Where an item of the list of what waits, or a match of a search by name, belongs: a project, an
 * epic, or a plan, by its id and its title.
 *
 * @param id    the identifier of the project, the epic, or the plan
 * @param title its title, or {@code null} where the store carries none
 */
public record WorkLocation(String id, @Nullable String title) {

    public WorkLocation {
        Objects.requireNonNull(id, "id");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        if (title != null) {
            map.put("title", title);
        }
        return map;
    }

    /**
     * @return the JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses one location.
     *
     * @param value the parsed JSON value
     * @return the location, or {@code null} when the value is no object with an id
     * @throws IOException on a malformed member
     */
    public static @Nullable WorkLocation fromValue(@Nullable Object value) throws IOException {
        var map = JsonTree.asObject(value);
        if (map == null) {
            return null;
        }
        var id = JsonTree.string(map, "id");
        if (id == null) {
            return null;
        }
        return new WorkLocation(id, JsonTree.string(map, "title"));
    }

    /**
     * Parses one location from a JSON object.
     *
     * @param json the JSON object
     * @return the parsed location
     * @throws IOException on format failure
     */
    public static WorkLocation fromJson(String json) throws IOException {
        var parsed = fromValue(JsonTree.parse(json));
        if (parsed == null) {
            throw new JsonFormatException("Expected a work location with an id");
        }
        return parsed;
    }
}
