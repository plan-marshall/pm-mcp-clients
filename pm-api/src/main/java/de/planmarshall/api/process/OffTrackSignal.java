/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.process;

import org.jspecify.annotations.Nullable;

/**
 * Diagnostic signal indicating a worker was detected as off track (UC-PLN-1).
 */
public enum OffTrackSignal {
    REPEATING("repeating"),
    NO_PROGRESS("no_progress"),
    FAILING_AGAIN("failing_again"),
    EDIT_CHURN("edit_churn"),
    AGENT_STARTED("agent_started");

    private final String value;

    OffTrackSignal(String value) {
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
    public static @Nullable OffTrackSignal fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var s : values()) {
            if (s.value.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
                return s;
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
    public static OffTrackSignal parse(String value) {
        var signal = fromValue(value);
        if (signal == null) {
            throw new IllegalArgumentException("Unknown OffTrackSignal: " + value);
        }
        return signal;
    }
}
