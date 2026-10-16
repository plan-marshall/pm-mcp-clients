/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a plan's operational activity changes ({@code activity.changed}).
 *
 * @param planId   plan identifier
 * @param activity activity state ("running", "pausing", "paused", "waiting_for_you", "held")
 * @param point    point of pause ("task", "phase"), or {@code null}
 */
public record ActivityChangedEvent(
        String planId,
        String activity,
        @Nullable String point
) {
    public static final String EVENT_NAME = "activity.changed";

    public ActivityChangedEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(activity, "activity");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("activity", activity);
        if (point != null) {
            map.put("point", point);
        }
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON tree map.
     *
     * @param map map representation
     * @return parsed event
     * @throws IOException on format failure
     */
    public static ActivityChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            planId = JsonTree.string(map, "planId");
        }
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in ActivityChangedEvent");
        }

        var activity = JsonTree.string(map, "activity");
        if (activity == null) {
            activity = JsonTree.string(map, "state");
        }
        if (activity == null) {
            throw new JsonFormatException("Missing activity in ActivityChangedEvent");
        }

        var point = JsonTree.string(map, "point");
        if (point == null) {
            point = JsonTree.string(map, "pausing_point");
        }

        return new ActivityChangedEvent(planId, activity, point);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static ActivityChangedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ActivityChangedEvent");
        }
        return fromMap(obj);
    }
}
