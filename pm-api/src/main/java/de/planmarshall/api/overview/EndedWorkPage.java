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
 * Paged response container for a project's ended work items ({@code GET /api/v1/projects/{projectId}/ended}).
 *
 * @param projectId  unique project identifier
 * @param items      ended plans and epics on this page
 * @param page       current page number (1-based)
 * @param totalPages total number of pages
 * @param hasNewer   whether a newer page exists
 * @param hasOlder   whether an older page exists
 */
public record EndedWorkPage(
String projectId,
List<EndedWorkItem> items,
int page,
int totalPages,
boolean hasNewer,
boolean hasOlder
) {
    public EndedWorkPage {
        Objects.requireNonNull(projectId, "projectId");
        items = items != null ? List.copyOf(items) : List.of();
        page = Math.max(1, page);
        totalPages = Math.max(1, totalPages);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("project_id", projectId);
        map.put("items", items.stream().map(EndedWorkItem::toMap).toList());
        map.put("page", page);
        map.put("total_pages", totalPages);
        map.put("has_newer", hasNewer);
        map.put("has_older", hasOlder);
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
     * @param map map representation
     * @return parsed page
     * @throws IOException on format failure
     */
    public static EndedWorkPage fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, "project_id");
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null) {
            projectId = "unknown";
        }

        var itemsListObj = JsonTree.asArray(map.get("items"));
        var items = new ArrayList<EndedWorkItem>();
        if (itemsListObj != null) {
            for (var elem : itemsListObj) {
                var iMap = JsonTree.asObject(elem);
                if (iMap != null) {
                    items.add(EndedWorkItem.fromMap(iMap));
                }
            }
        }

        var pageNum = JsonTree.number(map, "page");
        int page = pageNum != null ? pageNum.intValue() : 1;

        var totalPagesNum = JsonTree.number(map, "total_pages");
        if (totalPagesNum == null) {
            totalPagesNum = JsonTree.number(map, "pages");
        }
        int totalPages = totalPagesNum != null ? totalPagesNum.intValue() : 1;

        var hasNewer = Boolean.TRUE.equals(map.get("has_newer"))
                || (page > 1);
        var hasOlder = Boolean.TRUE.equals(map.get("has_older"))
                || (page < totalPages);

        return new EndedWorkPage(projectId, items, page, totalPages, hasNewer, hasOlder);
    }

    /**
     * Parses from JSON string (supports either an array of items or a container object).
     *
     * @param json JSON string
     * @return parsed page
     * @throws IOException on format failure
     */
    public static EndedWorkPage fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof List<?> list) {
            var items = new ArrayList<EndedWorkItem>(list.size());
            for (var elem : list) {
                var map = JsonTree.asObject(elem);
                if (map != null) {
                    items.add(EndedWorkItem.fromMap(map));
                }
            }
            return new EndedWorkPage("unknown", items, 1, 1, false, false);
        }
        var obj = JsonTree.asObject(parsed);
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object or array for EndedWorkPage");
        }
        return fromMap(obj);
    }
}
