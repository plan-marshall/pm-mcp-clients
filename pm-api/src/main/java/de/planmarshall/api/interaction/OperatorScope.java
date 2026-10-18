/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.interaction;

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
 * The scope of an operator state, the {@code scope} member of {@link OperatorStateResource}: its
 * {@link OperatorScopeKind}, the identifier of the scope where it has one, and the {@code project_id}
 * with its {@code display_name} where the scope belongs to a project (absent for the machine scope).
 *
 * @param kind               the kind of the scope
 * @param id                 the identifier of the scope ({@code plan_id}, {@code epic_id}, or {@code entry_id}),
 *                           or {@code null} for the machine and the workspace scope
 * @param projectId          the project the scope belongs to, or {@code null} for the machine
 * @param projectDisplayName the display name of the project, or {@code null}
 */
public record OperatorScope(
        OperatorScopeKind kind,
        @Nullable String id,
        @Nullable String projectId,
        @Nullable String projectDisplayName
) {
    public static final String FIELD_KIND = "kind";
    public static final String FIELD_ID = "id";
    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_PROJECT_DISPLAY_NAME = "display_name";

    public OperatorScope {
        Objects.requireNonNull(kind, FIELD_KIND);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind.wire());
        if (id != null) {
            map.put(FIELD_ID, id);
        }
        if (projectId != null) {
            map.put(FIELD_PROJECT_ID, projectId);
        }
        if (projectDisplayName != null) {
            map.put(FIELD_PROJECT_DISPLAY_NAME, projectDisplayName);
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
    public static OperatorScope fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kind = OperatorScopeKind.fromWireOrNull(JsonTree.string(map, FIELD_KIND));
        if (kind == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_KIND + " in OperatorScope: " + JsonTree.string(map, FIELD_KIND));
        }
        return new OperatorScope(kind, JsonTree.string(map, FIELD_ID), JsonTree.string(map, FIELD_PROJECT_ID),
                JsonTree.string(map, FIELD_PROJECT_DISPLAY_NAME));
    }

    /**
     * Reads one scope from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed scope
     * @throws IOException on format failure
     */
    public static OperatorScope readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for OperatorScope");
        }
        return fromMap(map);
    }
}
