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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Detailed project overview response representing active plans, epics, and alongside landings
 * within a single project ({@code GET /api/v1/projects/{projectId}}).
 *
 * @param projectId         project identifier
 * @param displayName       human-readable project name
 * @param plans             active plans in the project
 * @param epics             active epics in the project
 * @param alongsideLandings short landings running alongside plans/epics
 */
public record ProjectDetailResponse(
        String projectId,
        String displayName,
        List<ActivePlanSummary> plans,
        List<ActiveEpicSummary> epics,
        List<AlongsideLanding> alongsideLandings
) {
    public ProjectDetailResponse {
        Objects.requireNonNull(projectId, "projectId");
        displayName = displayName == null || displayName.isBlank() ? projectId : displayName;
        plans = plans != null ? List.copyOf(plans) : List.of();
        epics = epics != null ? List.copyOf(epics) : List.of();
        alongsideLandings = alongsideLandings != null ? List.copyOf(alongsideLandings) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("project_id", projectId);
        map.put("display_name", displayName);
        map.put("plans", plans.stream().map(ActivePlanSummary::toMap).toList());
        map.put("epics", epics.stream().map(ActiveEpicSummary::toMap).toList());
        map.put("alongside_landings", alongsideLandings.stream().map(AlongsideLanding::toMap).toList());
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
     * @return parsed response
     * @throws IOException on format failure
     */
    public static ProjectDetailResponse fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, "project_id");
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null) {
            throw new JsonFormatException("Missing project_id in ProjectDetailResponse");
        }

        var displayName = JsonTree.string(map, "display_name");
        if (displayName == null) {
            displayName = JsonTree.string(map, "name");
        }
        if (displayName == null) {
            displayName = projectId;
        }

        var plansListObj = JsonTree.asArray(map.get("plans"));
        var plans = new ArrayList<ActivePlanSummary>();
        if (plansListObj != null) {
            for (var elem : plansListObj) {
                var pMap = JsonTree.asObject(elem);
                if (pMap != null) {
                    plans.add(ActivePlanSummary.fromMap(pMap));
                }
            }
        }

        var epicsListObj = JsonTree.asArray(map.get("epics"));
        var epics = new ArrayList<ActiveEpicSummary>();
        if (epicsListObj != null) {
            for (var elem : epicsListObj) {
                var eMap = JsonTree.asObject(elem);
                if (eMap != null) {
                    epics.add(ActiveEpicSummary.fromMap(eMap));
                }
            }
        }

        var landingsListObj = JsonTree.asArray(map.get("alongside_landings"));
        if (landingsListObj == null) {
            landingsListObj = JsonTree.asArray(map.get("alongside"));
        }
        var alongside = new ArrayList<AlongsideLanding>();
        if (landingsListObj != null) {
            for (var elem : landingsListObj) {
                var lMap = JsonTree.asObject(elem);
                if (lMap != null) {
                    alongside.add(AlongsideLanding.fromMap(lMap));
                }
            }
        }

        return new ProjectDetailResponse(projectId, displayName, plans, epics, alongside);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed response
     * @throws IOException on format failure
     */
    public static ProjectDetailResponse fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ProjectDetailResponse");
        }
        return fromMap(obj);
    }
}
