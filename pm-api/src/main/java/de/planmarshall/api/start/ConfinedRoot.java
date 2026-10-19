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
 * The closed list of the roots the file picker may list (State Catalogue, Documents as a Source;
 * Runtime &amp; Interaction Model, Local API).
 * <p>
 * The listing is confined to the project's checkout and the operator's home directory, the checkout
 * first. A path that resolves outside its root, by {@code ..}, by an absolute path, or by a link
 * that leads out, is refused: the picker never reads the rest of the machine.
 */
public enum ConfinedRoot {

    /** The project's checkout. */
    CHECKOUT("checkout"),

    /** The operator's home directory (loopback listener and terminal client only). */
    HOME("home");

    private final String wireValue;

    ConfinedRoot(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the root is offered only on this machine
     */
    public boolean loopbackOnly() {
        return this == HOME;
    }

    /**
     * Resolves a root from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching root, or {@code null} if none matches
     */
    public static ConfinedRoot fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var root : values()) {
            if (root.wireValue.equalsIgnoreCase(value.trim())) {
                return root;
            }
        }
        return null;
    }

    /**
     * Resolves a root from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching root
     * @throws IllegalArgumentException if no root carries that value (fail-closed)
     */
    public static ConfinedRoot fromWire(String value) {
        var root = fromWireOrNull(value);
        if (root == null) {
            throw new IllegalArgumentException("Unknown confined root: " + value);
        }
        return root;
    }

    /**
     * @return the wire values of every root, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ConfinedRoot::wireValue).toList();
    }
}
