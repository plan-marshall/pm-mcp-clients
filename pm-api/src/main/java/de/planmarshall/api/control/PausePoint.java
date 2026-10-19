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

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of where a plan holds when it is paused ({@code plan.pause-when}, UC-CTL-1).
 *
 * <p>The point is the operator's own choice: {@code now} withdraws what runs, {@code task} lets the
 * current task finish, and {@code phase} runs the current phase to its end. The plan shows the
 * point while it is pausing (state {@code pausing}) and it becomes {@code paused} once it holds.
 */
public enum PausePoint {
    NOW("now"),
    AFTER_TASK("task"),
    AFTER_PHASE("phase");

    private final String value;

    PausePoint(String value) {
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
    public static @Nullable PausePoint fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PausePoint point : values()) {
            if (point.value.equalsIgnoreCase(value) || point.name().equalsIgnoreCase(value)) {
                return point;
            }
        }
        return null;
    }

    /**
     * Resolves an enum constant or throws.
     *
     * @param value wire string
     * @return enum constant
     */
    public static PausePoint parse(String value) {
        var point = fromValue(value);
        if (point == null) {
            throw new IllegalArgumentException("Unknown PausePoint: " + value);
        }
        return point;
    }
}
