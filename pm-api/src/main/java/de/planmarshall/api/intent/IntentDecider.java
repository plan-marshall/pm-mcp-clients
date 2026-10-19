/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.intent;

import java.util.Arrays;
import java.util.List;

/**
 * The closed list of who took a decision of a plan's intent record (Phase Workflows, The Intent
 * Record; design <em>Intent and Outline</em>).
 * <p>
 * The separation is load-bearing: a client shows the operator's and a quorum's decisions apart from
 * the model's own calls and the facts the repository settled, so that a model's choice is never read
 * as the operator's.
 */
public enum IntentDecider {

    /** The operator decided, in answer to a question. */
    OPERATOR("operator"),

    /** A quorum of the roles decided. */
    QUORUM("quorum"),

    /** The model decided as its own call. */
    MODEL("model"),

    /** A fact of the repository or of a source settled it. */
    FACT("fact");

    private final String wireValue;

    IntentDecider(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the decision belongs to the operator or to a quorum, never to the model
     */
    public boolean operatorFacing() {
        return this == OPERATOR || this == QUORUM;
    }

    /**
     * Resolves a decider from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching decider, or {@code null} if none matches
     */
    public static IntentDecider fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var decider : values()) {
            if (decider.wireValue.equalsIgnoreCase(value.trim())) {
                return decider;
            }
        }
        return null;
    }

    /**
     * Resolves a decider from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching decider
     * @throws IllegalArgumentException if no decider carries that value (fail-closed)
     */
    public static IntentDecider fromWire(String value) {
        var decider = fromWireOrNull(value);
        if (decider == null) {
            throw new IllegalArgumentException("Unknown intent decider: " + value);
        }
        return decider;
    }

    /**
     * @return the wire values of every decider, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(IntentDecider::wireValue).toList();
    }
}
