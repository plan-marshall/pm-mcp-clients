/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import java.util.Arrays;
import java.util.List;

/**
 * The closed set of link relations a daemon resource of {@code /api/v1} carries in its {@code links}
 * member: each relation names the target resource a {@link ResourceLink} reaches. The wire value is
 * the lower-case form written into JSON.
 */
public enum LinkRelation {

    /** The resource itself; a client follows it to re-read the resource it holds. */
    SELF("self"),

    /** The runtime status ({@code GET /api/v1/status}). */
    STATUS("status"),

    /** Stopping the runtime ({@code POST /api/v1/operator/runtime/stop}). */
    STOP("stop"),

    /** The web access of the runtime, its listeners and its paired devices. */
    WEB("web"),

    /** The credential entries ({@code /api/v1/credentials/{credential_key}}). */
    CREDENTIALS("credentials"),

    /** The runtime-wide event stream ({@code GET /api/v1/events}). */
    EVENTS("events");

    private final String wire;

    LinkRelation(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation of this relation
     */
    public String wire() {
        return wire;
    }

    /**
     * Resolves a wire value to its relation.
     *
     * @param wire the wire representation
     * @return the matching relation, or {@code null} if none matches
     */
    public static LinkRelation fromWire(String wire) {
        if (wire == null) {
            return null;
        }
        for (var relation : values()) {
            if (relation.wire.equalsIgnoreCase(wire)) {
                return relation;
            }
        }
        return null;
    }

    /**
     * @param wire the wire representation
     * @return the matching relation
     * @throws IllegalArgumentException if no relation matches (fail-closed)
     */
    public static LinkRelation requireWire(String wire) {
        var relation = fromWire(wire);
        if (relation == null) {
            throw new IllegalArgumentException("Unknown link relation: " + wire);
        }
        return relation;
    }

    /**
     * @return the wire representations of every relation, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(LinkRelation::wire).toList();
    }
}
