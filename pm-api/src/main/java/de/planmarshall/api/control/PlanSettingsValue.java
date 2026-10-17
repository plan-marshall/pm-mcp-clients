/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One value of a plan's settings with its origin ({@code plan-settings.open}, UC-CTL-5).
 *
 * @param aspect  the aspect, e.g. {@code landing}, {@code role.implement}, {@code provider.issues}
 * @param value   the value in force, or {@code null} where none is set
 * @param origin  where the value comes from: {@code plan}, {@code project}, {@code team},
 *                {@code machine}, {@code derived}, or {@code default}
 * @param choices the values the aspect admits, in wire order, never {@code null}
 */
public record PlanSettingsValue(
        String aspect,
        @Nullable String value,
        String origin,
        List<String> choices
) {
    public PlanSettingsValue {
        Objects.requireNonNull(aspect, "aspect");
        origin = origin != null && !origin.isBlank() ? origin : "default";
        choices = choices != null ? List.copyOf(choices) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("aspect", aspect);
        if (value != null) {
            map.put("value", value);
        }
        map.put("origin", origin);
        if (!choices.isEmpty()) {
            map.put("choices", choices);
        }
        return map;
    }

    /**
     * Parses from JSON tree map.
     *
     * @param map map representation
     * @return parsed value
     * @throws IOException on format failure
     */
    public static PlanSettingsValue fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var aspect = JsonTree.string(map, "aspect");
        if (aspect == null) {
            aspect = JsonTree.string(map, "key");
        }
        if (aspect == null) {
            throw new JsonFormatException("Missing aspect in PlanSettingsValue");
        }
        var choices = new ArrayList<String>();
        var list = JsonTree.asArray(map.get("choices"));
        if (list != null) {
            for (var element : list) {
                if (element instanceof String text) {
                    choices.add(text);
                }
            }
        }
        return new PlanSettingsValue(aspect, JsonTree.string(map, "value"),
                JsonTree.string(map, "origin"), choices);
    }
}
