/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.git;

import java.util.Arrays;

/**
 * The lane a plan of a git source runs in (UC-STA-7, UC-STA-9).
 * <p>
 * An adopted pull request is taken over in the {@code adopt} lane; captured local changes are
 * committed and landed in the {@code land} lane.
 */
public enum SourceLane {

    /** The lane of an adopted pull request. */
    ADOPT("adopt"),

    /** The lane of captured local changes. */
    LAND("land");

    private final String wireValue;

    SourceLane(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a lane from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching lane
     * @throws IllegalArgumentException if no lane carries that value
     */
    public static SourceLane fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Source lane must not be null");
        }
        var clean = value.trim();
        return Arrays.stream(values())
                .filter(lane -> lane.wireValue.equalsIgnoreCase(clean))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown source lane: " + value));
    }
}
