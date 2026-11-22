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

    private static final String PLANS_COUNT = "plans_count";
    private static final String EPICS_COUNT = "epics_count";
    private static final String LOGS_COUNT = "logs_count";
    private static final String READ_REPORTS_COUNT = "read_reports_count";
    private static final String TOTAL_BYTES = "total_bytes";

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("project_id", projectId);
        map.put(PLANS_COUNT, plansCount);
        map.put(EPICS_COUNT, epicsCount);
        map.put(LOGS_COUNT, logsCount);
        map.put(READ_REPORTS_COUNT, readReportsCount);
        map.put(TOTAL_BYTES, totalBytes);
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
        int plansCount = Objects.requireNonNullElse(JsonTree.integer(map, PLANS_COUNT), 0);
        int epicsCount = Objects.requireNonNullElse(JsonTree.integer(map, EPICS_COUNT), 0);
        int logsCount = Objects.requireNonNullElse(JsonTree.integer(map, LOGS_COUNT), 0);
        int readReportsCount = Objects.requireNonNullElse(JsonTree.integer(map, READ_REPORTS_COUNT), 0);
        var totalNumber = JsonTree.number(map, TOTAL_BYTES);
        var totalBytes = totalNumber != null ? totalNumber.longValue() : 0L;
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
