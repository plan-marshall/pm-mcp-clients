/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.interaction;

import java.util.Arrays;
import java.util.List;

/**
 * The closed enumeration of the kinds of scope an operator state belongs to: a plan, an epic, the
 * project's workspace scope, the machine's one scope, or a start's or a project onboarding's entry.
 * It is the closed list of the Runtime Model specification (Resources of the Operator's Clients) and
 * the Hypermedia Format specification (Operator Representation).
 * <p>
 * The list is closed and fails closed: a kind read from the wire that is not a member is refused by
 * {@link #fromWire(String)}.
 */
public enum OperatorScopeKind {

    /** A plan ({@code plan_id}). */
    PLAN("plan"),

    /** A multi-plan initiative ({@code epic_id}). */
    EPIC("epic"),

    /** The project's workspace scope (addressed with {@code plan_id: NO_PLAN}). */
    WORKSPACE("workspace"),

    /** The machine's one scope, which hosts its onboarding and its settings. */
    MACHINE("machine"),

    /** A start's or a project onboarding's entry ({@code entry_id}). */
    ENTRY("entry");

    private final String wire;

    OperatorScopeKind(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation of this kind
     */
    public String wire() {
        return wire;
    }

    /**
     * @return {@code true} if a scope of this kind carries an identifier (a plan, an epic, an entry);
     *         the machine scope and the project's workspace scope carry none
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
    public static OperatorScopeKind fromWireOrNull(String wire) {
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
    public static OperatorScopeKind fromWire(String wire) {
        var kind = fromWireOrNull(wire);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown operator scope kind: " + wire);
        }
        return kind;
    }

    /**
     * @return the wire representations of every kind, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(OperatorScopeKind::wire).toList();
    }
}
