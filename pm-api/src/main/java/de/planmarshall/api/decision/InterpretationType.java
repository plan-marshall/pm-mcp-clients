/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decision;

import java.util.Objects;

/**
 * Result kind of interpreting an operator's own words (UC-PLN-2).
 */
public enum InterpretationType {
    OPTION("option"),
    OPTION_WITH_CONDITION("option_with_condition"),
    OWN_OPTION("own_option"),
    DELEGATION("delegation"),
    UNCLEAR("unclear");

    private final String value;

    InterpretationType(String value) {
        this.value = value;
    }

    /**
     * Returns the wire value (e.g. {@code "option"}).
     *
     * @return wire value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves InterpretationType from wire or enum name.
     *
     * @param value wire or enum string
     * @return resolved interpretation type
     * @throws IllegalArgumentException on unknown value
     */
    public static InterpretationType fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var t : values()) {
            if (t.value.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown InterpretationType: " + value);
    }
}
