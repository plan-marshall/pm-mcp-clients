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
 * Event payload emitted when a sub-workflow is invoked ({@code subworkflow.called}).
 *
 * @param planId        plan identifier
 * @param parentNodeId  parent node invoking the sub-workflow
 * @param subworkflowId identifier of the called sub-workflow
 * @param timestamp     time called
 */
public record SubworkflowCalledEvent(
        String planId,
        String parentNodeId,
        String subworkflowId,
        String timestamp
) {
    public static final String EVENT_NAME = "subworkflow.called";

    public SubworkflowCalledEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(parentNodeId, "parentNodeId");
        Objects.requireNonNull(subworkflowId, "subworkflowId");
        timestamp = timestamp != null ? timestamp : "";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("parent_node_id", parentNodeId);
        map.put("subworkflow_id", subworkflowId);
        map.put("timestamp", timestamp);
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static SubworkflowCalledEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in SubworkflowCalledEvent");

        var parentNodeId = JsonTree.string(map, "parent_node_id");
        if (parentNodeId == null) parentNodeId = JsonTree.string(map, "parentNodeId");
        if (parentNodeId == null) throw new JsonFormatException("Missing parent_node_id in SubworkflowCalledEvent");

        var subworkflowId = JsonTree.string(map, "subworkflow_id");
        if (subworkflowId == null) subworkflowId = JsonTree.string(map, "subworkflowId");
        if (subworkflowId == null) throw new JsonFormatException("Missing subworkflow_id in SubworkflowCalledEvent");

        var timestamp = JsonTree.string(map, "timestamp");

        return new SubworkflowCalledEvent(planId, parentNodeId, subworkflowId, timestamp != null ? timestamp : "");
    }

    public static SubworkflowCalledEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for SubworkflowCalledEvent");
        return fromMap(obj);
    }
}
