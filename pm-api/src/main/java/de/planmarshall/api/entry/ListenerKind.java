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

import java.util.Objects;

/**
 * The closed list of web listeners, which open and close independently
 * (03-web-server.adoc#two-listeners).
 */
public enum ListenerKind {

    /** Plain HTTP on {@code 127.0.0.1:<web.port>}, for this machine and SSH forwards. */
    LOOPBACK("loopback"),

    /** HTTPS on all interfaces on its own port {@code web.lan_port}. */
    LAN("lan");

    private final String wireValue;

    ListenerKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the value used on the wire and in the {@code web} configuration object
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a wire value to its listener kind.
     *
     * @param value the wire value ({@code loopback} or {@code lan}, case-insensitive)
     * @return the listener kind
     * @throws IllegalArgumentException if the value names no listener
     */
    public static ListenerKind fromWire(String value) {
        Objects.requireNonNull(value, "value");
        for (var kind : values()) {
            if (kind.wireValue.equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown listener kind: " + value);
    }
}
