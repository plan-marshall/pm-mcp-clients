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
 * Event payload emitted when a step is closed ({@code step.closed}).
 *
 * @param planId          plan identifier
 * @param stepId          step identifier
 * @param outcome         outcome ("success", "failure")
 * @param durationSeconds duration in seconds
 */
public record StepClosedEvent(
        String planId,
        String stepId,
        String outcome,
        long durationSeconds
) {
    public static final String EVENT_NAME = "step.closed";

    public StepClosedEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(stepId, "stepId");
        outcome = outcome != null ? outcome : "success";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("step_id", stepId);
        map.put("outcome", outcome);
        map.put("duration_seconds", durationSeconds);
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static StepClosedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in StepClosedEvent");

        var stepId = JsonTree.string(map, "step_id");
        if (stepId == null) stepId = JsonTree.string(map, "stepId");
        if (stepId == null) throw new JsonFormatException("Missing step_id in StepClosedEvent");

        var outcome = JsonTree.string(map, "outcome");
        var durNum = JsonTree.number(map, "duration_seconds");
        long durationSeconds = durNum != null ? durNum.longValue() : 0L;

        return new StepClosedEvent(planId, stepId, outcome != null ? outcome : "success", durationSeconds);
    }

    public static StepClosedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for StepClosedEvent");
        return fromMap(obj);
    }
}
