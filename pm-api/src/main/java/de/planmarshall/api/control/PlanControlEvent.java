/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A server-sent event of a plan's own state changing under an operator act (UC-CTL-1).
 *
 * <p>The event catalogue names {@code plan.paused}, {@code plan.resumed}, and
 * {@code plan.cancelled}; the one carrying the change names it in {@link #event()}.
 *
 * @param event  the event name
 * @param planId the plan
 * @param state  the plan's state after the act, or {@code null}
 * @param point  the pause point when it pausing, or {@code null}
 * @param reason the reason of a cancel, or {@code null}
 */
public record PlanControlEvent(
        String event,
        String planId,
        @Nullable String state,
        @Nullable String point,
        @Nullable String reason
) {

    /** Event name when a plan holds at its point. */
    public static final String EVENT_PAUSED = "plan.paused";

    /** Event name when a plan runs on from where it rests. */
    public static final String EVENT_RESUMED = "plan.resumed";

    /** Event name when a plan is cancelled or abandoned. */
    public static final String EVENT_CANCELLED = "plan.cancelled";

    public PlanControlEvent {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(planId, "planId");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        if (state != null) {
            map.put("state", state);
        }
        if (point != null) {
            map.put("point", point);
        }
        if (reason != null) {
            map.put("reason", reason);
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
     * @param event the event name
     * @param map   map representation
     * @return parsed event
     * @throws IOException on format failure
     */
    public static PlanControlEvent fromMap(String event, Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            planId = JsonTree.string(map, "id");
        }
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in PlanControlEvent");
        }
        return new PlanControlEvent(event, planId,
                JsonTree.string(map, "state"),
                JsonTree.string(map, "point"),
                JsonTree.string(map, "reason"));
    }

    /**
     * Parses from JSON string.
     *
     * @param event the event name
     * @param json  JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static PlanControlEvent fromJson(String event, String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PlanControlEvent");
        }
        return fromMap(event, obj);
    }
}
