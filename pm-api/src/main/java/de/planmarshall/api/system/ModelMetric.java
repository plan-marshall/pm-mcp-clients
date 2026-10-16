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
 * Metric representation of model and token usage within a given time window.
 */
public record ModelMetric(
        String harnessAndModel,
        @Nullable String role,
        int callsCount,
        long tokensTotal,
        long tokensIn,
        long tokensOut,
        int errorsCount,
        int retriesCount,
        boolean isFloor,
        int unmeasuredCalls
) {
    public ModelMetric {
        Objects.requireNonNull(harnessAndModel, "harnessAndModel");
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("harness_and_model", harnessAndModel);
        if (role != null) {
            map.put("role", role);
        }
        map.put("calls_count", callsCount);
        map.put("tokens_total", tokensTotal);
        map.put("tokens_in", tokensIn);
        map.put("tokens_out", tokensOut);
        map.put("errors_count", errorsCount);
        map.put("retries_count", retriesCount);
        map.put("is_floor", isFloor);
        map.put("unmeasured_calls", unmeasuredCalls);
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
     * @return parsed metric
     * @throws IOException on error
     */
    public static ModelMetric fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var harnessAndModel = JsonTree.string(map, "harness_and_model");
        if (harnessAndModel == null) {
            harnessAndModel = JsonTree.string(map, "model");
        }
        if (harnessAndModel == null) {
            throw new JsonFormatException("Missing harness_and_model in ModelMetric");
        }
        var role = JsonTree.string(map, "role");
        var callsCount = JsonTree.integer(map, "calls_count") != null ? (int) JsonTree.integer(map, "calls_count") : 0;
        var tokensTotal = JsonTree.number(map, "tokens_total") != null ? JsonTree.number(map, "tokens_total").longValue() : 0L;
        var tokensIn = JsonTree.number(map, "tokens_in") != null ? JsonTree.number(map, "tokens_in").longValue() : 0L;
        var tokensOut = JsonTree.number(map, "tokens_out") != null ? JsonTree.number(map, "tokens_out").longValue() : 0L;
        var errorsCount = JsonTree.integer(map, "errors_count") != null ? (int) JsonTree.integer(map, "errors_count") : 0;
        var retriesCount = JsonTree.integer(map, "retries_count") != null ? (int) JsonTree.integer(map, "retries_count") : 0;
        var isFloor = Boolean.TRUE.equals(map.get("is_floor")) || Boolean.TRUE.equals(map.get("floor"));
        var unmeasuredCalls = JsonTree.integer(map, "unmeasured_calls") != null
                ? JsonTree.integer(map, "unmeasured_calls")
                : 0;

        return new ModelMetric(harnessAndModel, role, callsCount, tokensTotal, tokensIn, tokensOut, errorsCount, retriesCount, isFloor, unmeasuredCalls);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed metric
     * @throws IOException on error
     */
    public static ModelMetric fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ModelMetric");
        }
        return fromMap(obj);
    }
}
