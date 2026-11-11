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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.NeedsOperatorPage;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.api.overview.WorkLocation;
import de.planmarshall.operator.tui.TuiLevel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProjectsOverviewLevel")
class ProjectsOverviewLevelTest {

    private final Instant baseInstant = Instant.parse("2026-10-20T12:00:00Z");
    private final Clock clock = Clock.fixed(baseInstant, ZoneOffset.UTC);

    @Test
    @DisplayName("empty projects list produces non-selectable empty placeholder row")
    void emptyProjects() {
        var level = new ProjectsOverviewLevel(List.of());
        var rows = level.rows("en", clock);

        assertEquals(1, rows.size());
        var row = rows.getFirst();
        assertEquals(ProjectsOverviewLevel.EMPTY_ROW_ID, row.id());
        assertFalse(row.openable());
        assertFalse(row.needsOperator());
        assertTrue(row.line().contains("Nothing runs"));
    }

    @Test
    @DisplayName("level metadata contains localized breadcrumb title")
    void levelMetadata() {
        var enLevel = ProjectsOverviewLevel.level("en");
        assertEquals(TuiLevel.Kind.OVERVIEW, enLevel.kind());
        assertTrue(enLevel.title().contains("Running work"));

        var deLevel = ProjectsOverviewLevel.level("de");
        assertTrue(deLevel.title().contains("Laufende Arbeit"));
    }

    @Test
    @DisplayName("formats project rows with attention markers, summaries and relative times")
    void projectRowsFormatting() {
        var p1 = new ProjectSummary("proj-a", "Project Alpha", 2, 1, 0, null, false, baseInstant.minusSeconds(120));
        var p2 = new ProjectSummary("proj-b", "Project Beta", 1, 0, 2, null, false, baseInstant.minusSeconds(600));
        var p3 = new ProjectSummary("proj-c", "Project Setup", 0, 0, 0, null, true, null);

        var level = new ProjectsOverviewLevel(List.of(p1, p2, p3));
        var rows = level.rows("en", clock);

        assertEquals(3, rows.size());

        // The runtime serves the order; the client keeps it (activity moves no row).
        var first = rows.get(0);
        assertEquals("proj-a", first.id());
        assertFalse(first.needsOperator());
        assertTrue(first.line().contains("Project Alpha"));
        assertTrue(first.line().contains("2 plans"));
        assertTrue(first.line().contains("1 epic"));
        assertTrue(first.line().contains("2 min ago"));

        var second = rows.get(1);
        assertEquals("proj-b", second.id());
        assertTrue(second.needsOperator());
        assertTrue(second.line().contains("! 2"));
        assertTrue(second.line().contains("Project Beta"));
        assertTrue(second.line().contains("1 plan"));
        assertTrue(second.line().contains("10 min ago"));

        var third = rows.get(2);
        assertEquals("proj-c", third.id());
        assertTrue(third.needsOperator());
        assertTrue(third.line().contains("! 1"));
        assertTrue(third.line().contains("Project Setup"));
        assertTrue(third.line().contains("setup needs you"));
    }

    @Test
    @DisplayName("localizes summaries and relative times in German")
    void germanProjectRows() {
        var p = new ProjectSummary("proj-a", "Projekt A", 1, 0, 0, null, false, baseInstant.minusSeconds(300));
        var level = new ProjectsOverviewLevel(List.of(p));
        var rows = level.rows("de", clock);

        assertEquals(1, rows.size());
        var row = rows.getFirst();
        assertTrue(row.line().contains("1 Plan"));
        assertTrue(row.line().contains("vor 5 min"));
    }

    @Test
    @DisplayName("renders Needs you group above projects when items are present")
    void needsYouGroupAboveProjects() {
        var item1 = new NeedsOperatorItem(NeedsOperatorKind.DECISION, "/api/v1/decisions/Q-001",
                            new WorkLocation("proj-a", "Project Alpha"), null,
                            new WorkLocation("plan-1", "Plan One"), baseInstant.minusSeconds(60), "plan");
        var item2 = new NeedsOperatorItem(NeedsOperatorKind.CLEANUP_DUE, null, null, null, null,
                            baseInstant.minusSeconds(3600), null);
        var page = new NeedsOperatorPage(1, 1, false, false, 2, List.of(item1, item2));

        var p = new ProjectSummary("proj-a", "Project Alpha", 1, 0, 0, null, false, baseInstant);
        var level = new ProjectsOverviewLevel(List.of(p), page);
        var rows = level.rows("en", clock);

        assertEquals(4, rows.size());
        assertEquals(ProjectsOverviewLevel.GROUP_NEEDS_YOU_ID, rows.get(0).id());
        assertTrue(rows.get(0).line().contains("Needs you · 2"));
        assertFalse(rows.get(0).openable());

        assertEquals("needs:/api/v1/decisions/Q-001", rows.get(1).id());
        assertTrue(rows.get(1).line().contains("! decision"));
        assertTrue(rows.get(1).line().contains("Plan One"));
        assertTrue(rows.get(1).openable());
        assertTrue(rows.get(1).needsOperator());

        assertEquals(ProjectsOverviewLevel.rowIdOf(item2), rows.get(2).id());
        assertTrue(rows.get(2).line().contains("! cleanup due"));
        assertTrue(rows.get(2).openable());

        assertEquals("proj-a", rows.get(3).id());
        assertEquals(item1, level.needsItemOf(ProjectsOverviewLevel.rowIdOf(item1)));
        assertEquals(item2, level.needsItemOf(ProjectsOverviewLevel.rowIdOf(item2)));
        assertEquals(ProjectsOverviewLevel.rowIdOf(item1), ProjectsOverviewLevel.firstSelectableId(rows));
    }

    @Test
    @DisplayName("empty projects list with active needs items returns needs rows and handles invalid IDs")
    void emptyProjectsWithNeeds() {
        var item = new NeedsOperatorItem(NeedsOperatorKind.DECISION, "/api/v1/decisions/Q-001",
                            new WorkLocation("proj-a", "Project Alpha"), null,
                            new WorkLocation("plan-1", "Plan One"), baseInstant.minusSeconds(60), "plan");
        var page = new NeedsOperatorPage(1, 1, false, false, 1, List.of(item));
        var level = new ProjectsOverviewLevel(List.of(), page);
        var rows = level.rows("en", clock);
        assertEquals(2, rows.size());
        assertEquals(ProjectsOverviewLevel.GROUP_NEEDS_YOU_ID, rows.get(0).id());
        assertEquals(ProjectsOverviewLevel.rowIdOf(item), rows.get(1).id());
        assertNull(level.needsItemOf("needs:invalid"));
        assertNull(level.needsItemOf("needs:999"));
        assertNull(level.needsItemOf("not-a-needs-id"));
    }
}