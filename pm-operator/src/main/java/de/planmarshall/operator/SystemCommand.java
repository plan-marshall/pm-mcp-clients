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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.runtime.RuntimeRecord;
import de.planmarshall.api.runtime.RuntimeUnavailableException;
import de.planmarshall.api.system.RuntimeStatus;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.system.SystemFormatting;
import de.planmarshall.operator.system.SystemView;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiScreenDriver;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator system}: prints the system view (runtime, work, workers, jobs, models,
 * cleanup); never starts the runtime.
 *
 * <p>The printed view and the full-screen view render from one snapshot through one formatter,
 * {@link SystemView} ({@code doc/design/terminal-client/25-system.adoc#print}).
 */
@Command(name = "system", description = "Shows the system view; never starts the runtime.")
public final class SystemCommand implements Callable<Integer> {

    private static final String STATUS_PATH = "/api/v1/status";
    private static final String FOOTER = "plans · cleanup · system --window today · system --by role";

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
            out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                "operator.system.not_running", Map.of()));
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
            out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                "operator.system.not_running", Map.of()));
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

    /**
     * Prints the view once: the lifecycle state first while there is one, then the six figures the
     * full-screen view draws, then the commands that lead on.
     *
     * @param out      the verb's output
     * @param snapshot the snapshot the runtime served
     */
    void printSnapshot(PrintWriter out, SystemSnapshot snapshot) {
        var locale = context.clientEnvironment().locale();
        var lifecycle = SystemView.lifecycleLine(locale, readStatus());
        if (lifecycle != null) {
            out.println(lifecycle);
        }
        out.println(MessageCatalogue.format(locale, "system.view.title", Map.of()));
        for (var line : SystemView.printedLines(snapshot, locale, by)) {
            out.println(line);
        }
        out.println(FOOTER);
    }

    /**
     * Reads the lifecycle state the view states first. The status is read beside the snapshot and
     * never turns a view that was served into a failure.
     *
     * @return the runtime's status, or {@code null} when it could not be read
     */
    private @Nullable RuntimeStatus readStatus() {
        try {
            var response = context.runtime(false).send(HttpRequest.get(STATUS_PATH));
            return response.status() == 200 ? RuntimeStatus.fromJson(response.bodyText()) : null;
        } catch (IOException _) {
            return null;
        }
    }

    /**
     * @param bytes a size in bytes
     * @return the size with its largest unit
     */
    public static String formatBytes(long bytes) {
        return SystemFormatting.bytes(bytes);
    }
}
