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
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.ActivePlanSummary;
import de.planmarshall.api.overview.AlongsideLanding;
import de.planmarshall.api.overview.EndedWorkItem;
import de.planmarshall.api.overview.EndedWorkPage;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.api.runtime.RuntimeRecord;
import de.planmarshall.api.runtime.RuntimeUnavailableException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator status}: prints the runtime status and overview tree of running projects,
 * plans, and epics from {@code GET /api/v1/status} and {@code GET /api/v1/projects}.
 * Never starts the runtime and writes nothing.
 */
@Command(name = "status", description = "Shows the runtime status; never starts the runtime.")
final class StatusCommand implements Callable<Integer> {

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

    @Option(names = "--ended", description = "Show ended work as a tree")
    boolean ended;

    @Option(names = "--watch", description = "Watch status updates")
    boolean watch;

    StatusCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var record = RuntimeRecord.read(context.paths().runtimeRecord());
        if (record.isEmpty() || !record.get().processAlive()) {
            return notRunning();
        }

        if (ended) {
            return handleEnded(out);
        }

        Map<String, Object> status;
        try {
            status = fetchProjectStatus();
        } catch (RuntimeUnavailableException _) {
            return notRunning();
        }

        if (json) {
            if (project != null && !".".equals(project)) {
                try {
                    var detailRes = context.runtime(false).send(HttpRequest.get("/api/v1/projects/" + project));
                    if (detailRes.status() == 200) {
                        out.println(detailRes.bodyText());
                        out.flush();
                        return 0;
                    }
                } catch (IOException _) {
                    // Fall back to status json
                }
            }
            out.println(JsonTree.write(status));
            out.flush();
            return 0;
        }

        // Try rendering hierarchical overview tree
        try {
            var projectsResponse = context.runtime(false).send(HttpRequest.get("/api/v1/projects"));
            if (projectsResponse.status() == 200) {
                var overview = ProjectOverviewResponse.fromJson(projectsResponse.bodyText());
                if (!overview.projects().isEmpty() || status.containsKey("running_since")
                                    || status.containsKey("workers_count") || project != null) {
                    renderOverviewTree(out, status, overview.projects());
                    out.flush();
                    return 0;
                }
            }
        } catch (IOException _) {
            // Fall through to legacy render
        }

