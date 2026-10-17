/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import org.jspecify.annotations.Nullable;

/**
 * How documents are taken into a plan at its start: the machine's {@code sources.documents_mode}
 * (State Catalogue, Documents as a Source).
 */
public enum DocumentsMode {

    /** The documents stay where they are; the copy is not kept in step. */
    COPY("copy"),

    /** The documents leave where they are. */
    MOVE("move"),

    /** Ask each time; the default (Copy is preselected when asked). */
    ASK("ask");

    private final String wireValue;

    DocumentsMode(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @param value wire value or enum name
     * @return the mode, or {@code null} if unmatched
     */
    public static @Nullable DocumentsMode fromWire(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var mode : values()) {
            if (mode.wireValue.equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return null;
    }
}
