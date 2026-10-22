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

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of what became of a review item (design <em>Reviewing Findings or Watches</em>,
 * UC-CTL-5's review).
 *
 * <p><em>Keep</em> keeps the item as a finding or moves it to the watches; <em>Drop</em> drops it
 * with a reason; <em>Promote to a plan</em> stages a row and marks the item promoted. An item not
 * yet decided is {@code open}.
 */
public enum ReviewDisposition {
    OPEN("open"),
    KEPT("kept"),
    WATCH("watch"),
    DROPPED("dropped"),
    PROMOTED("promoted");

    private final String value;

    ReviewDisposition(String value) {
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
    public static @Nullable ReviewDisposition fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var clean = value.trim().toLowerCase(Locale.ROOT);
        for (ReviewDisposition disposition : values()) {
            if (disposition.value.equals(clean) || disposition.name().equalsIgnoreCase(clean)) {
                return disposition;
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
    public static ReviewDisposition parse(String value) {
        var disposition = fromValue(value);
        if (disposition == null) {
            throw new IllegalArgumentException("Unknown ReviewDisposition: " + value);
        }
        return disposition;
    }
}
