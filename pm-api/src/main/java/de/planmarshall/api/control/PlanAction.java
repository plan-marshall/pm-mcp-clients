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
 * Closed enumeration of the actions a plan offers, per its state (UC-CTL-1).
 *
 * <p>The wire values are those of the runtime's {@code plan.actions} representation, so a client
 * never decides itself what applies and only renders what the runtime offers.
 */
public enum PlanAction {
    PAUSE("pause"),
    START("start"),
    CANCEL("cancel"),
    ABANDON("abandon"),
    CHAT("chat"),
    SETTINGS("settings");

    private final String value;

    PlanAction(String value) {
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
    public static @Nullable PlanAction fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PlanAction action : values()) {
            if (action.value.equalsIgnoreCase(value) || action.name().equalsIgnoreCase(value)) {
                return action;
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
    public static PlanAction parse(String value) {
        var action = fromValue(value);
        if (action == null) {
            throw new IllegalArgumentException("Unknown PlanAction: " + value);
        }
        return action;
    }
}
