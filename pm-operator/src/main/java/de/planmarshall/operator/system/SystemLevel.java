/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.operator.tui.TuiRow;

/**
 * The system view as a level of the full-screen view
 * ({@code doc/design/terminal-client/25-system.adoc#full-screen}).
 *
 * <p>Six figures in a fixed order — runtime, work, workers, jobs, models, cleanup — each with the
 * detail lines the design lists, and the selection on the six figure rows alone. A figure that
 * counts things leads one step to what it counts ({@link Section}); the gauges lead nowhere, their
 * detail is the figure's own line.
 *
 * <p>The model is pure: it holds one snapshot the caller read and turns it into rows through
 * {@link SystemView}, the formatter both views share. The window, the grouping of the models, and
 * the project are the operator's choice and stay with the model until the caller replaces it.
 */
public final class SystemLevel {

    /** The row id of the runtime figure. */
    public static final String ROW_RUNTIME = "sys:runtime";

    /** The row id of the work figure. */
    public static final String ROW_WORK = "sys:work";

    /** The row id of the workers figure. */
    public static final String ROW_WORKERS = "sys:workers";

    /** The row id of the jobs figure. */
    public static final String ROW_JOBS = "sys:jobs";

    /** The row id of the models figure. */
    public static final String ROW_MODELS = "sys:models";

    /** The row id of the cleanup figure. */
    public static final String ROW_CLEANUP = "sys:cleanup";

    /** The row id of the locks line inside the jobs section. */
    public static final String ROW_LOCKS = "sys:jobs:locks";

    /** The level id of the system root. */
    public static final String LEVEL_ID = "system";

    private static final String SECTION_LEVEL_PREFIX = "system:";
    private static final String DOT = " \u00b7 ";
    private static final String FOOTER_SEPARATOR = "   ";
    private static final String GROUP_MODEL = SystemView.GROUP_MODEL;
    private static final String GROUP_ROLE = SystemView.GROUP_ROLE;
    private static final Set<String> GROUPINGS = Set.of(GROUP_MODEL, GROUP_ROLE);

    /** The parts of the system view below the six figures. */
    public enum Section {
        /** The six figures. */
        ROOT(null),
        /** The workers of the machine. */
        WORKERS("system.workers.title"),
        /** The job kinds and their slots. */
        JOBS("system.jobs.title"),
        /** The locks the runtime holds. */
        LOCKS("operator.system.locks.title"),
        /** The models' calls in the window. */
        MODELS("system.models.title"),
        /** The cleanup of ended work. */
        CLEANUP("system.cleanup.title");

        private final @Nullable String titleKey;

        Section(@Nullable String titleKey) {
            this.titleKey = titleKey;
        }

        /**
         * @param locale the interface locale
         * @return the localized title of the section, empty for the root
         */
        public String title(String locale) {
            return titleKey == null ? "" : MessageCatalogue.format(locale, titleKey, Map.of());
        }
    }

    /** The figures the selection can rest on. */
    public enum Figure {
        /** The runtime's identity, uptime, memory, storage, and listeners. */
        RUNTIME,
        /** The plans, epics, tasks, and projects at work. */
        WORK,
        /** The workers of the machine. */
        WORKERS,
        /** The job kinds, their slots, and the locks. */
        JOBS,
        /** The models' calls in the window. */
        MODELS,
        /** The cleanup of ended work. */
        CLEANUP
    }

    private final @Nullable SystemSnapshot snapshot;
    private final SystemWindow window;
    private final String groupBy;
    private final @Nullable String projectId;

    /**
     * @param snapshot  the snapshot the runtime served, or {@code null} when it could not be read
     * @param window    the models' window
     * @param groupBy   {@code model} or {@code role}
     * @param projectId the project the view shows, or {@code null} for all projects
     */
    public SystemLevel(@Nullable SystemSnapshot snapshot, SystemWindow window, @Nullable String groupBy,
                        @Nullable String projectId) {
        this.snapshot = snapshot;
        this.window = window != null ? window : SystemWindow.HOUR;
        this.groupBy = groupBy != null && GROUPINGS.contains(groupBy) ? groupBy : GROUP_MODEL;
        this.projectId = projectId;
    }

    /**
     * @param snapshot  the snapshot the runtime served
     * @param window    the models' window
     * @param groupBy   {@code model} or {@code role}
     * @param projectId the project the view shows, or {@code null}
     * @return the level at the six figures
     */
    public static SystemLevel root(SystemSnapshot snapshot, SystemWindow window, @Nullable String groupBy,
                        @Nullable String projectId) {
        return new SystemLevel(snapshot, window, groupBy, projectId);
    }

    /**
     * @param window    the models' window
     * @param groupBy   {@code model} or {@code role}
     * @param projectId the project the view shows, or {@code null}
     * @return a level that says the snapshot could not be read
     */
    public static SystemLevel unavailable(SystemWindow window, @Nullable String groupBy,
                        @Nullable String projectId) {
        return new SystemLevel(null, window, groupBy, projectId);
    }

