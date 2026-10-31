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
 * One page of the matches of finding work by name ({@code GET /api/v1/find?q=}).
 * <p>
 * Running work comes first, ended work after it, marked; within each by kind, in the order project,
 * epic, plan, decision, report.
 *
 * @param page       the page number, 1-based
 * @param totalPages the number of pages
 * @param hasNewer   whether a newer page exists
 * @param hasOlder   whether an older page exists
 * @param query      the text that was searched, repeated so a client can show it
 * @param matches    the matches of this page, in the runtime's order
 */
public record FindPage(
        int page,
        int totalPages,
        boolean hasNewer,
        boolean hasOlder,
        String query,
        List<FindMatch> matches
) {

    public FindPage {
        Objects.requireNonNull(query, "query");
        matches = matches != null ? List.copyOf(matches) : List.of();
        page = Math.max(1, page);
        totalPages = Math.max(1, totalPages);
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
        map.put("query", query);
        map.put("matches", matches.stream().map(FindMatch::toMap).toList());
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
     *
     * @param map the map representation
     * @return the parsed page
     * @throws IOException on format failure
     */
    public static FindPage fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var raw = JsonTree.asArray(map.get("matches"));
        var matches = new ArrayList<FindMatch>();
        if (raw != null) {
            for (var element : raw) {
                var match = JsonTree.asObject(element);
                if (match == null) {
                    throw new JsonFormatException("Expected a match object in the matches of find");
                }
                matches.add(FindMatch.fromMap(match));
            }
        }
        return new FindPage(
                intOf(map, "page", 1),
                intOf(map, "total_pages", 1),
                Boolean.TRUE.equals(map.get("has_newer")),
                Boolean.TRUE.equals(map.get("has_older")),
                JsonTree.string(map, "query") == null ? "" : JsonTree.string(map, "query"),
                matches);
    }

    /**
     * Parses a page from a JSON object.
     *
     * @param json the JSON object
     * @return the parsed page
     * @throws IOException on format failure
     */
    public static FindPage fromJson(String json) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(json));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for FindPage");
        }
        return fromMap(map);
    }

    private static int intOf(Map<String, Object> map, String key, int fallback) {
        var value = JsonTree.integer(map, key);
        return value != null ? value : fallback;
    }
}
