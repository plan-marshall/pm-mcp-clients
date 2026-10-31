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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;

@DisplayName("OverviewRecordsTest")
@EnableGeneratorController
class OverviewRecordsTest {

    @Test
    @DisplayName("validates closed enumerations and wire parsing")
    void closedEnumerations() {
        assertAll(
                () -> assertEquals(NeedsOperatorKind.DECISION, NeedsOperatorKind.parse("decision")),
                () -> assertEquals(NeedsOperatorKind.INTEGRITY_PAUSE, NeedsOperatorKind.parse("integrity_pause")),
                () -> assertEquals(NeedsOperatorKind.LANDING_FAILED, NeedsOperatorKind.fromValue("landing_failed")),
                () -> assertNull(NeedsOperatorKind.fromValue("unknown_kind")),
                () -> assertEquals(PlanActivity.RUNNING, PlanActivity.parse("running")),
                () -> assertEquals(PlanActivity.WAITING_FOR_YOU, PlanActivity.parse("waiting_for_you")),
                () -> assertNull(PlanActivity.fromValue(null)),
                () -> assertEquals(EndedOutcome.MERGED, EndedOutcome.parse("merged")),
                () -> assertEquals(EndedOutcome.ABORTED, EndedOutcome.parse("aborted")),
                () -> assertEquals(EpicTerminalReason.COMPLETED, EpicTerminalReason.parse("completed")),
                () -> assertEquals(EpicTerminalReason.CANCELLED, EpicTerminalReason.parse("cancelled"))
        );
    }

    @Test
    @DisplayName("validates all enumeration values and wire round-trip")
    void allEnumerationValues() {
        for (NeedsOperatorKind kind : NeedsOperatorKind.values()) {
            assertNotNull(kind.value());
            assertEquals(kind, NeedsOperatorKind.fromValue(kind.value()));
            assertEquals(kind, NeedsOperatorKind.parse(kind.value()));
        }
        for (PlanActivity act : PlanActivity.values()) {
            assertNotNull(act.value());
            assertEquals(act, PlanActivity.fromValue(act.value()));
            assertEquals(act, PlanActivity.parse(act.value()));
        }
        for (EndedOutcome outcome : EndedOutcome.values()) {
            assertNotNull(outcome.value());
            assertEquals(outcome, EndedOutcome.fromValue(outcome.value()));
            assertEquals(outcome, EndedOutcome.parse(outcome.value()));
        }
        for (EpicTerminalReason reason : EpicTerminalReason.values()) {
            assertNotNull(reason.value());
            assertEquals(reason, EpicTerminalReason.fromValue(reason.value()));
            assertEquals(reason, EpicTerminalReason.parse(reason.value()));
        }
    }

    @Test
    @DisplayName("round-trip serializes AlongsideLanding")
    void alongsideLanding() throws Exception {
        var unit = Generators.nonEmptyStrings().next();
        var landing = new AlongsideLanding(unit, "failed", null, "unit", "landing_failed");
        var map = landing.toMap();
        var parsed = AlongsideLanding.fromMap(map);

        assertEquals(unit, parsed.unit());
        assertEquals("failed", parsed.state());
        assertEquals("unit", parsed.kind());
        assertEquals("landing_failed", parsed.holdKind());

        var count = Generators.integers(0, 100).next();
        var removal = new AlongsideLanding(null, "running", count, "removal", null);
        var parsedRemoval = AlongsideLanding.fromMap(removal.toMap());
        assertEquals(count, parsedRemoval.count());
        assertEquals("removal", parsedRemoval.kind());

        var landingJson = landing.toJson();
        var parsedFromJson = AlongsideLanding.fromJson(landingJson);
        assertEquals(landing.unit(), parsedFromJson.unit());
        assertEquals(landing.holdKind(), parsedFromJson.holdKind());
    }

