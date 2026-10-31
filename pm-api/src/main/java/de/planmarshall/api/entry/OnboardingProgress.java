/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.entry;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The progress of the machine's onboarding as the runtime status reports it: the state the
 * onboarding stopped in, the step reached, the number of steps, and whether it is complete
 * (01-processes-and-transport.adoc#local-api, the runtime status).
 *
 * @param state     the onboarding state the machine is in, or {@code null} when none is open
 * @param step      the zero-based step reached
 * @param steps     the number of steps of the onboarding
 * @param completed whether the onboarding has completed
 */
public record OnboardingProgress(
        @Nullable String state,
        int step,
        int steps,
        boolean completed
) {

    /** The number of steps of the machine's onboarding when the status names none. */
    public static final int DEFAULT_STEPS = 7;

    private static final String FIELD_STATE = "state";
    private static final String FIELD_STEP = "step";
    private static final String FIELD_STEPS = "steps";
    private static final String FIELD_COMPLETED_STEPS = "completed_steps";
    private static final String FIELD_COMPLETED = "completed";
    private static final String COMPLETED_STATE = "onboard.completed";

    public OnboardingProgress {
        if (step < 0) {
            throw new IllegalArgumentException("step must not be negative: " + step);
        }
        if (steps < 0) {
            throw new IllegalArgumentException("steps must not be negative: " + steps);
        }
        if (step > steps && steps > 0) {
            throw new IllegalArgumentException("step " + step + " exceeds steps " + steps);
        }
    }

    /**
     * @return the progress of a completed onboarding
     */
    public static OnboardingProgress complete() {
        return new OnboardingProgress(COMPLETED_STATE, DEFAULT_STEPS, DEFAULT_STEPS, true);
    }

    /**
     * @return the progress of an onboarding that has not started
     */
    public static OnboardingProgress notStarted() {
        return new OnboardingProgress(null, 0, DEFAULT_STEPS, false);
    }

    /**
     * Converts to a JSON tree map, omitting an absent state.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (state != null) {
            map.put(FIELD_STATE, state);
        }
        map.put(FIELD_STEP, step);
        map.put(FIELD_STEPS, steps);
        map.put(FIELD_COMPLETED, completed);
        return map;
    }

    /**
     * @return compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this progress to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses onboarding progress from a JSON tree map.
     *
     * @param map the input map
     * @return the progress
     * @throws IOException if a member violates the record's contract
     */
    public static OnboardingProgress fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var state = JsonTree.string(map, FIELD_STATE);
        var step = JsonTree.integer(map, FIELD_STEP);
        if (step == null) {
            step = JsonTree.integer(map, FIELD_COMPLETED_STEPS);
        }
        var steps = JsonTree.integer(map, FIELD_STEPS);
        var completed = Boolean.TRUE.equals(JsonTree.bool(map, FIELD_COMPLETED))
                || COMPLETED_STATE.equals(state);
        try {
            return new OnboardingProgress(
                    state,
                    step != null ? step : (completed ? DEFAULT_STEPS : 0),
                    steps != null ? steps : DEFAULT_STEPS,
                    completed);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid OnboardingProgress: " + e.getMessage(), e);
        }
    }

    /**
     * Parses onboarding progress from a JSON string.
     *
     * @param json the JSON string
     * @return the progress
     * @throws IOException on parse failure
     */
    public static OnboardingProgress fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for OnboardingProgress");
        }
        return fromMap(obj);
    }

    /**
     * Reads onboarding progress from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the progress
     * @throws IOException on parse failure
     */
    public static OnboardingProgress readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for OnboardingProgress");
        }
        return fromMap(obj);
    }
}
