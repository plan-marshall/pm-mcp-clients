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
 * The closed list of the kinds of a chat's turn ({@code 01-processes-and-transport.adoc#chat-routes}).
 */
public enum TurnKind {

    /** A message of the operator or a reply of the role. */
    MESSAGE("message"),

    /** The role's proposed change, a turn of its own after the reply that made it. */
    PROPOSAL("proposal"),

    /** A notice the runtime writes, with the subject's narrative lines where it reports them. */
    NOTICE("notice");

    private final String wireValue;

    TurnKind(String wireValue) {
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
     * @return the kind, or {@code null} when the value names none
     */
    public static @Nullable TurnKind fromWireOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (var kind : values()) {
            if (kind.wireValue.equals(value)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * @param value the wire value
     * @return the kind
     * @throws IllegalArgumentException if the value names no kind (fail-closed)
     */
    public static TurnKind fromWire(String value) {
        Objects.requireNonNull(value, "value");
        var kind = fromWireOrNull(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown turn kind: " + value);
        }
        return kind;
    }

    /**
     * @return the wire values of the closed list, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(TurnKind::wireValue).toList();
    }
}
