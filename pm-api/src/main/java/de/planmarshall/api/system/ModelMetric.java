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

    private static final String KEY_CALLS_COUNT = "calls_count";
    private static final String KEY_TOKENS_TOTAL = "tokens_total";
    private static final String KEY_TOKENS_IN = "tokens_in";
    private static final String KEY_TOKENS_OUT = "tokens_out";
    private static final String KEY_ERRORS_COUNT = "errors_count";
    private static final String KEY_RETRIES_COUNT = "retries_count";
    private static final String KEY_UNMEASURED_CALLS = "unmeasured_calls";

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
        map.put(KEY_CALLS_COUNT, callsCount);
        map.put(KEY_TOKENS_TOTAL, tokensTotal);
        map.put(KEY_TOKENS_IN, tokensIn);
        map.put(KEY_TOKENS_OUT, tokensOut);
        map.put(KEY_ERRORS_COUNT, errorsCount);
        map.put(KEY_RETRIES_COUNT, retriesCount);
        map.put("is_floor", isFloor);
        map.put(KEY_UNMEASURED_CALLS, unmeasuredCalls);
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
        var callsVal = JsonTree.integer(map, KEY_CALLS_COUNT);
        var totalVal = JsonTree.number(map, KEY_TOKENS_TOTAL);
        var inVal = JsonTree.number(map, KEY_TOKENS_IN);
        var outVal = JsonTree.number(map, KEY_TOKENS_OUT);
        var errVal = JsonTree.integer(map, KEY_ERRORS_COUNT);
        var retVal = JsonTree.integer(map, KEY_RETRIES_COUNT);
        var isFloor = Boolean.TRUE.equals(map.get("is_floor")) || Boolean.TRUE.equals(map.get("floor"));
        var unmeasuredVal = JsonTree.integer(map, KEY_UNMEASURED_CALLS);

        return new ModelMetric(
                harnessAndModel,
                role,
                callsVal != null ? callsVal.intValue() : 0,
                totalVal != null ? totalVal.longValue() : 0L,
                inVal != null ? inVal.longValue() : 0L,
                outVal != null ? outVal.longValue() : 0L,
                errVal != null ? errVal.intValue() : 0,
                retVal != null ? retVal.intValue() : 0,
                isFloor,
                unmeasuredVal != null ? unmeasuredVal.intValue() : 0
        );
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
