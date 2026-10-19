/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.event;

import java.util.Arrays;
import java.util.List;

/**
 * The closed enumeration of the reasons a client event stream ends with the last event
 * {@link EventName#STREAM_CLOSED}: the {@code reason} member of the {@code stream.closed} payload.
 * It is the closed list of the Runtime Model specification (Event Catalogue, Stream Control).
 * <p>
 * The list is closed and fails closed: a reason read from the wire that is not a member is refused by
 * {@link #fromWire(String)}, never silently accepted. The web app shows the reason and, for a
 * revocation and an expiry, the channel and device that ended the stream.
 */
public enum StreamClosedEvent {

    /** The device was revoked; the stream's browser deletes its stored secret and shows the pairing page. */
    DEVICE_REVOKED("device_revoked"),

    /** The device expired after not being used (the device idle expiry). */
    DEVICE_EXPIRED("device_expired"),

    /** The web listener the stream arrived on was closed. */
    LISTENER_CLOSED("listener_closed"),

    /** The runtime is stopping and the stream ends with it. */
    RUNTIME_STOPPING("runtime_stopping"),

    /** The stream's scope ended; the reason names the successor stream where there is one. */
    SCOPE_ENDED("scope_ended");

    private final String wire;

    StreamClosedEvent(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation of this reason
     */
    public String wire() {
        return wire;
    }

    /**
     * Resolves a wire value to its reason.
     *
     * @param wire the wire representation
     * @return the matching reason, or {@code null} if none matches
     */
    public static StreamClosedEvent fromWireOrNull(String wire) {
        if (wire == null) {
            return null;
        }
        for (var reason : values()) {
            if (reason.wire.equals(wire)) {
                return reason;
            }
        }
        return null;
    }

    /**
     * Resolves a wire value to its reason.
     *
     * @param wire the wire representation
     * @return the matching reason
     * @throws IllegalArgumentException if no reason matches (fail-closed)
     */
    public static StreamClosedEvent fromWire(String wire) {
        var reason = fromWireOrNull(wire);
        if (reason == null) {
            throw new IllegalArgumentException("Unknown stream.closed reason: " + wire);
        }
        return reason;
    }

    /**
     * @return the wire representations of every reason, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(StreamClosedEvent::wire).toList();
    }
}
