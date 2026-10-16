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
 * Closed enumeration of worker execution states.
 */
public enum WorkerState {
    BUSY("busy"),
    IDLE("idle"),
    STARTING("starting"),
    RESTARTING("restarting");

    private final String value;

    WorkerState(String value) {
        this.value = value;
    }

    /**
     * Wire value for JSON representation.
     *
     * @return worker state string
     */
    public String value() {
        return value;
    }

    /**
     * Resolves worker state from string.
     *
     * @param value wire representation
     * @return resolved state
     */
    public static WorkerState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return IDLE;
        }
        for (var s : values()) {
            if (s.value.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return IDLE;
    }

    @Override
    public String toString() {
        return value;
    }
}
