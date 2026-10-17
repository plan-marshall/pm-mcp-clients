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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.start.PlanStartResult;
import de.planmarshall.api.start.StartEpicRequest;
import de.planmarshall.api.start.StartPlanRequest;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator start}: start a plan, an epic, or run a recipe in a project (UC-STA-1 to
 * UC-STA-5).
 * <p>
 * Each sub-verb answers the states a start asks, so the console does not have to: the project, the
 * prompt, the recipe, the documents, and the issue; what is missing is asked later by the server.
 */
@Command(name = "start", description = "Start a plan or an epic.",
                    subcommands = {
                                        StartCommand.PlanCommand.class,
                                        StartCommand.EpicCommand.class,
                                        StartCommand.RecipeCommand.class
                    })
@OperatorVerb
@SuppressWarnings("java:S1075")
public final class StartCommand implements Callable<Integer> {

    private static final String PATH_PROJECTS_PREFIX = "/api/v1/projects/";

    @Spec
    CommandSpec spec = CommandSpec.create();

    StartCommand(OperatorContext context) {
        Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    @Command(name = "plan", description = "Starts a plan in a project.")
    @OperatorVerb
    static final class PlanCommand implements Callable<Integer> {

        @Spec
        CommandSpec spec = CommandSpec.create();

        @Parameters(index = "0", arity = "0..1", description = "Target project ID")
        @Nullable
        String project;

        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String projectOption;

        @Option(names = "--prompt", description = "The operator's words for the work")
        @Nullable
        String prompt;

        @Option(names = "--recipe", description = "The recipe the plan runs")
        @Nullable
        String recipe;

        @Option(names = "--doc", description = "A document path; repeatable")
        List<String> documents = new ArrayList<>();

        @Option(names = "--issue", description = "An issue number or URL")
        @Nullable
        String issue;

        @Option(names = "--title", description = "The proposed plan title")
        @Nullable
        String title;

        @Option(names = "--lesson", description = "An unclaimed lesson the plan starts from (UC-STA-8)")
        @Nullable
        String lesson;

        @Option(names = "--use-anyway", description = "Use the lesson even though its premise is broken")
        boolean useAnyway;

        @Option(names = "--param", description = "A recipe parameter key=value")
        Map<String, String> parameters = new LinkedHashMap<>();

        @Option(names = "--adopt-pr", description = "Adopt the pull request named by this number or URL")
        @Nullable
        String adoptPr;

        @Option(names = "--local-changes", description = "Capture the checkout's local changes")
        boolean localChanges;

        @Option(names = {"-y", "--yes"}, description = "Answer the confirmation of a git source with yes")
        boolean yes;

        @Option(names = "--no-input", description = "Do not prompt; fail if a choice is needed")
        boolean noInput;

        @Option(names = "--accept-screening", description = "Adopt a flagged or unscreened pull request with this reason")
        @Nullable
        String acceptScreening;

        private final OperatorContext context;

        PlanCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var projectId = resolveProject(project, projectOption);

            if (lesson != null && !lesson.isBlank()
                                && !LessonSourceCommand.adoptForPlan(context, out, projectId, lesson, useAnyway)) {
                return 0;
            }
            if (adoptPr != null && !adoptPr.isBlank()) {
                return GitSourceCommand.adoptPr(context, out, projectId, adoptPr, yes, noInput,
                                    acceptScreening);
            }
            if (localChanges) {
                return GitSourceCommand.captureLocalChanges(context, out, projectId, yes, noInput);
            }

            var request = new StartPlanRequest(projectId, prompt, recipe, parameters, documents, issue,
                                title);
            var response = post(context, PATH_PROJECTS_PREFIX + OperatorUrlUtil.encodePathSegment(projectId)
                                + "/plans/start", request.toJson());
            return printResult(out, "start plan", response);
        }
    }

    @Command(name = "epic", description = "Starts an epic in a project.")
    @OperatorVerb
    static final class EpicCommand implements Callable<Integer> {

        @Spec
        CommandSpec spec = CommandSpec.create();

        @Parameters(index = "0", arity = "0..1", description = "Target project ID")
        @Nullable
        String project;

        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String projectOption;

        @Option(names = "--prompt", description = "The epic's objective")
        @Nullable
        String prompt;

        @Option(names = "--auto", arity = "0..1", fallbackValue = "true",
                            description = "Whether the epic starts its plans by itself")
        @Nullable
        Boolean autoStart;

        @Option(names = "--no-auto", description = "The epic starts none of its plans by itself")
        boolean noAuto;

        @Option(names = "--title", description = "The proposed epic title")
        @Nullable
        String title;

        private final OperatorContext context;

        EpicCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var projectId = resolveProject(project, projectOption);
            var auto = noAuto ? Boolean.FALSE : autoStart;

            var request = new StartEpicRequest(projectId, prompt, auto, title);
            var response = post(context, PATH_PROJECTS_PREFIX + OperatorUrlUtil.encodePathSegment(projectId)
                                + "/epics/start", request.toJson());
            return printResult(out, "start epic", response);
        }
    }

    @Command(name = "recipe", description = "Runs a recipe of a project.")
    @OperatorVerb
    static final class RecipeCommand implements Callable<Integer> {

        @Spec
        CommandSpec spec = CommandSpec.create();

        @Parameters(index = "0", arity = "0..1", description = "Recipe key")
        @Nullable
        String recipeKey;

        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String projectOption;

        @Option(names = "--param", description = "A recipe parameter key=value")
        Map<String, String> parameters = new LinkedHashMap<>();

        @Option(names = "--title", description = "The proposed plan title")
        @Nullable
        String title;

        private final OperatorContext context;

        RecipeCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (recipeKey == null || recipeKey.isBlank()) {
                out.println("Recipe key required.");
                out.flush();
                return 1;
            }
            var projectId = resolveProject(null, projectOption);

            var request = new StartPlanRequest(projectId, null, recipeKey, parameters, List.of(), null,
                                title);
            var response = post(context, PATH_PROJECTS_PREFIX + OperatorUrlUtil.encodePathSegment(projectId)
                                + "/plans/start", request.toJson());
            return printResult(out, "start recipe", response);
        }
    }

    private static String resolveProject(@Nullable String positional, @Nullable String option) {
        if (option != null && !option.isBlank()) {
            return option;
        }
        if (positional != null && !positional.isBlank()) {
            return positional;
        }
        return "default";
    }

    private static HttpResponse post(OperatorContext context, String path, String body) throws IOException {
        return context.runtime(true).send(HttpRequest.json("POST", path, body));
    }

    private static int printResult(PrintWriter out, String verb, HttpResponse response)
                        throws IOException {
        if (response.status() == 200 || response.status() == 201) {
            var result = PlanStartResult.fromJson(response.bodyText());
            var id = OutputSafety.stripControlSequences(result.id());
            out.println("✓ " + MessageCatalogue.format("start.started.title", Map.of("name", id)));
            if (result.followHref() != null) {
                out.println("    " + MessageCatalogue.get("start.started.follow") + ": " + result.followHref());
            }
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException(verb, response);
    }
}
