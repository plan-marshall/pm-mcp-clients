/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.process;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Token usage summary for a plan so far (UC-PLN-1).
 *
 * @param total         total tokens (input + output)
 * @param input         input tokens
 * @param output        output tokens
 * @param floor         whether total is a lower-bound floor (some tasks unmeasured)
 * @param tasksTotal    total tasks in plan
 * @param tasksMeasured measured tasks
 * @param noneYet       true if no model calls have happened yet
 * @param notMeasured   true if model calls occurred but no tasks were measured
 */
public record ProcessTokens(
        long total,
        long input,
        long output,
        boolean floor,
        int tasksTotal,
        int tasksMeasured,
        boolean noneYet,
        boolean notMeasured
) {
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("total", total);
        map.put("input", input);
        map.put("output", output);
        if (floor) {
            map.put("floor", true);
        }
        map.put("tasks_total", tasksTotal);
        map.put("tasks_measured", tasksMeasured);
        if (noneYet) {
            map.put("none_yet", true);
        }
        if (notMeasured) {
            map.put("not_measured", true);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static ProcessTokens fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var totNum = JsonTree.number(map, "total");
        long total = totNum != null ? totNum.longValue() : 0L;

        var inNum = JsonTree.number(map, "input");
        long input = inNum != null ? inNum.longValue() : 0L;

        var outNum = JsonTree.number(map, "output");
        long output = outNum != null ? outNum.longValue() : 0L;

        var floor = Boolean.TRUE.equals(map.get("floor"));

        var tTotalNum = JsonTree.number(map, "tasks_total");
        int tasksTotal = tTotalNum != null ? tTotalNum.intValue() : 0;

        var tMeasNum = JsonTree.number(map, "tasks_measured");
        int tasksMeasured = tMeasNum != null ? tMeasNum.intValue() : 0;

        var noneYet = Boolean.TRUE.equals(map.get("none_yet"));
        var notMeasured = Boolean.TRUE.equals(map.get("not_measured"));

        return new ProcessTokens(total, input, output, floor, tasksTotal, tasksMeasured, noneYet, notMeasured);
    }

    public static ProcessTokens fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ProcessTokens");
        }
        return fromMap(obj);
    }
}
