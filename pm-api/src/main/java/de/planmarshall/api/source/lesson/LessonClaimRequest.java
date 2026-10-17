/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.lesson;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The request that claims a lesson for a plan (UC-STA-8, {@code source-lesson.store}).
 *
 * @param projectId the enrolled project
 * @param lessonId  the lesson to claim
 * @param useAnyway whether the operator chose {@code use it anyway} over a broken premise
 */
public record LessonClaimRequest(
        String projectId,
        String lessonId,
        @Nullable Boolean useAnyway
) {

    private static final String FIELD_PROJECT_ID = "project_id";
    private static final String FIELD_LESSON_ID = "lesson_id";
    private static final String FIELD_USE_ANYWAY = "use_anyway";

    public LessonClaimRequest {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(lessonId, "lessonId");
    }

    /**
     * @return {@code true} if the operator chose {@code use it anyway}
     */
    public boolean isUseAnyway() {
        return Boolean.TRUE.equals(useAnyway);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_LESSON_ID, lessonId);
        map.put(FIELD_USE_ANYWAY, isUseAnyway());
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
     * @throws IOException if a required field is missing
     */
    public static LessonClaimRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        var lessonId = JsonTree.string(map, FIELD_LESSON_ID);
        if (projectId == null || lessonId == null) {
            throw new JsonFormatException("Missing required fields for LessonClaimRequest: project_id or lesson_id");
        }
        return new LessonClaimRequest(projectId, lessonId, JsonTree.bool(map, FIELD_USE_ANYWAY));
    }

    /**
     * @param json the JSON object string
     * @return the parsed request
     * @throws IOException if the value is not an object or a field is missing
     */
    public static LessonClaimRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LessonClaimRequest");
        }
        return fromMap(obj);
    }
}
