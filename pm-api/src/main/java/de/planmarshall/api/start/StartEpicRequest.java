/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The request that opens an epic's entry under its project (UC-STA-2).
 *
 * @param projectId the enrolled project
 * @param objective the epic's objective, or {@code null}
 * @param autoStart whether the epic starts its plans by itself, or {@code null} for the value in force
 * @param title     the proposed title, or {@code null} to derive it from the sources
 */
public record StartEpicRequest(
        String projectId,
        @Nullable String objective,
        @Nullable Boolean autoStart,
        @Nullable String title
) {

    private static final String FIELD_PROJECT_ID = "project_id";
    private static final String FIELD_OBJECTIVE = "objective";
    private static final String FIELD_AUTO_START = "auto_start";
    private static final String FIELD_TITLE = "title";

    public StartEpicRequest {
        Objects.requireNonNull(projectId, "projectId");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        if (objective != null) {
            map.put(FIELD_OBJECTIVE, objective);
        }
        if (autoStart != null) {
            map.put(FIELD_AUTO_START, autoStart);
        }
        if (title != null) {
            map.put(FIELD_TITLE, title);
        }
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
    public static StartEpicRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            throw new JsonFormatException("Missing required field for StartEpicRequest: project_id");
        }
        return new StartEpicRequest(projectId, JsonTree.string(map, FIELD_OBJECTIVE),
                JsonTree.bool(map, FIELD_AUTO_START), JsonTree.string(map, FIELD_TITLE));
    }

    /**
     * @param json the JSON object string
     * @return the parsed request
     * @throws IOException if the value is not an object or a field is missing
     */
    public static StartEpicRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for StartEpicRequest");
        }
        return fromMap(obj);
    }
}
