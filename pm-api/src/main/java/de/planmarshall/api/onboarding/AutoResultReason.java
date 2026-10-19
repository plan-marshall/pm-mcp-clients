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
 * The closed list of reasons an item of the automatic run's result carries (UC-ONB-2; State
 * Catalogue, Automatic Onboarding): why the run decided a value the way it did. The item schema of
 * {@code auto.result} is {@link AutoResultItem}, one item per aspect.
 *
 * <p>The list is closed and fails closed: a reason read from the wire that is not a member is
 * refused by {@link #parse(String)}.
 */
public enum AutoResultReason {

    /** The machine's own state decided it (the system language, the found tools, the stored tokens). */
    DETECTED("detected"),

    /** Every supported harness was enrolled, the first one the default. */
    ALL_SUPPORTED("all_supported"),

    /** The shipped default of the aspect decided it. */
    DEFAULT("default"),

    /** The operator was asked, because the run left the aspect open. */
    ASKED("asked");

    private final String wireValue;

    AutoResultReason(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value of the reason, e.g. {@code detected}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return the message key of the reason's line in the result ({@code onboard.auto.reason.*})
     */
    public String titleKey() {
        return "onboard.auto.reason." + wireValue;
    }

    /**
     * Resolves a reason from its wire value or its enum name.
     *
     * @param value wire value, e.g. {@code all_supported}, or enum name
     * @return the reason, or {@code null} if unmatched
     */
    public static @Nullable AutoResultReason fromWire(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var reason : values()) {
            if (reason.wireValue.equalsIgnoreCase(value) || reason.name().equalsIgnoreCase(value)) {
                return reason;
            }
        }
        return null;
    }

    /**
     * Resolves a reason from its wire value or its enum name.
     *
     * @param value wire value or enum name
     * @return the reason
     * @throws IllegalArgumentException if the value is unknown (fail-closed)
     */
    public static AutoResultReason parse(String value) {
        var reason = fromWire(value);
        if (reason == null) {
            throw new IllegalArgumentException("Unknown AutoResultReason: " + value);
        }
        return reason;
    }
}
