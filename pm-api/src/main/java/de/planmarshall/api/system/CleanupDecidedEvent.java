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

    public CleanupDecidedEvent {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(decision, "decision");
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
        map.put("decision", decision);
        map.put("who", who);
        map.put("deleted_count", deletedCount);
        map.put("freed_bytes", freedBytes);
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
        var decision = JsonTree.string(map, "decision");
        var who = JsonTree.string(map, "who");
        if (jobId == null || decision == null || who == null) {
            throw new JsonFormatException("Missing jobId, decision, or who in CleanupDecidedEvent");
        }
        var deletedCount = JsonTree.integer(map, "deleted_count") != null ? (int) JsonTree.integer(map, "deleted_count") : 0;
        var freedBytes = JsonTree.number(map, "freed_bytes") != null ? JsonTree.number(map, "freed_bytes").longValue() : 0L;
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
