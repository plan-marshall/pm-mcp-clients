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
 * The closed list of where the transcript of a start chat stands (State Catalogue, Chatting with the
 * Planning Role; design <em>Starting Work</em>, chatting about the request).
 * <p>
 * The transcript stays with the entry and is kept for audit, never read by a model of the plan as a
 * source. At {@code start.create} a chat still open is ended first, its transcript complete, and the
 * transcript moves with the source directory into the plan or the epic; abandoning the entry deletes
 * it with the sources.
 */
public enum StartTranscriptState {

    /** The transcript sits in the entry's {@code chat/} directory. */
    WITH_ENTRY("with_entry"),

    /** The transcript moved with the source directory into the created plan or epic. */
    MOVED("moved"),

    /** The transcript was deleted with the abandoned entry. */
    DELETED("deleted");

    private final String wireValue;

    StartTranscriptState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the transcript is complete and needs no further end
     */
    public boolean settled() {
        return this == MOVED || this == DELETED;
    }

    /**
     * Resolves a state from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching state, or {@code null} if none matches
     */
    public static StartTranscriptState fromWireOrNull(String value) {
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
    public static StartTranscriptState fromWire(String value) {
        var state = fromWireOrNull(value);
        if (state == null) {
            throw new IllegalArgumentException("Unknown start transcript state: " + value);
        }
        return state;
    }

    /**
     * @return the wire values of every state, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(StartTranscriptState::wireValue).toList();
    }
}
