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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator projects [--list]}: lists enrolled projects (01-commands.adoc#commands,
 * 17-projects.adoc).
 */
@Command(name = "projects", description = "Explores and lists projects on this machine.")
@OperatorVerb
public final class ProjectsCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec;

    @Option(names = "--list", description = "Prints the projects list into the terminal scrollback.")
    boolean list;

    ProjectsCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var response = context.runtime(true).send(HttpRequest.get("/api/v1/projects"));
        var out = spec.commandLine().getOut();
        if (response.status() == 200) {
            var parsed = JsonTree.parse(response.body());
            if (parsed instanceof List<?> list && !list.isEmpty()) {
                out.println("Projects");
                out.println("─────────────────────────────────────────────────────────────────────────────");
                for (var item : list) {
                    if (item instanceof Map<?, ?> project) {
                        var id = project.get("id");
                        var state = project.get("state");
                        var favourite = Boolean.TRUE.equals(project.get("favourite")) ? "★ " : "  ";
                        var plans = project.get("plans_count");
                        var plansStr = plans != null ? plans + " plans" : "";
                        out.printf("%s%-20s %-12s %s%n", favourite, id != null ? id : "", state != null ? state : "ready", plansStr);
                    } else {
                        out.println("  " + item);
                    }
                }
            } else {
                out.println("No projects enrolled yet. Use 'pm-operator project add' to enrol one.");
            }
        } else {
            out.printf("Failed to list projects: HTTP %d%n", response.status());
        }
        out.flush();
        return 0;
    }
}
