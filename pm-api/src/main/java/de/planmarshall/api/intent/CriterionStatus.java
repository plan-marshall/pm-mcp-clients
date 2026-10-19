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
 * The closed list of the verdicts a plan's intent criterion carries after the self-review (Phase
 * Workflows, The Intent Record; design <em>Intent and Outline</em>).
 * <p>
 * A criterion is {@link #MET} when its verification passed, {@link #NOT_MET} when the verification
 * failed, and {@link #NOT_VERIFIED} when the plan states honestly that it could not verify it. The
 * last two carry a reason; a client never infers a verdict from a deliverable's state.
 */
public enum CriterionStatus {

    /** The criterion is satisfied and its verification passed. */
    MET("met"),

    /** The criterion is not satisfied. */
    NOT_MET("not_met"),

    /** The criterion could not be verified, with the reason the self-review gives. */
    NOT_VERIFIED("not_verified");

    private final String wireValue;

    CriterionStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the status requires the reviewer's reason
     */
    public boolean requiresReason() {
        return this == NOT_MET || this == NOT_VERIFIED;
    }

    /**
     * Resolves a status from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching status, or {@code null} if none matches
     */
    public static CriterionStatus fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var status : values()) {
            if (status.wireValue.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }

    /**
     * Resolves a status from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching status
     * @throws IllegalArgumentException if no status carries that value (fail-closed)
     */
    public static CriterionStatus fromWire(String value) {
        var status = fromWireOrNull(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown criterion status: " + value);
        }
        return status;
    }

    /**
     * @return the wire values of every status, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(CriterionStatus::wireValue).toList();
    }
}
