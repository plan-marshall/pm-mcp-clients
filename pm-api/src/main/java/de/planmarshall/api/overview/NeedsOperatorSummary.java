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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The {@code needs_operator} summary the runtime derives for a plan, an epic, a project, or the
 * machine (PM-WF-23): whether anything needs the operator, how many items, and per item its kind
 * and a link to it.
 *
 * @param count how many items wait
 * @param items the items, each with its kind and its link
 */
public record NeedsOperatorSummary(int count, List<NeedsOperatorItemRef> items) {

    public NeedsOperatorSummary {
        items = items != null ? List.copyOf(items) : List.of();
        if (count < 0) {
            throw new IllegalArgumentException("A count of what waits is never negative");
        }
    }

    /**
     * @return an empty summary
     */
    public static NeedsOperatorSummary empty() {
        return new NeedsOperatorSummary(0, List.of());
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("count", count);
        map.put("items", items.stream().map(NeedsOperatorItemRef::toMap).toList());
        return map;
    }

    /**
     * @return the JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a summary.
     *
     * @param map the map representation
     * @return the parsed summary
     * @throws IOException on format failure
     */
    public static NeedsOperatorSummary fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var raw = JsonTree.asArray(map.get("items"));
        var items = new ArrayList<NeedsOperatorItemRef>();
        if (raw != null) {
            for (var element : raw) {
                var item = JsonTree.asObject(element);
                if (item == null) {
                    throw new JsonFormatException("Expected an item object in a needs_operator summary");
                }
                items.add(NeedsOperatorItemRef.fromMap(item));
            }
        }
        var count = JsonTree.integer(map, "count");
        return new NeedsOperatorSummary(count != null ? count : items.size(), items);
    }

    /**
     * Parses a summary from a JSON object.
     *
     * @param json the JSON object
     * @return the parsed summary
     * @throws IOException on format failure
     */
    public static NeedsOperatorSummary fromJson(String json) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(json));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for NeedsOperatorSummary");
        }
        return fromMap(map);
    }
}
