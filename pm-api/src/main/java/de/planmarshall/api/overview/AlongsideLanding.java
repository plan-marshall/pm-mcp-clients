/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;

/**
 * Represents a short landing running alongside plans and epics in a project.
 *
 * @param unit     the target unit name, or {@code null} for document removals
 * @param state    the current state ("running", "failed")
 * @param count    the document count for removal landings, or {@code null}
 * @param kind     the landing kind ("unit" or "removal")
 * @param holdKind the hold reason if failed ("landing_failed"), or {@code null}
 */
public record AlongsideLanding(
@Nullable String unit,
String state,
@Nullable Integer count,
String kind,
@Nullable String holdKind
) {
    public AlongsideLanding {
        state = state != null ? state : "running";
        kind = kind != null ? kind : (count != null ? "removal" : "unit");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (unit != null) {
            map.put("unit", unit);
        }
        map.put("state", state);
        if (count != null) {
            map.put("count", count);
        }
        map.put("kind", kind);
        if (holdKind != null) {
            map.put("hold_kind", holdKind);
        }
        return map;
    }

    /**
     * Parses from JSON tree map.
     *
     * @param map map representation
     * @return parsed alongside landing
     * @throws IOException on format error
     */
    public static AlongsideLanding fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var unit = JsonTree.string(map, "unit");
        var state = JsonTree.string(map, "state");
        if (state == null) {
            state = "running";
        }
        var countNum = JsonTree.number(map, "count");
        Integer count = countNum != null ? countNum.intValue() : null;
        var kind = JsonTree.string(map, "kind");
        if (kind == null) {
            kind = count != null ? "removal" : "unit";
        }
        var holdKind = JsonTree.string(map, "hold_kind");
        if (holdKind == null) {
            holdKind = JsonTree.string(map, "holdKind");
        }
        return new AlongsideLanding(unit, state, count, kind, holdKind);
    }
}
