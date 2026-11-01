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
 * A draft intake session: the sources brought in and the prompt text (UC-STA-3, UC-STA-4).
 * <p>
 * An entry never expires; a draft is kept among the project's open entries until it is started or
 * abandoned.
 *
 * @param id        the draft's id
 * @param projectId the project the draft belongs to
 * @param prompt    the operator's words, or {@code null}
 * @param sources   the sources collected so far
 * @param createdAt when the draft was opened
 */
public record IntakeDraft(
        String id,
        String projectId,
        @Nullable String prompt,
        List<SourceInput> sources,
        Instant createdAt
) {

    private static final String FIELD_ID = "id";
    private static final String FIELD_PROJECT_ID = "project_id";
    private static final String FIELD_PROMPT = "prompt";
    private static final String FIELD_SOURCES = "sources";
    private static final String FIELD_CREATED_AT = "created_at";

    public IntakeDraft {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        sources = sources == null ? List.of() : List.copyOf(sources);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        map.put(FIELD_PROJECT_ID, projectId);
        if (prompt != null) {
            map.put(FIELD_PROMPT, prompt);
        }
        map.put(FIELD_SOURCES, sources.stream().map(SourceInput::toMap).toList());
        map.put(FIELD_CREATED_AT, createdAt.toString());
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
     * @return the parsed draft
     * @throws IOException if a required field is missing
     */
    public static IntakeDraft fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, FIELD_ID);
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (id == null || projectId == null) {
            throw new JsonFormatException("Missing required fields for IntakeDraft: id or project_id");
        }
        var sources = new ArrayList<SourceInput>();
        var raw = JsonTree.asArray(map.get(FIELD_SOURCES));
        if (raw != null) {
            for (var item : raw) {
                var obj = JsonTree.asObject(item);
                if (obj != null) {
                    sources.add(SourceInput.fromMap(obj));
                }
            }
        }
        var createdAtStr = JsonTree.string(map, FIELD_CREATED_AT);
        return new IntakeDraft(id, projectId, JsonTree.string(map, FIELD_PROMPT), sources,
                createdAtStr != null ? Instant.parse(createdAtStr) : Instant.now());
    }

    /**
     * @param json the JSON object string
     * @return the parsed draft
     * @throws IOException if the value is not an object or a field is missing
     */
    public static IntakeDraft fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for IntakeDraft");
        }
        return fromMap(obj);
    }
}
