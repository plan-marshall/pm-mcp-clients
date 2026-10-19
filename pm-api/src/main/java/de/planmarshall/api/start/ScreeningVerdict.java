/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.util.Arrays;
import java.util.List;

/**
 * The closed list of the screener's verdicts over a stored source (Model Work, Screened Once
 * Stored): {@code clean}, {@code flagged}, and {@code unscreened}, the fail-closed verdict where the
 * screener failed or no configuration of the role {@code screener} qualifies.
 * <p>
 * The verdict is written into the source's manifest entry. It is absent for a source of this
 * machine, which is never screened.
 */
public enum ScreeningVerdict {

    /** The screener found nothing suspicious; the source may be trusted. */
    CLEAN("clean"),

    /** The screener found spans that may contain instructions to a model. */
    FLAGGED("flagged"),

    /** The screening failed closed; no model reads the source. */
    UNSCREENED("unscreened");

    private final String wireValue;

    ScreeningVerdict(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the source needs the operator's explicit acceptance before it may be
     *         used, and keeps the untrusted mark afterwards
     */
    public boolean needsAcceptance() {
        return this != CLEAN;
    }

    /**
     * Resolves a verdict from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching verdict, or {@code null} if none matches
     */
    public static ScreeningVerdict fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var verdict : values()) {
            if (verdict.wireValue.equalsIgnoreCase(value.trim())) {
                return verdict;
            }
        }
        return null;
    }

    /**
     * Resolves a verdict from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching verdict
     * @throws IllegalArgumentException if no verdict carries that value (fail-closed)
     */
    public static ScreeningVerdict fromWire(String value) {
        var verdict = fromWireOrNull(value);
        if (verdict == null) {
            throw new IllegalArgumentException("Unknown screening verdict: " + value);
        }
        return verdict;
    }

    /**
     * @return the wire values of every verdict, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ScreeningVerdict::wireValue).toList();
    }
}
