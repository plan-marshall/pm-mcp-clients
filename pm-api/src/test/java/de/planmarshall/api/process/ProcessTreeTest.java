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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

@DisplayName("ProcessTreeTest")
@EnableGeneratorController
class ProcessTreeTest {

    @Test
    @DisplayName("should serialize and deserialize complete ProcessTree round-trip")
    void shouldRoundTripProcessTree() throws Exception {
        // Arrange
        var planId = Generators.nonEmptyStrings().next();
        var planTitle = Generators.nonEmptyStrings().next();
        var projectId = Generators.nonEmptyStrings().next();
        var epicId = Generators.nonEmptyStrings().next();
        var taskId = Generators.nonEmptyStrings().next();
        var totalTokens = (long) Generators.integers(0, 1_000_000).next();
        var project = new ProcessProjectRef(projectId, projectId, "/api/v1/projects/" + projectId);
        var epic = new ProcessEpicRef(epicId, epicId, "/api/v1/epics/" + epicId + "/process");
        var tokens = new ProcessTokens(totalTokens, 380000L, 32000L, true, 17, 15, false, false);

        var offTrack = new OffTrackInfo("repeating", "show", 5, 5);
        var task1 = new LeafTask(
                taskId,
                "implement",
                "claude",
                "sonnet",
                1,
                "running",
                null,
                360L,
                48000L,
                "/api/v1/plans/" + planId + "/tasks/" + taskId + "/activity",
                offTrack
        );

        var leafEnvelope = new ProcessNode(
                "execute.envelope",
                "execute.envelope",
                "tasks",
                "running",
                1,
                1,
                360L,
                null,
                List.of(),
                List.of(task1),
                null,
                null,
                Map.of("self", "/api/v1/plans/" + planId + "/nodes/execute.envelope")
        );

        var phaseExecute = new ProcessNode(
                "execute",
                "phase execute",
                "phase",
                "running",
                1,
                1,
                1500L,
                null,
                List.of(leafEnvelope),
                List.of(),
                null,
                null,
                Map.of()
        );

        var prunedRefine = new ProcessNode(
                "refine",
                "phase refine",
                "phase",
                "not_in_plan",
                0,
                0,
                0L,
                "lane_choice",
                List.of(),
                List.of(),
                null,
                null,
                Map.of()
        );

        var tree = new ProcessTree(
                planId,
                planTitle,
                project,
                epic,
                "execute",
                "running",
                "execute.envelope · " + taskId,
                false,
                2,
                tokens,
                List.of(phaseExecute, prunedRefine),
                List.of("execute", "execute.envelope", taskId),
                214,
                "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                7800L
        );

        // Act
        var json = tree.toJson();
        assertNotNull(json);
        var parsed = ProcessTree.fromJson(json);

        // Assert
        assertEquals(planId, parsed.planId());
        assertEquals(planTitle, parsed.title());
        assertEquals(projectId, parsed.project().projectId());
        assertEquals(epicId, parsed.epic() != null ? parsed.epic().epicId() : "");
        assertEquals("execute", parsed.currentPhase());
        assertEquals("running", parsed.phaseState());
        assertEquals(totalTokens, parsed.tokensSoFar().total());
        assertTrue(parsed.tokensSoFar().floor());
        assertEquals(2, parsed.nodes().size());

        var parsedExec = parsed.nodes().get(0);
        assertEquals("execute", parsedExec.nodeId());
        assertEquals("running", parsedExec.state());
        assertEquals(1, parsedExec.subNodes().size());

        var parsedLeaf = parsedExec.subNodes().get(0);
        assertEquals("execute.envelope", parsedLeaf.nodeId());
        assertEquals(1, parsedLeaf.leafTasks().size());

        var parsedTask = parsedLeaf.leafTasks().get(0);
        assertEquals(taskId, parsedTask.taskId());
        assertEquals("implement", parsedTask.role());
        assertNotNull(parsedTask.offTrack());
        assertEquals("repeating", parsedTask.offTrack().signal());
        assertEquals("show", parsedTask.offTrack().action());

        var parsedPruned = parsed.nodes().get(1);
        assertEquals("refine", parsedPruned.nodeId());
        assertEquals("not_in_plan", parsedPruned.state());
        assertEquals("lane_choice", parsedPruned.pruneReason());
    }

