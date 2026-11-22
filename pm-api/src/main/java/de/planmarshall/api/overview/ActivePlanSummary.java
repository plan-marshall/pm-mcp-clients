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

import de.planmarshall.api.chat.QueuedRevision;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Summary of an active plan within a project in the Running Work overview (UC-PLN-4).
 *
 * @param planId             unique identifier of the plan
 * @param title              title of the plan
 * @param currentPhase       current workflow phase (e.g. "execute", "refine", "outline")
 * @param phaseState         current activity state ("running", "pausing", "paused", "waiting_for_you", "held")
 * @param pausingPoint       point when pausing ("task", "phase"), or {@code null}
 * @param phaseEnteredAt     timestamp when current phase was entered
 * @param runningSince       timestamp when plan started running
 * @param tasksDone          number of tasks completed
 * @param tasksTotal         total number of tasks
 * @param epicId             parent epic ID, or {@code null}
 * @param epicTitle          parent epic title, or {@code null}
 * @param pullRequestNumber  pull request number, or {@code null}
 * @param pullRequestUrl     pull request web URL, or {@code null}
 * @param offTrackSignal     signal string if worker is judged off-track (e.g. "repeating"), or {@code null}
 * @param needsOperatorKind  first reason why plan needs operator, or {@code null}
 * @param needsOperatorCount number of items needing operator attention, or {@code null}
 * @param change             size of the plan's change at its last capture, or {@code null}
 * @param queuedRevision     the revision accepted in a chat that waits for the plan's next task boundary
 *                           ({@code queued_revision}), or {@code null} when none is queued
 */
public record ActivePlanSummary(
        String planId,
        String title,
        String currentPhase,
        String phaseState,
        @Nullable String pausingPoint,
        @Nullable Instant phaseEnteredAt,
        @Nullable Instant runningSince,
        int tasksDone,
        int tasksTotal,
        @Nullable String epicId,
        @Nullable String epicTitle,
        @Nullable Integer pullRequestNumber,
        @Nullable String pullRequestUrl,
        @Nullable String offTrackSignal,
        @Nullable String needsOperatorKind,
        @Nullable Integer needsOperatorCount,
        @Nullable ChangeSize change,
        @Nullable QueuedRevision queuedRevision
) {
    private static final String QUEUED_REVISION = "queued_revision";

    public ActivePlanSummary {
        Objects.requireNonNull(planId, "planId");
        title = title != null ? title : planId;
        currentPhase = currentPhase != null ? currentPhase : "execute";
        phaseState = phaseState != null ? phaseState : "running";
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("title", title);
        map.put("current_phase", currentPhase);
        map.put("phase_state", phaseState);
        if (pausingPoint != null) {
            map.put("pausing_point", pausingPoint);
        }
        if (phaseEnteredAt != null) {
            map.put("phase_entered_at", phaseEnteredAt.toString());
        }
        if (runningSince != null) {
            map.put("running_since", runningSince.toString());
        }
        map.put("tasks_done", tasksDone);
        map.put("tasks_total", tasksTotal);
        if (epicId != null) {
            map.put("epic_id", epicId);
        }
        if (epicTitle != null) {
            map.put("epic_title", epicTitle);
        }
        if (pullRequestNumber != null) {
            map.put("pull_request_number", pullRequestNumber);
        }
        if (pullRequestUrl != null) {
            map.put("pull_request_url", pullRequestUrl);
        }
        if (offTrackSignal != null) {
            map.put("off_track_signal", offTrackSignal);
        }
        if (needsOperatorKind != null) {
            map.put("needs_operator_kind", needsOperatorKind);
        }
        if (needsOperatorCount != null) {
            map.put("needs_operator_count", needsOperatorCount);
        }
        if (change != null) {
            map.put("change", change.toMap());
        }
        if (queuedRevision != null) {
            map.put(QUEUED_REVISION, queuedRevision.toMap());
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
     * @return parsed summary
     * @throws IOException on format failure
     */
    public static ActivePlanSummary fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = firstString(map, "plan_id", "id");
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in ActivePlanSummary");
        }

        var title = JsonTree.string(map, "title");
        var currentPhase = firstString(map, "current_phase", "phase");
        var phaseState = firstString(map, "phase_state", "state");
        var pausingPoint = JsonTree.string(map, "pausing_point");

        Instant phaseEnteredAt = instantOrNull(JsonTree.string(map, "phase_entered_at"));
        Instant runningSince = instantOrNull(firstString(map, "running_since", "created_at"));

        int tasksDone = Objects.requireNonNullElse(optInt(map, "tasks_done"), 0);
        int tasksTotal = Objects.requireNonNullElse(optInt(map, "tasks_total"), 0);

        var epicId = JsonTree.string(map, "epic_id");
        var epicTitle = JsonTree.string(map, "epic_title");

        Integer pullRequestNumber = optInt(map, "pull_request_number");
        var prUrl = JsonTree.string(map, "pull_request_url");

        var offTrackSignal = firstString(map, "off_track_signal", "off_track");

        var needsKind = firstString(map, "needs_operator_kind", "needs_operator");
        Integer needsOperatorCount = optInt(map, "needs_operator_count");

        var change = ChangeSize.fromMap(JsonTree.asObject(map.get("change")));
        var queuedRevisionMap = JsonTree.objectOrNull(map, QUEUED_REVISION, "ActivePlanSummary");
        var queuedRevision = queuedRevisionMap != null ? QueuedRevision.fromMap(queuedRevisionMap) : null;

        return new ActivePlanSummary(
                planId,
                title != null ? title : planId,
                currentPhase != null ? currentPhase : "execute",
                phaseState != null ? phaseState : "running",
                pausingPoint,
                phaseEnteredAt,
                runningSince,
                tasksDone,
                tasksTotal,
                epicId,
                epicTitle,
                pullRequestNumber,
                prUrl,
                offTrackSignal,
                needsKind,
                needsOperatorCount,
                change,
                queuedRevision
        );
    }

    /** The member under the current name, else the one under the legacy name. */
    static @Nullable String firstString(Map<String, Object> map, String current, String legacy) {
        var value = JsonTree.string(map, current);
        return value != null ? value : JsonTree.string(map, legacy);
    }

    private static @Nullable Integer optInt(Map<String, Object> map, String key) {
        var number = JsonTree.number(map, key);
        return number != null ? number.intValue() : null;
    }

    private static @Nullable Instant instantOrNull(@Nullable String text) {
        return text != null ? Instant.parse(text) : null;
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed summary
     * @throws IOException on format failure
     */
    public static ActivePlanSummary fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ActivePlanSummary");
        }
        return fromMap(obj);
    }

    /**
     * Parses a list of active plan summaries from JSON array string.
     *
     * @param json JSON array string
     * @return list of active plan summaries
     * @throws IOException on parse failure
     */
    public static List<ActivePlanSummary> parseList(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (!(parsed instanceof List<?> list)) {
            throw new JsonFormatException("Expected JSON array for ActivePlanSummary list");
        }
        var result = new ArrayList<ActivePlanSummary>(list.size());
        for (var item : list) {
            var map = JsonTree.asObject(item);
            if (map != null) {
                result.add(fromMap(map));
            }
        }
        return result;
    }
}
