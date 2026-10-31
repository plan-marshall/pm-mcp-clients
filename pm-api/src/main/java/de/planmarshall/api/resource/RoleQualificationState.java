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
import org.jspecify.annotations.Nullable;

/**
 * The closed qualification state of a role (Model Work § Role Qualification): whether a valid
 * qualification record exists for the role's qualification digest at the project's current levels.
 */
public enum RoleQualificationState {

    /** A valid record qualifies every form of the role. */
    QUALIFIED("qualified"),

    /** Only the fresh form qualifies; the warm form falls below the project's level or gap. */
    QUALIFIED_FRESH_ONLY("qualified_fresh_only"),

    /** No valid record exists, or its measurement is below the project's current level. */
    UNQUALIFIED("unqualified"),

    /** The role artifact itself is invalid; an invalid artifact counts as undefined. */
    INVALID("invalid");

    private final String wire;

    RoleQualificationState(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation of this state
     */
    public String wire() {
        return wire;
    }

    /**
     * @param wire the wire representation
     * @return the matching state, or {@code null} if none matches
     */
    public static @Nullable RoleQualificationState fromWire(@Nullable String wire) {
        if (wire == null) {
            return null;
        }
        for (var state : values()) {
            if (state.wire.equalsIgnoreCase(wire)) {
                return state;
            }
        }
        return null;
    }

    /**
     * @param wire the wire representation
     * @return the matching state
     * @throws IllegalArgumentException if none matches (fail-closed)
     */
    public static RoleQualificationState requireWire(String wire) {
        var state = fromWire(wire);
        if (state == null) {
            throw new IllegalArgumentException("Unknown role qualification state: " + wire);
        }
        return state;
    }

    /**
     * @return the wire representations of every state, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(RoleQualificationState::wire).toList();
    }
}
