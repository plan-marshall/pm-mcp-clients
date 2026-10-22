/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.report;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A decision the plan took on its own, apart from the operator's decisions.
 *
 * <p>A shortcut also carries the limit it accepts and the condition that ends it, so the report
 * names both without the client reconstructing them.
 *
 * @param kind           kind of own call ({@code "design"}, {@code "shortcut"}, or {@code "dependency"})
 * @param description    what the plan decided, in its own words
 * @param limit          the limit a shortcut accepts ({@code null} for other kinds)
 * @param endsWhen       the condition that ends a shortcut ({@code null} for other kinds)
 * @param judgment       the self-review's judgment ({@code "consistent"} or {@code "conflicts"})
 * @param conflictTarget the item the call conflicts with ({@code null} unless the judgment conflicts)
 */
public record OwnCallRecord(
        String kind,
        String description,
        @Nullable String limit,
        @Nullable String endsWhen,
        String judgment,
        @Nullable String conflictTarget
) {
    private static final String FIELD_KIND = "kind";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_LIMIT = "limit";
    private static final String FIELD_ENDS_WHEN = "ends_when";
    private static final String FIELD_JUDGMENT = "judgment";
    private static final String FIELD_CONFLICT_TARGET = "conflict_target";

    public OwnCallRecord {
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(description, FIELD_DESCRIPTION);
        Objects.requireNonNull(judgment, FIELD_JUDGMENT);
        if ("shortcut".equalsIgnoreCase(kind)) {
            if (limit == null || limit.isBlank()) {
                throw new IllegalArgumentException("A shortcut requires the limit it accepts");
            }
            if (endsWhen == null || endsWhen.isBlank()) {
                throw new IllegalArgumentException("A shortcut requires the condition that ends it");
            }
        }
        if ("conflicts".equalsIgnoreCase(judgment) && (conflictTarget == null || conflictTarget.isBlank())) {
            throw new IllegalArgumentException("A conflicting own call requires the item it conflicts with");
        }
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind);
        map.put(FIELD_DESCRIPTION, description);
        if (limit != null) map.put(FIELD_LIMIT, limit);
        if (endsWhen != null) map.put(FIELD_ENDS_WHEN, endsWhen);
        map.put(FIELD_JUDGMENT, judgment);
        if (conflictTarget != null) map.put(FIELD_CONFLICT_TARGET, conflictTarget);
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed own call
     * @throws IOException when the kind or the judgment is missing
     */
    public static OwnCallRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kind = JsonTree.string(map, FIELD_KIND);
        if (kind == null) throw new JsonFormatException("Missing kind in OwnCallRecord");
        var description = JsonTree.string(map, FIELD_DESCRIPTION);
        if (description == null) description = JsonTree.string(map, "text");
        if (description == null) throw new JsonFormatException("Missing description in OwnCallRecord");
        var limit = JsonTree.string(map, FIELD_LIMIT);
        var endsWhen = JsonTree.string(map, FIELD_ENDS_WHEN);
        if (endsWhen == null) endsWhen = JsonTree.string(map, "condition");
        var judgment = JsonTree.string(map, FIELD_JUDGMENT);
        if (judgment == null) throw new JsonFormatException("Missing judgment in OwnCallRecord");
        var conflictTarget = JsonTree.string(map, FIELD_CONFLICT_TARGET);
        return new OwnCallRecord(kind, description, limit, endsWhen, judgment, conflictTarget);
    }
}
