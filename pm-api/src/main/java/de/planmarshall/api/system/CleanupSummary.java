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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Summary representation of due cleanup items, space freed, and sweep timestamps.
 */
public record CleanupSummary(
        int plansCount,
        int epicsCount,
        int logsCount,
        int reportsCount,
        int projectsCount,
        long freedBytes,
        int keptForGoodCount,
        @Nullable String lastSweepAt,
        @Nullable String nextCheckAt
) {
    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plans_count", plansCount);
        map.put("epics_count", epicsCount);
        map.put("logs_count", logsCount);
        map.put("reports_count", reportsCount);
        map.put("projects_count", projectsCount);
        map.put("freed_bytes", freedBytes);
        map.put("kept_for_good_count", keptForGoodCount);
        if (lastSweepAt != null) {
            map.put("last_sweep_at", lastSweepAt);
        }
        if (nextCheckAt != null) {
            map.put("next_check_at", nextCheckAt);
        }
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
     * @return parsed cleanup summary
     * @throws IOException on error
     */
    public static CleanupSummary fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var plansCount = JsonTree.integer(map, "plans_count") != null ? (int) JsonTree.integer(map, "plans_count") : 0;
        var epicsCount = JsonTree.integer(map, "epics_count") != null ? (int) JsonTree.integer(map, "epics_count") : 0;
        var logsCount = JsonTree.integer(map, "logs_count") != null ? (int) JsonTree.integer(map, "logs_count") : 0;
        var reportsCount = JsonTree.integer(map, "reports_count") != null ? (int) JsonTree.integer(map, "reports_count") : 0;
        var projectsCount = JsonTree.integer(map, "projects_count") != null ? (int) JsonTree.integer(map, "projects_count") : 0;
        var freedBytes = JsonTree.number(map, "freed_bytes") != null ? JsonTree.number(map, "freed_bytes").longValue() : 0L;
        var keptForGoodCount = JsonTree.integer(map, "kept_for_good_count") != null ? (int) JsonTree.integer(map, "kept_for_good_count") : 0;
        var lastSweepAt = JsonTree.string(map, "last_sweep_at");
        var nextCheckAt = JsonTree.string(map, "next_check_at");

        return new CleanupSummary(plansCount, epicsCount, logsCount, reportsCount, projectsCount, freedBytes, keptForGoodCount, lastSweepAt, nextCheckAt);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed cleanup summary
     * @throws IOException on error
     */
    public static CleanupSummary fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for CleanupSummary");
        }
        return fromMap(obj);
    }
}