    @Test
    @DisplayName("should serialize and deserialize TaskActivity with full entry types")
    void shouldRoundTripTaskActivity() throws Exception {
        // Arrange
        var taskId = Generators.nonEmptyStrings().next();
        var harness = Generators.nonEmptyStrings().next();
        var content = Generators.nonEmptyStrings().next();
        var toolSize = Generators.nonEmptyStrings().next();
        var gapCount = Generators.integers(0, 20).next();
        var entry1 = new ActivityEntry(
                "14:02:11",
                "text",
                null,
                null,
                false,
                content,
                null,
                List.of(),
                List.of()
        );

        var entry2 = new ActivityEntry(
                "14:02:14",
                "tool_call",
                "read",
                "pm-relay/src/main/java/Transport.java",
                false,
                null,
                null,
                List.of(),
                List.of()
        );

        var entry3 = new ActivityEntry(
                "14:02:20",
                "tool_call",
                "bash",
                "./mvnw -pl pm-relay test",
                true,
                null,
                toolSize,
                List.of(),
                List.of()
        );

        var entry4 = new ActivityEntry(
                "14:03:02",
                "file_changed",
                null,
                null,
                false,
                null,
                null,
                List.of("pm-relay/src/main/java/Transport.java"),
                List.of()
        );

        var offTrack = new OffTrackInfo("repeating", "show", 5, 5);
        var activity = new TaskActivity(
                taskId,
                "implement",
                harness,
                "sonnet",
                1,
                360L,
                48000L,
                List.of(entry1, entry2, entry3, entry4),
                offTrack,
                false,
                gapCount
        );

        // Act
        var json = activity.toJson();
        var parsed = TaskActivity.fromJson(json);

        // Assert
        assertEquals(taskId, parsed.taskId());
        assertEquals("implement", parsed.role());
        assertEquals(harness, parsed.harness());
        assertEquals(4, parsed.entries().size());
        assertEquals(gapCount, parsed.gapCount());
        assertFalse(parsed.serverOnly());

        assertEquals("text", parsed.entries().get(0).kind());
        assertEquals(content, parsed.entries().get(0).content());

        assertEquals("tool_call", parsed.entries().get(1).kind());
        assertEquals("read", parsed.entries().get(1).toolName());

        assertEquals("tool_call", parsed.entries().get(2).kind());
        assertTrue(parsed.entries().get(2).toolFailed());
        assertEquals(toolSize, parsed.entries().get(2).size());

        assertEquals("file_changed", parsed.entries().get(3).kind());
        assertEquals(1, parsed.entries().get(3).files().size());
    }

    @Test
    @DisplayName("should serialize and deserialize EpicProcess")
    void shouldRoundTripEpicProcess() throws Exception {
        // Arrange
        var epicId = Generators.nonEmptyStrings().next();
        var project = Generators.nonEmptyStrings().next();
        var totalTokens = (long) Generators.integers(0, 1_000_000).next();
        var p1 = new EpicProcessPlan("fix-relay", "Fix Relay", "execute", "running", 2, 5, true);
        var p2 = new EpicProcessPlan("docs-typo", "Docs Typo", "init", "staged", 0, 1, false);
        var tokens = new ProcessTokens(totalTokens, 450000L, 50000L, false, 20, 20, false, false);

        var epic = new EpicProcess(
                epicId,
                project,
                "Relay Hardening",
                "running",
                List.of(p1),
                List.of(p2),
                List.of("remediation.ci-failure · done"),
                tokens,
                true
        );

        // Act
        var json = epic.toJson();
        var parsed = EpicProcess.fromJson(json);

        // Assert
        assertEquals(epicId, parsed.epicId());
        assertEquals(project, parsed.project());
        assertEquals(1, parsed.runningPlans().size());
        assertEquals(1, parsed.stagedPlans().size());
        assertTrue(parsed.reviewReady());
        assertNotNull(parsed.tokensSoFar());
        assertEquals(totalTokens, parsed.tokensSoFar().total());
    }

