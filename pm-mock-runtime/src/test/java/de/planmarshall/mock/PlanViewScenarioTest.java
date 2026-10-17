/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.process.ActivityEntryKind;
import de.planmarshall.api.process.EpicProcess;
import de.planmarshall.api.process.NodeState;
import de.planmarshall.api.process.OffTrackSignal;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.process.PruneReason;
import de.planmarshall.api.process.TaskActivity;

@DisplayName("PlanViewScenarioTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PlanViewScenarioTest {

    @Test
    @DisplayName("loads plan-view scenario and serves full process tree for fix-relay")
    void shouldLoadScenarioAndServeProcessTree() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PLAN_VIEW)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.PLAN_VIEW, runtime.currentScenarioName());

            var res = client.send(HttpRequest.get("/api/v1/plans/fix-relay/process")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var body = new String(res.body());
            var tree = ProcessTree.fromJson(body);

            assertEquals("fix-relay", tree.planId());
            assertEquals("Fix Relay Transport", tree.title());
            assertNotNull(tree.project());
            assertEquals("pm-mcp-clients", tree.project().projectId());
            assertNotNull(tree.epic());
            assertEquals("relay-hardening", tree.epic().epicId());
            assertEquals("execute", tree.currentPhase());
            assertEquals("running", tree.phaseState());
            assertFalse(tree.nodes().isEmpty());

            // Check execute phase node has sub-nodes and leaf tasks
            var execNode = tree.nodes().stream()
                    .filter(n -> "execute".equals(n.nodeId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(NodeState.RUNNING, NodeState.fromValue(execNode.state()));
            assertFalse(execNode.subNodes().isEmpty());

            var implSubNode = execNode.subNodes().getFirst();
            assertFalse(implSubNode.leafTasks().isEmpty());
            var task003 = implSubNode.leafTasks().stream()
                    .filter(t -> "TASK-003".equals(t.taskId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("TASK-003", task003.taskId());
            assertNotNull(task003.offTrack());
            assertEquals(OffTrackSignal.REPEATING, OffTrackSignal.fromValue(task003.offTrack().signal()));
        }
    }

    @Test
    @DisplayName("serves land-lane plan docs-typo with pruned phases")
    void shouldServePrunedPhasesForLandLanePlan() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PLAN_VIEW)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/plans/docs-typo/process")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var tree = ProcessTree.fromJson(new String(res.body()));
            assertEquals("docs-typo", tree.planId());

            // Find pruned node
            var prunedNode = tree.nodes().stream()
                    .filter(n -> n.pruneReason() != null)
                    .findFirst()
                    .orElseThrow();
            assertEquals(PruneReason.LANE_CHOICE, PruneReason.fromValue(prunedNode.pruneReason()));
            assertEquals(NodeState.NOT_IN_PLAN, NodeState.fromValue(prunedNode.state()));
        }
    }

    @Test
    @DisplayName("serves worker activity log for leaf task with off-track details")
    void shouldServeWorkerActivityLog() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PLAN_VIEW)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/plans/fix-relay/tasks/TASK-003/activity")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var activity = TaskActivity.fromJson(new String(res.body()));
            assertEquals("TASK-003", activity.taskId());
            assertNotNull(activity.offTrack());
            assertEquals(OffTrackSignal.REPEATING, OffTrackSignal.fromValue(activity.offTrack().signal()));
            assertFalse(activity.entries().isEmpty());

            var kinds = activity.entries().stream().map(e -> ActivityEntryKind.fromValue(e.kind())).toList();
            assertTrue(kinds.contains(ActivityEntryKind.MODEL_TEXT));
            assertTrue(kinds.contains(ActivityEntryKind.TOOL_CALL));
            assertTrue(kinds.contains(ActivityEntryKind.FILES_CHANGED));
        }
    }

    @Test
    @DisplayName("serves epic process view with staged and running plans")
    void shouldServeEpicProcess() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PLAN_VIEW)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/epics/relay-hardening/process")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var epic = EpicProcess.fromJson(new String(res.body()));
            assertEquals("relay-hardening", epic.epicId());
            assertEquals("pm-mcp-clients", epic.project());
            assertFalse(epic.runningPlans().isEmpty());
            assertEquals("fix-relay", epic.runningPlans().getFirst().planId());
            assertFalse(epic.stagedPlans().isEmpty());
            assertEquals("relay-auth-v2", epic.stagedPlans().getFirst().planId());
        }
    }

    @Test
    @DisplayName("serves live activity SSE event stream")
    void shouldServeActivityEventStream() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PLAN_VIEW)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/plans/fix-relay/tasks/TASK-003/activity/events")
                         .withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                assertNotNull(exchange.events());
            }
        }
    }

    @Test
    @DisplayName("synthesizes dynamic plan and epic process routes for unknown IDs")
    void shouldSynthesizeDynamicProcessRoutes() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.PLAN_VIEW)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/plans/dynamic-custom-plan/process")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());
            var tree = ProcessTree.fromJson(new String(res.body()));
            assertEquals("dynamic-custom-plan", tree.planId());

            var epicRes = client.send(HttpRequest.get("/api/v1/epics/dynamic-custom-epic/process")
                    .withHeader("Authorization", auth));
            assertEquals(200, epicRes.status());
            var epic = EpicProcess.fromJson(new String(epicRes.body()));
            assertEquals("dynamic-custom-epic", epic.epicId());
        }
    }
}
