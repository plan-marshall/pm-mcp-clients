/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.util.Arrays;
import java.util.Locale;

/**
 * The states a chat passes through (design <em>Chatting with the Planning Role</em>).
 *
 * <p>The wire value is the one the representation and the message catalogue use
 * ({@code chat.<wire>}); the state id is that same prefixed name, as the state catalogue spells it
 * ({@code chat.open}, {@code chat.turn}, {@code chat.read-back}, {@code chat.apply},
 * {@code chat.end}).
 */
public enum ChatState {

    /** The chat is opened and the role is given the facts of the subject as data. */
    OPEN("open"),

    /** The operator's message is the chat's operator wait and goes to the role. */
    TURN("turn"),

    /** The role's one proposed change is read back, old against new, before it applies. */
    READ_BACK("read-back"),

    /** An accepted change is applied through the subject's own path and its checks. */
    APPLY("apply"),

    /** The chat is closed; a proposal not yet accepted is dropped with it. */
    END("end");

    private static final String STATE_PREFIX = "chat.";

    private final String wireValue;

    ChatState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return the state id the state catalogue and the clients use ({@code chat.<wire>})
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
    public static ChatState fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Chat state must not be null");
        }
        var clean = value.trim().toLowerCase(Locale.ROOT);
        if (clean.startsWith(STATE_PREFIX)) {
            clean = clean.substring(STATE_PREFIX.length());
        }
        var candidate = clean;
        return Arrays.stream(values())
                .filter(state -> state.wireValue.equals(candidate))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown chat state: " + value));
    }
}
