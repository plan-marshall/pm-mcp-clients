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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a report is marked read ({@code report.read}).
 *
 * @param reportId identifier of the read report
 * @param readAt   timestamp when read
 * @param channel  operator channel ({@code "web"} or {@code "cli"})
 * @param device   optional device label or identifier
 */
public record ReportReadEvent(
        String reportId,
        Instant readAt,
        String channel,
        @Nullable String device
) {
    public static final String EVENT_NAME = "report.read";
    private static final String FIELD_REPORT_ID = "report_id";
    private static final String FIELD_READ_AT = "read_at";
    private static final String FIELD_CHANNEL = "channel";
    private static final String FIELD_DEVICE = "device";

    public ReportReadEvent {
        Objects.requireNonNull(reportId, FIELD_REPORT_ID);
        Objects.requireNonNull(readAt, FIELD_READ_AT);
        Objects.requireNonNull(channel, FIELD_CHANNEL);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_REPORT_ID, reportId);
        map.put(FIELD_READ_AT, readAt.toString());
        map.put(FIELD_CHANNEL, channel);
        if (device != null) map.put(FIELD_DEVICE, device);
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
    public static ReportReadEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var reportId = JsonTree.string(map, FIELD_REPORT_ID);
        if (reportId == null) reportId = JsonTree.string(map, "id");
        if (reportId == null) throw new JsonFormatException("Missing report_id in ReportReadEvent");

        var readAtStr = JsonTree.string(map, FIELD_READ_AT);
        var readAt = readAtStr != null ? Instant.parse(readAtStr) : Instant.now();

        var channel = JsonTree.string(map, FIELD_CHANNEL);
        if (channel == null) channel = "unknown";

        var device = JsonTree.string(map, FIELD_DEVICE);
        return new ReportReadEvent(reportId, readAt, channel, device);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static ReportReadEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ReportReadEvent");
        }
        return fromMap(obj);
    }
}
