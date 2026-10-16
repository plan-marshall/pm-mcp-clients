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

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of a plan's operational activity states.
 */
public enum PlanActivity {
    RUNNING("running"),
    PAUSING("pausing"),
    PAUSED("paused"),
    WAITING_FOR_YOU("waiting_for_you"),
    HELD("held");

    private final String value;

    PlanActivity(String value) {
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
    public static @Nullable PlanActivity fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PlanActivity activity : values()) {
            if (activity.value.equalsIgnoreCase(value) || activity.name().equalsIgnoreCase(value)) {
                return activity;
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
    public static PlanActivity parse(String value) {
        var activity = fromValue(value);
        if (activity == null) {
            throw new IllegalArgumentException("Unknown PlanActivity: " + value);
        }
        return activity;
    }
}
