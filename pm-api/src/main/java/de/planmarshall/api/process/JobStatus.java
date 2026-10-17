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
 * Status of a job in a leaf node (UC-PLN-1).
 */
public enum JobStatus {
    RUNNING("running"),
    DONE("done"),
    FAILED("failed"),
    WAITS_SLOT("waits_slot");

    private final String value;

    JobStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static JobStatus fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var s : values()) {
            if (s.value.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return DONE;
    }
}
