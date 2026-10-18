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
 * Request payload of {@code POST /api/v1/enrol}: the direct verb of a project's enrolment
 * ({@code pm-operator enrol [--pilot] [path]}, PM-EXEC-2).
 *
 * @param projectPath the checkout path to enrol; resolved canonically by the runtime
 * @param pilot       whether the project is enrolled in pilot mode (the lifecycle gate)
 */
public record EnrolRequest(
        String projectPath,
        boolean pilot
) {
    public static final String FIELD_PATH = "path";
    public static final String FIELD_PILOT = "pilot";

    public EnrolRequest {
        Objects.requireNonNull(projectPath, "projectPath");
        if (projectPath.isBlank()) {
            throw new IllegalArgumentException("projectPath must not be blank");
        }
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PATH, projectPath);
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
     * @return parsed request
     * @throws IOException on format failure
     */
    public static EnrolRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var path = JsonTree.string(map, FIELD_PATH);
        if (path == null) {
            path = JsonTree.string(map, "project_path");
        }
        if (path == null || path.isBlank()) {
            throw new JsonFormatException("Missing path in EnrolRequest");
        }
        var pilot = Boolean.TRUE.equals(JsonTree.bool(map, FIELD_PILOT));
        return new EnrolRequest(path, pilot);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed request
     * @throws IOException on format failure
     */
    public static EnrolRequest fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for EnrolRequest");
    }
}
