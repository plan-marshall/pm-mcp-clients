/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.config;

import java.util.Arrays;
import java.util.List;

/**
 * The closed enumeration of the levels an effective configuration value is resolved from, from the
 * farthest (the compiled {@code default}) to the nearest (a plan's own {@code plan} value). The
 * catalogue is the closed list of the Runtime Model specification (Local API, {@code GET /api/v1/config})
 * and the Store Schemas specification (Project Values over Machine Values): the nearest level wins,
 * and a level that says nothing defers to the next.
 * <p>
 * The {@link #rank()} gives the resolution order: a value of a higher rank overrides a value of a
 * lower rank, so the {@code overrides} origin of a value is always of a strictly lower rank. The list
 * is closed and fails closed: a level read from the wire that is not a member is refused by
 * {@link #fromWire(String)}, never silently accepted.
 */
public enum ConfigOrigin {

    /** The compiled default of the binary. */
    DEFAULT("default", 0),

    /** A value derived from the derived facts of the machine or the project. */
    DERIVED("derived", 1),

    /** The machine's value ({@code machine-config.json}). */
    MACHINE("machine", 2),

    /** The team's value ({@code .marshall/config/}). */
    TEAM("team", 3),

    /** The project's operator value ({@code local-config.json}). */
    PROJECT("project", 4),

    /** A plan's own value in its plan instance. */
    PLAN("plan", 5);

    private static final List<String> DEFAULT_OPERATOR_LEVELS = List.of(
            MACHINE.wire(), PROJECT.wire(), PLAN.wire());

    private final String wire;
    private final int rank;

    ConfigOrigin(String wire, int rank) {
        this.wire = wire;
        this.rank = rank;
    }

    /**
     * @return the wire representation of this level
     */
    public String wire() {
        return wire;
    }

    /**
     * @return the resolution rank; a higher rank overrides a lower one
     */
    public int rank() {
        return rank;
    }

    /**
     * @param other the other level
     * @return {@code true} if this level overrides {@code other} in the resolution order
     */
    public boolean overrides(ConfigOrigin other) {
        return other != null && rank > other.rank;
    }

    /**
     * Resolves a wire value to its level.
     *
     * @param wire the wire representation
     * @return the matching level, or {@code null} if none matches
     */
    public static ConfigOrigin fromWireOrNull(String wire) {
        if (wire == null) {
            return null;
        }
        for (var origin : values()) {
            if (origin.wire.equalsIgnoreCase(wire)) {
                return origin;
            }
        }
        return null;
    }

    /**
     * Resolves a wire value to its level.
     *
     * @param wire the wire representation
     * @return the matching level
     * @throws IllegalArgumentException if no level matches (fail-closed)
     */
    public static ConfigOrigin fromWire(String wire) {
        var origin = fromWireOrNull(wire);
        if (origin == null) {
            throw new IllegalArgumentException("Unknown configuration origin: " + wire);
        }
        return origin;
    }

    /**
     * @return the wire representations of every level, in resolution order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ConfigOrigin::wire).toList();
    }

    /**
     * @return the wire representations of the levels an operator may write (machine, project, plan)
     */
    public static List<String> operatorLevels() {
        return DEFAULT_OPERATOR_LEVELS;
    }
}
