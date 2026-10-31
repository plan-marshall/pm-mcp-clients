/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import org.jspecify.annotations.Nullable;

/**
 * The closed kinds of a match of finding work by name (UC-PLN-8), in the order the runtime serves
 * them within running work and within ended work.
 */
public enum FindKind {
    /** A project of the machine. */
    PROJECT("project"),
    /** An epic of a project. */
    EPIC("epic"),
    /** A plan of a project or an epic. */
    PLAN("plan"),
    /** An open decision of a plan, an epic, or a project's setup. */
    DECISION("decision"),
    /** A report to the operator. */
    REPORT("report");

    private final String value;

    FindKind(String value) {
        this.value = value;
    }

    /**
     * @return the wire string value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves a kind from its wire string value.
     *
     * @param value the wire string
     * @return the kind, or {@code null} if unmatched
     */
    public static @Nullable FindKind fromValue(@Nullable String value) {
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
     * Resolves a kind or throws.
     *
     * @param value the wire string
     * @return the kind
     */
    public static FindKind parse(String value) {
        var kind = fromValue(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown FindKind: " + value);
        }
        return kind;
    }
}
