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
 * Closed enumeration of how a plan ended (UC-CTL-1, UC-CTL-4), design <em>How a Plan Ends</em>.
 *
 * <p>Wherever ended work is shown, a plan names its outcome from this list; an epic names its end
 * from {@link EpicTerminalReason}. The wire value is the key {@code overview.ended.outcome} uses.
 */
public enum PlanOutcome {
    MERGED("merged"),
    NO_CHANGE("no_change"),
    HANDED_BACK("handed_back"),
    RETURNED("returned"),
    CANCELLED("cancelled"),
    ABANDONED("abandoned"),
    RELEASED("released"),
    ABORTED("aborted");

    private final String value;

    PlanOutcome(String value) {
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
    public static @Nullable PlanOutcome fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PlanOutcome outcome : values()) {
            if (outcome.value.equalsIgnoreCase(value) || outcome.name().equalsIgnoreCase(value)) {
                return outcome;
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
    public static PlanOutcome parse(String value) {
        var outcome = fromValue(value);
        if (outcome == null) {
            throw new IllegalArgumentException("Unknown PlanOutcome: " + value);
        }
        return outcome;
    }
}
