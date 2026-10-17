/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.lesson;

import java.util.Arrays;

/**
 * The closed list of what a lesson's premise cites (UC-STA-8).
 * <p>
 * The wire value is the one the representation and the message catalogue use
 * ({@code lesson_source.evidence.<wire>}).
 */
public enum PremiseEvidenceKind {

    /** A file the lesson cites, addressed by its path in the repository. */
    FILE("file"),

    /** A symbol the lesson cites, addressed by its qualified name. */
    SYMBOL("symbol");

    private final String wireValue;

    PremiseEvidenceKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a kind from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching kind
     * @throws IllegalArgumentException if no kind carries that value
     */
    public static PremiseEvidenceKind fromWire(String value) {
        if (value != null) {
            var clean = value.trim();
            return Arrays.stream(values())
                    .filter(kind -> kind.wireValue.equalsIgnoreCase(clean))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown premise evidence kind: " + value));
        }
        throw new IllegalArgumentException("Premise evidence kind must not be null");
    }
}
