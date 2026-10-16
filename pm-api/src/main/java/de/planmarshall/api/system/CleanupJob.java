/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Representation of an active or historical cleanup job (e.g. {@code cln-0012}).
 */
public record CleanupJob(
        String jobId,
        CleanupJobState state,
        String dueSince,
        CleanupSummary summary,
        List<CleanupProjectItem> projectItems,
        Map<String, String> links
) {
    public CleanupJob {
        Objects.requireNonNull(jobId, "jobId");
        state = state != null ? state : CleanupJobState.OPEN;
        dueSince = dueSince != null ? dueSince : "";
        Objects.requireNonNull(summary, "summary");
        projectItems = projectItems != null ? List.copyOf(projectItems) : List.of();
        links = links != null ? Collections.unmodifiableMap(new LinkedHashMap<>(links)) : Map.of();
    }

    /**
     * @return {@code true} if this cleanup job is open
     */
    public boolean isOpen() {
        return state == CleanupJobState.OPEN;
    }

    /**
     * Converts to JSON map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("job_id", jobId);
        map.put("state", state.value());
        map.put("due_since", dueSince);
        map.put("summary", summary.toMap());
        map.put("project_items", projectItems.stream().map(CleanupProjectItem::toMap).toList());
        map.put("links", links);
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
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed cleanup job
     * @throws IOException on error
     */
    public static CleanupJob fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var jobId = JsonTree.string(map, "job_id");
        if (jobId == null) {
            jobId = JsonTree.string(map, "id");
        }
        if (jobId == null) {
            throw new JsonFormatException("Missing job_id in CleanupJob");
        }
        var stateStr = JsonTree.string(map, "state");
        var state = CleanupJobState.fromValue(stateStr);
        var dueSince = JsonTree.string(map, "due_since");
        var summaryObj = JsonTree.asObject(map.get("summary"));
        if (summaryObj == null) {
            throw new JsonFormatException("Missing summary in CleanupJob");
        }
        var summary = CleanupSummary.fromMap(summaryObj);

        var projectItems = new ArrayList<CleanupProjectItem>();
        if (map.get("project_items") instanceof List<?> list) {
            for (var itemObj : list) {
                var itemMap = JsonTree.asObject(itemObj);
                if (itemMap != null) {
                    projectItems.add(CleanupProjectItem.fromMap(itemMap));
                }
            }
        }
        var links = new LinkedHashMap<String, String>();
        if (map.get("links") instanceof Map<?, ?> m) {
            for (var entry : m.entrySet()) {
                links.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return new CleanupJob(jobId, state, dueSince != null ? dueSince : "", summary, projectItems, links);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed cleanup job
     * @throws IOException on error
     */
    public static CleanupJob fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for CleanupJob");
        }
        return fromMap(obj);
    }
}
