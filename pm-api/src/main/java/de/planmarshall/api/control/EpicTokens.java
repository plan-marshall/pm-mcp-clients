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
 * An epic's tokens so far, with the split (design <em>Epic View</em>, specification <em>Tokens, Not
 * Money</em>).
 *
 * <p>The total is the sum of the epic's plans and of its own tasks (decompose, landing, review).
 * Where some tasks were measured and others not, the total is a floor; where model calls happened
 * but no task was measured, {@link #total()} is {@code not measured} with its coverage.
 *
 * @param total the epic's total figure
 * @param own   the figure of the epic's own tasks
 * @param plans the figure per plan, one step away
 */
public record EpicTokens(
        TokenFigure total,
        TokenFigure own,
        List<PlanTokenFigure> plans
) {

    public EpicTokens {
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(own, "own");
        plans = plans != null ? List.copyOf(plans) : List.of();
    }

    /**
     * @return {@code true} where model calls happened and no task was measured
     */
    public boolean notMeasured() {
        return !total.measured();
    }

    /**
     * @return the count of tasks whose usage was not measured
     */
    public int notMeasuredCount() {
        return total.notMeasuredTasks();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("total", total.toMap());
        map.put("own", own.toMap());
        map.put("plans", plans.stream().map(PlanTokenFigure::toMap).toList());
        map.put("not_measured", notMeasured());
        map.put("not_measured_count", notMeasuredCount());
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
     * @return the parsed tokens
     * @throws IOException if a required field is missing
     */
    public static EpicTokens fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var total = JsonTree.asObject(map.get("total"));
        if (total == null) {
            throw new JsonFormatException("Missing total in EpicTokens");
        }
        return new EpicTokens(TokenFigure.fromMap(total), own(map), readPlans(map.get("plans")));
    }

    /**
     * @param json the JSON object string
     * @return the parsed tokens
     * @throws IOException if the value is not an object
     */
    public static EpicTokens fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EpicTokens");
        }
        return fromMap(obj);
    }

    private static TokenFigure own(Map<String, Object> map) throws IOException {
        var own = JsonTree.asObject(map.get("own"));
        return own != null ? TokenFigure.fromMap(own) : TokenFigure.notMeasured();
    }

    private static List<PlanTokenFigure> readPlans(@Nullable Object raw) throws IOException {
        var plans = new ArrayList<PlanTokenFigure>();
        for (var object : ControlJson.readObjects(raw)) {
            plans.add(PlanTokenFigure.fromMap(object));
        }
        return plans;
    }
}
