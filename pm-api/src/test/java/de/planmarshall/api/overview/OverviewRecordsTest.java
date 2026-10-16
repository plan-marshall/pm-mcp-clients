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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        var parsedPChanged = ProjectWorkChangedEvent.fromJson(pChanged.toJson());
        assertEquals("pm-mcp-clients", parsedPChanged.projectId());
        assertEquals("work_started", parsedPChanged.changeType());
        assertEquals(3, parsedPChanged.activePlansCount());

        var now = Instant.now();
        var phChanged = new PhaseChangedEvent("PLN-001", "outline", "execute", now);
        var parsedPhChanged = PhaseChangedEvent.fromJson(phChanged.toJson());
        assertEquals("PLN-001", parsedPhChanged.planId());
        assertEquals("execute", parsedPhChanged.newPhase());

        var actChanged = new ActivityChangedEvent("PLN-001", "pausing", "task");
        var parsedActChanged = ActivityChangedEvent.fromJson(actChanged.toJson());
        assertEquals("pausing", parsedActChanged.activity());
        assertEquals("task", parsedActChanged.point());

        var needsChanged = new NeedsOperatorChangedEvent("pm-mcp-clients", "PLN-001", null, "decision", 1);
        var parsedNeedsChanged = NeedsOperatorChangedEvent.fromJson(needsChanged.toJson());
        assertEquals("decision", parsedNeedsChanged.kind());
        assertEquals(1, parsedNeedsChanged.count());

        var offTrack = new TaskOffTrackEvent("PLN-001", "TSK-003", "repeating", true);
        var parsedOffTrack = TaskOffTrackEvent.fromJson(offTrack.toJson());
        assertEquals("repeating", parsedOffTrack.signal());
        assertTrue(parsedOffTrack.offTrack());
    }
}
