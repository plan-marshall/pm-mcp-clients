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

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.runtime.RuntimeRecord;
import de.planmarshall.api.runtime.RuntimeUnavailableException;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.ModelMetric;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.system.SystemFormatting;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiScreenDriver;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator system}: prints the system view (runtime, work, workers, jobs, models, cleanup);
 * never starts the runtime.
 */
@Command(name = "system", description = "Shows the system view; never starts the runtime.")
public final class SystemCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--project", description = "Target project ID")
    @Nullable
    String project;

    @Option(names = "--window", description = "Time window for metrics: hour, today, 7d")
    @Nullable
    String window;

    @Option(names = "--by", description = "Group metrics by model or role")
    @Nullable
    String by;

    @Option(names = "--watch", description = "Watch updates")
    boolean watch;

    @Option(names = "--json", description = "Output raw JSON")
    boolean json;

    SystemCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var runtimeRecord = RuntimeRecord.read(context.paths().runtimeRecord());
        if (runtimeRecord.isEmpty() || !runtimeRecord.get().processAlive()) {
            out.println("runtime   not running");
            out.flush();
            return 0;
        }

        if (!json && context.isConsoleActive()) {
            var fullScreen = openFullScreen(out);
            if (fullScreen != null) {
                return fullScreen;
            }
        }

        try {
            var runtime = context.runtime(false);
            var query = new ArrayList<String>();
            if (window != null && !window.isBlank()) {
                query.add("window=" + OperatorUrlUtil.encodePathSegment(window));
            }
            if (project != null && !project.isBlank() && !".".equals(project)) {
                query.add("project=" + OperatorUrlUtil.encodePathSegment(project));
            }
            var path = "/api/v1/system" + (query.isEmpty() ? "" : "?" + String.join("&", query));

            var response = runtime.send(HttpRequest.get(path));
            if (response.status() == 200) {
                if (json) {
                    out.println(response.bodyText());
                    out.flush();
                    return 0;
                }
                var snapshot = SystemSnapshot.fromJson(response.bodyText());
                printSnapshot(out, snapshot);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("system", response);
        } catch (RuntimeUnavailableException _) {
            out.println("runtime   not running");
            out.flush();
            return 0;
        }
    }

    /**
     * Opens the system view on the alternate screen where the console can show it. Below the
     * minimum width it prints {@code operator.system.too_narrow} and returns {@code null}, so the
     * caller prints the snapshot instead
     * ({@code doc/design/terminal-client/25-system.adoc#full-screen}).
     *
     * @param out the console's writer
     * @return the exit code when the view ran, or {@code null} to print the snapshot
     */
    private @Nullable Integer openFullScreen(PrintWriter out) {
        Terminal terminal = null;
        try {
            terminal = context.createTerminal();
        } catch (IOException _) {
            return null;
        }
        try {
            var availability = TuiScreenDriver.availability(context, terminal, TuiLayout.MIN_COLUMNS);
            if (!availability.available()) {
                TuiScreenDriver.printUnavailable(context, spec.commandLine().getErr(), availability);
                return null;
            }
            var app = new OverviewFullScreenApp(context, terminal,
                                OverviewFullScreenApp.InitialSelection.system(window, by, resolvedProject()),
                                Clock.systemUTC(), new OverviewStateStore(context.stateDirectory()),
                                context.noticeBuffer());
            app.run(out);
            return 0;
        } catch (IOException _) {
            return null;
        } finally {
            if (context.ownsTerminal(terminal)) {
                try {
                    terminal.close();
                } catch (IOException _) {
                    // The terminal is gone; the snapshot is printed all the same.
                }
            }
        }
    }

    private @Nullable String resolvedProject() {
        return project == null || project.isBlank() || ".".equals(project) ? null : project;
    }

    void printSnapshot(PrintWriter out, SystemSnapshot snapshot) {
        out.println("System");
        printRuntime(out, snapshot.runtime());
        printWork(out, snapshot.work());
        printWorkers(out, snapshot.workers());
        printJobs(out, snapshot.jobs());
        printModels(out, snapshot.models(), snapshot.window());
        printCleanup(out, snapshot.cleanup());
        out.println("plans · cleanup · system --window today · system --by role");
    }

    private void printRuntime(PrintWriter out, RuntimeMetrics r) {
        var version = OutputSafety.stripControlSequences(r.version());
        var commit = OutputSafety.stripControlSequences(r.build());
        var avail = r.newerVersion() != null
                            ? " · " + OutputSafety.stripControlSequences(r.newerVersion()) + " available: runtime upgrade"
                            : "";
        out.printf("Runtime   pm-mcpd %s · build %s%s%n", version, commit, avail);

        var since = OutputSafety.stripControlSequences(r.startedAt());
        long upSec = r.uptimeSeconds();
        long hours = upSec / 3600;
        long minutes = (upSec % 3600) / 60;
        var uptimeStr = hours > 0 ? hours + " h " + minutes + " min" : minutes + " min";
        out.printf("          running since %s · %s%n", since, uptimeStr);

        var memUsed = formatBytes(r.heapUsedBytes());
        var memMax = formatBytes(r.heapMaxBytes());
        var memResident = formatBytes(r.residentMemoryBytes());
        var cpuStr = r.cpuPercent() != null && r.cpuPercent() >= 0.0
                            ? String.format(Locale.ROOT, "CPU %.0f %%", r.cpuPercent())
                            : "CPU not available (not measured on this platform)";
        out.printf("          memory %s in use of %s · %s resident · %s%n", memUsed, memMax, memResident, cpuStr);

        var storeUsed = formatBytes(r.machineStoreBytes());
        var storeFree = formatBytes(r.diskFreeBytes());
        var measured = OutputSafety.stripControlSequences(r.measuredAt());
        out.printf("          machine store %s · %s free on its disk · measured %s%n", storeUsed, storeFree, measured);

        var loopback = OutputSafety.stripControlSequences(r.loopbackListener());
        var net = OutputSafety.stripControlSequences(r.networkListener());
        var netPart = "off".equalsIgnoreCase(net)
                            ? "network off"
                            : String.format(Locale.ROOT, "network %s (%d %s)", net, r.pairedDevicesCount(),
                            r.pairedDevicesCount() == 1 ? "device" : "devices");
        out.printf("          listeners  socket · loopback %s · %s%n", loopback, netPart);

        if (r.relaysBehindVersion() != null && r.relaysBehindCount() > 0) {
            out.printf("          %d host sessions run pm-mcp %s · pm-operator client update%n",
                                r.relaysBehindCount(), OutputSafety.stripControlSequences(r.relaysBehindVersion()));
        }
    }

    private void printWork(PrintWriter out, WorkMetrics w) {
        var planParts = formatStateCounts(w.plansByState(), List.of("running", "waiting_for_you", "held", "paused"));
        out.printf("Work      plans    %s%n", planParts.isEmpty() ? "none running" : planParts);

        var epicParts = formatStateCounts(w.epicsByState(), List.of("running", "waiting_for_you", "held", "paused"));
        out.printf("          epics    %s%n", epicParts.isEmpty() ? "none running" : epicParts);

        out.printf("          tasks    %d running · %d queued%n", w.tasksRunning(), w.tasksQueued());
        var prjSuffix = w.activeProjectsCount() + " with running work";
        if (w.recoveringProjects() != null && !w.recoveringProjects().isEmpty()) {
            prjSuffix += " (recovering: " + String.join(", ", w.recoveringProjects()) + ")";
        }
        out.printf("          projects %s%n", prjSuffix);
    }

    private String formatStateCounts(Map<String, Integer> map, List<String> preferredOrder) {
        var parts = new ArrayList<String>();
        for (var state : preferredOrder) {
            var count = map.get(state);
            if (count != null && count > 0) {
                parts.add(count + " " + state.replace('_', ' '));
            }
        }
        for (var entry : map.entrySet()) {
            if (!preferredOrder.contains(entry.getKey()) && entry.getValue() > 0) {
                parts.add(entry.getValue() + " " + entry.getKey().replace('_', ' '));
            }
        }
        return String.join(" · ", parts);
    }

    private void printWorkers(PrintWriter out, List<WorkerMetric> workers) {
        long busyCount = workers.stream().filter(w -> "BUSY".equalsIgnoreCase(w.state().name())).count();
        long idleCount = workers.stream().filter(w -> "IDLE".equalsIgnoreCase(w.state().name())).count();
        out.printf("Workers   %d · %d busy · %d idle%n", workers.size(), busyCount, idleCount);

        for (var w : workers) {
            var role = OutputSafety.stripControlSequences(w.role());
            var harness = OutputSafety.stripControlSequences(w.harness());
            var model = OutputSafety.stripControlSequences(w.model());
            var state = w.state().name().toLowerCase(Locale.ROOT);
            var item = w.currentPlanTitle() != null ? OutputSafety.stripControlSequences(w.currentPlanTitle()) : "";
            if (item.isEmpty()) {
                out.printf("          %-14s%-8s%-12s%s%n", role, harness, model, state);
            } else {
                out.printf("          %-14s%-8s%-12s%-6s%s%n", role, harness, model, state, item);
            }
        }
    }

    private void printJobs(PrintWriter out, Map<String, Object> jobs) {
        var build = JsonTree.asObject(jobs.get("build"));
        var test = JsonTree.asObject(jobs.get("test"));
        var locksHeld = JsonTree.integer(jobs, "locks_held");

        var buildStr = "build  0 of 0 slots";
        if (build != null) {
            int used = JsonTree.integer(build, "slots_used") != null ? JsonTree.integer(build, "slots_used") : 0;
            int total = JsonTree.integer(build, "slots_total") != null ? JsonTree.integer(build, "slots_total") : 0;
            int queued = JsonTree.integer(build, "queued") != null ? JsonTree.integer(build, "queued") : 0;
            buildStr = String.format(Locale.ROOT, "build  %d of %d %s%s", used, total,
                                total == 1 ? "slot" : "slots",
                                queued > 0 ? " · " + queued + " queued" : "");
        }

        var testStr = "test  0 of 0 slots";
        if (test != null) {
            int used = JsonTree.integer(test, "slots_used") != null ? JsonTree.integer(test, "slots_used") : 0;
            int total = JsonTree.integer(test, "slots_total") != null ? JsonTree.integer(test, "slots_total") : 0;
            testStr = String.format(Locale.ROOT, "test  %d of %d %s", used, total,
                                total == 1 ? "slot" : "slots");
        }

        out.printf("Jobs      %-31s %s%n", buildStr, testStr);
        out.printf("          locks  %d held%n", locksHeld != null ? locksHeld : 0);
    }

    private void printModels(PrintWriter out, List<ModelMetric> models, SystemWindow win) {
        var winStr = switch (win) {
            case TODAY -> "today";
            case SEVEN_DAYS -> "7 days";
            default -> "last hour";
        };
        var groupStr = "role".equalsIgnoreCase(by) ? "role" : "model";
        out.printf("Models    %s, per %s%n", winStr, groupStr);

        if (models.isEmpty()) {
            out.println("          No model calls in this window.");
            return;
        }

        for (var m : models) {
            var id = OutputSafety.stripControlSequences(m.harnessAndModel());
            var calls = m.callsCount();
            String tokenPart;
            if (m.unmeasuredCalls() > 0 && m.tokensTotal() == 0L) {
                var harness = id.contains(":") ? id.substring(0, id.indexOf(':')) : id;
                tokenPart = "tokens not measured: not reported by " + harness;
            } else {
                var tot = formatTokens(m.tokensTotal());
                var in = formatTokens(m.tokensIn());
                var outTok = formatTokens(m.tokensOut());
                tokenPart = String.format(Locale.ROOT, "%s tokens (%s in · %s out)", tot, in, outTok);
            }

            var extra = new ArrayList<String>();
            if (m.retriesCount() > 0) {
                extra.add(m.retriesCount() == 1 ? "1 retry" : m.retriesCount() + " retries");
            }
            if (m.errorsCount() > 0) {
                extra.add(m.errorsCount() == 1 ? "1 error" : m.errorsCount() + " errors");
            }
            var extraStr = extra.isEmpty() ? "" : "   " + String.join("   ", extra);

            out.printf("          %-20s %3d calls   %s%s%n", id, calls, tokenPart, extraStr);
        }
    }

    private void printCleanup(PrintWriter out, CleanupSummary c) {
        if (c.plansCount() > 0 || c.epicsCount() > 0 || c.logsCount() > 0) {
            var sizeStr = formatBytes(c.freedBytes());
            out.printf("Cleanup   due: %d plans · %d epics · %d logs · frees %s%n",
                                c.plansCount(), c.epicsCount(), c.logsCount(), sizeStr);
            out.printf("          %d kept for good · last sweep %s%n",
                                c.keptForGoodCount(), OutputSafety.stripControlSequences(c.lastSweepAt()));
        } else {
            out.printf("Cleanup   nothing due · next check %s%n",
                                OutputSafety.stripControlSequences(c.nextCheckAt()));
            out.printf("          %d kept for good · last sweep %s%n",
                                c.keptForGoodCount(), OutputSafety.stripControlSequences(c.lastSweepAt()));
        }
    }

    public static String formatBytes(long bytes) {
        return SystemFormatting.bytes(bytes);
    }

    public static String formatTokens(long tokens) {
        return SystemFormatting.tokens(tokens);
    }
}
