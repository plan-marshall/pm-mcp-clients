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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One source of a start: its kind, what it is, and how it is shown.
 *
 * @param kind     the kind of the source
 * @param location the location or reference of the source (a path, an issue URL, a lesson id)
 * @param title    the title once it is resolved, or {@code null} while unresolved
 */
public record SourceInput(
        SourceKind kind,
        String location,
        @Nullable String title
) {

    private static final String FIELD_KIND = "kind";
    private static final String FIELD_LOCATION = "location";
    private static final String FIELD_TITLE = "title";

    public SourceInput {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(location, "location");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_LOCATION, location);
        if (title != null) {
            map.put(FIELD_TITLE, title);
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
     * @return the parsed source
     * @throws IOException if a required field is missing
     */
    public static SourceInput fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kindValue = JsonTree.string(map, FIELD_KIND);
        var location = JsonTree.string(map, FIELD_LOCATION);
        if (kindValue == null || location == null) {
            throw new JsonFormatException("Missing required fields for SourceInput: kind or location");
        }
        return new SourceInput(SourceKind.fromWire(kindValue), location, JsonTree.string(map, FIELD_TITLE));
    }

    /**
     * @param json the JSON object string
     * @return the parsed source
     * @throws IOException if the value is not an object or a field is missing
     */
    public static SourceInput fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for SourceInput");
        }
        return fromMap(obj);
    }
}
