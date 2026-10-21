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

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.project.BeingAddedProject;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.project.ProjectReadiness;
import de.planmarshall.operator.OutputSafety;
import de.planmarshall.operator.overview.OverviewFormatting;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;

/**
 * The explorer's view model ({@code doc/design/terminal-client/17-projects.adoc#explorer}): the
 * four groups, the rows of a project and of an onboarding that has not enrolled yet, and the
 * order of _All projects_ by name or by most used.
 *
 * <p>The model is pure: it reads the runtime's {@link ProjectExplorer} and composes
 * {@link TuiRow}s. The application owns the stack, the keys, and the calls that change the
 * runtime.
 *
 * @param explorer   the runtime's report of the machine's projects
 * @param byMostUsed {@code true} to order _All projects_ by most used, {@code false} by name
 */
public record ProjectsExplorerLevel(ProjectExplorer explorer, boolean byMostUsed) {

    /** Row id of the _Being added_ group header. */
    public static final String GROUP_BEING_ADDED_ID = "__projects_group_being_added__";

    /** Row id of the _Favourites_ group header. */
    public static final String GROUP_FAVOURITES_ID = "__projects_group_favourites__";

    /** Row id of the _Recent_ group header. */
    public static final String GROUP_RECENT_ID = "__projects_group_recent__";

    /** Row id of the _All projects_ group header. */
    public static final String GROUP_ALL_ID = "__projects_group_all__";

    /** Row id of the empty list. */
    public static final String EMPTY_ROW_ID = "__projects_empty__";

    /** The prefix of the row id of an enrolled project. */
    public static final String PROJECT_PREFIX = "project:";

    /** The prefix of the row id of an onboarding that has not enrolled yet. */
    public static final String BEING_ADDED_PREFIX = "being:";

    private static final String SEPARATOR = "   ";
    private static final String DOT_SEPARATOR = " \u00b7 ";
    private static final String STAR = "\u2605 ";
    private static final int NAME_COLUMNS = 20;

    /**
     * @param explorer   the runtime's report
     * @param byMostUsed {@code true} to order _All projects_ by most used
     */
    public ProjectsExplorerLevel {
        Objects.requireNonNull(explorer, "explorer");
    }

    /**
     * @param explorer the runtime's report
     * @return the explorer ordered by name
     */
    public static ProjectsExplorerLevel byName(ProjectExplorer explorer) {
        return new ProjectsExplorerLevel(explorer, false);
    }

    /**
     * @param locale the interface locale
     * @return the root level of the explorer
     */
    public static TuiLevel level(String locale) {
        return TuiLevel.root(TuiLevel.Kind.PROJECTS,
                            MessageCatalogue.format(locale, "project.explorer.title", Map.of()));
    }

    /**
     * @param locale the interface locale
     * @param clock  the reference clock of the relative activity column
     * @return the rows of the explorer, group headers included
     */
    public List<TuiRow> rows(String locale, Clock clock) {
        var rows = new ArrayList<TuiRow>();
        if (explorer.beingAdded().isEmpty() && explorer.favourites().isEmpty()
                            && explorer.recent().isEmpty() && explorer.allProjects().isEmpty()) {
            rows.add(TuiRow.leaf(EMPTY_ROW_ID, MessageCatalogue.format(locale, "project.explorer.empty", Map.of())));
            return List.copyOf(rows);
        }

        appendBeingAdded(rows, locale);
        appendGroup(rows, locale, GROUP_FAVOURITES_ID, "project.explorer.favourites", explorer.favourites(), clock);
        appendGroup(rows, locale, GROUP_RECENT_ID, "project.explorer.recent", recentWithoutFavourites(), clock);
        appendAllProjects(rows, locale, clock);
        return List.copyOf(rows);
    }

    /**
     * @return the projects in the order _All projects_ shows them
     */
    public List<EnrolledProject> orderedAllProjects() {
        var sorted = new ArrayList<>(explorer.allProjects());
        if (byMostUsed) {
            sorted.sort(Comparator.comparing(EnrolledProject::lastActive,
                                Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
                                .thenComparing(project -> project.projectId().toLowerCase(Locale.ROOT)));
        } else {
            sorted.sort(Comparator.comparing((EnrolledProject project) -> project.displayName().toLowerCase(Locale.ROOT))
                                .thenComparing(project -> project.projectId().toLowerCase(Locale.ROOT)));
        }
        return List.copyOf(sorted);
    }

    /**
     * @param rowId the row id
     * @return the enrolled project of that row, or {@code null}
     */
    public @Nullable EnrolledProject projectOf(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(PROJECT_PREFIX)) {
            return null;
        }
        var projectId = rowId.substring(PROJECT_PREFIX.length());
        return allKnownProjects().stream()
                            .filter(project -> project.projectId().equals(projectId))
                            .findFirst().orElse(null);
    }

