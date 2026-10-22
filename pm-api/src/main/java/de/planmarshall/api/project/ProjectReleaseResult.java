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
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Result payload returned when a project is released (UC-PRJ-3).
 *
 * @param projectId           identifier of the released project
 * @param plansEndedCount     number of plans ended and archived as released
 * @param epicsEndedCount     number of epics ended and archived as released
 * @param entriesDeletedCount number of pending unstarted entries deleted
 * @param releasedAt          timestamp when release took effect
 * @param landingWaiting      description of active landing waited for, if any
 * @param waitStatus          outcome status of landing wait
 */
public record ProjectReleaseResult(
        String projectId,
        int plansEndedCount,
        int epicsEndedCount,
        int entriesDeletedCount,
        Instant releasedAt,
        @Nullable String landingWaiting,
        ReleaseWaitStatus waitStatus
) {
    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_PLANS_ENDED_COUNT = "plans_ended_count";
    public static final String FIELD_EPICS_ENDED_COUNT = "epics_ended_count";
    public static final String FIELD_ENTRIES_DELETED_COUNT = "entries_deleted_count";
    public static final String FIELD_RELEASED_AT = "released_at";
    public static final String FIELD_LANDING_WAITING = "landing_waiting";
    public static final String FIELD_WAIT_STATUS = "wait_status";

    public ProjectReleaseResult {
        Objects.requireNonNull(projectId, "projectId");
        releasedAt = releasedAt != null ? releasedAt : Instant.now();
        waitStatus = waitStatus != null ? waitStatus : ReleaseWaitStatus.NONE;
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_PLANS_ENDED_COUNT, plansEndedCount);
        map.put(FIELD_EPICS_ENDED_COUNT, epicsEndedCount);
        map.put(FIELD_ENTRIES_DELETED_COUNT, entriesDeletedCount);
        map.put(FIELD_RELEASED_AT, releasedAt.toString());
        if (landingWaiting != null) {
            map.put(FIELD_LANDING_WAITING, landingWaiting);
        }
        map.put(FIELD_WAIT_STATUS, waitStatus.value());
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
     * @param map input map
     * @return parsed release result
     * @throws IOException on format failure
     */
    public static ProjectReleaseResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new JsonFormatException("Missing project_id in ProjectReleaseResult");
        }

        var plans = JsonTree.integer(map, FIELD_PLANS_ENDED_COUNT);
        int plansEndedCount = plans != null ? plans : 0;

        var epics = JsonTree.integer(map, FIELD_EPICS_ENDED_COUNT);
        int epicsEndedCount = epics != null ? epics : 0;

        var entries = JsonTree.integer(map, FIELD_ENTRIES_DELETED_COUNT);
        int entriesDeletedCount = entries != null ? entries : 0;

        var relAtStr = JsonTree.string(map, FIELD_RELEASED_AT);
        Instant releasedAt = Instant.now();
        if (relAtStr != null && !relAtStr.isBlank()) {
            try {
                releasedAt = Instant.parse(relAtStr);
            } catch (DateTimeParseException _) {
                // Ignore parse errors, fallback to Instant.now()
            }
        }

        var landingWaiting = JsonTree.string(map, FIELD_LANDING_WAITING);

        var statusStr = JsonTree.string(map, FIELD_WAIT_STATUS);
        var waitStatus = ReleaseWaitStatus.fromValue(statusStr);
        if (waitStatus == null) {
            waitStatus = ReleaseWaitStatus.NONE;
        }

        return new ProjectReleaseResult(
                projectId,
                plansEndedCount,
                epicsEndedCount,
                entriesDeletedCount,
                releasedAt,
                landingWaiting,
                waitStatus
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed release result
     * @throws IOException on format failure
     */
    public static ProjectReleaseResult fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for ProjectReleaseResult");
    }
}
