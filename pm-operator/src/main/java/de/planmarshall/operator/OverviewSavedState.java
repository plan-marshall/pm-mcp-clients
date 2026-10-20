/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import java.io.IOException;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Where the operator left the overview, as it is stored in
 * {@code $XDG_STATE_HOME/pm-operator/overview.json}
 * (doc/design/terminal-client/15-overview.adoc#overview-persistence).
 *
 * <p>It holds no content of a plan: a project id, the view of the project, the id of the selected
 * item, and the system view's window, grouping, and project. Every field is optional; a missing
 * field means the default of the view.
 *
 * @param projectId       the project the operator was in, or {@code null}
 * @param projectView     {@code plan} or {@code epic}, or {@code null}
 * @param selectedId      the id of the selected plan or epic, or {@code null}
 * @param systemWindow    the system view's window ({@code hour}, {@code today}, {@code 7d})
 * @param systemGroupBy   the system view's grouping ({@code model} or {@code role})
 * @param systemProjectId the project the system view showed, or {@code null}
 */
public record OverviewSavedState(@Nullable String projectId, @Nullable String projectView,
                                 @Nullable String selectedId, @Nullable String systemWindow,
                                 @Nullable String systemGroupBy, @Nullable String systemProjectId) {

    /** The views a project can be shown in. */
    public static final Set<String> PROJECT_VIEWS = Set.of("plan", "epic");

    /** The windows the system view can show. */
    public static final Set<String> SYSTEM_WINDOWS = Set.of("hour", "today", "7d");

    /** The groupings the system view can show. */
    public static final Set<String> SYSTEM_GROUPINGS = Set.of("model", "role");

    /**
     * @return an empty state, which every view falls back to its default for
     */
    public static OverviewSavedState empty() {
        return new OverviewSavedState(null, null, null, null, null, null);
    }

    /**
     * Writes the state as one JSON object.
     *
     * @param generator the generator to write to
     * @throws IOException on a write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        writeString(generator, "project_id", projectId);
        writeString(generator, "project_view", valid(projectView, PROJECT_VIEWS));
        writeString(generator, "selected_id", selectedId);
        writeString(generator, "system_window", valid(systemWindow, SYSTEM_WINDOWS));
        writeString(generator, "system_group_by", valid(systemGroupBy, SYSTEM_GROUPINGS));
        writeString(generator, "system_project_id", systemProjectId);
        generator.writeEndObject();
    }

    /**
     * Reads the state from one JSON object. Unknown fields are skipped; a field of the wrong type
     * or outside its closed set is ignored, so a hand-edited file never breaks the view.
     *
     * @param parser the parser, positioned at the start object
     * @return the state of the file
     * @throws IOException on a malformed document
     */
    public static OverviewSavedState fromJson(JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IOException("overview.json is not a JSON object");
        }
        String projectId = null;
        String projectView = null;
        String selectedId = null;
        String systemWindow = null;
        String systemGroupBy = null;
        String systemProjectId = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var name = parser.currentName();
            var value = parser.nextToken();
            switch (name == null ? "" : name) {
                case "project_id" -> projectId = readString(parser, value);
                case "project_view" -> projectView = readString(parser, value);
                case "selected_id" -> selectedId = readString(parser, value);
                case "system_window" -> systemWindow = readString(parser, value);
                case "system_group_by" -> systemGroupBy = readString(parser, value);
                case "system_project_id" -> systemProjectId = readString(parser, value);
                default -> parser.skipChildren();
            }
        }
        return new OverviewSavedState(projectId, valid(projectView, PROJECT_VIEWS), selectedId,
                            valid(systemWindow, SYSTEM_WINDOWS), valid(systemGroupBy, SYSTEM_GROUPINGS),
                            systemProjectId);
    }

    private static @Nullable String readString(JsonParser parser, JsonToken value) throws IOException {
        if (value == JsonToken.VALUE_STRING) {
            var text = parser.getText();
            return text == null || text.isBlank() ? null : truncate(text);
        }
        if (value == JsonToken.START_OBJECT || value == JsonToken.START_ARRAY) {
            parser.skipChildren();
        }
        return null;
    }

    private static @Nullable String truncate(String value) {
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    private static @Nullable String valid(@Nullable String value, Set<String> allowed) {
        return value != null && allowed.contains(value) ? value : null;
    }

    private static void writeString(JsonGenerator generator, String name, @Nullable String value)
                        throws IOException {
        if (value != null) {
            generator.writeStringField(name, value);
        }
    }
}
