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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a new cleanup job opens ({@code cleanup.job_opened}).
 */
public record CleanupJobOpenedEvent(
        CleanupJob job
) {
    public static final String EVENT_NAME = "cleanup.job_opened";

    public CleanupJobOpenedEvent {
        Objects.requireNonNull(job, "job");
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("job", job.toMap());
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
    public static CleanupJobOpenedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        if (map.get("job") instanceof Map<?, ?> jm) {
            var m = JsonTree.asObject(jm);
            if (m != null) {
                return new CleanupJobOpenedEvent(CleanupJob.fromMap(m));
            }
        }
        if (map.containsKey("job_id") || map.containsKey("id")) {
            return new CleanupJobOpenedEvent(CleanupJob.fromMap(map));
        }
        throw new JsonFormatException("Missing job in CleanupJobOpenedEvent");
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static CleanupJobOpenedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for CleanupJobOpenedEvent");
        }
        return fromMap(obj);
    }
}
