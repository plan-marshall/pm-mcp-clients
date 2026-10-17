/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.git;

import java.util.Arrays;

/**
 * The screening verdict of external text of a pull request (UC-STA-7).
 * <p>
 * {@code clean} may be adopted without an extra acceptance; {@code flagged} and {@code unscreened}
 * need an explicit acceptance by the operator before the pull request is taken over.
 */
public enum ScreeningOutcome {

    /** The screening found nothing suspicious. */
    CLEAN("clean"),

    /** The screening found text that may contain instructions to a model. */
    FLAGGED("flagged"),

    /** No screener was configured, so the text counts as untrusted. */
    UNSCREENED("unscreened");

    private final String wireValue;

    ScreeningOutcome(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the pull request needs an explicit acceptance before adoption
     */
    public boolean needsAcceptance() {
        return this != CLEAN;
    }

    /**
     * Resolves an outcome from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching outcome
     * @throws IllegalArgumentException if no outcome carries that value
     */
    public static ScreeningOutcome fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Screening outcome must not be null");
        }
        var clean = value.trim();
        return Arrays.stream(values())
                .filter(outcome -> outcome.wireValue.equalsIgnoreCase(clean))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown screening outcome: " + value));
    }
}
