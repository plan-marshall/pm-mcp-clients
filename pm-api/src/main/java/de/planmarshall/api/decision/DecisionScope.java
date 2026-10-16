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
 * Closed enumeration of decision scopes (UC-PLN-2).
 */
public enum DecisionScope {
    PLAN("plan"),
    EPIC("epic"),
    PROJECT_SETUP("setup"),
    MACHINE("machine");

    private final String value;

    DecisionScope(String value) {
        this.value = value;
    }

    /**
     * Returns the wire value (e.g. {@code "plan"}).
     *
     * @return wire value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves a scope from wire or enum name.
     *
     * @param value wire or enum string
     * @return resolved scope
     * @throws IllegalArgumentException on unknown scope
     */
    public static DecisionScope fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var scope : values()) {
            if (scope.value.equalsIgnoreCase(value) || scope.name().equalsIgnoreCase(value)
                    || ("project_setup".equalsIgnoreCase(value) && scope == PROJECT_SETUP)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown DecisionScope: " + value);
    }
}
