/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.project;

import org.jspecify.annotations.Nullable;

/**
 * Status of short landing wait during project release (UC-PRJ-3).
 */
public enum ReleaseWaitStatus {
    NONE("none"),
    WAITING("waiting"),
    TIMEOUT("timeout"),
    COMPLETED("completed");

    private final String value;

    ReleaseWaitStatus(String value) {
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
    public static @Nullable ReleaseWaitStatus fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ReleaseWaitStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
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
    public static ReleaseWaitStatus parse(String value) {
        var status = fromValue(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown ReleaseWaitStatus: " + value);
        }
        return status;
    }
}
