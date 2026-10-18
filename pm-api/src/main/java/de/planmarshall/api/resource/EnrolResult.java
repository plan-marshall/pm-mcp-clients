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
 * Result of a project enrolment ({@code POST /api/v1/enrol}, PM-EXEC-2).
 *
 * @param status    the enrolment outcome, {@code enrolled}
 * @param projectId the identifier the runtime assigned to the enrolled project
 * @param pilot     whether the project was enrolled in pilot mode
 */
public record EnrolResult(
        String status,
        String projectId,
        boolean pilot
) {
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_PILOT = "pilot";

    /** The status of a completed enrolment. */
    public static final String STATUS_ENROLLED = "enrolled";

    public EnrolResult {
        Objects.requireNonNull(status, FIELD_STATUS);
        Objects.requireNonNull(projectId, "projectId");
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STATUS, status);
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_PILOT, pilot);
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
     * @throws IOException on format failure
     */
    public static EnrolResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var status = JsonTree.string(map, FIELD_STATUS);
        if (status == null || status.isBlank()) {
            status = STATUS_ENROLLED;
        }
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new JsonFormatException("Missing project_id in EnrolResult");
        }
        var pilot = Boolean.TRUE.equals(JsonTree.bool(map, FIELD_PILOT));
        return new EnrolResult(status, projectId, pilot);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed result
     * @throws IOException on format failure
     */
    public static EnrolResult fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for EnrolResult");
    }
}
