/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import org.jspecify.annotations.Nullable;

/**
 * The closed list of what one {@link HarnessVerificationStage} did (UC-ONB-4): it passed, it failed
 * (naming the field of the draft it concerns), or it was not run because an earlier stage failed.
 *
 * <p>The list is closed and fails closed: a status read from the wire that is not a member is
 * refused by {@link #parse(String)}.
 */
public enum VerificationStageStatus {

    /** The stage passed. */
    PASSED("passed"),

    /** The stage failed; its fix points at the field of the draft it concerns. */
    FAILED("failed"),

    /** The stage was not run because an earlier one failed. */
    NOT_RUN("not_run");

    private final String wireValue;

    VerificationStageStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value of the status, e.g. {@code not_run}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a status from its wire value or its enum name.
     *
     * @param value wire value, e.g. {@code passed}, or enum name
     * @return the status, or {@code null} if unmatched
     */
    public static @Nullable VerificationStageStatus fromWire(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var status : values()) {
            if (status.wireValue.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Resolves a status from its wire value or its enum name.
     *
     * @param value wire value or enum name
     * @return the status
     * @throws IllegalArgumentException if the value is unknown (fail-closed)
     */
    public static VerificationStageStatus parse(String value) {
        var status = fromWire(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown VerificationStageStatus: " + value);
        }
        return status;
    }
}
