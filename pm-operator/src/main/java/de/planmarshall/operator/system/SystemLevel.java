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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.ModelMetric;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.api.system.WorkerState;
import de.planmarshall.operator.OutputSafety;
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
 * <p>The model is pure: it holds one snapshot the caller read and turns it into rows. The window,
 * the grouping of the models, and the project are the operator's choice and stay with the model
 * until the caller replaces it.
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
    private static final String LABEL_FORMAT = "%-9s ";
    private static final String DETAIL_INDENT = "      ";
    private static final String GROUP_MODEL = "model";
    private static final String GROUP_ROLE = "role";
    private static final List<String> PLAN_STATE_ORDER =
                        List.of("running", "waiting_for_you", "held", "paused", "pausing");
    private static final List<String> EPIC_STATE_ORDER =
                        List.of("running", "waiting_for_you", "held", "paused");
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
                            Map.of("version", clean(snapshot.runtime().version())));
        if (section != Section.ROOT) {
            title += DOT + clean(section.title(locale));
        }
        if (projectId != null) {
            title += DOT + clean(projectId);
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
        var rows = new ArrayList<TuiRow>(6);
        rows.add(runtimeRow(locale));
        rows.add(workRow(locale));
        rows.add(new TuiRow(ROW_WORKERS, figure(locale, "system.workers.title", workersSummary(locale)),
                            lines(workerLines(locale)), false, true));
        rows.add(jobsRow(locale));
        rows.add(new TuiRow(ROW_MODELS, figure(locale, "system.models.title", modelsSummary(locale)),
                            lines(modelLines(locale)), false, true));
        rows.add(cleanupRow(locale));
        return rows;
    }

    private TuiRow runtimeRow(String locale) {
        var runtime = Objects.requireNonNull(snapshot).runtime();
        var content = MessageCatalogue.format(locale, "system.runtime.since",
                            Map.of("time", clean(runtime.startedAt()),
                                                "uptime", SystemFormatting.uptime(runtime.uptimeSeconds())));
        if (runtime.newerVersion() != null) {
            content += DOT + MessageCatalogue.format(locale, "system.runtime.available",
                                Map.of("version", clean(runtime.newerVersion())));
        }
        var details = new ArrayList<String>();
        details.add(memoryLine(locale, runtime));
        details.add(MessageCatalogue.format(locale, "system.runtime.store",
                            Map.of("size", SystemFormatting.bytes(runtime.machineStoreBytes()),
                                                "free", SystemFormatting.bytes(runtime.diskFreeBytes()),
                                                "time", clean(runtime.measuredAt()))));
        details.add(listenersLine(locale, runtime));
        if (runtime.relaysBehindCount() > 0 && runtime.relaysBehindVersion() != null) {
            details.add(MessageCatalogue.format(locale, "system.runtime.relays_behind",
                                Map.of("count", runtime.relaysBehindCount(),
                                                    "version", clean(runtime.relaysBehindVersion()))));
        }
        return new TuiRow(ROW_RUNTIME, figure(locale, "system.runtime.title", content), lines(details), false, true);
    }

    private static String memoryLine(String locale, RuntimeMetrics runtime) {
        var memory = MessageCatalogue.format(locale, "system.runtime.memory",
                            Map.of("used", SystemFormatting.bytes(runtime.heapUsedBytes()),
                                                "max", SystemFormatting.bytes(runtime.heapMaxBytes()),
                                                "resident", SystemFormatting.bytes(runtime.residentMemoryBytes())));
        var cpu = runtime.cpuPercent() != null && runtime.cpuPercent() >= 0.0
                            ? MessageCatalogue.format(locale, "system.runtime.cpu",
                            Map.of("percent", String.format(Locale.ROOT, "%.0f", runtime.cpuPercent())))
                            : MessageCatalogue.format(locale, "operator.system.runtime.cpu_unavailable", Map.of());
        return memory + DOT + cpu;
    }

    private static String listenersLine(String locale, RuntimeMetrics runtime) {
        var loopback = clean(runtime.loopbackListener());
        var network = clean(runtime.networkListener());
        return MessageCatalogue.format(locale, "system.runtime.listeners",
                            Map.of("loopback", "off".equalsIgnoreCase(loopback) ? "off" : loopback,
                                                "loopback_address", loopback,
                                                "lan", "off".equalsIgnoreCase(network) ? "off" : network,
                                                "lan_address", network,
                                                "devices", runtime.pairedDevicesCount()));
    }

    private TuiRow workRow(String locale) {
        var work = Objects.requireNonNull(snapshot).work();
        var plans = MessageCatalogue.format(locale, "system.work.plans", Map.of()) + "  "
                            + stateCounts(locale, work.plansByState(), PLAN_STATE_ORDER);
        var details = new ArrayList<String>();
        details.add(MessageCatalogue.format(locale, "system.work.epics", Map.of()) + "  "
                            + stateCounts(locale, work.epicsByState(), EPIC_STATE_ORDER));
        details.add(MessageCatalogue.format(locale, "system.work.tasks",
                            Map.of("running", work.tasksRunning(), "queued", work.tasksQueued())));
        details.add(projectsLine(locale, work));
        return new TuiRow(ROW_WORK, figure(locale, "system.work.title", plans), lines(details), false, true);
    }

    private static String projectsLine(String locale, WorkMetrics work) {
        var projects = MessageCatalogue.format(locale, "system.work.projects",
                            Map.of("count", work.activeProjectsCount()));
        if (!work.recoveringProjects().isEmpty()) {
            projects += " (" + String.join(", ", work.recoveringProjects().stream().map(SystemLevel::clean).toList())
                                + " " + MessageCatalogue.format(locale, "system.work.recovering", Map.of()) + ")";
        }
        return projects;
    }

    private static String stateCounts(String locale, Map<String, Integer> states, List<String> order) {
        var parts = new ArrayList<String>();
        for (var state : order) {
            appendState(parts, locale, states, state);
        }
        for (var entry : states.entrySet()) {
            if (!order.contains(entry.getKey())) {
                appendState(parts, locale, states, entry.getKey());
            }
        }
        return parts.isEmpty() ? MessageCatalogue.format(locale, "system.work.none_running", Map.of())
                            : String.join(DOT, parts);
    }

    private static void appendState(List<String> parts, String locale, Map<String, Integer> states, String state) {
        var count = states.get(state);
        if (count != null && count > 0) {
            parts.add(MessageCatalogue.format(locale, "system.work.state_count",
                                Map.of("count", count, "state", stateLabel(locale, state))));
        }
    }

    private static String stateLabel(String locale, String state) {
        var key = "overview.state." + state;
        return MessageCatalogue.hasKey(locale, key)
                            ? MessageCatalogue.format(locale, key, Map.of())
                            : state.replace('_', ' ');
    }

    private String workersSummary(String locale) {
        var workers = Objects.requireNonNull(snapshot).workers();
        long busy = workers.stream().filter(worker -> worker.state() == WorkerState.BUSY).count();
        long idle = workers.stream().filter(worker -> worker.state() == WorkerState.IDLE).count();
        return MessageCatalogue.format(locale, "system.workers.count",
                            Map.of("count", workers.size(), "busy", busy, "idle", idle));
    }

    private List<String> workerLines(String locale) {
        var workers = Objects.requireNonNull(snapshot).workers();
        var lines = new ArrayList<String>(workers.size());
        for (var worker : workers) {
            lines.add(workerLine(locale, worker));
        }
        return lines;
    }

    private static String workerLine(String locale, WorkerMetric worker) {
        var line = String.format(Locale.ROOT, "%-14s%-10s%-12s%-9s", clean(worker.role()),
                            clean(worker.harness()), clean(worker.model()),
                            MessageCatalogue.format(locale, "system.workers.state",
                                                Map.of("state", worker.state().value())));
        var plan = worker.currentPlanTitle();
        return plan == null || plan.isBlank() ? line.stripTrailing() : line + clean(plan);
    }

    private TuiRow jobsRow(String locale) {
        var jobs = Objects.requireNonNull(snapshot).jobs();
        var details = new ArrayList<String>();
        details.add(slotLine(locale, jobs, "build"));
        details.add(slotLine(locale, jobs, "test"));
        var summary = slotLine(locale, jobs, "build") + DOT + slotLine(locale, jobs, "test") + DOT
                            + locksLine(locale, jobs);
        return new TuiRow(ROW_JOBS, figure(locale, "system.jobs.title", summary), lines(details), false, true);
    }

    private static String slotLine(String locale, Map<String, Object> jobs, String kind) {
        var slots = JsonTree.asObject(jobs.get(kind));
        int used = integer(slots, "slots_used");
        int total = integer(slots, "slots_total");
        int queued = integer(slots, "queued");
        return MessageCatalogue.format(locale, "system.jobs.slots",
                            Map.of("kind", kind, "used", used, "total", total, "queued", queued));
    }

    private static int integer(@Nullable Map<String, Object> map, String key) {
        if (map == null) {
            return 0;
        }
        var value = JsonTree.integer(map, key);
        return value != null ? value : 0;
    }

    private static String locksLine(String locale, Map<String, Object> jobs) {
        var locks = JsonTree.integer(jobs, "locks_held");
        return MessageCatalogue.format(locale, "system.jobs.locks", Map.of("count", locks != null ? locks : 0));
    }

    private String modelsSummary(String locale) {
        var models = Objects.requireNonNull(snapshot).models();
        if (models.isEmpty()) {
            return MessageCatalogue.format(locale, "system.models.none", Map.of());
        }
        var totals = totals(models.stream().map(ModelTotals::of).toList());
        return MessageCatalogue.format(locale, "operator.system.models.summary",
                            Map.of("window", windowLabel(locale), "calls", totals.calls(),
                                                "tokens", tokensText(locale, totals)));
    }

    private static String tokensText(String locale, ModelTotals totals) {
        if (totals.unmeasured() > 0 && totals.total() == 0) {
            return MessageCatalogue.format(locale, "common.tokens.not_measured", Map.of());
        }
        return MessageCatalogue.format(locale, "common.tokens.split",
                            Map.of("total", TokenFormat.formatTokens(totals.total(), totals.floor(), locale),
                                                "in", TokenFormat.formatTokens(totals.in(), totals.floor(), locale),
                                                "out", TokenFormat.formatTokens(totals.out(), totals.floor(), locale)));
    }

    private String windowLabel(String locale) {
        var windowKey = switch (window) {
            case HOUR -> "hour";
            case TODAY -> "today";
            case SEVEN_DAYS -> "week";
        };
        return MessageCatalogue.format(locale, "system.models.window", Map.of("window", windowKey));
    }

    private List<String> modelLines(String locale) {
        var models = Objects.requireNonNull(snapshot).models();
        if (models.isEmpty()) {
            return List.of(MessageCatalogue.format(locale, "system.models.none", Map.of()));
        }
        var lines = new ArrayList<String>();
        for (var entry : grouped(models).entrySet()) {
            lines.add(modelLine(locale, entry.getKey(), entry.getValue()));
        }
        return lines;
    }

    private Map<String, List<ModelMetric>> grouped(List<ModelMetric> models) {
        var groups = new LinkedHashMap<String, List<ModelMetric>>();
        for (var model : models) {
            var label = GROUP_ROLE.equals(groupBy) && model.role() != null
                                ? model.role() : model.harnessAndModel();
            groups.computeIfAbsent(clean(label), _ -> new ArrayList<>()).add(model);
        }
        return groups;
    }

    private String modelLine(String locale, String label, List<ModelMetric> models) {
        var totals = totals(models.stream().map(ModelTotals::of).toList());
        var calls = MessageCatalogue.format(locale, "system.models.line",
                            Map.of("calls", totals.calls(), "tokens", tokensText(locale, totals)));
        var line = String.format(Locale.ROOT, "%-22s %s", label, calls);
        if (totals.errors() > 0 || totals.retries() > 0) {
            line += "   " + MessageCatalogue.format(locale, "system.models.errors",
                                Map.of("errors", totals.errors(), "retries", totals.retries()));
        }
        if (totals.unmeasured() > 0) {
            line += "   " + MessageCatalogue.format(locale, "system.models.unmeasured",
                                Map.of("count", totals.unmeasured()));
        }
        return line;
    }

    private static ModelTotals totals(List<ModelTotals> models) {
        int calls = 0;
        long total = 0;
        long in = 0;
        long out = 0;
        int errors = 0;
        int retries = 0;
        int unmeasured = 0;
        boolean floor = false;
        for (var model : models) {
            calls += model.calls();
            total += model.total();
            in += model.in();
            out += model.out();
            errors += model.errors();
            retries += model.retries();
            unmeasured += model.unmeasured();
            floor = floor || model.floor();
        }
        return new ModelTotals(calls, total, in, out, errors, retries, floor, unmeasured);
    }

    private TuiRow cleanupRow(String locale) {
        var cleanup = Objects.requireNonNull(snapshot).cleanup();
        var details = new ArrayList<String>();
        details.add(MessageCatalogue.format(locale, "operator.system.cleanup.kept",
                            Map.of("count", cleanup.keptForGoodCount(),
                                                "time", clean(cleanup.lastSweepAt()))));
        return new TuiRow(ROW_CLEANUP, figure(locale, "system.cleanup.title", cleanupLine(locale, cleanup)),
                            lines(details), false, true);
    }

    private static String cleanupLine(String locale, CleanupSummary cleanup) {
        boolean due = cleanup.plansCount() > 0 || cleanup.epicsCount() > 0 || cleanup.logsCount() > 0
                            || cleanup.reportsCount() > 0 || cleanup.projectsCount() > 0;
        if (!due) {
            return MessageCatalogue.format(locale, "system.cleanup.line_none",
                                Map.of("time", clean(cleanup.nextCheckAt())));
        }
        return MessageCatalogue.format(locale, "system.cleanup.line_due",
                            Map.of("what", cleanupWhat(locale, cleanup),
                                                "size", SystemFormatting.bytes(cleanup.freedBytes())));
    }

    private static String cleanupWhat(String locale, CleanupSummary cleanup) {
        return MessageCatalogue.format(locale, "system.cleanup.summary.what",
                            Map.of("plans", cleanup.plansCount(), "epics", cleanup.epicsCount(),
                                                "logs", cleanup.logsCount(), "reports", cleanup.reportsCount(),
                                                "projects", cleanup.projectsCount()));
    }

    private List<TuiRow> workerRows(String locale) {
        var workers = Objects.requireNonNull(snapshot).workers();
        if (workers.isEmpty()) {
            return List.of(TuiRow.leaf("sys:workers:none",
                                MessageCatalogue.format(locale, "system.workers.none", Map.of())));
        }
        var rows = new ArrayList<TuiRow>(workers.size());
        for (int index = 0; index < workers.size(); index++) {
            rows.add(TuiRow.leaf("sys:workers:" + index, workerLine(locale, workers.get(index))));
        }
        return rows;
    }

    private List<TuiRow> jobRows(String locale) {
        var jobs = Objects.requireNonNull(snapshot).jobs();
        var rows = new ArrayList<TuiRow>(3);
        rows.add(TuiRow.leaf("sys:jobs:build", slotLine(locale, jobs, "build")));
        rows.add(TuiRow.leaf("sys:jobs:test", slotLine(locale, jobs, "test")));
        rows.add(new TuiRow(ROW_LOCKS, locksLine(locale, jobs), null, false, true));
        return rows;
    }

    private List<TuiRow> lockRows(String locale) {
        var jobs = Objects.requireNonNull(snapshot).jobs();
        var locks = JsonTree.integer(jobs, "locks_held");
        int held = locks != null ? locks : 0;
        var line = held == 0
                            ? MessageCatalogue.format(locale, "operator.system.locks.none", Map.of())
                            : locksLine(locale, jobs);
        return List.of(TuiRow.leaf("sys:locks:count", line));
    }

    private List<TuiRow> modelRows(String locale) {
        var models = Objects.requireNonNull(snapshot).models();
        if (models.isEmpty()) {
            return List.of(TuiRow.leaf("sys:models:none",
                                MessageCatalogue.format(locale, "system.models.none", Map.of())));
        }
        var rows = new ArrayList<TuiRow>();
        int index = 0;
        for (var entry : grouped(models).entrySet()) {
            rows.add(TuiRow.leaf("sys:models:" + index++, modelLine(locale, entry.getKey(), entry.getValue())));
        }
        return rows;
    }

    private List<TuiRow> cleanupRows(String locale) {
        var cleanup = Objects.requireNonNull(snapshot).cleanup();
        var rows = new ArrayList<TuiRow>(4);
        rows.add(TuiRow.leaf("sys:cleanup:due", cleanupLine(locale, cleanup)));
        rows.add(TuiRow.leaf("sys:cleanup:freed",
                            MessageCatalogue.format(locale, "system.cleanup.summary.frees",
                                                Map.of("size", SystemFormatting.bytes(cleanup.freedBytes())))));
        rows.add(TuiRow.leaf("sys:cleanup:kept",
                            MessageCatalogue.format(locale, "operator.system.cleanup.kept",
                                                Map.of("count", cleanup.keptForGoodCount(),
                                                                    "time", clean(cleanup.lastSweepAt())))));
        rows.add(TuiRow.leaf("sys:cleanup:next",
                            MessageCatalogue.format(locale, "operator.system.cleanup.next",
                                                Map.of("time", clean(cleanup.nextCheckAt())))));
        return rows;
    }

    private static String figure(String locale, String titleKey, String content) {
        return String.format(Locale.ROOT, LABEL_FORMAT, MessageCatalogue.format(locale, titleKey, Map.of())) + content;
    }

    private static @Nullable String lines(List<String> details) {
        if (details.isEmpty()) {
            return null;
        }
        var prefixed = new ArrayList<String>(details.size());
        for (var detail : details) {
            prefixed.add(DETAIL_INDENT + detail);
        }
        return String.join("\n", prefixed);
    }

    private static String clean(@Nullable String text) {
        return text == null ? "" : OutputSafety.stripControlSequences(text);
    }

    /** The token figures of one model or of a group of models. */
    private record ModelTotals(int calls, long total, long in, long out, int errors, int retries, boolean floor,
    int unmeasured) {

        private static ModelTotals of(ModelMetric model) {
            return new ModelTotals(model.callsCount(), model.tokensTotal(), model.tokensIn(), model.tokensOut(),
                                model.errorsCount(), model.retriesCount(),
                                model.isFloor() || model.unmeasuredCalls() > 0, model.unmeasuredCalls());
        }
    }
}
