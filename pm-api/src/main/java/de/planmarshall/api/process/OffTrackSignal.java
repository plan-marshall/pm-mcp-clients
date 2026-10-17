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
 * Diagnostic signal indicating a worker was detected as off track (UC-PLN-1).
 */
public enum OffTrackSignal {
    REPEATING("repeating"),
    NO_PROGRESS("no_progress"),
    OVER_SIZE("over_size"),
    OUTSIDE_FOOTPRINT("outside_footprint");

    private final String value;

    OffTrackSignal(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OffTrackSignal fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var s : values()) {
            if (s.value.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return REPEATING;
    }
}
