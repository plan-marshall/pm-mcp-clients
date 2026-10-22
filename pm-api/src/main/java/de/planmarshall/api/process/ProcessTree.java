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
 * Full process tree representation of a plan (UC-PLN-1).
 *
 * @param planId              unique plan identifier
 * @param title               plan title
 * @param project             parent project reference
 * @param epic                parent epic reference, or {@code null}
 * @param currentPhase        current phase name (e.g. "execute", "refine")
 * @param phaseState          current state (e.g. "running", "waiting_for_you")
 * @param activity            active summary string, or {@code null}
 * @param needsOperator       whether operator attention is required
 * @param instanceVersion     workflow composition instance version
 * @param tokensSoFar         tokens consumed so far
 * @param nodes               top-level nodes (phases and pruned phase groups)
 * @param currentPositionPath path of node IDs from root to the active execution cursor
 * @param pullRequestNumber   associated pull request number, or {@code null}
 * @param pullRequestUrl      associated pull request URL, or {@code null}
 * @param durationSeconds     total running duration in seconds
 */
public record ProcessTree(
        String planId,
        String title,
        ProcessProjectRef project,
        @Nullable ProcessEpicRef epic,
        String currentPhase,
        String phaseState,
        @Nullable String activity,
        boolean needsOperator,
        int instanceVersion,
        ProcessTokens tokensSoFar,
        List<ProcessNode> nodes,
        List<String> currentPositionPath,
        @Nullable Integer pullRequestNumber,
        @Nullable String pullRequestUrl,
        long durationSeconds
) {
    public ProcessTree {
        Objects.requireNonNull(planId, "planId");
        title = title != null ? title : planId;
        Objects.requireNonNull(project, "project");
        currentPhase = currentPhase != null ? currentPhase : "execute";
        phaseState = phaseState != null ? phaseState : "running";
        tokensSoFar = tokensSoFar != null ? tokensSoFar : new ProcessTokens(0L, 0L, 0L, false, 0, 0, true, false);
        nodes = nodes != null ? List.copyOf(nodes) : List.of();
        currentPositionPath = currentPositionPath != null ? List.copyOf(currentPositionPath) : List.of();
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("title", title);
        map.put("project", project.toMap());
        if (epic != null) {
            map.put("epic", epic.toMap());
        }
        map.put("current_phase", currentPhase);
        map.put("phase_state", phaseState);
        if (activity != null) {
            map.put("activity", activity);
        }
        if (needsOperator) {
            map.put("needs_operator", true);
        }
        map.put("instance_version", instanceVersion);
        map.put("tokens_so_far", tokensSoFar.toMap());

        var nodesList = new ArrayList<Map<String, Object>>();
        for (var n : nodes) {
            nodesList.add(n.toMap());
        }
        map.put("nodes", nodesList);
        map.put("current_position_path", currentPositionPath);

        if (pullRequestNumber != null) {
            map.put("pull_request_number", pullRequestNumber);
        }
        if (pullRequestUrl != null) {
            map.put("pull_request_url", pullRequestUrl);
        }
        map.put("duration_seconds", durationSeconds);
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static ProcessTree fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            planId = JsonTree.string(map, "id");
        }
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in ProcessTree");
        }

        var title = JsonTree.string(map, "title");

        ProcessProjectRef project;
        var projObj = JsonTree.asObject(map.get("project"));
        if (projObj != null) {
            project = ProcessProjectRef.fromMap(projObj);
        } else {
            var projId = JsonTree.string(map, "project_id");
            var projName = JsonTree.string(map, "project_name");
            project = new ProcessProjectRef(projId != null ? projId : "project", projName != null ? projName : "Project", null);
        }

        ProcessEpicRef epic = null;
        var epicObj = JsonTree.asObject(map.get("epic"));
        if (epicObj != null) {
            epic = ProcessEpicRef.fromMap(epicObj);
        } else {
            var epicId = JsonTree.string(map, "epic_id");
            if (epicId != null) {
                var epicTitle = JsonTree.string(map, "epic_title");
                epic = new ProcessEpicRef(epicId, epicTitle != null ? epicTitle : epicId, null);
            }
        }

        var currentPhase = JsonTree.string(map, "current_phase");
        var phaseState = JsonTree.string(map, "phase_state");
        var activity = JsonTree.string(map, "activity");
        var needsOperator = Boolean.TRUE.equals(map.get("needs_operator"));

        var verNum = JsonTree.number(map, "instance_version");
        int instanceVersion = verNum != null ? verNum.intValue() : 1;

        ProcessTokens tokensSoFar = null;
        var tokensObj = JsonTree.asObject(map.get("tokens_so_far"));
        if (tokensObj != null) {
            tokensSoFar = ProcessTokens.fromMap(tokensObj);
        } else {
            tokensSoFar = new ProcessTokens(0L, 0L, 0L, false, 0, 0, true, false);
        }

        var nodes = new ArrayList<ProcessNode>();
        if (map.get("nodes") instanceof List<?> list) {
            for (var item : list) {
                var nodeMap = JsonTree.asObject(item);
                if (nodeMap != null) {
                    nodes.add(ProcessNode.fromMap(nodeMap));
                }
            }
        }

        var currentPositionPath = new ArrayList<String>();
        if (map.get("current_position_path") instanceof List<?> list) {
            for (var item : list) {
                if (item != null) {
                    currentPositionPath.add(item.toString());
                }
            }
        }

        var prNum = JsonTree.number(map, "pull_request_number");
        Integer pullRequestNumber = prNum != null ? prNum.intValue() : null;
        var prUrl = JsonTree.string(map, "pull_request_url");

        var durNum = JsonTree.number(map, "duration_seconds");
        long durationSeconds = durNum != null ? durNum.longValue() : 0L;

        return new ProcessTree(
                planId,
                title != null ? title : planId,
                project,
                epic,
                currentPhase != null ? currentPhase : "execute",
                phaseState != null ? phaseState : "running",
                activity,
                needsOperator,
                instanceVersion,
                tokensSoFar,
                nodes,
                currentPositionPath,
                pullRequestNumber,
                prUrl,
                durationSeconds
        );
    }

    public static ProcessTree fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ProcessTree");
        }
        return fromMap(obj);
    }
}
