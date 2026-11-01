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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Request payload to release a project (UC-PRJ-3).
 *
 * @param projectId      identifier of the project to release
 * @param confirmRelease whether operator confirmed the release
 * @param force          whether to force release ignoring waiting landings
 * @param reason         operator-supplied reason for releasing
 */
public record ProjectReleaseRequest(
        String projectId,
        boolean confirmRelease,
        boolean force,
        @Nullable String reason
) {
    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_CONFIRM_RELEASE = "confirm_release";
    public static final String FIELD_FORCE = "force";
    public static final String FIELD_REASON = "reason";

    public ProjectReleaseRequest {
        Objects.requireNonNull(projectId, "projectId");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_CONFIRM_RELEASE, confirmRelease);
        map.put(FIELD_FORCE, force);
        if (reason != null) {
            map.put(FIELD_REASON, reason);
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
     * Parses from JSON tree map.
     *
     * @param map input map
     * @return parsed request
     * @throws IOException on format failure
     */
    public static ProjectReleaseRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new JsonFormatException("Missing project_id in ProjectReleaseRequest");
        }

        var confirm = JsonTree.bool(map, FIELD_CONFIRM_RELEASE);
        if (confirm == null) {
            confirm = JsonTree.bool(map, "confirm");
        }
        boolean confirmRelease = Boolean.TRUE.equals(confirm);

        var forceVal = JsonTree.bool(map, FIELD_FORCE);
        boolean force = Boolean.TRUE.equals(forceVal);

        var reason = JsonTree.string(map, FIELD_REASON);

        return new ProjectReleaseRequest(projectId, confirmRelease, force, reason);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed request
     * @throws IOException on format failure
     */
    public static ProjectReleaseRequest fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for ProjectReleaseRequest");
    }
}
