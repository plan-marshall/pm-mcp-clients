/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.report;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonTree;

/**
 * Record of a re-planning event during plan execution.
 *
 * @param trigger what caused the re-planning (e.g. {@code "the base moved"})
 * @param time    timestamp string (e.g. {@code "09:55"})
 * @param change  summary of changes made (e.g. {@code "TASK-5 added"})
 */
public record ReplanningRecord(String trigger, String time, String change) {

    private static final String FIELD_TRIGGER = "trigger";
    private static final String FIELD_TIME = "time";
    private static final String FIELD_CHANGE = "change";

    public ReplanningRecord {
        Objects.requireNonNull(trigger, FIELD_TRIGGER);
        Objects.requireNonNull(time, FIELD_TIME);
        Objects.requireNonNull(change, FIELD_CHANGE);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_TRIGGER, trigger);
        map.put(FIELD_TIME, time);
        map.put(FIELD_CHANGE, change);
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed replanning record
     */
    public static ReplanningRecord fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var trigger = JsonTree.string(map, FIELD_TRIGGER);
        if (trigger == null) trigger = "";
        var time = JsonTree.string(map, FIELD_TIME);
        if (time == null) time = "";
        var change = JsonTree.string(map, FIELD_CHANGE);
        if (change == null) change = "";
        return new ReplanningRecord(trigger, time, change);
    }
}
