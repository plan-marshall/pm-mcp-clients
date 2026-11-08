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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import de.planmarshall.api.overview.ProjectSummary;
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
        assertTrue(row.line().contains("vor 5 Min."));
    }
}
