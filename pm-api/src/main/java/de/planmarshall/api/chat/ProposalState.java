/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The closed list of a proposal's states
 * ({@code 01-processes-and-transport.adoc#chat-proposal-states}).
 */
public enum ProposalState {

    /** Open until it is answered. */
    OPEN("open"),

    /** Replaced by a later proposal of the role. */
    REPLACED("replaced"),

    /** A plan's revision accepted and queued for the plan's next task boundary. */
    ACCEPTED("accepted"),

    /** Rejected by the operator. */
    REJECTED("rejected"),

    /** Applied at its boundary, or at once where its path has none. */
    APPLIED("applied"),

    /** Refused with the finding of a check that failed. */
    REFUSED("refused"),

    /** The plan left the revision window before the proposal was answered or applied. */
    LAPSED("lapsed");

    private final String wireValue;

    ProposalState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the value used on the wire
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @param value the wire value, may be {@code null}
     * @return the state, or {@code null} when the value names none
     */
    public static @Nullable ProposalState fromWireOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (var state : values()) {
            if (state.wireValue.equals(value)) {
                return state;
            }
        }
        return null;
    }

    /**
     * @param value the wire value
     * @return the state
     * @throws IllegalArgumentException if the value names no state (fail-closed)
     */
    public static ProposalState fromWire(String value) {
        Objects.requireNonNull(value, "value");
        var state = fromWireOrNull(value);
        if (state == null) {
            throw new IllegalArgumentException("Unknown proposal state: " + value);
        }
        return state;
    }

    /**
     * @return the wire values of the closed list, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ProposalState::wireValue).toList();
    }
}
