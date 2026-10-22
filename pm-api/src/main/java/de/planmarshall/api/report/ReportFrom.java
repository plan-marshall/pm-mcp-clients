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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Origin of a report (project, optional epic, optional plan).
 *
 * @param projectId    identifier of the project
 * @param epicId       optional identifier of the epic
 * @param planId       optional identifier of the plan
 * @param projectTitle optional display title of the project
 * @param epicTitle    optional display title of the epic
 * @param planTitle    optional display title of the plan
 * @param detailLink   optional hypermedia target link
 */
public record ReportFrom(
        String projectId,
        @Nullable String epicId,
        @Nullable String planId,
        @Nullable String projectTitle,
        @Nullable String epicTitle,
        @Nullable String planTitle,
        @Nullable String detailLink
) {
    private static final String FIELD_PROJECT_ID = "project_id";
    private static final String FIELD_EPIC_ID = "epic_id";
    private static final String FIELD_PLAN_ID = "plan_id";
    private static final String FIELD_PROJECT_TITLE = "project_title";
    private static final String FIELD_EPIC_TITLE = "epic_title";
    private static final String FIELD_PLAN_TITLE = "plan_title";
    private static final String FIELD_DETAIL_LINK = "detail_link";

    public ReportFrom {
        Objects.requireNonNull(projectId, "projectId");
    }

    /**
     * Compact convenience constructor.
     *
     * @param projectId identifier of the project
     * @param epicId    optional epic identifier
     * @param planId    optional plan identifier
     */
    public ReportFrom(String projectId, @Nullable String epicId, @Nullable String planId) {
        this(projectId, epicId, planId, null, null, null, null);
    }

    /**
     * Project identifier.
     *
     * @return project identifier
     */
    public String project() {
        return projectId;
    }

    /**
     * Optional epic identifier.
     *
     * @return epic identifier or null
     */
    public @Nullable String epic() {
        return epicId;
    }

    /**
     * Optional plan identifier.
     *
     * @return plan identifier or null
     */
    public @Nullable String plan() {
        return planId;
    }

    /**
     * True if origin is the local host machine or system scope.
     *
     * @return boolean
     */
    public boolean isSystem() {
        return "system".equalsIgnoreCase(projectId) || "this_machine".equalsIgnoreCase(projectId);
    }

    /**
     * Formats the breadcrumb path string, e.g. {@code "pm-mcp-clients › relay-hardening › fix-relay"}.
     *
     * @return formatted path
     */
    public String path() {
        var sb = new StringBuilder(projectId);
        if (epicId != null && !epicId.isBlank()) {
            sb.append(" › ").append(epicId);
        }
        if (planId != null && !planId.isBlank()) {
            sb.append(" › ").append(planId);
        }
        return sb.toString();
    }


    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        if (epicId != null) map.put(FIELD_EPIC_ID, epicId);
        if (planId != null) map.put(FIELD_PLAN_ID, planId);
        if (projectTitle != null) map.put(FIELD_PROJECT_TITLE, projectTitle);
        if (epicTitle != null) map.put(FIELD_EPIC_TITLE, epicTitle);
        if (planTitle != null) map.put(FIELD_PLAN_TITLE, planTitle);
        if (detailLink != null) map.put(FIELD_DETAIL_LINK, detailLink);
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a map representation.
     *
     * @param map tree map
     * @return parsed ReportFrom
     * @throws IOException on format failure
     */
    public static ReportFrom fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            projectId = JsonTree.string(map, "projectId");
        }
        if (projectId == null) {
            projectId = JsonTree.string(map, "project");
        }
        if (projectId == null) {
            throw new JsonFormatException("Missing project_id in ReportFrom");
        }

        var epicId = JsonTree.string(map, FIELD_EPIC_ID);
        if (epicId == null) epicId = JsonTree.string(map, "epicId");
        if (epicId == null) epicId = JsonTree.string(map, "epic");

        var planId = JsonTree.string(map, FIELD_PLAN_ID);
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) planId = JsonTree.string(map, "plan");

        var projectTitle = JsonTree.string(map, FIELD_PROJECT_TITLE);
        if (projectTitle == null) projectTitle = JsonTree.string(map, "projectTitle");

        var epicTitle = JsonTree.string(map, FIELD_EPIC_TITLE);
        if (epicTitle == null) epicTitle = JsonTree.string(map, "epicTitle");

        var planTitle = JsonTree.string(map, FIELD_PLAN_TITLE);
        if (planTitle == null) planTitle = JsonTree.string(map, "planTitle");

        var detailLink = JsonTree.string(map, FIELD_DETAIL_LINK);
        if (detailLink == null) detailLink = JsonTree.string(map, "detailLink");

        return new ReportFrom(projectId, epicId, planId, projectTitle, epicTitle, planTitle, detailLink);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed ReportFrom
     * @throws IOException on parse failure
     */
    public static ReportFrom fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ReportFrom");
        }
        return fromMap(obj);
    }
}
