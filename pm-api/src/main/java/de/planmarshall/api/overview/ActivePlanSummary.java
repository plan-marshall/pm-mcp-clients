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
@Nullable Integer needsOperatorCount
) {
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
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            planId = JsonTree.string(map, "id");
        }
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in ActivePlanSummary");
        }

        var title = JsonTree.string(map, "title");
        var currentPhase = JsonTree.string(map, "current_phase");
        if (currentPhase == null) {
            currentPhase = JsonTree.string(map, "phase");
        }
        var phaseState = JsonTree.string(map, "phase_state");
        if (phaseState == null) {
            phaseState = JsonTree.string(map, "state");
        }
        var pausingPoint = JsonTree.string(map, "pausing_point");

        var phaseEnteredAtStr = JsonTree.string(map, "phase_entered_at");
        Instant phaseEnteredAt = phaseEnteredAtStr != null ? Instant.parse(phaseEnteredAtStr) : null;

        var runningSinceStr = JsonTree.string(map, "running_since");
        if (runningSinceStr == null) {
            runningSinceStr = JsonTree.string(map, "created_at");
        }
        Instant runningSince = runningSinceStr != null ? Instant.parse(runningSinceStr) : null;

        var doneNum = JsonTree.number(map, "tasks_done");
        int tasksDone = doneNum != null ? doneNum.intValue() : 0;

        var totalNum = JsonTree.number(map, "tasks_total");
        int tasksTotal = totalNum != null ? totalNum.intValue() : 0;

        var epicId = JsonTree.string(map, "epic_id");
        var epicTitle = JsonTree.string(map, "epic_title");

        var prNum = JsonTree.number(map, "pull_request_number");
        Integer pullRequestNumber = prNum != null ? prNum.intValue() : null;
        var prUrl = JsonTree.string(map, "pull_request_url");

        var offTrackSignal = JsonTree.string(map, "off_track_signal");
        if (offTrackSignal == null) {
            offTrackSignal = JsonTree.string(map, "off_track");
        }

        var needsKind = JsonTree.string(map, "needs_operator_kind");
        if (needsKind == null) {
            needsKind = JsonTree.string(map, "needs_operator");
        }
        var needsCountNum = JsonTree.number(map, "needs_operator_count");
        Integer needsOperatorCount = needsCountNum != null ? needsCountNum.intValue() : null;

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
                needsOperatorCount
        );
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
