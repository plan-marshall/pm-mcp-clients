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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator plan}: inspects, adds, pauses, resumes, watches, or terminates plans
 * (01-commands.adoc, 19-actions.adoc).
 */
@Command(name = "plan", description = "Inspects or controls plans.",
        subcommands = {
                PlanCommand.AddCommand.class,
                PlanCommand.PauseCommand.class,
                PlanCommand.StartCommand.class,
                PlanCommand.FindingsCommand.class,
                PlanCommand.WatchesCommand.class,
                PlanCommand.CancelCommand.class,
                PlanCommand.AbandonCommand.class,
                PlanCommand.ChatCommand.class,
                PlanCommand.ReportCommand.class
        })
@OperatorVerb
public final class PlanCommand implements Callable<Integer> {

    private static final String KIND = "plan";
    private static final String PLANS_API = "/api/v1/plans/";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", arity = "0..1", description = "Plan ID")
    @Nullable
    String planId;

    @Option(names = "--all", description = "Show all details")
    boolean all;

    @Option(names = "--depth", description = "Process tree depth: compact, standard, full")
    @Nullable
    String depth;

    @Option(names = "--activity", description = "Task ID for worker activity")
    @Nullable
    String activity;

    @Option(names = "--watch", description = "Watch updates")
    boolean watch;

    PlanCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var runtime = context.runtime(true);

