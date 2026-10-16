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
 * Group of cleanup items belonging to a specific project (or unenrolled projects).
 */
public record CleanupProjectItem(
        String projectId,
        int plansCount,
        int epicsCount,
        int logsCount,
        int readReportsCount,
        long totalBytes,
        List<CleanupItemEntry> items
) {
    public CleanupProjectItem {
        Objects.requireNonNull(projectId, "projectId");
        items = items != null ? List.copyOf(items) : List.of();
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("project_id", projectId);
        map.put("plans_count", plansCount);
        map.put("epics_count", epicsCount);
        map.put("logs_count", logsCount);
        map.put("read_reports_count", readReportsCount);
        map.put("total_bytes", totalBytes);
        map.put("items", items.stream().map(CleanupItemEntry::toMap).toList());
        return map;
    }

    /**
     * Serializes to JSON.
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
     * @return parsed project item
     * @throws IOException on error
     */
    public static CleanupProjectItem fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, "project_id");
        if (projectId == null) {
            throw new JsonFormatException("Missing project_id in CleanupProjectItem");
        }
        var plansCount = JsonTree.integer(map, "plans_count") != null ? (int) JsonTree.integer(map, "plans_count") : 0;
        var epicsCount = JsonTree.integer(map, "epics_count") != null ? (int) JsonTree.integer(map, "epics_count") : 0;
        var logsCount = JsonTree.integer(map, "logs_count") != null ? (int) JsonTree.integer(map, "logs_count") : 0;
        var readReportsCount = JsonTree.integer(map, "read_reports_count") != null ? (int) JsonTree.integer(map, "read_reports_count") : 0;
        var totalBytes = JsonTree.number(map, "total_bytes") != null ? JsonTree.number(map, "total_bytes").longValue() : 0L;
        var items = new ArrayList<CleanupItemEntry>();
        if (map.get("items") instanceof List<?> list) {
            for (var obj : list) {
                var itemMap = JsonTree.asObject(obj);
                if (itemMap != null) {
                    items.add(CleanupItemEntry.fromMap(itemMap));
                }
            }
        }
        return new CleanupProjectItem(projectId, plansCount, epicsCount, logsCount, readReportsCount, totalBytes, items);
    }
}
