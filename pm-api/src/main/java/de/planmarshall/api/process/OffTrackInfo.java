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
 * Off-track diagnosis and action details for a worker task (UC-PLN-1).
 *
 * @param signal    diagnostic signal (e.g. "repeating", "no_progress", "over_size", "outside_footprint")
 * @param action    action taken (e.g. "show", "nudge", "yield", "restart")
 * @param value     measured metric value, or {@code null}
 * @param threshold configured threshold triggering the signal, or {@code null}
 */
public record OffTrackInfo(
        String signal,
        String action,
        @Nullable Integer value,
        @Nullable Integer threshold
) {
    public OffTrackInfo {
        Objects.requireNonNull(signal, "signal");
        action = action != null ? action : "show";
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("signal", signal);
        map.put("action", action);
        if (value != null) {
            map.put("value", value);
        }
        if (threshold != null) {
            map.put("threshold", threshold);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static OffTrackInfo fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var signal = JsonTree.string(map, "signal");
        if (signal == null) {
            throw new JsonFormatException("Missing signal in OffTrackInfo");
        }
        var action = JsonTree.string(map, "action");
        var valNum = JsonTree.number(map, "value");
        Integer value = valNum != null ? valNum.intValue() : null;
        var threshNum = JsonTree.number(map, "threshold");
        Integer threshold = threshNum != null ? threshNum.intValue() : null;

        return new OffTrackInfo(signal, action != null ? action : "show", value, threshold);
    }

    public static OffTrackInfo fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for OffTrackInfo");
        }
        return fromMap(obj);
    }
}
