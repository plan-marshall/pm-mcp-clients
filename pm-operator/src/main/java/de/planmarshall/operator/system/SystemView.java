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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.ModelMetric;
import de.planmarshall.api.system.RuntimeLifecycleState;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.RuntimeStatus;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.api.system.WorkerState;
import de.planmarshall.operator.OutputSafety;

/**
 * The one formatter of the system view: it turns the snapshot the runtime served into the six
 * figures, and both views print those figures
 * ({@code doc/design/terminal-client/25-system.adoc#print}, {@code #full-screen}).
 *
 * <p>The printed view ({@code SystemCommand}) and the full-screen view ({@link SystemLevel}) render
 * from the same {@link Figure} list, so listeners, token units, and every figure read the same in
 * both. The model is the record of the wire read, {@code de.planmarshall.api.system.SystemSnapshot};
 * this class holds no second one.
 *
 * <p>Every name in a figure comes from the runtime and is shown as text: {@link OutputSafety}
 * removes control sequences before it reaches a view.
 */
public final class SystemView {

    /** The separator between two facts of one line. */
    public static final String DOT = " \u00b7 ";

    /** The column of a figure's label in the printed view. */
    private static final String LABEL_FORMAT = "%-9s ";

    /** The indent of a detail line in the printed view. */
    private static final String PRINTED_INDENT = "          ";

    /** The indent of a detail line in the full-screen view. */
    public static final String DETAIL_INDENT = "      ";

    /** The grouping of the models per model. */
    public static final String GROUP_MODEL = "model";

    /** The grouping of the models per role. */
    public static final String GROUP_ROLE = "role";

    private static final List<String> PLAN_STATE_ORDER =
                        List.of("running", "waiting_for_you", "held", "paused", "pausing");
    private static final List<String> EPIC_STATE_ORDER =
                        List.of("running", "waiting_for_you", "held", "paused");

    private static final String KINDS_LOCK = "locks_held";

    /**
     * One figure of the view: its label, its line, and the detail lines printed below it.
     *
     * @param label           the localized label of the figure
     * @param line            the figure's line, without the label
     * @param details         the detail lines, in the order the design lists them
     * @param printedHeadline what only the printed view states before the line, or {@code null}
     */
    public record Figure(String label, String line, List<String> details,
    @Nullable String printedHeadline) {

        /**
         * @param label   the localized label of the figure
         * @param line    the figure's line
         * @param details the detail lines
         */
        public Figure {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(line, "line");
            details = details == null ? List.of() : List.copyOf(details);
        }

        /**
         * @param label   the localized label of the figure
         * @param line    the figure's line
         * @param details the detail lines
         */
        public Figure(String label, String line, List<String> details) {
            this(label, line, details, null);
        }
    }

    private SystemView() {
    }

    /**
     * @param snapshot the snapshot the runtime served
     * @param locale   the interface locale
     * @param window   the models' window
     * @param groupBy  {@code model} or {@code role}
     * @return the six figures in the order the design lists them
     */
    public static List<Figure> root(SystemSnapshot snapshot, String locale, SystemWindow window,
                        @Nullable String groupBy) {
        Objects.requireNonNull(snapshot, "snapshot");
        var rows = new ArrayList<Figure>(6);
        rows.add(runtime(snapshot.runtime(), locale));
        rows.add(work(snapshot.work(), locale));
        rows.add(workers(snapshot.workers(), locale));
        rows.add(jobs(snapshot.jobs(), locale));
        rows.add(models(snapshot.models(), locale, window, groupBy));
        rows.add(cleanup(snapshot.cleanup(), locale));
        return rows;
    }

    /**
     * @param snapshot the snapshot the runtime served
     * @param locale   the interface locale
     * @param groupBy  {@code model} or {@code role}
     * @return the printed view's body, one line per figure line and one per detail
     */
    public static List<String> printedLines(SystemSnapshot snapshot, String locale, @Nullable String groupBy) {
        var lines = new ArrayList<String>();
        for (var figure : root(snapshot, locale, snapshot.window(), groupBy)) {
            var line = String.format(Locale.ROOT, LABEL_FORMAT, figure.label());
            if (figure.printedHeadline() != null) {
                line += figure.printedHeadline() + DOT;
            }
            lines.add(line + figure.line());
            for (var detail : figure.details()) {
                lines.add(PRINTED_INDENT + detail);
            }
        }
        return lines;
    }

