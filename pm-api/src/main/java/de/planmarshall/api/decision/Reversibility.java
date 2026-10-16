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
 * Reversibility of a decision (UC-PLN-2).
 */
public enum Reversibility {
    REVERSIBLE("reversible"),
    COSTLY("costly"),
    IRREVERSIBLE("irreversible");

    private final String value;

    Reversibility(String value) {
        this.value = value;
    }

    /**
     * Returns the wire value (e.g. {@code "reversible"}).
     *
     * @return wire value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves Reversibility from wire or enum name.
     *
     * @param value wire or enum string
     * @return resolved reversibility
     * @throws IllegalArgumentException on unknown value
     */
    public static Reversibility fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var r : values()) {
            if (r.value.equalsIgnoreCase(value) || r.name().equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown Reversibility: " + value);
    }
}