    /**
     * @param rowId the row id
     * @return the onboarding run of that row, or {@code null}
     */
    public @Nullable BeingAddedProject beingAddedOf(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(BEING_ADDED_PREFIX)) {
            return null;
        }
        var path = rowId.substring(BEING_ADDED_PREFIX.length());
        return explorer.beingAdded().stream()
                            .filter(being -> being.checkoutPath().equals(path))
                            .findFirst().orElse(null);
    }

    /**
     * @param rowId a row id
     * @return {@code true} when the row is a group header or the empty row
     */
    public static boolean isHeader(@Nullable String rowId) {
        return rowId != null && (rowId.startsWith("__") || EMPTY_ROW_ID.equals(rowId));
    }

    /**
     * @param rowId a row id
     * @return {@code true} when the row names an enrolled project or an onboarding run
     */
    public static boolean isSelectable(@Nullable String rowId) {
        return rowId != null
                            && (rowId.startsWith(PROJECT_PREFIX) || rowId.startsWith(BEING_ADDED_PREFIX));
    }

    /**
     * @param rows the rows of the explorer
     * @return the id of the first project or onboarding row, or {@code null}
     */
    public static @Nullable String firstSelectableId(List<TuiRow> rows) {
        for (var row : rows) {
            if (isSelectable(row.id())) {
                return row.id();
            }
        }
        return null;
    }

    /**
     * @param favourites the favourites in their new order
     * @return the same level with the new order
     */
    public ProjectsExplorerLevel withFavourites(List<EnrolledProject> favourites) {
        var next = new ProjectExplorer(List.copyOf(favourites), explorer.recent(), explorer.allProjects(),
                            explorer.beingAdded());
        return new ProjectsExplorerLevel(next, byMostUsed);
    }

    /**
     * @return the same projects ordered the other way
     */
    public ProjectsExplorerLevel withSortToggled() {
        return new ProjectsExplorerLevel(explorer, !byMostUsed);
    }

    /**
     * @param projectId the project
     * @return the project in the runtime's report, or {@code null}
     */
    public @Nullable EnrolledProject findProject(String projectId) {
        return allKnownProjects().stream()
                            .filter(project -> project.projectId().equals(projectId))
                            .findFirst().orElse(null);
    }

    private List<EnrolledProject> allKnownProjects() {
        var all = new ArrayList<EnrolledProject>();
        all.addAll(explorer.favourites());
        all.addAll(explorer.recent());
        all.addAll(explorer.allProjects());
        return all;
    }

    private List<EnrolledProject> recentWithoutFavourites() {
        var favouriteIds = explorer.favourites().stream().map(EnrolledProject::projectId).toList();
        return explorer.recent().stream()
                            .filter(project -> !favouriteIds.contains(project.projectId()))
                            .toList();
    }

    private void appendBeingAdded(List<TuiRow> rows, String locale) {
        var beingAdded = explorer.beingAdded();
        if (beingAdded.isEmpty()) {
            return;
        }
        rows.add(header(GROUP_BEING_ADDED_ID,
                            MessageCatalogue.format(locale, "project.adding.title", Map.of())));
        for (var being : beingAdded) {
            var path = OutputSafety.stripControlSequences(being.checkoutPath());
            var step = MessageCatalogue.format(locale, "operator.projects.being_added.step",
                                Map.of("step", OutputSafety.stripControlSequences(being.currentStep()),
                                                    "number", being.stepNumber(),
                                                    "total", being.totalSteps()));
            rows.add(TuiRow.openable(BEING_ADDED_PREFIX + being.checkoutPath(),
                                path + SEPARATOR + step, null));
        }
    }

    private void appendGroup(List<TuiRow> rows, String locale, String headerId, String headerKey,
                        List<EnrolledProject> projects, Clock clock) {
        if (projects.isEmpty()) {
            return;
        }
        rows.add(header(headerId, MessageCatalogue.format(locale, headerKey, Map.of())));
        for (var project : projects) {
            rows.add(projectRow(locale, project, clock));
        }
    }

    private void appendAllProjects(List<TuiRow> rows, String locale, Clock clock) {
        var projects = orderedAllProjects();
        if (projects.isEmpty()) {
            return;
        }
        var order = MessageCatalogue.format(locale,
                            byMostUsed ? "project.explorer.most_used" : "project.explorer.by_name", Map.of());
        rows.add(header(GROUP_ALL_ID,
                            MessageCatalogue.format(locale, "project.explorer.all", Map.of()) + DOT_SEPARATOR + order));
        for (var project : projects) {
            rows.add(projectRow(locale, project, clock));
        }
    }

    private static TuiRow header(String id, String title) {
        return new TuiRow(id, title, null, false, false);
    }

    private TuiRow projectRow(String locale, EnrolledProject project, Clock clock) {
        var line = new StringBuilder();
        line.append(project.isFavourite() ? STAR : "  ");
        line.append(TuiLayout.pad(OutputSafety.stripControlSequences(project.projectId()), NAME_COLUMNS));
        line.append(SEPARATOR).append(readiness(locale, project));

        if (project.needsOperatorCount() > 0) {
            line.append(SEPARATOR).append(MessageCatalogue.format(locale, "operator.projects.list.needs_you",
                                Map.of("count", project.needsOperatorCount())));
        }
        if (project.activePlansCount() > 0 || project.activeEpicsCount() > 0) {
            line.append(SEPARATOR).append(MessageCatalogue.format(locale, "project.explorer.counts",
                                Map.of("plans", project.activePlansCount(), "epics", project.activeEpicsCount())));
        }
        var active = OverviewFormatting.formatRelativeActivity(locale, project.lastActive(), clock);
        if (!active.isEmpty()) {
            line.append(SEPARATOR).append(active);
        }
        return new TuiRow(PROJECT_PREFIX + project.projectId(), line.toString().stripTrailing(), null,
                            project.needsOperatorCount() > 0, true);
    }

    private static String readiness(String locale, EnrolledProject project) {
        if (project.isReleased() || project.readiness() == ProjectReadiness.RELEASED) {
            return MessageCatalogue.format(locale, "project.state.released", Map.of());
        }
        if (project.readiness() == ProjectReadiness.SETUP_NEEDS) {
            var need = project.setupNeedsSummary() != null && !project.setupNeedsSummary().isBlank()
                                ? OutputSafety.stripControlSequences(project.setupNeedsSummary())
                                : MessageCatalogue.format(locale, "project.state.setup", Map.of());
            return MessageCatalogue.format(locale, "project.state.setup_needs", Map.of("need", need));
        }
        return MessageCatalogue.format(locale, "project.state.ready", Map.of());
    }
}
