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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Top-level response container for the list of projects with running work ({@code GET /api/v1/projects}).
 *
 * @param projects list of projects with running work
 */
public record ProjectOverviewResponse(List<ProjectSummary> projects) {

    public ProjectOverviewResponse {
        projects = projects != null ? List.copyOf(projects) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        var list = projects.stream().map(ProjectSummary::toMap).toList();
        map.put("projects", list);
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
     * Parses from JSON string (supports either an array of projects or a container object).
     *
     * @param json JSON string
     * @return parsed response
     * @throws IOException on format failure
     */
    public static ProjectOverviewResponse fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof List<?> list) {
            var items = new ArrayList<ProjectSummary>(list.size());
            for (var elem : list) {
                var map = JsonTree.asObject(elem);
                if (map != null) {
                    items.add(ProjectSummary.fromMap(map));
                }
            }
            return new ProjectOverviewResponse(items);
        }
        var obj = JsonTree.asObject(parsed);
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object or array for ProjectOverviewResponse");
        }
        var listObj = JsonTree.asArray(obj.get("projects"));
        var items = new ArrayList<ProjectSummary>();
        if (listObj != null) {
            for (var elem : listObj) {
                var map = JsonTree.asObject(elem);
                if (map != null) {
                    items.add(ProjectSummary.fromMap(map));
                }
            }
        }
        return new ProjectOverviewResponse(items);
    }
}
