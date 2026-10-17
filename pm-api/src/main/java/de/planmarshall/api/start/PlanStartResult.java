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
 * The result of a start: the plan or the epic exists ({@code start.committed}, {@code
 * start-epic.committed}).
 *
 * @param id         the created plan or epic id
 * @param kind       {@code plan} or {@code epic}
 * @param title      the title the work carries
 * @param state      the state the run reached (normally {@code started})
 * @param followHref the hypermedia link to follow the work, or {@code null}
 */
public record PlanStartResult(
        String id,
        String kind,
        String title,
        String state,
        @Nullable String followHref
) {

    /** The kind of a started plan. */
    public static final String KIND_PLAN = "plan";

    /** The kind of a started epic. */
    public static final String KIND_EPIC = "epic";

    /** The state a successful start reaches. */
    public static final String STATE_STARTED = "started";

    private static final String FIELD_ID = "id";
    private static final String FIELD_KIND = "kind";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_FOLLOW_HREF = "follow_href";

    public PlanStartResult {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(title, FIELD_TITLE);
        state = state == null || state.isBlank() ? STATE_STARTED : state;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        map.put(FIELD_KIND, kind);
        map.put(FIELD_TITLE, title);
        map.put(FIELD_STATE, state);
        if (followHref != null) {
            map.put(FIELD_FOLLOW_HREF, followHref);
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
    public static PlanStartResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, FIELD_ID);
        var kind = JsonTree.string(map, FIELD_KIND);
        var title = JsonTree.string(map, FIELD_TITLE);
        if (id == null || kind == null || title == null) {
            throw new JsonFormatException("Missing required fields for PlanStartResult: id, kind or title");
        }
        return new PlanStartResult(id, kind, title, JsonTree.string(map, FIELD_STATE),
                JsonTree.string(map, FIELD_FOLLOW_HREF));
    }

    /**
     * @param json the JSON object string
     * @return the parsed result
     * @throws IOException if the value is not an object or a field is missing
     */
    public static PlanStartResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PlanStartResult");
        }
        return fromMap(obj);
    }
}
