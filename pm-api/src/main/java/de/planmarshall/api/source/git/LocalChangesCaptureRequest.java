/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.git;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The request that captures the checkout's local changes into a plan in the {@code land} lane
 * (UC-STA-9).
 * <p>
 * The capture is a confirmation gate: without {@link #confirmed()} the runtime refuses, so an
 * accidental snapshot commit cannot be made.
 *
 * @param projectId the enrolled project
 * @param confirmed whether the operator confirmed the capture
 */
public record LocalChangesCaptureRequest(
        String projectId,
        boolean confirmed
) {

    private static final String FIELD_PROJECT_ID = "project_id";
    private static final String FIELD_CONFIRMED = "confirmed";

    public LocalChangesCaptureRequest {
        Objects.requireNonNull(projectId, "projectId");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_CONFIRMED, confirmed);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the input map
     * @return the parsed request
     * @throws IOException if the project id is missing
     */
    public static LocalChangesCaptureRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            throw new JsonFormatException("Missing required field for LocalChangesCaptureRequest: project_id");
        }
        return new LocalChangesCaptureRequest(projectId,
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_CONFIRMED)));
    }

    /**
     * @param json the JSON object string
     * @return the parsed request
     * @throws IOException if the value is not an object or the project id is missing
     */
    public static LocalChangesCaptureRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LocalChangesCaptureRequest");
        }
        return fromMap(obj);
    }
}
