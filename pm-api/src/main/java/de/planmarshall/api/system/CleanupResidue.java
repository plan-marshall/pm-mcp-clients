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
 * Metric representation of technical residue cleaned up automatically by the runtime sweep.
 */
public record CleanupResidue(
        String step,
        int count,
        long bytes,
        @Nullable String lastSweep
) {
    public CleanupResidue {
        Objects.requireNonNull(step, "step");
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("step", step);
        map.put("count", count);
        map.put("bytes", bytes);
        if (lastSweep != null) {
            map.put("last_sweep", lastSweep);
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
     * @return parsed residue metric
     * @throws IOException on error
     */
    public static CleanupResidue fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var step = JsonTree.string(map, "step");
        if (step == null) {
            throw new JsonFormatException("Missing step in CleanupResidue");
        }
        var count = JsonTree.integer(map, "count") != null ? (int) JsonTree.integer(map, "count") : 0;
        var bytes = JsonTree.number(map, "bytes") != null ? JsonTree.number(map, "bytes").longValue() : 0L;
        var lastSweep = JsonTree.string(map, "last_sweep");
        return new CleanupResidue(step, count, bytes, lastSweep);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed residue
     * @throws IOException on error
     */
    public static CleanupResidue fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for CleanupResidue");
        }
        return fromMap(obj);
    }
}
