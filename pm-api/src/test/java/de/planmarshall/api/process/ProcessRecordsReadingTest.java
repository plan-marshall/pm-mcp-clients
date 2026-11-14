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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * How the records of the process tree below the plan's head read a sparse member set: the defaults of an
 * absent member, the identifier fallback, and the refusal of an unusable shape.
 */
@DisplayName("Process records read leniently")
@EnableGeneratorController
class ProcessRecordsReadingTest {

    @Nested
    @DisplayName("LeafTask")
    class LeafTaskReading {

        @Test
        @DisplayName("should fill every absent member of a task that carries only its id")
        void shouldDefaultAbsentMembers() throws Exception {
            var taskId = Generators.nonBlankStrings().next();

            var task = LeafTask.fromMap(Map.of("id", taskId));

            assertAll(
                    () -> assertEquals(taskId, task.taskId()),
                    () -> assertEquals("implement", task.role()),
                    () -> assertEquals("agent", task.harness()),
                    () -> assertEquals("default", task.model()),
                    () -> assertEquals(1, task.attempt()),
                    () -> assertEquals("planned", task.status()),
                    () -> assertNull(task.outcome()),
                    () -> assertEquals(0L, task.durationSeconds()),
                    () -> assertEquals(0L, task.tokensSoFar()),
                    () -> assertNull(task.activityLink()),
                    () -> assertNull(task.offTrack()));
        }

        @Test
        @DisplayName("should write no absent optional member and read back the present ones")
        void shouldRoundTripOptionalMembers() throws Exception {
            var taskId = Generators.nonBlankStrings().next();
            var outcome = Generators.nonBlankStrings().next();
            var link = "/api/v1/tasks/" + Generators.nonBlankStrings().next();
            var bare = new LeafTask(taskId, null, null, null, 1, null, null, 0L, 0L, null, null);
            var full = new LeafTask(taskId, "review", "codex", "gpt", 2, "done", outcome, 5L, 7L, link, null);

            var parsed = LeafTask.fromJson(full.toJson());

            assertAll(
                    () -> assertFalse(bare.toMap().containsKey("outcome")),
                    () -> assertFalse(bare.toMap().containsKey("activity_link")),
                    () -> assertFalse(bare.toMap().containsKey("off_track")),
                    () -> assertEquals(full, parsed));
        }

        @Test
        @DisplayName("should refuse a task without an id and JSON that is no object")
        void shouldRefuseUnusableShapes() {
            var role = Generators.nonBlankStrings().next();
            var map = Map.<String, Object>of("role", role);
            var json = JsonTree.write(List.of(role));

            assertAll(
                    () -> assertThrows(JsonFormatException.class, () -> LeafTask.fromMap(map)),
                    () -> assertThrows(JsonFormatException.class, () -> LeafTask.fromJson(json)));
        }
    }

    @Nested
    @DisplayName("ProcessNode")
    class ProcessNodeReading {

        @Test
        @DisplayName("should fill every absent member of a node that carries only its id")
        void shouldDefaultAbsentMembers() throws Exception {
            var nodeId = Generators.nonBlankStrings().next();

            var node = ProcessNode.fromMap(Map.of("id", nodeId));

            assertAll(
                    () -> assertEquals(nodeId, node.nodeId()),
                    () -> assertEquals(nodeId, node.title()),
                    () -> assertEquals("state", node.kind()),
                    () -> assertEquals("planned", node.state()),
                    () -> assertEquals(1, node.runCount()),
                    () -> assertEquals(1, node.currentRun()),
                    () -> assertEquals(0L, node.durationSeconds()),
                    () -> assertTrue(node.subNodes().isEmpty()),
                    () -> assertTrue(node.leafTasks().isEmpty()),
                    () -> assertNull(node.leafJob()),
                    () -> assertTrue(node.links().isEmpty()));
        }

