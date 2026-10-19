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
import java.util.Set;

/**
 * The closed list of the states a source of a start shows on its line (State Catalogue, Source
 * Units; design <em>Starting Work</em>, the list of sources).
 * <p>
 * A source is never changed once stored: a failure is retried by gathering it again, and an
 * acceptance changes only its trust mark and its disposition. The states {@link #STORED},
 * {@link #COPIED}, and {@link #MOVED} are the settled ones; every other state still blocks
 * <em>Continue</em>.
 */
public enum SourceState {

    /** The source is being checked. */
    CHECKING("checking"),

    /** The source is being fetched. */
    FETCHING("fetching"),

    /** The stored copy is being screened. */
    SCREENING("screening"),

    /** The source is stored. */
    STORED("stored"),

    /** The documents are copied into the source directory. */
    COPIED("copied"),

    /** The documents are moved into the source directory. */
    MOVED("moved"),

    /** The screener flagged spans of the source. */
    FLAGGED("flagged"),

    /** No screener qualifies, or the screening failed; the source is untrusted. */
    UNSCREENED("unscreened"),

    /** The gathering failed; the source carries its cause. */
    FAILED("failed");

    private static final Set<SourceState> SETTLED = Set.of(STORED, COPIED, MOVED);

    private final String wireValue;

    SourceState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the source is stored and needs nothing more before <em>Continue</em>
     */
    public boolean settled() {
        return SETTLED.contains(this);
    }

    /**
     * @return {@code true} if the source failed and carries a cause
     */
    public boolean failed() {
        return this == FAILED;
    }

    /**
     * Resolves a state from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching state, or {@code null} if none matches
     */
    public static SourceState fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var state : values()) {
            if (state.wireValue.equalsIgnoreCase(value.trim())) {
                return state;
            }
        }
        return null;
    }

    /**
     * Resolves a state from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching state
     * @throws IllegalArgumentException if no state carries that value (fail-closed)
     */
    public static SourceState fromWire(String value) {
        var state = fromWireOrNull(value);
        if (state == null) {
            throw new IllegalArgumentException("Unknown source state: " + value);
        }
        return state;
    }

    /**
     * @return the wire values of every state, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(SourceState::wireValue).toList();
    }
}