    /**
     * @param figure the figure
     * @return the figure's row as the full-screen view draws it
     */
    public static String rowLine(Figure figure) {
        return String.format(Locale.ROOT, LABEL_FORMAT, figure.label()) + figure.line();
    }

    /**
     * @param figure the figure
     * @return the figure's detail block as the full-screen view draws it, or {@code null} for none
     */
    public static @Nullable String rowDetails(Figure figure) {
        if (figure.details().isEmpty()) {
            return null;
        }
        var prefixed = new ArrayList<String>(figure.details().size());
        for (var detail : figure.details()) {
            prefixed.add(DETAIL_INDENT + detail);
        }
        return String.join("\n", prefixed);
    }

    /**
     * The lifecycle state the view states first, in both views
     * ({@code doc/design/shared/system.adoc#lifecycle}): {@code draining · 2 plans finish},
     * {@code stopping}, {@code upgrading}, or {@code stopped}.
     *
     * @param locale the interface locale
     * @param status the runtime's status, or {@code null} when it could not be read
     * @return the line, or {@code null} while the runtime runs or its state is not known
     */
    public static @Nullable String lifecycleLine(String locale, @Nullable RuntimeStatus status) {
        if (status == null || status.state() == RuntimeLifecycleState.RUNNING) {
            return null;
        }
        var state = MessageCatalogue.format(locale, "system.lifecycle." + status.state().value(), Map.of());
        // The count is what the runtime still holds while it drains; the later states stand alone.
        var finish = status.state() == RuntimeLifecycleState.DRAINING
                            ? Math.max(0, status.drainRemainingCount()) : 0;
        return MessageCatalogue.format(locale, "system.lifecycle.line", Map.of("state", state, "finish", finish));
    }

    /**
     * @param runtime the runtime's figures
     * @param locale  the interface locale
     * @return the runtime figure
     */
    public static Figure runtime(RuntimeMetrics runtime, String locale) {
        var content = MessageCatalogue.format(locale, "system.runtime.since",
                            Map.of("time", clean(runtime.startedAt()),
                                                "uptime", SystemFormatting.uptime(runtime.uptimeSeconds())));
        if (runtime.newerVersion() != null) {
            content += DOT + MessageCatalogue.format(locale, "system.runtime.available",
                                Map.of("version", clean(runtime.newerVersion())));
        }
        var details = new ArrayList<String>(4);
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
        var headline = MessageCatalogue.format(locale, "system.runtime.version_build",
                            Map.of("version", clean(runtime.version()), "build", clean(runtime.build())));
        return new Figure(MessageCatalogue.format(locale, "system.runtime.title", Map.of()), content, details,
                            headline);
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
        return MessageCatalogue.format(locale, "system.runtime.listeners_console",
                            Map.of("loopback", loopback, "loopback_address", loopback,
                                                "lan", network, "lan_address", network,
                                                "devices", runtime.pairedDevicesCount()));
    }

    /**
     * @param work   the work figures
     * @param locale the interface locale
     * @return the work figure
     */
    public static Figure work(WorkMetrics work, String locale) {
        var plans = MessageCatalogue.format(locale, "system.work.plans", Map.of()) + "  "
                            + stateCounts(locale, work.plansByState(), PLAN_STATE_ORDER);
        var details = new ArrayList<String>(3);
        details.add(MessageCatalogue.format(locale, "system.work.epics", Map.of()) + "  "
                            + stateCounts(locale, work.epicsByState(), EPIC_STATE_ORDER));
        details.add(MessageCatalogue.format(locale, "system.work.tasks_console",
                            Map.of("running", work.tasksRunning(), "queued", work.tasksQueued())));
        details.add(projectsLine(locale, work));
        return new Figure(MessageCatalogue.format(locale, "system.work.title", Map.of()), plans, details);
    }

