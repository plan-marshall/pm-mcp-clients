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
 * A plan's own values per aspect with their origin and the settings revision
 * ({@code plan-settings.open}, UC-CTL-5).
 *
 * <p>The revision travels with every dialogue: a change is sent against the revision the dialogue
 * was opened with, and the first change wins.
 *
 * @param planId   the plan the settings belong to
 * @param revision the settings revision
 * @param values   the plan's values per aspect
 */
public record PlanSettingsRecord(
        String planId,
        int revision,
        List<PlanSettingsValue> values
) {
    public PlanSettingsRecord {
        Objects.requireNonNull(planId, "planId");
        values = values != null ? List.copyOf(values) : List.of();
    }

    /**
     * @param aspect the aspect
     * @return the value of the aspect, or {@code null} if the plan does not carry it
     */
    public @Nullable PlanSettingsValue value(String aspect) {
        for (var entry : values) {
            if (entry.aspect().equals(aspect)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("revision", revision);
        map.put("values", values.stream().map(PlanSettingsValue::toMap).toList());
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON tree map.
     *
     * @param map map representation
     * @return parsed record
     * @throws IOException on format failure
     */
    public static PlanSettingsRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            planId = JsonTree.string(map, "id");
        }
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in PlanSettingsRecord");
        }
        var revision = JsonTree.integer(map, "revision");
        var values = new ArrayList<PlanSettingsValue>();
        var list = JsonTree.asArray(map.get("values"));
        if (list != null) {
            for (var element : list) {
                var entry = JsonTree.asObject(element);
                if (entry != null) {
                    values.add(PlanSettingsValue.fromMap(entry));
                }
            }
        }
        return new PlanSettingsRecord(planId, revision != null ? revision : 0, values);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed record
     * @throws IOException on format failure
     */
    public static PlanSettingsRecord fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PlanSettingsRecord");
        }
        return fromMap(obj);
    }
}
