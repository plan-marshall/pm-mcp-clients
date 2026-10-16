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
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator project}: manages a project's enrolment and lifecycle (01-commands.adoc#commands,
 * 17-projects.adoc).
 */
@Command(name = "project", description = "Manages project enrolments and lifecycle.",
                          subcommands = {
                                                    ProjectCommand.AddCommand.class,
                                                    ProjectCommand.SetupCommand.class,
                                                    ProjectCommand.FavouriteCommand.class,
                                                    ProjectCommand.ReleaseCommand.class,
                                                    ProjectCommand.UnenrolCommand.class
                          })
@OperatorVerb
public final class ProjectCommand implements Callable<Integer> {

    private static final String PROJECTS_API = "/api/v1/projects/";

    @Spec
    CommandSpec spec = CommandSpec.create();

    ProjectCommand(OperatorContext context) {
        Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    @Command(name = "add", description = "Onboards a project from the specified or current directory.")
    @OperatorVerb
    static final class AddCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", arity = "0..1", defaultValue = ".", description = "Working directory or checkout path")
        String path = ".";

        AddCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            body.put("path", path);
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/projects", JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 201) {
                out.println("✓ Project onboarding started for " + path);
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.printf("Failed to onboard project at %s: HTTP %d%n", path, response.status());
            err.flush();
            return 1;
        }
    }

    @Command(name = "setup", description = "Continues a project's onboarding setup.")
    @OperatorVerb
    static final class SetupCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Project ID")
        String projectId = "";

        @Parameters(index = "1", arity = "0..1", description = "Setup step name or number")
        @Nullable
        String step;

        SetupCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var path = PROJECTS_API + OperatorUrlUtil.encodePathSegment(projectId) + "/setup";
            var body = new LinkedHashMap<String, Object>();
            if (step != null) body.put("step", step);
            var response = context.runtime(true).send(HttpRequest.json("POST", path, JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                out.println("✓ Project setup for " + projectId + " (step " + (step != null ? step : "current") + ")");
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.printf("Failed to setup project %s: HTTP %d%n", projectId, response.status());
            err.flush();
            return 1;
        }
    }

    @Command(name = "favourite", description = "Marks or unmarks a project as favourite.")
    @OperatorVerb
    static final class FavouriteCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Project ID")
        String projectId = "";

        @Option(names = "--off", description = "Remove favourite mark")
        boolean off;

        FavouriteCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            body.put("favourite", !off);
            var path = PROJECTS_API + OperatorUrlUtil.encodePathSegment(projectId) + "/favourite";
            var response = context.runtime(true).send(HttpRequest.json("PUT", path, JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ Project %s favourite: %s%n", projectId, off ? "off" : "on (★)");
                out.flush();
                return 0;
            }
            return 1;
        }
    }

    @Command(name = "release", description = "Releases a project: no new work starts in it.")
    @OperatorVerb
    static final class ReleaseCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Project ID")
        String projectId = "";

        @Option(names = "--reason", description = "Reason for releasing project")
        @Nullable
        String reason;

        ReleaseCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var body = new LinkedHashMap<String, Object>();
            if (reason != null) body.put("reason", reason);
            var path = PROJECTS_API + OperatorUrlUtil.encodePathSegment(projectId) + "/release";
            var response = context.runtime(true).send(HttpRequest.json("POST", path, JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ Project %s released%n", projectId);
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.printf("Failed to release project %s: HTTP %d%n", projectId, response.status());
            err.flush();
            return 1;
        }
    }

    @Command(name = "unenrol", description = "Removes a project's enrolment.")
    @OperatorVerb
    static final class UnenrolCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Project ID")
        String projectId = "";

        UnenrolCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var path = PROJECTS_API + OperatorUrlUtil.encodePathSegment(projectId) + "/unenrol";
            var response = context.runtime(true).send(HttpRequest.json("POST", path, "{}"));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ Project %s unenrolled%n", projectId);
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.printf("Failed to unenrol project %s: HTTP %d%n", projectId, response.status());
            err.flush();
            return 1;
        }
    }
}
