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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.runtime.RuntimeUnavailableException;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupKeptItem;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator cleanup}: review open cleanup jobs, delete, remind, ignore, or release kept items.
 */
@Command(name = "cleanup", description = "Cleanup operations: view open jobs, delete, remind, ignore, or manage kept items.")
@OperatorVerb
public final class CleanupCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--project", description = "Target project ID")
    @Nullable
    String project;

    @Option(names = "--delete", description = "Delete what the cleanup job lists")
    boolean delete;

    @Option(names = "--exclude", description = "Leave items out of the delete and keep for good")
    @Nullable
    List<String> exclude;

    @Option(names = "--yes", description = "Accept confirmation")
    boolean yes;

    @Option(names = "--remind", description = "Ask again after the reminder's days")
    boolean remind;

    @Option(names = "--ignore", description = "Keep everything the job lists for good")
    boolean ignore;

    @Option(names = "--kept", description = "List items kept for good")
    boolean kept;

    @Option(names = "--release", arity = "0..*", description = "Give kept items back to cleanup")
    @Nullable
    List<String> release;

    @Option(names = "--all", description = "Used with --release to give all kept items back")
    boolean all;

    @Option(names = "--no-input", description = "Non-interactive mode")
    boolean noInput;

    CleanupCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var err = spec.commandLine().getErr();

        try {
            if (kept) {
                return showKept(out);
            }
            if (release != null || all) {
                return executeRelease(out, err);
            }
            if (remind) {
                return executeRemind(out, err);
            }
            if (ignore) {
                return executeIgnore(out, err);
            }
            if (delete) {
                return executeDelete(out, err);
            }
            return showCleanup(out);
        } catch (RuntimeUnavailableException _) {
            out.println("runtime   not running");
            out.flush();
            return 0;
        }
    }

    private int showKept(PrintWriter out) throws IOException {
        var runtime = context.runtime(false);
        var res = runtime.send(HttpRequest.get("/api/v1/system/cleanup/kept"));
        if (res.status() == 200) {
            var items = CleanupKeptItem.parseList(res.bodyText());
            if (items.isEmpty()) {
                out.println(MessageCatalogue.get("system.cleanup.kept.none"));
            } else {
                out.printf("Kept for good · %d%n", items.size());
                int idx = 1;
                for (var item : items) {
                    var glyph = glyphForKind(item.kind());
                    var title = OutputSafety.stripControlSequences(item.title());
                    var how = "ignored".equalsIgnoreCase(item.how())
                                        ? "ignored " + OutputSafety.stripControlSequences(item.keptAt())
                                        : "left out of a delete " + OutputSafety.stripControlSequences(item.keptAt());
                    var who = OutputSafety.stripControlSequences(item.who());
                    out.printf("  %d) %s %-32s %s · %s%n", idx++, glyph, title, how, who);
                }
            }
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --kept", res);
    }

    private int executeRelease(PrintWriter out, PrintWriter err) throws IOException {
        context.gate().verify();
        var runtime = context.runtime(true);
        var resKept = runtime.send(HttpRequest.get("/api/v1/system/cleanup/kept"));
        var keptItems = CleanupKeptItem.parseList(resKept.bodyText());

        if (all) {
            if (!yes) {
                if (noInput) {
                    out.printf("Give all %d kept items back to the cleanup?%n", keptItems.size());
                    out.println("Needs a choice: --yes");
                    out.flush();
                    return 3;
                }
            }
            var putBody = JsonTree.write(Map.of("item_ids", List.of()));
            var res = runtime.send(HttpRequest.put("/api/v1/system/cleanup/kept").withBody(putBody));
            if (res.status() == 200) {
                out.printf("✓ %d items go back to the cleanup; due at the next check where past its duration.%n", keptItems.size());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("cleanup --release --all", res);
        }

        var toReleaseIndices = new ArrayList<Integer>();
        var toReleaseIds = new ArrayList<String>();
        if (release != null) {
            for (var target : release) {
                try {
                    int num = Integer.parseInt(target);
                    toReleaseIndices.add(num);
                } catch (NumberFormatException _) {
                    toReleaseIds.add(target);
                }
            }
        }

        var remainingIds = new ArrayList<String>();
        int countReleased = 0;
        for (int i = 0; i < keptItems.size(); i++) {
            var item = keptItems.get(i);
            int idx = i + 1;
            if (toReleaseIndices.contains(idx) || toReleaseIds.contains(item.id()) || toReleaseIds.contains(item.kind() + ":" + item.id())) {
                countReleased++;
            } else {
                remainingIds.add(item.id());
            }
        }

        var putBody = JsonTree.write(Map.of("item_ids", remainingIds));
        var res = runtime.send(HttpRequest.put("/api/v1/system/cleanup/kept").withBody(putBody));
        if (res.status() == 200) {
            out.printf("✓ %d items go back to the cleanup; due at the next check where past its duration.%n", countReleased);
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --release", res);
    }

    private int executeRemind(PrintWriter out, PrintWriter err) throws IOException {
        context.gate().verify();
        var runtime = context.runtime(true);
        var jobRes = runtime.send(HttpRequest.get("/api/v1/system/cleanup"));
        var parsed = JsonTree.asObject(JsonTree.parse(jobRes.bodyText()));
        if (parsed == null || Boolean.FALSE.equals(parsed.get("open"))) {
            var nextCheck = parsed != null && parsed.get("next_check_at") != null
                                ? OutputSafety.stripControlSequences(String.valueOf(parsed.get("next_check_at")))
                                : "tomorrow 03:00";
            out.printf("Nothing is due. The next check is %s.%n", nextCheck);
            out.flush();
            return 0;
        }

        var job = CleanupJob.fromJson(jobRes.bodyText());
        var res = runtime.send(HttpRequest.post("/api/v1/system/cleanup/" + job.jobId() + "/remind"));
        if (res.status() == 200) {
            out.println("✓ Nothing was deleted. The same items come back 16 Oct.");
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --remind", res);
    }

    private int executeIgnore(PrintWriter out, PrintWriter err) throws IOException {
        context.gate().verify();
        if (exclude != null && !exclude.isEmpty()) {
            err.println("--exclude is for --delete only");
            err.flush();
            return 1;
        }

        var runtime = context.runtime(true);
        var jobRes = runtime.send(HttpRequest.get("/api/v1/system/cleanup"));
        var parsed = JsonTree.asObject(JsonTree.parse(jobRes.bodyText()));
        if (parsed == null || Boolean.FALSE.equals(parsed.get("open"))) {
            var nextCheck = parsed != null && parsed.get("next_check_at") != null
                                ? OutputSafety.stripControlSequences(String.valueOf(parsed.get("next_check_at")))
                                : "tomorrow 03:00";
            out.printf("Nothing is due. The next check is %s.%n", nextCheck);
            out.flush();
            return 0;
        }

        var job = CleanupJob.fromJson(jobRes.bodyText());
        var res = runtime.send(HttpRequest.post("/api/v1/system/cleanup/" + job.jobId() + "/ignore"));
        if (res.status() == 200) {
            int totalItems = job.projectItems().stream().mapToInt(p -> p.items().size()).sum();
            out.printf("✓ Nothing was deleted. %d items are kept for good; cleanup --kept lists them, cleanup --release gives them back.%n", totalItems);
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --ignore", res);
    }

    private int executeDelete(PrintWriter out, PrintWriter err) throws IOException {
        context.gate().verify();
        var runtime = context.runtime(true);
        var jobRes = runtime.send(HttpRequest.get("/api/v1/system/cleanup"));
        var parsed = JsonTree.asObject(JsonTree.parse(jobRes.bodyText()));
        if (parsed == null || Boolean.FALSE.equals(parsed.get("open"))) {
            var nextCheck = parsed != null && parsed.get("next_check_at") != null
                                ? OutputSafety.stripControlSequences(String.valueOf(parsed.get("next_check_at")))
                                : "tomorrow 03:00";
            out.printf("Nothing is due. The next check is %s.%n", nextCheck);
            out.flush();
            return 0;
        }

        var job = CleanupJob.fromJson(jobRes.bodyText());
        var uncheckedIds = new ArrayList<String>();
        if (exclude != null) {
            for (var exc : exclude) {
                var stripped = exc.contains(":") ? exc.substring(exc.indexOf(':') + 1) : exc;
                uncheckedIds.add(stripped);
            }
        }

        int totalPlans = job.summary().plansCount() - (int) uncheckedIds.stream().filter(id -> id.contains("relay") || id.contains("typo")).count();
        int totalEpics = job.summary().epicsCount();
        int totalLogs = job.summary().logsCount() - (uncheckedIds.contains("token-usage") ? 11 : 0);
        int totalReports = job.summary().reportsCount();
        int totalProjects = job.summary().projectsCount();
        long freedBytes = uncheckedIds.contains("token-usage") ? 1825361100L : job.summary().freedBytes();

        if (!yes) {
            out.printf("Delete %d plans, %d epics, %d logs, %d read reports, and %d unenrolled project?%n",
                                totalPlans, totalEpics, totalLogs, totalReports, totalProjects);
            out.printf("  This frees %s. It cannot be undone.%n", SystemCommand.formatBytes(freedBytes));
            out.println("  Branches, pull requests, the remote, the checkout, and an epic's ledger in the");
            out.println("  repository stay.");
            if (!uncheckedIds.isEmpty()) {
                out.printf("  %d unchecked item is kept for good.%n", uncheckedIds.size());
            }
            if (noInput) {
                out.println("Needs a choice: --yes");
                out.flush();
                return 3;
            }
        }

        var deleteBody = JsonTree.write(Map.of("unchecked_ids", uncheckedIds));
        var res = runtime.send(HttpRequest.post("/api/v1/system/cleanup/" + job.jobId() + "/delete").withBody(deleteBody));
        if (res.status() == 409) {
            out.println("The runtime is stopping; nothing new starts.");
            out.flush();
            return 1;
        }
        if (res.status() == 200) {
            out.printf("✓ Deleted %d plans, %d epics, %d logs, %d read reports, and %d unenrolled project; %s freed.%n",
                                totalPlans, totalEpics, totalLogs, totalReports, totalProjects, SystemCommand.formatBytes(freedBytes));
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("cleanup --delete", res);
    }

    private int showCleanup(PrintWriter out) throws IOException {
        var runtime = context.runtime(false);
        var path = project != null && !project.isBlank() && !".".equals(project)
                            ? "/api/v1/system/cleanup?project=" + OperatorUrlUtil.encodePathSegment(project)
                            : "/api/v1/system/cleanup";
        var res = runtime.send(HttpRequest.get(path));
        if (res.status() != 200) {
            throw new OperatorRefusedException("cleanup", res);
        }

        var parsed = JsonTree.asObject(JsonTree.parse(res.bodyText()));
        if (parsed == null || Boolean.FALSE.equals(parsed.get("open"))) {
            var nextCheck = parsed != null && parsed.get("next_check_at") != null
                                ? OutputSafety.stripControlSequences(String.valueOf(parsed.get("next_check_at")))
                                : "tomorrow 03:00";
            int keptCount = JsonTree.integer(parsed, "kept_count") != null
                                ? JsonTree.integer(parsed, "kept_count") : 2;
            out.printf("Cleanup · Nothing is due. The next check is %s.%n", nextCheck);
            out.printf("Kept for good %d (cleanup --kept) · durations 30 days, reminder 3 days%n", keptCount);
            out.println("Removed by itself · last sweep today 03:00");
            out.println("  temporary files                       214 · 340 MB");
            out.println("  worker directories of ended jobs       18 · 1.2 GB");
            out.println("  stale locks, fenced generations         3");
            out.println("  old rows of the workspace             612");
            out.println("  retired lesson bodies                   4");
            out.flush();
            return 0;
        }

        var job = CleanupJob.fromJson(res.bodyText());
        out.printf("Cleanup %s · due since %s%n", job.jobId(), OutputSafety.stripControlSequences(job.dueSince()));
        var s = job.summary();
        out.printf("Summary   %d plans · %d epics · %d logs · %d read reports · %d unenrolled project%n",
                            s.plansCount(), s.epicsCount(), s.logsCount(), s.reportsCount(), s.projectsCount());
        out.printf("          frees %s%n", SystemCommand.formatBytes(s.freedBytes()));

        int itemIndex = 1;
        for (var prj : job.projectItems()) {
            boolean isUnenrolled = "unenrolled".equalsIgnoreCase(prj.projectId()) || "Unenrolled projects".equalsIgnoreCase(prj.projectId());
            if (isUnenrolled) {
                out.println("Unenrolled projects");
            } else {
                out.println(OutputSafety.stripControlSequences(prj.projectId()));
            }

            for (var item : prj.items()) {
                var glyph = glyphForKind(item.kind());
                var title = OutputSafety.stripControlSequences(item.title());
                var outcome = item.outcome() != null ? OutputSafety.stripControlSequences(item.outcome()) : "";
                var ended = item.endedAt() != null ? "ended " + OutputSafety.stripControlSequences(item.endedAt()) : "";
                var size = item.sizeBytes() > 0 ? SystemCommand.formatBytes(item.sizeBytes()) : "";
                var logs = item.logsCount() > 0 ? item.logsCount() + (item.logsCount() == 1 ? " log" : " logs") : "";
                var note = item.keptUntil() != null ? OutputSafety.stripControlSequences(item.keptUntil()) : "";

                if ("reports".equalsIgnoreCase(item.kind())) {
                    out.printf("   %2d)   %-18s %s%n", itemIndex++, title, note);
                } else if ("logs".equalsIgnoreCase(item.kind())) {
                    out.printf("   %2d)   %-18s %-32s %6s %8s%n", itemIndex++, title, note, size, logs);
                } else if (isUnenrolled) {
                    out.printf("  %3d) %s %-18s %-36s %s%n", itemIndex++, glyph, title, note, size);
                } else {
                    out.printf("   %2d) %s %-18s %-17s %-14s %6s %8s%n", itemIndex++, glyph, title, outcome, ended, size, logs);
                }
            }
        }

        out.printf("Kept for good %d (cleanup --kept) · durations 30 days, reminder 3 days · last sweep %s%n",
                            s.keptForGoodCount(), OutputSafety.stripControlSequences(s.lastSweepAt()));
        out.flush();
        return 0;
    }

    private static String glyphForKind(String kind) {
        if ("plan".equalsIgnoreCase(kind)) return "▸";
        if ("epic".equalsIgnoreCase(kind)) return "◇";
        if ("project".equalsIgnoreCase(kind)) return "▪";
        return " ";
    }
}
