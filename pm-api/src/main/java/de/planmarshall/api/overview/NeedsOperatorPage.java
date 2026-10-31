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
 * One page of the one list of everything that waits ({@code GET /api/v1/machine/needs-operator}).
 * <p>
 * The items are in the runtime's order: the items that block running work first, then the oldest.
 *
 * @param page       the page number, 1-based
 * @param totalPages the number of pages
 * @param hasNewer   whether a newer page exists
 * @param hasOlder   whether an older page exists
 * @param total      the number of items of every page
 * @param items      the items of this page, in the runtime's order
 */
public record NeedsOperatorPage(
        int page,
        int totalPages,
        boolean hasNewer,
        boolean hasOlder,
        int total,
        List<NeedsOperatorItem> items
) {

    public NeedsOperatorPage {
        items = items != null ? List.copyOf(items) : List.of();
        page = Math.max(1, page);
        totalPages = Math.max(1, totalPages);
        total = Math.max(0, total);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("page", page);
        map.put("total_pages", totalPages);
        map.put("has_newer", hasNewer);
        map.put("has_older", hasOlder);
        map.put("total", total);
        map.put("items", items.stream().map(NeedsOperatorItem::toMap).toList());
        return map;
    }

    /**
     * @return the JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a page.
     * <p>
     * A page that names no {@code total} takes the rows it carries as its total: a runtime that omits
     * the figure never makes a client invent a larger one.
     *
     * @param map the map representation
     * @return the parsed page
     * @throws IOException on format failure
     */
    public static NeedsOperatorPage fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var raw = JsonTree.asArray(map.get("items"));
        var items = new ArrayList<NeedsOperatorItem>();
        if (raw != null) {
            for (var element : raw) {
                var item = JsonTree.asObject(element);
                if (item == null) {
                    throw new JsonFormatException("Expected an item object in the list of what waits");
                }
                items.add(NeedsOperatorItem.fromMap(item));
            }
        }
        return new NeedsOperatorPage(
                intOf(map, "page", 1),
                intOf(map, "total_pages", 1),
                Boolean.TRUE.equals(map.get("has_newer")),
                Boolean.TRUE.equals(map.get("has_older")),
                intOf(map, "total", items.size()),
                items);
    }

    /**
     * Parses a page from a JSON object.
     *
     * @param json the JSON object
     * @return the parsed page
     * @throws IOException on format failure
     */
    public static NeedsOperatorPage fromJson(String json) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(json));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for NeedsOperatorPage");
        }
        return fromMap(map);
    }

    private static int intOf(Map<String, Object> map, String key, int fallback) {
        var value = JsonTree.integer(map, key);
        return value != null ? value : fallback;
    }
}
