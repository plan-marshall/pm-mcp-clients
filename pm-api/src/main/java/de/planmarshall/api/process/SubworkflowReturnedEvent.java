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
 * Event payload emitted when a sub-workflow completes and returns ({@code subworkflow.returned}).
 *
 * @param planId          plan identifier
 * @param parentNodeId    parent node identifier
 * @param subworkflowId   identifier of the returned sub-workflow
 * @param state           outcome state ("done", "failed")
 * @param durationSeconds duration in seconds
 */
public record SubworkflowReturnedEvent(
        String planId,
        String parentNodeId,
        String subworkflowId,
        String state,
        long durationSeconds
) {
    public static final String EVENT_NAME = "subworkflow.returned";

    public SubworkflowReturnedEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(parentNodeId, "parentNodeId");
        Objects.requireNonNull(subworkflowId, "subworkflowId");
        state = state != null ? state : "done";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("parent_node_id", parentNodeId);
        map.put("subworkflow_id", subworkflowId);
        map.put("state", state);
        map.put("duration_seconds", durationSeconds);
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static SubworkflowReturnedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in SubworkflowReturnedEvent");

        var parentNodeId = JsonTree.string(map, "parent_node_id");
        if (parentNodeId == null) parentNodeId = JsonTree.string(map, "parentNodeId");
        if (parentNodeId == null) throw new JsonFormatException("Missing parent_node_id in SubworkflowReturnedEvent");

        var subworkflowId = JsonTree.string(map, "subworkflow_id");
        if (subworkflowId == null) subworkflowId = JsonTree.string(map, "subworkflowId");
        if (subworkflowId == null) throw new JsonFormatException("Missing subworkflow_id in SubworkflowReturnedEvent");

        var state = JsonTree.string(map, "state");
        var durNum = JsonTree.number(map, "duration_seconds");
        long durationSeconds = durNum != null ? durNum.longValue() : 0L;

        return new SubworkflowReturnedEvent(planId, parentNodeId, subworkflowId, state != null ? state : "done", durationSeconds);
    }

    public static SubworkflowReturnedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for SubworkflowReturnedEvent");
        return fromMap(obj);
    }
}
