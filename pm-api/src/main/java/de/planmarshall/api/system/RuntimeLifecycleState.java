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
 * Closed enumeration of the runtime daemon's lifecycle states.
 */
public enum RuntimeLifecycleState {
    RUNNING("running"),
    DRAINING("draining"),
    STOPPING("stopping"),
    UPGRADING("upgrading"),
    STOPPED("stopped");

    private final String value;

    RuntimeLifecycleState(String value) {
        this.value = value;
    }

    /**
     * Wire value for JSON payloads and status.
     *
     * @return state string
     */
    public String value() {
        return value;
    }

    /**
     * Resolves from wire string.
     *
     * @param value wire representation
     * @return resolved lifecycle state
     */
    public static RuntimeLifecycleState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return RUNNING;
        }
        for (var state : values()) {
            if (state.value.equalsIgnoreCase(value) || state.name().equalsIgnoreCase(value)) {
                return state;
            }
        }
        return RUNNING;
    }

    @Override
    public String toString() {
        return value;
    }
}
