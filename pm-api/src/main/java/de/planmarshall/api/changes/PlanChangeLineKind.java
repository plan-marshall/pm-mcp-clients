/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.changes;

import org.jspecify.annotations.Nullable;

/**
 * What one line of a hunk is: context of both sides, removed from the base, or added by the capture.
 */
public enum PlanChangeLineKind {

    /** A line the change left untouched. */
    CONTEXT("context"),

    /** A line the change removed. */
    REMOVED("removed"),

    /** A line the change added. */
    ADDED("added");

    private final String wire;

    PlanChangeLineKind(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation
     */
    public String wire() {
        return wire;
    }

    /**
     * @param wire the wire representation
     * @return the matching kind, or {@code null} if none matches
     */
    public static @Nullable PlanChangeLineKind fromWireOrNull(String wire) {
        for (var kind : values()) {
            if (kind.wire.equals(wire)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * @param wire the wire representation
     * @return the matching kind
     * @throws IllegalArgumentException if no kind matches (fail-closed)
     */
    public static PlanChangeLineKind fromWire(String wire) {
        var kind = fromWireOrNull(wire);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown kind of a changed line: " + wire);
        }
        return kind;
    }
}
