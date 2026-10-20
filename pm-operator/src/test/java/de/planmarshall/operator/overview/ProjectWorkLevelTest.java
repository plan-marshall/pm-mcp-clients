/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.overview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import de.planmarshall.api.overview.ActiveEpicSummary;
import de.planmarshall.api.overview.ActivePlanSummary;
import de.planmarshall.api.overview.AlongsideLanding;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.operator.tui.TuiLevel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProjectWorkLevel")
class ProjectWorkLevelTest {

    private final Instant baseInstant = Instant.parse("2026-10-20T12:00:00Z");
    private final Clock clock = Clock.fixed(baseInstant, ZoneOffset.UTC);

    @Test
    @DisplayName("empty project work returns non-selectable empty row")
    void emptyWork() {
        var detail = new ProjectDetailResponse("proj-a", "Project Alpha", List.of(), List.of(), List.of());
        var level = new ProjectWorkLevel(detail, false);
        var rows = level.rows("en", clock, false);

        assertEquals(1, rows.size());
        assertEquals(ProjectWorkLevel.EMPTY_PROJECT_ROW_ID, rows.getFirst().id());
        assertFalse(rows.getFirst().openable());
    }

    @Test
    @DisplayName("level title reflects by-plan and by-epic views")
    void levelTitles() {
        var detail = new ProjectDetailResponse("proj-a", "Project Alpha", List.of(), List.of(), List.of());

        var planLevel = new ProjectWorkLevel(detail, false).level("en");
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, planLevel.kind());
        assertTrue(planLevel.title().contains("Project Alpha"));
        assertTrue(planLevel.title().contains("by plan"));

        var epicLevel = new ProjectWorkLevel(detail, true).level("en");
        assertEquals(TuiLevel.Kind.PROJECT_BY_EPIC, epicLevel.kind());
        assertTrue(epicLevel.title().contains("Project Alpha"));
        assertTrue(epicLevel.title().contains("by epic"));
    }

    @Test
    @DisplayName("by-plan rows format plan markers, details, PR and belongs-to epic")
    void byPlanRows() {
        var plan1 = new ActivePlanSummary("pln-1", "Plan One", "execute", "running",
                            null, baseInstant.minusSeconds(300), baseInstant.minusSeconds(1200),
                            2, 5, "epc-1", "Epic Alpha", 42, "https://github.com/org/repo/pull/42", null, null, null);
        var plan2 = new ActivePlanSummary("pln-2", "Plan Two", "refine", "waiting_for_you",
                            null, baseInstant.minusSeconds(100), baseInstant.minusSeconds(600),
                            0, 0, null, null, null, null, null, "decision", 1);

        var detail = new ProjectDetailResponse("proj-a", "Project Alpha", List.of(plan1, plan2), List.of(), List.of());
        var level = new ProjectWorkLevel(detail, false);
        var rows = level.rows("en", clock, false);

        assertEquals(2, rows.size());

        // plan2 needs operator -> should sort first!
        var first = rows.get(0);
        assertEquals("pln-2", first.id());
        assertTrue(first.needsOperator());
        assertTrue(first.line().contains("Plan Two"));
        assertTrue(first.line().contains("refine"));
        assertTrue(first.line().contains("waiting for you"));

        // plan1 has details: duration, epic, tasks, PR
        var second = rows.get(1);
        assertEquals("pln-1", second.id());
        assertFalse(second.needsOperator());
        assertTrue(second.line().contains("Plan One"));
        assertTrue(second.line().contains("execute"));
        assertNotNull(second.details());
        assertTrue(second.details().contains("Epic Alpha"));
        assertTrue(second.details().contains("2 of 5 tasks"));
        assertTrue(second.details().contains("#42"));
        assertFalse(second.details().contains("https://"), "PR URL should be hidden unless help visible");

        // When help is visible, URL appears in details
        var helpRows = level.rows("en", clock, true);
        assertTrue(helpRows.get(1).details().contains("https://github.com/org/repo/pull/42"));
    }

    @Test
    @DisplayName("by-epic rows group child plans and un-epic plans")
    void byEpicRows() {
        var childPlan = new ActivePlanSummary("pln-1", "Child Plan", "execute", "running",
                            null, baseInstant.minusSeconds(60), baseInstant.minusSeconds(600),
                            1, 3, "epc-1", "Epic Alpha", null, null, null, null, null);
        var orphanedPlan = new ActivePlanSummary("pln-2", "Solo Plan", "refine", "running",
                            null, baseInstant.minusSeconds(50), baseInstant.minusSeconds(300),
                            0, 0, null, null, null, null, null, null, null);

        var epic = new ActiveEpicSummary("epc-1", "Epic Alpha", baseInstant.minusSeconds(1800), 1, 0,
                            0, 1, null, null, List.of(childPlan));

        var detail = new ProjectDetailResponse("proj-a", "Project Alpha", List.of(childPlan, orphanedPlan), List.of(epic), List.of());
        var level = new ProjectWorkLevel(detail, true);
        var rows = level.rows("en", clock, false);

        // Expect: 1. Epic row, 2. Child plan row, 3. Header "without epic", 4. Solo plan row
        assertEquals(4, rows.size());

        var epicRow = rows.get(0);
        assertEquals("epc-1", epicRow.id());
        assertTrue(epicRow.line().contains("Epic Alpha"));
        assertTrue(epicRow.line().contains("1 running · 0 staged"));

        var childRow = rows.get(1);
        assertEquals("pln-1", childRow.id());
        assertTrue(childRow.line().contains("└─"));
        assertTrue(childRow.line().contains("Child Plan"));

        var headerRow = rows.get(2);
        assertEquals(ProjectWorkLevel.GROUP_WITHOUT_EPIC_ID, headerRow.id());
        assertFalse(headerRow.openable());

        var soloRow = rows.get(3);
        assertEquals("pln-2", soloRow.id());
        assertTrue(soloRow.line().contains("Solo Plan"));
    }

    @Test
    @DisplayName("alongside landings and setup rows are rendered under alongside header")
    void alongsideAndSetupRows() {
        var landing1 = new AlongsideLanding("auth", "running", null, "unit", null);
        var landing2 = new AlongsideLanding(null, "failed", 2, "removal", "landing_failed");

        var detail = new ProjectDetailResponse("proj-a", "Project Alpha", List.of(), List.of(), List.of(landing1, landing2));
        var level = new ProjectWorkLevel(detail, false, true);
        var rows = level.rows("en", clock, false);

        // Header, landing1, landing2, setup
        assertEquals(4, rows.size());

        assertEquals(ProjectWorkLevel.GROUP_ALONGSIDE_ID, rows.get(0).id());

        var authLanding = rows.get(1);
        assertEquals("landing:auth", authLanding.id());
        assertFalse(authLanding.needsOperator());
        assertTrue(authLanding.line().contains("landing auth · running"));

        var failedRemoval = rows.get(2);
        assertEquals("landing:removal", failedRemoval.id());
        assertTrue(failedRemoval.needsOperator());
        assertTrue(failedRemoval.line().contains("landing failed"));
        assertTrue(failedRemoval.line().contains("2 moved documents"));

        var setupRow = rows.get(3);
        assertEquals("setup:proj-a", setupRow.id());
        assertTrue(setupRow.needsOperator());
        assertTrue(setupRow.line().contains("setup needs you"));
    }
}
