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

        // proj-b has 2 attention items -> should be first!
        var first = rows.get(0);
        assertEquals("proj-b", first.id());
        assertTrue(first.needsOperator());
        assertTrue(first.line().contains("! 2"));
        assertTrue(first.line().contains("Project Beta"));
        assertTrue(first.line().contains("1 plan"));
        assertTrue(first.line().contains("10 min ago"));

        // proj-c has setup needs operator -> should be second
        var second = rows.get(1);
        assertEquals("proj-c", second.id());
        assertTrue(second.needsOperator());
        assertTrue(second.line().contains("! 1"));
        assertTrue(second.line().contains("Project Setup"));
        assertTrue(second.line().contains("setup needs you"));

        // proj-a has no attention items -> should be last
        var third = rows.get(2);
        assertEquals("proj-a", third.id());
        assertFalse(third.needsOperator());
        assertTrue(third.line().contains("Project Alpha"));
        assertTrue(third.line().contains("2 plans"));
        assertTrue(third.line().contains("1 epic"));
        assertTrue(third.line().contains("2 min ago"));
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
