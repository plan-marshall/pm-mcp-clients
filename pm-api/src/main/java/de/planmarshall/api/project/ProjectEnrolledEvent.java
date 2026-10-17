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
 * Event payload emitted when a project is enrolled ({@code project.enrolled}).
 *
 * @param projectId    unique project identifier
 * @param displayName  human-readable display name of the project
 * @param timestamp    timestamp of the enrolment
 */
public record ProjectEnrolledEvent(
        String projectId,
        String displayName,
        Instant timestamp
) {
    public static final String EVENT_NAME = "project.enrolled";
    public static final String FIELD_EVENT = "event";
    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_DISPLAY_NAME = "display_name";
    public static final String FIELD_TIMESTAMP = "timestamp";

    public ProjectEnrolledEvent {
        Objects.requireNonNull(projectId, "projectId");
        displayName = displayName == null || displayName.isBlank() ? projectId : displayName;
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
        map.put(FIELD_DISPLAY_NAME, displayName);
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
    public static ProjectEnrolledEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new JsonFormatException("Missing project_id in ProjectEnrolledEvent");
        }

        var displayName = JsonTree.string(map, FIELD_DISPLAY_NAME);
        if (displayName == null) {
            displayName = JsonTree.string(map, "name");
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

        return new ProjectEnrolledEvent(projectId, displayName, timestamp);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static ProjectEnrolledEvent fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for ProjectEnrolledEvent");
    }
}
