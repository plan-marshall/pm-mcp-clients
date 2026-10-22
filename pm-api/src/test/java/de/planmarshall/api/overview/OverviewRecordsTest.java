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

import de.planmarshall.api.json.JsonFormatException;

@DisplayName("OverviewRecordsTest")
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
        var landing = new AlongsideLanding("remediation.ci-failure", "failed", null, "unit", "landing_failed");
        var map = landing.toMap();
        var parsed = AlongsideLanding.fromMap(map);

        assertEquals("remediation.ci-failure", parsed.unit());
        assertEquals("failed", parsed.state());
        assertEquals("unit", parsed.kind());
        assertEquals("landing_failed", parsed.holdKind());

        var removal = new AlongsideLanding(null, "running", 2, "removal", null);
        var parsedRemoval = AlongsideLanding.fromMap(removal.toMap());
        assertEquals(2, parsedRemoval.count());
        assertEquals("removal", parsedRemoval.kind());

        var landingJson = landing.toJson();
        var parsedFromJson = AlongsideLanding.fromJson(landingJson);
        assertEquals(landing.unit(), parsedFromJson.unit());
        assertEquals(landing.holdKind(), parsedFromJson.holdKind());
    }

    @Test
    @DisplayName("round-trip serializes ProjectSummary and ProjectOverviewResponse")
    void projectSummaryAndOverviewResponse() throws Exception {
        var now = Instant.parse("2026-10-16T14:00:00Z");
        var summary = new ProjectSummary(
                "pm-mcp-clients",
                "PM Clients",
                3,
                1,
                2,
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
        assertEquals("pm-mcp-clients", parsedResp.projects().getFirst().projectId());

        // writeList and parseList
        var listJson = ProjectSummary.writeList(List.of(summary));
        var parsedList = ProjectSummary.parseList(listJson);
        assertEquals(1, parsedList.size());
        assertEquals("pm-mcp-clients", parsedList.getFirst().projectId());
    }

    @Test
    @DisplayName("round-trip serializes ActivePlanSummary")
    void activePlanSummary() throws Exception {
        var entered = Instant.parse("2026-10-16T13:45:00Z");
        var running = Instant.parse("2026-10-16T11:50:00Z");

        var plan = new ActivePlanSummary(
                "PLN-001",
                "Overview resource of the runtime",
                "execute",
                "running",
                null,
                entered,
                running,
                7,
                12,
                "EPC-001",
                "client work",
                214,
                "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                "repeating",
                "decision",
                1
        );

        var json = plan.toJson();
        var parsed = ActivePlanSummary.fromJson(json);

        assertEquals(plan.planId(), parsed.planId());
        assertEquals(plan.title(), parsed.title());
        assertEquals(plan.currentPhase(), parsed.currentPhase());
        assertEquals(plan.phaseState(), parsed.phaseState());
        assertEquals(entered, parsed.phaseEnteredAt());
        assertEquals(running, parsed.runningSince());
        assertEquals(7, parsed.tasksDone());
        assertEquals(12, parsed.tasksTotal());
        assertEquals("EPC-001", parsed.epicId());
        assertEquals(214, parsed.pullRequestNumber());
        assertEquals("repeating", parsed.offTrackSignal());
        assertEquals("decision", parsed.needsOperatorKind());
        assertEquals(1, parsed.needsOperatorCount());
    }

    @Test
    @DisplayName("round-trip serializes ActiveEpicSummary and ProjectDetailResponse")
    void activeEpicAndProjectDetail() throws Exception {
        var running = Instant.parse("2026-10-13T10:00:00Z");
        var plan = new ActivePlanSummary(
                "PLN-002",
                "Message catalogue",
                "outline",
                "pausing",
                "task",
                null,
                null,
                0,
                0,
                "EPC-001",
                "client work",
                null,
                null,
                null,
                null,
                null
        );

        var epic = new ActiveEpicSummary(
                "EPC-001",
                "client work",
                running,
                2,
                3,
                4,
                9,
                "decision",
                1,
                List.of(plan)
        );

        var epicJson = epic.toJson();
        var parsedEpic = ActiveEpicSummary.fromJson(epicJson);
        assertEquals("EPC-001", parsedEpic.epicId());
        assertEquals(2, parsedEpic.runningPlansCount());
        assertEquals(1, parsedEpic.plans().size());

        var landing = new AlongsideLanding("remediation.ci-failure", "failed", null, "unit", "landing_failed");
        var detail = new ProjectDetailResponse(
                "pm-mcp-clients",
                "pm-mcp-clients",
                List.of(plan),
                List.of(epic),
                List.of(landing)
        );

        var detailJson = detail.toJson();
        var parsedDetail = ProjectDetailResponse.fromJson(detailJson);

        assertEquals("pm-mcp-clients", parsedDetail.projectId());
        assertEquals(1, parsedDetail.plans().size());
        assertEquals(1, parsedDetail.epics().size());
        assertEquals(1, parsedDetail.alongsideLandings().size());
    }

    @Test
    @DisplayName("round-trip serializes EndedWorkItem and EndedWorkPage")
    void endedWork() throws Exception {
        var ended = Instant.parse("2026-10-15T11:20:00Z");
        var item = new EndedWorkItem(
                "fix-relay",
                "plan",
                "Fix the relay",
                "merged",
                null,
                214,
                "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                "All tests passed",
                ended
        );

        var json = item.toJson();
        var parsed = EndedWorkItem.fromJson(json);

        assertEquals("fix-relay", parsed.id());
        assertEquals("plan", parsed.kind());
        assertEquals("merged", parsed.outcome());
        assertEquals(214, parsed.pullRequestNumber());
        assertEquals("All tests passed", parsed.reason());
        assertEquals(ended, parsed.endedAt());

        var page = new EndedWorkPage("pm-mcp-clients", List.of(item), 1, 3, false, true);
        var pageJson = page.toJson();
        var parsedPage = EndedWorkPage.fromJson(pageJson);

        assertEquals("pm-mcp-clients", parsedPage.projectId());
        assertEquals(1, parsedPage.items().size());
        assertEquals(1, parsedPage.page());
        assertEquals(3, parsedPage.totalPages());
        assertFalse(parsedPage.hasNewer());
        assertTrue(parsedPage.hasOlder());
    }

    @Test
    @DisplayName("round-trip serializes overview SSE events")
    void overviewEvents() throws Exception {
        var pChanged = new ProjectWorkChangedEvent("pm-mcp-clients", "work_started", 3, 1, 2);
        assertEquals("project.changed", ProjectWorkChangedEvent.EVENT_CHANGED);
        var parsedPChanged = ProjectWorkChangedEvent.fromJson(pChanged.toJson());
        assertEquals("pm-mcp-clients", parsedPChanged.projectId());
        assertEquals("work_started", parsedPChanged.changeType());
        assertEquals(3, parsedPChanged.activePlansCount());
        assertEquals(1, parsedPChanged.activeEpicsCount());
        assertEquals(2, parsedPChanged.needsOperatorCount());

        var now = Instant.now();
        var phChanged = new PhaseChangedEvent("PLN-001", "outline", "execute", now);
        assertEquals("phase.changed", PhaseChangedEvent.EVENT_NAME);
        var parsedPhChanged = PhaseChangedEvent.fromJson(phChanged.toJson());
        assertEquals("PLN-001", parsedPhChanged.planId());
        assertEquals("outline", parsedPhChanged.oldPhase());
        assertEquals("execute", parsedPhChanged.newPhase());
        assertNotNull(parsedPhChanged.phaseEnteredAt());

        var actChanged = new ActivityChangedEvent("PLN-001", "pausing", "task");
        assertEquals("activity.changed", ActivityChangedEvent.EVENT_NAME);
        var parsedActChanged = ActivityChangedEvent.fromJson(actChanged.toJson());
        assertEquals("PLN-001", parsedActChanged.planId());
        assertEquals("pausing", parsedActChanged.activity());
        assertEquals("task", parsedActChanged.point());

        var needsChanged = new NeedsOperatorChangedEvent("pm-mcp-clients", "PLN-001", null, "decision", 1);
        assertEquals("needs_operator.changed", NeedsOperatorChangedEvent.EVENT_NAME);
        var parsedNeedsChanged = NeedsOperatorChangedEvent.fromJson(needsChanged.toJson());
        assertEquals("pm-mcp-clients", parsedNeedsChanged.projectId());
        assertEquals("PLN-001", parsedNeedsChanged.planId());
        assertNull(parsedNeedsChanged.epicId());
        assertEquals("decision", parsedNeedsChanged.kind());
        assertEquals(1, parsedNeedsChanged.count());

        var offTrack = new TaskOffTrackEvent("PLN-001", "TSK-003", "repeating", true);
        assertEquals("task.off_track", TaskOffTrackEvent.EVENT_NAME);
        var parsedOffTrack = TaskOffTrackEvent.fromJson(offTrack.toJson());
        assertEquals("PLN-001", parsedOffTrack.planId());
        assertEquals("TSK-003", parsedOffTrack.taskId());
        assertEquals("repeating", parsedOffTrack.signal());
        assertTrue(parsedOffTrack.offTrack());
    }

    @Test
    @DisplayName("tests fallback fields and parsing from arrays")
    void recordAliasesAndFallbacks() throws Exception {
        // ProjectSummary aliases (id, name, plans_count, epics_count, setupNeedsOperator)
        var pSummaryMap = Map.<String, Object>of(
                "id", "prj-alias",
                "name", "Project Alias",
                "plans_count", 5,
                "epics_count", 2,
                "setupNeedsOperator", true
        );
        var parsedSummary = ProjectSummary.fromMap(pSummaryMap);
        assertEquals("prj-alias", parsedSummary.projectId());
        assertEquals("Project Alias", parsedSummary.displayName());
        assertEquals(5, parsedSummary.activePlansCount());
        assertEquals(2, parsedSummary.activeEpicsCount());
        assertTrue(parsedSummary.setupNeedsOperator());

        // Null display name defaults to projectId
        var nullNameSummary = new ProjectSummary("raw-id", null, 0, 0, 0, null, false, null);
        assertEquals("raw-id", nullNameSummary.displayName());

        // ProjectOverviewResponse from array
        var arrayResponse = ProjectOverviewResponse.fromJson("[{\"id\":\"p-arr\",\"name\":\"Arr Prj\"}]");
        assertEquals(1, arrayResponse.projects().size());
        assertEquals("p-arr", arrayResponse.projects().getFirst().projectId());

        // ActivePlanSummary aliases (id, phase, state, created_at, off_track, needs_operator)
        var planAliasMap = Map.<String, Object>of(
                "id", "pln-alias",
                "title", "Alias Plan",
                "phase", "finalize",
                "state", "paused",
                "created_at", "2026-10-16T12:00:00Z",
                "off_track", "timeout",
                "needs_operator", "stalled",
                "needs_operator_count", 1
        );
        var parsedPlan = ActivePlanSummary.fromMap(planAliasMap);
        assertEquals("pln-alias", parsedPlan.planId());
        assertEquals("finalize", parsedPlan.currentPhase());
        assertEquals("paused", parsedPlan.phaseState());
        assertEquals("timeout", parsedPlan.offTrackSignal());
        assertEquals("stalled", parsedPlan.needsOperatorKind());

        // ActiveEpicSummary aliases (id, created_at, needs_operator)
        var epicAliasMap = Map.<String, Object>of(
                "id", "epc-alias",
                "title", "Alias Epic",
                "created_at", "2026-10-10T08:00:00Z",
                "needs_operator", "decision"
        );
        var parsedEpic = ActiveEpicSummary.fromMap(epicAliasMap);
        assertEquals("epc-alias", parsedEpic.epicId());
        assertEquals("decision", parsedEpic.needsOperatorKind());
        assertTrue(parsedEpic.plans().isEmpty());

        // EndedWorkPage from array
        var endedArrayJson = "[{\"id\":\"pln-ended-arr\",\"kind\":\"plan\",\"outcome\":\"merged\",\"ended_at\":\"2026-10-16T09:00:00Z\"}]";
        var parsedEndedPage = EndedWorkPage.fromJson(endedArrayJson);
        assertEquals("unknown", parsedEndedPage.projectId());
        assertEquals(1, parsedEndedPage.items().size());
        assertEquals("pln-ended-arr", parsedEndedPage.items().getFirst().id());

        // EndedWorkItem terminal_reason alias
        var endedItemMap = Map.<String, Object>of(
                "id", "epc-ended",
                "kind", "epic",
                "title", "Ended Epic",
                "terminal_reason", "completed",
                "ended_at", "2026-10-16T08:00:00Z"
        );
        var parsedItem = EndedWorkItem.fromMap(endedItemMap);
        assertEquals("epc-ended", parsedItem.id());
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
