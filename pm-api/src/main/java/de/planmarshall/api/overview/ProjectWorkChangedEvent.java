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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a project's running work changes ({@code project.changed},
 * {@code project.work_started}, {@code project.work_ended}).
 *
 * @param projectId          unique project identifier
 * @param changeType         type of change ("work_started", "work_ended", "changed")
 * @param activePlansCount   updated count of active plans
 * @param activeEpicsCount   updated count of active epics
 * @param needsOperatorCount updated count of items needing operator
 */
public record ProjectWorkChangedEvent(
String projectId,
String changeType,
int activePlansCount,
int activeEpicsCount,
int needsOperatorCount
) {
    public static final String EVENT_CHANGED = "project.changed";
    public static final String EVENT_WORK_STARTED = "project.work_started";
    public static final String EVENT_WORK_ENDED = "project.work_ended";

    public ProjectWorkChangedEvent {
        Objects.requireNonNull(projectId, "projectId");
        changeType = changeType != null ? changeType : "changed";
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("project_id", projectId);
        map.put("change_type", changeType);
        map.put("active_plans_count", activePlansCount);
        map.put("active_epics_count", activeEpicsCount);
        map.put("needs_operator_count", needsOperatorCount);
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
     * @param map map representation
     * @return parsed event
     * @throws IOException on format failure
     */
    public static ProjectWorkChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, "project_id");
        if (projectId == null) {
            projectId = JsonTree.string(map, "projectId");
        }
        if (projectId == null) {
            throw new JsonFormatException("Missing project_id in ProjectWorkChangedEvent");
        }
        var changeType = JsonTree.string(map, "change_type");
        if (changeType == null) {
            changeType = JsonTree.string(map, "type");
        }

        var plansNum = JsonTree.number(map, "active_plans_count");
        int plans = plansNum != null ? plansNum.intValue() : 0;

        var epicsNum = JsonTree.number(map, "active_epics_count");
        int epics = epicsNum != null ? epicsNum.intValue() : 0;

        var needsNum = JsonTree.number(map, "needs_operator_count");
        int needs = needsNum != null ? needsNum.intValue() : 0;

        return new ProjectWorkChangedEvent(projectId, changeType, plans, epics, needs);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static ProjectWorkChangedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ProjectWorkChangedEvent");
        }
        return fromMap(obj);
    }
}
