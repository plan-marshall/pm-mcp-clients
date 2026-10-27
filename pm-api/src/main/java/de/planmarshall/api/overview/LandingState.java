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
 * Closed enumeration of a landing's state, covering the two carriers that report one: the
 * {@code state} of an {@link AlongsideLanding} ({@code running}, {@code failed}) and the terminal
 * {@code status} of a placing ({@code written}, {@code committed}, {@code landed}).
 * <p>
 * A placing that ended without committing or landing is the failure the hold {@code landing_failed}
 * names; a placing still at {@code written} is in progress, not a failure.
 */
public enum LandingState {
    RUNNING("running"),
    FAILED("failed"),
    WRITTEN("written"),
    COMMITTED("committed"),
    LANDED("landed");

    private final String value;

    LandingState(String value) {
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
    public static @Nullable LandingState fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var state : values()) {
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
    public static LandingState parse(String value) {
        var state = fromValue(value);
        if (state == null) {
            throw new IllegalArgumentException("Unknown LandingState: " + value);
        }
        return state;
    }
}
