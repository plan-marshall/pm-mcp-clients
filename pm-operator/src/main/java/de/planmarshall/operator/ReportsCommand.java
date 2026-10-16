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
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.report.ReadReportsResponse;
import de.planmarshall.api.report.ReportItem;
import de.planmarshall.api.report.ReportsPage;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator reports}: list unread reports, mark reports read, or open report details
 * according to {@code 24-reports.adoc} and {@code shared/reports.adoc}.
 */
@Command(name = "reports", description = "Reports operations.",
        subcommands = {
                ReportsCommand.ReadCommand.class,
                ReportsCommand.OpenCommand.class
        })
@OperatorVerb
public final class ReportsCommand implements Callable<Integer> {

    private static final String REPORTS_API = "/api/v1/reports/";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--project", description = "Filter by project ID")
    @Nullable
    String project;

    @Option(names = "--older", description = "Show older reports page")
    boolean older;

    ReportsCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var path = OperatorUrlUtil.appendScope("/api/v1/reports", project, null);
        if (older) {
            path += (path.contains("?") ? "&" : "?") + "older=true";
        }

        var response = context.runtime(true).send(HttpRequest.get(path));
        if (response.status() == 200) {
            printReports(out, response.bodyText());
            return 0;
        }
        throw new OperatorRefusedException("reports", response);
    }

    private static void printReports(PrintWriter out, String bodyText) throws IOException {
        var page = ReportsPage.fromJson(bodyText);
        if (page.items().isEmpty()) {
            out.println(MessageCatalogue.get("report.list.empty"));
            out.flush();
            return;
        }

        out.printf("Reports · %d unread%n", page.unreadCount());

        var projectGroups = groupReportsByProject(page.items());
        for (var entry : projectGroups.entrySet()) {
            printProjectGroup(out, entry.getKey(), entry.getValue());
        }

        var firstId = page.items().get(0).id();
        out.println(MessageCatalogue.format("operator.reports.read_hint", Map.of("id", firstId)));

        if (page.hasOlder() && page.olderCount() > 0) {
            out.println(MessageCatalogue.format("operator.reports.older", Map.of("count", page.olderCount())));
        }
        out.flush();
    }

    private static Map<String, List<ReportItem>> groupReportsByProject(List<ReportItem> items) {
        var projectGroups = new LinkedHashMap<String, List<ReportItem>>();
        for (var item : items) {
            var proj = item.from().isSystem() ? "This machine" : item.from().project();
            if (proj == null || proj.isBlank()) {
                proj = "This machine";
            }
            projectGroups.computeIfAbsent(proj, _ -> new ArrayList<>()).add(item);
        }
        return projectGroups;
    }

    private static void printProjectGroup(PrintWriter out, String projectName, List<ReportItem> projectItems) {
        out.println(ConsoleNotice.stripControlSequences(projectName));

        var epicGroups = new LinkedHashMap<String, List<ReportItem>>();
        var planItems = new ArrayList<ReportItem>();
        var projSelfItems = new ArrayList<ReportItem>();

        for (var item : projectItems) {
            if (item.from().epic() != null && !item.from().epic().isBlank()) {
                epicGroups.computeIfAbsent(item.from().epic(), _ -> new ArrayList<>()).add(item);
            } else if (item.from().plan() != null && !item.from().plan().isBlank()) {
                planItems.add(item);
            } else {
                projSelfItems.add(item);
            }
        }

        for (var epicEntry : epicGroups.entrySet()) {
            out.println("  " + ConsoleNotice.stripControlSequences(epicEntry.getKey()));
            for (var item : epicEntry.getValue()) {
                printReportItem(out, item, "    ");
            }
        }
        for (var item : planItems) {
            printReportItem(out, item, "  ");
        }
        for (var item : projSelfItems) {
            printReportItem(out, item, "  ");
        }
    }

    private static String resolveTargetName(ReportItem item) {
        var targetName = item.from().plan();
        if (targetName == null || targetName.isBlank()) {
            targetName = item.from().epic();
        }
        if (targetName == null || targetName.isBlank()) {
            targetName = "project";
        }
        return ConsoleNotice.stripControlSequences(targetName);
    }

    private static void printReportItem(PrintWriter out, ReportItem item, String indent) {
        var id = ConsoleNotice.stripControlSequences(item.id());
        var targetName = resolveTargetName(item);
        var subject = ConsoleNotice.stripControlSequences(item.subject());
        var timeStr = ConsoleNotice.stripControlSequences(
                item.isSentLater() ? item.created() + " · sent " + item.sent() : item.created());

        out.printf("%s%-10s %-19s %-37s %s%n", indent, id, targetName, subject, timeStr);

        var body = item.body() != null && !item.body().isBlank() ? ConsoleNotice.stripControlSequences(item.body()) : "";
        var detailHint = item.detail() != null && !item.detail().isBlank()
                ? MessageCatalogue.format("operator.reports.open_hint", Map.of("id", id)) : "";

        printItemBodyAndDetail(out, indent, body, detailHint);
    }

    private static void printItemBodyAndDetail(PrintWriter out, String indent, String body, String detailHint) {
        if (body.isEmpty() && detailHint.isEmpty()) {
            return;
        }
        var secondIndent = indent + "          ";
        if (!body.isEmpty() && !detailHint.isEmpty()) {
            out.printf("%s%-48s %s%n", secondIndent, body, detailHint);
        } else if (!body.isEmpty()) {
            out.printf("%s%s%n", secondIndent, body);
        } else {
            out.printf("%s%48s%s%n", secondIndent, "", detailHint);
        }
    }

    @Command(name = "read", description = "Marks reports read.")
    @OperatorVerb
    static final class ReadCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(arity = "0..*", description = "Report IDs to mark read")
        List<String> reportIds = new ArrayList<>();

        @Option(names = "--all", description = "Mark all unread reports read")
        boolean all;

        ReadCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (all) {
                var response = context.runtime(true).send(
                        HttpRequest.json("POST", "/api/v1/reports/read", "{\"all\":true}"));
                if (response.status() == 200 || response.status() == 204) {
                    var parsed = ReadReportsResponse.fromJson(response.bodyText());
                    out.printf("✓ %d reports marked read.%n", parsed.markedCount());
                    out.flush();
                    return 0;
                }
                throw new OperatorRefusedException("reports read --all", response);
            }

            if (reportIds.isEmpty()) {
                out.println("Specify report IDs or --all.");
                out.flush();
                return 1;
            }

            int marked = 0;
            for (var id : reportIds) {
                var safeId = OperatorUrlUtil.encodePathSegment(id);
                var response = context.runtime(true).send(
                        HttpRequest.json("POST", REPORTS_API + safeId + "/read", "{}"));
                if (response.status() == 200 || response.status() == 204) {
                    marked++;
                } else if (response.status() == 404) {
                    out.println(MessageCatalogue.format("operator.reports.not_found", Map.of("id", id)));
                }
            }
            out.printf("✓ %d reports marked read.%n", marked);
            out.flush();
            return 0;
        }
    }

    @Command(name = "open", description = "Opens a report's detail and marks it read.")
    @OperatorVerb
    static final class OpenCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", description = "Report ID")
        String reportId = "";

        OpenCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var safeId = OperatorUrlUtil.encodePathSegment(reportId);
            var response = context.runtime(true).send(HttpRequest.get(REPORTS_API + safeId));
            if (response.status() == 200) {
                // Also mark as read in the same act
                context.runtime(true).send(HttpRequest.json("POST", REPORTS_API + safeId + "/read", "{}"));
                var bodyText = response.bodyText().trim();
                if (bodyText.startsWith("{")) {
                    try {
                        var item = ReportItem.fromJson(bodyText);
                        if (item.detail() != null && !item.detail().isBlank()) {
                            var args = PmConsole.parseArgs(item.detail());
                            return PmOperator.execute(context, out, spec.commandLine().getErr(), args.toArray(new String[0]));
                        }
                    } catch (IOException _) {
                        // ignore and fall through to text printing
                    }
                }
                out.println(bodyText);
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                out.println(MessageCatalogue.format("operator.reports.not_found", Map.of("id", reportId)));
                out.flush();
                return 1;
            }
            throw new OperatorRefusedException("reports open " + reportId, response);
        }
    }
}
