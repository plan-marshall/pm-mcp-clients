/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.project;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Representation of a project whose onboarding is in progress before enrolment (UC-PRJ-1, UC-PRJ-2).
 *
 * @param checkoutPath filesystem path of the checkout
 * @param currentStep  name of the current onboarding step (e.g. "Identity")
 * @param stepNumber   1-based index of current step (e.g. 2)
 * @param totalSteps   total steps in onboarding (e.g. 10)
 * @param continueLink hypermedia link to continue onboarding
 * @param abandonLink  hypermedia link to abandon onboarding
 */
public record BeingAddedProject(
        String checkoutPath,
        String currentStep,
        int stepNumber,
        int totalSteps,
        @Nullable String continueLink,
        @Nullable String abandonLink
) {
    public static final String FIELD_CHECKOUT_PATH = "checkout_path";
    public static final String FIELD_CURRENT_STEP = "current_step";
    public static final String FIELD_STEP_NUMBER = "step_number";
    public static final String FIELD_TOTAL_STEPS = "total_steps";
    public static final String FIELD_CONTINUE_LINK = "continue_link";
    public static final String FIELD_ABANDON_LINK = "abandon_link";

    public BeingAddedProject {
        Objects.requireNonNull(checkoutPath, "checkoutPath");
        currentStep = currentStep != null ? currentStep : "Checkout";
        if (totalSteps <= 0) {
            totalSteps = 10;
        }
        if (stepNumber <= 0) {
            stepNumber = 1;
        }
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CHECKOUT_PATH, checkoutPath);
        map.put(FIELD_CURRENT_STEP, currentStep);
        map.put(FIELD_STEP_NUMBER, stepNumber);
        map.put(FIELD_TOTAL_STEPS, totalSteps);
        if (continueLink != null) {
            map.put(FIELD_CONTINUE_LINK, continueLink);
        }
        if (abandonLink != null) {
            map.put(FIELD_ABANDON_LINK, abandonLink);
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
     * @param map input map
     * @return parsed being added project
     * @throws IOException on format failure
     */
    public static BeingAddedProject fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var checkoutPath = JsonTree.string(map, FIELD_CHECKOUT_PATH);
        if (checkoutPath == null) {
            checkoutPath = JsonTree.string(map, "path");
        }
        if (checkoutPath == null || checkoutPath.isBlank()) {
            throw new JsonFormatException("Missing checkout_path in BeingAddedProject");
        }

        var currentStep = JsonTree.string(map, FIELD_CURRENT_STEP);
        if (currentStep == null) {
            currentStep = JsonTree.string(map, "step");
        }
        if (currentStep == null) {
            currentStep = "Checkout";
        }

        var stepNum = JsonTree.integer(map, FIELD_STEP_NUMBER);
        if (stepNum == null) {
            stepNum = JsonTree.integer(map, "step_num");
        }
        int stepNumber = stepNum != null ? stepNum : 1;

        var total = JsonTree.integer(map, FIELD_TOTAL_STEPS);
        if (total == null) {
            total = JsonTree.integer(map, "total");
        }
        int totalSteps = total != null ? total : 10;

        var continueLink = JsonTree.string(map, FIELD_CONTINUE_LINK);
        var abandonLink = JsonTree.string(map, FIELD_ABANDON_LINK);

        return new BeingAddedProject(checkoutPath, currentStep, stepNumber, totalSteps, continueLink, abandonLink);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed being added project
     * @throws IOException on format failure
     */
    public static BeingAddedProject fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for BeingAddedProject");
    }
}
