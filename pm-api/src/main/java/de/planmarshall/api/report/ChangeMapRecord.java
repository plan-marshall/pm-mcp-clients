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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The change map of a plan as the runtime rendered it: a fixed-width text diagram and the state of
 * its completeness.
 *
 * <p>The diagram is carried verbatim. A client shows it as delivered, in a fixed-width font, and
 * never redraws or re-wraps it.
 *
 * @param diagram the fixed-width text diagram, its lines separated by {@code \n}
 * @param state   completeness of the map ({@code "ok"}, {@code "partial"}, or {@code "indeterminate"})
 * @param cause   why the map is not {@code "ok"} ({@code null} when it is)
 */
public record ChangeMapRecord(
        String diagram,
        String state,
        @Nullable String cause
) {
    private static final String FIELD_DIAGRAM = "diagram";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_CAUSE = "cause";

    public ChangeMapRecord {
        Objects.requireNonNull(diagram, FIELD_DIAGRAM);
        Objects.requireNonNull(state, FIELD_STATE);
        var normalized = state.toLowerCase(Locale.ROOT);
        if (("partial".equals(normalized) || "indeterminate".equals(normalized))
                && (cause == null || cause.isBlank())) {
            throw new IllegalArgumentException("A " + normalized + " change map requires its cause");
        }
        if ("ok".equals(normalized) && cause != null && !cause.isBlank()) {
            throw new IllegalArgumentException("A complete change map carries no cause");
        }
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_DIAGRAM, diagram);
        map.put(FIELD_STATE, state);
        if (cause != null) map.put(FIELD_CAUSE, cause);
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed change map
     * @throws IOException when the diagram or the state is missing
     */
    public static ChangeMapRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var diagram = JsonTree.string(map, FIELD_DIAGRAM);
        if (diagram == null) diagram = JsonTree.string(map, "text");
        if (diagram == null) throw new JsonFormatException("Missing diagram in ChangeMapRecord");
        var state = JsonTree.string(map, FIELD_STATE);
        if (state == null) throw new JsonFormatException("Missing state in ChangeMapRecord");
        var cause = JsonTree.string(map, FIELD_CAUSE);
        return new ChangeMapRecord(diagram, state, cause);
    }
}
