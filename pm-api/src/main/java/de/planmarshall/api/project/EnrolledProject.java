/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.project;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Representation of an enrolled project in the Project Explorer (UC-PRJ-2).
 *
 * @param projectId            unique identifier of the project
 * @param displayName          display name of the project
 * @param remoteUrl            remote Git URL or identifier, if known
 * @param readiness            readiness state of the project
 * @param setupNeedsSummary    what setup needs if not ready
 * @param needsOperatorCount   count of items needing operator attention
 * @param activePlansCount     count of active plans in the project
 * @param activeEpicsCount     count of active epics in the project
 * @param lastActive           timestamp of most recent activity
 * @param isFavourite          whether marked as favourite
 * @param isReleased           whether the project has been released
 */
public record EnrolledProject(
        String projectId,
        String displayName,
        @Nullable String remoteUrl,
        ProjectReadiness readiness,
        @Nullable String setupNeedsSummary,
        int needsOperatorCount,
        int activePlansCount,
        int activeEpicsCount,
        @Nullable Instant lastActive,
        boolean isFavourite,
        boolean isReleased
) {
    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_DISPLAY_NAME = "display_name";
    public static final String FIELD_REMOTE_URL = "remote_url";
    public static final String FIELD_READINESS = "readiness";
    public static final String FIELD_SETUP_NEEDS_SUMMARY = "setup_needs_summary";
    public static final String FIELD_NEEDS_OPERATOR_COUNT = "needs_operator_count";
    public static final String FIELD_ACTIVE_PLANS_COUNT = "active_plans_count";
    public static final String FIELD_ACTIVE_EPICS_COUNT = "active_epics_count";
    public static final String FIELD_LAST_ACTIVE = "last_active";
    public static final String FIELD_IS_FAVOURITE = "is_favourite";
    public static final String FIELD_IS_RELEASED = "is_released";

    public EnrolledProject {
        Objects.requireNonNull(projectId, "projectId");
        displayName = displayName == null || displayName.isBlank() ? projectId : displayName;
        readiness = readiness != null ? readiness : ProjectReadiness.READY;
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
        if (remoteUrl != null) {
            map.put(FIELD_REMOTE_URL, remoteUrl);
        }
        map.put(FIELD_READINESS, readiness.value());
        if (setupNeedsSummary != null) {
            map.put(FIELD_SETUP_NEEDS_SUMMARY, setupNeedsSummary);
        }
        map.put(FIELD_NEEDS_OPERATOR_COUNT, needsOperatorCount);
        map.put(FIELD_ACTIVE_PLANS_COUNT, activePlansCount);
        map.put(FIELD_ACTIVE_EPICS_COUNT, activeEpicsCount);
        if (lastActive != null) {
            map.put(FIELD_LAST_ACTIVE, lastActive.toString());
        }
        map.put(FIELD_IS_FAVOURITE, isFavourite);
        map.put(FIELD_IS_RELEASED, isReleased);
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
     * Parses an enrolled project from a JSON tree map.
     *
     * @param map input map
     * @return parsed enrolled project
     * @throws IOException on format failure
     */
    public static EnrolledProject fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new JsonFormatException("Missing project_id in EnrolledProject");
        }

        var displayName = JsonTree.string(map, FIELD_DISPLAY_NAME);
        if (displayName == null) {
            displayName = JsonTree.string(map, "name");
        }

        var remoteUrl = JsonTree.string(map, FIELD_REMOTE_URL);
        if (remoteUrl == null) {
            remoteUrl = JsonTree.string(map, "repo");
        }

        var readinessStr = JsonTree.string(map, FIELD_READINESS);
        if (readinessStr == null) {
            readinessStr = JsonTree.string(map, "state");
        }
        var readiness = ProjectReadiness.fromValue(readinessStr);
        if (readiness == null) {
            readiness = ProjectReadiness.READY;
        }

        var setupNeedsSummary = JsonTree.string(map, FIELD_SETUP_NEEDS_SUMMARY);
        if (setupNeedsSummary == null) {
            setupNeedsSummary = JsonTree.string(map, "setup_needs");
        }

        var needsOp = JsonTree.integer(map, FIELD_NEEDS_OPERATOR_COUNT);
        int needsOperatorCount = needsOp != null ? needsOp : 0;

        var plans = JsonTree.integer(map, FIELD_ACTIVE_PLANS_COUNT);
        if (plans == null) {
            plans = JsonTree.integer(map, "plans_count");
        }
        int activePlansCount = plans != null ? plans : 0;

        var epics = JsonTree.integer(map, FIELD_ACTIVE_EPICS_COUNT);
        if (epics == null) {
            epics = JsonTree.integer(map, "epics_count");
        }
        int activeEpicsCount = epics != null ? epics : 0;

        var lastActiveStr = JsonTree.string(map, FIELD_LAST_ACTIVE);
        Instant lastActive = null;
        if (lastActiveStr != null && !lastActiveStr.isBlank()) {
            try {
                lastActive = Instant.parse(lastActiveStr);
            }
            /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
            catch (Exception _) {
                // Ignore parse errors, leave null
            }
        }

        var isFav = JsonTree.bool(map, FIELD_IS_FAVOURITE);
        if (isFav == null) {
            isFav = JsonTree.bool(map, "favourite");
        }
        boolean isFavourite = Boolean.TRUE.equals(isFav);

        var isRel = JsonTree.bool(map, FIELD_IS_RELEASED);
        if (isRel == null) {
            isRel = JsonTree.bool(map, "released");
        }
        boolean isReleased = Boolean.TRUE.equals(isRel) || readiness == ProjectReadiness.RELEASED;

        return new EnrolledProject(
                projectId,
                displayName,
                remoteUrl,
                readiness,
                setupNeedsSummary,
                needsOperatorCount,
                activePlansCount,
                activeEpicsCount,
                lastActive,
                isFavourite,
                isReleased
        );
    }

    /**
     * Parses an enrolled project from a JSON string.
     *
     * @param json JSON string
     * @return parsed enrolled project
     * @throws IOException on format failure
     */
    public static EnrolledProject fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for EnrolledProject");
    }
}
