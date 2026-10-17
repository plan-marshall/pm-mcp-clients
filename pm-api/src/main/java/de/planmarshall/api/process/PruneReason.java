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
 * Closed list of reasons why a phase or node was omitted/pruned from the composed plan (UC-PLN-1).
 */
public enum PruneReason {
    LANE_CHOICE("lane_choice"),
    UNNEEDED_STEP("unneeded_step"),
    NOT_IN_PLAN("not_in_plan");

    private final String value;

    PruneReason(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static PruneReason fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var reason : values()) {
            if (reason.value.equalsIgnoreCase(value) || reason.name().equalsIgnoreCase(value)) {
                return reason;
            }
        }
        return NOT_IN_PLAN;
    }
}
