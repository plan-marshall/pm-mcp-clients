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
 * Closed enumeration of cleanup job lifecycle states.
 */
public enum CleanupJobState {
    OPEN("open"),
    POSTPONED("postponed"),
    DELETING("deleting"),
    DONE("done");

    private final String value;

    CleanupJobState(String value) {
        this.value = value;
    }

    /**
     * Wire value for JSON.
     *
     * @return state string
     */
    public String value() {
        return value;
    }

    /**
     * Resolves cleanup job state from string.
     *
     * @param value wire representation
     * @return resolved state
     */
    public static CleanupJobState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return OPEN;
        }
        for (var s : values()) {
            if (s.value.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return OPEN;
    }

    @Override
    public String toString() {
        return value;
    }
}
