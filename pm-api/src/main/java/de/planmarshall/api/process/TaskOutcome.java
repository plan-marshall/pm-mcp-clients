/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.process;

import java.util.Objects;

/**
 * Outcome of a completed task in a leaf node (UC-PLN-1).
 */
public enum TaskOutcome {
    SUCCESS("success"),
    FAILURE("failure"),
    YIELDED("yielded");

    private final String value;

    TaskOutcome(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static TaskOutcome fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var o : values()) {
            if (o.value.equalsIgnoreCase(value) || o.name().equalsIgnoreCase(value)) {
                return o;
            }
        }
        return SUCCESS;
    }
}
