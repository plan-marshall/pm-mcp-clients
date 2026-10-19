/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of what a staged plan of an epic waits for ({@code epic.next-work}, design
 * <em>An Epic's Actions</em> and <em>Epic View, What the Epic Does by Itself</em>).
 *
 * <p>A staged row is either {@code can start} ({@link #NONE}) or waits for one of these reasons;
 * an {@code overlap} names the running plan it waits for and the files in common, a
 * {@code dependency} the staged plan that must land first, and {@code scope} a full place.
 */
public enum EpicWaitReason {
    NONE("none"),
    DEPENDENCY("dependency"),
    LOCK("lock"),
    SCOPE("scope"),
    OVERLAP("overlap"),
    STALE("stale"),
    HELD("held");

    private final String value;

    EpicWaitReason(String value) {
        this.value = value;
    }

    /**
     * @return the wire string value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves an enum constant from its wire string value.
     *
     * @param value wire string
     * @return enum constant, or {@code null} if unmatched
     */
    public static @Nullable EpicWaitReason fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (EpicWaitReason reason : values()) {
            if (reason.value.equalsIgnoreCase(value) || reason.name().equalsIgnoreCase(value)) {
                return reason;
            }
        }
        return null;
    }

    /**
     * Resolves an enum constant or throws.
     *
     * @param value wire string
     * @return enum constant
     */
    public static EpicWaitReason parse(String value) {
        var reason = fromValue(value);
        if (reason == null) {
            throw new IllegalArgumentException("Unknown EpicWaitReason: " + value);
        }
        return reason;
    }
}
