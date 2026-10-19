/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import org.jspecify.annotations.Nullable;

/**
 * The closed list of what a harness and version report of one {@link UsageFigure} (Client
 * Profiles, Tokens, Not Money): the figure is reported, declared not applicable to this harness, or
 * not reported at all. A figure that is not applicable does not make the report partial, whereas a
 * figure that is not reported does.
 *
 * <p>The list is closed and fails closed: a status read from the wire that is not a member is
 * refused by {@link #parse(String)}.
 */
public enum UsageFigureStatus {

    /** The harness reports the figure. */
    REPORTED("reported"),

    /** The harness declares the figure not applicable to itself (for example no cache write). */
    NOT_APPLICABLE("not_applicable"),

    /** The harness reports nothing for the figure. */
    NOT_REPORTED("not_reported");

    private final String wireValue;

    UsageFigureStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value of the status, e.g. {@code not_reported}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a status from its wire value or its enum name.
     *
     * @param value wire value, e.g. {@code not_applicable}, or enum name
     * @return the status, or {@code null} if unmatched
     */
    public static @Nullable UsageFigureStatus fromWire(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var status : values()) {
            if (status.wireValue.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Resolves a status from its wire value or its enum name.
     *
     * @param value wire value or enum name
     * @return the status
     * @throws IllegalArgumentException if the value is unknown (fail-closed)
     */
    public static UsageFigureStatus parse(String value) {
        var status = fromWire(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown UsageFigureStatus: " + value);
        }
        return status;
    }
}
