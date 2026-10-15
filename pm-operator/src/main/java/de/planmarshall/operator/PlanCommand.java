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
 * {@code pm-operator plan}: manage plans, their execution lifecycle, chat, and reports.
 * Subcommands: {@code add}, {@code pause}, {@code start}, {@code cancel}, {@code abandon},
 * {@code chat}, {@code report}.
 * Without a subcommand, displays plan tree/activity or lists running plans.
 */
@Command(name = "plan", description = "Plan operations and actions.",
        subcommands = {
                PlanCommand.AddCommand.class,
                PlanCommand.PauseCommand.class,
                PlanCommand.StartCommand.class,
                PlanCommand.CancelCommand.class,
                PlanCommand.AbandonCommand.class,
                PlanCommand.ChatCommand.class,
                PlanCommand.ReportCommand.class
        })
@OperatorVerb
public final class PlanCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", arity = "0..1", description = "Plan ID")
    @Nullable
    String planId;

    @Option(names = "--all", description = "Show all details")
    boolean all;

    @Option(names = "--depth", description = "Process tree depth")
    @Nullable
    Integer depth;

    @Option(names = "--tokens", description = "Include tokens so far")
    boolean tokens;

    @Option(names = "--components", description = "Include token components")
    boolean components;

    @Option(names = "--activity", description = "Task ID for worker activity")
    @Nullable
    String activity;

    @Option(names = "--watch", description = "Watch updates")
    boolean watch;

    PlanCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var runtime = context.runtime(true);

        if (planId != null && !planId.isBlank()) {
            var path = "/api/v1/plans/" + planId;
            if (activity != null && !activity.isBlank()) {
                path += "/tasks/" + activity + "/activity";
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
                out.println("Running Plans:");
                for (var item : list) {
                    if (item instanceof Map<?, ?> plan) {
                        var title = plan.get("title");
                        out.printf("  %-20s %-12s %s%n",
                                plan.get("id"),
                                plan.get("phase"),
                                title != null ? title : "");
                    } else {
                        out.println("  " + item);
                    }
                }
            } else {
                out.println("No plans running.");
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
        CommandSpec spec;
        private final OperatorContext context;

        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String project;

        @Option(names = "--title", description = "Plan title")
        @Nullable
        String title;

        @Option(names = "--id", description = "Explicit plan ID")
        @Nullable
        String id;

        @Option(names = "--description", description = "Plan description")
        @Nullable
        String description;

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

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var payload = new LinkedHashMap<String, Object>();
            if (project != null && !".".equals(project)) {
                payload.put("project", project);
            }
            if (title != null) payload.put("title", title);
            if (id != null) payload.put("id", id);
            if (description != null) payload.put("description", description);
            if (!documents.isEmpty()) payload.put("documents", documents);
            if (!issues.isEmpty()) payload.put("issues", issues);
            if (!lessons.isEmpty()) payload.put("lessons", lessons);
            if (localChanges) payload.put("local_changes", true);
            if (pullRequest != null) payload.put("pull_request", pullRequest);
            if (copy) payload.put("documents_mode", "copy");
            if (move) payload.put("documents_mode", "move");

            var path = project != null && !".".equals(project)
                    ? "/api/v1/projects/" + project + "/entries"
                    : "/api/v1/plans";

            var response = context.runtime(true).send(HttpRequest.json("POST", path, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 201) {
                var planName = id != null ? id : (title != null ? title : "Plan");
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
        CommandSpec spec;
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
            if (now) {
                point = "now";
            } else if (afterPhase) {
                point = "after_phase";
            }

            var payload = Map.of("point", point);
            var response = context.runtime(true).send(
                    HttpRequest.json("POST", "/api/v1/plans/" + planId + "/pause", JsonTree.write(payload)));
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

    @Command(name = "start", description = "Starts or resumes a plan.")
    @OperatorVerb
    static final class StartCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
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
            var response = context.runtime(true).send(
                    HttpRequest.json("POST", "/api/v1/plans/" + planId + "/start", "{}"));
            if (response.status() == 200 || response.status() == 202) {
                out.printf("✓ %s started%n", planId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan start", response);
        }
    }

    @Command(name = "cancel", description = "Cancels a running plan.")
    @OperatorVerb
    static final class CancelCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        @Option(names = "--reason", description = "Reason for cancellation")
        @Nullable
        String reason;

        @Option(names = "--close-pr", description = "Close open pull request")
        boolean closePr;

        @Option(names = "--keep-pr", description = "Keep open pull request")
        boolean keepPr;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        CancelCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (reason == null || reason.isBlank()) {
                if (noInput) {
                    out.println("Cancellation reason required (--reason)");
                    out.flush();
                    return 3;
                }
                reason = "Cancelled by operator";
            }
            var payload = new LinkedHashMap<String, Object>();
            payload.put("reason", reason);
            payload.put("close_pr", !keepPr);

            var response = context.runtime(true).send(
                    HttpRequest.json("POST", "/api/v1/plans/" + planId + "/cancel", JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 202) {
                out.printf("✓ %s cancelled and archived%n", planId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan cancel", response);
        }
    }

    @Command(name = "abandon", description = "Abandons a paused plan.")
    @OperatorVerb
    static final class AbandonCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        @Option(names = "--reason", description = "Reason for abandoning")
        @Nullable
        String reason;

        @Option(names = "--close-pr", description = "Close open pull request")
        boolean closePr;

        @Option(names = "--keep-pr", description = "Keep open pull request")
        boolean keepPr;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;

        AbandonCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (reason == null || reason.isBlank()) {
                if (noInput) {
                    out.println("Abandon reason required (--reason)");
                    out.flush();
                    return 3;
                }
                reason = "Abandoned by operator";
            }
            var payload = new LinkedHashMap<String, Object>();
            payload.put("reason", reason);
            payload.put("close_pr", !keepPr);

            var response = context.runtime(true).send(
                    HttpRequest.json("POST", "/api/v1/plans/" + planId + "/abandon", JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 202) {
                out.printf("✓ %s abandoned and archived%n", planId);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan abandon", response);
        }
    }

    @Command(name = "chat", description = "Chats with a plan's planning role.")
    @OperatorVerb
    static final class ChatCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
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
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (message != null && !message.isBlank()) {
                var response = context.runtime(true).send(
                        HttpRequest.json("POST", "/api/v1/plans/" + planId + "/chat",
                                JsonTree.write(Map.of("message", message))));
                if (response.status() == 200 || response.status() == 201) {
                    out.println(response.bodyText());
                    out.flush();
                    return 0;
                }
                throw new OperatorRefusedException("plan chat", response);
            }

            var response = context.runtime(true).send(HttpRequest.get("/api/v1/plans/" + planId + "/chat"));
            if (response.status() == 200) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan chat", response);
        }
    }

    @Command(name = "report", description = "Shows an ended plan's completion report.")
    @OperatorVerb
    static final class ReportCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        @Option(names = "--tokens", description = "Include tokens breakdown")
        boolean tokens;

        @Option(names = "--components", description = "Include raw token components")
        boolean components;

        ReportCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var path = "/api/v1/plans/" + planId + "/completion";
            if (tokens) {
                path += "?tokens=true";
                if (components) path += "&components=true";
            }
            var response = context.runtime(true).send(HttpRequest.get(path));
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
