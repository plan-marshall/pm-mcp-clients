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

import java.util.List;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.process.ActivityEntryKind;
import de.planmarshall.api.process.EpicProcess;
import de.planmarshall.api.process.NodeState;
import de.planmarshall.api.process.OffTrackSignal;
import de.planmarshall.api.process.ProcessTokens;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.process.PruneReason;
import de.planmarshall.api.process.TaskActivity;

@DisplayName("PlanViewScenarioTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PlanViewScenarioTest {

    private static final String AUTH_HEADER = "Authorization";

    @Nested
    @DisplayName("The running plan")
    class RunningPlan {

        @Test
        @DisplayName("serves the full process tree with its path, project, epic, and tokens")
        void processTree() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var tree = ProcessTree.fromJson(get(runtime, "/api/v1/plans/fix-relay/process").bodyText());

                assertEquals("fix-relay", tree.planId());
                assertEquals("Fix Relay Transport", tree.title());
                assertNotNull(tree.project());
                assertEquals("pm-mcp-clients", tree.project().projectId());
                assertNotNull(tree.epic());
                assertEquals("relay-hardening", tree.epic().epicId());
                assertEquals("execute", tree.currentPhase());
                assertEquals("running", tree.phaseState());
                assertEquals(1, tree.instanceVersion());
                assertEquals(List.of("execute", "execute.envelope", "TASK-003"),
                        tree.currentPositionPath());
                assertFalse(tree.nodes().isEmpty());

                var execNode = tree.nodes().stream()
                        .filter(node -> "execute".equals(node.nodeId()))
                        .findFirst().orElseThrow();
                assertEquals(NodeState.RUNNING, NodeState.fromValue(execNode.state()));
                assertFalse(execNode.subNodes().isEmpty());

                var task003 = execNode.subNodes().getFirst().leafTasks().stream()
                        .filter(task -> "TASK-003".equals(task.taskId()))
                        .findFirst().orElseThrow();
                assertNotNull(task003.offTrack());
                assertEquals(OffTrackSignal.REPEATING, OffTrackSignal.fromValue(task003.offTrack().signal()));
                assertNotNull(task003.activityLink());
            }
        }

        @Test
        @DisplayName("serves the land-lane plan with its pruned lane")
        void prunedLane() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.pruned-lane")) {
                var tree = ProcessTree.fromJson(get(runtime, "/api/v1/plans/docs-typo/process").bodyText());
                assertEquals("docs-typo", tree.planId());

                var pruned = tree.nodes().stream()
                        .filter(node -> node.pruneReason() != null)
                        .findFirst().orElseThrow();
                assertEquals(PruneReason.LANE_CHOICE, PruneReason.fromValue(pruned.pruneReason()));
                assertEquals(NodeState.NOT_IN_PLAN, NodeState.fromValue(pruned.state()));
            }
        }

        @Test
        @DisplayName("serves a plan in each phase")
        void eachPhase() throws Exception {
            for (var phase : List.of("init", "refine", "outline", "plan", "execute", "finalize")) {
                try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.phase." + phase)) {
                    var tree = ProcessTree.fromJson(
                            get(runtime, "/api/v1/plans/fix-relay/process").bodyText());
                    assertEquals(phase, tree.currentPhase(), phase);
                    var current = tree.nodes().stream()
                            .filter(node -> phase.equals(node.nodeId()))
                            .findFirst().orElseThrow();
                    assertEquals(NodeState.RUNNING, NodeState.fromValue(current.state()), phase);
                }
            }
        }

        @Test
        @DisplayName("shows a phase that runs a second time")
        void loopback() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.loopback")) {
                var tree = ProcessTree.fromJson(get(runtime, "/api/v1/plans/fix-relay/process").bodyText());
                var refine = tree.nodes().stream()
                        .filter(node -> "refine".equals(node.nodeId()))
                        .findFirst().orElseThrow();
                assertEquals(2, refine.runCount());
                assertEquals(2, refine.currentRun());
            }
        }

        @Test
        @DisplayName("shows a sub-workflow nested three levels deep")
        void nestedThreeDeep() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.nested-3-deep")) {
                var tree = ProcessTree.fromJson(get(runtime, "/api/v1/plans/fix-relay/process").bodyText());
                assertEquals(List.of("execute", "execute.envelope",
                        "execute.envelope.remediation", "execute.envelope.remediation.fix",
                        "fix.apply"), tree.currentPositionPath());

                var execute = tree.nodes().stream()
                        .filter(node -> "execute".equals(node.nodeId()))
                        .findFirst().orElseThrow();
                var level1 = execute.subNodes().getFirst();
                var level2 = level1.subNodes().getFirst();
                var level3 = level2.subNodes().getFirst();
                assertEquals("execute.envelope", level1.nodeId());
                assertEquals("execute.envelope.remediation", level2.nodeId());
                assertEquals("execute.envelope.remediation.fix", level3.nodeId());
            }
        }

        @Test
        @DisplayName("shows the new instance version on the plan")
        void newVersion() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.new-version")) {
                var tree = ProcessTree.fromJson(get(runtime, "/api/v1/plans/fix-relay/process").bodyText());
                assertEquals(2, tree.instanceVersion());
            }
        }
    }

    @Nested
    @DisplayName("The worker's activity")
    class Activity {

        @Test
        @DisplayName("serves the live activity with every entry kind and the off-track signal")
        void liveActivity() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.failed-tool")) {
                var activity = TaskActivity.fromJson(
                        get(runtime, "/api/v1/plans/fix-relay/tasks/TASK-003/activity").bodyText());
                assertEquals("TASK-003", activity.taskId());
                assertNotNull(activity.offTrack());
                assertEquals(OffTrackSignal.REPEATING, OffTrackSignal.fromValue(activity.offTrack().signal()));
                assertFalse(activity.entries().isEmpty());

                var kinds = activity.entries().stream()
                        .map(entry -> ActivityEntryKind.fromValue(entry.kind())).toList();
                assertTrue(kinds.contains(ActivityEntryKind.MODEL_TEXT));
                assertTrue(kinds.contains(ActivityEntryKind.TOOL_CALL));
                assertTrue(kinds.contains(ActivityEntryKind.FILES_CHANGED));
                assertTrue(kinds.contains(ActivityEntryKind.QUESTIONS_ASKED));

                var failedTool = activity.entries().stream()
                        .filter(entry -> entry.toolFailed() && "bash".equals(entry.toolName()))
                        .findFirst().orElseThrow();
                assertEquals("bash", failedTool.toolName());
            }
        }

        @Test
        @DisplayName("serves an ended task's activity as it was")
        void endedActivity() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.ended-task")) {
                var activity = TaskActivity.fromJson(
                        get(runtime, "/api/v1/plans/fix-relay/tasks/TASK-001/activity").bodyText());
                assertEquals("TASK-001", activity.taskId());
                assertEquals(25_000, activity.tokensSoFar());
                assertEquals(ActivityEntryKind.ANSWER_ACK,
                        ActivityEntryKind.fromValue(activity.entries().getLast().kind()));
            }
        }

        @Test
        @DisplayName("streams a live activity event on the task's own stream")
        void activityStream() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                try (var exchange = client.open(HttpRequest
                             .get("/api/v1/plans/fix-relay/tasks/TASK-003/activity/events")
                             .withHeader(AUTH_HEADER, auth))) {
                    assertTrue(exchange.isEventStream());
                    var event = exchange.events().next();
                    assertNotNull(event);
                    assertEquals("task.activity", event.event());
                    var data = JsonTree.asObject(JsonTree.parse(event.data()));
                    assertEquals("assistant_text", JsonTree.string(data, "kind"));
                    assertTrue(JsonTree.string(data, "text").contains("Analyzing"));
                }
            }
        }
    }

    @Nested
    @DisplayName("The tokens so far")
    class Tokens {

        @Test
        @DisplayName("is fully measured")
        void complete() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.tokens-complete")) {
                var tokens = tokens(runtime);
                assertEquals(17, tokens.tasksMeasured());
                assertEquals(17, tokens.tasksTotal());
                assertFalse(tokens.floor());
                assertFalse(tokens.noneYet());
                assertFalse(tokens.notMeasured());
                assertEquals(512_000, tokens.total());
            }
        }

        @Test
        @DisplayName("is partly measured: the total is a floor with its coverage")
        void partial() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.tokens-partial")) {
                var tokens = tokens(runtime);
                assertEquals(15, tokens.tasksMeasured());
                assertEquals(17, tokens.tasksTotal());
                assertTrue(tokens.floor());
                assertFalse(tokens.notMeasured());
            }
        }

        @Test
        @DisplayName("has none before the first model call")
        void noneYet() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.tokens-none")) {
                var tokens = tokens(runtime);
                assertTrue(tokens.noneYet());
                assertEquals(0, tokens.tasksMeasured());
                assertFalse(tokens.floor());
            }
        }

        @Test
        @DisplayName("is not measured at all after model calls: no total and no floor")
        void notMeasured() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#process.tokens-not-measured")) {
                var tokens = tokens(runtime);
                assertTrue(tokens.notMeasured());
                assertEquals(0, tokens.tasksMeasured());
                assertEquals(17, tokens.tasksTotal());
                assertFalse(tokens.floor());
            }
        }
    }

    @Test
    @DisplayName("serves the epic process with the running and staged plans")
    void epicProcess() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var epic = EpicProcess.fromJson(get(runtime, "/api/v1/epics/relay-hardening/process").bodyText());
            assertEquals("relay-hardening", epic.epicId());
            assertEquals("pm-mcp-clients", epic.project());
            assertEquals("fix-relay", epic.runningPlans().getFirst().planId());
            assertEquals("relay-auth-v2", epic.stagedPlans().getFirst().planId());
        }
    }

    @Test
    @DisplayName("synthesizes the dynamic process routes for unknown ids")
    void dynamicRoutes() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var tree = ProcessTree.fromJson(
                    get(runtime, "/api/v1/plans/dynamic-custom-plan/process").bodyText());
            assertEquals("dynamic-custom-plan", tree.planId());

            var activity = TaskActivity.fromJson(
                    get(runtime, "/api/v1/plans/dynamic-custom-plan/tasks/TASK-042/activity").bodyText());
            assertEquals("TASK-042", activity.taskId());

            var epic = EpicProcess.fromJson(
                    get(runtime, "/api/v1/epics/dynamic-custom-epic/process").bodyText());
            assertEquals("dynamic-custom-epic", epic.epicId());
        }
    }

    private static ProcessTokens tokens(MockRuntime runtime) throws Exception {
        var tree = ProcessTree.fromJson(get(runtime, "/api/v1/plans/fix-relay/process").bodyText());
        return tree.tokensSoFar();
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
