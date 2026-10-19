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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One plan of an epic's decomposition draft, as the control contract shows it (UC-STA-6, design
 * <em>Starting Work, The Decomposition</em>): its objective, deliverables, expected surface,
 * dependencies, and order, with the change marker of a later run.
 *
 * @param planId       the plan id
 * @param title        the plan's title
 * @param objective    the plan's objective, or {@code null}
 * @param deliverables the plan's deliverables
 * @param surface      the files or directories the plan is expected to change
 * @param dependencies the ids of plans that must land before this one starts
 * @param order        the plan's position within its workstream, from {@code 1}
 * @param change       the change marker of a later run, or {@code null}
 * @param locked       {@code true} if the decomposition may not touch the plan
 */
public record DecompositionPlan(
        String planId,
        String title,
        @Nullable String objective,
        List<String> deliverables,
        List<String> surface,
        List<String> dependencies,
        int order,
        @Nullable String change,
        boolean locked
) {

    public DecompositionPlan {
        Objects.requireNonNull(planId, "planId");
        title = title != null ? title : planId;
        deliverables = deliverables != null ? List.copyOf(deliverables) : List.of();
        surface = surface != null ? List.copyOf(surface) : List.of();
        dependencies = dependencies != null ? List.copyOf(dependencies) : List.of();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("title", title);
        if (objective != null) {
            map.put("objective", objective);
        }
        map.put("deliverables", deliverables);
        map.put("surface", surface);
        map.put("dependencies", dependencies);
        map.put("order", order);
        if (change != null) {
            map.put("change", change);
        }
        map.put("locked", locked);
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
     * @return the parsed plan
     * @throws IOException if a required field is missing
     */
    public static DecompositionPlan fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in DecompositionPlan");
        }
        var order = ControlJson.optInteger(map, "order");
        return new DecompositionPlan(
                planId,
                JsonTree.string(map, "title"),
                JsonTree.string(map, "objective"),
                ControlJson.readStrings(map, "deliverables"),
                ControlJson.readStrings(map, "surface"),
                ControlJson.readStrings(map, "dependencies"),
                order != null ? order : 0,
                JsonTree.string(map, "change"),
                ControlJson.bool(map, "locked"));
    }

    /**
     * @param json the JSON object string
     * @return the parsed plan
     * @throws IOException if the value is not an object
     */
    public static DecompositionPlan fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DecompositionPlan");
        }
        return fromMap(obj);
    }
}
