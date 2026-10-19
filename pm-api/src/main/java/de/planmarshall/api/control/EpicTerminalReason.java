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
 * Closed enumeration of how an epic ended (UC-CTL-2, UC-CTL-3), design <em>How a Plan Ends</em>.
 *
 * <p>An epic ends {@code completed} (archived because it is through), {@code cancelled} (the
 * operator's <em>Cancel</em>), or {@code released} (the release of its project, which ends it
 * together with its plans). The wire value is the key {@code overview.ended.epic_end} uses.
 */
public enum EpicTerminalReason {
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    RELEASED("released");

    private final String value;

    EpicTerminalReason(String value) {
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
    public static @Nullable EpicTerminalReason fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (EpicTerminalReason reason : values()) {
            if (reason.value.equalsIgnoreCase(value) || reason.name().equalsIgnoreCase(value)) {
                return reason;
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
    public static EpicTerminalReason parse(String value) {
        var reason = fromValue(value);
        if (reason == null) {
            throw new IllegalArgumentException("Unknown EpicTerminalReason: " + value);
        }
        return reason;
    }
}
