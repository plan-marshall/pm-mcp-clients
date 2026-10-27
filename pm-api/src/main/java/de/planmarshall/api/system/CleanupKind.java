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

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of the kind of item a cleanup run removes or keeps, the wire value of
 * {@link CleanupItemEntry#kind()} and {@link CleanupKeptItem#kind()}.
 * <p>
 * The lifecycle of the run itself is {@link CleanupJobState}, a different axis.
 */
public enum CleanupKind {
    PLAN("plan"),
    EPIC("epic"),
    LOGS("logs"),
    READ_REPORTS("read_reports"),
    UNENROLLED_PROJECT("unenrolled_project");

    private final String value;

    CleanupKind(String value) {
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
    public static @Nullable CleanupKind fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var kind : values()) {
            if (kind.value.equalsIgnoreCase(value) || kind.name().equalsIgnoreCase(value)) {
                return kind;
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
    public static CleanupKind parse(String value) {
        var kind = fromValue(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown CleanupKind: " + value);
        }
        return kind;
    }
}