        out.print(render(status));
        out.flush();
        return 0;
    }

    private int handleEnded(PrintWriter out) throws IOException {
        String locale = context.clientEnvironment().locale();
        if (json) {
            try {
                var path = project != null && !".".equals(project)
                                    ? "/api/v1/projects/" + project + "/ended"
                                    : "/api/v1/status?ended=true";
                var response = context.runtime(false).send(HttpRequest.get(path));
                if (response.status() == 200) {
                    out.println(response.bodyText());
                    out.flush();
                    return 0;
                }
                throw new OperatorRefusedException("status --ended", response);
            } catch (RuntimeUnavailableException _) {
                return notRunning();
            }
        }

        try {
            if (project != null && !".".equals(project)) {
                renderEndedPages(out, project, locale, true, true);
                out.flush();
                return 0;
            }

            var projectsRes = context.runtime(false).send(HttpRequest.get("/api/v1/projects"));
            if (projectsRes.status() == 200) {
                var overview = ProjectOverviewResponse.fromJson(projectsRes.bodyText());
                if (overview.projects().isEmpty()) {
                    out.println(MessageCatalogue.format(locale, "overview.ended.empty", Map.of("project", "all")));
                } else {
                    for (var p : overview.projects()) {
                        renderEndedPages(out, p.projectId(), locale, false, false);
                    }
                }
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("status --ended", projectsRes);
        } catch (RuntimeUnavailableException _) {
            return notRunning();
        }
    }

    private void renderEndedPages(PrintWriter out, String projectId, String locale, boolean renderEmpty, boolean failHard) throws IOException {
        int pageNum = 1;
        boolean headerPrinted = false;
        var cleanProject = OutputSafety.stripControlSequences(projectId);

        while (true) {
            var res = context.runtime(false).send(HttpRequest.get("/api/v1/projects/" + projectId + "/ended?page=" + pageNum));
            if (res.status() != 200) {
                if (failHard) {
                    throw new OperatorRefusedException("status --ended", res);
                }
                break;
            }
            var page = EndedWorkPage.fromJson(res.bodyText());
            if (!page.items().isEmpty()) {
                if (!headerPrinted) {
                    out.println(MessageCatalogue.format(locale, "operator.status.ended_title", Map.of("project", cleanProject)));
                    headerPrinted = true;
                }
                renderEndedItems(out, page.items(), locale);
            }
            if (!page.hasOlder() || page.items().isEmpty()) {
                break;
            }
            pageNum++;
        }

        if (!headerPrinted && renderEmpty) {
            out.println(MessageCatalogue.format(locale, "operator.status.ended_title", Map.of("project", cleanProject)));
            out.println("  " + MessageCatalogue.format(locale, "overview.ended.empty", Map.of("project", cleanProject)));
        }
    }

    private void renderEndedItems(PrintWriter out, List<EndedWorkItem> items, String locale) {
        for (var item : items) {
            var kind = "epic".equalsIgnoreCase(item.kind())
                                ? MessageCatalogue.format(locale, "overview.kind.epic", Map.of())
                                : MessageCatalogue.format(locale, "overview.kind.plan", Map.of());
            var title = OutputSafety.stripControlSequences(item.title());
            var outcome = "epic".equalsIgnoreCase(item.kind())
                                ? MessageCatalogue.format(locale, "overview.ended.epic_end", Map.of("end", Objects.toString(item.terminalReason(), "ended")))
                                : MessageCatalogue.format(locale, "overview.ended.outcome", Map.of("outcome", Objects.toString(item.outcome(), "ended")));
            var time = formatRelativeTime(item.endedAt());
            out.printf("  %-4s  %-30s   %s \u00b7 %s%n", kind, title, outcome, time);

            if (item.pullRequestNumber() != null) {
                var prText = MessageCatalogue.format(locale, "operator.status.pull_request", Map.of(
                                    "number", item.pullRequestNumber(),
                                    "url", OutputSafety.stripControlSequences(Objects.toString(item.pullRequestUrl(), ""))
                ));
                out.printf("        %s%n", prText);
            }
            if (item.reason() != null && !item.reason().isBlank()) {
                var reasonText = MessageCatalogue.format(locale, "operator.status.reason", Map.of(
                                    "reason", OutputSafety.stripControlSequences(item.reason())
                ));
                out.printf("        %s%n", reasonText);
            }
        }
    }

    private void renderOverviewTree(PrintWriter out, Map<String, Object> status, List<ProjectSummary> projects) {
        String locale = context.clientEnvironment().locale();

        // 1. Runtime summary line
        var version = Objects.toString(status.get("version"), "0.1");
        if (!version.startsWith("pm-mcpd")) {
            version = "pm-mcpd " + version;
        }
        var runningSince = Objects.toString(status.get("running_since"), "-");
        var workers = status.get("workers_count") != null ? status.get("workers_count") : 0;
        var slots = status.get("build_slots_used") != null ? status.get("build_slots_used") : 0;
        var total = status.get("build_slots_total") != null ? status.get("build_slots_total") : 0;

        var header = MessageCatalogue.format(locale, "operator.status.runtime", Map.of(
                            "version", version,
                            "time", runningSince,
                            "workers", workers,
                            "slots", slots,
                            "total", total
        ));
        out.println(header);
        out.println();

        var filteredProjects = projects.stream()
                            .filter(p -> project == null || ".".equals(project) || project.equals(p.projectId()))
                            .toList();

        if (filteredProjects.isEmpty()) {
            out.println(MessageCatalogue.format(locale, "overview.projects.empty", Map.of()));
            return;
        }

        boolean groupByEpic = byEpic || "epic".equalsIgnoreCase(by);

        for (var p : filteredProjects) {
            renderProjectNode(out, p, groupByEpic, locale);
        }
    }

    private void renderProjectNode(PrintWriter out, ProjectSummary p, boolean groupByEpic, String locale) {
        String markerCol = p.needsOperatorCount() > 0
                            ? String.format("! %-2d ", p.needsOperatorCount())
                            : (p.setupNeedsOperator() ? "! 1  " : "     ");

        var pName = OutputSafety.stripControlSequences(p.displayName() != null ? p.displayName() : p.projectId());
        String countsPart;
        if (p.setupNeedsOperator() && p.activePlansCount() == 0) {
            countsPart = MessageCatalogue.format(locale, "overview.project.setup", Map.of());
        } else if (p.activePlansCount() > 0 && p.activeEpicsCount() > 0) {
            countsPart = MessageCatalogue.format(locale, "overview.project.counts", Map.of(
                                "plans", p.activePlansCount(),
                                "epics", p.activeEpicsCount()
            ));
        } else if (p.activePlansCount() > 0) {
            countsPart = MessageCatalogue.format(locale, "overview.project.plans_only", Map.of(
                                "plans", p.activePlansCount()
            ));
        } else if (p.activeEpicsCount() > 0) {
            countsPart = MessageCatalogue.format(locale, "overview.project.epics_only", Map.of(
                                "epics", p.activeEpicsCount()
            ));
        } else {
            countsPart = "";
        }
        out.printf("%s%s \u00b7 %s%n", markerCol, pName, countsPart);

        ProjectDetailResponse detail = null;
        try {
            var res = context.runtime(false).send(HttpRequest.get("/api/v1/projects/" + p.projectId()));
            if (res.status() == 200) {
                detail = ProjectDetailResponse.fromJson(res.bodyText());
            }
        } catch (IOException _) {
            // detail unavailable
        }

        if (detail == null) {
            return;
        }

        if (groupByEpic) {
            renderProjectByEpic(out, detail, locale);
        } else {
            renderProjectByPlan(out, detail, locale);
        }

        renderAlongsideLandings(out, detail.alongsideLandings(), locale);
    }

    private void renderProjectByPlan(PrintWriter out, ProjectDetailResponse detail, String locale) {
        for (var plan : detail.plans()) {
            renderPlanRow(out, plan, "     ", locale);
        }
    }

    private void renderPlanRow(PrintWriter out, ActivePlanSummary plan, String baseIndent, String locale) {
        String marker;
        if (plan.needsOperatorCount() != null && plan.needsOperatorCount() > 0) {
            var kind = plan.needsOperatorKind() != null
                                ? MessageCatalogue.format(locale, "overview.needs." + plan.needsOperatorKind(), Map.of())
                                : "attention";
            marker = String.format("! %-8s ", kind);
        } else {
            marker = "           ";
        }

        var title = OutputSafety.stripControlSequences(plan.title());
        var phase = OutputSafety.stripControlSequences(plan.currentPhase());
        var rawState = plan.phaseState();
        var stateKey = "waiting_for_you".equals(rawState) ? "waiting" : rawState;
        var state = stateKey != null
                            ? MessageCatalogue.format(locale, "overview.state." + stateKey, Map.of())
                            : "running";
        var timeInPhase = formatDuration(plan.phaseEnteredAt());
        var timePart = timeInPhase.isEmpty() ? "" : " \u00b7 " + timeInPhase;

        out.printf("%s%splan  %-30s   %s \u00b7 %s%s%n", baseIndent, marker, title, phase, state, timePart);

        // Details line
        var details = new ArrayList<String>();
        if (plan.epicTitle() != null || plan.epicId() != null) {
            var epTitle = plan.epicTitle() != null ? plan.epicTitle() : plan.epicId();
            details.add(MessageCatalogue.format(locale, "overview.plan.belongs_to",
                                Map.of("epic", "epic " + OutputSafety.stripControlSequences(epTitle))));
        }
        if (plan.tasksTotal() > 0) {
            details.add(MessageCatalogue.format(locale, "overview.progress.tasks",
                                Map.of("done", plan.tasksDone(), "total", plan.tasksTotal())));
        }
        if (plan.offTrackSignal() != null) {
            details.add(MessageCatalogue.format(locale, "overview.plan.off_track",
                                Map.of("signal", OutputSafety.stripControlSequences(plan.offTrackSignal()))));
        }

        if (!details.isEmpty()) {
            out.printf("%s                       %s%n", baseIndent, String.join(" \u00b7 ", details));
        }
        if (plan.pullRequestNumber() != null) {
            var prText = MessageCatalogue.format(locale, "operator.status.pull_request", Map.of(
                                "number", plan.pullRequestNumber(),
                                "url", OutputSafety.stripControlSequences(Objects.toString(plan.pullRequestUrl(), ""))
            ));
            out.printf("%s                       %s%n", baseIndent, prText);
        }
    }

    private void renderProjectByEpic(PrintWriter out, ProjectDetailResponse detail, String locale) {
        for (var epic : detail.epics()) {
            String epicMarker = epic.needsOperatorCount() != null && epic.needsOperatorCount() > 0
                                ? String.format("! %-8s ", epic.needsOperatorKind() != null ? OutputSafety.stripControlSequences(epic.needsOperatorKind()) : "attention")
                                : "           ";
            var epicTitle = OutputSafety.stripControlSequences(epic.title());
            var plansCount = MessageCatalogue.format(locale, "overview.epic.plans", Map.of(
                                "running", epic.runningPlansCount(),
                                "staged", epic.stagedPlansCount()
            ));
            out.printf("     %sepic  %-30s   %s%n", epicMarker, epicTitle, plansCount);

            var childPlans = epic.plans();
            for (int i = 0; i < childPlans.size(); i++) {
                var child = childPlans.get(i);
                boolean last = i == childPlans.size() - 1;
                String branch = last ? "       \u2514\u2500  " : "       \u251c\u2500  ";
                renderPlanRow(out, child, branch, locale);
            }
        }

        // Plans without epic
        var withoutEpic = detail.plans().stream()
                            .filter(p -> p.epicId() == null)
                            .toList();
        if (!withoutEpic.isEmpty()) {
            out.printf("     %s%n", MessageCatalogue.format(locale, "overview.group.without_epic", Map.of()));
            for (var plan : withoutEpic) {
                renderPlanRow(out, plan, "     ", locale);
            }
        }
    }

    private void renderAlongsideLandings(PrintWriter out, List<AlongsideLanding> landings, String locale) {
        if (landings.isEmpty()) {
            return;
        }
        out.printf("     %s%n", MessageCatalogue.format(locale, "overview.group.alongside", Map.of()));
        for (var landing : landings) {
            String marker = "failed".equalsIgnoreCase(landing.state())
                                ? "! landing failed   "
                                : "                   ";
            String desc;
            if (landing.unit() != null) {
                desc = "landing " + OutputSafety.stripControlSequences(landing.unit());
            } else if (landing.count() != null) {
                desc = "removing " + landing.count() + " moved documents from git";
            } else {
                desc = "landing " + OutputSafety.stripControlSequences(landing.kind());
            }
            var state = OutputSafety.stripControlSequences(landing.state());
            out.printf("     %s%-35s   %s%n", marker, desc, state);
        }
    }

    private static String formatDuration(@Nullable Object raw) {
        if (raw == null) {
            return "";
        }
        try {
            Instant instant = raw instanceof Instant inst ? inst : Instant.parse(raw.toString());
            var duration = Duration.between(instant, Instant.now());
            long minutes = Math.max(0, duration.toMinutes());
            long hours = duration.toHours();
            if (hours > 0) {
                long remMins = minutes % 60;
                return remMins > 0 ? hours + " h " + remMins + " min" : hours + " h";
            }
            return minutes + " min";
        } catch (DateTimeParseException _) {
            return "";
        }
    }

    private static String formatRelativeTime(@Nullable Object raw) {
        if (raw == null) {
            return "recently";
        }
        try {
            Instant instant = raw instanceof Instant inst ? inst : Instant.parse(raw.toString());
            var diff = Duration.between(instant, Instant.now());
            long days = diff.toDays();
            if (days == 0) {
                long hours = diff.toHours();
                if (hours > 0) {
                    return hours + " h ago";
                }
                long minutes = diff.toMinutes();
                return minutes > 0 ? minutes + " min ago" : "just now";
            }
            if (days < 7) {
                return days + " d ago";
            }
            long weeks = days / 7;
            if (weeks < 5) {
                return weeks + (weeks == 1 ? " week ago" : " weeks ago");
            }
            long months = days / 30;
            return months + (months == 1 ? " month ago" : " months ago");
        } catch (DateTimeParseException _) {
            return raw.toString();
        }
    }

    private Map<String, Object> fetchProjectStatus() throws IOException {
        var path = RuntimeClient.STATUS_PATH;
        if (project != null && !".".equals(project)) {
            path += "?project=" + project;
        }
        var response = context.runtime(false).send(HttpRequest.get(path));
        if (response.status() != 200) {
            throw new OperatorRefusedException("status", response);
        }
        var status = JsonTree.asObject(JsonTree.parse(response.body()));
        if (status == null) {
            throw new OperatorRefusedException("status", response);
        }
        return status;
    }

    static Map<String, Object> fetch(RuntimeClient runtime) throws IOException {
        var response = runtime.send(HttpRequest.get(RuntimeClient.STATUS_PATH));
        if (response.status() != 200) {
            throw new OperatorRefusedException("status", response);
        }
        var status = JsonTree.asObject(JsonTree.parse(response.body()));
        if (status == null) {
            throw new OperatorRefusedException("status", response);
        }
        return status;
    }

    private int notRunning() {
        var out = spec.commandLine().getOut();
        out.println(json ? "{\"running\":false}" : "runtime   not running");
        out.flush();
        return 0;
    }

    static String render(Map<String, Object> status) {
        var text = new StringBuilder();
        line(text, "runtime", "running");
        line(text, "version", status.get("version"));
        line(text, "pid", status.get("pid"));
        line(text, "listener", status.get("listener"));
        line(text, "socket", status.get("socket_path"));
        line(text, "started", status.get("started_at"));
        line(text, "web", WebCommand.describe(JsonTree.object(status, "web")));
        return text.toString();
    }

    private static void line(StringBuilder text, String label, @Nullable Object value) {
        text.append(String.format("%-9s %s%n", label, value == null ? "-" : value));
    }
}
