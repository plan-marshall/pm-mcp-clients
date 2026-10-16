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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Immutable metrics representation of running work, tasks, and projects.
 */
public record WorkMetrics(
        Map<String, Integer> plansByState,
        Map<String, Integer> epicsByState,
        int tasksRunning,
        int tasksQueued,
        int activeProjectsCount,
        List<String> recoveringProjects
) {
    public WorkMetrics {
        plansByState = plansByState != null ? Collections.unmodifiableMap(new LinkedHashMap<>(plansByState)) : Map.of();
        epicsByState = epicsByState != null ? Collections.unmodifiableMap(new LinkedHashMap<>(epicsByState)) : Map.of();
        recoveringProjects = recoveringProjects != null ? List.copyOf(recoveringProjects) : List.of();
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plans_by_state", plansByState);
        map.put("epics_by_state", epicsByState);
        map.put("tasks_running", tasksRunning);
        map.put("tasks_queued", tasksQueued);
        map.put("active_projects_count", activeProjectsCount);
        map.put("recovering_projects", recoveringProjects);
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
     * @return parsed work metrics
     * @throws IOException on error
     */
    public static WorkMetrics fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var plansByState = new LinkedHashMap<String, Integer>();
        if (map.get("plans_by_state") instanceof Map<?, ?> m) {
            for (var entry : m.entrySet()) {
                var v = entry.getValue();
                if (v instanceof Number n) {
                    plansByState.put(String.valueOf(entry.getKey()), n.intValue());
                } else if (v instanceof JsonNumber jn) {
                    try {
                        plansByState.put(String.valueOf(entry.getKey()), (int) jn.longValue());
                    } catch (NumberFormatException _) {
                        // ignore
                    }
                } else if (v instanceof String s) {
                    try {
                        plansByState.put(String.valueOf(entry.getKey()), Integer.parseInt(s));
                    } catch (NumberFormatException _) {
                        // ignore
                    }
                }
            }
        }
        var epicsByState = new LinkedHashMap<String, Integer>();
        if (map.get("epics_by_state") instanceof Map<?, ?> m) {
            for (var entry : m.entrySet()) {
                var v = entry.getValue();
                if (v instanceof Number n) {
                    epicsByState.put(String.valueOf(entry.getKey()), n.intValue());
                } else if (v instanceof JsonNumber jn) {
                    try {
                        epicsByState.put(String.valueOf(entry.getKey()), (int) jn.longValue());
                    } catch (NumberFormatException _) {
                        // ignore
                    }
                } else if (v instanceof String s) {
                    try {
                        epicsByState.put(String.valueOf(entry.getKey()), Integer.parseInt(s));
                    } catch (NumberFormatException _) {
                        // ignore
                    }
                }
            }
        }
        var tasksRunning = JsonTree.integer(map, "tasks_running") != null ? (int) JsonTree.integer(map, "tasks_running") : 0;
        var tasksQueued = JsonTree.integer(map, "tasks_queued") != null ? (int) JsonTree.integer(map, "tasks_queued") : 0;
        var activeProjectsCount = JsonTree.integer(map, "active_projects_count") != null ? (int) JsonTree.integer(map, "active_projects_count") : 0;
        var recoveringProjects = new ArrayList<String>();
        if (map.get("recovering_projects") instanceof List<?> list) {
            for (var item : list) {
                if (item != null) recoveringProjects.add(String.valueOf(item));
            }
        }
        return new WorkMetrics(plansByState, epicsByState, tasksRunning, tasksQueued, activeProjectsCount, recoveringProjects);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed work metrics
     * @throws IOException on error
     */
    public static WorkMetrics fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for WorkMetrics");
        }
        return fromMap(obj);
    }
}
