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
 * Closed enumeration of readiness states for an enrolled project (UC-PRJ-2).
 */
public enum ProjectReadiness {
    READY("ready"),
    SETUP_NEEDS("setup_needs"),
    RELEASED("released");

    private final String value;

    ProjectReadiness(String value) {
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
    public static @Nullable ProjectReadiness fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ProjectReadiness readiness : values()) {
            if (readiness.value.equalsIgnoreCase(value) || readiness.name().equalsIgnoreCase(value)) {
                return readiness;
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
    public static ProjectReadiness parse(String value) {
        var readiness = fromValue(value);
        if (readiness == null) {
            throw new IllegalArgumentException("Unknown ProjectReadiness: " + value);
        }
        return readiness;
    }
}
