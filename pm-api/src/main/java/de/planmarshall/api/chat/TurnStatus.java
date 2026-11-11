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
 * The closed list of the statuses of a chat's turn
 * ({@code 01-processes-and-transport.adoc#chat-routes}).
 */
public enum TurnStatus {

    /** The reply is being written; the turn carries its text so far. */
    STREAMING("streaming"),

    /** The turn is complete. */
    COMPLETE("complete"),

    /** The reply was stopped by the operator; the text so far is kept. */
    STOPPED("stopped"),

    /** The reply failed with its reason; the text so far is kept. */
    FAILED("failed");

    private final String wireValue;

    TurnStatus(String wireValue) {
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
     * @return the status, or {@code null} when the value names none
     */
    public static @Nullable TurnStatus fromWireOrNull(@Nullable String value) {
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
     * @return the status
     * @throws IllegalArgumentException if the value names no status (fail-closed)
     */
    public static TurnStatus fromWire(String value) {
        Objects.requireNonNull(value, "value");
        var status = fromWireOrNull(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown turn status: " + value);
        }
        return status;
    }

    /**
     * @return the wire values of the closed list, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(TurnStatus::wireValue).toList();
    }
}
