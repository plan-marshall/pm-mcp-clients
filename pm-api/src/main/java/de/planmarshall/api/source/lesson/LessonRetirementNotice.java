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
 * What the runtime reports after a lesson was retired (UC-STA-8, the {@code retire} choice of a
 * broken premise).
 *
 * @param lessonId the lesson that was retired
 * @param retired  whether the lesson is now retired
 * @param reason   why it was retired, or {@code null}
 */
public record LessonRetirementNotice(
        String lessonId,
        boolean retired,
        @Nullable String reason
) {

    private static final String FIELD_LESSON_ID = "lesson_id";
    private static final String FIELD_RETIRED = "retired";
    private static final String FIELD_REASON = "reason";

    public LessonRetirementNotice {
        Objects.requireNonNull(lessonId, "lessonId");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LESSON_ID, lessonId);
        map.put(FIELD_RETIRED, retired);
        if (reason != null) {
            map.put(FIELD_REASON, reason);
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
     * @return the parsed notice
     * @throws IOException if a required field is missing
     */
    public static LessonRetirementNotice fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var lessonId = JsonTree.string(map, FIELD_LESSON_ID);
        if (lessonId == null) {
            throw new JsonFormatException("Missing required field for LessonRetirementNotice: lesson_id");
        }
        return new LessonRetirementNotice(lessonId, Boolean.TRUE.equals(JsonTree.bool(map, FIELD_RETIRED)),
                JsonTree.string(map, FIELD_REASON));
    }

    /**
     * @param json the JSON object string
     * @return the parsed notice
     * @throws IOException if the value is not an object or a field is missing
     */
    public static LessonRetirementNotice fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LessonRetirementNotice");
        }
        return fromMap(obj);
    }
}
