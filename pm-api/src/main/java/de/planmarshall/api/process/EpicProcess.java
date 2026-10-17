/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.process;

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
 * Representation of an epic's process (UC-PLN-5).
 *
 * @param epicId            unique identifier of the epic
 * @param project           display name or ID of parent project
 * @param title             display title
 * @param state             overall epic state
 * @param runningPlans      plans actively executing
 * @param stagedPlans       plans queued/staged for execution
 * @param alongsideLandings active alongside landing descriptions
 * @param tokensSoFar       cumulative token metrics across epic
 * @param reviewReady       whether the completion review is ready
 */
public record EpicProcess(
        String epicId,
        String project,
        String title,
        String state,
        List<EpicProcessPlan> runningPlans,
        List<EpicProcessPlan> stagedPlans,
        List<String> alongsideLandings,
        @Nullable ProcessTokens tokensSoFar,
        boolean reviewReady
) {
    public EpicProcess {
        Objects.requireNonNull(epicId, "epicId");
        project = project != null ? project : "project";
        title = title != null ? title : epicId;
        state = state != null ? state : "running";
        runningPlans = runningPlans != null ? List.copyOf(runningPlans) : List.of();
        stagedPlans = stagedPlans != null ? List.copyOf(stagedPlans) : List.of();
        alongsideLandings = alongsideLandings != null ? List.copyOf(alongsideLandings) : List.of();
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("epic_id", epicId);
        map.put("project", project);
        map.put("title", title);
        map.put("state", state);

        var runList = new ArrayList<Map<String, Object>>();
        for (var p : runningPlans) {
            runList.add(p.toMap());
        }
        map.put("running_plans", runList);

        var stagedList = new ArrayList<Map<String, Object>>();
        for (var p : stagedPlans) {
            stagedList.add(p.toMap());
        }
        map.put("staged_plans", stagedList);

        map.put("alongside_landings", alongsideLandings);
        if (tokensSoFar != null) {
            map.put("tokens_so_far", tokensSoFar.toMap());
        }
        if (reviewReady) {
            map.put("review_ready", true);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static EpicProcess fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var epicId = JsonTree.string(map, "epic_id");
        if (epicId == null) {
            epicId = JsonTree.string(map, "id");
        }
        if (epicId == null) {
            throw new JsonFormatException("Missing epic_id in EpicProcess");
        }

        var project = JsonTree.string(map, "project");
        var title = JsonTree.string(map, "title");
        var state = JsonTree.string(map, "state");

        var runningPlans = new ArrayList<EpicProcessPlan>();
        if (map.get("running_plans") instanceof List<?> list) {
            for (var item : list) {
                var pMap = JsonTree.asObject(item);
                if (pMap != null) {
                    runningPlans.add(EpicProcessPlan.fromMap(pMap));
                }
            }
        }

        var stagedPlans = new ArrayList<EpicProcessPlan>();
        if (map.get("staged_plans") instanceof List<?> list) {
            for (var item : list) {
                var pMap = JsonTree.asObject(item);
                if (pMap != null) {
                    stagedPlans.add(EpicProcessPlan.fromMap(pMap));
                }
            }
        }

        var alongsideLandings = new ArrayList<String>();
        if (map.get("alongside_landings") instanceof List<?> list) {
            for (var item : list) {
                if (item != null) {
                    alongsideLandings.add(item.toString());
                }
            }
        }

        ProcessTokens tokensSoFar = null;
        var tokensObj = JsonTree.asObject(map.get("tokens_so_far"));
        if (tokensObj != null) {
            tokensSoFar = ProcessTokens.fromMap(tokensObj);
        }

        var reviewReady = Boolean.TRUE.equals(map.get("review_ready"));

        return new EpicProcess(
                epicId,
                project != null ? project : "project",
                title != null ? title : epicId,
                state != null ? state : "running",
                runningPlans,
                stagedPlans,
                alongsideLandings,
                tokensSoFar,
                reviewReady
        );
    }

    public static EpicProcess fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EpicProcess");
        }
        return fromMap(obj);
    }
}
