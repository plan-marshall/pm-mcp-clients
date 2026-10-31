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
 * One cited file or symbol the premise check could not find (UC-STA-8).
 * <p>
 * The evidence is what the operator reads before deciding to use the lesson anyway, to retire it,
 * or to leave it untouched: what is missing, where it was expected, and when it was last seen.
 *
 * @param kind               whether a file or a symbol is missing
 * @param name               the cited name that could not be resolved
 * @param expectedPath       where the file was expected, or {@code null} for a symbol
 * @param lastVerifiedCommit the commit the lesson last saw it at, or {@code null} if unknown
 */
public record LessonPremiseEvidence(
        PremiseEvidenceKind kind,
        String name,
        @Nullable String expectedPath,
        @Nullable String lastVerifiedCommit
) {

    private static final String FIELD_KIND = "kind";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_EXPECTED_PATH = "expected_path";
    private static final String FIELD_LAST_VERIFIED_COMMIT = "last_verified_commit";

    public LessonPremiseEvidence {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_NAME, name);
        if (expectedPath != null) {
            map.put(FIELD_EXPECTED_PATH, expectedPath);
        }
        if (lastVerifiedCommit != null) {
            map.put(FIELD_LAST_VERIFIED_COMMIT, lastVerifiedCommit);
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
     * @return the parsed evidence
     * @throws IOException if a required field is missing
     */
    public static LessonPremiseEvidence fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kindValue = JsonTree.string(map, FIELD_KIND);
        var name = JsonTree.string(map, FIELD_NAME);
        if (kindValue == null || name == null) {
            throw new JsonFormatException("Missing required fields for LessonPremiseEvidence: kind or name");
        }
        return new LessonPremiseEvidence(PremiseEvidenceKind.fromWire(kindValue), name,
                JsonTree.string(map, FIELD_EXPECTED_PATH),
                JsonTree.string(map, FIELD_LAST_VERIFIED_COMMIT));
    }

    /**
     * @param json the JSON object string
     * @return the parsed evidence
     * @throws IOException if the value is not an object or a field is missing
     */
    public static LessonPremiseEvidence fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LessonPremiseEvidence");
        }
        return fromMap(obj);
    }
}
