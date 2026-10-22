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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a plan transitions to a new phase ({@code phase.changed}).
 *
 * @param planId         plan identifier
 * @param oldPhase       previous phase name, or {@code null}
 * @param newPhase       new current phase name
 * @param phaseEnteredAt timestamp when new phase was entered
 */
public record PhaseChangedEvent(
        String planId,
        @Nullable String oldPhase,
        String newPhase,
        Instant phaseEnteredAt
) {
    public static final String EVENT_NAME = "phase.changed";

    public PhaseChangedEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(newPhase, "newPhase");
        phaseEnteredAt = phaseEnteredAt != null ? phaseEnteredAt : Instant.now();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        if (oldPhase != null) {
            map.put("old_phase", oldPhase);
        }
        map.put("new_phase", newPhase);
        map.put("phase_entered_at", phaseEnteredAt.toString());
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
    public static PhaseChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            planId = JsonTree.string(map, "planId");
        }
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in PhaseChangedEvent");
        }

        var oldPhase = JsonTree.string(map, "old_phase");
        var newPhase = JsonTree.string(map, "new_phase");
        if (newPhase == null) {
            newPhase = JsonTree.string(map, "phase");
        }
        if (newPhase == null) {
            throw new JsonFormatException("Missing new_phase in PhaseChangedEvent");
        }

        var enteredAtStr = JsonTree.string(map, "phase_entered_at");
        Instant enteredAt = enteredAtStr != null ? Instant.parse(enteredAtStr) : Instant.now();

        return new PhaseChangedEvent(planId, oldPhase, newPhase, enteredAt);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static PhaseChangedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PhaseChangedEvent");
        }
        return fromMap(obj);
    }
}
