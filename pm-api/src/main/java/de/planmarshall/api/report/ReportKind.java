/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.report;

import java.util.Objects;

/**
 * Closed enumeration of the 16 report kinds emitted by the runtime (UC-PLN-6).
 */
public enum ReportKind {
    PLAN_STARTED("plan.started"),
    PLAN_ENDED("plan.ended"),
    PLAN_REVISED("plan.revised"),
    PLAN_HOLD_RELEASED("plan.hold_released"),
    EPIC_LAUNCHABLE("epic.launchable"),
    EPIC_WAITS_OVERLAP("epic.waits_overlap"),
    EPIC_OVERLAP_FOUND("epic.overlap_found"),
    EPIC_RECUT_APPLIED("epic.recut_applied"),
    EPIC_THROUGH("epic.through"),
    EPIC_ENDED("epic.ended"),
    PROJECT_ONBOARDED("project.onboarded"),
    PROJECT_RELEASED("project.released"),
    SOURCE_FLAGGED("source.flagged"),
    DECISION_CLOSED_ELSEWHERE("decision.closed_elsewhere"),
    TASK_OFF_TRACK("task.off_track"),
    SYSTEM_CLEANUP_DUE("system.cleanup_due");

    private final String value;

    ReportKind(String value) {
        this.value = value;
    }

    /**
     * Returns the wire value (e.g. {@code "plan.started"}).
     *
     * @return the report kind string
     */
    public String value() {
        return value;
    }

    /**
     * Resolves an enum constant from its wire value.
     *
     * @param value wire string value
     * @return matching ReportKind
     * @throws IllegalArgumentException if no match is found
     */
    public static ReportKind fromValue(String value) {
        Objects.requireNonNull(value, "value");
        for (ReportKind kind : values()) {
            if (kind.value.equalsIgnoreCase(value) || kind.name().equalsIgnoreCase(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown ReportKind: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
