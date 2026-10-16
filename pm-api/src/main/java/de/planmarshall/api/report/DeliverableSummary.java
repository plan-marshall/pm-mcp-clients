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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonTree;

/**
 * Summary of a plan deliverable in a completion report.
 *
 * @param name  the deliverable name or title
 * @param state state of deliverable: {@code "done"}, {@code "not_done"}, or {@code "dropped"}
 */
public record DeliverableSummary(String name, String state) {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_STATE = "state";

    public DeliverableSummary {
        Objects.requireNonNull(name, FIELD_NAME);
        Objects.requireNonNull(state, FIELD_STATE);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_NAME, name);
        map.put(FIELD_STATE, state);
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed deliverable summary
     */
    public static DeliverableSummary fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var name = JsonTree.string(map, FIELD_NAME);
        if (name == null) name = JsonTree.string(map, "title");
        if (name == null) name = "";
        var state = JsonTree.string(map, FIELD_STATE);
        if (state == null) state = "done";
        return new DeliverableSummary(name, state);
    }
}
