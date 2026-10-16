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
 * Answer types supported by a decision part (UC-PLN-2).
 */
public enum AnswerType {
    ONE_OPTION("one_option"),
    SEVERAL_OPTIONS("several_options"),
    VALUE("value");

    private final String value;

    AnswerType(String value) {
        this.value = value;
    }

    /**
     * Returns the wire value (e.g. {@code "one_option"}).
     *
     * @return wire value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves AnswerType from wire or enum name.
     *
     * @param value wire or enum string
     * @return resolved answer type
     * @throws IllegalArgumentException on unknown value
     */
    public static AnswerType fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var t : values()) {
            if (t.value.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown AnswerType: " + value);
    }
}
