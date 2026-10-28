/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.system;

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of the status of a model row in the system view.
 * <p>
 * The value is derived from the row's counters rather than served: {@link ModelMetric} carries no
 * status field. A row whose calls were not measured reads {@code not_measured}; otherwise a row with
 * errors or retries reads each counter it has — a row can carry both, so the view shows both badges —
 * and a row with neither reads {@code ok}.
 */
public enum ModelStatus {
    OK("ok"),
    RETRY("retry"),
    ERROR("error"),
    NOT_MEASURED("not_measured");

    private final String value;

    ModelStatus(String value) {
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
    public static @Nullable ModelStatus fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
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
    public static ModelStatus parse(String value) {
        var status = fromValue(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown ModelStatus: " + value);
        }
        return status;
    }
}