    /** @return the snapshot, or {@code null} when it could not be read */
    public @Nullable SystemSnapshot snapshot() {
        return snapshot;
    }

    /** @return the models' window */
    public SystemWindow window() {
        return window;
    }

    /** @return {@code model} or {@code role} */
    public String groupBy() {
        return groupBy;
    }

    /** @return the project the view shows, or {@code null} for all projects */
    public @Nullable String projectId() {
        return projectId;
    }

    /** @return {@code true} when the runtime offers to upgrade this installation */
    public boolean upgradeSupported() {
        return snapshot != null && snapshot.runtime().upgradeSupported();
    }

    /** @return {@code true} when no newer version is known yet */
    public boolean versionUnknown() {
        return snapshot != null && snapshot.runtime().newerVersion() == null;
    }

    /**
     * @param newWindow the new window
     * @param newGroup  the new grouping
     * @param newProject the new project
     * @return this level showing the same snapshot under the new choice
     */
    public SystemLevel withChoice(SystemWindow newWindow, @Nullable String newGroup, @Nullable String newProject) {
        return new SystemLevel(snapshot, newWindow, newGroup, newProject);
    }

    /**
     * @param fresh the snapshot just read
     * @return this level showing the fresh snapshot
     */
    public SystemLevel withSnapshot(@Nullable SystemSnapshot fresh) {
        return new SystemLevel(fresh, window, groupBy, projectId);
    }

    /** @return the grouping the {@code b} key leads to */
    public String nextGroupBy() {
        return GROUP_MODEL.equals(groupBy) ? GROUP_ROLE : GROUP_MODEL;
    }

    /** @return the window the {@code w} key leads to */
    public SystemWindow nextWindow() {
        return switch (window) {
            case HOUR -> SystemWindow.TODAY;
            case TODAY -> SystemWindow.SEVEN_DAYS;
            case SEVEN_DAYS -> SystemWindow.HOUR;
        };
    }

    /**
     * @param section the section
     * @return the level id the view stack uses for it
     */
    public static String levelId(Section section) {
        return section == Section.ROOT ? LEVEL_ID : SECTION_LEVEL_PREFIX + section.name().toLowerCase(Locale.ROOT);
    }

    /**
     * @param id the level id
     * @return the section the level shows
     */
    public static Section sectionOf(@Nullable String id) {
        if (id == null || LEVEL_ID.equals(id) || !id.startsWith(SECTION_LEVEL_PREFIX)) {
            return Section.ROOT;
        }
        var name = id.substring(SECTION_LEVEL_PREFIX.length()).toUpperCase(Locale.ROOT);
        for (var section : Section.values()) {
            if (section.name().equals(name)) {
                return section;
            }
        }
        return Section.ROOT;
    }

    /**
     * @param rowId the row id
     * @return the figure the row belongs to, or {@code null} for a detail row
     */
    public static @Nullable Figure figureOf(String rowId) {
        return switch (rowId) {
            case ROW_RUNTIME -> Figure.RUNTIME;
            case ROW_WORK -> Figure.WORK;
            case ROW_WORKERS -> Figure.WORKERS;
            case ROW_JOBS -> Figure.JOBS;
            case ROW_MODELS -> Figure.MODELS;
            case ROW_CLEANUP -> Figure.CLEANUP;
            default -> null;
        };
    }

    /**
     * @param locale the interface locale
     * @return the header of the level
     */
    public String breadcrumb(String locale) {
        return breadcrumb(locale, Section.ROOT);
    }

    /**
     * @param locale  the interface locale
     * @param section the section the level shows
     * @return the header of the level
     */
    public String breadcrumb(String locale, Section section) {
        var title = snapshot == null
                            ? MessageCatalogue.format(locale, "system.view.title", Map.of())
                            : MessageCatalogue.format(locale, "operator.system.title",
                            Map.of("version", SystemView.clean(snapshot.runtime().version())));
        if (section != Section.ROOT) {
            title += DOT + SystemView.clean(section.title(locale));
        }
        if (projectId != null) {
            title += DOT + SystemView.clean(projectId);
        }
        return title;
    }

    /**
     * The footer of the view: the keys of the level, with {@code b} naming the grouping it leads to
     * and {@code p} naming all projects while one is shown.
     *
     * @param locale the interface locale
     * @return the footer line
     */
    public String footer(String locale) {
        var byTarget = GROUP_MODEL.equals(groupBy) ? GROUP_ROLE : GROUP_MODEL;
        var projectKey = projectId == null
                            ? "operator.system.keys.per_project" : "operator.system.keys.all_projects";
        // Priority order: a narrower terminal drops the tail, never a key that matters more.
        return String.join(FOOTER_SEPARATOR,
                            key(locale, "↑↓", "operator.overview.keys.move"),
                            key(locale, "Enter", "operator.overview.keys.open"),
                            key(locale, "a", "operator.overview.keys.actions"),
                            key(locale, "?", "operator.overview.keys.help"),
                            key(locale, "q", "operator.overview.keys.leave"),
                            key(locale, "w", "operator.system.keys.window"),
                            "b " + MessageCatalogue.format(locale, "operator.system.keys.by",
                                                Map.of("by", byTarget)),
                            key(locale, "p", projectKey));
    }

