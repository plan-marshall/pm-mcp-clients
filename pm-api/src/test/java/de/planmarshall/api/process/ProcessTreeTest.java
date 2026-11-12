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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.QueuedRevision;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.ChangeSize;

@DisplayName("ProcessTreeTest")
@EnableGeneratorController
class ProcessTreeTest {

    /**
     * A complete process tree with the generated values it was built from: a running execute phase with one
     * envelope and one off-track task, and a pruned refine phase.
     */
    private record FullTree(String planId, String planTitle, String projectId, String epicId, String taskId,
                            long totalTokens, ProcessTree tree) {
    }

    private static FullTree fullTree() {
        var planId = Generators.nonBlankStrings().next();
        var planTitle = Generators.nonBlankStrings().next();
        var projectId = Generators.nonBlankStrings().next();
        var epicId = Generators.nonBlankStrings().next();
        var taskId = Generators.nonBlankStrings().next();
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
                7800L,
                null,
                null
        );
        return new FullTree(planId, planTitle, projectId, epicId, taskId, totalTokens, tree);
    }

    @Test
    @DisplayName("should round-trip the plan's own members of a complete ProcessTree")
    void shouldRoundTripProcessTree() throws Exception {
        // Arrange
        var full = fullTree();

        // Act
        var json = full.tree().toJson();
        assertNotNull(json);
        var parsed = ProcessTree.fromJson(json);

        // Assert
        assertEquals(full.planId(), parsed.planId());
        assertEquals(full.planTitle(), parsed.title());
        assertEquals(full.projectId(), parsed.project().projectId());
        assertEquals(full.epicId(), parsed.epic() != null ? parsed.epic().epicId() : "");
        assertEquals("execute", parsed.currentPhase());
        assertEquals("running", parsed.phaseState());
        assertEquals(full.totalTokens(), parsed.tokensSoFar().total());
        assertTrue(parsed.tokensSoFar().floor());
        assertEquals(2, parsed.nodes().size());
    }

    @Test
    @DisplayName("should round-trip the running phase of a complete ProcessTree with its envelope and its task")
    void shouldRoundTripProcessTreeRunningPhase() throws Exception {
        // Arrange
        var full = fullTree();

        // Act
        var parsed = ProcessTree.fromJson(full.tree().toJson());

        // Assert
        var parsedExec = parsed.nodes().get(0);
        assertEquals("execute", parsedExec.nodeId());
        assertEquals("running", parsedExec.state());
        assertEquals(1, parsedExec.subNodes().size());

        var parsedLeaf = parsedExec.subNodes().get(0);
        assertEquals("execute.envelope", parsedLeaf.nodeId());
        assertEquals(1, parsedLeaf.leafTasks().size());

        var parsedTask = parsedLeaf.leafTasks().get(0);
        assertEquals(full.taskId(), parsedTask.taskId());
        assertEquals("implement", parsedTask.role());
        assertNotNull(parsedTask.offTrack());
        assertEquals("repeating", parsedTask.offTrack().signal());
        assertEquals("show", parsedTask.offTrack().action());
    }

    @Test
    @DisplayName("should round-trip the pruned phase of a complete ProcessTree and write no absent member")
    void shouldRoundTripProcessTreePrunedPhaseAndAbsentMembers() throws Exception {
        // Arrange
        var full = fullTree();

        // Act
        var json = full.tree().toJson();
        var parsed = ProcessTree.fromJson(json);

        // Assert
        var parsedPruned = parsed.nodes().get(1);
        assertEquals("refine", parsedPruned.nodeId());
        assertEquals("not_in_plan", parsedPruned.state());
        assertEquals("lane_choice", parsedPruned.pruneReason());
        assertFalse(json.contains("\"change\""), json);
        assertNull(parsed.change());
        assertNull(parsed.queuedRevision());
        assertFalse(json.contains("\"queued_revision\""));
    }

    @Test
    @DisplayName("should serialize and deserialize a non-null size of change")
    void shouldRoundTripChangeSize() throws Exception {
        var project = new ProcessProjectRef("project", "project", null);
        var change = new ChangeSize(12, 340, 27, 2);
        var tree = new ProcessTree("plan", "Plan", project, null, "execute", "running", null,
                false, 1, new ProcessTokens(0L, 0L, 0L, false, 0, 0, true, false),
                List.of(), List.of(), null, null, 0L, change, null);

        var json = tree.toJson();
        var parsed = ProcessTree.fromJson(json);

        assertTrue(json.contains("\"change\""));
        assertEquals(change, parsed.change());
    }

    @Test
    @DisplayName("should carry the plan's queued_revision through a round-trip and refuse a malformed one")
    void shouldRoundTripQueuedRevision() throws Exception {
        var planId = Generators.nonBlankStrings().next();
        var project = new ProcessProjectRef(planId, planId, null);
        var revision = new QueuedRevision(Generators.nonBlankStrings().next(), Generators.nonBlankStrings().next(),
                new ChatSubject(ChatSubjectKind.DECISION, Generators.nonBlankStrings().next()),
                Generators.booleans().next(), MessageText.of("chat.applies.on_start"));
        var tree = new ProcessTree(planId, planId, project, null, "execute", "running", null,
                false, 1, new ProcessTokens(0L, 0L, 0L, false, 0, 0, true, false),
                List.of(), List.of(), null, null, 0L, null, revision);

        var parsed = ProcessTree.fromJson(tree.toJson());
        var malformed = new LinkedHashMap<>(tree.toMap());
        malformed.put("queued_revision", Map.of("turn_id", planId));

        assertEquals(revision, parsed.queuedRevision());
        assertThrows(JsonFormatException.class, () -> ProcessTree.fromMap(malformed));
    }

    @Nested
    @DisplayName("Reading a sparse tree")
    class SparseTree {

        private static final ProcessTokens NONE_YET = new ProcessTokens(0L, 0L, 0L, false, 0, 0, true, false);

        @Test
        @DisplayName("should fill every absent member of a tree that carries only its plan id with the defaults")
        void shouldDefaultEveryAbsentMember() throws Exception {
            var planId = Generators.nonBlankStrings().next();

            var parsed = ProcessTree.fromMap(Map.of("plan_id", planId));

            assertEquals(planId, parsed.title());
            assertEquals(new ProcessProjectRef("project", "project", null), parsed.project());
            assertNull(parsed.epic());
            assertEquals("execute", parsed.currentPhase());
            assertEquals("running", parsed.phaseState());
            assertNull(parsed.activity());
            assertFalse(parsed.needsOperator());
            assertEquals(1, parsed.instanceVersion());
            assertEquals(NONE_YET, parsed.tokensSoFar());
            assertTrue(parsed.nodes().isEmpty());
            assertTrue(parsed.currentPositionPath().isEmpty());
            assertNull(parsed.pullRequestNumber());
            assertNull(parsed.pullRequestUrl());
            assertEquals(0L, parsed.durationSeconds());
        }

        @Test
        @DisplayName("should take the plan id from the member id when plan_id is absent")
        void shouldReadPlanIdFromId() throws Exception {
            var planId = Generators.nonBlankStrings().next();

            assertEquals(planId, ProcessTree.fromMap(Map.of("id", planId)).planId());
        }

        @Test
        @DisplayName("should refuse a tree without plan_id and without id")
        void shouldRefuseTreeWithoutPlanId() {
            var title = Generators.nonBlankStrings().next();

            var map = Map.<String, Object>of("title", title);

            assertThrows(JsonFormatException.class, () -> ProcessTree.fromMap(map));
        }

        @Test
        @DisplayName("should refuse JSON that is no object")
        void shouldRefuseJsonThatIsNoObject() {
            var planId = Generators.nonBlankStrings().next();

            var json = JsonTree.write(List.of(planId));

            assertThrows(JsonFormatException.class, () -> ProcessTree.fromJson(json));
        }

        @Test
        @DisplayName("should build project and epic from the flat members when their objects are absent")
        void shouldReadFlatProjectAndEpic() throws Exception {
            var planId = Generators.nonBlankStrings().next();
            var projectId = Generators.nonBlankStrings().next();
            var projectName = Generators.nonBlankStrings().next();
            var epicId = Generators.nonBlankStrings().next();
            var epicTitle = Generators.nonBlankStrings().next();

            var named = ProcessTree.fromMap(Map.of("plan_id", planId, "project_id", projectId,
                    "project_name", projectName, "epic_id", epicId, "epic_title", epicTitle));
            var bare = ProcessTree.fromMap(Map.of("plan_id", planId, "project_id", projectId, "epic_id", epicId));

            assertEquals(new ProcessProjectRef(projectId, projectName, null), named.project());
            assertEquals(new ProcessEpicRef(epicId, epicTitle, null), named.epic());
            // the flat project without a name takes its id as name, as the project object does
            assertEquals(projectId, bare.project().displayName());
            assertEquals(new ProcessEpicRef(epicId, epicId, null), bare.epic());
        }

        @Test
        @DisplayName("should skip a node that is no object and a null entry of the position path")
        void shouldSkipUnusableListEntries() throws Exception {
            var planId = Generators.nonBlankStrings().next();
            var step = Generators.nonBlankStrings().next();
            var path = new ArrayList<Object>();
            path.add(null);
            path.add(step);

            var parsed = ProcessTree.fromMap(Map.of("plan_id", planId, "nodes", List.of(step),
                    "current_position_path", path));
            var notLists = ProcessTree.fromMap(Map.of("plan_id", planId, "nodes", step,
                    "current_position_path", step));

            assertTrue(parsed.nodes().isEmpty());
            assertEquals(List.of(step), parsed.currentPositionPath());
            assertTrue(notLists.nodes().isEmpty());
            assertTrue(notLists.currentPositionPath().isEmpty());
        }

        @Test
        @DisplayName("should write needs_operator only for a tree that needs the operator and read it back")
        void shouldRoundTripNeedsOperator() throws Exception {
            var planId = Generators.nonBlankStrings().next();
            var project = new ProcessProjectRef(planId, planId, null);
            var needing = new ProcessTree(planId, null, project, null, null, null, null, true, 1, null, null,
                    null, null, null, 0L, null, null);
            var quiet = new ProcessTree(planId, null, project, null, null, null, null, false, 1, null, null,
                    null, null, null, 0L, null, null);

            var parsed = ProcessTree.fromJson(needing.toJson());

            assertEquals(Boolean.TRUE, needing.toMap().get("needs_operator"));
            assertFalse(quiet.toMap().containsKey("needs_operator"));
            assertTrue(parsed.needsOperator());
            assertEquals(planId, needing.title());
            assertEquals(NONE_YET, needing.tokensSoFar());
            assertTrue(needing.nodes().isEmpty());
            assertTrue(needing.currentPositionPath().isEmpty());
        }
    }

    @Test
    @DisplayName("should serialize and deserialize TaskActivity with full entry types")
    void shouldRoundTripTaskActivity() throws Exception {
        // Arrange
        var taskId = Generators.nonBlankStrings().next();
        var harness = Generators.nonBlankStrings().next();
        var content = Generators.nonBlankStrings().next();
        var toolSize = Generators.nonBlankStrings().next();
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
        var epicId = Generators.nonBlankStrings().next();
        var project = Generators.nonBlankStrings().next();
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
        var planId = Generators.nonBlankStrings().next();
        var taskId = Generators.nonBlankStrings().next();
        var run = Generators.integers(0, 100).next();
        var leftSeconds = (long) Generators.integers(0, 100_000).next();
        var returnedSeconds = (long) Generators.integers(0, 100_000).next();
        var instanceVersion = Generators.integers(0, 100).next();
        var activityContent = Generators.nonBlankStrings().next();
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
        var toolTarget = Generators.nonBlankStrings().next();
        var entry1 = new ActivityEntry("10:00:00", "tool", "read", toolTarget, false, null, "1 KiB", List.of(),
                List.of());
        var map1 = entry1.toMap();
        assertEquals("tool", map1.get("kind"));
        assertEquals("read", map1.get("tool_name"));
        assertEquals(toolTarget, map1.get("tool_target"));
        var restored1 = ActivityEntry.fromMap(map1);
        assertEquals(entry1, restored1);

        var content = Generators.nonBlankStrings().next();
        var files = List.of(Generators.nonBlankStrings().next(), Generators.nonBlankStrings().next());
        var questions = List.of(Generators.nonBlankStrings().next());
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
        var waitsFor = Generators.nonBlankStrings().next();
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
        var projectId = Generators.nonBlankStrings().next();
        var projectName = Generators.nonBlankStrings().next();
        var proj = new ProcessProjectRef(projectId, projectName, "/api/v1/projects/" + projectId);
        assertEquals(proj, ProcessProjectRef.fromJson(proj.toJson()));

        var epicId = Generators.nonBlankStrings().next();
        var epicTitle = Generators.nonBlankStrings().next();
        var epic = new ProcessEpicRef(epicId, epicTitle, "/api/v1/epics/" + epicId + "/process");
        assertEquals(epic, ProcessEpicRef.fromJson(epic.toJson()));

        var offTrack = new OffTrackInfo("stalled", "restart", 3, 3);
        assertEquals(offTrack, OffTrackInfo.fromJson(offTrack.toJson()));
    }
}
