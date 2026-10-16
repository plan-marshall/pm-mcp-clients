/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.system;

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
 * Event payload emitted when runtime enters or updates draining state ({@code runtime.draining}).
 */
public record RuntimeDrainingEvent(
        String who,
        int remainingCount,
        List<String> remainingItems,
        boolean withdrawable,
        @Nullable String startedAt
) {
    public static final String EVENT_NAME = "runtime.draining";

    public RuntimeDrainingEvent {
        Objects.requireNonNull(who, "who");
        remainingItems = remainingItems != null ? List.copyOf(remainingItems) : List.of();
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("who", who);
        map.put("remaining_count", remainingCount);
        map.put("remaining_items", remainingItems);
        map.put("withdrawable", withdrawable);
        if (startedAt != null) {
            map.put("started_at", startedAt);
        }
        return map;
    }

    /**
     * Serializes to JSON.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed event
     * @throws IOException on error
     */
    public static RuntimeDrainingEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var who = JsonTree.string(map, "who");
        if (who == null) {
            throw new JsonFormatException("Missing who in RuntimeDrainingEvent");
        }
        var remVal = JsonTree.integer(map, "remaining_count");
        var remainingCount = remVal != null ? remVal.intValue() : 0;
        var remainingItems = new ArrayList<String>();
        if (map.get("remaining_items") instanceof List<?> list) {
            for (var item : list) {
                if (item != null) remainingItems.add(String.valueOf(item));
            }
        }
        var withdrawable = Boolean.TRUE.equals(map.get("withdrawable"));
        var startedAt = JsonTree.string(map, "started_at");
        return new RuntimeDrainingEvent(who, remainingCount, remainingItems, withdrawable, startedAt);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static RuntimeDrainingEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RuntimeDrainingEvent");
        }
        return fromMap(obj);
    }
}
