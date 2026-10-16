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
 * An individual item or group offered for deletion within a cleanup job.
 */
public record CleanupItemEntry(
        String id,
        String kind,
        String title,
        @Nullable String outcome,
        @Nullable String endedAt,
        long sizeBytes,
        int logsCount,
        int readReportsCount,
        @Nullable String keptUntil
) {
    public CleanupItemEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(title, "title");
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("kind", kind);
        map.put("title", title);
        if (outcome != null) {
            map.put("outcome", outcome);
        }
        if (endedAt != null) {
            map.put("ended_at", endedAt);
        }
        map.put("size_bytes", sizeBytes);
        map.put("logs_count", logsCount);
        map.put("read_reports_count", readReportsCount);
        if (keptUntil != null) {
            map.put("kept_until", keptUntil);
        }
        return map;
    }

    /**
     * Serializes to JSON string.
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
     * @return parsed item entry
     * @throws IOException on error
     */
    public static CleanupItemEntry fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, "id");
        var kind = JsonTree.string(map, "kind");
        var title = JsonTree.string(map, "title");
        if (id == null || kind == null || title == null) {
            throw new JsonFormatException("Missing id, kind, or title in CleanupItemEntry");
        }
        var outcome = JsonTree.string(map, "outcome");
        var endedAt = JsonTree.string(map, "ended_at");
        var sizeBytes = JsonTree.number(map, "size_bytes") != null ? JsonTree.number(map, "size_bytes").longValue() : 0L;
        var logsCount = JsonTree.integer(map, "logs_count") != null ? (int) JsonTree.integer(map, "logs_count") : 0;
        var readReportsCount = JsonTree.integer(map, "read_reports_count") != null ? (int) JsonTree.integer(map, "read_reports_count") : 0;
        var keptUntil = JsonTree.string(map, "kept_until");

        return new CleanupItemEntry(id, kind, title, outcome, endedAt, sizeBytes, logsCount, readReportsCount, keptUntil);
    }
}
