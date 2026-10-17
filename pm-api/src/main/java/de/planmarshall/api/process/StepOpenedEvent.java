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
 * Event payload emitted when a step is opened ({@code step.opened}).
 *
 * @param planId    plan identifier
 * @param stepId    step identifier
 * @param timestamp time opened
 */
public record StepOpenedEvent(
        String planId,
        String stepId,
        String timestamp
) {
    public static final String EVENT_NAME = "step.opened";

    public StepOpenedEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(stepId, "stepId");
        timestamp = timestamp != null ? timestamp : "";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("step_id", stepId);
        map.put("timestamp", timestamp);
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static StepOpenedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in StepOpenedEvent");

        var stepId = JsonTree.string(map, "step_id");
        if (stepId == null) stepId = JsonTree.string(map, "stepId");
        if (stepId == null) throw new JsonFormatException("Missing step_id in StepOpenedEvent");

        var timestamp = JsonTree.string(map, "timestamp");

        return new StepOpenedEvent(planId, stepId, timestamp != null ? timestamp : "");
    }

    public static StepOpenedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for StepOpenedEvent");
        return fromMap(obj);
    }
}
