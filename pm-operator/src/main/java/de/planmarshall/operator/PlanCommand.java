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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.HoldResolutionResult;
import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsUpdateResult;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.process.ActivityEntryKind;
import de.planmarshall.api.process.NodeState;
import de.planmarshall.api.process.ProcessNode;
import de.planmarshall.api.process.ProcessTokens;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.process.TaskActivity;
import de.planmarshall.api.report.DecisionRecord;
import de.planmarshall.api.report.DeliverableSummary;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReplanningRecord;
import de.planmarshall.api.report.TokenBreakdownItem;
import de.planmarshall.api.report.TokenComponentRecord;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.api.report.TokenUsageRecord;
import de.planmarshall.api.start.EntryKind;

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
                                        PlanCommand.ResolveCommand.class,
                                        PlanCommand.SettingsCommand.class,
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

    @Option(names = {"--task", "--activity"}, description = "Task ID for worker activity")
    @Nullable
    String activity;

    @Option(names = "--tokens", description = "Show tokens so far")
    boolean tokens;

    @Option(names = "--components", description = "Show raw token components")
    boolean components;

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
            if (activity != null && !activity.isBlank()) {
                var path = PLANS_API + OperatorUrlUtil.encodePathSegment(planId)
                                    + "/tasks/" + OperatorUrlUtil.encodePathSegment(activity) + "/activity";
                var response = runtime.send(HttpRequest.get(path));
                if (response.status() == 200) {
                    try {
                        var taskActivity = TaskActivity.fromJson(response.bodyText());
                        printTaskActivity(out, planId, taskActivity);
                        return 0;
                    }
                    /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
                    catch (Exception e) {
                        out.println(response.bodyText());
                        out.flush();
                        return 0;
                    }
                }
                if (response.status() == 404) {
                    out.printf("Task %s or plan %s not found.%n", activity, planId);
                    out.flush();
                    return 1;
                }
                throw new OperatorRefusedException("plan activity " + planId + " " + activity, response);
            }

            var path = PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/process";
            if (depth != null) {
                path += "?depth=" + depth;
            }
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() == 404) {
                // Fallback to direct plan endpoint for legacy/minimal test fixtures
                response = runtime.send(HttpRequest.get(PLANS_API + OperatorUrlUtil.encodePathSegment(planId)));
            }
            if (response.status() == 200) {
                var body = response.bodyText();
                if (body.contains("\"nodes\"") || body.contains("\"plan_id\"")) {
                    try {
                        var tree = ProcessTree.fromJson(body);
                        printProcessTree(out, tree);
                        return 0;
                    }
                    /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
                    catch (Exception ignored) {
                        // Fall through to print bodyText
                    }
                }
                out.println(body);
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

    private void printTaskActivity(PrintWriter out, String planId, TaskActivity act) {
        var cleanPlan = OutputSafety.stripControlSequences(planId);
        var cleanTask = OutputSafety.stripControlSequences(act.taskId());
        out.printf("%s · %s%n", cleanPlan, cleanTask);

        var duration = formatPlanDuration(act.durationSeconds());
        var tokensStr = TokenFormat.formatTokens(act.tokensSoFar(), false, "en") + " tokens";
        out.printf("  %s · %s %s · attempt %d · running %s · %s%n",
                            OutputSafety.stripControlSequences(act.role()),
                            OutputSafety.stripControlSequences(act.harness()),
                            OutputSafety.stripControlSequences(act.model()),
                            act.attempt(),
                            duration,
                            tokensStr);

        for (var entry : act.entries()) {
            var ts = OutputSafety.stripControlSequences(entry.timestamp());
            var kind = ActivityEntryKind.fromValue(entry.kind());
            switch (kind) {
                case MODEL_TEXT -> {
                    var content = OutputSafety.stripControlSequences(entry.content());
                    out.printf("  %s  wrote      %s%n", ts, content != null ? content : "");
                }
                case TOOL_CALL -> {
                    var toolName = OutputSafety.stripControlSequences(entry.toolName());
                    var toolTarget = OutputSafety.stripControlSequences(entry.toolTarget());
                    var toolStr = (toolName != null ? toolName : "") + (toolTarget != null ? " " + toolTarget : "");
                    var failStr = entry.toolFailed() ? "        failed" + (entry.size() != null ? " · " + OutputSafety.stripControlSequences(entry.size()) : "") : "";
                    out.printf("  %s  tool       %-30s%s%n", ts, toolStr, failStr);
                }
                case FILES_CHANGED -> {
                    var files = entry.files().stream().map(OutputSafety::stripControlSequences).toList();
                    out.printf("  %s  changed    %s%n", ts, String.join(", ", files));
                }
                case QUESTIONS_ASKED -> {
                    var content = OutputSafety.stripControlSequences(entry.content());
                    out.printf("  %s  asked      %s%n", ts, content != null ? content : "");
                }
                case LINK -> {
                    var target = OutputSafety.stripControlSequences(entry.toolTarget());
                    out.printf("  %s  link       %s%n", ts, target != null ? target : "");
                }
                default -> {
                    var content = OutputSafety.stripControlSequences(entry.content());
                    out.printf("  %s  %-9s  %s%n", ts, OutputSafety.stripControlSequences(entry.kind()), content != null ? content : "");
                }
            }
        }

        if (act.offTrack() != null) {
            var ot = act.offTrack();
            var sig = OutputSafety.stripControlSequences(ot.signal());
            var action = OutputSafety.stripControlSequences(ot.action());
            var valThresh = ot.value() != null && ot.threshold() != null ? String.format(" (%d of %d)", ot.value(), ot.threshold()) : "";
            out.printf("  ⚠ off track  %s%s: %s%n", sig, valThresh, action);
        }
        out.flush();
    }

    private void printProcessTree(PrintWriter out, ProcessTree tree) {
        var projectStr = tree.project() != null ? OutputSafety.stripControlSequences(tree.project().projectId()) : "";
        var epicStr = tree.epic() != null ? OutputSafety.stripControlSequences(tree.epic().epicId()) : null;
        var planStr = OutputSafety.stripControlSequences(tree.planId());

        if (!projectStr.isEmpty()) {
            if (epicStr != null && !epicStr.isEmpty()) {
                out.printf("%s › %s › %s%n", projectStr, epicStr, planStr);
            } else {
                out.printf("%s › %s%n", projectStr, planStr);
            }
        }

        var durationStr = tree.durationSeconds() > 0 ? " · " + formatPlanDuration(tree.durationSeconds()) : "";
        var prStr = tree.pullRequestNumber() != null ? " · pull request #" + tree.pullRequestNumber() + (tree.pullRequestUrl() != null ? " " + OutputSafety.stripControlSequences(tree.pullRequestUrl()) : "") : "";
        out.printf("%s · %s · %s%s%s%n",
                            planStr,
                            OutputSafety.stripControlSequences(tree.currentPhase()),
                            OutputSafety.stripControlSequences(tree.phaseState()),
                            durationStr,
                            prStr);

        if (!projectStr.isEmpty()) {
            if (epicStr != null && !epicStr.isEmpty()) {
                out.printf("  in %s · epic %-20s status --project %s · epic %s%n",
                                    projectStr, epicStr, projectStr, epicStr);
            } else {
                out.printf("  in %-35s status --project %s%n", projectStr, projectStr);
            }
        }

        if (tree.tokensSoFar() != null) {
            out.printf("  tokens so far   %-25s plan %s --tokens%n", formatTokens(tree.tokensSoFar()), planStr);
        }

        int maxDepth = parseDepth(depth, all);
        renderNodes(out, tree.nodes(), 1, maxDepth);
        out.flush();
    }

    private void renderNodes(PrintWriter out, List<ProcessNode> nodes, int level, int maxDepth) {
        for (var node : nodes) {
            renderNode(out, node, level, maxDepth);
        }
    }

    private void renderNode(PrintWriter out, ProcessNode node, int level, int maxDepth) {
        var indent = "  ".repeat(level);
        var stateEnum = NodeState.fromValue(node.state());
        var glyph = stateEnum.glyph();
        var stateName = stateEnum.value();

        if (node.pruneReason() != null || stateEnum == NodeState.NOT_IN_PLAN) {
            var reason = node.pruneReason() != null ? " (" + node.pruneReason().replace('_', ' ') + ")" : "";
            out.printf("%s%s not in this plan: %s%s%n", indent, glyph, OutputSafety.stripControlSequences(node.title()), reason);
            return;
        }

        var runStr = node.runCount() > 1 && stateEnum == NodeState.DONE ? " ×" + node.runCount() : "";
        var currentRunStr = node.currentRun() > 1 && stateEnum == NodeState.RUNNING ? "run " + node.currentRun() : "";
        var durationStr = node.durationSeconds() > 0 ? formatPlanDuration(node.durationSeconds()) : "";

        var infoParts = new ArrayList<String>();
        if (!currentRunStr.isEmpty()) infoParts.add(currentRunStr);
        if (!durationStr.isEmpty()) infoParts.add(durationStr);
        if (!node.leafTasks().isEmpty()) {
            long doneCount = node.leafTasks().stream().filter(t -> "done".equalsIgnoreCase(t.status()) || "submitted".equalsIgnoreCase(t.status())).count();
            infoParts.add(String.format("tasks: %d of %d done", doneCount, node.leafTasks().size()));
        }
        if (node.waitsFor() != null && !node.waitsFor().isBlank()) {
            infoParts.add("waits: " + OutputSafety.stripControlSequences(node.waitsFor()));
        }
        var info = String.join(" · ", infoParts);

        var kindPrefix = "phase".equals(node.kind()) ? "phase " : ("sub_workflow".equals(node.kind()) ? "sub-workflow " : "");
        var title = OutputSafety.stripControlSequences(node.title());
        if (title.startsWith("phase ") || title.startsWith("sub-workflow ")) {
            kindPrefix = "";
        }

        var stateCol = String.format("%-9s", stateName + runStr);
        var titleCol = kindPrefix + title;

        if (!info.isEmpty()) {
            out.printf("%s%s %s %-30s %s%n", indent, glyph, stateCol, titleCol, info);
        } else {
            out.printf("%s%s %s %s%n", indent, glyph, stateCol, titleCol);
        }

        if ((level + 1) <= maxDepth && !node.leafTasks().isEmpty()) {
            var taskIndent = "  ".repeat(level + 1);
            for (var task : node.leafTasks()) {
                var taskGlyph = getTaskGlyph(task.status());
                var taskStateCol = String.format("%-9s", task.status());
                var taskDesc = OutputSafety.stripControlSequences(task.taskId() + " (" + task.role() + ")");
                var taskInfoParts = new ArrayList<String>();
                if (task.durationSeconds() > 0) {
                    taskInfoParts.add(formatPlanDuration(task.durationSeconds()));
                }
                if (task.offTrack() != null) {
                    taskInfoParts.add("off-track: " + OutputSafety.stripControlSequences(task.offTrack().signal()));
                }
                var taskInfo = String.join(" · ", taskInfoParts);
                if (!taskInfo.isEmpty()) {
                    out.printf("%s%s %s %-30s %s%n", taskIndent, taskGlyph, taskStateCol, taskDesc, taskInfo);
                } else {
                    out.printf("%s%s %s %s%n", taskIndent, taskGlyph, taskStateCol, taskDesc);
                }
            }
        }

        if ((level + 1) <= maxDepth && node.leafJob() != null) {
            var job = node.leafJob();
            var jobIndent = "  ".repeat(level + 1);
            var jobGlyph = getTaskGlyph(job.status());
            var jobStateCol = String.format("%-9s", job.status());
            var jobDesc = OutputSafety.stripControlSequences("job (" + job.kind() + ")");
            var jobInfoParts = new ArrayList<String>();
            if (job.durationSeconds() > 0) {
                jobInfoParts.add(formatPlanDuration(job.durationSeconds()));
            }
            if (job.waitsSlot()) {
                jobInfoParts.add("waits slot");
            }
            if (job.outcome() != null && !job.outcome().isBlank()) {
                jobInfoParts.add(job.outcome());
            }
            var jobInfo = String.join(" · ", jobInfoParts);
            if (!jobInfo.isEmpty()) {
                out.printf("%s%s %s %-30s %s%n", jobIndent, jobGlyph, jobStateCol, jobDesc, jobInfo);
            } else {
                out.printf("%s%s %s %s%n", jobIndent, jobGlyph, jobStateCol, jobDesc);
            }
        }

        if (level < maxDepth && !node.subNodes().isEmpty()) {
            renderNodes(out, node.subNodes(), level + 1, maxDepth);
        }
    }

    private static String getTaskGlyph(@Nullable String status) {
        if (status == null) {
            return "○";
        }
        return switch (status.toLowerCase()) {
            case "done", "submitted" -> "✓";
            case "running" -> "▶";
            case "waiting", "waiting_for_you" -> "!";
            case "failed" -> "✗";
            default -> "○";
        };
    }

    static String formatPlanDuration(long seconds) {
        if (seconds <= 0) {
            return "0 s";
        }
        if (seconds < 60) {
            return seconds + " s";
        }
        long hours = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + " h " + mins + " min";
        }
        return mins + " min";
    }

    static String formatTokens(ProcessTokens tokens) {
        if (tokens.noneYet()) {
            return "none yet";
        }
        var prefix = tokens.floor() ? "≥ " : "";
        return String.format("%s%s (%s in · %s out)",
                            prefix,
                            TokenFormat.formatTokens(tokens.total(), false, "en"),
                            TokenFormat.formatTokens(tokens.input(), false, "en"),
                            TokenFormat.formatTokens(tokens.output(), false, "en"));
    }

    private static int parseDepth(@Nullable String depth, boolean all) {
        if (all) {
            return Integer.MAX_VALUE;
        }
        if (depth == null || depth.isBlank()) {
            return 3;
        }
        return switch (depth.toLowerCase()) {
            case "compact", "1" -> 1;
            case "standard", "2" -> 2;
            case "full", "3" -> 3;
            default -> {
                try {
                    yield Integer.parseInt(depth);
                } catch (NumberFormatException e) {
                    yield 3;
                }
            }
        };
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

        @Option(names = "--entry", description = "Continue starting an open entry")
        @Nullable
        String entryId;

        AddCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var answers = new StartAnswers()
                                .project(common.project)
                                .description(common.description);
            answers.title = common.title;
            answers.id = common.id;
            answers.documents.addAll(documents);
            answers.issues.addAll(issues);
            answers.lesson = lessons.isEmpty() ? null : lessons.getFirst();
            answers.localChanges = localChanges;
            answers.pullRequest = pullRequest;
            answers.copy = copy;
            answers.move = move;
            answers.noInput = noInput;
            answers.entryId = entryId;
            return StartDialogue.run(context, spec.commandLine().getOut(), EntryKind.PLAN, answers);
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
            return ActionSupport.endWork(context, spec, KIND, options, ActionSupport.CANCEL);
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
            return ActionSupport.endWork(context, spec, KIND, options, "abandon");
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

        @Option(names = "--tokens", description = "Show tokens breakdown")
        boolean tokens;

        @Option(names = "--components", description = "Show raw token components")
        boolean components;

        private static final String FMT_14S = "  %-14s %s%n";
        private static final String FMT_INDENT = "                 %s%n";
        private static final String NOT_MEASURED = " not measured";
        private static final String KEY_STATE = "state";

        ReportCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var response = context.runtime(true).send(
                                HttpRequest.get(PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/completion"));
            if (response.status() == 409) {
                var state = "running";
                try {
                    var parsed = JsonTree.parse(response.body());
                    if (parsed instanceof Map<?, ?> map && map.get(KEY_STATE) != null) {
                        state = map.get(KEY_STATE).toString();
                    }
                } catch (IOException _) {
                    // Ignore unparseable 409 body
                }
                out.println(MessageCatalogue.format("operator.reports.not_ended",
                                    Map.of("plan", planId, KEY_STATE, state)));
                out.flush();
                return 1;
            }
            if (response.status() == 404) {
                out.printf("Plan %s not found.%n", planId);
                out.flush();
                return 1;
            }
            if (response.status() == 200) {
                var bodyText = response.bodyText().trim();
                if (bodyText.startsWith("{")) {
                    try {
                        printCompletionReport(out, bodyText, tokens, components);
                        return 0;
                    } catch (IOException _) {
                        // ignore and fall back to printing bodyText
                    }
                }
                out.println(bodyText);
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan report", response);
        }

        private void printCompletionReport(PrintWriter out, String body, boolean showTokens, boolean showComponents) throws IOException {
            var report = PlanCompletionReport.fromJson(body);

            var headParts = new ArrayList<String>();
            headParts.add(ConsoleNotice.stripControlSequences(report.planId()));
            headParts.add("completion report");
            headParts.add(ConsoleNotice.stripControlSequences(report.outcome()));
            if (report.pullRequestNumber() != null) {
                headParts.add("#" + report.pullRequestNumber());
            }
            if (report.ended() != null) {
                headParts.add(report.ended());
            }
            out.println(String.join(" · ", headParts));

            var from = report.from();
            if (from != null && from.project() != null) {
                var path = "in " + from.project();
                if (from.epic() != null && !from.epic().isBlank()) {
                    path += " › " + from.epic();
                }
                out.printf("  %-50s plan %s%n", path, report.planId());
            }

            var outcomeDesc = report.terminalReason() != null ? report.terminalReason() : report.outcome();
            out.printf(FMT_14S, "outcome", ConsoleNotice.stripControlSequences(outcomeDesc));
            if (report.pullRequestUrl() != null && !report.pullRequestUrl().isBlank()) {
                out.printf(FMT_14S, "", ConsoleNotice.stripControlSequences(report.pullRequestUrl()));
            }

            var reason = report.reason() != null && !report.reason().isBlank() ? report.reason() : "—";
            out.printf(FMT_14S, "reason", ConsoleNotice.stripControlSequences(reason));

            var ran = formatRan(report);
            if (ran != null) {
                out.printf(FMT_14S, "ran", ran);
            }

            printDeliverables(out, report.deliverables());
            printReplanning(out, report.replanningRecords());
            printDecisions(out, report.decisionRecords());

            var usage = report.tokenUsage();
            if (usage != null) {
                printTokensSummary(out, usage, ran, report.planId(), showTokens, showComponents);
            }
            out.flush();
        }

        private static @Nullable String formatRan(PlanCompletionReport report) {
            var ran = report.ran();
            if (ran == null && report.durationSeconds() > 0) {
                ran = formatDuration(report.durationSeconds());
                if (report.startedAt() != null && report.endedAt() != null) {
                    var zone = ZoneId.systemDefault();
                    var timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);
                    ran += ", " + timeFmt.format(report.startedAt()) + " to " + timeFmt.format(report.endedAt());
                }
                if (report.pausedSeconds() > 0) {
                    ran += " · paused " + formatDuration(report.pausedSeconds());
                    if (report.pausedBy() != null) {
                        ran += " by " + report.pausedBy();
                    }
                }
            }
            return ran;
        }

        private static void printDeliverables(PrintWriter out, List<DeliverableSummary> deliverables) {
            if (deliverables.isEmpty()) {
                out.printf(FMT_14S, "deliverables", "no deliverables yet: it ended in refine");
            } else {
                long doneCount = deliverables.stream().filter(d -> "done".equalsIgnoreCase(d.state())).count();
                out.printf("  %-14s %d of %d done%n", "deliverables", doneCount, deliverables.size());
                for (var deliv : deliverables) {
                    out.printf("                   %-5s %s%n", deliv.state(), ConsoleNotice.stripControlSequences(deliv.name()));
                }
            }
        }

        private static void printReplanning(PrintWriter out, List<ReplanningRecord> replanningRecords) {
            if (!replanningRecords.isEmpty()) {
                var first = replanningRecords.get(0);
                var repSummary = String.format("%d · after %s (%s): %s",
                                    replanningRecords.size(),
                                    ConsoleNotice.stripControlSequences(first.trigger()),
                                    first.time() != null ? first.time() : "",
                                    ConsoleNotice.stripControlSequences(first.change()));
                out.printf(FMT_14S, "re-planning", repSummary);
            }
        }

        private static void printDecisions(PrintWriter out, List<DecisionRecord> decisionRecords) {
            if (!decisionRecords.isEmpty()) {
                var decList = new ArrayList<String>();
                for (var d : decisionRecords) {
                    var decText = d.id();
                    if (d.question() != null && !d.question().isBlank()) {
                        decText += " " + ConsoleNotice.stripControlSequences(d.question());
                    }
                    var by = d.by();
                    if ("operator".equalsIgnoreCase(by)) {
                        by = d.channel() != null ? "you, the " + d.channel() + " app" : "you";
                    }
                    decText += " (" + by + ")";
                    decList.add(decText);
                }
                out.printf("  %-14s %d · %s%n", "decisions", decisionRecords.size(), String.join(" · ", decList));
            }
        }

        private void printTokensSummary(PrintWriter out, TokenUsageRecord usage, @Nullable String ran, String planId, boolean showTokens, boolean showComponents) {
            var totalSplit = TokenFormat.formatSplit(usage.total(), usage.input(), usage.output(), usage.floor(), "en");
            if (showTokens) {
                out.printf("tokens         %s · ran %s%n", totalSplit, ran != null ? ran : "");
                printTokensDetail(out, usage, planId, showComponents);
            } else {
                out.printf(FMT_14S, "tokens", totalSplit + " · " + (ran != null ? ran : ""));
                int unmeasured = usage.tasksTotal() - usage.tasksMeasured();
                var covText = String.format("%d workers · %d models · %d of %d tasks not measured",
                                    usage.workersCount(), usage.modelsCount(), unmeasured, usage.tasksTotal());
                out.printf(FMT_INDENT, covText);
                out.printf("                 plan report %s --tokens%n", planId);
            }
        }

        private static String formatPin(TokenBreakdownItem item) {
            if (item.harnessModel() == null) {
                return "";
            }
            var effort = item.effort() != null ? item.effort() : "medium";
            return " " + item.harnessModel() + " · " + effort;
        }

        private void printTokensDetail(PrintWriter out, TokenUsageRecord usage, String planId, boolean showComponents) {
            int unmeasured = usage.tasksTotal() - usage.tasksMeasured();
            if (unmeasured > 0) {
                var reason = usage.unmeasuredReason() != null ? ": " + usage.unmeasuredReason() : "";
                out.printf("  %-12s %d of %d tasks not measured%s%n", "coverage", unmeasured, usage.tasksTotal(), reason);
            } else {
                out.printf("  %-12s %d of %d tasks measured%n", "coverage", usage.tasksTotal(), usage.tasksTotal());
            }

            printBreakdown(out, "  by phase", usage.byPhase(), 10, false, showComponents);
            printBreakdown(out, "  by role", usage.byRole(), 13, true, showComponents);
            printBreakdown(out, "  by model", usage.byModel(), 20, false, showComponents);
            printWaste(out, usage);

            if (!showComponents) {
                out.printf("raw components: plan report %s --tokens --components%n", planId);
            }
        }

        private static void printBreakdown(PrintWriter out, String header, List<TokenBreakdownItem> items, int nameWidth, boolean isRole, boolean showComponents) {
            if (items.isEmpty()) {
                return;
            }
            out.println(header);
            for (var item : items) {
                var val = TokenFormat.formatSplit(item.total(), item.input(), item.output(), item.floor(), "en");
                var unmeasuredItem = item.unmeasured() > 0 ? ("   " + item.unmeasured() + NOT_MEASURED) : "";
                var paddedName = padRight(item.name(), nameWidth);
                if (isRole) {
                    var pin = formatPin(item);
                    out.printf("               %s %-26s %s%s%n", paddedName, pin, val, unmeasuredItem);
                } else {
                    out.printf("               %s %s%s%n", paddedName, val, unmeasuredItem);
                }
                if (showComponents && item.components() != null) {
                    out.printf(FMT_INDENT, formatComponents(item.components()));
                }
            }
        }

        private static String padRight(String s, int width) {
            if (s.length() >= width) {
                return s;
            }
            return s + " ".repeat(width - s.length());
        }

        private static void printWaste(PrintWriter out, TokenUsageRecord usage) {
            if (usage.totalWasteTokens() > 0 || !usage.wasteRecords().isEmpty()) {
                var wasteTotal = TokenFormat.formatTokens(usage.totalWasteTokens(), false, "en");
                out.printf("  %-12s %s tokens (%d %% of measured)%n", "waste", wasteTotal, usage.wasteSharePercent());
                for (var w : usage.wasteRecords()) {
                    var label = switch (w.wasteClass()) {
                        case "no_product" -> "produced nothing";
                        case "retries" -> "retries";
                        case "remediation" -> "remediation";
                        default -> w.wasteClass();
                    };
                    var amount = TokenFormat.formatTokens(w.tokens(), false, "en");
                    var extra = w.causeSummary() != null ? w.causeSummary() : (w.tasksCount() + " tasks");
                    out.printf("               %-18s %-6s %s%n", label, amount, extra);
                }
            }
        }

        private static String formatComponents(@Nullable TokenComponentRecord c) {
            if (c == null) {
                return "";
            }
            return String.format("input %s · cache read %s · cache write %s · output %s · reasoning %s",
                                TokenFormat.formatTokens(c.input(), false, "en"),
                                TokenFormat.formatTokens(c.cacheRead(), false, "en"),
                                TokenFormat.formatTokens(c.cacheWrite(), false, "en"),
                                TokenFormat.formatTokens(c.output(), false, "en"),
                                TokenFormat.formatTokens(c.reasoning(), false, "en"));
        }

        private static String formatDuration(long seconds) {
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            if (hours > 0) {
                return hours + " h " + mins + " min";
            }
            return mins + " min";
        }
    }

    @Command(name = "resolve", description = "Resolves what holds a plan (UC-CTL-4).")
    @OperatorVerb
    static final class ResolveCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        @Parameters(index = "1", description = "Hold ID")
        String holdId = "";

        @Option(names = "--act", required = true, description = "The act that resolves the hold")
        String act = "";

        @Option(names = "--reason", description = "Reason for an act that cannot be undone")
        @Nullable
        String reason;

        ResolveCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var payload = new LinkedHashMap<String, Object>();
            payload.put("act", act);
            if (reason != null && !reason.isBlank()) {
                payload.put("reason", reason);
            }
            var endpoint = PLANS_API + OperatorUrlUtil.encodePathSegment(planId)
                                + "/holds/" + OperatorUrlUtil.encodePathSegment(holdId) + "/resolve";
            var response = context.runtime(true).send(HttpRequest.json("POST", endpoint, JsonTree.write(payload)));
            if (response.status() == 200) {
                try {
                    var result = HoldResolutionResult.fromJson(response.bodyText());
                    if (result.released()) {
                        out.println(MessageCatalogue.format("control.hold.released",
                                            Map.of("subject", result.subject())));
                    } else {
                        out.println(MessageCatalogue.format("control.hold.still_held",
                                            Map.of("cause", result.cause() != null ? result.cause() : "")));
                    }
                    out.flush();
                    return 0;
                } catch (IOException _) {
                    out.println(response.bodyText());
                    out.flush();
                    return 0;
                }
            }
            throw new OperatorRefusedException("plan resolve", response);
        }
    }

    @Command(name = "settings", description = "Shows or changes a plan's own settings values (UC-CTL-5).")
    @OperatorVerb
    static final class SettingsCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Plan ID")
        String planId = "";

        @Option(names = "--set", description = "Set aspect=value in the plan's overrides")
        @Nullable
        String set;

        @Option(names = "--remove", description = "Remove the plan's own value of an aspect")
        @Nullable
        String remove;

        @Option(names = "--yes", description = "Apply again although another client changed the value first")
        boolean yes;

        @Option(names = "--no-input", description = "Stop instead of asking on a conflict")
        boolean noInput;

        SettingsCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var settings = loadSettings();
            if (settings == null) {
                out.printf("Plan %s not found.%n", planId);
                out.flush();
                return 1;
            }

            if (set == null && remove == null) {
                printSettings(out, settings);
                return 0;
            }

            String aspect;
            String value;
            if (remove != null) {
                aspect = remove;
                value = null;
            } else {
                var eq = set.indexOf('=');
                if (eq < 0) {
                    out.println("Give the value as --set aspect=value.");
                    out.flush();
                    return 1;
                }
                aspect = set.substring(0, eq).trim();
                value = set.substring(eq + 1).trim();
            }
            return apply(out, settings, aspect, value);
        }

        private @Nullable PlanSettingsRecord loadSettings() throws IOException {
            var response = context.runtime(true).send(
                                HttpRequest.get(PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/settings"));
            if (response.status() == 200) {
                return PlanSettingsRecord.fromJson(response.bodyText());
            }
            return null;
        }

        private int apply(PrintWriter out, PlanSettingsRecord settings, String aspect, @Nullable String value)
                            throws IOException {
            var result = put(settings.revision(), aspect, value);
            if (result.applied()) {
                out.println(MessageCatalogue.format("control.settings.applies",
                                    Map.of("plan", planId, "version", result.revision())));
                out.flush();
                return 0;
            }
            if (result.conflict()) {
                var current = result.currentValue() != null ? result.currentValue() : "";
                if (yes) {
                    var again = put(result.revision(), aspect, value);
                    if (again.applied()) {
                        out.println(MessageCatalogue.format("control.settings.applies",
                                            Map.of("plan", planId, "version", again.revision())));
                        out.flush();
                        return 0;
                    }
                    return refused(out, aspect, value);
                }
                out.println(MessageCatalogue.format("common.conflict.changed", Map.of("value", current)));
                if (noInput) {
                    out.println(MessageCatalogue.format("common.conflict.no_input", Map.of("value", current)));
                    out.flush();
                    return 3;
                }
                out.flush();
                return 1;
            }
            return refused(out, aspect, value);
        }

        private PlanSettingsUpdateResult put(int revision, String aspect, @Nullable String value) throws IOException {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("aspect", aspect);
            if (value != null) {
                payload.put("value", value);
            } else {
                payload.put("remove", true);
            }
            payload.put("revision", revision);
            var endpoint = PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/settings";
            var response = context.runtime(true).send(HttpRequest.json("PUT", endpoint, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 409) {
                return PlanSettingsUpdateResult.fromJson(response.bodyText());
            }
            return PlanSettingsUpdateResult.tooLate(planId);
        }

        private int refused(PrintWriter out, String aspect, @Nullable String value) throws IOException {
            var response = context.runtime(true).send(
                                HttpRequest.get(PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/settings"));
            var reason = "the value does not fit";
            if (response.status() == 200) {
                var current = PlanSettingsRecord.fromJson(response.bodyText());
                var entry = current.value(aspect);
                if (entry != null && !entry.choices().isEmpty()) {
                    reason = "it is not one of " + String.join(", ", entry.choices());
                }
            }
            out.println(MessageCatalogue.format("control.settings.refused",
                                Map.of("value", value != null ? value : aspect, "reason", reason)));
            out.flush();
            return 1;
        }

        private void printSettings(PrintWriter out, PlanSettingsRecord settings) {
            out.println(MessageCatalogue.format("control.settings.title", Map.of("plan", planId)));
            for (var entry : settings.values()) {
                out.printf("  %-18s %-28s %s%n",
                                    entry.aspect(),
                                    entry.value() != null ? OutputSafety.stripControlSequences(entry.value()) : "\u2014",
                                    MessageCatalogue.format("control.settings.origin", Map.of("origin", entry.origin())));
            }
            out.flush();
        }
    }
}

