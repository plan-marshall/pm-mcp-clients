/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decomposition;

import java.util.Arrays;

/**
 * What the operator's words ask the planning role to do (UC-STA-6).
 * <p>
 * {@code edit} rewrites an existing staged plan, {@code add} drafts a new one, and {@code rewrite}
 * re-drafts a whole decomposition or a plan the role proposed again.
 */
public enum InstructAction {

    /** Rewrite an existing staged plan. */
    EDIT("edit"),

    /** Draft a new staged plan. */
    ADD("add"),

    /** Draft the whole cut again (or a plan, from the top). */
    REWRITE("rewrite");

    private final String wireValue;

    InstructAction(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves an action from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching action
     * @throws IllegalArgumentException if no action carries that value
     */
    public static InstructAction fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Instruct action must not be null");
        }
        var clean = value.trim();
        return Arrays.stream(values())
                .filter(action -> action.wireValue.equalsIgnoreCase(clean))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown instruct action: " + value));
    }
}