    @Test
    @DisplayName("round-trip serializes ProjectSummary and ProjectOverviewResponse")
    void projectSummaryAndOverviewResponse() throws Exception {
        var now = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var projectId = Generators.nonEmptyStrings().next();
        var displayName = Generators.nonEmptyStrings().next();
        var activePlansCount = Generators.integers(0, 100).next();
        var activeEpicsCount = Generators.integers(0, 100).next();
        var needsOperatorCount = Generators.integers(0, 100).next();
        var summary = new ProjectSummary(
                projectId,
                displayName,
                activePlansCount,
                activeEpicsCount,
                needsOperatorCount,
                "decision",
                false,
                now
        );

        var json = summary.toJson();
        var parsed = ProjectSummary.fromJson(json);

        assertEquals(summary.projectId(), parsed.projectId());
        assertEquals(summary.displayName(), parsed.displayName());
        assertEquals(summary.activePlansCount(), parsed.activePlansCount());
        assertEquals(summary.activeEpicsCount(), parsed.activeEpicsCount());
        assertEquals(summary.needsOperatorCount(), parsed.needsOperatorCount());
        assertEquals(summary.needsOperatorSummary(), parsed.needsOperatorSummary());
        assertFalse(parsed.setupNeedsOperator());
        assertEquals(now, parsed.lastActivityAt());

        var response = new ProjectOverviewResponse(List.of(summary));
        var respJson = response.toJson();
        var parsedResp = ProjectOverviewResponse.fromJson(respJson);

        assertEquals(1, parsedResp.projects().size());
        assertEquals(projectId, parsedResp.projects().getFirst().projectId());

        // writeList and parseList
        var listJson = ProjectSummary.writeList(List.of(summary));
        var parsedList = ProjectSummary.parseList(listJson);
        assertEquals(1, parsedList.size());
        assertEquals(projectId, parsedList.getFirst().projectId());
    }

    @Test
    @DisplayName("round-trip serializes ActivePlanSummary")
    void activePlanSummary() throws Exception {
        var entered = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var running = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var planId = Generators.nonEmptyStrings().next();
        var title = Generators.nonEmptyStrings().next();
        var epicId = Generators.nonEmptyStrings().next();
        var epicTitle = Generators.nonEmptyStrings().next();
        var tasksDone = Generators.integers(0, 50).next();
        var tasksTotal = Generators.integers(51, 100).next();
        var pullRequestNumber = Generators.integers(1, 1000).next();
        var needsOperatorCount = Generators.integers(0, 10).next();

        var plan = new ActivePlanSummary(
                planId,
                title,
                "execute",
                "running",
                null,
                entered,
                running,
                tasksDone,
                tasksTotal,
                epicId,
                epicTitle,
                pullRequestNumber,
                "https://github.com/plan-marshall/pm-mcp-clients/pull/" + pullRequestNumber,
                "repeating",
                "decision",
                needsOperatorCount
        );

        var json = plan.toJson();
        var parsed = ActivePlanSummary.fromJson(json);

        assertEquals(plan.planId(), parsed.planId());
        assertEquals(plan.title(), parsed.title());
        assertEquals(plan.currentPhase(), parsed.currentPhase());
        assertEquals(plan.phaseState(), parsed.phaseState());
        assertEquals(entered, parsed.phaseEnteredAt());
        assertEquals(running, parsed.runningSince());
        assertEquals(tasksDone, parsed.tasksDone());
        assertEquals(tasksTotal, parsed.tasksTotal());
        assertEquals(epicId, parsed.epicId());
        assertEquals(pullRequestNumber, parsed.pullRequestNumber());
        assertEquals("repeating", parsed.offTrackSignal());
        assertEquals("decision", parsed.needsOperatorKind());
        assertEquals(needsOperatorCount, parsed.needsOperatorCount());
    }

    @Test
    @DisplayName("round-trip serializes ActiveEpicSummary and ProjectDetailResponse")
    void activeEpicAndProjectDetail() throws Exception {
        var running = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var planId = Generators.nonEmptyStrings().next();
        var planTitle = Generators.nonEmptyStrings().next();
        var epicId = Generators.nonEmptyStrings().next();
        var epicTitle = Generators.nonEmptyStrings().next();
        var runningPlansCount = Generators.integers(0, 100).next();
        var plan = new ActivePlanSummary(
                planId,
                planTitle,
                "outline",
                "pausing",
                "task",
                null,
                null,
                0,
                0,
                epicId,
                epicTitle,
                null,
                null,
                null,
                null,
                null
        );

        var epic = new ActiveEpicSummary(
                epicId,
                epicTitle,
                running,
                runningPlansCount,
                3,
                4,
                9,
                "decision",
                1,
                List.of(plan)
        );

        var epicJson = epic.toJson();
        var parsedEpic = ActiveEpicSummary.fromJson(epicJson);
        assertEquals(epicId, parsedEpic.epicId());
        assertEquals(runningPlansCount, parsedEpic.runningPlansCount());
        assertEquals(1, parsedEpic.plans().size());

        var unit = Generators.nonEmptyStrings().next();
        var landing = new AlongsideLanding(unit, "failed", null, "unit", "landing_failed");
        var projectId = Generators.nonEmptyStrings().next();
        var detail = new ProjectDetailResponse(
                projectId,
                projectId,
                List.of(plan),
                List.of(epic),
                List.of(landing)
        );

        var detailJson = detail.toJson();
        var parsedDetail = ProjectDetailResponse.fromJson(detailJson);

        assertEquals(projectId, parsedDetail.projectId());
        assertEquals(1, parsedDetail.plans().size());
        assertEquals(1, parsedDetail.epics().size());
        assertEquals(1, parsedDetail.alongsideLandings().size());
    }

