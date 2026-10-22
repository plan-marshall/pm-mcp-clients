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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The outcome of verifying a lesson's premise against the repository tree (UC-STA-8).
 * <p>
 * The check is three-valued: {@link #STATUS_HOLDS} when every cited file and symbol still exists,
 * {@link #STATUS_BROKEN} when at least one is missing (with the evidence naming each one), and
 * {@link #STATUS_UNAVAILABLE} when the repository tree could not be read at all.
 *
 * @param lessonId the lesson the check ran for
 * @param status   one of {@code holds}, {@code broken}, or {@code unavailable}
 * @param evidence the missing files and symbols, empty when the premise holds
 * @param cause    why an unavailable check could not run, or {@code null}
 */
public record LessonPremiseCheckResult(
        String lessonId,
        String status,
        List<LessonPremiseEvidence> evidence,
        @Nullable String cause
) {

    /** The premise holds: every cited file and symbol exists. */
    public static final String STATUS_HOLDS = "holds";

    /** The premise is broken: at least one cited file or symbol is missing. */
    public static final String STATUS_BROKEN = "broken";

    /** The check could not run: the repository tree was unreadable. */
    public static final String STATUS_UNAVAILABLE = "unavailable";

    private static final String FIELD_LESSON_ID = "lesson_id";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_EVIDENCE = "evidence";
    private static final String FIELD_CAUSE = "cause";

    public LessonPremiseCheckResult {
        Objects.requireNonNull(lessonId, "lessonId");
        Objects.requireNonNull(status, "status");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    /**
     * @return {@code true} if the premise holds
     */
    public boolean holds() {
        return STATUS_HOLDS.equals(status);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LESSON_ID, lessonId);
        map.put(FIELD_STATUS, status);
        map.put(FIELD_EVIDENCE, evidence.stream().map(LessonPremiseEvidence::toMap).toList());
        if (cause != null) {
            map.put(FIELD_CAUSE, cause);
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
    public static LessonPremiseCheckResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var lessonId = JsonTree.string(map, FIELD_LESSON_ID);
        var status = JsonTree.string(map, FIELD_STATUS);
        if (lessonId == null || status == null) {
            throw new JsonFormatException("Missing required fields for LessonPremiseCheckResult: lesson_id or status");
        }
        var evidence = new ArrayList<LessonPremiseEvidence>();
        var raw = JsonTree.asArray(map.get(FIELD_EVIDENCE));
        if (raw != null) {
            for (var item : raw) {
                var obj = JsonTree.asObject(item);
                if (obj != null) {
                    evidence.add(LessonPremiseEvidence.fromMap(obj));
                }
            }
        }
        return new LessonPremiseCheckResult(lessonId, status, evidence, JsonTree.string(map, FIELD_CAUSE));
    }

    /**
     * @param json the JSON object string
     * @return the parsed result
     * @throws IOException if the value is not an object or a field is missing
     */
    public static LessonPremiseCheckResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LessonPremiseCheckResult");
        }
        return fromMap(obj);
    }
}
