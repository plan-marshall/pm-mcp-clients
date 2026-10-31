/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.git;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The provider's rate limit as the observation saw it (UC-STA-7).
 * <p>
 * A rate limit that is in force still lets the pull request be observed, but not adopted; the
 * reset tells the operator when to try again.
 *
 * @param limit     the requests allowed in the window, or {@code 0} when the provider reports none
 * @param remaining the requests left in the window
 * @param resetAt   when the window resets, or {@code null} when the provider reports none
 * @param limited   whether the rate limit is in force
 */
public record RateLimitStatus(
        int limit,
        int remaining,
        @Nullable Instant resetAt,
        boolean limited
) {

    private static final String FIELD_LIMIT = "limit";
    private static final String FIELD_REMAINING = "remaining";
    private static final String FIELD_RESET_AT = "reset_at";
    private static final String FIELD_LIMITED = "limited";

    public RateLimitStatus {
        if (limit < 0) {
            limit = 0;
        }
        if (remaining < 0) {
            remaining = 0;
        }
    }

    /**
     * @return a status of an unconstrained provider
     */
    public static RateLimitStatus unlimited() {
        return new RateLimitStatus(0, 0, null, false);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LIMIT, limit);
        map.put(FIELD_REMAINING, remaining);
        if (resetAt != null) {
            map.put(FIELD_RESET_AT, resetAt.toString());
        }
        map.put(FIELD_LIMITED, limited);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the input map
     * @return the parsed status
     * @throws IOException if the reset instant is not an ISO-8601 timestamp
     */
    public static RateLimitStatus fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var limit = JsonTree.integer(map, FIELD_LIMIT);
        var remaining = JsonTree.integer(map, FIELD_REMAINING);
        var resetStr = JsonTree.string(map, FIELD_RESET_AT);
        Instant resetAt = null;
        if (resetStr != null && !resetStr.isBlank()) {
            try {
                resetAt = Instant.parse(resetStr);
            } catch (DateTimeParseException _) {
                throw new JsonFormatException("Invalid reset_at for RateLimitStatus: " + resetStr);
            }
        }
        return new RateLimitStatus(limit != null ? limit : 0, remaining != null ? remaining : 0,
                resetAt, Boolean.TRUE.equals(JsonTree.bool(map, FIELD_LIMITED)));
    }

    /**
     * @param json the JSON object string
     * @return the parsed status
     * @throws IOException if the value is not an object
     */
    public static RateLimitStatus fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RateLimitStatus");
        }
        return fromMap(obj);
    }
}
