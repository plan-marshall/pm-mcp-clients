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

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of states for project onboarding (UC-PRJ-1).
 */
public enum ProjectOnboardingState {
    CHECKOUT("checkout"),
    CONSENT("consent"),
    DETECT("detect"),
    PROBE("probe"),
    IDENTITY("identity"),
    ENROLMENT("enrolment");

    private final String value;

    ProjectOnboardingState(String value) {
        this.value = value;
    }

    /**
     * @return the wire string value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves an enum constant from its wire string value.
     *
     * @param value wire string
     * @return enum constant, or {@code null} if unmatched
     */
    public static @Nullable ProjectOnboardingState fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ProjectOnboardingState state : values()) {
            if (state.value.equalsIgnoreCase(value) || state.name().equalsIgnoreCase(value)) {
                return state;
            }
        }
        return null;
    }

    /**
     * Resolves an enum constant or throws IllegalArgumentException.
     *
     * @param value wire string
     * @return enum constant
     */
    public static ProjectOnboardingState parse(String value) {
        var state = fromValue(value);
        if (state == null) {
            throw new IllegalArgumentException("Unknown ProjectOnboardingState: " + value);
        }
        return state;
    }
}
