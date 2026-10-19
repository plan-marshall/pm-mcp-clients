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
 * The plan the planning role drafts for a promoted review item ({@code review.draft-plan}, design
 * <em>Reviewing Findings or Watches</em>).
 *
 * @param planId       the id proposed for the staged plan
 * @param objective    the plan's objective, or {@code null}
 * @param deliverables the plan's deliverables
 * @param surface      the plan's expected surface
 * @param dependencies the ids of staged plans that must land before this one starts
 */
public record DraftedPlan(
        String planId,
        @Nullable String objective,
        List<String> deliverables,
        List<String> surface,
        List<String> dependencies
) {

    public DraftedPlan {
        Objects.requireNonNull(planId, "planId");
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
        if (objective != null) {
            map.put("objective", objective);
        }
        map.put("deliverables", deliverables);
        map.put("surface", surface);
        map.put("dependencies", dependencies);
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
     * @return the parsed draft
     * @throws IOException if a required field is missing
     */
    public static DraftedPlan fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in DraftedPlan");
        }
        return new DraftedPlan(planId, JsonTree.string(map, "objective"),
                ControlJson.readStrings(map, "deliverables"),
                ControlJson.readStrings(map, "surface"),
                ControlJson.readStrings(map, "dependencies"));
    }

    /**
     * @param json the JSON object string
     * @return the parsed draft
     * @throws IOException if the value is not an object
     */
    public static DraftedPlan fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DraftedPlan");
        }
        return fromMap(obj);
    }
}
