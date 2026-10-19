/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.entry;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The closed list of the states a web listener can be in
 * (03-web-server.adoc#two-listeners, 03-web-server.adoc#web-listener-bind). A listener opens and
 * closes independently of the other, and a refused open leaves it closed.
 */
public enum ListenerState {

    /** The listener is serving. */
    OPEN("open"),

    /** The listener is not serving. */
    CLOSED("closed"),

    /** The listener could not open because its port is occupied on a local address. */
    PORT_OCCUPIED("port_occupied");

    private final String wireValue;

    ListenerState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the value used on the wire
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a wire value to its listener state.
     *
     * @param value the wire value ({@code open}, {@code closed}, or {@code port_occupied})
     * @return the listener state, or {@code null} when the value names none
     */
    public static ListenerState fromWireOrNull(String value) {
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
     * Resolves a wire value to its listener state.
     *
     * @param value the wire value
     * @return the listener state
     * @throws IllegalArgumentException if the value names no state (fail-closed)
     */
    public static ListenerState fromWire(String value) {
        Objects.requireNonNull(value, "value");
        var state = fromWireOrNull(value);
        if (state == null) {
            throw new IllegalArgumentException("Unknown listener state: " + value);
        }
        return state;
    }

    /**
     * @return the wire values of the closed list, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ListenerState::wireValue).toList();
    }
}
