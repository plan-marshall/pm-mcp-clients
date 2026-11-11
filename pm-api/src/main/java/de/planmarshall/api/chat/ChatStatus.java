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
 * The closed list of a chat's states, the state ids of the unit {@code chat} of the state catalogue
 * ({@code pm-mcp-core/doc/specification/state-catalogue.adoc#chat}). The wire value is the state id
 * itself.
 */
public enum ChatStatus {

    /** The chat is minted and its context rendered from the store ({@code auto}). */
    OPEN("chat.open"),

    /** The chat waits for the operator's message or end ({@code operator_wait}). */
    TURN("chat.turn"),

    /** The role's reply is written ({@code job}). */
    REPLY("chat.reply"),

    /** Whether a proposal is still open after a reply ({@code auto}). */
    ROUTE("chat.route"),

    /** The open proposal waits for accept or reject ({@code operator_wait}). */
    READ_BACK("chat.read-back"),

    /** An accepted change is applied through its subject's path ({@code auto}). */
    APPLY("chat.apply"),

    /** The chat has ended ({@code terminal}). */
    END("chat.end"),

    /** The accepted proposal was carried to the calling state ({@code terminal}). */
    CARRIED("chat.carried");

    private final String wireValue;

    ChatStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the state id used on the wire
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} for the two terminal states
     */
    public boolean isEnded() {
        return this == END || this == CARRIED;
    }

    /**
     * @param value the wire value, may be {@code null}
     * @return the state, or {@code null} when the value names none
     */
    public static @Nullable ChatStatus fromWireOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (var status : values()) {
            if (status.wireValue.equals(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * @param value the wire value
     * @return the state
     * @throws IllegalArgumentException if the value names no state (fail-closed)
     */
    public static ChatStatus fromWire(String value) {
        Objects.requireNonNull(value, "value");
        var status = fromWireOrNull(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown chat state: " + value);
        }
        return status;
    }

    /**
     * @return the wire values of the closed list, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ChatStatus::wireValue).toList();
    }
}
