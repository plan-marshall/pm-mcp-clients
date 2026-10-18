/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Result of a project unenrolment ({@code POST /api/v1/projects/{project_id}/unenrol}, PM-EXEC-2).
 *
 * @param status the unenrolment outcome, {@code unenrolled}
 */
public record UnenrolResult(
        String status
) {
    public static final String FIELD_STATUS = "status";

    /** The status of a completed unenrolment. */
    public static final String STATUS_UNENROLLED = "unenrolled";

    public UnenrolResult {
        Objects.requireNonNull(status, FIELD_STATUS);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STATUS, status);
        return map;
    }

    /**
     * Serializes to a JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map input map
     * @return parsed result
     */
    public static UnenrolResult fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var status = JsonTree.string(map, FIELD_STATUS);
        if (status == null || status.isBlank()) {
            status = STATUS_UNENROLLED;
        }
        return new UnenrolResult(status);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed result
     * @throws IOException on format failure
     */
    public static UnenrolResult fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for UnenrolResult");
    }
}
