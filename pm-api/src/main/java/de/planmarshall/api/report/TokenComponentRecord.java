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

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonNumber;

/**
 * Raw token components reported by harnesses.
 *
 * @param inputTokens      input tokens, or null if not reported
 * @param cacheReadTokens  cache read tokens, or null if not reported
 * @param cacheWriteTokens cache write tokens, or null if not reported
 * @param outputTokens     output tokens, or null if not reported
 * @param reasoningTokens  reasoning tokens, or null if not reported
 */
public record TokenComponentRecord(
        @Nullable Long inputTokens,
        @Nullable Long cacheReadTokens,
        @Nullable Long cacheWriteTokens,
        @Nullable Long outputTokens,
        @Nullable Long reasoningTokens
) {
    public long input() {
        return inputTokens != null ? inputTokens : 0L;
    }

    public long cacheRead() {
        return cacheReadTokens != null ? cacheReadTokens : 0L;
    }

    public long cacheWrite() {
        return cacheWriteTokens != null ? cacheWriteTokens : 0L;
    }

    public long output() {
        return outputTokens != null ? outputTokens : 0L;
    }

    public long reasoning() {
        return reasoningTokens != null ? reasoningTokens : 0L;
    }

    private static final String FIELD_INPUT = "input";
    private static final String FIELD_CACHE_READ = "cache_read";
    private static final String FIELD_CACHE_WRITE = "cache_write";
    private static final String FIELD_OUTPUT = "output";
    private static final String FIELD_REASONING = "reasoning";

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (inputTokens != null) map.put(FIELD_INPUT, inputTokens);
        if (cacheReadTokens != null) map.put(FIELD_CACHE_READ, cacheReadTokens);
        if (cacheWriteTokens != null) map.put(FIELD_CACHE_WRITE, cacheWriteTokens);
        if (outputTokens != null) map.put(FIELD_OUTPUT, outputTokens);
        if (reasoningTokens != null) map.put(FIELD_REASONING, reasoningTokens);
        return map;
    }

    /**
     * Parses from map.
     *
     * @param map tree map
     * @return parsed component record
     */
    public static TokenComponentRecord fromMap(Map<String, Object> map) {
        Long in = toLong(map.get(FIELD_INPUT));
        Long cr = toLong(map.get(FIELD_CACHE_READ));
        Long cw = toLong(map.get(FIELD_CACHE_WRITE));
        Long out = toLong(map.get(FIELD_OUTPUT));
        Long rea = toLong(map.get(FIELD_REASONING));
        return new TokenComponentRecord(in, cr, cw, out, rea);
    }

    private static Long toLong(Object obj) {
        if (obj instanceof JsonNumber jn) {
            return jn.longValue();
        }
        if (obj instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
