/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decision;

import java.util.Objects;

/**
 * Quorum voting consensus band (UC-PLN-2).
 */
public enum QuorumBand {
    UNANIMOUS("unanimous"),
    CLEAR_MAJORITY("clear_majority"),
    SPLIT("split"),
    NO_CONSENSUS("no_consensus");

    private final String value;

    QuorumBand(String value) {
        this.value = value;
    }

    /**
     * Returns the wire value (e.g. {@code "unanimous"}).
     *
     * @return wire value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves QuorumBand from wire or enum name.
     *
     * @param value wire or enum string
     * @return resolved quorum band
     * @throws IllegalArgumentException on unknown value
     */
    public static QuorumBand fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (var b : values()) {
            if (b.value.equalsIgnoreCase(value) || b.name().equalsIgnoreCase(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unknown QuorumBand: " + value);
    }
}
