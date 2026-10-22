/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The actions of a plan or an epic in its current state ({@code plan.actions}, {@code epic.actions}),
 * UC-CTL-1 and UC-CTL-2.
 *
 * @param subjectId    the plan or epic identifier
 * @param subjectKind  {@code "plan"} or {@code "epic"}
 * @param state        the subject's state, e.g. {@code running}, {@code paused}, {@code held}
 * @param pausingPoint the point a pausing plan holds at ({@code now}, {@code task}, {@code phase}),
 *                     or {@code null}
 * @param holdKind     the kind of the hold when the subject is held, or {@code null}
 * @param epicId       the epic that started the plan and is told, or {@code null}
 * @param actions      the actions of the state, each with its availability
 */
public record WorkActions(
        String subjectId,
        String subjectKind,
        String state,
        @Nullable String pausingPoint,
        @Nullable String holdKind,
        @Nullable String epicId,
        List<ActionAvailability> actions
) {
    public WorkActions {
        Objects.requireNonNull(subjectId, "subjectId");
        subjectKind = subjectKind == null || subjectKind.isBlank() ? "plan" : subjectKind;
        state = state == null || state.isBlank() ? "running" : state;
        actions = actions != null ? List.copyOf(actions) : List.of();
    }

    /**
     * @param action the action value
     * @return the availability of the action, or {@code null} if the state does not offer it
     */
    public @Nullable ActionAvailability action(String action) {
        for (var availability : actions) {
            if (availability.action().equals(action)) {
                return availability;
            }
        }
        return null;
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("subject_id", subjectId);
        map.put("subject_kind", subjectKind);
        map.put("state", state);
        if (pausingPoint != null) {
            map.put("pausing_point", pausingPoint);
        }
        if (holdKind != null) {
            map.put("hold_kind", holdKind);
        }
        if (epicId != null) {
            map.put("epic_id", epicId);
        }
        map.put("actions", actions.stream().map(ActionAvailability::toMap).toList());
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
     * @return parsed actions
     * @throws IOException on format failure
     */
    public static WorkActions fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var subjectId = JsonTree.string(map, "subject_id");
        if (subjectId == null) {
            subjectId = JsonTree.string(map, "plan_id");
        }
        if (subjectId == null) {
            subjectId = JsonTree.string(map, "epic_id");
        }
        if (subjectId == null) {
            throw new JsonFormatException("Missing subject_id in WorkActions");
        }
        var actions = new ArrayList<ActionAvailability>();
        var list = JsonTree.asArray(map.get("actions"));
        if (list != null) {
            for (var element : list) {
                var entry = JsonTree.asObject(element);
                if (entry != null) {
                    actions.add(ActionAvailability.fromMap(entry));
                }
            }
        }
        return new WorkActions(
                subjectId,
                JsonTree.string(map, "subject_kind"),
                JsonTree.string(map, "state"),
                JsonTree.string(map, "pausing_point"),
                JsonTree.string(map, "hold_kind"),
                JsonTree.string(map, "epic_id"),
                actions
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed actions
     * @throws IOException on format failure
     */
    public static WorkActions fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for WorkActions");
        }
        return fromMap(obj);
    }
}
