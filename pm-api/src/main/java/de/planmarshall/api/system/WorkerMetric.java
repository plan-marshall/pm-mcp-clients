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
 * Metric representation of an active or idle worker process on the machine.
 */
public record WorkerMetric(
        String role,
        String harness,
        String model,
        WorkerState state,
        @Nullable String currentPlanTitle
) {
    public WorkerMetric {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(harness, "harness");
        Objects.requireNonNull(model, "model");
        state = state != null ? state : WorkerState.IDLE;
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("role", role);
        map.put("harness", harness);
        map.put("model", model);
        map.put("state", state.value());
        if (currentPlanTitle != null) {
            map.put("current_plan_title", currentPlanTitle);
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
     * @return parsed worker metric
     * @throws IOException on parse failure
     */
    public static WorkerMetric fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var role = JsonTree.string(map, "role");
        var harness = JsonTree.string(map, "harness");
        var model = JsonTree.string(map, "model");
        if (role == null || harness == null || model == null) {
            throw new JsonFormatException("Missing required fields for WorkerMetric: role, harness, or model");
        }
        var stateStr = JsonTree.string(map, "state");
        var state = WorkerState.fromValue(stateStr);
        var planTitle = JsonTree.string(map, "current_plan_title");
        return new WorkerMetric(role, harness, model, state, planTitle);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed worker metric
     * @throws IOException on error
     */
    public static WorkerMetric fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for WorkerMetric");
        }
        return fromMap(obj);
    }
}
