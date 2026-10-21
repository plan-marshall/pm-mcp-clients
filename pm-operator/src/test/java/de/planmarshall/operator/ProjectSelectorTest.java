/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.project.ProjectReadiness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProjectSelector")
class ProjectSelectorTest {

    @Test
    @DisplayName("orders favourites first, then recent, then the rest without duplicates")
    void ordersFavouritesFirst() {
        var favourite = project("favourite");
        var recent = project("recent");
        var other = project("other");
        var explorer = new ProjectExplorer(List.of(favourite), List.of(recent, favourite),
                            List.of(other, recent, favourite), List.of());

        var ordered = ProjectSelector.order(explorer);

        assertEquals(List.of("favourite", "recent", "other"),
                            ordered.stream().map(EnrolledProject::projectId).toList());
    }

    @Test
    @DisplayName("accepts a number, an exact id, or leave")
    void parsesChoices() {
        var projects = List.of(project("first"), project("second"));

        assertEquals("first", ProjectSelector.parseChoice("1", projects).projectId());
        assertEquals("second", ProjectSelector.parseChoice(" 2 ", projects).projectId());
        assertEquals("second", ProjectSelector.parseChoice("second", projects).projectId());
        assertNull(ProjectSelector.parseChoice("q", projects));
        assertNull(ProjectSelector.parseChoice("", projects));
        assertNull(ProjectSelector.parseChoice("3", projects));
        assertNull(ProjectSelector.parseChoice(null, projects));
    }

    @Test
    @DisplayName("leaves a verb without --project untouched")
    void leavesOtherVerbsUntouched() {
        var context = new OperatorContext(Map.of(), Path.of("."), false);
        var outcome = ProjectSelector.rewrite(context, new PrintWriter(new StringWriter()),
                            new PrintWriter(new StringWriter()), new String[]{"reports", "--all"});
        assertTrue(outcome.proceed());
        assertEquals(List.of("reports", "--all"), outcome.args());
    }

    @Test
    @DisplayName("detects --project without a value and --project=? and stops when input is not possible")
    void stopsWithoutAValueWhenNonInteractive() {
        var context = new OperatorContext(Map.of(), Path.of("."), false);
        for (var args : List.of(
                            new String[]{"reports", "--project"},
                            new String[]{"reports", "--project", "?"},
                            new String[]{"reports", "--project=?"})) {
            var err = new StringWriter();
            var outcome = ProjectSelector.rewrite(context, new PrintWriter(new StringWriter()),
                                new PrintWriter(err), args);
            assertFalse(outcome.proceed(), String.join(" ", args));
            assertEquals(3, outcome.exitCode());
            assertTrue(err.toString().contains("interactive terminal"), err.toString());
        }
    }

    private static EnrolledProject project(String id) {
        return new EnrolledProject(id, id, null, ProjectReadiness.READY, null, 0, 0, 0, null, false, false);
    }
}
