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
import de.planmarshall.api.json.JsonTree;

/**
 * Immutable snapshot of the entire system state, metrics, workers, and cleanup summary.
 */
public record SystemSnapshot(
        RuntimeMetrics runtime,
        WorkMetrics work,
        List<WorkerMetric> workers,
        Map<String, Object> jobs,
        List<ModelMetric> models,
        CleanupSummary cleanup,
        SystemWindow window,
        @Nullable String project
) {
    public SystemSnapshot {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(work, "work");
        workers = workers != null ? List.copyOf(workers) : List.of();
        jobs = jobs != null ? Collections.unmodifiableMap(new LinkedHashMap<>(jobs)) : Map.of();
        models = models != null ? List.copyOf(models) : List.of();
        Objects.requireNonNull(cleanup, "cleanup");
        window = window != null ? window : SystemWindow.HOUR;
    }

    private static final String KEY_RUNTIME = "runtime";
    private static final String KEY_CLEANUP = "cleanup";

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(KEY_RUNTIME, runtime.toMap());
        map.put("work", work.toMap());
        map.put("workers", workers.stream().map(WorkerMetric::toMap).toList());
        map.put("jobs", jobs);
        map.put("models", models.stream().map(ModelMetric::toMap).toList());
        map.put(KEY_CLEANUP, cleanup.toMap());
        map.put("window", window.value());
        if (project != null) {
            map.put("project", project);
        }
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

    private static List<WorkerMetric> parseWorkers(Object obj) throws IOException {
        var workers = new ArrayList<WorkerMetric>();
        if (obj instanceof List<?> list) {
            for (var item : list) {
                var wMap = JsonTree.asObject(item);
                if (wMap != null) {
                    workers.add(WorkerMetric.fromMap(wMap));
                }
            }
        }
        return workers;
    }

    private static List<ModelMetric> parseModels(Object obj) throws IOException {
        var models = new ArrayList<ModelMetric>();
        if (obj instanceof List<?> list) {
            for (var item : list) {
                var mMap = JsonTree.asObject(item);
                if (mMap != null) {
                    models.add(ModelMetric.fromMap(mMap));
                }
            }
        }
        return models;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed system snapshot
     * @throws IOException on error
     */
    public static SystemSnapshot fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var runtimeObj = JsonTree.asObject(map.get(KEY_RUNTIME));
        if (runtimeObj == null) {
            throw new JsonFormatException("Missing runtime in SystemSnapshot");
        }
        var runtime = RuntimeMetrics.fromMap(runtimeObj);

        var workObj = JsonTree.asObject(map.get("work"));
        if (workObj == null) {
            throw new JsonFormatException("Missing work in SystemSnapshot");
        }
        var work = WorkMetrics.fromMap(workObj);

        var workers = parseWorkers(map.get("workers"));

        var jobs = new LinkedHashMap<String, Object>();
        if (map.get("jobs") instanceof Map<?, ?> m) {
            for (var entry : m.entrySet()) {
                jobs.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        var models = parseModels(map.get("models"));

        var cleanupObj = JsonTree.asObject(map.get(KEY_CLEANUP));
        if (cleanupObj == null) {
            throw new JsonFormatException("Missing cleanup in SystemSnapshot");
        }
        var cleanup = CleanupSummary.fromMap(cleanupObj);

        var windowStr = JsonTree.string(map, "window");
        var window = SystemWindow.fromValue(windowStr);
        var project = JsonTree.string(map, "project");

        return new SystemSnapshot(runtime, work, workers, jobs, models, cleanup, window, project);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed system snapshot
     * @throws IOException on error
     */
    public static SystemSnapshot fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for SystemSnapshot");
        }
        return fromMap(obj);
    }
}
