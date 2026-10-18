/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.event;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The scope an event happened in, the {@code scope} member of an {@link EventEnvelope}'s
 * {@code data}: the kind ({@code runtime}, {@code plan}, {@code epic}, {@code workspace},
 * {@code machine}, or {@code entry}), the identifier of the scope where there is one (absent for the
 * machine scope and the runtime-wide stream), and the {@code project_id} of the project the scope
 * belongs to where there is one.
 *
 * @param kind      the kind of the scope
 * @param id        the identifier of the scope, or {@code null} for the machine and the runtime
 * @param projectId the project the scope belongs to, or {@code null}
 */
public record EventScope(
        String kind,
        @Nullable String id,
        @Nullable String projectId
) {
    public static final String FIELD_KIND = "kind";
    public static final String FIELD_ID = "id";
    public static final String FIELD_PROJECT_ID = "project_id";

    public EventScope {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("kind must not be blank in EventScope");
        }
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind);
        if (id != null) {
            map.put(FIELD_ID, id);
        }
        if (projectId != null) {
            map.put(FIELD_PROJECT_ID, projectId);
        }
        return map;
    }

    /**
     * Writes this scope to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map input map
     * @return parsed scope
     * @throws IOException on format failure
     */
    public static EventScope fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kind = JsonTree.string(map, FIELD_KIND);
        if (kind == null || kind.isBlank()) {
            throw new JsonFormatException("Missing " + FIELD_KIND + " in EventScope");
        }
        return new EventScope(kind, JsonTree.string(map, FIELD_ID), JsonTree.string(map, FIELD_PROJECT_ID));
    }

    /**
     * Reads one scope from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed scope
     * @throws IOException on format failure
     */
    public static EventScope readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for EventScope");
        }
        return fromMap(map);
    }
}
