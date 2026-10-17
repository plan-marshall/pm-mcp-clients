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
 * Job representation under a leaf state (UC-PLN-1).
 *
 * @param kind            job kind (e.g. "build")
 * @param status          job status ("running", "done", "failed", "waits_slot")
 * @param outcome         job outcome ("success", "failure"), or {@code null}
 * @param durationSeconds duration in seconds
 * @param waitsSlot       whether job waits for a build slot
 */
public record LeafJob(
        String kind,
        String status,
        @Nullable String outcome,
        long durationSeconds,
        boolean waitsSlot
) {
    public LeafJob {
        kind = kind != null ? kind : "job";
        status = status != null ? status : "done";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("kind", kind);
        map.put("status", status);
        if (outcome != null) {
            map.put("outcome", outcome);
        }
        map.put("duration_seconds", durationSeconds);
        if (waitsSlot) {
            map.put("waits_slot", true);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static LeafJob fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kind = JsonTree.string(map, "kind");
        var status = JsonTree.string(map, "status");
        var outcome = JsonTree.string(map, "outcome");
        var durNum = JsonTree.number(map, "duration_seconds");
        long durationSeconds = durNum != null ? durNum.longValue() : 0L;
        var waitsSlot = Boolean.TRUE.equals(map.get("waits_slot"));

        return new LeafJob(kind != null ? kind : "job", status != null ? status : "done", outcome, durationSeconds, waitsSlot);
    }

    public static LeafJob fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LeafJob");
        }
        return fromMap(obj);
    }
}
