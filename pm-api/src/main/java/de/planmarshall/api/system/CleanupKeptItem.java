/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Representation of an item excluded from deletion and kept for good.
 */
public record CleanupKeptItem(
        String id,
        String kind,
        String title,
        String how,
        String keptAt,
        String who
) {
    public CleanupKeptItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(title, "title");
        how = how != null ? how : "ignore";
        keptAt = keptAt != null ? keptAt : "";
        who = who != null ? who : "";
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("kind", kind);
        map.put("title", title);
        map.put("how", how);
        map.put("kept_at", keptAt);
        map.put("who", who);
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
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed kept item
     * @throws IOException on error
     */
    public static CleanupKeptItem fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, "id");
        var kind = JsonTree.string(map, "kind");
        var title = JsonTree.string(map, "title");
        if (id == null || kind == null || title == null) {
            throw new JsonFormatException("Missing required fields for CleanupKeptItem");
        }
        var how = JsonTree.string(map, "how");
        var keptAt = JsonTree.string(map, "kept_at");
        var who = JsonTree.string(map, "who");
        return new CleanupKeptItem(id, kind, title, how, keptAt, who);
    }

    /**
     * Parses a list of kept items from JSON string.
     *
     * @param json JSON array string
     * @return list of kept items
     * @throws IOException on error
     */
    public static List<CleanupKeptItem> parseList(String json) throws IOException {
        var list = JsonTree.asArray(JsonTree.parse(json));
        if (list == null) return List.of();
        var result = new ArrayList<CleanupKeptItem>();
        for (var item : list) {
            var obj = JsonTree.asObject(item);
            if (obj != null) {
                result.add(fromMap(obj));
            }
        }
        return List.copyOf(result);
    }
}
