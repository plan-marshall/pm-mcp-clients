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
    private static final String KEY_LOGS_COUNT = "logs_count";
    private static final String KEY_REPORTS_COUNT = "reports_count";
    private static final String KEY_PROJECTS_COUNT = "projects_count";
    private static final String KEY_FREED_BYTES = "freed_bytes";
    private static final String KEY_KEPT_FOR_GOOD_COUNT = "kept_for_good_count";

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plans_count", plansCount);
        map.put("epics_count", epicsCount);
        map.put(KEY_LOGS_COUNT, logsCount);
        map.put(KEY_REPORTS_COUNT, reportsCount);
        map.put(KEY_PROJECTS_COUNT, projectsCount);
        map.put(KEY_FREED_BYTES, freedBytes);
        map.put(KEY_KEPT_FOR_GOOD_COUNT, keptForGoodCount);
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
     */
    public static CleanupSummary fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var plans = JsonTree.integer(map, "plans_count");
        var epics = JsonTree.integer(map, "epics_count");
        var logs = JsonTree.integer(map, KEY_LOGS_COUNT);
        var reports = JsonTree.integer(map, KEY_REPORTS_COUNT);
        var projects = JsonTree.integer(map, KEY_PROJECTS_COUNT);
        var freed = JsonTree.number(map, KEY_FREED_BYTES);
        var kept = JsonTree.integer(map, KEY_KEPT_FOR_GOOD_COUNT);
        var lastSweepAt = JsonTree.string(map, "last_sweep_at");
        var nextCheckAt = JsonTree.string(map, "next_check_at");

        return new CleanupSummary(
                plans != null ? plans.intValue() : 0,
                epics != null ? epics.intValue() : 0,
                logs != null ? logs.intValue() : 0,
                reports != null ? reports.intValue() : 0,
                projects != null ? projects.intValue() : 0,
                freed != null ? freed.longValue() : 0L,
                kept != null ? kept.intValue() : 0,
                lastSweepAt,
                nextCheckAt
        );
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
