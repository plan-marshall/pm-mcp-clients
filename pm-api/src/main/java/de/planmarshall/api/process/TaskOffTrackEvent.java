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

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a worker on a task is judged off track or recovers ({@code task.off_track}).
 *
 * @param planId   plan identifier
 * @param taskId   task identifier, or {@code null}
 * @param signal   off-track diagnostic signal (e.g. "repeating"), or {@code null}
 * @param action   action taken (e.g. "show", "nudge", "yield", "restart"), or {@code null}
 * @param offTrack {@code true} if worker is off track, {@code false} if cleared
 */
public record TaskOffTrackEvent(
        String planId,
        @Nullable String taskId,
        @Nullable String signal,
        @Nullable String action,
        boolean offTrack
) {
    public static final String EVENT_NAME = "task.off_track";

    public TaskOffTrackEvent {
        Objects.requireNonNull(planId, "planId");
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        if (taskId != null) {
            map.put("task_id", taskId);
        }
        if (signal != null) {
            map.put("signal", signal);
        }
        if (action != null) {
            map.put("action", action);
        }
        map.put("off_track", offTrack);
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static TaskOffTrackEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in TaskOffTrackEvent");

        var taskId = JsonTree.string(map, "task_id");
        var signal = JsonTree.string(map, "signal");
        var action = JsonTree.string(map, "action");
        var offTrack = Boolean.TRUE.equals(map.get("off_track")) || Boolean.TRUE.equals(map.get("offTrack"));

        return new TaskOffTrackEvent(planId, taskId, signal, action, offTrack);
    }

    public static TaskOffTrackEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for TaskOffTrackEvent");
        return fromMap(obj);
    }
}
