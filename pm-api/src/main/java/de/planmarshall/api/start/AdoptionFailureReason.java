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
 * The closed list of the reasons an epic's ledger cannot be adopted (State Catalogue,
 * {@code start-epic.adopt-failed}; design <em>Starting Work</em>, adopting an epic from elsewhere).
 * <p>
 * The wire value is the one of the {@code reason} member of the message key
 * {@code start.epic.adopt.failed}: {@code ledger_unreadable} names the failing file,
 * {@code ledger_invalid} says its ledger is not valid, and {@code ledger_terminal} says it has ended.
 */
public enum AdoptionFailureReason {

    /** A file of the ledger cannot be read. */
    LEDGER_UNREADABLE("ledger_unreadable"),

    /** The ledger's records fail their schemas. */
    LEDGER_INVALID("ledger_invalid"),

    /** The epic's header is terminal and is not adopted. */
    LEDGER_TERMINAL("ledger_terminal");

    private final String wireValue;

    AdoptionFailureReason(String wireValue) {
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
    public static AdoptionFailureReason fromWireOrNull(String value) {
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
    public static AdoptionFailureReason fromWire(String value) {
        var reason = fromWireOrNull(value);
        if (reason == null) {
            throw new IllegalArgumentException("Unknown adoption failure reason: " + value);
        }
        return reason;
    }

    /**
     * @return the wire values of every reason, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(AdoptionFailureReason::wireValue).toList();
    }
}
