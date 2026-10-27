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

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of the gate a waiver request names.
 * <p>
 * A waiver's {@code waiver_gate} carries the kind and its scope, separated by a colon
 * ({@code ci_check:integration-tests}, {@code coverage:pm-api}); the kind is the part before the
 * separator.
 */
public enum GateKind {
    CI_CHECK("ci_check"),
    COVERAGE("coverage");

    private final String value;

    GateKind(String value) {
        this.value = value;
    }

    /**
     * @return the wire string value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves the kind from a {@code waiver_gate} value or a bare kind.
     *
     * @param gate the gate, with or without its scope
     * @return enum constant, or {@code null} if unmatched
     */
    public static @Nullable GateKind fromValue(@Nullable String gate) {
        if (gate == null || gate.isBlank()) {
            return null;
        }
        var trimmed = gate.trim();
        int separator = trimmed.indexOf(':');
        var kind = separator < 0 ? trimmed : trimmed.substring(0, separator);
        for (var candidate : values()) {
            if (candidate.value.equalsIgnoreCase(kind) || candidate.name().equalsIgnoreCase(kind)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Resolves the kind or throws IllegalArgumentException.
     *
     * @param gate the gate, with or without its scope
     * @return enum constant
     */
    public static GateKind parse(String gate) {
        var kind = fromValue(gate);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown GateKind: " + gate);
        }
        return kind;
    }
}
