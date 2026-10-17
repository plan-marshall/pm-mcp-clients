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
 * The outcome of a consent state: the operator granted it for this run or remembered it, or
 * declined it ({@code no}).
 */
public enum ConsentStatus {

    /** {@code yes}: the action runs this time. */
    GRANTED("granted"),

    /** {@code yes, and don't ask again}: the action runs and the consent is remembered. */
    REMEMBERED("remembered"),

    /** {@code no}: nothing is looked at, nothing is run, nothing is recorded. */
    DECLINED("declined");

    private final String wireValue;

    ConsentStatus(String wireValue) {
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
     * @return the status, or {@code null} if unmatched
     */
    public static @Nullable ConsentStatus fromWire(@Nullable String value) {
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
}
