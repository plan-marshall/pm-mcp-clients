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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Where the machine's onboarding stands (UC-ONB-1): the current state, the aspect it belongs to,
 * the aspects already completed, and whether the automatic run is in progress.
 *
 * @param state            the current state id, e.g. {@code onboard.mode} or {@code harness.choose}
 * @param aspect           the aspect the state belongs to, or {@code null} before the run
 * @param completedAspects the aspects already completed, in the order of the first run
 * @param auto             {@code true} while the automatic run is in progress
 * @param revision         the revision of the recorded state
 */
public record MachineOnboardingState(
        String state,
        @Nullable OnboardingAspect aspect,
        List<OnboardingAspect> completedAspects,
        boolean auto,
        int revision
) {
    public MachineOnboardingState {
        state = state != null && !state.isBlank() ? state : "onboard.mode";
        completedAspects = completedAspects != null ? List.copyOf(completedAspects) : List.of();
    }

    /**
     * @return the completed step count, 0-based
     */
    public int completedSteps() {
        return completedAspects.size();
    }

    /**
     * @return {@code true} when the onboarding has reached its resting state
     */
    public boolean completed() {
        return "onboard.completed".equals(state);
    }

    /**
     * @param name a state id
     * @return the aspect the state belongs to, or {@code null}
     */
    public static @Nullable OnboardingAspect aspectOf(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (var aspect : OnboardingAspect.values()) {
            if (name.startsWith(aspect.wireValue() + ".")) {
                return aspect;
            }
        }
        return null;
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("state", state);
        if (aspect != null) {
            map.put("aspect", aspect.wireValue());
        }
        map.put("completed_aspects", completedAspects.stream().map(OnboardingAspect::wireValue).toList());
        map.put("completed_steps", completedSteps());
        map.put("auto", auto);
        map.put("revision", revision);
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
     * @return parsed state
     * @throws IOException on format failure
     */
    public static MachineOnboardingState fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var state = JsonTree.string(map, "state");
        var completed = new ArrayList<OnboardingAspect>();
        var list = JsonTree.asArray(map.get("completed_aspects"));
        if (list != null) {
            for (var element : list) {
                if (element instanceof String text) {
                    var aspect = OnboardingAspect.fromWire(text);
                    if (aspect != null) {
                        completed.add(aspect);
                    }
                }
            }
        }
        var revision = JsonTree.integer(map, "revision");
        return new MachineOnboardingState(
                state,
                OnboardingAspect.fromWire(JsonTree.string(map, "aspect")),
                completed,
                Boolean.TRUE.equals(JsonTree.bool(map, "auto")),
                revision != null ? revision : 0);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed state
     * @throws IOException on format failure
     */
    public static MachineOnboardingState fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for MachineOnboardingState");
        }
        return fromMap(obj);
    }
}
