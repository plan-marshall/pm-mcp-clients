/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.scenario;

import java.util.Objects;

/**
 * Definition of an offered hypermedia link in a scenario state.
 *
 * @param next    the target state id this link moves to
 * @param method  the expected HTTP method, default {@code POST} if not specified
 * @param href    the expected URI path, or {@code null} to match by relation suffix
 * @param delayMs optional artificial delay in milliseconds before answering
 * @param status  HTTP status to answer (typically 200, or 409 on domain refusal)
 * @param error   optional error code string for non-2xx responses
 */
public record LinkDefinition(String next, String method, String href, long delayMs, int status, String error) {

    /** Default status code for a successful link transition. */
    public static final int DEFAULT_STATUS = 200;

    /** Default HTTP method for a link transition. */
    public static final String DEFAULT_METHOD = "POST";

    /**
     * Compact constructor with defaults and normalization.
     */
    public LinkDefinition {
        Objects.requireNonNull(next, "next");
        method = method == null || method.isBlank() ? DEFAULT_METHOD : method.toUpperCase();
        if (status <= 0) {
            status = DEFAULT_STATUS;
        }
    }

    /**
     * Convenience factory for a simple forward link.
     *
     * @param next the target state id
     * @return the link definition
     */
    public static LinkDefinition of(String next) {
        return new LinkDefinition(next, DEFAULT_METHOD, null, 0, DEFAULT_STATUS, null);
    }
}