        @Test
        @DisplayName("should skip a child that is no object and a link without name or target")
        void shouldSkipUnusableEntries() throws Exception {
            var nodeId = Generators.nonBlankStrings().next();
            var target = "/api/v1/nodes/" + Generators.nonBlankStrings().next();
            var links = new HashMap<String, Object>();
            links.put("self", target);
            links.put("abort", null);
            links.put(null, target);

            var node = ProcessNode.fromMap(Map.of("node_id", nodeId, "sub_nodes", List.of(nodeId),
                    "leaf_tasks", List.of(nodeId), "links", links));

            assertAll(
                    () -> assertTrue(node.subNodes().isEmpty()),
                    () -> assertTrue(node.leafTasks().isEmpty()),
                    () -> assertEquals(Map.of("self", target), node.links()));
        }

        @Test
        @DisplayName("should replace absent lists and links of a constructed node and write none of them")
        void shouldReplaceAbsentCollections() {
            var nodeId = Generators.nonBlankStrings().next();

            var node = new ProcessNode(nodeId, null, null, null, 0, 0, 0L, null, null, null, null, null, null);

            assertAll(
                    () -> assertTrue(node.subNodes().isEmpty()),
                    () -> assertTrue(node.leafTasks().isEmpty()),
                    () -> assertTrue(node.links().isEmpty()),
                    () -> assertFalse(node.toMap().containsKey("sub_nodes")),
                    () -> assertFalse(node.toMap().containsKey("links")));
        }

        @Test
        @DisplayName("should refuse a node without an id and JSON that is no object")
        void shouldRefuseUnusableShapes() {
            var title = Generators.nonBlankStrings().next();
            var map = Map.<String, Object>of("title", title);
            var json = JsonTree.write(List.of(title));

            assertAll(
                    () -> assertThrows(JsonFormatException.class, () -> ProcessNode.fromMap(map)),
                    () -> assertThrows(JsonFormatException.class, () -> ProcessNode.fromJson(json)));
        }
    }

    @Nested
    @DisplayName("TaskActivity")
    class TaskActivityReading {

        @Test
        @DisplayName("should fill every absent member of an activity that carries only its task id")
        void shouldDefaultAbsentMembers() throws Exception {
            var taskId = Generators.nonBlankStrings().next();

            var activity = TaskActivity.fromMap(Map.of("id", taskId, "entries", List.of(taskId)));

            assertAll(
                    () -> assertEquals(taskId, activity.taskId()),
                    () -> assertEquals("implement", activity.role()),
                    () -> assertEquals("agent", activity.harness()),
                    () -> assertEquals("default", activity.model()),
                    () -> assertEquals(1, activity.attempt()),
                    () -> assertEquals(0L, activity.durationSeconds()),
                    () -> assertEquals(0L, activity.tokensSoFar()),
                    () -> assertTrue(activity.entries().isEmpty()),
                    () -> assertNull(activity.offTrack()),
                    () -> assertFalse(activity.serverOnly()),
                    () -> assertEquals(0, activity.gapCount()));
        }

        @Test
        @DisplayName("should write server_only only when set and read it back, without an absent off-track")
        void shouldRoundTripServerOnly() throws Exception {
            var taskId = Generators.nonBlankStrings().next();
            var serverOnly = new TaskActivity(taskId, null, null, null, 1, 0L, 0L, null, null, true, 0);

            var parsed = TaskActivity.fromJson(serverOnly.toJson());

            assertAll(
                    () -> assertTrue(parsed.serverOnly()),
                    () -> assertTrue(serverOnly.entries().isEmpty()),
                    () -> assertFalse(serverOnly.toMap().containsKey("off_track")),
                    () -> assertFalse(serverOnly.toMap().containsKey("gap_count")));
        }

        @Test
        @DisplayName("should refuse an activity without a task id and JSON that is no object")
        void shouldRefuseUnusableShapes() {
            var role = Generators.nonBlankStrings().next();
            var map = Map.<String, Object>of("role", role);
            var json = JsonTree.write(List.of(role));

            assertAll(
                    () -> assertThrows(JsonFormatException.class, () -> TaskActivity.fromMap(map)),
                    () -> assertThrows(JsonFormatException.class, () -> TaskActivity.fromJson(json)));
        }
    }

    @Nested
    @DisplayName("EpicProcessPlan")
    class EpicProcessPlanReading {

