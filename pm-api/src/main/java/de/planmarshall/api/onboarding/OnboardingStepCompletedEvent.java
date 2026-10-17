/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Broadcast when an aspect of the machine onboarding completes, so both clients advance together
 * (UC-ONB-1; Both Clients, One State).
 *
 * @param aspect         the aspect that completed
 * @param state          the state the onboarding moved to
 * @param completedSteps the completed step count after the aspect
 */
public record OnboardingStepCompletedEvent(
        OnboardingAspect aspect,
        String state,
        int completedSteps
) {
    public OnboardingStepCompletedEvent {
        Objects.requireNonNull(aspect, "aspect");
        state = state != null && !state.isBlank() ? state : "onboard.completed";
    }

    /**
     * @return the SSE event name
     */
    public static String eventName() {
        return "onboarding.step_completed";
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("aspect", aspect.wireValue());
        map.put("state", state);
        map.put("completed_steps", completedSteps);
        return map;
    }

    /**
     * Serializes to a JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map map representation
     * @return parsed event
     * @throws IOException on format failure
     */
    public static OnboardingStepCompletedEvent fromMap(Map<String, Object> map) throws IOException {
        var aspect = OnboardingAspect.fromWire(JsonTree.string(map, "aspect"));
        if (aspect == null) {
            throw new JsonFormatException("Missing or unknown aspect in OnboardingStepCompletedEvent");
        }
        var steps = JsonTree.integer(map, "completed_steps");
        return new OnboardingStepCompletedEvent(aspect, JsonTree.string(map, "state"),
                steps != null ? steps : 0);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static OnboardingStepCompletedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for OnboardingStepCompletedEvent");
        }
        return fromMap(obj);
    }
}
