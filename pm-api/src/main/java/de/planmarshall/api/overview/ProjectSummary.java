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

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Summary representation of an enrolled project in the Running Work overview (UC-PLN-4).
 *
 * @param projectId            unique identifier of the project
 * @param displayName          display name of the project
 * @param activePlansCount     count of active plans in the project
 * @param activeEpicsCount     count of active epics in the project
 * @param needsOperatorCount   count of items waiting for the operator
 * @param needsOperatorSummary summary description of first waiting item (e.g. "decision")
 * @param setupNeedsOperator   whether the project's setup is waiting for operator input
 * @param lastActivityAt       timestamp of most recent activity
 */
public record ProjectSummary(
        String projectId,
        String displayName,
        int activePlansCount,
        int activeEpicsCount,
        int needsOperatorCount,
        @Nullable String needsOperatorSummary,
        boolean setupNeedsOperator,
        @Nullable Instant lastActivityAt
) {
    private static final String FIELD_PROJECT_ID = "project_id";
    private static final String FIELD_DISPLAY_NAME = "display_name";
    private static final String FIELD_ACTIVE_PLANS = "active_plans_count";
    private static final String FIELD_ACTIVE_EPICS = "active_epics_count";
    private static final String FIELD_NEEDS_OPERATOR_COUNT = "needs_operator_count";
    private static final String FIELD_NEEDS_OPERATOR_SUMMARY = "needs_operator_summary";
    private static final String FIELD_SETUP_NEEDS_OPERATOR = "setup_needs_operator";
    private static final String FIELD_LAST_ACTIVITY_AT = "last_activity_at";

    public ProjectSummary {
        Objects.requireNonNull(projectId, "projectId");
        displayName = displayName == null || displayName.isBlank() ? projectId : displayName;
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_DISPLAY_NAME, displayName);
        map.put(FIELD_ACTIVE_PLANS, activePlansCount);
        map.put(FIELD_ACTIVE_EPICS, activeEpicsCount);
        map.put(FIELD_NEEDS_OPERATOR_COUNT, needsOperatorCount);
        if (needsOperatorSummary != null) {
            map.put(FIELD_NEEDS_OPERATOR_SUMMARY, needsOperatorSummary);
        }
        map.put(FIELD_SETUP_NEEDS_OPERATOR, setupNeedsOperator);
        if (lastActivityAt != null) {
            map.put(FIELD_LAST_ACTIVITY_AT, lastActivityAt.toString());
        }
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
     * Parses from JSON tree map.
     *
     * @param map the input map
     * @return parsed summary
     * @throws IOException on format failure
     */
    public static ProjectSummary fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null) {
            throw new JsonFormatException("Missing project_id in ProjectSummary");
        }

        var displayName = JsonTree.string(map, FIELD_DISPLAY_NAME);
        if (displayName == null) {
            displayName = JsonTree.string(map, "name");
        }
        if (displayName == null) {
            displayName = projectId;
        }

        var plansNum = JsonTree.number(map, FIELD_ACTIVE_PLANS);
        if (plansNum == null) {
            plansNum = JsonTree.number(map, "plans_count");
        }
        int activePlans = plansNum != null ? plansNum.intValue() : 0;

        var epicsNum = JsonTree.number(map, FIELD_ACTIVE_EPICS);
        if (epicsNum == null) {
            epicsNum = JsonTree.number(map, "epics_count");
        }
        int activeEpics = epicsNum != null ? epicsNum.intValue() : 0;

        var needsCountNum = JsonTree.number(map, FIELD_NEEDS_OPERATOR_COUNT);
        int needsCount = needsCountNum != null ? needsCountNum.intValue() : 0;

        var needsSummary = JsonTree.string(map, FIELD_NEEDS_OPERATOR_SUMMARY);
        var setupNeeds = Boolean.TRUE.equals(map.get(FIELD_SETUP_NEEDS_OPERATOR))
                || Boolean.TRUE.equals(map.get("setupNeedsOperator"));

        var lastActivityStr = JsonTree.string(map, FIELD_LAST_ACTIVITY_AT);
        Instant lastActivity = lastActivityStr != null ? Instant.parse(lastActivityStr) : null;

        return new ProjectSummary(
                projectId,
                displayName,
                activePlans,
                activeEpics,
                needsCount,
                needsSummary,
                setupNeeds,
                lastActivity
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed summary
     * @throws IOException on parse failure
     */
    public static ProjectSummary fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ProjectSummary");
        }
        return fromMap(obj);
    }

    /**
     * Parses a list of project summaries from JSON array string.
     *
     * @param json JSON array string
     * @return list of project summaries
     * @throws IOException on parse failure
     */
    public static List<ProjectSummary> parseList(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (!(parsed instanceof List<?> list)) {
            throw new JsonFormatException("Expected JSON array for ProjectSummary list");
        }
        var result = new ArrayList<ProjectSummary>(list.size());
        for (var item : list) {
            var map = JsonTree.asObject(item);
            if (map != null) {
                result.add(fromMap(map));
            }
        }
        return result;
    }

    /**
     * Serializes a list of project summaries to JSON array string.
     *
     * @param summaries list of project summaries
     * @return JSON string
     */
    public static String writeList(List<ProjectSummary> summaries) {
        var list = summaries.stream().map(ProjectSummary::toMap).toList();
        return JsonTree.write(list);
    }
}
