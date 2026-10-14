/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.scenario;

import java.util.Objects;

/**
 * Definition of an automatic state transition that occurs without an incoming request.
 *
 * @param afterMs delay in milliseconds relative to state entry
 * @param next    the target state id
 */
public record AutoTransitionDefinition(long afterMs, String next) {

    /**
     * Compact constructor validating required fields.
     */
    public AutoTransitionDefinition {
        if (afterMs < 0) {
            throw new IllegalArgumentException("afterMs cannot be negative: " + afterMs);
        }
        Objects.requireNonNull(next, "next");
    }
}
