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
 * One match of finding work by name (UC-PLN-8).
 *
 * @param kind    the closed kind of the match
 * @param id      the identifier of the matched project, epic, plan, decision, or report
 * @param title   its title, shown as plain text
 * @param project the project it belongs to, or {@code null}
 * @param epic    the epic it belongs to, or {@code null}
 * @param plan    the plan it belongs to, or {@code null}
 * @param ended   whether it is ended work
 * @param outcome the outcome of ended work, or {@code null}
 */
public record FindMatch(
        FindKind kind,
        String id,
        String title,
        @Nullable WorkLocation project,
        @Nullable WorkLocation epic,
        @Nullable WorkLocation plan,
        boolean ended,
        @Nullable String outcome
) {

    public FindMatch {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        title = title == null ? id : title;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("kind", kind.value());
        map.put("id", id);
        map.put("title", title);
        if (project != null) {
            map.put("project", project.toMap());
        }
        if (epic != null) {
            map.put("epic", epic.toMap());
        }
        if (plan != null) {
            map.put("plan", plan.toMap());
        }
        map.put("ended", ended);
        if (outcome != null) {
            map.put("outcome", outcome);
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
     * Parses one match.
     *
     * @param map the map representation
     * @return the parsed match
     * @throws IOException on format failure
     */
    public static FindMatch fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kind = FindKind.fromValue(JsonTree.string(map, "kind"));
        if (kind == null) {
            throw new JsonFormatException("Unknown kind in FindMatch: " + JsonTree.string(map, "kind"));
        }
        var id = JsonTree.string(map, "id");
        if (id == null) {
            throw new JsonFormatException("Missing id in FindMatch");
        }
        return new FindMatch(
                kind,
                id,
                JsonTree.string(map, "title"),
                WorkLocation.fromValue(map.get("project")),
                WorkLocation.fromValue(map.get("epic")),
                WorkLocation.fromValue(map.get("plan")),
                Boolean.TRUE.equals(map.get("ended")),
                JsonTree.string(map, "outcome"));
    }

    /**
     * Parses one match from a JSON object.
     *
     * @param json the JSON object
     * @return the parsed match
     * @throws IOException on format failure
     */
    public static FindMatch fromJson(String json) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(json));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for FindMatch");
        }
        return fromMap(map);
    }
}
