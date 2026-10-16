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
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a new report is generated and sent to clients ({@code report.sent}).
 *
 * @param report the report item
 */
public record ReportSentEvent(ReportItem report) {

    public static final String EVENT_NAME = "report.sent";
    private static final String FIELD_REPORT = "report";

    public ReportSentEvent {
        Objects.requireNonNull(report, FIELD_REPORT);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_REPORT, report.toMap());
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
     * @return parsed event
     * @throws IOException on error
     */
    public static ReportSentEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var reportObj = map.get(FIELD_REPORT);
        if (reportObj == null) {
            // Check if map itself represents the report item
            if (map.containsKey("id") && map.containsKey("kind")) {
                return new ReportSentEvent(ReportItem.fromMap(map));
            }
            throw new JsonFormatException("Missing report field in ReportSentEvent");
        }
        var rm = JsonTree.asObject(reportObj);
        if (rm == null) throw new JsonFormatException("Expected JSON object for report in ReportSentEvent");
        return new ReportSentEvent(ReportItem.fromMap(rm));
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static ReportSentEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ReportSentEvent");
        }
        return fromMap(obj);
    }
}
