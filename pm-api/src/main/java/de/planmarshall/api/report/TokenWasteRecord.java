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
 * Waste entry categorizing unmeasured or wasted tokens.
 *
 * @param wasteClass   waste classification ({@code "no_product"}, {@code "retries"}, {@code "remediation"})
 * @param tokens       token amount
 * @param sharePercent share of total or measured tokens
 * @param tasksCount   number of tasks that contributed to this waste
 * @param causeSummary summary of causes (e.g. {@code "retryable 1 · stall 1"})
 */
public record TokenWasteRecord(
        String wasteClass,
        long tokens,
        int sharePercent,
        int tasksCount,
        @Nullable String causeSummary
) {
    private static final String FIELD_WASTE_CLASS = "waste_class";
    private static final String FIELD_TOKENS = "tokens";
    private static final String FIELD_SHARE_PERCENT = "share_percent";
    private static final String FIELD_TASKS_COUNT = "tasks_count";
    private static final String FIELD_CAUSE_SUMMARY = "cause_summary";

    public TokenWasteRecord {
        Objects.requireNonNull(wasteClass, "wasteClass");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_WASTE_CLASS, wasteClass);
        map.put(FIELD_TOKENS, tokens);
        map.put(FIELD_SHARE_PERCENT, sharePercent);
        map.put(FIELD_TASKS_COUNT, tasksCount);
        if (causeSummary != null) {
            map.put(FIELD_CAUSE_SUMMARY, causeSummary);
        }
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed waste record
     */
    public static TokenWasteRecord fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var wasteClass = JsonTree.string(map, FIELD_WASTE_CLASS);
        if (wasteClass == null) wasteClass = "unknown";
        long tokens = getLong(map.get(FIELD_TOKENS));
        int share = (int) getLong(map.get(FIELD_SHARE_PERCENT));
        int tasks = (int) getLong(map.get(FIELD_TASKS_COUNT));
        var causes = JsonTree.string(map, FIELD_CAUSE_SUMMARY);
        return new TokenWasteRecord(wasteClass, tokens, share, tasks, causes);
    }

    private static long getLong(Object obj) {
        if (obj instanceof JsonNumber jn) return jn.longValue();
        if (obj instanceof Number n) return n.longValue();
        return 0L;
    }
}
