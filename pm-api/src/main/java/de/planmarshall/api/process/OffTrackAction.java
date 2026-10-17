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
 * Action taken by the runtime server when a worker is judged off-track (UC-PLN-1).
 */
public enum OffTrackAction {
    SHOW("show"),
    NUDGE("nudge"),
    YIELD("yield"),
    RESTART("restart");

    private final String value;

    OffTrackAction(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OffTrackAction fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var a : values()) {
            if (a.value.equalsIgnoreCase(value) || a.name().equalsIgnoreCase(value)) {
                return a;
            }
        }
        return SHOW;
    }
}
