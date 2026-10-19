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
import java.util.Map;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One token figure of a plan, an epic's own tasks, or the whole epic (design <em>Epic View</em>,
 * specification <em>Tokens, Not Money</em>).
 *
 * <p>Tokens are never money and an epic carries no budget of its own. Where some tasks were
 * measured and others not, the figure is a floor ({@link #floor}) with its coverage; where model
 * calls happened but no task's usage was measured it is {@code not_measured} with the count of the
 * tasks that were not measured, never {@code 0} and never a floor.
 *
 * @param total           the sum of the tokens used across the measured tasks
 * @param input           the input tokens of the sum
 * @param output          the output tokens of the sum
 * @param measured        {@code false} where no task's usage was measured
 * @param floor           {@code true} where the figure is a lower bound ({@code >=})
 * @param measuredTasks   the tasks whose usage was measured
 * @param notMeasuredTasks the tasks that were not measured
 */
public record TokenFigure(
        long total,
        long input,
        long output,
        boolean measured,
        boolean floor,
        int measuredTasks,
        int notMeasuredTasks
) {

    /**
     * @return a figure that records model calls whose usage was not measured
     */
    public static TokenFigure notMeasured() {
        return new TokenFigure(0, 0, 0, false, false, 0, 0);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("total", total);
        map.put("input", input);
        map.put("output", output);
        map.put("measured", measured);
        map.put("floor", floor);
        map.put("measured_tasks", measuredTasks);
        map.put("not_measured_tasks", notMeasuredTasks);
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
     * @return the parsed figure
     * @throws IOException if the value is not an object
     */
    public static TokenFigure fromMap(Map<String, Object> map) throws IOException {
        if (map == null) {
            throw new JsonFormatException("Expected object for TokenFigure");
        }
        var total = JsonTree.number(map, "total");
        var input = JsonTree.number(map, "input");
        var output = JsonTree.number(map, "output");
        var measuredTasks = ControlJson.optInteger(map, "measured_tasks");
        var notMeasuredTasks = ControlJson.optInteger(map, "not_measured_tasks");
        return new TokenFigure(
                total != null ? total.longValue() : 0L,
                input != null ? input.longValue() : 0L,
                output != null ? output.longValue() : 0L,
                ControlJson.bool(map, "measured"),
                ControlJson.bool(map, "floor"),
                measuredTasks != null ? measuredTasks : 0,
                notMeasuredTasks != null ? notMeasuredTasks : 0);
    }

    /**
     * @param json the JSON object string
     * @return the parsed figure
     * @throws IOException if the value is not an object
     */
    public static TokenFigure fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for TokenFigure");
        }
        return fromMap(obj);
    }
}