    @Test
    @DisplayName("round-trip serializes EndedWorkItem and EndedWorkPage")
    void endedWork() throws Exception {
        var ended = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var itemId = Generators.nonEmptyStrings().next();
        var title = Generators.nonEmptyStrings().next();
        var pullRequestNumber = Generators.integers(1, 1000).next();
        var reason = Generators.nonEmptyStrings().next();
        var item = new EndedWorkItem(
                itemId,
                "plan",
                title,
                "merged",
                null,
                pullRequestNumber,
                "https://github.com/plan-marshall/pm-mcp-clients/pull/" + pullRequestNumber,
                reason,
                ended
        );

        var json = item.toJson();
        var parsed = EndedWorkItem.fromJson(json);

        assertEquals(itemId, parsed.id());
        assertEquals("plan", parsed.kind());
        assertEquals("merged", parsed.outcome());
        assertEquals(pullRequestNumber, parsed.pullRequestNumber());
        assertEquals(reason, parsed.reason());
        assertEquals(ended, parsed.endedAt());

        var projectId = Generators.nonEmptyStrings().next();
        var page = new EndedWorkPage(projectId, List.of(item), 1, 3, false, true);
        var pageJson = page.toJson();
        var parsedPage = EndedWorkPage.fromJson(pageJson);

        assertEquals(projectId, parsedPage.projectId());
        assertEquals(1, parsedPage.items().size());
        assertEquals(1, parsedPage.page());
        assertEquals(3, parsedPage.totalPages());
        assertFalse(parsedPage.hasNewer());
        assertTrue(parsedPage.hasOlder());
    }

    @Test
    @DisplayName("round-trip serializes overview SSE events")
    void overviewEvents() throws Exception {
        var projectId = Generators.nonEmptyStrings().next();
        var activePlansCount = Generators.integers(0, 100).next();
        var activeEpicsCount = Generators.integers(0, 100).next();
        var needsOperatorCount = Generators.integers(0, 100).next();
        var pChanged = new ProjectWorkChangedEvent(projectId, "work_started", activePlansCount,
                activeEpicsCount, needsOperatorCount);
        assertEquals("project.changed", ProjectWorkChangedEvent.EVENT_CHANGED);
        var parsedPChanged = ProjectWorkChangedEvent.fromJson(pChanged.toJson());
        assertEquals(projectId, parsedPChanged.projectId());
        assertEquals("work_started", parsedPChanged.changeType());
        assertEquals(activePlansCount, parsedPChanged.activePlansCount());
        assertEquals(activeEpicsCount, parsedPChanged.activeEpicsCount());
        assertEquals(needsOperatorCount, parsedPChanged.needsOperatorCount());

        var phaseEnteredAt = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var planId = Generators.nonEmptyStrings().next();
        var phChanged = new PhaseChangedEvent(planId, "outline", "execute", phaseEnteredAt);
        assertEquals("phase.changed", PhaseChangedEvent.EVENT_NAME);
        var parsedPhChanged = PhaseChangedEvent.fromJson(phChanged.toJson());
        assertEquals(planId, parsedPhChanged.planId());
        assertEquals("outline", parsedPhChanged.oldPhase());
        assertEquals("execute", parsedPhChanged.newPhase());
        assertNotNull(parsedPhChanged.phaseEnteredAt());

        var actChanged = new ActivityChangedEvent(planId, "pausing", "task");
        assertEquals("activity.changed", ActivityChangedEvent.EVENT_NAME);
        var parsedActChanged = ActivityChangedEvent.fromJson(actChanged.toJson());
        assertEquals(planId, parsedActChanged.planId());
        assertEquals("pausing", parsedActChanged.activity());
        assertEquals("task", parsedActChanged.point());

        var needsCount = Generators.integers(0, 100).next();
        var needsChanged = new NeedsOperatorChangedEvent(projectId, planId, null, "decision", needsCount);
        assertEquals("needs_operator.changed", NeedsOperatorChangedEvent.EVENT_NAME);
        var parsedNeedsChanged = NeedsOperatorChangedEvent.fromJson(needsChanged.toJson());
        assertEquals(projectId, parsedNeedsChanged.projectId());
        assertEquals(planId, parsedNeedsChanged.planId());
        assertNull(parsedNeedsChanged.epicId());
        assertEquals("decision", parsedNeedsChanged.kind());
        assertEquals(needsCount, parsedNeedsChanged.count());

        var taskId = Generators.nonEmptyStrings().next();
        var offTrack = new TaskOffTrackEvent(planId, taskId, "repeating", true);
        assertEquals("task.off_track", TaskOffTrackEvent.EVENT_NAME);
        var parsedOffTrack = TaskOffTrackEvent.fromJson(offTrack.toJson());
        assertEquals(planId, parsedOffTrack.planId());
        assertEquals(taskId, parsedOffTrack.taskId());
        assertEquals("repeating", parsedOffTrack.signal());
        assertTrue(parsedOffTrack.offTrack());
    }

