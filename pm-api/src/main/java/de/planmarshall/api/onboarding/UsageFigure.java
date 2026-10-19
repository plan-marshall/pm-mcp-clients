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
 * The closed list of token-usage figures a harness reports (Client Profiles, Tokens, Not Money):
 * the five figures of a run that each harness and version reports, reports as not applicable, or
 * does not report at all. It is the "why?" detail behind the {@link TokenUsageCoverage} of a
 * harness, one {@link UsageFigureStatus} per figure.
 *
 * <p>The list is closed and fails closed: a figure read from the wire that is not a member is
 * refused by {@link #parse(String)}. No figure is a price; the figures are counts of tokens.
 */
public enum UsageFigure {

    /** The prompt tokens. */
    INPUT("input"),

    /** The tokens read from the prompt cache. */
    CACHE_READ("cache_read"),

    /** The tokens written to the prompt cache. */
    CACHE_WRITE("cache_write"),

    /** The completion tokens. */
    OUTPUT("output"),

    /** The reasoning tokens, where the harness reports them at all. */
    REASONING("reasoning");

    private final String wireValue;

    UsageFigure(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value of the figure, e.g. {@code cache_write}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a figure from its wire value or its enum name.
     *
     * @param value wire value, e.g. {@code cache_read}, or enum name
     * @return the figure, or {@code null} if unmatched
     */
    public static @Nullable UsageFigure fromWire(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var figure : values()) {
            if (figure.wireValue.equalsIgnoreCase(value) || figure.name().equalsIgnoreCase(value)) {
                return figure;
            }
        }
        return null;
    }

    /**
     * Resolves a figure from its wire value or its enum name.
     *
     * @param value wire value or enum name
     * @return the figure
     * @throws IllegalArgumentException if the value is unknown (fail-closed)
     */
    public static UsageFigure parse(String value) {
        var figure = fromWire(value);
        if (figure == null) {
            throw new IllegalArgumentException("Unknown UsageFigure: " + value);
        }
        return figure;
    }
}
