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
 * The closed enumeration of the kinds of scope an {@link EventScope} names: the runtime-wide stream
 * (no scope id), a plan, an epic, a project's workspace scope, the machine scope, or an entry. It is
 * the closed set of the Event Catalogue's envelope scope. A scope read from the wire that is not a
 * member is refused by {@link #fromWire(String)}.
 */
public enum EventScopeKind {

    /** The runtime-wide stream. */
    RUNTIME("runtime"),

    /** A plan ({@code plan_id}). */
    PLAN("plan"),

    /** A multi-plan initiative ({@code epic_id}). */
    EPIC("epic"),

    /** The project's workspace scope. */
    WORKSPACE("workspace"),

    /** The machine's one scope. */
    MACHINE("machine"),

    /** A start's or a project onboarding's entry ({@code entry_id}). */
    ENTRY("entry");

    private final String wire;

    EventScopeKind(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation of this kind
     */
    public String wire() {
        return wire;
    }

    /**
     * @return {@code true} if a scope of this kind carries an identifier (a plan, an epic, an entry)
     */
    public boolean requiresIdentifier() {
        return this == PLAN || this == EPIC || this == ENTRY;
    }

    /**
     * Resolves a wire value to its kind.
     *
     * @param wire the wire representation
     * @return the matching kind, or {@code null} if none matches
     */
    public static EventScopeKind fromWireOrNull(String wire) {
        if (wire == null) {
            return null;
        }
        for (var kind : values()) {
            if (kind.wire.equals(wire)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * Resolves a wire value to its kind.
     *
     * @param wire the wire representation
     * @return the matching kind
     * @throws IllegalArgumentException if no kind matches (fail-closed)
     */
    public static EventScopeKind fromWire(String wire) {
        var kind = fromWireOrNull(wire);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown event scope kind: " + wire);
        }
        return kind;
    }

    /**
     * @return the wire representations of every kind, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(EventScopeKind::wire).toList();
    }
}
