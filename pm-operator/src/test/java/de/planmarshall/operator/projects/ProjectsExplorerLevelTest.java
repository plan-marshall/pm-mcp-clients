/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import de.planmarshall.api.project.BeingAddedProject;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.project.ProjectReadiness;
import de.planmarshall.operator.tui.TuiRow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProjectsExplorerLevel")
class ProjectsExplorerLevelTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("renders Being added, Favourites, Recent, and All projects by name")
    void rendersTheFourGroups() {
        var explorer = new ProjectExplorer(
                            List.of(project("pm-mcp-clients", true, ProjectReadiness.READY, 2, 3, 1,
                                                "2026-10-20T11:58:00Z")),
                            List.of(project("plan-marshall", false, ProjectReadiness.SETUP_NEEDS, 1, 0, 0,
                                                "2026-10-19T10:00:00Z")),
                            List.of(project("API-Sheriff", false, ProjectReadiness.READY, 0, 0, 0,
                                                "2026-09-12T10:00:00Z"),
                                                project("legacy-portal", false, ProjectReadiness.RELEASED, 0, 0, 0,
                                                                    "2026-08-17T10:00:00Z"),
                                                project("pm-mcp-clients", true, ProjectReadiness.READY, 2, 3, 1,
                                                                    "2026-10-20T11:58:00Z")),
                            List.of(new BeingAddedProject("~/git/cui-parent", "Identity", 2, 10, null, null)));

        var rows = new ProjectsExplorerLevel(explorer, false).rows("en", CLOCK);
        var text = String.join("\n", rows.stream().map(TuiRow::line).toList());

        assertTrue(text.contains("Being added"), text);
        assertTrue(text.contains("~/git/cui-parent"), text);
        assertTrue(text.contains("Identity (2 of 10)"), text);
        assertTrue(text.contains("Favourites"), text);
        assertTrue(text.contains("pm-mcp-clients"), text);
        assertTrue(text.contains("needs you: 2"), text);
        assertTrue(text.contains("3 plans \u00b7 1 epic"), text);
        assertTrue(text.contains("2 min ago"), text);
        assertTrue(text.contains("Recent"), text);
        assertTrue(text.contains("plan-marshall"), text);
        assertTrue(text.contains("setup needs: credential sonar"), text);
        assertTrue(text.contains("All projects \u00b7 by name"), text);
        assertTrue(text.contains("legacy-portal"), text);
        assertTrue(text.contains("released"), text);
    }

    @Test
    @DisplayName("All projects orders by name or by most used")
    void ordersAllProjects() {
        var explorer = new ProjectExplorer(List.of(), List.of(),
                            List.of(project("zeta", false, ProjectReadiness.READY, 0, 0, 0, "2026-10-20T10:00:00Z"),
                                                project("alpha", false, ProjectReadiness.READY, 0, 0, 0,
                                                                    "2026-10-18T10:00:00Z")),
                            List.of());

        var byName = new ProjectsExplorerLevel(explorer, false).rows("en", CLOCK);
        var byUsage = new ProjectsExplorerLevel(explorer, true).rows("en", CLOCK);

        assertTrue(indexOf(byName, "alpha") < indexOf(byName, "zeta"));
        assertTrue(indexOf(byUsage, "zeta") < indexOf(byUsage, "alpha"));
        assertTrue(byUsage.stream().anyMatch(row -> row.line().contains("All projects \u00b7 most used")),
                            byUsage.toString());
    }

    @Test
    @DisplayName("an empty machine shows the empty text alone")
    void emptyExplorer() {
        var rows = new ProjectsExplorerLevel(new ProjectExplorer(List.of(), List.of(), List.of(), List.of()), false)
                            .rows("en", CLOCK);
        assertEquals(1, rows.size());
        assertEquals(ProjectsExplorerLevel.EMPTY_ROW_ID, rows.getFirst().id());
        assertTrue(rows.getFirst().line().contains("No project is enrolled yet."));
    }

    @Test
    @DisplayName("row ids resolve to their project or onboarding run")
    void resolvesRows() {
        var being = new BeingAddedProject("/home/oliver/git/cui-parent", "Identity", 2, 10, null, null);
        var project = project("pm-mcp-clients", true, ProjectReadiness.READY, 0, 0, 0, null);
        var level = new ProjectsExplorerLevel(new ProjectExplorer(List.of(project), List.of(), List.of(project),
                            List.of(being)), false);

        assertEquals(project, level.projectOf(ProjectsExplorerLevel.PROJECT_PREFIX + "pm-mcp-clients"));
        assertEquals(being, level.beingAddedOf(ProjectsExplorerLevel.BEING_ADDED_PREFIX + being.checkoutPath()));
        assertTrue(ProjectsExplorerLevel.isHeader(ProjectsExplorerLevel.GROUP_ALL_ID));
        assertFalse(ProjectsExplorerLevel.isHeader(ProjectsExplorerLevel.PROJECT_PREFIX + "pm-mcp-clients"));
        assertTrue(ProjectsExplorerLevel.isSelectable(ProjectsExplorerLevel.PROJECT_PREFIX + "pm-mcp-clients"));
        assertNotNull(ProjectsExplorerLevel.firstSelectableId(level.rows("en", CLOCK)));
    }

    private static int indexOf(List<TuiRow> rows, String projectId) {
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).id().equals(ProjectsExplorerLevel.PROJECT_PREFIX + projectId)) {
                return index;
            }
        }
        throw new AssertionError("row not found: " + projectId);
    }

    private static EnrolledProject project(String id, boolean favourite, ProjectReadiness readiness, int needs,
                        int plans, int epics, String lastActive) {
        return new EnrolledProject(id, id, null, readiness,
                            readiness == ProjectReadiness.SETUP_NEEDS ? "credential sonar" : null,
                            needs, plans, epics, lastActive != null ? Instant.parse(lastActive) : null,
                            favourite, readiness == ProjectReadiness.RELEASED);
    }
}
