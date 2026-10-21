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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectReadiness;
import de.planmarshall.operator.tui.TuiMenuEntry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProjectsExplorerActions")
class ProjectsExplorerActionsTest {

    @Test
    @DisplayName("a ready project offers the Add actions, Running work, Setup, and Favourite")
    void readyProjectMenu() {
        var entries = ProjectsExplorerActions.forProject("en", project(ProjectReadiness.READY, false), List.of());
        assertEquals(List.of("e", "p", "r", "u", "*"), triggers(entries));
        assertEquals(ProjectsExplorerActions.ADD_EPIC, entries.getFirst().action());
        assertTrue(entries.stream().filter(entry -> ProjectsExplorerActions.FAVOURITE.equals(entry.action()))
                            .allMatch(TuiMenuEntry::enabled));
    }

    @Test
    @DisplayName("an open start entry is offered first as Continue starting")
    void openEntryComesFirst() {
        var entries = ProjectsExplorerActions.forProject("en", project(ProjectReadiness.READY, false),
                            List.of(new ProjectOpenEntry("ent-3f9a", "plan", 2)));
        assertEquals(ProjectsExplorerActions.CONTINUE_STARTING, entries.getFirst().action());
        assertEquals("c", entries.getFirst().trigger());
        assertTrue(entries.getFirst().label().contains("2 sources"), entries.getFirst().label());
    }

    @Test
    @DisplayName("a project that is not ready offers Continue first and disables both Add actions with the reason")
    void notReadyProjectMenu() {
        var entries = ProjectsExplorerActions.forProject("en", project(ProjectReadiness.SETUP_NEEDS, false),
                            List.of());
        assertEquals(List.of("c", "e", "p", "r", "u", "*"), triggers(entries));
        var addEpic = entries.get(1);
        assertFalse(addEpic.enabled());
        assertTrue(addEpic.reason().contains("pm-mcp-clients is not ready: credential sonar"),
                            addEpic.reason());
        assertFalse(entries.get(2).enabled());
    }

    @Test
    @DisplayName("a released project offers Enrol again and Ended work, and no Running work")
    void releasedProjectMenu() {
        var entries = ProjectsExplorerActions.forProject("en", project(ProjectReadiness.RELEASED, false), List.of());
        assertEquals(List.of("a", "E"), triggers(entries));
        assertEquals(ProjectsExplorerActions.ENROL_AGAIN, entries.getFirst().action());
        assertEquals(ProjectsExplorerActions.ENDED_WORK, entries.get(1).action());
    }

    @Test
    @DisplayName("a favourite offers to be removed from the favourites")
    void favouriteLabel() {
        var entries = ProjectsExplorerActions.forProject("en", project(ProjectReadiness.READY, true), List.of());
        var favourite = entries.stream().filter(entry -> ProjectsExplorerActions.FAVOURITE.equals(entry.action()))
                            .findFirst().orElseThrow();
        assertTrue(favourite.label().contains("Remove from favourites"), favourite.label());
    }

    @Test
    @DisplayName("a project being added offers Continue and Abandon")
    void beingAddedMenu() {
        var rows = ProjectsExplorerActions.forBeingAdded("en");
        assertEquals(List.of("c", "x"), triggers(rows));
        assertEquals(ProjectsExplorerActions.CONTINUE_ONBOARDING, rows.getFirst().action());
        assertEquals(ProjectsExplorerActions.ABANDON, rows.get(1).action());
    }

    private static List<String> triggers(List<TuiMenuEntry> entries) {
        return entries.stream().map(TuiMenuEntry::trigger).toList();
    }

    private static EnrolledProject project(ProjectReadiness readiness, boolean favourite) {
        return new EnrolledProject("pm-mcp-clients", "pm-mcp-clients", null, readiness,
                            readiness == ProjectReadiness.SETUP_NEEDS ? "credential sonar" : null,
                            0, 0, 0, null, favourite, readiness == ProjectReadiness.RELEASED);
    }
}