    private static String key(String locale, String display, String textKey) {
        return display + " " + MessageCatalogue.format(locale, textKey, Map.of());
    }

    /**
     * @param locale the interface locale
     * @return the six figures
     */
    public List<TuiRow> rows(String locale) {
        return rows(locale, Section.ROOT);
    }

    /**
     * @param locale  the interface locale
     * @param section the section the level shows
     * @return the rows of that section
     */
    public List<TuiRow> rows(String locale, Section section) {
        if (snapshot == null) {
            return List.of(TuiRow.leaf("sys:unavailable",
                                MessageCatalogue.format(locale, "operator.system.load_failed", Map.of())));
        }
        return switch (section) {
            case ROOT -> rootRows(locale);
            case WORKERS -> workerRows(locale);
            case JOBS -> jobRows(locale);
            case LOCKS -> lockRows(locale);
            case MODELS -> modelRows(locale);
            case CLEANUP -> cleanupRows(locale);
        };
    }

    private List<TuiRow> rootRows(String locale) {
        var figures = SystemView.root(snapshot, locale, window, groupBy);
        var rows = new ArrayList<TuiRow>(figures.size());
        var ids = List.of(ROW_RUNTIME, ROW_WORK, ROW_WORKERS, ROW_JOBS, ROW_MODELS, ROW_CLEANUP);
        for (int index = 0; index < figures.size(); index++) {
            rows.add(new TuiRow(ids.get(index), SystemView.rowLine(figures.get(index)),
                                SystemView.rowDetails(figures.get(index)), false, true));
        }
        return rows;
    }

    private List<TuiRow> workerRows(String locale) {
        var workers = requiredSnapshot().workers();
        if (workers.isEmpty()) {
            return List.of(TuiRow.leaf("sys:workers:none",
                                MessageCatalogue.format(locale, "system.workers.none", Map.of())));
        }
        var lines = SystemView.workerLines(locale, workers);
        var rows = new ArrayList<TuiRow>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            rows.add(TuiRow.leaf("sys:workers:" + index, lines.get(index)));
        }
        return rows;
    }

    private List<TuiRow> jobRows(String locale) {
        var jobs = requiredSnapshot().jobs();
        var kinds = SystemView.jobKinds(jobs);
        var lines = SystemView.jobKindLines(locale, jobs);
        var rows = new ArrayList<TuiRow>(lines.size() + 1);
        for (int index = 0; index < lines.size(); index++) {
            rows.add(TuiRow.leaf("sys:jobs:" + index, lines.get(index)));
        }
        rows.add(new TuiRow(ROW_LOCKS, SystemView.locksLine(locale, jobs), null, false, true));
        return rows;
    }

    private List<TuiRow> lockRows(String locale) {
        var jobs = requiredSnapshot().jobs();
        var locks = JsonTree.integer(jobs, "locks_held");
        int held = locks != null ? locks : 0;
        var line = held == 0
                            ? MessageCatalogue.format(locale, "operator.system.locks.none", Map.of())
                            : SystemView.locksLine(locale, jobs);
        return List.of(TuiRow.leaf("sys:locks:count", line));
    }

    private List<TuiRow> modelRows(String locale) {
        var models = requiredSnapshot().models();
        if (models.isEmpty()) {
            return List.of(TuiRow.leaf("sys:models:none",
                                MessageCatalogue.format(locale, "system.models.none", Map.of())));
        }
        var lines = SystemView.modelLines(locale, models, groupBy);
        var rows = new ArrayList<TuiRow>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            rows.add(TuiRow.leaf("sys:models:" + index, lines.get(index)));
        }
        return rows;
    }

    private List<TuiRow> cleanupRows(String locale) {
        CleanupSummary cleanup = requiredSnapshot().cleanup();
        var rows = new ArrayList<TuiRow>(4);
        rows.add(TuiRow.leaf("sys:cleanup:due", SystemView.cleanupLine(locale, cleanup)));
        rows.add(TuiRow.leaf("sys:cleanup:freed",
                            MessageCatalogue.format(locale, "system.cleanup.summary.frees",
                                                Map.of("size", SystemFormatting.bytes(cleanup.freedBytes())))));
        rows.add(TuiRow.leaf("sys:cleanup:kept", SystemView.keptLine(locale, cleanup)));
        rows.add(TuiRow.leaf("sys:cleanup:next",
                            MessageCatalogue.format(locale, "operator.system.cleanup.next",
                                                Map.of("time", SystemView.clean(cleanup.nextCheckAt())))));
        return rows;
    }

    private SystemSnapshot requiredSnapshot() {
        return Objects.requireNonNull(snapshot, "snapshot");
    }
}
