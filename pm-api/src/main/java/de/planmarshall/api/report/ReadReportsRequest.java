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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Request payload for {@code POST /api/v1/reports/read}.
 *
 * @param reportIds explicit list of report identifiers to mark read
 */
public record ReadReportsRequest(List<String> reportIds) {

    private static final String FIELD_REPORT_IDS = "report_ids";

    public ReadReportsRequest {
        reportIds = reportIds != null ? Collections.unmodifiableList(new ArrayList<>(reportIds)) : List.of();
    }

    /**
     * Backward-compatibility constructor for callers previously passing {@code all}.
     *
     * @param reportIds explicit list of report identifiers to mark read
     * @param all ignored; all-form has been removed
     */
    public ReadReportsRequest(List<String> reportIds, boolean all) {
        this(reportIds);
    }

    /**
     * Creates a specific-reports request.
     *
     * @param ids report IDs
     * @return ReadReportsRequest
     */
    public static ReadReportsRequest of(List<String> ids) {
        return new ReadReportsRequest(ids);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (!reportIds.isEmpty()) {
            map.put(FIELD_REPORT_IDS, reportIds);
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
     * Parses from a JSON map.
     *
     * @param map tree map
     * @return parsed request
     */
    public static ReadReportsRequest fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var ids = new ArrayList<String>();
        var idsObj = map.get(FIELD_REPORT_IDS);
        if (idsObj == null) {
            idsObj = map.get("reportIds");
        }
        if (idsObj instanceof List<?> list) {
            for (var item : list) {
                if (item != null) {
                    ids.add(String.valueOf(item));
                }
            }
        }
        return new ReadReportsRequest(ids);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed request
     * @throws IOException on error
     */
    public static ReadReportsRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ReadReportsRequest");
        }
        return fromMap(obj);
    }
}
