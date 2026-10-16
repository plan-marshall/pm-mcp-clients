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

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.report.DecisionRecord;
import de.planmarshall.api.report.DeliverableSummary;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReplanningRecord;
import de.planmarshall.api.report.TokenBreakdownItem;
import de.planmarshall.api.report.TokenComponentRecord;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.api.report.TokenUsageRecord;

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
}

