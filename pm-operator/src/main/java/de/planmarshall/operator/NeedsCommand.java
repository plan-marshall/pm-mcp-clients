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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.display.Display;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.NeedsOperatorPage;
import de.planmarshall.api.runtime.RuntimeClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator needs}: prints the list of what waits across projects in the order the runtime
 * serves it (UC-PLN-7, CLI-UX-S-04, CLI-UX-P-01).
 */
@Command(name = "needs", description = "Shows what needs operator attention across projects.")
@OperatorVerb
public final class NeedsCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--json", description = "Prints the needs list as JSON.")
    boolean json;

    NeedsCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var client = context.runtime(true);
        var response = client.send(HttpRequest.get(MachineApiPaths.NEEDS_OPERATOR));
        if (response.status() != 200) {
            throw new OperatorRefusedException("needs", response);
        }

        if (json) {
            out.println(new String(response.body(), StandardCharsets.UTF_8));
            out.flush();
            return 0;
        }

        var page = NeedsOperatorPage.fromJson(response.bodyText());
        var locale = context.clientEnvironment().locale();

        if (page.items().isEmpty()) {
            var emptyKey = MessageCatalogue.hasKey(locale, "operator.needs.empty")
                    ? "operator.needs.empty"
                    : "overview.needs.empty";
            out.println(MessageCatalogue.format(locale, emptyKey, Map.of()));
            out.flush();
            return 0;
        }

        var decisionIds = fetchDecisionIds(client);
        if (MessageCatalogue.hasKey(locale, "operator.needs.title")) {
            out.println(MessageCatalogue.format(locale, "operator.needs.title", Map.of("total", page.total())));
        } else {
            out.printf("Needs you · %d%n", page.total());
        }

        for (var item : page.items()) {
            printItem(out, item, locale, decisionIds);
        }
        out.flush();
        return 0;
    }

    private void printItem(PrintWriter out, NeedsOperatorItem item, String locale, Map<String, String> decisionIds) {
        var kindLabel = formatKind(item.kind(), locale);
        var subject = formatSubject(item);
        var blocks = formatBlocks(item.blocks(), locale);
        var since = formatSince(item.since(), locale);
        var cmd = formatCommand(item, decisionIds);

        out.printf("  %-16s %-16s %-19s %-13s %s%n",
                "! " + kindLabel,
                subject,
                blocks,
                since,
                cmd);
    }

    private static String formatKind(NeedsOperatorKind kind, String locale) {
        var key = switch (kind) {
            case DECISION -> "overview.needs.decision";
            case WAIVER -> "overview.needs.waiver";
            case WAITS_FOR_YOU -> "overview.needs.waits_for_you";
            case INTEGRITY_PAUSE -> "overview.needs.integrity_pause";
            case STALLED -> "overview.needs.stalled";
            case CONDITION -> "overview.needs.condition";
            case HELD -> "overview.needs.held";
            case LANDING_FAILED -> "overview.needs.landing_failed";
            case FINDINGS -> "overview.needs.findings";
            case SETUP -> "overview.needs.setup";
            case CREDENTIAL -> "overview.needs.credential";
            case CLEANUP_DUE -> "overview.needs.cleanup_due";
        };
        if (MessageCatalogue.hasKey(locale, key)) {
            return MessageCatalogue.format(locale, key, Map.of());
        }
        return kind.value().replace('_', ' ');
    }

    private static String formatSubject(NeedsOperatorItem item) {
        if (item.plan() != null && !item.plan().id().isBlank()) {
            return OutputSafety.stripControlSequences(item.plan().id());
        }
        if (item.epic() != null && !item.epic().id().isBlank()) {
            return OutputSafety.stripControlSequences(item.epic().id());
        }
        if (item.project() != null && !item.project().id().isBlank()) {
            return OutputSafety.stripControlSequences(item.project().id());
        }
        if (item.kind() == NeedsOperatorKind.CLEANUP_DUE) {
            return "cleanup";
        }
        if (item.kind() == NeedsOperatorKind.CREDENTIAL) {
            return "credential";
        }
        return "machine";
    }

    private static String formatBlocks(@Nullable String blocks, String locale) {
        if (blocks == null || blocks.isBlank()) {
            if (MessageCatalogue.hasKey(locale, "overview.needs.blocks_nothing")) {
                return MessageCatalogue.format(locale, "overview.needs.blocks_nothing", Map.of());
            }
            return "blocks no work";
        }
        return "blocks the " + blocks;
    }

    private String formatSince(@Nullable Instant since, String locale) {
        if (since == null) {
            return "";
        }
        var formatted = Display.time(locale, since, Display.TimeStyle.SHORT, context.clock());
        if (formatted.isBlank()) {
            formatted = Display.time(locale, since, context.clock());
        }
        return "since " + formatted;
    }

    private static String formatCommand(NeedsOperatorItem item, Map<String, String> decisionIds) {
        var kind = item.kind();
        var planId = item.plan() != null ? item.plan().id() : null;
        var epicId = item.epic() != null ? item.epic().id() : null;
        var projId = item.project() != null ? item.project().id() : null;

        return switch (kind) {
            case DECISION, WAIVER -> {
                if (planId != null && decisionIds.containsKey(planId)) {
                    yield "answer " + decisionIds.get(planId);
                }
                if (projId != null && decisionIds.containsKey(projId)) {
                    yield "answer " + decisionIds.get(projId);
                }
                yield "answer";
            }
            case FINDINGS -> epicId != null ? "epic findings " + epicId : "reports";
            case SETUP -> projId != null ? "project setup " + projId : "project setup";
            case CREDENTIAL -> "credentials";
            case CLEANUP_DUE -> "cleanup";
            case LANDING_FAILED -> planId != null ? "plan hold " + planId : (epicId != null ? "epic " + epicId : "plans");
            case HELD, CONDITION, STALLED, INTEGRITY_PAUSE, WAITS_FOR_YOU -> {
                if (planId != null) {
                    yield "plan " + planId;
                }
                if (epicId != null) {
                    yield "epic " + epicId;
                }
                yield "plans";
            }
        };
    }

    private static Map<String, String> fetchDecisionIds(RuntimeClient client) {
        try {
            var resp = client.send(HttpRequest.get("/api/v1/decisions"));
            if (resp.status() != 200) {
                resp = client.send(HttpRequest.get("/api/v1/questions"));
            }
            if (resp.status() == 200) {
                var parsed = JsonTree.parse(resp.body());
                if (parsed instanceof List<?> list) {
                    var map = new HashMap<String, String>();
                    for (var el : list) {
                        if (el instanceof Map<?, ?> m) {
                            var id = m.get("id");
                            var plan = m.get("plan");
                            var proj = m.get("project");
                            if (id != null && plan != null) {
                                map.put(plan.toString(), id.toString());
                            } else if (id != null && proj != null) {
                                map.put(proj.toString(), id.toString());
                            }
                        }
                    }
                    return map;
                }
            }
        } catch (IOException _) {
            // Fall back gracefully to empty map if decisions route is unavailable
        }
        return Map.of();
    }
}
