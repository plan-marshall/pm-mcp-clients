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
 * The closed list of the formats of a turn's text ({@code 01-processes-and-transport.adoc#chat-routes}):
 * the operator's turns, untrusted turns and notices are always plain; a role's reply is the Markdown
 * subset the clients render.
 */
public enum TurnFormat {

    /** Plain text, never rendered. */
    PLAIN("plain"),

    /** The Markdown subset of the chat's text. */
    MARKDOWN("markdown");

    private final String wireValue;

    TurnFormat(String wireValue) {
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
     * @return the format, or {@code null} when the value names none
     */
    public static @Nullable TurnFormat fromWireOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (var format : values()) {
            if (format.wireValue.equals(value)) {
                return format;
            }
        }
        return null;
    }

    /**
     * @param value the wire value
     * @return the format
     * @throws IllegalArgumentException if the value names no format (fail-closed)
     */
    public static TurnFormat fromWire(String value) {
        Objects.requireNonNull(value, "value");
        var format = fromWireOrNull(value);
        if (format == null) {
            throw new IllegalArgumentException("Unknown turn format: " + value);
        }
        return format;
    }

    /**
     * @return the wire values of the closed list, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(TurnFormat::wireValue).toList();
    }
}