    @Test
    @DisplayName("should serialize and deserialize all process SSE events")
    void shouldRoundTripProcessEvents() throws Exception {
        var planId = Generators.nonEmptyStrings().next();
        var taskId = Generators.nonEmptyStrings().next();
        var run = Generators.integers(0, 100).next();
        var leftSeconds = (long) Generators.integers(0, 100_000).next();
        var returnedSeconds = (long) Generators.integers(0, 100_000).next();
        var instanceVersion = Generators.integers(0, 100).next();
        var activityContent = Generators.nonEmptyStrings().next();
        var changedTotal = (long) Generators.integers(0, 1_000_000).next();

        // StateEnteredEvent
        var entered = new StateEnteredEvent(planId, "execute", run, "2026-10-17T10:00:00Z");
        assertEquals(StateEnteredEvent.EVENT_NAME, "state.entered");
        var parsedEntered = StateEnteredEvent.fromJson(entered.toJson());
        assertEquals(planId, parsedEntered.planId());
        assertEquals("execute", parsedEntered.nodeId());
        assertEquals(run, parsedEntered.run());

        // StateLeftEvent
        var left = new StateLeftEvent(planId, "execute", "done", 1, leftSeconds);
        assertEquals(StateLeftEvent.EVENT_NAME, "state.left");
        var parsedLeft = StateLeftEvent.fromJson(left.toJson());
        assertEquals("done", parsedLeft.state());
        assertEquals(leftSeconds, parsedLeft.durationSeconds());

        // SubworkflowCalledEvent
        var called = new SubworkflowCalledEvent(planId, "execute", "land", "2026-10-17T10:02:00Z");
        assertEquals(SubworkflowCalledEvent.EVENT_NAME, "subworkflow.called");
        var parsedCalled = SubworkflowCalledEvent.fromJson(called.toJson());
        assertEquals("land", parsedCalled.subworkflowId());

        // SubworkflowReturnedEvent
        var returned = new SubworkflowReturnedEvent(planId, "execute", "land", "done", returnedSeconds);
        assertEquals(SubworkflowReturnedEvent.EVENT_NAME, "subworkflow.returned");
        var parsedReturned = SubworkflowReturnedEvent.fromJson(returned.toJson());
        assertEquals("done", parsedReturned.state());
        assertEquals(returnedSeconds, parsedReturned.durationSeconds());

        // StepOpenedEvent & StepClosedEvent
        var stepOpened = new StepOpenedEvent(planId, "build", "2026-10-17T10:05:00Z");
        assertEquals(StepOpenedEvent.EVENT_NAME, "step.opened");
        assertEquals("build", StepOpenedEvent.fromJson(stepOpened.toJson()).stepId());

        var stepClosed = new StepClosedEvent(planId, "build", "success", 45L);
        assertEquals(StepClosedEvent.EVENT_NAME, "step.closed");
        assertEquals("success", StepClosedEvent.fromJson(stepClosed.toJson()).outcome());

        // InstanceChangedEvent
        var instChanged = new InstanceChangedEvent(planId, instanceVersion, "waiver granted");
        assertEquals(InstanceChangedEvent.EVENT_NAME, "instance.changed");
        assertEquals(instanceVersion, InstanceChangedEvent.fromJson(instChanged.toJson()).instanceVersion());

        // ActivityEvent
        var actEntry = new ActivityEntry("14:02:11", "text", null, null, false, activityContent, null, List.of(),
                List.of());
        var actEvent = new ActivityEvent(planId, taskId, actEntry);
        assertEquals(ActivityEvent.EVENT_NAME, "activity");
        var parsedActEvent = ActivityEvent.fromJson(actEvent.toJson());
        assertEquals(taskId, parsedActEvent.taskId());
        assertEquals(activityContent, parsedActEvent.entry().content());

        // TokensChangedEvent
        var tokTokens = new ProcessTokens(changedTotal, 80L, 20L, false, 1, 1, false, false);
        var tokEvent = new TokensChangedEvent(planId, tokTokens);
        assertEquals(TokensChangedEvent.EVENT_NAME, "tokens.changed");
        assertEquals(changedTotal, TokensChangedEvent.fromJson(tokEvent.toJson()).tokens().total());

        // TaskOffTrackEvent
        var offEvent = new TaskOffTrackEvent(planId, taskId, "repeating", "show", true);
        assertEquals(TaskOffTrackEvent.EVENT_NAME, "task.off_track");
        var parsedOffEvent = TaskOffTrackEvent.fromJson(offEvent.toJson());
        assertEquals("repeating", parsedOffEvent.signal());
        assertEquals("show", parsedOffEvent.action());
        assertTrue(parsedOffEvent.offTrack());
    }

