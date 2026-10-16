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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Single breakdown row for token usage (by phase, by role, or by model).
 *
 * @param label                identifier or name of the bucket
 * @param tokens               total token count
 * @param inputTokens          input/cache tokens
 * @param outputTokens         output/reasoning tokens
 * @param unmeasuredTasksCount count of unmeasured tasks in this bucket
 * @param isFloor              true if token total is a floor (≥)
 * @param details              optional role pin details (e.g. {@code "claude:opus-5.5 · high"})
 * @param components           optional raw token components
 */
public record TokenBreakdownItem(
        String label,
        long tokens,
        long inputTokens,
        long outputTokens,
        int unmeasuredTasksCount,
        boolean isFloor,
        @Nullable String details,
        @Nullable TokenComponentRecord components
) {
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_TOKENS = "tokens";
    private static final String FIELD_INPUT_TOKENS = "input_tokens";
    private static final String FIELD_OUTPUT_TOKENS = "output_tokens";
    private static final String FIELD_UNMEASURED_TASKS = "unmeasured_tasks";
    private static final String FIELD_IS_FLOOR = "is_floor";
    private static final String FIELD_DETAILS = "details";
    private static final String FIELD_COMPONENTS = "components";

    public TokenBreakdownItem {
        Objects.requireNonNull(label, FIELD_LABEL);
    }

    public String name() {
        return label;
    }

    public long total() {
        return tokens;
    }

    public long input() {
        return inputTokens;
    }

    public long output() {
        return outputTokens;
    }

    public int unmeasured() {
        return unmeasuredTasksCount;
    }

    public boolean floor() {
        return isFloor;
    }

    public @Nullable String harnessModel() {
        return details;
    }

    public @Nullable String effort() {
        return null;
    }


    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LABEL, label);
        map.put(FIELD_TOKENS, tokens);
        map.put(FIELD_INPUT_TOKENS, inputTokens);
        map.put(FIELD_OUTPUT_TOKENS, outputTokens);
        if (unmeasuredTasksCount > 0) {
            map.put(FIELD_UNMEASURED_TASKS, unmeasuredTasksCount);
        }
        if (isFloor) {
            map.put(FIELD_IS_FLOOR, true);
        }
        if (details != null) {
            map.put(FIELD_DETAILS, details);
        }
        if (components != null) {
            map.put(FIELD_COMPONENTS, components.toMap());
        }
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed item
     */
    public static TokenBreakdownItem fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var label = JsonTree.string(map, FIELD_LABEL);
        if (label == null) {
            label = JsonTree.string(map, "name");
        }
        if (label == null) label = "";
        long tokens = getLong(map.get(FIELD_TOKENS));
        if (tokens == 0L) {
            tokens = getLong(map.get("total"));
        }
        long input = getLong(map.get(FIELD_INPUT_TOKENS));
        if (input == 0L) {
            input = getLong(map.get("input"));
        }
        long output = getLong(map.get(FIELD_OUTPUT_TOKENS));
        if (output == 0L) {
            output = getLong(map.get("output"));
        }
        int unmeasured = (int) getLong(map.get(FIELD_UNMEASURED_TASKS));
        if (unmeasured == 0) {
            unmeasured = (int) getLong(map.get("unmeasured"));
        }
        boolean isFloor = Boolean.TRUE.equals(map.get(FIELD_IS_FLOOR)) || Boolean.TRUE.equals(map.get("floor"));
        var details = JsonTree.string(map, FIELD_DETAILS);
        if (details == null) {
            details = JsonTree.string(map, "harness_model");
        }


        TokenComponentRecord components = null;
        var compObj = map.get(FIELD_COMPONENTS);
        if (compObj instanceof Map<?, ?> cm) {
            var m = new LinkedHashMap<String, Object>();
            for (var e : cm.entrySet()) {
                m.put(String.valueOf(e.getKey()), e.getValue());
            }
            components = TokenComponentRecord.fromMap(m);
        }

        return new TokenBreakdownItem(label, tokens, input, output, unmeasured, isFloor, details, components);
    }

    private static long getLong(Object obj) {
        if (obj instanceof JsonNumber jn) return jn.longValue();
        if (obj instanceof Number n) return n.longValue();
        return 0L;
    }
}