        if (planId != null && !planId.isBlank()) {
            var path = PLANS_API + OperatorUrlUtil.encodePathSegment(planId);
            if (activity != null && !activity.isBlank()) {
                path += "/tasks/" + OperatorUrlUtil.encodePathSegment(activity) + "/activity";
            } else if (depth != null) {
                path += "/process?depth=" + depth;
            }
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() == 200) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                out.printf("Plan %s not found.%n", planId);
                out.flush();
                return 1;
            }
            throw new OperatorRefusedException("plan " + planId, response);
        }

        // List running plans
        var response = runtime.send(HttpRequest.get("/api/v1/plans"));
        if (response.status() == 200) {
            var parsed = JsonTree.parse(response.body());
            if (parsed instanceof List<?> list && !list.isEmpty()) {
                ActionSupport.printWorkList(out, "Running Plans:", "phase", list);
            } else {
                out.println("No running plans.");
            }
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("plan list", response);
    }

    @Command(name = "add", description = "Starts a plan from its sources.")
    @OperatorVerb
    static final class AddCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Mixin
        ActionSupport.CommonAddOptions common = new ActionSupport.CommonAddOptions();

        @Option(names = "--document", description = "Input documents")
        List<String> documents = new ArrayList<>();

        @Option(names = "--issue", description = "Input issue URL or number")
        List<String> issues = new ArrayList<>();

        @Option(names = "--lesson", description = "Input lesson ID")
        List<String> lessons = new ArrayList<>();

        @Option(names = "--local-changes", description = "Include local changes as source")
        boolean localChanges;

        @Option(names = "--pull-request", description = "Pull request source")
        @Nullable
        String pullRequest;

        @Option(names = "--copy", description = "Copy input documents")
        boolean copy;

        @Option(names = "--move", description = "Move input documents")
        boolean move;

        @Option(names = "--no-input", description = "Stop on missing required input")
        boolean noInput;

        AddCommand(OperatorContext context) {
            this.context = context;
        }

        private Map<String, Object> buildPayload() {
            var payload = new LinkedHashMap<String, Object>();
            ActionSupport.populateCommonAdd(payload, common);
            if (!documents.isEmpty()) payload.put("documents", documents);
            if (!issues.isEmpty()) payload.put("issues", issues);
            if (!lessons.isEmpty()) payload.put("lessons", lessons);
            if (localChanges) payload.put("local_changes", true);
            if (pullRequest != null) payload.put("pull_request", pullRequest);
            if (copy) payload.put("documents_mode", "copy");
            if (move) payload.put("documents_mode", "move");
            return payload;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var payload = buildPayload();

            var path = common.project != null && !".".equals(common.project)
                    ? "/api/v1/projects/" + OperatorUrlUtil.encodePathSegment(common.project) + "/entries"
                    : "/api/v1/plans";

            var response = context.runtime(true).send(HttpRequest.json("POST", path, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 201) {
                String planName = "Plan";
                if (common.id != null) {
                    planName = common.id;
                } else if (common.title != null) {
                    planName = common.title;
                }
                out.printf("✓ %s has started%n", planName);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan add", response);
        }
    }

    @Command(name = "pause", description = "Pauses a running plan.")
    @OperatorVerb
    static final class PauseCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        @Option(names = "--now", description = "Pause immediately")
        boolean now;

        @Option(names = "--after-task", description = "Pause after current task")
        boolean afterTask;

        @Option(names = "--after-phase", description = "Pause after current phase")
        boolean afterPhase;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        PauseCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            String point = "after_task";
            if (now) point = "now";
            if (afterPhase) point = "after_phase";

            var payload = Map.of("point", point);
            var endpoint = PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/pause";
            var response = context.runtime(true).send(
                    HttpRequest.json("POST", endpoint, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 202) {
                if (now) {
                    out.printf("✓ %s paused%n", planId);
                } else {
                    out.printf("✓ %s pausing after the %s%n", planId, "after_phase".equals(point) ? "phase" : "task");
                }
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan pause", response);
        }
    }

    @Command(name = "start", description = "Resumes a paused plan.")
    @OperatorVerb
    static final class StartCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        StartCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var endpoint = PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/start";
            var response = context.runtime(true).send(
                    HttpRequest.json("POST", endpoint, "{}"));
            if (response.status() == 200 || response.status() == 202) {
                out.printf("✓ %s started%n", planId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan start", response);
        }
    }

    @Command(name = "findings", description = "Displays findings of a plan.")
    @OperatorVerb
    static final class FindingsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        FindingsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var endpoint = PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/findings";
            var response = context.runtime(true).send(HttpRequest.get(endpoint));
            if (response.status() == 200) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan findings", response);
        }
    }

    @Command(name = "watches", description = "Displays watches/listeners on a plan.")
    @OperatorVerb
    static final class WatchesCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        WatchesCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var endpoint = PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/watches";
            var response = context.runtime(true).send(HttpRequest.get(endpoint));
            if (response.status() == 200) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan watches", response);
        }
    }

    @Command(name = "cancel", description = "Cancels a running plan.")
    @OperatorVerb
    static final class CancelCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Mixin
        ActionSupport.TerminationOptions options = new ActionSupport.TerminationOptions();

        CancelCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            return ActionSupport.endWork(context, spec, KIND, options.id != null ? options.id : "",
                    options.reason, options.keepPr, options.noInput, "cancel");
        }
    }

    @Command(name = "abandon", description = "Abandons a paused plan.")
    @OperatorVerb
    static final class AbandonCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Mixin
        ActionSupport.TerminationOptions options = new ActionSupport.TerminationOptions();

        AbandonCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            return ActionSupport.endWork(context, spec, KIND, options.id != null ? options.id : "",
                    options.reason, options.keepPr, options.noInput, "abandon");
        }
    }

    @Command(name = "chat", description = "Chats with a plan's planning role.")
    @OperatorVerb
    static final class ChatCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        @Option(names = "--message", description = "Message to send")
        @Nullable
        String message;

        ChatCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            return ActionSupport.chat(context, spec, KIND, planId, message);
        }
    }

    @Command(name = "report", description = "Renders a plan's completion report.")
    @OperatorVerb
    static final class ReportCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        ReportCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var response = context.runtime(true).send(
                    HttpRequest.get(PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/completion"));
            if (response.status() == 200) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            if (response.status() == 409) {
                out.printf("%s is running; its completion report follows when it ends%n", planId);
                out.flush();
                return 1;
            }
            throw new OperatorRefusedException("plan report", response);
        }
    }
}
