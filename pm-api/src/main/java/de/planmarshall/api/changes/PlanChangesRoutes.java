/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.changes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The HTTP paths of a plan's changes ({@code 01-processes-and-transport.adoc#plan-changes}): the
 * list, and one file's hunks by its {@code path} as a query value.
 * <p>
 * The plan id is encoded as one path segment and the file's path as a query value; a blank id or
 * path is refused.
 */
public final class PlanChangesRoutes {

    /** The prefix of every path of the local API. */
    public static final String API_PREFIX = "/api/v1";

    private static final String CHANGES = "/changes";
    private static final String FILE = "/file";
    private static final String PATH_QUERY = "path=";

    private final String planPath;

    private PlanChangesRoutes(String planPath) {
        this.planPath = planPath;
    }

    /**
     * @param planId the plan's id
     * @return the routes of that plan's changes
     */
    public static PlanChangesRoutes plan(String planId) {
        var id = Objects.requireNonNull(planId, "planId");
        if (id.isBlank()) {
            throw new IllegalArgumentException("planId must not be blank");
        }
        if (".".equals(id) || "..".equals(id)) {
            throw new IllegalArgumentException("planId must not be a dot segment");
        }
        return new PlanChangesRoutes(API_PREFIX + "/plans/"
                + URLEncoder.encode(id, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    /**
     * @return the path of the plan's list of changes
     */
    public String list() {
        return planPath + CHANGES;
    }

    /**
     * @param path the file's path in the capture
     * @return the path of that file's hunks
     */
    public String file(String path) {
        var filePath = Objects.requireNonNull(path, "path");
        if (filePath.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        return list() + FILE + "?" + PATH_QUERY
                + URLEncoder.encode(filePath, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
