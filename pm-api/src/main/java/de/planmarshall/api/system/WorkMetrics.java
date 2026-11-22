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
import org.jspecify.annotations.Nullable;

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

    private static final String TASKS_RUNNING = "tasks_running";
    private static final String TASKS_QUEUED = "tasks_queued";
    private static final String ACTIVE_PROJECTS_COUNT = "active_projects_count";

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plans_by_state", plansByState);
        map.put("epics_by_state", epicsByState);
        map.put(TASKS_RUNNING, tasksRunning);
        map.put(TASKS_QUEUED, tasksQueued);
        map.put(ACTIVE_PROJECTS_COUNT, activeProjectsCount);
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
     */
    public static WorkMetrics fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var plansByState = readCounts(map.get("plans_by_state"));
        var epicsByState = readCounts(map.get("epics_by_state"));
        int tasksRunning = Objects.requireNonNullElse(JsonTree.integer(map, TASKS_RUNNING), 0);
        int tasksQueued = Objects.requireNonNullElse(JsonTree.integer(map, TASKS_QUEUED), 0);
        int activeProjectsCount = Objects.requireNonNullElse(JsonTree.integer(map, ACTIVE_PROJECTS_COUNT), 0);
        var recoveringProjects = new ArrayList<String>();
        if (map.get("recovering_projects") instanceof List<?> list) {
            for (var item : list) {
                if (item != null) recoveringProjects.add(String.valueOf(item));
            }
        }
        return new WorkMetrics(plansByState, epicsByState, tasksRunning, tasksQueued, activeProjectsCount, recoveringProjects);
    }

    /** Reads a count per state; an entry whose value is no readable integer is skipped. */
    private static Map<String, Integer> readCounts(Object raw) {
        var counts = new LinkedHashMap<String, Integer>();
        if (raw instanceof Map<?, ?> m) {
            for (var entry : m.entrySet()) {
                var count = countOf(entry.getValue());
                if (count != null) {
                    counts.put(String.valueOf(entry.getKey()), count);
                }
            }
        }
        return counts;
    }

    private static @Nullable Integer countOf(Object value) {
        try {
            return switch (value) {
                case Number n -> n.intValue();
                case JsonNumber jn -> (int) jn.longValue();
                case String s -> Integer.parseInt(s);
                case null, default -> null;
            };
        } catch (NumberFormatException _) {
            return null;
        }
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