        @Test
        @DisplayName("should fill the absent members of a running and of a staged plan")
        void shouldDefaultAbsentMembers() throws Exception {
            var planId = Generators.nonBlankStrings().next();

            var running = EpicProcessPlan.fromMap(Map.of("id", planId, "running", true));
            var staged = EpicProcessPlan.fromMap(Map.of("plan_id", planId));

            assertAll(
                    () -> assertEquals(new EpicProcessPlan(planId, planId, "init", "running", 0, 0, true), running),
                    () -> assertEquals(new EpicProcessPlan(planId, planId, "init", "staged", 0, 0, false), staged));
        }

        @Test
        @DisplayName("should keep a stated phase state whatever the running flag says, and refuse a plan without id")
        void shouldKeepStatedPhaseStateAndRefuseMissingId() throws Exception {
            var planId = Generators.nonBlankStrings().next();
            var done = Generators.integers(0, 50).next();

            var plan = EpicProcessPlan.fromMap(Map.of("plan_id", planId, "phase_state", "done", "running", true,
                    "tasks_done", done, "tasks_total", done));
            var map = Map.<String, Object>of("title", planId);

            assertAll(
                    () -> assertEquals("done", plan.phaseState()),
                    () -> assertEquals(done, plan.tasksDone()),
                    () -> assertEquals(done, plan.tasksTotal()),
                    () -> assertThrows(JsonFormatException.class, () -> EpicProcessPlan.fromMap(map)));
        }
    }

    @Nested
    @DisplayName("EpicProcess")
    class EpicProcessReading {

        @Test
        @DisplayName("should fill every absent member of an epic that carries only its id")
        void shouldDefaultAbsentMembers() throws Exception {
            var epicId = Generators.nonBlankStrings().next();

            var epic = EpicProcess.fromMap(Map.of("id", epicId));

            assertAll(
                    () -> assertEquals(epicId, epic.epicId()),
                    () -> assertEquals("project", epic.project()),
                    () -> assertEquals(epicId, epic.title()),
                    () -> assertEquals("running", epic.state()),
                    () -> assertTrue(epic.runningPlans().isEmpty()),
                    () -> assertTrue(epic.stagedPlans().isEmpty()),
                    () -> assertTrue(epic.alongsideLandings().isEmpty()),
                    () -> assertNull(epic.tokensSoFar()),
                    () -> assertFalse(epic.reviewReady()));
        }

        @Test
        @DisplayName("should skip a plan that is no object and a null landing")
        void shouldSkipUnusableEntries() throws Exception {
            var epicId = Generators.nonBlankStrings().next();
            var landing = Generators.nonBlankStrings().next();
            var landings = new ArrayList<Object>();
            landings.add(null);
            landings.add(landing);

            var epic = EpicProcess.fromMap(Map.of("epic_id", epicId, "running_plans", List.of(landing),
                    "staged_plans", List.of(landing), "alongside_landings", landings));

            assertAll(
                    () -> assertTrue(epic.runningPlans().isEmpty()),
                    () -> assertTrue(epic.stagedPlans().isEmpty()),
                    () -> assertEquals(List.of(landing), epic.alongsideLandings()));
        }

        @Test
        @DisplayName("should write neither tokens nor review_ready for an epic without them")
        void shouldWriteNoAbsentMember() {
            var epicId = Generators.nonBlankStrings().next();

            var epic = new EpicProcess(epicId, null, null, null, null, null, null, null, false);

            assertAll(
                    () -> assertFalse(epic.toMap().containsKey("tokens_so_far")),
                    () -> assertFalse(epic.toMap().containsKey("review_ready")),
                    () -> assertTrue(epic.runningPlans().isEmpty()),
                    () -> assertTrue(epic.alongsideLandings().isEmpty()));
        }

        @Test
        @DisplayName("should refuse an epic without an id and JSON that is no object")
        void shouldRefuseUnusableShapes() {
            var title = Generators.nonBlankStrings().next();
            var map = Map.<String, Object>of("title", title);
            var json = JsonTree.write(List.of(title));

            assertAll(
                    () -> assertThrows(JsonFormatException.class, () -> EpicProcess.fromMap(map)),
                    () -> assertThrows(JsonFormatException.class, () -> EpicProcess.fromJson(json)));
        }
    }
}
