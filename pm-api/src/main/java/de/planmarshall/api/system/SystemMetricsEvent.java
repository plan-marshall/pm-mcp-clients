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
 * Event payload emitted when system metrics or worker states change ({@code system.metrics}).
 */
public record SystemMetricsEvent(
        Map<String, Object> metrics
) {
    public static final String EVENT_NAME = "system.metrics";

    public SystemMetricsEvent {
        metrics = metrics != null ? Map.copyOf(metrics) : Map.of();
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("metrics", metrics);
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
    public static SystemMetricsEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        if (map.get("metrics") instanceof Map<?, ?> m) {
            var mm = new LinkedHashMap<String, Object>();
            for (var entry : m.entrySet()) {
                mm.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return new SystemMetricsEvent(mm);
        }
        return new SystemMetricsEvent(map);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static SystemMetricsEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for SystemMetricsEvent");
        }
        return fromMap(obj);
    }
}
