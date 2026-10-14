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
 * Definition of a static route response independent of the state machine (e.g. status endpoints).
 *
 * @param representation the response payload
 * @param status         the HTTP status code (default 200)
 */
public record RouteDefinition(Object representation, int status) {

    /** Default HTTP status. */
    public static final int DEFAULT_STATUS = 200;

    /**
     * Compact constructor validating defaults.
     */
    public RouteDefinition {
        Objects.requireNonNull(representation, "representation");
        if (status <= 0) {
            status = DEFAULT_STATUS;
        }
    }

    /**
     * @param representation the representation payload
     * @return the route definition with status 200
     */
    public static RouteDefinition of(Object representation) {
        return new RouteDefinition(representation, DEFAULT_STATUS);
    }
}
