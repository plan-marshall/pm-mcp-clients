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
 * Event payload emitted when a cleanup job is decided ({@code cleanup.decided}).
 */
public record CleanupDecidedEvent(
        String jobId,
        String decision,
        String who,
        int deletedCount,
        long freedBytes,
        @Nullable String decidedAt
) {
    public static final String EVENT_NAME = "cleanup.decided";

    private static final String DECISION_KEY = "decision";
    private static final String DELETED_COUNT = "deleted_count";
    private static final String FREED_BYTES = "freed_bytes";

    public CleanupDecidedEvent {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(decision, DECISION_KEY);
        Objects.requireNonNull(who, "who");
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("job_id", jobId);
        map.put(DECISION_KEY, decision);
        map.put("who", who);
        map.put(DELETED_COUNT, deletedCount);
        map.put(FREED_BYTES, freedBytes);
        if (decidedAt != null) {
            map.put("decided_at", decidedAt);
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
     * @return parsed event
     * @throws IOException on error
     */
    public static CleanupDecidedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var jobId = JsonTree.string(map, "job_id");
        if (jobId == null) {
            jobId = JsonTree.string(map, "id");
        }
        var decision = JsonTree.string(map, DECISION_KEY);
        var who = JsonTree.string(map, "who");
        if (jobId == null || decision == null || who == null) {
            throw new JsonFormatException("Missing jobId, decision, or who in CleanupDecidedEvent");
        }
        int deletedCount = Objects.requireNonNullElse(JsonTree.integer(map, DELETED_COUNT), 0);
        var freedNumber = JsonTree.number(map, FREED_BYTES);
        var freedBytes = freedNumber != null ? freedNumber.longValue() : 0L;
        var decidedAt = JsonTree.string(map, "decided_at");
        return new CleanupDecidedEvent(jobId, decision, who, deletedCount, freedBytes, decidedAt);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static CleanupDecidedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for CleanupDecidedEvent");
        }
        return fromMap(obj);
    }
}
