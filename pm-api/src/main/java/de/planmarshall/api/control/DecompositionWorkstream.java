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
 * One workstream of a decomposition draft (UC-STA-6): a named group of plans in order.
 *
 * @param id          the workstream id ({@code WS-1}, {@code WS-2}, ...)
 * @param name        the workstream's name
 * @param description what the workstream covers in one line, or {@code null}
 * @param order       the workstream's position in the cut, from {@code 1}
 * @param plans       the plans of the workstream, in order
 */
public record DecompositionWorkstream(
        String id,
        String name,
        @Nullable String description,
        int order,
        List<DecompositionPlan> plans
) {

    public DecompositionWorkstream {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        plans = plans != null ? List.copyOf(plans) : List.of();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("name", name);
        if (description != null) {
            map.put("description", description);
        }
        map.put("order", order);
        map.put("plans", plans.stream().map(DecompositionPlan::toMap).toList());
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
     * @return the parsed workstream
     * @throws IOException if a required field is missing
     */
    public static DecompositionWorkstream fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, "id");
        var name = JsonTree.string(map, "name");
        if (id == null || name == null) {
            throw new JsonFormatException("Missing required fields for DecompositionWorkstream: id, name");
        }
        var order = ControlJson.optInteger(map, "order");
        return new DecompositionWorkstream(id, name, JsonTree.string(map, "description"),
                order != null ? order : 0, readPlans(map));
    }

    /**
     * @param json the JSON object string
     * @return the parsed workstream
     * @throws IOException if the value is not an object
     */
    public static DecompositionWorkstream fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DecompositionWorkstream");
        }
        return fromMap(obj);
    }

    private static List<DecompositionPlan> readPlans(Map<String, Object> map) throws IOException {
        var plans = new ArrayList<DecompositionPlan>();
        for (var object : ControlJson.readObjects(map, "plans")) {
            plans.add(DecompositionPlan.fromMap(object));
        }
        return plans;
    }
}
