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
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator plans}: opens the overview of running work across projects.
 * Where the full-screen view is not available or non-interactive, prints the status tree.
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

    @Option(names = "--watch", description = "Watch status updates")
    boolean watch;

    PlansCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
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
            statusCmd.watch = this.watch;
            return statusCmd.call();
        }

        var planCmd = new PlanCommand(context);
        planCmd.spec = this.spec;
        return planCmd.call();
    }
}
