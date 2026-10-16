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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Retention duration settings governing when ended work items become due for cleanup.
 */
public record CleanupDurations(
        int plansAfterDays,
        int epicsAfterDays,
        int logsAfterDays,
        int reportsAfterDays,
        int unenrolledAfterDays,
        int remindAfterDays
) {
    public static final int DEFAULT_DAYS = 30;
    public static final int DEFAULT_REMIND_DAYS = 3;
    public static final int MINIMUM_RETENTION_DAYS = 7;
    public static final int MINIMUM_REMIND_DAYS = 1;

    public CleanupDurations {
        // -1 represents "never"
    }

    /**
     * Default system cleanup durations.
     *
     * @return default durations
     */
    public static CleanupDurations defaults() {
        return new CleanupDurations(DEFAULT_DAYS, DEFAULT_DAYS, DEFAULT_DAYS, DEFAULT_DAYS, DEFAULT_DAYS, DEFAULT_REMIND_DAYS);
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plans_after_days", plansAfterDays);
        map.put("epics_after_days", epicsAfterDays);
        map.put("logs_after_days", logsAfterDays);
        map.put("reports_after_days", reportsAfterDays);
        map.put("unenrolled_after_days", unenrolledAfterDays);
        map.put("remind_after_days", remindAfterDays);
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
     * @return parsed durations
     * @throws IOException on error
     */
    public static CleanupDurations fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var plansVal = JsonTree.integer(map, "plans_after_days");
        var epicsVal = JsonTree.integer(map, "epics_after_days");
        var logsVal = JsonTree.integer(map, "logs_after_days");
        var reportsVal = JsonTree.integer(map, "reports_after_days");
        var unenrolledVal = JsonTree.integer(map, "unenrolled_after_days");
        var remindVal = JsonTree.integer(map, "remind_after_days");

        return new CleanupDurations(
                plansVal != null ? plansVal.intValue() : DEFAULT_DAYS,
                epicsVal != null ? epicsVal.intValue() : DEFAULT_DAYS,
                logsVal != null ? logsVal.intValue() : DEFAULT_DAYS,
                reportsVal != null ? reportsVal.intValue() : DEFAULT_DAYS,
                unenrolledVal != null ? unenrolledVal.intValue() : DEFAULT_DAYS,
                remindVal != null ? remindVal.intValue() : DEFAULT_REMIND_DAYS
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed durations
     * @throws IOException on error
     */
    public static CleanupDurations fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for CleanupDurations");
        }
        return fromMap(obj);
    }
}
