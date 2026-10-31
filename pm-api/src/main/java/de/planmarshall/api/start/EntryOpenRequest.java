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
import java.util.Set;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The request that opens an entry under its project ({@code POST /api/v1/projects/{project_id}/entries},
 * UC-STA-1, UC-STA-2).
 * <p>
 * The opening request is the <em>only</em> request that names the project: from then on the entry is
 * addressed at the top level by its {@code entry_id} ({@code /api/v1/entries/{entry_id}} and its
 * events at {@code /api/v1/entries/{entry_id}/events}). A plan's entry may open with the recipe it
 * runs; an epic's entry never does.
 *
 * @param kind      the kind of work the entry starts ({@code plan} or {@code epic})
 * @param objective the operator's words for the work, or {@code null}
 * @param title     the proposed title, or {@code null} to derive it from the sources
 * @param recipeKey the recipe a plan runs, or {@code null} for a plan from sources
 * @param fromEpic  the epic that starts the plan on its own, or {@code null}
 */
public record EntryOpenRequest(
        EntryKind kind,
        @Nullable String objective,
        @Nullable String title,
        @Nullable String recipeKey,
        @Nullable String fromEpic
) {

    public static final String FIELD_KIND = "kind";
    public static final String FIELD_OBJECTIVE = "objective";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_RECIPE_KEY = "recipe_key";
    public static final String FIELD_FROM_EPIC = "from_epic";

    private static final Set<String> KNOWN_FIELDS =
            Set.of(FIELD_KIND, FIELD_OBJECTIVE, FIELD_TITLE, FIELD_RECIPE_KEY, FIELD_FROM_EPIC);

    public EntryOpenRequest {
        Objects.requireNonNull(kind, FIELD_KIND);
        if (kind.epic() && recipeKey != null) {
            throw new IllegalArgumentException("An epic's entry takes no recipe");
        }
    }

    /**
     * @param kind the kind of work
     * @return a request that opens an entry of that kind without any content
     */
    public static EntryOpenRequest of(EntryKind kind) {
        return new EntryOpenRequest(kind, null, null, null, null);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind.wireValue());
        if (objective != null) {
            map.put(FIELD_OBJECTIVE, objective);
        }
        if (title != null) {
            map.put(FIELD_TITLE, title);
        }
        if (recipeKey != null) {
            map.put(FIELD_RECIPE_KEY, recipeKey);
        }
        if (fromEpic != null) {
            map.put(FIELD_FROM_EPIC, fromEpic);
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
     * @return the parsed request
     * @throws IOException if the kind is missing or unknown, or the request breaks an invariant
     */
    public static EntryOpenRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "EntryOpenRequest");
        var kind = EntryKind.fromWireOrNull(JsonTree.string(map, FIELD_KIND));
        if (kind == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_KIND + " in EntryOpenRequest: " + JsonTree.string(map, FIELD_KIND));
        }
        try {
            return new EntryOpenRequest(kind, JsonTree.string(map, FIELD_OBJECTIVE),
                    JsonTree.string(map, FIELD_TITLE), JsonTree.string(map, FIELD_RECIPE_KEY),
                    JsonTree.string(map, FIELD_FROM_EPIC));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid EntryOpenRequest: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed request
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static EntryOpenRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EntryOpenRequest");
        }
        return fromMap(obj);
    }
}
