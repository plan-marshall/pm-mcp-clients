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
 * What a harness and version report of its token usage (Client Profiles, Tokens, Not Money):
 * complete, partial (naming what is missing), or not reported.
 */
public enum TokenUsageCoverage {

    /** Every figure the profile needs is reported. */
    COMPLETE("complete"),

    /** Some figures are missing; the missing ones are named. */
    PARTIAL("partial"),

    /** The harness reports no figures at all. */
    NOT_REPORTED("not_reported");

    private final String wireValue;

    TokenUsageCoverage(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @param value wire value or enum name
     * @return the coverage, or {@code null} if unmatched
     */
    public static @Nullable TokenUsageCoverage fromWire(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var coverage : values()) {
            if (coverage.wireValue.equalsIgnoreCase(value) || coverage.name().equalsIgnoreCase(value)) {
                return coverage;
            }
        }
        return null;
    }
}
