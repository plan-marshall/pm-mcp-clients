/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.system;

/**
 * Closed enumeration of the time windows for model metrics and system aggregations.
 */
public enum SystemWindow {
    HOUR("hour"),
    TODAY("today"),
    SEVEN_DAYS("7d");

    private final String value;

    SystemWindow(String value) {
        this.value = value;
    }

    /**
     * Wire value for query parameter and JSON representation.
     *
     * @return window string
     */
    public String value() {
        return value;
    }

    /**
     * Resolves an enum value from string representation.
     *
     * @param value wire representation
     * @return resolved window
     */
    public static SystemWindow fromValue(String value) {
        if (value == null || value.isBlank()) {
            return HOUR;
        }
        for (var w : values()) {
            if (w.value.equalsIgnoreCase(value) || w.name().equalsIgnoreCase(value)
                    || ("week".equalsIgnoreCase(value) && w == SEVEN_DAYS)) {
                return w;
            }
        }
        return HOUR;
    }

    @Override
    public String toString() {
        return value;
    }
}
