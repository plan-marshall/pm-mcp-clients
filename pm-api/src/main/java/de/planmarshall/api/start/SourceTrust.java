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
 * The closed list of how a source of a start is trusted (Model Work, What Is on This Machine Is
 * Trusted; design <em>Starting Work</em>, the list of sources).
 * <p>
 * What is on this machine is trusted and never screened: the operator's description, the documents
 * the operator chose, a lesson, the local changes, and an epic's row. A source from outside the
 * machine is untrusted until its screening passed, and an accepted flagged or unscreened source
 * keeps the untrusted mark.
 */
public enum SourceTrust {

    /** The source is the operator's own or its screening passed. */
    TRUSTED("trusted"),

    /** The source comes from outside the machine and is not cleared for instruction use. */
    UNTRUSTED("untrusted");

    private final String wireValue;

    SourceTrust(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a trust from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching trust, or {@code null} if none matches
     */
    public static SourceTrust fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var trust : values()) {
            if (trust.wireValue.equalsIgnoreCase(value.trim())) {
                return trust;
            }
        }
        return null;
    }

    /**
     * Resolves a trust from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching trust
     * @throws IllegalArgumentException if no trust carries that value (fail-closed)
     */
    public static SourceTrust fromWire(String value) {
        var trust = fromWireOrNull(value);
        if (trust == null) {
            throw new IllegalArgumentException("Unknown source trust: " + value);
        }
        return trust;
    }

    /**
     * @return the wire values of every trust, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(SourceTrust::wireValue).toList();
    }
}
