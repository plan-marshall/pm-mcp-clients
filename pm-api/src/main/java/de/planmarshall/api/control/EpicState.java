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
 * Closed enumeration of an epic's state, the wire value of {@link EpicResource#state()}.
 * <p>
 * The state of a plan is {@link de.planmarshall.api.overview.PlanActivity}; the reason an ended epic
 * names is {@link EpicTerminalReason}.
 */
public enum EpicState {
    RUNNING("running"),
    WAITING_FOR_YOU("waiting_for_you"),
    HELD("held"),
    LANDING("landing"),
    ENDED("ended");

    private final String value;

    EpicState(String value) {
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
    public static @Nullable EpicState fromValue(@Nullable String value) {
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
    public static EpicState parse(String value) {
        var state = fromValue(value);
        if (state == null) {
            throw new IllegalArgumentException("Unknown EpicState: " + value);
        }
        return state;
    }
}
