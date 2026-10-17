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
 * Task representation under a leaf state (UC-PLN-1).
 *
 * @param taskId          unique identifier of the task (e.g. "TASK-003")
 * @param role            assigned role (e.g. "implement")
 * @param harness         worker harness (e.g. "claude")
 * @param model           worker model (e.g. "sonnet")
 * @param attempt         attempt number
 * @param status          status string ("running", "done", "waiting", "failed", "planned")
 * @param outcome         outcome string ("success", "failure", "yielded"), or {@code null}
 * @param durationSeconds duration in seconds
 * @param tokensSoFar     tokens consumed by task so far
 * @param activityLink    hypermedia link to worker activity, or {@code null}
 * @param offTrack        off-track diagnostic info, or {@code null}
 */
public record LeafTask(
        String taskId,
        String role,
        String harness,
        String model,
        int attempt,
        String status,
        @Nullable String outcome,
        long durationSeconds,
        long tokensSoFar,
        @Nullable String activityLink,
        @Nullable OffTrackInfo offTrack
) {
    public LeafTask {
        Objects.requireNonNull(taskId, "taskId");
        role = role != null ? role : "implement";
        harness = harness != null ? harness : "agent";
        model = model != null ? model : "default";
        status = status != null ? status : "planned";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("task_id", taskId);
        map.put("role", role);
        map.put("harness", harness);
        map.put("model", model);
        map.put("attempt", attempt);
        map.put("status", status);
        if (outcome != null) {
            map.put("outcome", outcome);
        }
        map.put("duration_seconds", durationSeconds);
        map.put("tokens_so_far", tokensSoFar);
        if (activityLink != null) {
            map.put("activity_link", activityLink);
        }
        if (offTrack != null) {
            map.put("off_track", offTrack.toMap());
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static LeafTask fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var taskId = JsonTree.string(map, "task_id");
        if (taskId == null) {
            taskId = JsonTree.string(map, "id");
        }
        if (taskId == null) {
            throw new JsonFormatException("Missing task_id in LeafTask");
        }
        var role = JsonTree.string(map, "role");
        var harness = JsonTree.string(map, "harness");
        var model = JsonTree.string(map, "model");

        var attemptNum = JsonTree.number(map, "attempt");
        int attempt = attemptNum != null ? attemptNum.intValue() : 1;

        var status = JsonTree.string(map, "status");
        var outcome = JsonTree.string(map, "outcome");

        var durNum = JsonTree.number(map, "duration_seconds");
        long durationSeconds = durNum != null ? durNum.longValue() : 0L;

        var tokNum = JsonTree.number(map, "tokens_so_far");
        long tokensSoFar = tokNum != null ? tokNum.longValue() : 0L;

        var activityLink = JsonTree.string(map, "activity_link");

        OffTrackInfo offTrack = null;
        var offTrackObj = JsonTree.asObject(map.get("off_track"));
        if (offTrackObj != null) {
            offTrack = OffTrackInfo.fromMap(offTrackObj);
        }

        return new LeafTask(
                taskId,
                role != null ? role : "implement",
                harness != null ? harness : "agent",
                model != null ? model : "default",
                attempt,
                status != null ? status : "planned",
                outcome,
                durationSeconds,
                tokensSoFar,
                activityLink,
                offTrack
        );
    }

    public static LeafTask fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LeafTask");
        }
        return fromMap(obj);
    }
}
