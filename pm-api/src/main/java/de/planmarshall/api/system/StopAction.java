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
 * Closed enumeration of when to stop the runtime daemon.
 */
public enum StopAction {
    DRAIN("drain"),
    NOW("now");

    private final String value;

    StopAction(String value) {
        this.value = value;
    }

    /**
     * Wire value for JSON.
     *
     * @return action string
     */
    public String value() {
        return value;
    }

    /**
     * Resolves from string.
     *
     * @param value wire representation
     * @return resolved stop action
     */
    public static StopAction fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DRAIN;
        }
        for (var a : values()) {
            if (a.value.equalsIgnoreCase(value) || a.name().equalsIgnoreCase(value)) {
                return a;
            }
        }
        return DRAIN;
    }

    @Override
    public String toString() {
        return value;
    }
}
