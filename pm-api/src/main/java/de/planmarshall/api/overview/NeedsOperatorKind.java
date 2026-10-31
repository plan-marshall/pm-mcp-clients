/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of reasons why an item needs operator attention, in the order of the
 * specification: the decision, the waiver, any other operator wait, the holds, new findings, the
 * setup, then the machine's own items.
 */
public enum NeedsOperatorKind {
    DECISION("decision"),
    WAIVER("waiver"),
    WAITS_FOR_YOU("waits_for_you"),
    STALLED("stalled"),
    CONDITION("condition"),
    INTEGRITY_PAUSE("integrity_pause"),
    HELD("held"),
    LANDING_FAILED("landing_failed"),
    FINDINGS("findings"),
    SETUP("setup"),
    CREDENTIAL("credential"),
    CLEANUP_DUE("cleanup_due");

    private final String value;

    NeedsOperatorKind(String value) {
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
    public static @Nullable NeedsOperatorKind fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (NeedsOperatorKind kind : values()) {
            if (kind.value.equalsIgnoreCase(value) || kind.name().equalsIgnoreCase(value)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * Resolves an enum constant or throws IllegalArgumentException.
     *
     * @param value wire string
     * @return enum constant
     */
    public static NeedsOperatorKind parse(String value) {
        var kind = fromValue(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown NeedsOperatorKind: " + value);
        }
        return kind;
    }
}
