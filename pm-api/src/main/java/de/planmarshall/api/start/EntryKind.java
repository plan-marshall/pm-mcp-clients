/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.util.Arrays;
import java.util.List;

/**
 * The closed list of the kinds of work a start opens: a plan's entry runs the {@code bootstrap-plan}
 * unit, an epic's entry runs {@code bootstrap-epic} (State Catalogue, Starting from the Clients).
 * <p>
 * The list is closed and fails closed: a kind read from the wire that is not a member is refused by
 * {@link #fromWire(String)}.
 */
public enum EntryKind {

    /** A plan's start ({@code bootstrap-plan}). */
    PLAN("plan"),

    /** An epic's start ({@code bootstrap-epic}). */
    EPIC("epic");

    private final String wireValue;

    EntryKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} for an epic's entry, which offers no recipe, lesson, local changes, or
     *         pull request as a source
     */
    public boolean epic() {
        return this == EPIC;
    }

    /**
     * Resolves a kind from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching kind, or {@code null} if none matches
     */
    public static EntryKind fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var kind : values()) {
            if (kind.wireValue.equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        return null;
    }

    /**
     * Resolves a kind from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching kind
     * @throws IllegalArgumentException if no kind carries that value (fail-closed)
     */
    public static EntryKind fromWire(String value) {
        var kind = fromWireOrNull(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown entry kind: " + value);
        }
        return kind;
    }

    /**
     * @return the wire values of every kind, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(EntryKind::wireValue).toList();
    }
}
