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
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.project.EnrolledProject;
import de.planmarshall.api.project.ProjectReleaseRequest;

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
    private static final String FMT_PROJECT_NOT_FOUND = "Project %s not found.%n";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", arity = "0..1", description = "Project ID to inspect")
    @Nullable
    String projectId;

    ProjectCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        if (projectId == null || projectId.isBlank()) {
            spec.commandLine().usage(spec.commandLine().getOut());
            return 0;
        }

        context.gate().verify();
        var safeId = OperatorUrlUtil.encodePathSegment(projectId);
        var response = context.runtime(true).send(HttpRequest.get(PROJECTS_API + safeId));
        var out = spec.commandLine().getOut();

        if (response.status() == 404) {
            out.printf(FMT_PROJECT_NOT_FOUND, OutputSafety.stripControlSequences(projectId));
            out.flush();
            return 1;
        }

        if (response.status() == 200) {
            var project = EnrolledProject.fromJson(response.bodyText());
            renderProjectDetail(out, project);
            return 0;
        }

        throw new OperatorRefusedException("project " + OutputSafety.stripControlSequences(projectId), response);
    }

    private static void renderProjectDetail(PrintWriter out, EnrolledProject project) {
        var name = OutputSafety.stripControlSequences(project.displayName());
        var pId = OutputSafety.stripControlSequences(project.projectId());
        var remote = project.remoteUrl() != null ? OutputSafety.stripControlSequences(project.remoteUrl()) : "";
        var readiness = switch (project.readiness()) {
            case READY -> "ready";
            case RELEASED -> "released";
            case SETUP_NEEDS -> "setup needs: " + OutputSafety.stripControlSequences(
                                project.setupNeedsSummary() != null ? project.setupNeedsSummary() : "setup");
        };

        out.println("Project:   " + name + " (" + pId + ")");
        if (!remote.isBlank()) {
            out.println("Remote:    " + remote);
        }
        out.println("Readiness: " + readiness);
        if (project.needsOperatorCount() > 0) {
            out.println("Operator:  needs you: " + project.needsOperatorCount());
        }
        out.println("Work:      " + project.activePlansCount() + " plans · " + project.activeEpicsCount() + " epics");
        if (project.lastActive() != null) {
            out.println("Last seen: " + ProjectsCommand.formatRelativeTime(project.lastActive()));
        }
        out.println("Favourite: " + (project.isFavourite() ? "yes (★)" : "no"));
        out.println("Released:  " + (project.isReleased() ? "yes" : "no"));
        out.flush();
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
            var safePath = OutputSafety.stripControlSequences(path);
            if (response.status() == 200 || response.status() == 201) {
                out.println("✓ Project onboarding started for " + safePath);
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.printf("Failed to onboard project at %s: HTTP %d%n", safePath, response.status());
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
            var safeId = OperatorUrlUtil.encodePathSegment(projectId);
            var sanitizedId = OutputSafety.stripControlSequences(projectId);
            var path = PROJECTS_API + safeId + "/setup";
            var body = new LinkedHashMap<String, Object>();
            if (step != null) body.put("step", step);
            var response = context.runtime(true).send(HttpRequest.json("POST", path, JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200) {
                var stepStr = step != null ? OutputSafety.stripControlSequences(step) : "current";
                out.println("✓ Project setup for " + sanitizedId + " (step " + stepStr + ")");
                out.flush();
                return 0;
            }
            var err = spec.commandLine().getErr();
            err.printf("Failed to setup project %s: HTTP %d%n", sanitizedId, response.status());
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
            var safeId = OperatorUrlUtil.encodePathSegment(projectId);
            var sanitizedId = OutputSafety.stripControlSequences(projectId);
            var body = new LinkedHashMap<String, Object>();
            body.put("favourite", !off);
            var path = PROJECTS_API + safeId + "/favourite";
            var response = context.runtime(true).send(HttpRequest.json("PUT", path, JsonTree.write(body)));
            var out = spec.commandLine().getOut();
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ Project %s favourite: %s%n", sanitizedId, off ? "off" : "on (★)");
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

        @Option(names = {"-y", "--yes"}, description = "Confirm release without interactive prompt")
        boolean yes;

        @Option(names = "--force", description = "Force release even if short landings are pending")
        boolean force;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        ReleaseCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var safeId = OperatorUrlUtil.encodePathSegment(projectId);
            var sanitizedId = OutputSafety.stripControlSequences(projectId);

            if (reason == null || reason.isBlank()) {
                if (noInput) {
                    err.println("Release reason required (--reason)");
                    err.flush();
                    return 3;
                }
                reason = "Released by operator";
            }

            if (!yes && noInput) {
                err.println("Release confirmation required (--yes)");
                err.flush();
                return 3;
            }

            var req = new ProjectReleaseRequest(projectId, true, force, reason);
            var path = PROJECTS_API + safeId + "/release";
            var response = context.runtime(true).send(HttpRequest.json("POST", path, req.toJson()));
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ %s released%n", sanitizedId);
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                err.printf(FMT_PROJECT_NOT_FOUND, sanitizedId);
                err.flush();
                return 1;
            }
            if (response.status() == 409) {
                err.printf("✗ %s was not released: release landing pending. Nothing was ended.%n", sanitizedId);
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("release project " + sanitizedId, response);
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

        @Option(names = {"-y", "--yes"}, description = "Confirm unenrolment without interactive prompt")
        boolean yes;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        UnenrolCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var err = spec.commandLine().getErr();
            var safeId = OperatorUrlUtil.encodePathSegment(projectId);
            var sanitizedId = OutputSafety.stripControlSequences(projectId);

            if (!yes && noInput) {
                err.println("Unenrol confirmation required (--yes)");
                err.flush();
                return 3;
            }

            var path = PROJECTS_API + safeId + "/unenrol";
            var response = context.runtime(true).send(HttpRequest.json("POST", path, "{}"));
            if (response.status() == 200 || response.status() == 204) {
                out.printf("✓ %s unenrolled%n", sanitizedId);
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                err.printf(FMT_PROJECT_NOT_FOUND, sanitizedId);
                err.flush();
                return 1;
            }
            if (response.status() == 409) {
                err.printf("✗ %s still runs work; unenrolling is refused while it does.%n", sanitizedId);
                err.flush();
                return 1;
            }
            throw new OperatorRefusedException("unenrol project " + sanitizedId, response);
        }
    }
}
