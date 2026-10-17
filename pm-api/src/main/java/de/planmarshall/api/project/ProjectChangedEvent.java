/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.project;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a project's state or readiness changes ({@code project.changed}).
 *
 * @param projectId  unique project identifier
 * @param readiness  updated readiness state
 * @param timestamp  timestamp of the change
 */
public record ProjectChangedEvent(
        String projectId,
        ProjectReadiness readiness,
        Instant timestamp
) {
    public static final String EVENT_NAME = "project.changed";
    public static final String FIELD_EVENT = "event";
    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_READINESS = "readiness";
    public static final String FIELD_TIMESTAMP = "timestamp";

    public ProjectChangedEvent {
        Objects.requireNonNull(projectId, "projectId");
        readiness = readiness != null ? readiness : ProjectReadiness.READY;
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_EVENT, EVENT_NAME);
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_READINESS, readiness.value());
        map.put(FIELD_TIMESTAMP, timestamp.toString());
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
     * Parses from JSON tree map.
     *
     * @param map input map
     * @return parsed event
     * @throws IOException on format failure
     */
    public static ProjectChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new JsonFormatException("Missing project_id in ProjectChangedEvent");
        }

        var readinessStr = JsonTree.string(map, FIELD_READINESS);
        var readiness = ProjectReadiness.fromValue(readinessStr);
        if (readiness == null) {
            readiness = ProjectReadiness.READY;
        }

        var tsStr = JsonTree.string(map, FIELD_TIMESTAMP);
        Instant timestamp = Instant.now();
        if (tsStr != null && !tsStr.isBlank()) {
            try {
                timestamp = Instant.parse(tsStr);
            }
            /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
            catch (Exception _) {
                // Ignore parse errors, fallback to Instant.now()
            }
        }

        return new ProjectChangedEvent(projectId, readiness, timestamp);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static ProjectChangedEvent fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for ProjectChangedEvent");
    }
}
