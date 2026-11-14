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
 * The status of one changed file of a plan's change, as git reports it.
 */
public enum ChangedFileStatus {

    /** The file is new in the capture. */
    ADDED("added"),

    /** The file existed in the base and changed. */
    MODIFIED("modified"),

    /** The file existed in the base and is gone. */
    DELETED("deleted"),

    /** The file moved; the path it came from is the file's {@code renamed_from}. */
    RENAMED("renamed");

    private final String wire;

    ChangedFileStatus(String wire) {
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
     * @return the matching status, or {@code null} if none matches
     */
    public static @Nullable ChangedFileStatus fromWireOrNull(String wire) {
        for (var status : values()) {
            if (status.wire.equals(wire)) {
                return status;
            }
        }
        return null;
    }

    /**
     * @param wire the wire representation
     * @return the matching status
     * @throws IllegalArgumentException if no status matches (fail-closed)
     */
    public static ChangedFileStatus fromWire(String wire) {
        var status = fromWireOrNull(wire);
        if (status == null) {
            throw new IllegalArgumentException("Unknown status of a changed file: " + wire);
        }
        return status;
    }
}