    private static String projectsLine(String locale, WorkMetrics work) {
        var projects = MessageCatalogue.format(locale, "system.work.projects",
                            Map.of("count", work.activeProjectsCount()));
        if (!work.recoveringProjects().isEmpty()) {
            projects += " (" + String.join(", ", work.recoveringProjects().stream()
                                .map(SystemView::clean).toList())
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

    private static void appendState(List<String> parts, String locale, Map<String, Integer> states,
                        String state) {
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

    /**
     * @param workers the machine's workers
     * @param locale  the interface locale
     * @return the workers figure
     */
    public static Figure workers(List<WorkerMetric> workers, String locale) {
        long busy = workers.stream().filter(worker -> worker.state() == WorkerState.BUSY).count();
        long idle = workers.stream().filter(worker -> worker.state() == WorkerState.IDLE).count();
        var summary = MessageCatalogue.format(locale, "system.workers.count",
                            Map.of("count", workers.size(), "busy", busy, "idle", idle));
        return new Figure(MessageCatalogue.format(locale, "system.workers.title", Map.of()), summary,
                            workerLines(locale, workers));
    }

    /**
     * @param locale  the interface locale
     * @param workers the machine's workers
     * @return one line per worker
     */
    public static List<String> workerLines(String locale, List<WorkerMetric> workers) {
        var lines = new ArrayList<String>(workers.size());
        for (var worker : workers) {
            lines.add(workerLine(locale, worker));
        }
        return lines;
    }

    /**
     * @param locale the interface locale
     * @param worker the worker
     * @return the worker's line
     */
    public static String workerLine(String locale, WorkerMetric worker) {
        var line = String.format(Locale.ROOT, "%-14s%-10s%-12s%-12s", clean(worker.role()),
                            clean(worker.harness()), clean(worker.model()),
                            MessageCatalogue.format(locale, "system.workers.state",
                                                Map.of("state", worker.state().value())));
        var plan = worker.currentPlanTitle();
        return plan == null || plan.isBlank() ? line.stripTrailing() : line + clean(plan);
    }

    /**
     * The jobs figure: one line per job kind, and the locks. Each fact is stated once, on its own
     * line ({@code doc/design/shared/system.adoc#jobs}).
     *
     * @param jobs   the job kinds and the locks the runtime holds
     * @param locale the interface locale
     * @return the jobs figure
     */
    public static Figure jobs(Map<String, Object> jobs, String locale) {
        return new Figure(MessageCatalogue.format(locale, "system.jobs.title", Map.of()),
                            locksLine(locale, jobs), jobKindLines(locale, jobs));
    }

    /**
     * @param locale the interface locale
     * @param jobs   the job kinds and the locks
     * @return one detail line per job kind, in the runtime's order
     */
    public static List<String> jobKindLines(String locale, Map<String, Object> jobs) {
        var lines = new ArrayList<String>(jobs.size());
        for (var kind : jobKinds(jobs)) {
            lines.add(MessageCatalogue.format(locale, "system.jobs.slots_console",
                                Map.of("kind", clean(kind), "used", integer(jobs, kind, "slots_used"),
                                                    "total", integer(jobs, kind, "slots_total"),
                                                    "queued", integer(jobs, kind, "queued"))));
        }
        return lines;
    }

    /**
     * @param jobs the job kinds and the locks
     * @return the kinds that carry slots, in a stable order
     */
    public static List<String> jobKinds(Map<String, Object> jobs) {
        var kinds = new ArrayList<String>(jobs.size());
        for (var entry : jobs.entrySet()) {
            if (!KINDS_LOCK.equals(entry.getKey()) && JsonTree.asObject(entry.getValue()) != null) {
                kinds.add(entry.getKey());
            }
        }
        // The runtime may serve the kinds in any order; the view states them in the same one every
        // time, so a redrawn figure does not move its lines.
        kinds.sort(String::compareTo);
        return kinds;
    }

    /**
     * @param locale the interface locale
     * @param jobs   the job kinds and the locks
     * @return the locks line
     */
    public static String locksLine(String locale, Map<String, Object> jobs) {
        var locks = JsonTree.integer(jobs, KINDS_LOCK);
        return MessageCatalogue.format(locale, "system.jobs.locks", Map.of("count", locks != null ? locks : 0));
    }

    private static int integer(Map<String, Object> jobs, String kind, String field) {
        var slots = JsonTree.asObject(jobs.get(kind));
        if (slots == null) {
            return 0;
        }
        var value = JsonTree.integer(slots, field);
        return value != null ? value : 0;
    }

    /**
     * @param models  the models' calls in the window
     * @param locale  the interface locale
     * @param window  the models' window
     * @param groupBy {@code model} or {@code role}
     * @return the models figure
     */
    public static Figure models(List<ModelMetric> models, String locale, SystemWindow window,
                        @Nullable String groupBy) {
        var summary = modelsSummary(models, locale, window, groupBy);
        var details = models.isEmpty() ? List.<String>of() : modelLines(locale, models, groupBy);
        return new Figure(MessageCatalogue.format(locale, "system.models.title", Map.of()), summary, details);
    }

    /**
     * @param locale  the interface locale
     * @param models  the models' calls in the window
     * @param groupBy {@code model} or {@code role}
     * @return one line per model or per role
     */
    public static List<String> modelLines(String locale, List<ModelMetric> models, @Nullable String groupBy) {
        var lines = new ArrayList<String>();
        for (var entry : grouped(models, groupBy).entrySet()) {
            lines.add(modelLine(locale, entry.getKey(), entry.getValue()));
        }
        return lines;
    }

    private static String modelsSummary(List<ModelMetric> models, String locale, SystemWindow window,
                        @Nullable String groupBy) {
        if (models.isEmpty()) {
            return MessageCatalogue.format(locale, "system.models.none", Map.of());
        }
        var totals = totals(models.stream().map(ModelTotals::of).toList());
        var windowLabel = windowLabel(locale, window);
        var grouping = MessageCatalogue.format(locale,
                            GROUP_ROLE.equals(groupBy)
                                                ? "system.models.grouping.role" : "system.models.grouping.model",
                            Map.of());
        if (totals.total() == 0 && totals.unmeasured() > 0) {
            // The reason is a fact of the line it belongs to; the figure states no second "not measured".
            return MessageCatalogue.format(locale, "operator.system.models.summary_no_tokens",
                                Map.of("window", windowLabel, "by", grouping, "calls", totals.calls()));
        }
        return MessageCatalogue.format(locale, "operator.system.models.summary",
                            Map.of("window", windowLabel, "by", grouping, "calls", totals.calls(),
                                                "tokens", tokensText(locale, totals)));
    }

    /**
     * @param locale the interface locale
     * @param label  the model's or the role's label
     * @param models the models of that label
     * @return the models' line
     */
    public static String modelLine(String locale, String label, List<ModelMetric> models) {
        var totals = totals(models.stream().map(ModelTotals::of).toList());
        var calls = MessageCatalogue.format(locale, "system.models.line",
                            Map.of("calls", totals.calls(), "tokens", modelTokensText(locale, totals, models)));
        var line = String.format(Locale.ROOT, "%-22s %s", label, calls);
        var extras = new ArrayList<String>(2);
        if (totals.errors() > 0) {
            extras.add(MessageCatalogue.format(locale, "system.models.error", Map.of("count", totals.errors())));
        }
        if (totals.retries() > 0) {
            extras.add(MessageCatalogue.format(locale, "system.models.retry", Map.of("count", totals.retries())));
        }
        if (totals.unmeasured() > 0 && totals.total() > 0) {
            // The line's total is a floor: the calls left out are counted once, beside the floor.
            extras.add(MessageCatalogue.format(locale, "system.models.unmeasured",
                                Map.of("count", totals.unmeasured())));
        }
        if (!extras.isEmpty()) {
            line += "   " + String.join(DOT, extras);
        }
        return line;
    }

    private static String modelTokensText(String locale, ModelTotals totals, List<ModelMetric> models) {
        if (totals.total() == 0 && totals.unmeasured() > 0) {
            return MessageCatalogue.format(locale, "system.models.not_measured",
                                Map.of("model", harnessOf(models)));
        }
        return tokensText(locale, totals);
    }

    /**
     * @param models the models of one line
     * @return the harness that reported no tokens, taken from the model itself so that a line
     *         grouped by role still names the harness and not the role
     */
    private static String harnessOf(List<ModelMetric> models) {
        return String.join(", ", models.stream()
                            .filter(model -> model.unmeasuredCalls() > 0)
                            .map(model -> clean(model.harnessAndModel()))
                            .map(name -> name.indexOf(':') > 0 ? name.substring(0, name.indexOf(':')) : name)
                            .distinct()
                            .toList());
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

    private static String windowLabel(String locale, SystemWindow window) {
        var windowKey = switch (window) {
            case HOUR -> "hour";
            case TODAY -> "today";
            case SEVEN_DAYS -> "week";
        };
        return MessageCatalogue.format(locale, "system.models.window", Map.of("window", windowKey));
    }

    private static Map<String, List<ModelMetric>> grouped(List<ModelMetric> models, @Nullable String groupBy) {
        var groups = new LinkedHashMap<String, List<ModelMetric>>();
        for (var model : models) {
            var label = GROUP_ROLE.equals(groupBy) && model.role() != null
                                ? model.role() : model.harnessAndModel();
            groups.computeIfAbsent(clean(label), _ -> new ArrayList<>()).add(model);
        }
        return groups;
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

    /**
     * @param cleanup the cleanup's figures
     * @param locale  the interface locale
     * @return the cleanup figure
     */
    public static Figure cleanup(CleanupSummary cleanup, String locale) {
        return new Figure(MessageCatalogue.format(locale, "system.cleanup.title", Map.of()),
                            cleanupLine(locale, cleanup), cleanupDetails(locale, cleanup));
    }

    /**
     * @param locale  the interface locale
     * @param cleanup the cleanup's figures
     * @return the cleanup's due line
     */
    public static String cleanupLine(String locale, CleanupSummary cleanup) {
        boolean due = cleanup.plansCount() > 0 || cleanup.epicsCount() > 0 || cleanup.logsCount() > 0
                            || cleanup.reportsCount() > 0 || cleanup.projectsCount() > 0;
        if (!due) {
            return MessageCatalogue.format(locale, "system.cleanup.line_none",
                                Map.of("time", clean(cleanup.nextCheckAt())));
        }
        return MessageCatalogue.format(locale, "system.cleanup.line_due",
                            Map.of("what", MessageCatalogue.format(locale, "system.cleanup.summary.what",
                                                                    Map.of("plans", cleanup.plansCount(), "epics", cleanup.epicsCount(),
                                                                                        "logs", cleanup.logsCount(),
                                                                                        "reports", cleanup.reportsCount(),
                                                                                        "projects", cleanup.projectsCount())),
                                                "size", SystemFormatting.bytes(cleanup.freedBytes())));
    }

    /**
     * @param locale  the interface locale
     * @param cleanup the cleanup's figures
     * @return the cleanup's kept-for-good line
     */
    public static String keptLine(String locale, CleanupSummary cleanup) {
        return MessageCatalogue.format(locale, "operator.system.cleanup.kept",
                            Map.of("count", cleanup.keptForGoodCount(), "time", clean(cleanup.lastSweepAt())));
    }

    /**
     * @param locale  the interface locale
     * @param cleanup the cleanup's figures
     * @return the cleanup's detail lines
     */
    public static List<String> cleanupDetails(String locale, CleanupSummary cleanup) {
        var details = new ArrayList<String>(1);
        details.add(keptLine(locale, cleanup));
        return details;
    }

    /**
     * What the runtime runs, as the confirmation of an act states it
     * ({@code doc/design/shared/system.adoc#stop}). One derivation for the dialogue and for the
     * view's figures.
     *
     * @param locale   the interface locale
     * @param snapshot the snapshot the runtime served
     * @return the counts, each group left out where it is zero
     */
    public static String runningNow(String locale, SystemSnapshot snapshot) {
        var what = MessageCatalogue.format(locale, "system.confirm.what",
                            Map.of("plans", runningPlans(snapshot.work().plansByState()),
                                                "epics", runningPlans(snapshot.work().epicsByState()),
                                                "tasks", snapshot.work().tasksRunning(),
                                                "jobs", usedSlots(snapshot.jobs()),
                                                "workers", snapshot.workers().size()));
        // The counts are one ICU message, so a zero group leaves its separator behind.
        var separator = DOT.strip();
        while (what.startsWith(separator)) {
            what = what.substring(separator.length()).strip();
        }
        return what;
    }

    private static int runningPlans(Map<String, Integer> byState) {
        var count = byState.get("running");
        return count != null ? count : 0;
    }

    private static int usedSlots(Map<String, Object> jobs) {
        var used = 0;
        for (var kind : jobKinds(jobs)) {
            used += integer(jobs, kind, "slots_used");
        }
        return used;
    }

    /**
     * @param text a name the runtime served
     * @return the text without control sequences
     */
    public static String clean(@Nullable String text) {
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
