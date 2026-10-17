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
 * Kind of node within a plan's process tree (UC-PLN-1).
 */
public enum NodeKind {
    PHASE("phase"),
    SUB_WORKFLOW("sub_workflow"),
    STATE("state"),
    JOB("job"),
    TASKS("tasks"),
    WAITS("waits"),
    LEAF("leaf");

    private final String value;

    NodeKind(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static NodeKind fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var kind : values()) {
            if (kind.value.equalsIgnoreCase(value) || kind.name().equalsIgnoreCase(value)) {
                return kind;
            }
        }
        return STATE;
    }
}
