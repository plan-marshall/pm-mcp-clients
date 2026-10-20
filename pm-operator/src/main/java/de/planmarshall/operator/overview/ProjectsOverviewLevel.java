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

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;

/**
 * Level 1 of the full-screen overview: the projects list
 * ({@code doc/design/terminal-client/15-overview.adoc#full-screen-layout}).
 */
public final class ProjectsOverviewLevel {

    /** Empty project list row ID. */
    public static final String EMPTY_ROW_ID = "__empty__";

    private final List<ProjectSummary> projects;

    /**
     * @param projects the list of project summaries
     */
    public ProjectsOverviewLevel(List<ProjectSummary> projects) {
        this.projects = List.copyOf(Objects.requireNonNull(projects, "projects"));
    }

    /**
     * @return the immutable project list
     */
    public List<ProjectSummary> projects() {
        return projects;
    }

    /**
     * Creates the {@link TuiLevel} representation for Level 1.
     *
     * @param locale the target locale
     * @return the root overview level
     */
    public static TuiLevel level(String locale) {
        var title = MessageCatalogue.format(locale, "overview.running.title", Map.of())
                            + " \u00b7 "
                            + (locale.toLowerCase(Locale.ROOT).startsWith("de") ? "Projekte" : "projects");
        return TuiLevel.root(TuiLevel.Kind.OVERVIEW, title);
    }

    /**
     * Builds the visible {@link TuiRow} items for this level.
     *
     * @param locale the target locale
     * @param clock  the reference clock for relative activity timestamps
     * @return list of TUI rows
     */
    public List<TuiRow> rows(String locale, Clock clock) {
        if (projects.isEmpty()) {
            var emptyText = MessageCatalogue.format(locale, "overview.projects.empty", Map.of());
            return List.of(new TuiRow(EMPTY_ROW_ID, emptyText, null, false, false));
        }

        var sorted = OverviewFormatting.sortProjects(projects);
        var rows = new ArrayList<TuiRow>(sorted.size());

        for (var project : sorted) {
            boolean needs = OverviewFormatting.needsOperator(project);
            int attentionCount = OverviewFormatting.projectAttentionCount(project);

            String marker;
            if (attentionCount > 0) {
                marker = String.format("%-5s", MessageCatalogue.format(locale, "overview.needs.count", Map.of("count", attentionCount)));
            } else {
                marker = "     ";
            }

            var name = project.displayName() != null && !project.displayName().isBlank()
                                ? project.displayName()
                                : project.projectId();
            var cleanName = OverviewFormatting.clean(name);

            var summary = formatSummary(locale, project);
            var relativeTime = OverviewFormatting.formatRelativeActivity(locale, project.lastActivityAt(), clock);

            String line;
            if (!relativeTime.isEmpty()) {
                line = String.format("%s%-16s %-36s %s", marker, cleanName, summary, relativeTime);
            } else {
                line = String.format("%s%-16s %s", marker, cleanName, summary);
            }

            rows.add(new TuiRow(project.projectId(), line.stripTrailing(), null, needs, true));
        }

        return rows;
    }

    private static String formatSummary(String locale, ProjectSummary project) {
        if (project.setupNeedsOperator()) {
            return MessageCatalogue.format(locale, "overview.project.setup", Map.of());
        }
        int plans = project.activePlansCount();
        int epics = project.activeEpicsCount();

        if (plans > 0 && epics > 0) {
            return MessageCatalogue.format(locale, "overview.project.counts", Map.of("plans", plans, "epics", epics));
        }
        if (plans > 0) {
            return MessageCatalogue.format(locale, "overview.project.plans_only", Map.of("plans", plans));
        }
        if (epics > 0) {
            return MessageCatalogue.format(locale, "overview.project.epics_only", Map.of("epics", epics));
        }
        return MessageCatalogue.format(locale, "overview.projects.none_active", Map.of());
    }
}
