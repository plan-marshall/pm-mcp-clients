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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when items waiting for the operator change on a project, plan, or epic, and,
 * with the machine's summary, on the machine itself ({@code needs_operator.changed}).
 *
 * @param projectId project identifier, or {@code null}
 * @param planId    plan identifier, or {@code null}
 * @param epicId    epic identifier, or {@code null}
 * @param kind      kind of wait ("decision", "stalled", "condition", "none"), or {@code null}
 * @param count     number of items waiting
 * @param summary   the machine's summary the event announces, or {@code null} for a plan, epic, or
 *                  project change
 */
public record NeedsOperatorChangedEvent(
        @Nullable String projectId,
        @Nullable String planId,
        @Nullable String epicId,
        @Nullable String kind,
        int count,
        @Nullable NeedsOperatorSummary summary
) {
    public static final String EVENT_NAME = "needs_operator.changed";

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (projectId != null) {
            map.put("project_id", projectId);
        }
        if (planId != null) {
            map.put("plan_id", planId);
        }
        if (epicId != null) {
            map.put("epic_id", epicId);
        }
        if (kind != null) {
            map.put("kind", kind);
        }
        map.put("count", count);
        if (summary != null) {
            map.put("needs_operator", summary.toMap());
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
     * @param map map representation
     * @return parsed event
     * @throws IOException on format failure
     */
    public static NeedsOperatorChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, "project_id");
        if (projectId == null) {
            projectId = JsonTree.string(map, "projectId");
        }
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            planId = JsonTree.string(map, "planId");
        }
        var epicId = JsonTree.string(map, "epic_id");
        if (epicId == null) {
            epicId = JsonTree.string(map, "epicId");
        }

        var kind = JsonTree.string(map, "kind");
        var countNum = JsonTree.number(map, "count");
        int count = countNum != null ? countNum.intValue() : 0;
        var summaryMap = JsonTree.asObject(map.get("needs_operator"));
        var summary = summaryMap != null ? NeedsOperatorSummary.fromMap(summaryMap) : null;

        return new NeedsOperatorChangedEvent(projectId, planId, epicId, kind, count, summary);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static NeedsOperatorChangedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for NeedsOperatorChangedEvent");
        }
        return fromMap(obj);
    }
}
