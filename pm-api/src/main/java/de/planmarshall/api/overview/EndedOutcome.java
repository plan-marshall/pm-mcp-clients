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
 * Closed enumeration of outcomes for an ended plan.
 */
public enum EndedOutcome {
    MERGED("merged"),
    NO_CHANGE("no_change"),
    HANDED_BACK("handed_back"),
    RETURNED("returned"),
    CANCELLED("cancelled"),
    ABANDONED("abandoned"),
    RELEASED("released"),
    ABORTED("aborted");

    private final String value;

    EndedOutcome(String value) {
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
    public static @Nullable EndedOutcome fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (EndedOutcome outcome : values()) {
            if (outcome.value.equalsIgnoreCase(value) || outcome.name().equalsIgnoreCase(value)) {
                return outcome;
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
    public static EndedOutcome parse(String value) {
        var outcome = fromValue(value);
        if (outcome == null) {
            throw new IllegalArgumentException("Unknown EndedOutcome: " + value);
        }
        return outcome;
    }
}
