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
 * The result of claiming a lesson: the lesson now belongs to a plan ({@code
 * source-lesson.store}), or the claim was refused.
 *
 * @param lessonId the lesson the claim ran for
 * @param status   {@code claimed} or {@code refused}
 * @param planId   the plan that owns the lesson, or {@code null} if the claim was refused
 */
public record LessonClaimResult(
        String lessonId,
        String status,
        @Nullable String planId
) {

    /** The lesson was claimed for the plan. */
    public static final String STATUS_CLAIMED = "claimed";

    /** The claim was refused, for example because another plan owns the lesson. */
    public static final String STATUS_REFUSED = "refused";

    private static final String FIELD_LESSON_ID = "lesson_id";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_PLAN_ID = "plan_id";

    public LessonClaimResult {
        Objects.requireNonNull(lessonId, "lessonId");
        Objects.requireNonNull(status, "status");
    }

    /**
     * @return {@code true} if the claim succeeded
     */
    public boolean claimed() {
        return STATUS_CLAIMED.equals(status);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LESSON_ID, lessonId);
        map.put(FIELD_STATUS, status);
        if (planId != null) {
            map.put(FIELD_PLAN_ID, planId);
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
     * @return the parsed result
     * @throws IOException if a required field is missing
     */
    public static LessonClaimResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var lessonId = JsonTree.string(map, FIELD_LESSON_ID);
        var status = JsonTree.string(map, FIELD_STATUS);
        if (lessonId == null || status == null) {
            throw new JsonFormatException("Missing required fields for LessonClaimResult: lesson_id or status");
        }
        return new LessonClaimResult(lessonId, status, JsonTree.string(map, FIELD_PLAN_ID));
    }

    /**
     * @param json the JSON object string
     * @return the parsed result
     * @throws IOException if the value is not an object or a field is missing
     */
    public static LessonClaimResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LessonClaimResult");
        }
        return fromMap(obj);
    }
}
