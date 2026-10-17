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
 * State of a node within a plan's process tree (UC-PLN-1).
 */
public enum NodeState {
    DONE("done", "✓", "+"),
    RUNNING("running", "▶", ">"),
    WAITING_FOR_OPERATOR("waiting_for_operator", "!", "!"),
    FAILED("failed", "✗", "x"),
    PLANNED("planned", "○", "o"),
    NOT_TAKEN("not_taken", "–", "-"),
    NOT_IN_PLAN("not_in_plan", "·", ".");

    private final String value;
    private final String glyph;
    private final String asciiGlyph;

    NodeState(String value, String glyph, String asciiGlyph) {
        this.value = value;
        this.glyph = glyph;
        this.asciiGlyph = asciiGlyph;
    }

    public String value() {
        return value;
    }

    public String glyph() {
        return glyph;
    }

    public String asciiGlyph() {
        return asciiGlyph;
    }

    public static NodeState fromValue(String value) {
        Objects.requireNonNull(value, "value");
        if ("waiting".equalsIgnoreCase(value) || "waiting_for_you".equalsIgnoreCase(value)) {
            return WAITING_FOR_OPERATOR;
        }
        for (var state : values()) {
            if (state.value.equalsIgnoreCase(value) || state.name().equalsIgnoreCase(value)) {
                return state;
            }
        }
        return PLANNED;
    }
}
