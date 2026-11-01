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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Worker activity payload for a leaf task (UC-PLN-1).
 *
 * @param taskId          task identifier (e.g. "TASK-003")
 * @param role            worker role (e.g. "implement")
 * @param harness         agent harness (e.g. "claude")
 * @param model           model name (e.g. "sonnet")
 * @param attempt         attempt count
 * @param durationSeconds elapsed seconds
 * @param tokensSoFar     tokens consumed by task so far
 * @param entries         chronological list of activity entries (newest last)
 * @param offTrack        off-track status info, or {@code null}
 * @param serverOnly      true if harness emits no step stream
 * @param gapCount        number of older entries trimmed due to journal limits
 */
public record TaskActivity(
        String taskId,
        String role,
        String harness,
        String model,
        int attempt,
        long durationSeconds,
        long tokensSoFar,
        List<ActivityEntry> entries,
        @Nullable OffTrackInfo offTrack,
        boolean serverOnly,
        int gapCount
) {
    public TaskActivity {
        Objects.requireNonNull(taskId, "taskId");
        role = role != null ? role : "implement";
        harness = harness != null ? harness : "agent";
        model = model != null ? model : "default";
        entries = entries != null ? List.copyOf(entries) : List.of();
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("task_id", taskId);
        map.put("role", role);
        map.put("harness", harness);
        map.put("model", model);
        map.put("attempt", attempt);
        map.put("duration_seconds", durationSeconds);
        map.put("tokens_so_far", tokensSoFar);
        var entriesList = new ArrayList<Map<String, Object>>();
        for (var e : entries) {
            entriesList.add(e.toMap());
        }
        map.put("entries", entriesList);
        if (offTrack != null) {
            map.put("off_track", offTrack.toMap());
        }
        if (serverOnly) {
            map.put("server_only", true);
        }
        if (gapCount > 0) {
            map.put("gap_count", gapCount);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static TaskActivity fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var taskId = JsonTree.string(map, "task_id");
        if (taskId == null) {
            taskId = JsonTree.string(map, "id");
        }
        if (taskId == null) {
            throw new JsonFormatException("Missing task_id in TaskActivity");
        }
        var role = JsonTree.string(map, "role");
        var harness = JsonTree.string(map, "harness");
        var model = JsonTree.string(map, "model");

        var attemptNum = JsonTree.number(map, "attempt");
        int attempt = attemptNum != null ? attemptNum.intValue() : 1;

        var durNum = JsonTree.number(map, "duration_seconds");
        long durationSeconds = durNum != null ? durNum.longValue() : 0L;

        var tokNum = JsonTree.number(map, "tokens_so_far");
        long tokensSoFar = tokNum != null ? tokNum.longValue() : 0L;

        var entries = new ArrayList<ActivityEntry>();
        if (map.get("entries") instanceof List<?> list) {
            for (var item : list) {
                var entryMap = JsonTree.asObject(item);
                if (entryMap != null) {
                    entries.add(ActivityEntry.fromMap(entryMap));
                }
            }
        }

        OffTrackInfo offTrack = null;
        var offTrackObj = JsonTree.asObject(map.get("off_track"));
        if (offTrackObj != null) {
            offTrack = OffTrackInfo.fromMap(offTrackObj);
        }

        var serverOnly = Boolean.TRUE.equals(map.get("server_only"));
        var gapNum = JsonTree.number(map, "gap_count");
        int gapCount = gapNum != null ? gapNum.intValue() : 0;

        return new TaskActivity(
                taskId,
                role != null ? role : "implement",
                harness != null ? harness : "agent",
                model != null ? model : "default",
                attempt,
                durationSeconds,
                tokensSoFar,
                entries,
                offTrack,
                serverOnly,
                gapCount
        );
    }

    public static TaskActivity fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for TaskActivity");
        }
        return fromMap(obj);
    }
}
