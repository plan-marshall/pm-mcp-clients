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
 * Node within a plan's composed process tree (UC-PLN-1).
 *
 * @param nodeId          unique identifier of the node (e.g. "execute", "execute.envelope", "land")
 * @param title           display title
 * @param kind            node kind ("phase", "sub_workflow", "state", "job", "tasks", "waits", "leaf")
 * @param state           execution state ("done", "running", "waiting_for_operator", "failed", "planned", "not_taken", "not_in_plan")
 * @param runCount        total times this node has run
 * @param currentRun      current run index (when running)
 * @param durationSeconds cumulative duration in seconds
 * @param pruneReason     reason if omitted from plan (e.g. "lane_choice", "unneeded_step"), or {@code null}
 * @param subNodes        nested child nodes (for phases and sub-workflows)
 * @param leafTasks       leaf worker tasks, if kind is tasks/leaf
 * @param leafJob         leaf job details, if kind is job/leaf
 * @param waitsFor        description of operator wait (e.g. "a decision"), or {@code null}
 * @param links           hypermedia action links
 */
public record ProcessNode(
        String nodeId,
        String title,
        String kind,
        String state,
        int runCount,
        int currentRun,
        long durationSeconds,
        @Nullable String pruneReason,
        List<ProcessNode> subNodes,
        List<LeafTask> leafTasks,
        @Nullable LeafJob leafJob,
        @Nullable String waitsFor,
        Map<String, String> links
) {
    public ProcessNode {
        Objects.requireNonNull(nodeId, "nodeId");
        title = title != null ? title : nodeId;
        kind = kind != null ? kind : "state";
        state = state != null ? state : "planned";
        subNodes = subNodes != null ? List.copyOf(subNodes) : List.of();
        leafTasks = leafTasks != null ? List.copyOf(leafTasks) : List.of();
        links = links != null ? Map.copyOf(links) : Map.of();
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("node_id", nodeId);
        map.put("title", title);
        map.put("kind", kind);
        map.put("state", state);
        map.put("run_count", runCount);
        map.put("current_run", currentRun);
        map.put("duration_seconds", durationSeconds);
        if (pruneReason != null) {
            map.put("prune_reason", pruneReason);
        }
        if (!subNodes.isEmpty()) {
            var subList = new ArrayList<Map<String, Object>>();
            for (var sub : subNodes) {
                subList.add(sub.toMap());
            }
            map.put("sub_nodes", subList);
        }
        if (!leafTasks.isEmpty()) {
            var taskList = new ArrayList<Map<String, Object>>();
            for (var t : leafTasks) {
                taskList.add(t.toMap());
            }
            map.put("leaf_tasks", taskList);
        }
        if (leafJob != null) {
            map.put("leaf_job", leafJob.toMap());
        }
        if (waitsFor != null) {
            map.put("waits_for", waitsFor);
        }
        if (!links.isEmpty()) {
            map.put("links", links);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static ProcessNode fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var nodeId = JsonTree.string(map, "node_id");
        if (nodeId == null) {
            nodeId = JsonTree.string(map, "id");
        }
        if (nodeId == null) {
            throw new JsonFormatException("Missing node_id in ProcessNode");
        }
        var title = JsonTree.string(map, "title");
        var kind = JsonTree.string(map, "kind");
        var state = JsonTree.string(map, "state");

        var runCountNum = JsonTree.number(map, "run_count");
        int runCount = runCountNum != null ? runCountNum.intValue() : 1;

        var curRunNum = JsonTree.number(map, "current_run");
        int currentRun = curRunNum != null ? curRunNum.intValue() : 1;

        var durNum = JsonTree.number(map, "duration_seconds");
        long durationSeconds = durNum != null ? durNum.longValue() : 0L;

        var pruneReason = JsonTree.string(map, "prune_reason");

        var subNodes = new ArrayList<ProcessNode>();
        if (map.get("sub_nodes") instanceof List<?> list) {
            for (var item : list) {
                var nodeMap = JsonTree.asObject(item);
                if (nodeMap != null) {
                    subNodes.add(ProcessNode.fromMap(nodeMap));
                }
            }
        }

        var leafTasks = new ArrayList<LeafTask>();
        if (map.get("leaf_tasks") instanceof List<?> list) {
            for (var item : list) {
                var taskMap = JsonTree.asObject(item);
                if (taskMap != null) {
                    leafTasks.add(LeafTask.fromMap(taskMap));
                }
            }
        }

        LeafJob leafJob = null;
        var leafJobObj = JsonTree.asObject(map.get("leaf_job"));
        if (leafJobObj != null) {
            leafJob = LeafJob.fromMap(leafJobObj);
        }

        var waitsFor = JsonTree.string(map, "waits_for");

        var links = new LinkedHashMap<String, String>();
        if (map.get("links") instanceof Map<?, ?> linkMap) {
            for (var entry : linkMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    links.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        return new ProcessNode(
                nodeId,
                title != null ? title : nodeId,
                kind != null ? kind : "state",
                state != null ? state : "planned",
                runCount,
                currentRun,
                durationSeconds,
                pruneReason,
                subNodes,
                leafTasks,
                leafJob,
                waitsFor,
                links
        );
    }

    public static ProcessNode fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ProcessNode");
        }
        return fromMap(obj);
    }
}