    @Test
    @DisplayName("tests fallback fields and parsing from arrays")
    void recordAliasesAndFallbacks() throws Exception {
        // ProjectSummary aliases (id, name, plans_count, epics_count, setupNeedsOperator)
        var projectId = Generators.nonEmptyStrings().next();
        var displayName = Generators.nonEmptyStrings().next();
        var plansCount = Generators.integers(0, 100).next();
        var epicsCount = Generators.integers(0, 100).next();
        var pSummaryMap = Map.<String, Object>of(
                "id", projectId,
                "name", displayName,
                "plans_count", plansCount,
                "epics_count", epicsCount,
                "setupNeedsOperator", true
        );
        var parsedSummary = ProjectSummary.fromMap(pSummaryMap);
        assertEquals(projectId, parsedSummary.projectId());
        assertEquals(displayName, parsedSummary.displayName());
        assertEquals(plansCount, parsedSummary.activePlansCount());
        assertEquals(epicsCount, parsedSummary.activeEpicsCount());
        assertTrue(parsedSummary.setupNeedsOperator());

        // Null display name defaults to projectId
        var rawId = Generators.nonEmptyStrings().next();
        var nullNameSummary = new ProjectSummary(rawId, null, 0, 0, 0, null, false, null);
        assertEquals(rawId, nullNameSummary.displayName());

        // ProjectOverviewResponse from array
        var arrayResponse = ProjectOverviewResponse.fromJson("[{\"id\":\"p-arr\",\"name\":\"Arr Prj\"}]");
        assertEquals(1, arrayResponse.projects().size());
        assertEquals("p-arr", arrayResponse.projects().getFirst().projectId());

        // ActivePlanSummary aliases (id, phase, state, created_at, off_track, needs_operator)
        var planAliasId = Generators.nonEmptyStrings().next();
        var planAliasTitle = Generators.nonEmptyStrings().next();
        var planAliasMap = Map.<String, Object>of(
                "id", planAliasId,
                "title", planAliasTitle,
                "phase", "finalize",
                "state", "paused",
                "created_at", "2026-10-16T12:00:00Z",
                "off_track", "timeout",
                "needs_operator", "stalled",
                "needs_operator_count", 1
        );
        var parsedPlan = ActivePlanSummary.fromMap(planAliasMap);
        assertEquals(planAliasId, parsedPlan.planId());
        assertEquals("finalize", parsedPlan.currentPhase());
        assertEquals("paused", parsedPlan.phaseState());
        assertEquals("timeout", parsedPlan.offTrackSignal());
        assertEquals("stalled", parsedPlan.needsOperatorKind());

        // ActiveEpicSummary aliases (id, created_at, needs_operator)
        var epicAliasId = Generators.nonEmptyStrings().next();
        var epicAliasTitle = Generators.nonEmptyStrings().next();
        var epicAliasMap = Map.<String, Object>of(
                "id", epicAliasId,
                "title", epicAliasTitle,
                "created_at", "2026-10-10T08:00:00Z",
                "needs_operator", "decision"
        );
        var parsedEpic = ActiveEpicSummary.fromMap(epicAliasMap);
        assertEquals(epicAliasId, parsedEpic.epicId());
        assertEquals("decision", parsedEpic.needsOperatorKind());
        assertTrue(parsedEpic.plans().isEmpty());

        // EndedWorkPage from array
        var endedArrayJson = "[{\"id\":\"pln-ended-arr\",\"kind\":\"plan\",\"outcome\":\"merged\",\"ended_at\":\"2026-10-16T09:00:00Z\"}]";
        var parsedEndedPage = EndedWorkPage.fromJson(endedArrayJson);
        assertEquals("unknown", parsedEndedPage.projectId());
        assertEquals(1, parsedEndedPage.items().size());
        assertEquals("pln-ended-arr", parsedEndedPage.items().getFirst().id());

        // EndedWorkItem terminal_reason alias
        var endedItemId = Generators.nonEmptyStrings().next();
        var endedItemTitle = Generators.nonEmptyStrings().next();
        var endedItemMap = Map.<String, Object>of(
                "id", endedItemId,
                "kind", "epic",
                "title", endedItemTitle,
                "terminal_reason", "completed",
                "ended_at", "2026-10-16T08:00:00Z"
        );
        var parsedItem = EndedWorkItem.fromMap(endedItemMap);
        assertEquals(endedItemId, parsedItem.id());
        assertEquals("completed", parsedItem.terminalReason());
        assertNull(parsedItem.outcome());
    }

