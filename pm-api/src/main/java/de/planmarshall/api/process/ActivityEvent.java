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
 * Event payload emitted when a worker task generates new live activity ({@code activity}).
 *
 * @param planId plan identifier
 * @param taskId task identifier
 * @param entry  new activity entry
 */
public record ActivityEvent(
        String planId,
        String taskId,
        ActivityEntry entry
) {
    public static final String EVENT_NAME = "activity";

    public ActivityEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(entry, "entry");
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("task_id", taskId);
        map.put("entry", entry.toMap());
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static ActivityEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in ActivityEvent");

        var taskId = JsonTree.string(map, "task_id");
        if (taskId == null) taskId = JsonTree.string(map, "taskId");
        if (taskId == null) throw new JsonFormatException("Missing task_id in ActivityEvent");

        var entryObj = JsonTree.asObject(map.get("entry"));
        if (entryObj == null) throw new JsonFormatException("Missing entry in ActivityEvent");

        return new ActivityEvent(planId, taskId, ActivityEntry.fromMap(entryObj));
    }

    public static ActivityEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for ActivityEvent");
        return fromMap(obj);
    }
}
