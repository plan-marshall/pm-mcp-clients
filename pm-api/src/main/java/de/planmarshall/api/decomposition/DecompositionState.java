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
import java.util.Locale;

/**
 * The states an epic's decomposition passes through (UC-STA-6).
 * <p>
 * The wire value is the one the representation and the message catalogue use
 * ({@code start.decomposition.*}, {@code decomposition.<wire>}).
 */
public enum DecompositionState {

    /** The planning role drafts the workstreams and their staged plans. */
    DRAFT("draft"),

    /** The server checks surface disjointness, the dependency graph and workstream integrity. */
    CHECK("check"),

    /** The operator reviews the cut, plan by plan. */
    REVIEW("review"),

    /** A rewrite is read back, old against new, before it is accepted. */
    READ_BACK("read_back"),

    /** The cut is agreed and written and committed to the epic ledger. */
    COMMIT("commit"),

    /** The role found no feasible cut, or its task was lost; the epic holds. */
    HELD("held");

    private static final String STATE_PREFIX = "decomposition.";

    private final String wireValue;

    DecompositionState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return the state id the state catalogue and the clients use ({@code decomposition.<wire>})
     */
    public String stateId() {
        return STATE_PREFIX + wireValue;
    }

    /**
     * Resolves a state from its wire value or its full state id, ignoring case.
     *
     * @param value the wire value or state id
     * @return the matching state
     * @throws IllegalArgumentException if no state carries that value
     */
    public static DecompositionState fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Decomposition state must not be null");
        }
        var clean = value.trim().toLowerCase(Locale.ROOT);
        if (clean.startsWith(STATE_PREFIX)) {
            clean = clean.substring(STATE_PREFIX.length());
        }
        var candidate = clean.replace('-', '_');
        return Arrays.stream(values())
                .filter(state -> state.wireValue.equals(candidate))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown decomposition state: " + value));
    }
}