    @Test
    @DisplayName("validates error conditions on missing required fields or bad JSON")
    void recordValidationErrors() {
        assertThrows(NullPointerException.class, () -> new ProjectSummary(null, "Name", 0, 0, 0, null, false, null));
        assertThrows(NullPointerException.class, () -> new ActivePlanSummary(null, "T", "p", "s", null, null, null, 0, 0, null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class, () -> new ActiveEpicSummary(null, "T", null, 0, 0, 0, 0, null, null, null));
        assertThrows(NullPointerException.class, () -> new ProjectDetailResponse(null, "D", null, null, null));
        assertThrows(NullPointerException.class, () -> new EndedWorkPage(null, null, 1, 1, false, false));
        assertThrows(NullPointerException.class, () -> new EndedWorkItem(null, "plan", "T", "merged", null, null, null, null, null));

        assertThrows(JsonFormatException.class, () -> ProjectSummary.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectSummary.fromMap(Map.of()));
        assertThrows(JsonFormatException.class, () -> ProjectSummary.parseList("{}"));
        assertThrows(JsonFormatException.class, () -> ActivePlanSummary.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ActivePlanSummary.fromMap(Map.of()));
        assertThrows(JsonFormatException.class, () -> ActiveEpicSummary.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ActiveEpicSummary.fromMap(Map.of()));
        assertThrows(JsonFormatException.class, () -> ProjectOverviewResponse.fromJson("\"invalid\""));
        assertThrows(JsonFormatException.class, () -> ProjectDetailResponse.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectDetailResponse.fromMap(Map.of()));
        assertThrows(JsonFormatException.class, () -> EndedWorkPage.fromJson("\"invalid\""));
        assertThrows(JsonFormatException.class, () -> EndedWorkItem.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> EndedWorkItem.fromMap(Map.of()));
        assertThrows(JsonFormatException.class, () -> EndedWorkItem.fromMap(Map.of("id", "item-1")));
        assertThrows(JsonFormatException.class, () -> AlongsideLanding.fromJson("[]"));

        assertThrows(JsonFormatException.class, () -> ProjectWorkChangedEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ProjectWorkChangedEvent.fromMap(Map.of()));
        assertThrows(JsonFormatException.class, () -> PhaseChangedEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> PhaseChangedEvent.fromMap(Map.of()));
        assertThrows(JsonFormatException.class, () -> ActivityChangedEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> ActivityChangedEvent.fromMap(Map.of()));
        assertThrows(JsonFormatException.class, () -> NeedsOperatorChangedEvent.fromJson("[]"));
        assertThrows(NullPointerException.class, () -> NeedsOperatorChangedEvent.fromMap(null));
        assertThrows(JsonFormatException.class, () -> TaskOffTrackEvent.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> TaskOffTrackEvent.fromMap(Map.of()));
    }
}
