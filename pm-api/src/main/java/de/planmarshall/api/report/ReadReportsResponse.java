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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Response payload for {@code POST /api/v1/reports/read} and {@code POST /api/v1/reports/{id}/read}.
 *
 * @param markedCount number of reports marked read
 * @param status      status string (e.g. {@code "ok"})
 */
public record ReadReportsResponse(int markedCount, String status) {

    private static final String FIELD_COUNT = "count";
    private static final String FIELD_MARKED_COUNT = "marked_count";
    private static final String FIELD_STATUS = "status";

    public ReadReportsResponse {
        Objects.requireNonNull(status, "status");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_COUNT, markedCount);
        map.put(FIELD_MARKED_COUNT, markedCount);
        map.put(FIELD_STATUS, status);
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
     * Parses from a JSON map.
     *
     * @param map tree map
     * @return parsed response
     */
    public static ReadReportsResponse fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        int count = 0;
        var countObj = map.get(FIELD_MARKED_COUNT);
        if (countObj == null) {
            countObj = map.get(FIELD_COUNT);
        }
        if (countObj instanceof JsonNumber jn) {
            count = (int) jn.longValue();
        } else if (countObj instanceof Number n) {
            count = n.intValue();
        }

        var status = JsonTree.string(map, FIELD_STATUS);
        if (status == null) {
            status = "ok";
        }
        return new ReadReportsResponse(count, status);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed response
     * @throws IOException on error
     */
    public static ReadReportsResponse fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ReadReportsResponse");
        }
        return fromMap(obj);
    }
}
