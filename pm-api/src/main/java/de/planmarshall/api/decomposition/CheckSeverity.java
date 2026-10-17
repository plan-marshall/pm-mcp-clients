/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decomposition;

import java.util.Arrays;

/**
 * How severe a decomposition check finding is (UC-STA-6).
 * <p>
 * Only an {@link #ERROR} keeps the cut from being agreed; a {@link #WARNING} and an {@link #INFO}
 * are shown beside the plan and never block the commit.
 */
public enum CheckSeverity {

    /** A note that never blocks the commit. */
    INFO("info"),

    /** A concern the operator should see; it never blocks the commit. */
    WARNING("warning"),

    /** A failing check ({@code decomposition.check}); the cut cannot be agreed. */
    ERROR("error");

    private final String wireValue;

    CheckSeverity(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a severity from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching severity
     * @throws IllegalArgumentException if no severity carries that value
     */
    public static CheckSeverity fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Check severity must not be null");
        }
        var clean = value.trim();
        return Arrays.stream(values())
                .filter(severity -> severity.wireValue.equalsIgnoreCase(clean))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown check severity: " + value));
    }
}
