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
 * Closed enumeration of what holds a plan, an epic, or a landing (UC-CTL-4).
 *
 * <p>A hold is exactly one of these five kinds; a paused plan is no hold, since pausing is the
 * operator's own choice and the plan shows it by its state.
 */
public enum HoldKind {
    STALLED("stalled"),
    CONDITION("condition"),
    INTEGRITY_PAUSE("integrity_pause"),
    HELD("held"),
    LANDING_FAILED("landing_failed");

    private final String value;

    HoldKind(String value) {
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
    public static @Nullable HoldKind fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (HoldKind kind : values()) {
            if (kind.value.equalsIgnoreCase(value) || kind.name().equalsIgnoreCase(value)) {
                return kind;
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
    public static HoldKind parse(String value) {
        var kind = fromValue(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown HoldKind: " + value);
        }
        return kind;
    }
}
