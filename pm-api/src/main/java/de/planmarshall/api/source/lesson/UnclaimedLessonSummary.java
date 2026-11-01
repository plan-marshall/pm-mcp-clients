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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One unclaimed lesson of a project as the choice offers it (UC-STA-8).
 * <p>
 * Only lessons that are neither claimed by a plan nor retired appear here; they are listed newest
 * first so the lesson of the most recent plan is the first the operator sees.
 *
 * @param lessonId     the lesson's id
 * @param title        the lesson's short title
 * @param summary      the lesson's summary, one or two sentences
 * @param recordedAt   when the lesson was recorded
 * @param citingPlanId the plan that recorded the lesson, or {@code null} if unknown
 * @param symbolsCited the files and symbols the lesson cites
 */
public record UnclaimedLessonSummary(
        String lessonId,
        String title,
        String summary,
        Instant recordedAt,
        @Nullable String citingPlanId,
        List<String> symbolsCited
) {

    private static final String FIELD_LESSON_ID = "lesson_id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_RECORDED_AT = "recorded_at";
    private static final String FIELD_CITING_PLAN_ID = "citing_plan_id";
    private static final String FIELD_SYMBOLS_CITED = "symbols_cited";

    public UnclaimedLessonSummary {
        Objects.requireNonNull(lessonId, "lessonId");
        Objects.requireNonNull(title, "title");
        summary = summary == null ? "" : summary;
        recordedAt = recordedAt == null ? Instant.now() : recordedAt;
        symbolsCited = symbolsCited == null ? List.of() : List.copyOf(symbolsCited);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LESSON_ID, lessonId);
        map.put(FIELD_TITLE, title);
        map.put(FIELD_SUMMARY, summary);
        map.put(FIELD_RECORDED_AT, recordedAt.toString());
        if (citingPlanId != null) {
            map.put(FIELD_CITING_PLAN_ID, citingPlanId);
        }
        map.put(FIELD_SYMBOLS_CITED, symbolsCited);
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
     * @return the parsed summary
     * @throws IOException if a required field is missing
     */
    public static UnclaimedLessonSummary fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var lessonId = JsonTree.string(map, FIELD_LESSON_ID);
        var title = JsonTree.string(map, FIELD_TITLE);
        if (lessonId == null || title == null) {
            throw new JsonFormatException("Missing required fields for UnclaimedLessonSummary: lesson_id or title");
        }
        var symbols = new ArrayList<String>();
        var raw = JsonTree.asArray(map.get(FIELD_SYMBOLS_CITED));
        if (raw != null) {
            for (var item : raw) {
                if (item != null) {
                    symbols.add(String.valueOf(item));
                }
            }
        }
        var recordedAtStr = JsonTree.string(map, FIELD_RECORDED_AT);
        return new UnclaimedLessonSummary(lessonId, title, JsonTree.string(map, FIELD_SUMMARY),
                recordedAtStr != null ? Instant.parse(recordedAtStr) : Instant.now(),
                JsonTree.string(map, FIELD_CITING_PLAN_ID), symbols);
    }

    /**
     * @param json the JSON object string
     * @return the parsed summary
     * @throws IOException if the value is not an object or a field is missing
     */
    public static UnclaimedLessonSummary fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for UnclaimedLessonSummary");
        }
        return fromMap(obj);
    }
}
