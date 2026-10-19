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
 * The closed list of stages an unknown harness is run through when its draft profile is verified
 * (UC-ONB-4; Client Profiles, Harness Scan): the identity, detection, launch, isolation, classes,
 * limits, paths, faults, and usage stages, in that order. A state after a failing stage lists the
 * later ones as not run.
 *
 * <p>The list is closed and fails closed: a stage read from the wire that is not a member is
 * refused by {@link #parse(String)}.
 */
public enum HarnessVerificationStage {

    /** What the harness calls itself ({@code identity}). */
    IDENTITY("identity"),

    /** Whether the harness is detected the way its profile says ({@code detection}). */
    DETECTION("detection"),

    /** A headless launch answered ({@code launch}). */
    LAUNCH("launch"),

    /** The harness was confined the way its profile says ({@code isolation}). */
    ISOLATION("isolation"),

    /** The harness ran under its class settings ({@code classes}). */
    CLASSES("classes"),

    /** The harness honoured its limits ({@code limits}). */
    LIMITS("limits"),

    /** The harness read and wrote the paths its profile says ({@code paths}). */
    PATHS("paths"),

    /** The harness failed the way its profile says ({@code faults}). */
    FAULTS("faults"),

    /** What the harness reports of its token usage, the stage whose result is a {@link HarnessUsageReport}. */
    USAGE("usage");

    private final String wireValue;

    HarnessVerificationStage(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value of the stage, e.g. {@code isolation}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a stage from its wire value or its enum name.
     *
     * @param value wire value, e.g. {@code usage}, or enum name
     * @return the stage, or {@code null} if unmatched
     */
    public static @Nullable HarnessVerificationStage fromWire(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var stage : values()) {
            if (stage.wireValue.equalsIgnoreCase(value) || stage.name().equalsIgnoreCase(value)) {
                return stage;
            }
        }
        return null;
    }

    /**
     * Resolves a stage from its wire value or its enum name.
     *
     * @param value wire value or enum name
     * @return the stage
     * @throws IllegalArgumentException if the value is unknown (fail-closed)
     */
    public static HarnessVerificationStage parse(String value) {
        var stage = fromWire(value);
        if (stage == null) {
            throw new IllegalArgumentException("Unknown HarnessVerificationStage: " + value);
        }
        return stage;
    }
}
