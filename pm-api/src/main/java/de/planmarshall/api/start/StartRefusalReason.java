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
 * The closed list of the reasons a start refuses to open (State Catalogue, {@code start.refused} and
 * {@code start-epic.refused}; design <em>Starting Work</em>, the setup gate).
 * <p>
 * The start opens on the refusal instead of its sources: what is missing is named, together with the
 * one action that goes on where it is fixed, and nothing is created. The wire value is the one of the
 * message key {@code start.refused.<wire>}.
 */
public enum StartRefusalReason {

    /** The machine's onboarding is not complete. */
    MACHINE_NOT_ONBOARDED("machine_not_onboarded"),

    /** The project is not ready. */
    PROJECT_NOT_READY("project_not_ready");

    private final String wireValue;

    StartRefusalReason(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a reason from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching reason, or {@code null} if none matches
     */
    public static StartRefusalReason fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var reason : values()) {
            if (reason.wireValue.equalsIgnoreCase(value.trim())) {
                return reason;
            }
        }
        return null;
    }

    /**
     * Resolves a reason from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching reason
     * @throws IllegalArgumentException if no reason carries that value (fail-closed)
     */
    public static StartRefusalReason fromWire(String value) {
        var reason = fromWireOrNull(value);
        if (reason == null) {
            throw new IllegalArgumentException("Unknown start refusal reason: " + value);
        }
        return reason;
    }

    /**
     * @return the wire values of every reason, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(StartRefusalReason::wireValue).toList();
    }
}
