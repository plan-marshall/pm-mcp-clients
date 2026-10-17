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
 * Event payload emitted when a process node is entered ({@code state.entered}).
 *
 * @param planId    plan identifier
 * @param nodeId    node identifier
 * @param run       run count
 * @param timestamp time entered
 */
public record StateEnteredEvent(
        String planId,
        String nodeId,
        int run,
        String timestamp
) {
    public static final String EVENT_NAME = "state.entered";

    public StateEnteredEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(nodeId, "nodeId");
        timestamp = timestamp != null ? timestamp : "";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("node_id", nodeId);
        map.put("run", run);
        map.put("timestamp", timestamp);
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static StateEnteredEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in StateEnteredEvent");

        var nodeId = JsonTree.string(map, "node_id");
        if (nodeId == null) nodeId = JsonTree.string(map, "nodeId");
        if (nodeId == null) throw new JsonFormatException("Missing node_id in StateEnteredEvent");

        var runNum = JsonTree.number(map, "run");
        int run = runNum != null ? runNum.intValue() : 1;

        var timestamp = JsonTree.string(map, "timestamp");
        if (timestamp == null) timestamp = JsonTree.string(map, "time");

        return new StateEnteredEvent(planId, nodeId, run, timestamp != null ? timestamp : "");
    }

    public static StateEnteredEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for StateEnteredEvent");
        return fromMap(obj);
    }
}