    @Test
    @DisplayName("should verify closed enumerations mappings and conversions")
    void shouldVerifyEnumerations() {
        assertEquals(NodeKind.PHASE, NodeKind.fromValue("phase"));
        assertEquals(NodeKind.SUB_WORKFLOW, NodeKind.fromValue("sub_workflow"));
        assertEquals(NodeKind.LEAF, NodeKind.fromValue("leaf"));
        assertEquals("phase", NodeKind.PHASE.value());

        assertEquals(NodeState.DONE, NodeState.fromValue("done"));
        assertEquals(NodeState.RUNNING, NodeState.fromValue("running"));
        assertEquals(NodeState.WAITING_FOR_OPERATOR, NodeState.fromValue("waiting_for_you"));
        assertEquals(NodeState.WAITING_FOR_OPERATOR, NodeState.fromValue("waiting"));
        assertEquals(NodeState.NOT_IN_PLAN, NodeState.fromValue("not_in_plan"));
        assertEquals("✓", NodeState.DONE.glyph());
        assertEquals("+", NodeState.DONE.asciiGlyph());
        assertEquals("▶", NodeState.RUNNING.glyph());
        assertEquals(">", NodeState.RUNNING.asciiGlyph());

        assertEquals(PruneReason.LANE_CHOICE, PruneReason.fromValue("lane_choice"));
        assertEquals(PruneReason.UNNEEDED_STEP, PruneReason.fromValue("unneeded_step"));
        assertEquals(PruneReason.NOT_IN_PLAN, PruneReason.fromValue("not_in_plan"));

        assertEquals(TaskStatus.RUNNING, TaskStatus.fromValue("running"));
        assertEquals(TaskStatus.DONE, TaskStatus.fromValue("done"));

        assertEquals(TaskOutcome.SUCCESS, TaskOutcome.fromValue("success"));
        assertEquals(TaskOutcome.FAILURE, TaskOutcome.fromValue("failure"));
        assertEquals(TaskOutcome.YIELDED, TaskOutcome.fromValue("yielded"));

        assertEquals(JobStatus.RUNNING, JobStatus.fromValue("running"));
        assertEquals(JobStatus.WAITS_SLOT, JobStatus.fromValue("waits_slot"));

        assertEquals(JobOutcome.SUCCESS, JobOutcome.fromValue("success"));
        assertEquals(JobOutcome.FAILURE, JobOutcome.fromValue("failure"));

        assertEquals(OffTrackSignal.REPEATING, OffTrackSignal.fromValue("repeating"));
        assertEquals(OffTrackSignal.NO_PROGRESS, OffTrackSignal.fromValue("no_progress"));

        assertEquals(OffTrackAction.SHOW, OffTrackAction.fromValue("show"));
        assertEquals(OffTrackAction.RESTART, OffTrackAction.fromValue("restart"));

        assertEquals(ActivityEntryKind.MODEL_TEXT, ActivityEntryKind.fromValue("text"));
        assertEquals(ActivityEntryKind.TOOL_CALL, ActivityEntryKind.fromValue("tool_call"));
        assertEquals(ActivityEntryKind.TOOL_CALL, ActivityEntryKind.fromValue("tool"));
        assertEquals(ActivityEntryKind.FILES_CHANGED, ActivityEntryKind.fromValue("file_changed"));
    }

    @Test
    @DisplayName("should serialize and deserialize LeafJob round-trip")
    void shouldRoundTripLeafJob() throws Exception {
        var durationSeconds = (long) Generators.integers(0, 10_000).next();
        var job1 = new LeafJob("build", "running", "success", durationSeconds, true);
        assertEquals("build", job1.kind());
        assertEquals("running", job1.status());
        assertEquals("success", job1.outcome());
        assertEquals(durationSeconds, job1.durationSeconds());
        assertTrue(job1.waitsSlot());

        var map = job1.toMap();
        assertEquals("build", map.get("kind"));
        assertEquals("running", map.get("status"));
        assertEquals("success", map.get("outcome"));
        assertEquals(durationSeconds, map.get("duration_seconds"));
        assertEquals(true, map.get("waits_slot"));

        var json = job1.toJson();
        assertNotNull(json);

        var restored = LeafJob.fromJson(json);
        assertEquals(job1, restored);

        // Test defaults
        var jobDefault = new LeafJob(null, null, null, 0L, false);
        assertEquals("job", jobDefault.kind());
        assertEquals("done", jobDefault.status());
        assertFalse(jobDefault.waitsSlot());
        assertFalse(jobDefault.toMap().containsKey("waits_slot"));
        assertFalse(jobDefault.toMap().containsKey("outcome"));
    }

