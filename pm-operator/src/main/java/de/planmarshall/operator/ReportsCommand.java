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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator reports}: list unread reports, mark reports read, or open report details.
 */
@Command(name = "reports", description = "Reports operations.",
        subcommands = {
                ReportsCommand.ReadCommand.class,
                ReportsCommand.OpenCommand.class
        })
@OperatorVerb
public final class ReportsCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec;

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
        var path = new StringBuilder("/api/v1/reports");
        var hasQuery = false;
        if (project != null && !".".equals(project)) {
            path.append("?project=").append(project);
            hasQuery = true;
        }
        if (older) {
            path.append(hasQuery ? "&older=true" : "?older=true");
        }

        var response = context.runtime(true).send(HttpRequest.get(path.toString()));
        if (response.status() == 200) {
            var parsed = JsonTree.parse(response.body());
            if (parsed instanceof List<?> list && !list.isEmpty()) {
                out.printf("Reports · %d unread%n", list.size());
                for (var item : list) {
                    if (item instanceof Map<?, ?> rep) {
                        var subject = rep.get("subject") != null ? rep.get("subject") : (rep.get("plan") != null ? rep.get("plan") : "");
                        var summary = rep.get("summary") != null ? rep.get("summary") : "";
                        out.printf("  %-10s %-20s %s%n", rep.get("id"), subject, summary);
                    } else {
                        out.println("  " + item);
                    }
                }
            } else {
                out.println("Nothing new. Reports you have read are not shown any more.");
            }
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("reports", response);
    }

    @Command(name = "read", description = "Marks reports read.")
    @OperatorVerb
    static final class ReadCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec;
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
                    var parsed = JsonTree.parse(response.body());
                    int count = 0;
                    if (parsed instanceof Map<?, ?> map && map.get("count") instanceof JsonNumber jn) {
                        count = (int) jn.longValue();
                    }
                    out.printf("✓ %d reports marked read.%n", count);
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
                var response = context.runtime(true).send(
                        HttpRequest.json("POST", "/api/v1/reports/" + id + "/read", "{}"));
                if (response.status() == 200 || response.status() == 204) {
                    marked++;
                } else if (response.status() == 404) {
                    out.printf("Report %s not found.%n", id);
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
        CommandSpec spec;
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
            var response = context.runtime(true).send(HttpRequest.get("/api/v1/reports/" + reportId));
            if (response.status() == 200) {
                // Also mark as read
                context.runtime(true).send(HttpRequest.json("POST", "/api/v1/reports/" + reportId + "/read", "{}"));
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            if (response.status() == 404) {
                out.printf("Report %s not found.%n", reportId);
                out.flush();
                return 1;
            }
            throw new OperatorRefusedException("reports open " + reportId, response);
        }
    }
}
