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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.HoldResolutionResult;
import de.planmarshall.api.control.PausePoint;
import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsUpdateResult;
import de.planmarshall.api.display.Display;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.process.ActivityEntryKind;
import de.planmarshall.api.process.NodeState;
import de.planmarshall.api.process.ProcessNode;
import de.planmarshall.api.process.ProcessTokens;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.process.TaskActivity;
import de.planmarshall.api.report.ChangeMapRecord;
import de.planmarshall.api.report.DecisionRecord;
import de.planmarshall.api.report.DeliverableSummary;
import de.planmarshall.api.report.OwnCallRecord;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReplanningRecord;
import de.planmarshall.api.report.TokenBreakdownItem;
import de.planmarshall.api.report.TokenComponentRecord;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.api.report.TokenUsageRecord;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.planview.PlanCompletionLevel;
import de.planmarshall.operator.planview.PlanViewLevel;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiScreenDriver;

import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

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

    @Option(names = "--depth", paramLabel = "<n>", converter = DepthConverter.class,
                        description = "Opens n levels of the process tree (default 3).")
    @Nullable
    Integer depth;

    @Option(names = {"--task", "--activity"}, description = "Task ID for worker activity")
    @Nullable
    String activity;

    @Option(names = "--tokens", description = "Show tokens so far")
    boolean tokens;

    @Option(names = "--components", description = "Show raw token components")
    boolean components;

    @Option(names = "--watch", description = "Watch updates")
    boolean watch;

    @Option(names = "--print", description = "Prints the plan into the scrollback instead of opening the full-screen view.")
    boolean print;

    PlanCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var fullScreen = runFullScreen(out);
        if (fullScreen != null) {
            return fullScreen;
        }
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
                    } catch (IOException _) {
                        // Not a task activity the parser can read: print the body as it came
                        out.println(response.bodyText());
                        out.flush();
                        return 0;
                    }
                }
                if (response.status() == 404) {
                    RefusalRenderer.render(out, context.locale(), ApiErrorCode.SCOPE_NOT_FOUND);
                    return 1;
                }
                throw new OperatorRefusedException("plan activity " + planId + " " + activity, response);
            }

            var path = PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/process";
            if (depth != null) {
                path += "?depth=" + depth;
            }
            var response = runtime.send(HttpRequest.get(path));
            if (response.status() == 200) {
                var body = response.bodyText();
                if (body.contains("\"nodes\"") || body.contains("\"plan_id\"")) {
                    try {
                        var tree = ProcessTree.fromJson(body);
                        printProcessTree(out, tree);
                        return 0;
                    } catch (IOException _) {
                        // Not a process tree the parser can read: fall through to print bodyText
                    }
                }
                out.println(body);
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                RefusalRenderer.render(out, context.locale(), ApiErrorCode.SCOPE_NOT_FOUND);
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

    /**
     * Opens the full-screen plan view when the verb is run interactively for one plan and without
     * a scrollback-only flag ({@code --print}, {@code --all}, {@code --depth}, {@code --activity},
     * {@code --tokens}, {@code --components}, {@code --watch}).
     *
     * @param out the writer of the console
     * @return {@code 0} when the view was left normally, {@code 130} when a dialogue of the view was
     *         interrupted, or {@code null} to use the scrollback
     * @throws IOException when the terminal cannot be opened
     */
    private @Nullable Integer runFullScreen(PrintWriter out) throws IOException {
        if (planId == null || planId.isBlank() || print || all || depth != null || activity != null
                            || tokens || components || watch || !context.isInteractiveTerminal()) {
            return null;
        }
        Terminal terminal = null;
        try {
            terminal = context.createTerminal();
        } catch (IOException _) {
            return null;
        }
        var ownsTerminal = context.ownsTerminal(terminal);
        context.useTerminal(terminal);
        try {
            var availability = TuiScreenDriver.availability(context, terminal, TuiLayout.MIN_COLUMNS);
            if (!availability.available()) {
                TuiScreenDriver.printUnavailable(context, spec.commandLine().getErr(), availability);
                return null;
            }
            var app = new OverviewFullScreenApp(
                                context, terminal,
                                OverviewFullScreenApp.InitialSelection.plan(planId),
                                context.clock(),
                                new OverviewStateStore(context.stateDirectory()),
                                context.noticeBuffer());
            if (app.run(out) != TuiScreenDriver.Result.LEFT) {
                return null;
            }
            return app.interrupted() ? StartPrompt.EXIT_INTERRUPTED : 0;
        } finally {
            try {
                context.clearTerminal(terminal);
            } catch (IllegalStateException _) {
                // Scope already cleared
            }
            if (ownsTerminal) {
                try {
                    terminal.close();
                } catch (IOException _) {
                    // Terminal close failure ignored
                }
            }
        }
    }

    private void printTaskActivity(PrintWriter out, String planId, TaskActivity act) {
        var locale = context.clientEnvironment().locale();
        var cleanPlan = OutputSafety.stripControlSequences(planId);
        var cleanTask = OutputSafety.stripControlSequences(act.taskId());
        out.printf("%s · %s%n", cleanPlan, cleanTask);

        var duration = Display.duration(locale, act.durationSeconds());
        var tokensStr = TokenFormat.formatTokens(act.tokensSoFar(), false, locale) + " tokens";
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

        var locale = context.clientEnvironment().locale();
        var durationStr = tree.durationSeconds() > 0 ? " · " + Display.duration(locale, tree.durationSeconds()) : "";
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
            out.printf("  tokens so far   %-25s plan %s --tokens%n", formatTokens(tree.tokensSoFar(), locale), planStr);
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

        var locale = context.clientEnvironment().locale();
        var runStr = node.runCount() > 1 && stateEnum == NodeState.DONE ? " ×" + node.runCount() : "";
        var currentRunStr = node.currentRun() > 1 && stateEnum == NodeState.RUNNING ? "run " + node.currentRun() : "";
        var durationStr = node.durationSeconds() > 0 ? Display.duration(locale, node.durationSeconds()) : "";

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
                var taskGlyph = PlanViewLevel.taskGlyph(task.status());
                var taskStateCol = String.format("%-9s", OutputSafety.stripControlSequences(task.status()));
                var taskDesc = OutputSafety.stripControlSequences(task.taskId() + " (" + task.role() + ")");
                var taskInfoParts = new ArrayList<String>();
                if (task.durationSeconds() > 0) {
                    taskInfoParts.add(Display.duration(locale, task.durationSeconds()));
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
            var jobGlyph = PlanViewLevel.taskGlyph(job.status());
            var jobStateCol = String.format("%-9s", OutputSafety.stripControlSequences(job.status()));
            var jobDesc = OutputSafety.stripControlSequences("job (" + job.kind() + ")");
            var jobInfoParts = new ArrayList<String>();
            if (job.durationSeconds() > 0) {
                jobInfoParts.add(Display.duration(locale, job.durationSeconds()));
            }
            if (job.waitsSlot()) {
                jobInfoParts.add("waits slot");
            }
            if (job.outcome() != null && !job.outcome().isBlank()) {
                jobInfoParts.add(OutputSafety.stripControlSequences(job.outcome()));
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

    static String formatTokens(ProcessTokens tokens, String locale) {
        if (tokens.noneYet()) {
            return "none yet";
        }
        var prefix = tokens.floor() ? "≥ " : "";
        return String.format("%s%s (%s in · %s out)",
                            prefix,
                            TokenFormat.formatTokens(tokens.total(), false, locale),
                            TokenFormat.formatTokens(tokens.input(), false, locale),
                            TokenFormat.formatTokens(tokens.output(), false, locale));
    }

    static String formatTokens(ProcessTokens tokens) {
        return formatTokens(tokens, "en");
    }

    private static int parseDepth(@Nullable Integer depth, boolean all) {
        if (all) {
            return Integer.MAX_VALUE;
        }
        return depth != null ? depth : 3;
    }

    /**
     * Parses {@code --depth <n>}: a positive number of levels. The value is validated at parse
     * time, so an explicit {@code 0} is refused with the usage and never sent to the runtime.
     */
    static final class DepthConverter implements ITypeConverter<Integer> {

        @Override
        public Integer convert(String value) {
            try {
                int depth = Integer.parseInt(value.trim());
                if (depth < 1) {
                    throw new TypeConversionException(
                                        MessageCatalogue.get("operator.plan.depth_invalid"));
                }
                return depth;
            } catch (NumberFormatException _) {
                throw new TypeConversionException(MessageCatalogue.get("operator.plan.depth_invalid"));
            }
        }
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
        @Nullable
        String lesson;

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
                                .withProject(common.project)
                                .withDescription(common.description);
            answers.title = common.title;
            answers.id = common.id;
            answers.documents.addAll(documents);
            answers.issues.addAll(issues);
            answers.lesson = lesson;
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
            String point;
            try {
                point = point(out);
            } catch (StartPrompt.InterruptException _) {
                return StartPrompt.EXIT_INTERRUPTED;
            } catch (StartPrompt.StartLeftException _) {
                return 0;
            }
            if (point == null) {
                return 3;
            }

            var payload = Map.of("point", point);
            var endpoint = PLANS_API + OperatorUrlUtil.encodePathSegment(planId) + "/pause";
            var response = context.runtime(true).send(
                                HttpRequest.json("POST", endpoint, JsonTree.write(payload)));
            if (response.status() == 200 || response.status() == 202) {
                if (PausePoint.NOW.value().equals(point)) {
                    out.printf("✓ %s paused%n", planId);
                } else {
                    out.printf("✓ %s pausing after the %s%n", planId,
                                        PausePoint.AFTER_PHASE.value().equals(point) ? "phase" : "task");
                }
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("plan pause", response);
        }

        /**
         * @param out the verb's output
         * @return the point the pause takes, or {@code null} when nothing may be asked and no point
         *         was given
         */
        private @Nullable String point(PrintWriter out) {
            if (now) {
                return PausePoint.NOW.value();
            }
            if (afterPhase) {
                return PausePoint.AFTER_PHASE.value();
            }
            if (afterTask) {
                return PausePoint.AFTER_TASK.value();
            }
            if (noInput || !context.isInteractiveTerminal()) {
                out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                    "operator.actions.pause.stopped", Map.of()));
                out.flush();
                return null;
            }
            return askedPoint(out);
        }

        private String askedPoint(PrintWriter out) {
            var locale = context.clientEnvironment().locale();
            var prompt = new StartPrompt(context, out, locale, false);
            var choice = ActionSupport.pausePrompt(prompt, planId, null).ask();
            var point = PausePoint.fromValue(choice.key());
            return point != null ? point.value() : PausePoint.AFTER_TASK.value();
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
                RefusalRenderer.render(out, context.locale(), ApiErrorCode.SCOPE_NOT_FOUND);
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
            var locale = context.clientEnvironment().locale();

            printReportHeader(out, report, locale);
            var ran = formatRan(report, locale, context.clock().getZone());
            printReportMetadata(out, report, locale, ran);

            printDeliverables(out, report.deliverables(), locale);
            printReplanning(out, report.replanningRecords());
            printDecisions(out, report.decisionRecords(), locale);
            printOwnCalls(out, report.ownCalls(), locale);
            printChangeMap(out, report.changeMap());

            var usage = report.tokenUsage();
            if (usage != null) {
                printTokensSummary(out, usage, ran, report.planId(), showTokens, showComponents, locale);
            }
            out.flush();
        }

        private static void printReportHeader(PrintWriter out, PlanCompletionReport report, String locale) {
            var headParts = new ArrayList<String>();
            headParts.add(ConsoleNotice.stripControlSequences(report.planId()));
            var reportTitle = MessageCatalogue.get(locale, "report.completion.open");
            if (!"de".equalsIgnoreCase(locale) && !locale.startsWith("de")) {
                reportTitle = reportTitle.toLowerCase(Locale.ROOT);
            }
            headParts.add(reportTitle);
            headParts.add(ConsoleNotice.stripControlSequences(PlanCompletionLevel.formatOutcome(locale, report.outcome())));
            if (report.pullRequestNumber() != null) {
                headParts.add("#" + report.pullRequestNumber());
            }
            var ended = report.ended();
            headParts.add(ended != null && !ended.isBlank()
                                ? ConsoleNotice.stripControlSequences(ended)
                                : MessageCatalogue.format(locale, "common.unknown", Map.of()));
            out.println(String.join(" · ", headParts));
        }

        private static void printReportMetadata(PrintWriter out, PlanCompletionReport report, String locale, @Nullable String ran) {
            var from = report.from();
            if (from != null && from.project() != null) {
                var path = "in " + ConsoleNotice.stripControlSequences(from.project());
                if (from.epic() != null && !from.epic().isBlank()) {
                    path += " › " + ConsoleNotice.stripControlSequences(from.epic());
                }
                out.printf("  %-50s plan %s%n", path, ConsoleNotice.stripControlSequences(report.planId()));
            }

            var outcomeDesc = report.terminalReason() != null ? report.terminalReason() : report.outcome();
            out.printf(FMT_14S, "outcome", ConsoleNotice.stripControlSequences(outcomeDesc));
            if (report.pullRequestUrl() != null && !report.pullRequestUrl().isBlank()) {
                out.printf(FMT_14S, "", ConsoleNotice.stripControlSequences(report.pullRequestUrl()));
            }

            var reason = report.reason() != null && !report.reason().isBlank()
                                ? report.reason()
                                : MessageCatalogue.format(locale, "common.none", Map.of());
            out.printf(FMT_14S, "reason", ConsoleNotice.stripControlSequences(reason));

            if (ran != null) {
                out.printf(FMT_14S, "ran", ran);
            }
        }

        private static @Nullable String formatRan(PlanCompletionReport report, String locale, ZoneId zone) {
            var written = report.ran();
            if (written != null) {
                return ConsoleNotice.stripControlSequences(written);
            }
            if (report.durationSeconds() <= 0) {
                return null;
            }
            var duration = Display.duration(locale, report.durationSeconds());
            var started = report.startedAt();
            var endedAt = report.endedAt();
            if (started == null || endedAt == null) {
                return duration;
            }
            // Ran is the wall time from start to end and how long the operator paused it
            // (completion-report.adoc#layout): the pause is the operator's, never a name the
            // runtime wrote.
            var timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);
            var paused = report.pausedSeconds() > 0 ? Display.duration(locale, report.pausedSeconds()) : "none";
            return MessageCatalogue.format(locale, "report.completion.ran_value", Map.of(
                                "duration", duration,
                                "start", timeFmt.format(started),
                                "end", timeFmt.format(endedAt),
                                "paused", paused));
        }

        private static void printDeliverables(PrintWriter out, List<DeliverableSummary> deliverables, String locale) {
            if (deliverables.isEmpty()) {
                out.printf(FMT_14S, "deliverables", "no deliverables yet: it ended in refine");
            } else {
                long doneCount = deliverables.stream().filter(d -> "done".equalsIgnoreCase(d.state())).count();
                out.printf("  %-14s %d of %d done%n", "deliverables", doneCount, deliverables.size());
                for (var deliv : deliverables) {
                    var stateStr = Display.word(locale, "task", deliv.state());
                    out.printf("                   %-5s %s%n", stateStr, ConsoleNotice.stripControlSequences(deliv.name()));
                }
            }
        }

        private static void printReplanning(PrintWriter out, List<ReplanningRecord> replanningRecords) {
            if (!replanningRecords.isEmpty()) {
                var first = replanningRecords.get(0);
                var repSummary = String.format("%d · after %s (%s): %s",
                                    replanningRecords.size(),
                                    ConsoleNotice.stripControlSequences(first.trigger()),
                                    first.time() != null ? ConsoleNotice.stripControlSequences(first.time()) : "",
                                    ConsoleNotice.stripControlSequences(first.change()));
                out.printf(FMT_14S, "re-planning", repSummary);
            }
        }

        private static void printDecisions(PrintWriter out, List<DecisionRecord> decisionRecords, String locale) {
            if (!decisionRecords.isEmpty()) {
                var decList = new ArrayList<String>();
                for (var d : decisionRecords) {
                    var decText = ConsoleNotice.stripControlSequences(d.id());
                    if (d.question() != null && !d.question().isBlank()) {
                        decText += " " + ConsoleNotice.stripControlSequences(d.question());
                    }
                    decText += " (" + decidedBy(d, locale) + ")";
                    decList.add(decText);
                }
                out.printf("  %-14s %d · %s%n", "decisions", decisionRecords.size(), String.join(" · ", decList));
            }
        }

        /**
         * How a decision was taken, as its record names it (completion-report.adoc#texts,
         * {@code report.completion.decision_by}): by you with the channel, left to the plan, by a
         * quorum, or withdrawn; any other value is shown as the runtime wrote it.
         */
        private static String decidedBy(DecisionRecord decision, String locale) {
            var by = decision.by();
            var channel = decision.channel();
            if ("operator".equals(by) && (channel == null || channel.isBlank())) {
                return MessageCatalogue.format(locale, "operator.actions.chat.you", Map.of());
            }
            return MessageCatalogue.format(locale, "report.completion.decision_by", Map.of(
                                "by", ConsoleNotice.stripControlSequences(by),
                                "channel", channel != null ? ConsoleNotice.stripControlSequences(channel) : ""));
        }

        private static void printOwnCalls(PrintWriter out, List<OwnCallRecord> ownCalls, String locale) {
            if (ownCalls.isEmpty()) {
                return;
            }
            out.printf("  %-14s %d%n", "own calls", ownCalls.size());
            for (var ownCall : ownCalls) {
                var kind = ownCallKind(ownCall.kind());
                var description = ConsoleNotice.stripControlSequences(ownCall.description());
                var judgment = ownCallJudgment(ownCall, locale);
                if (hasOwnCallDetails(ownCall)) {
                    out.printf("                   %-19s %s%n", kind, description);
                    var details = new ArrayList<String>();
                    if (ownCall.limit() != null && !ownCall.limit().isBlank()) {
                        details.add("limit " + ConsoleNotice.stripControlSequences(ownCall.limit()));
                    }
                    if (ownCall.endsWhen() != null && !ownCall.endsWhen().isBlank()) {
                        details.add("ends when " + ConsoleNotice.stripControlSequences(ownCall.endsWhen()));
                    }
                    details.add(judgment);
                    out.printf("                   %-19s %s%n", "", String.join(" · ", details));
                } else {
                    out.printf("                   %-19s %s · %s%n", kind, description, judgment);
                }
            }
        }

        private static String ownCallKind(String kind) {
            return switch (kind) {
                case "design" -> "design call";
                case "shortcut" -> "shortcut";
                case "dependency" -> "replaced dependency";
                default -> ConsoleNotice.stripControlSequences(kind);
            };
        }

        private static boolean hasOwnCallDetails(OwnCallRecord ownCall) {
            return ownCall.limit() != null && !ownCall.limit().isBlank()
                                || ownCall.endsWhen() != null && !ownCall.endsWhen().isBlank();
        }

        private static String ownCallJudgment(OwnCallRecord ownCall, String locale) {
            if ("conflicts".equalsIgnoreCase(ownCall.judgment())) {
                var target = ownCall.conflictTarget();
                var item = target != null && !target.isBlank()
                                    ? ConsoleNotice.stripControlSequences(target)
                                    : MessageCatalogue.get(locale, "common.unknown");
                return "conflicts with " + item;
            }
            if ("consistent".equalsIgnoreCase(ownCall.judgment())) {
                return "consistent";
            }
            return ConsoleNotice.stripControlSequences(ownCall.judgment());
        }

        private static void printChangeMap(PrintWriter out, @Nullable ChangeMapRecord changeMap) {
            if (changeMap == null) {
                return;
            }
            var value = switch (changeMap.state().toLowerCase(Locale.ROOT)) {
                case "ok" -> "complete";
                case "partial" -> "partial";
                case "indeterminate" -> "indeterminate";
                default -> ConsoleNotice.stripControlSequences(changeMap.state());
            };
            if (changeMap.cause() != null && !changeMap.cause().isBlank()) {
                value += " · cause: " + ConsoleNotice.stripControlSequences(changeMap.cause());
            }
            out.printf(FMT_14S, "change map", value);
            for (var line : changeMap.diagram().split("\n", -1)) {
                out.printf("                   %s%n", ConsoleNotice.stripControlSequences(line));
            }
        }

        private void printTokensSummary(PrintWriter out, TokenUsageRecord usage, @Nullable String ran, String planId, boolean showTokens, boolean showComponents, String locale) {
            var totalSplit = TokenFormat.formatSplit(usage.total(), usage.input(), usage.output(), usage.floor(), locale);
            if (showTokens) {
                out.printf("tokens         %s · ran %s%n", totalSplit, ran != null ? ran : "");
                printTokensDetail(out, usage, planId, showComponents, locale);
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
            // The pin as the plan's records name it (completion-report.adoc#tokens): an effort the
            // records do not carry is left out, never stood in.
            var pin = " " + ConsoleNotice.stripControlSequences(item.harnessModel());
            var effort = item.effort();
            return effort != null && !effort.isBlank()
                                ? pin + " · " + ConsoleNotice.stripControlSequences(effort)
                                : pin;
        }

        private void printTokensDetail(PrintWriter out, TokenUsageRecord usage, String planId, boolean showComponents, String locale) {
            int unmeasured = usage.tasksTotal() - usage.tasksMeasured();
            if (unmeasured > 0) {
                var reason = usage.unmeasuredReason() != null
                                    ? ": " + ConsoleNotice.stripControlSequences(usage.unmeasuredReason())
                                    : "";
                out.printf("  %-12s %d of %d tasks not measured%s%n", "coverage", unmeasured, usage.tasksTotal(), reason);
            } else {
                out.printf("  %-12s %d of %d tasks measured%n", "coverage", usage.tasksTotal(), usage.tasksTotal());
            }

            printBreakdown(out, "  by phase", usage.byPhase(), 10, false, showComponents, locale);
            printBreakdown(out, "  by role", usage.byRole(), 13, true, showComponents, locale);
            printBreakdown(out, "  by model", usage.byModel(), 20, false, showComponents, locale);
            printWaste(out, usage, locale);

            if (!showComponents) {
                out.printf("raw components: plan report %s --tokens --components%n", planId);
            }
        }

        private static void printBreakdown(PrintWriter out, String header, List<TokenBreakdownItem> items, int nameWidth, boolean isRole, boolean showComponents, String locale) {
            if (items.isEmpty()) {
                return;
            }
            out.println(header);
            for (var item : items) {
                var val = TokenFormat.formatSplit(item.total(), item.input(), item.output(), item.floor(), locale);
                var unmeasuredItem = item.unmeasured() > 0 ? ("   " + item.unmeasured() + NOT_MEASURED) : "";
                var paddedName = padRight(ConsoleNotice.stripControlSequences(item.name()), nameWidth);
                if (isRole) {
                    var pin = formatPin(item);
                    out.printf("               %s %-26s %s%s%n", paddedName, pin, val, unmeasuredItem);
                } else {
                    out.printf("               %s %s%s%n", paddedName, val, unmeasuredItem);
                }
                if (showComponents && item.components() != null) {
                    out.printf(FMT_INDENT, formatComponents(item.components(), locale));
                }
            }
        }

        private static String padRight(String s, int width) {
            if (s.length() >= width) {
                return s;
            }
            return s + " ".repeat(width - s.length());
        }

        private static void printWaste(PrintWriter out, TokenUsageRecord usage, String locale) {
            if (usage.totalWasteTokens() > 0 || !usage.wasteRecords().isEmpty()) {
                var wasteTotal = TokenFormat.formatTokens(usage.totalWasteTokens(), false, locale);
                out.printf("  %-12s %s tokens (%d %% of measured)%n", "waste", wasteTotal, usage.wasteSharePercent());
                for (var w : usage.wasteRecords()) {
                    var label = switch (w.wasteClass()) {
                        case "no_product" -> "produced nothing";
                        case "retries" -> "retries";
                        case "remediation" -> "remediation";
                        default -> ConsoleNotice.stripControlSequences(w.wasteClass());
                    };
                    var amount = TokenFormat.formatTokens(w.tokens(), false, locale);
                    var extra = w.causeSummary() != null
                                        ? ConsoleNotice.stripControlSequences(w.causeSummary())
                                        : (w.tasksCount() + " tasks");
                    out.printf("               %-18s %-6s %s%n", label, amount, extra);
                }
            }
        }

        private static String formatComponents(@Nullable TokenComponentRecord c, String locale) {
            if (c == null) {
                return "";
            }
            return String.format("input %s · cache read %s · cache write %s · output %s · reasoning %s",
                                TokenFormat.formatTokens(c.input(), false, locale),
                                TokenFormat.formatTokens(c.cacheRead(), false, locale),
                                TokenFormat.formatTokens(c.cacheWrite(), false, locale),
                                TokenFormat.formatTokens(c.output(), false, locale),
                                TokenFormat.formatTokens(c.reasoning(), false, locale));
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
            if (set != null || remove != null) {
                context.gate().verify();
            }
            var out = spec.commandLine().getOut();
            var settings = loadSettings();
            if (settings == null) {
                RefusalRenderer.render(out, context.locale(), ApiErrorCode.SCOPE_NOT_FOUND);
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
            return new PlanSettingsUpdateResult(false, false, revision, null, null);
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

