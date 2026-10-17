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
 * Event payload emitted when a process node is left ({@code state.left}).
 *
 * @param planId          plan identifier
 * @param nodeId          node identifier
 * @param state           terminal state of the node ("done", "failed")
 * @param run             run count
 * @param durationSeconds duration in seconds
 */
public record StateLeftEvent(
        String planId,
        String nodeId,
        String state,
        int run,
        long durationSeconds
) {
    public static final String EVENT_NAME = "state.left";

    public StateLeftEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(nodeId, "nodeId");
        state = state != null ? state : "done";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("node_id", nodeId);
        map.put("state", state);
        map.put("run", run);
        map.put("duration_seconds", durationSeconds);
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static StateLeftEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in StateLeftEvent");

        var nodeId = JsonTree.string(map, "node_id");
        if (nodeId == null) nodeId = JsonTree.string(map, "nodeId");
        if (nodeId == null) throw new JsonFormatException("Missing node_id in StateLeftEvent");

        var state = JsonTree.string(map, "state");
        var runNum = JsonTree.number(map, "run");
        int run = runNum != null ? runNum.intValue() : 1;

        var durNum = JsonTree.number(map, "duration_seconds");
        long durationSeconds = durNum != null ? durNum.longValue() : 0L;

        return new StateLeftEvent(planId, nodeId, state != null ? state : "done", run, durationSeconds);
    }

    public static StateLeftEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for StateLeftEvent");
        return fromMap(obj);
    }
}
