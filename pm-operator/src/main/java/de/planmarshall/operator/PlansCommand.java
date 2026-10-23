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
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiScreenDriver;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator plans}: opens the full-screen overview across projects
 * ({@code doc/design/terminal-client/15-overview.adoc}).
 * Where full-screen is not available, below minimum width, or non-interactive, falls back to
 * the {@link StatusCommand} tree output.
 */
@Command(name = "plans", description = "Shows the overview of running work across projects.")
@OperatorVerb
public final class PlansCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--json", description = "Prints the status as JSON.")
    boolean json;

    @Option(names = "--project", description = "Target project ID")
    @Nullable
    String project;

    @Option(names = {"--by"}, description = "Group by ('plan' or 'epic')")
    @Nullable
    String by;

    @Option(names = {"--by-epic"}, description = "Group plans under their parent epics")
    boolean byEpic;

    @Option(names = "--ended", description = "Show ended work")
    boolean ended;

    PlansCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();

        Terminal terminal = null;
        try {
            terminal = context.createTerminal();
        } catch (IOException _) {
            // terminal unavailable
        }

        try {
            var availability = terminal == null ? null
                                : TuiScreenDriver.availability(context, terminal, TuiLayout.MIN_COLUMNS);
            boolean canOpen = !json && availability != null && availability.available();

            if (canOpen) {
                String effectiveBy = by;
                if (byEpic) {
                    effectiveBy = "epic";
                }
                var app = new OverviewFullScreenApp(context, terminal, project, effectiveBy, ended);
                var result = app.run(out);
                if (result != TuiScreenDriver.Result.TOO_NARROW) {
                    return 0;
                }

                // Below 60 columns: result == TOO_NARROW.
                // app.run(out) wrote operator.overview.too_narrow to terminal.writer().
                return runStatusFallback(terminal);
            }

            if (!json && availability != null) {
                TuiScreenDriver.printUnavailable(context, spec.commandLine().getErr(), availability);
            }
            return delegateToStatus();
        } finally {
            if (context.ownsTerminal(terminal)) {
                terminal.close();
            }
        }
    }

    private int runStatusFallback(Terminal terminal) throws IOException {
        terminal.writer().flush();
        return delegateToStatus();
    }

    private int delegateToStatus() throws IOException {
        boolean overviewAvailable;
        try {
            overviewAvailable = context.runtime(false).send(HttpRequest.get("/api/v1/projects")).status() == 200;
        } catch (IOException _) {
            overviewAvailable = false;
        }

        if (overviewAvailable) {
            var statusCmd = new StatusCommand(context);
            statusCmd.spec = this.spec;
            statusCmd.json = this.json;
            statusCmd.project = this.project;
            statusCmd.by = this.by;
            statusCmd.byEpic = this.byEpic;
            statusCmd.ended = this.ended;
            return statusCmd.call();
        }

        var planCmd = new PlanCommand(context);
        planCmd.spec = this.spec;
        return planCmd.call();
    }
}
