/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api;

/**
 * The paths of the runtime's operator API that the machine's own resources are read at (UC-PLN-7,
 * UC-PLN-8).
 * <p>
 * These are the HTTP paths of the local API, never the filesystem layout: {@link MachinePaths} names
 * the machine root, its socket and its locks, and holds no request path.
 */
public final class MachineApiPaths {

    /** The machine resource, which carries the machine's {@code needs_operator} summary. */
    public static final String MACHINE = "/api/v1/machine";

    /** The one list of everything that waits, a page at a time. */
    public static final String NEEDS_OPERATOR = "/api/v1/machine/needs-operator";

    /** The search over running and ended work, matched by a part of an id or a title. */
    public static final String FIND = "/api/v1/find";

    /** The query parameter of {@link #FIND}; an empty value is refused. */
    public static final String QUERY_PARAM = "q";

    private MachineApiPaths() {
        // Constants only
    }
}
