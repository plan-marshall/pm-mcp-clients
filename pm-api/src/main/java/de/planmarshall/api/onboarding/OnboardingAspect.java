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

import org.jspecify.annotations.Nullable;

/**
 * The seven aspects of the machine onboarding, in the fixed order of the first run
 * (UC-ONB-3 to UC-ONB-9, state catalogue {@code onboard.language} to
 * {@code onboard.plan-defaults}).
 *
 * <p>Each aspect is a compiled sub-workflow of the machine scope, called with
 * {@code onboard_run: first_run} in the guided run and with {@code onboard_run: reconfigure}
 * for a later run from the machine's settings.
 */
public enum OnboardingAspect {

    /** The interface and model languages ({@code language}). */
    LANGUAGE("language", 1, "onboard.step.language"),

    /** The enrolled harnesses and their default ({@code harness}). */
    HARNESSES("harness", 2, "onboard.step.harnesses"),

    /** The models plans may use ({@code catalogue}). */
    MODELS("catalogue", 3, "onboard.step.models"),

    /** The model assigned to every role ({@code roles}). */
    ROLES("roles", 4, "onboard.step.roles"),

    /** The credential store ({@code store}). */
    CREDENTIALS("store", 5, "onboard.step.credentials"),

    /** The machine's providers ({@code providers}). */
    PROVIDERS("providers", 6, "onboard.step.providers"),

    /** How plans run by default ({@code defaults}). */
    PLANS("defaults", 7, "onboard.step.plans");

    private final String wireValue;
    private final int step;
    private final String stepNameKey;

    OnboardingAspect(String wireValue, int step, String stepNameKey) {
        this.wireValue = wireValue;
        this.step = step;
        this.stepNameKey = stepNameKey;
    }

    /**
     * @return the wire value used in the aspect's state ids
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return the position of the aspect in the fixed order of the first run, 1-based
     */
    public int step() {
        return step;
    }

    /**
     * @return the message key of the aspect's step name ({@code onboard.step.*})
     */
    public String stepNameKey() {
        return stepNameKey;
    }

    /**
     * Resolves an aspect from its wire value or its enum name.
     *
     * @param value wire value, e.g. {@code harness}, or enum name, e.g. {@code HARNESSES}
     * @return the aspect, or {@code null} if unmatched
     */
    public static @Nullable OnboardingAspect fromWire(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var aspect : values()) {
            if (aspect.wireValue.equalsIgnoreCase(value) || aspect.name().equalsIgnoreCase(value)) {
                return aspect;
            }
        }
        return null;
    }

    /**
     * Resolves an aspect from its wire value or its enum name.
     *
     * @param value wire value or enum name
     * @return the aspect
     * @throws IllegalArgumentException if the value is unknown
     */
    public static OnboardingAspect parse(String value) {
        var aspect = fromWire(value);
        if (aspect == null) {
            throw new IllegalArgumentException("Unknown OnboardingAspect: " + value);
        }
        return aspect;
    }
}
