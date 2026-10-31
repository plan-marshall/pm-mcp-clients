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
 * Summary of an active epic within a project in the Running Work overview (UC-PLN-4).
 *
 * @param epicId             unique identifier of the epic
 * @param title              title of the epic
 * @param runningSince       timestamp when epic started running
 * @param runningPlansCount  number of actively running plans
 * @param stagedPlansCount   number of staged/scheduled plans
 * @param plansDone          number of plans shipped/completed
 * @param plansTotal         total number of plans in the epic
 * @param needsOperatorKind  first reason why epic or its plans need operator, or {@code null}
 * @param needsOperatorCount number of items needing operator attention, or {@code null}
 * @param plans              nested active plans belonging to this epic
 */
public record ActiveEpicSummary(
        String epicId,
        String title,
        @Nullable Instant runningSince,
        int runningPlansCount,
        int stagedPlansCount,
        int plansDone,
        int plansTotal,
        @Nullable String needsOperatorKind,
        @Nullable Integer needsOperatorCount,
        List<ActivePlanSummary> plans
) {
    public ActiveEpicSummary {
        Objects.requireNonNull(epicId, "epicId");
        title = title != null ? title : epicId;
        plans = plans != null ? List.copyOf(plans) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("epic_id", epicId);
        map.put("title", title);
        if (runningSince != null) {
            map.put("running_since", runningSince.toString());
        }
        map.put("running_plans_count", runningPlansCount);
        map.put("staged_plans_count", stagedPlansCount);
        map.put("plans_done", plansDone);
        map.put("plans_total", plansTotal);
        if (needsOperatorKind != null) {
            map.put("needs_operator_kind", needsOperatorKind);
        }
        if (needsOperatorCount != null) {
            map.put("needs_operator_count", needsOperatorCount);
        }
        map.put("plans", plans.stream().map(ActivePlanSummary::toMap).toList());
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
    public static ActiveEpicSummary fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var epicId = JsonTree.string(map, "epic_id");
        if (epicId == null) {
            epicId = JsonTree.string(map, "id");
        }
        if (epicId == null) {
            throw new JsonFormatException("Missing epic_id in ActiveEpicSummary");
        }

        var title = JsonTree.string(map, "title");
        var runningSinceStr = JsonTree.string(map, "running_since");
        if (runningSinceStr == null) {
            runningSinceStr = JsonTree.string(map, "created_at");
        }
        Instant runningSince = runningSinceStr != null ? Instant.parse(runningSinceStr) : null;

        var runningPlansNum = JsonTree.number(map, "running_plans_count");
        int runningPlans = runningPlansNum != null ? runningPlansNum.intValue() : 0;

        var stagedPlansNum = JsonTree.number(map, "staged_plans_count");
        int stagedPlans = stagedPlansNum != null ? stagedPlansNum.intValue() : 0;

        var plansDoneNum = JsonTree.number(map, "plans_done");
        int plansDone = plansDoneNum != null ? plansDoneNum.intValue() : 0;

        var plansTotalNum = JsonTree.number(map, "plans_total");
        int plansTotal = plansTotalNum != null ? plansTotalNum.intValue() : 0;

        var needsKind = JsonTree.string(map, "needs_operator_kind");
        if (needsKind == null) {
            needsKind = JsonTree.string(map, "needs_operator");
        }
        var needsCountNum = JsonTree.number(map, "needs_operator_count");
        Integer needsOperatorCount = needsCountNum != null ? needsCountNum.intValue() : null;

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

        return new ActiveEpicSummary(
                epicId,
                title != null ? title : epicId,
                runningSince,
                runningPlans,
                stagedPlans,
                plansDone,
                plansTotal,
                needsKind,
                needsOperatorCount,
                plans
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed summary
     * @throws IOException on format failure
     */
    public static ActiveEpicSummary fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ActiveEpicSummary");
        }
        return fromMap(obj);
    }
}