    @Test
    @DisplayName("should serialize and deserialize ActivityEntry across all kinds")
    void shouldRoundTripActivityEntry() throws Exception {
        var toolTarget = Generators.nonEmptyStrings().next();
        var entry1 = new ActivityEntry("10:00:00", "tool", "read", toolTarget, false, null, "1 KiB", List.of(),
                List.of());
        var map1 = entry1.toMap();
        assertEquals("tool", map1.get("kind"));
        assertEquals("read", map1.get("tool_name"));
        assertEquals(toolTarget, map1.get("tool_target"));
        var restored1 = ActivityEntry.fromMap(map1);
        assertEquals(entry1, restored1);

        var content = Generators.nonEmptyStrings().next();
        var files = List.of(Generators.nonEmptyStrings().next(), Generators.nonEmptyStrings().next());
        var questions = List.of(Generators.nonEmptyStrings().next());
        var entry2 = new ActivityEntry("10:01:00", "file_changed", null, null, false, content, null, files,
                questions);
        var json2 = entry2.toJson();
        var restored2 = ActivityEntry.fromJson(json2);
        assertEquals(entry2, restored2);

        // Test defaults
        var entryDefault = new ActivityEntry("10:02:00", "text", null, null, false, null, null, null, null);
        assertEquals("text", entryDefault.kind());
        assertTrue(entryDefault.files().isEmpty());
        assertTrue(entryDefault.questions().isEmpty());
    }

    @Test
    @DisplayName("should serialize and deserialize ProcessTokens with edge cases")
    void shouldRoundTripProcessTokens() throws Exception {
        var tokensNone = new ProcessTokens(0L, 0L, 0L, false, 0, 0, true, false);
        var mapNone = tokensNone.toMap();
        assertEquals(true, mapNone.get("none_yet"));
        var restoredNone = ProcessTokens.fromMap(mapNone);
        assertEquals(tokensNone, restoredNone);

        var tokensNotMeasured = new ProcessTokens(0L, 0L, 0L, false, 5, 0, false, true);
        var jsonNotMeasured = tokensNotMeasured.toJson();
        var restoredNotMeasured = ProcessTokens.fromJson(jsonNotMeasured);
        assertEquals(tokensNotMeasured, restoredNotMeasured);
    }

    @Test
    @DisplayName("should serialize and deserialize ProcessNode with LeafJob")
    void shouldRoundTripProcessNodeWithLeafJob() throws Exception {
        var waitsFor = Generators.nonEmptyStrings().next();
        var job = new LeafJob("compile", "waits_slot", null, 45L, true);
        var node = new ProcessNode("build-step", "Build Step", "job", "running", 1, 1, 45L, null, List.of(),
                List.of(), job, waitsFor, Map.of("abort", "/api/v1/plans/p/abort"));

        var map = node.toMap();
        assertNotNull(map.get("leaf_job"));
        assertEquals(waitsFor, map.get("waits_for"));

        var json = node.toJson();
        var restored = ProcessNode.fromJson(json);
        assertEquals(node, restored);
        assertNotNull(restored.leafJob());
        assertEquals("compile", restored.leafJob().kind());
        assertTrue(restored.leafJob().waitsSlot());
    }

    @Test
    @DisplayName("should serialize and deserialize ProcessProjectRef and ProcessEpicRef")
    void shouldRoundTripProjectAndEpicRefs() throws Exception {
        var projectId = Generators.nonEmptyStrings().next();
        var projectName = Generators.nonEmptyStrings().next();
        var proj = new ProcessProjectRef(projectId, projectName, "/api/v1/projects/" + projectId);
        assertEquals(proj, ProcessProjectRef.fromJson(proj.toJson()));

        var epicId = Generators.nonEmptyStrings().next();
        var epicTitle = Generators.nonEmptyStrings().next();
        var epic = new ProcessEpicRef(epicId, epicTitle, "/api/v1/epics/" + epicId + "/process");
        assertEquals(epic, ProcessEpicRef.fromJson(epic.toJson()));

        var offTrack = new OffTrackInfo("stalled", "restart", 3, 3);
        assertEquals(offTrack, OffTrackInfo.fromJson(offTrack.toJson()));
    }
}
