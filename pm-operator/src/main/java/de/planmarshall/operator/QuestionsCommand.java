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
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.display.Display;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator questions}: lists open decisions and questions across plans and projects (UC-PLN-2).
 */
@Command(name = "questions", description = "Lists open decisions and questions.")
@OperatorVerb
public final class QuestionsCommand implements Callable<Integer> {

    private static final String COMMON_UNKNOWN = "common.unknown";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--project", description = "Filter by project ID")
    @Nullable
    String project;

    @Option(names = "--plan", description = "Filter by plan ID")
    @Nullable
    String plan;

    QuestionsCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var path = OperatorUrlUtil.appendScope("/api/v1/decisions", project, plan);
        var response = context.runtime(true).send(HttpRequest.get(path));
        if (response.status() == 404) {
            response = context.runtime(true).send(
                                HttpRequest.get(OperatorUrlUtil.appendScope("/api/v1/questions", project, plan)));
        }
        if (response.status() == 200) {
            printQuestions(out, response.body(), context.clientEnvironment().locale(), context.clock());
            return 0;
        }
        throw new OperatorRefusedException("questions", response);
    }

    private static void printQuestions(PrintWriter out, byte[] body, String locale, Clock clock) throws IOException {
        var parsed = JsonTree.parse(body);
        if (parsed instanceof List<?> list && !list.isEmpty()) {
            boolean allWaivers = list.stream().allMatch(item -> item instanceof Map<?, ?> m
                                && isWaiver(m, text(m.get("id"))));

            if (allWaivers) {
                out.println(MessageCatalogue.format(locale, "operator.questions.waivers_pending",
                                    Map.of("count", list.size())));
            } else {
                out.println(MessageCatalogue.format(locale, "operator.questions.open_pending",
                                    Map.of("count", list.size())));
            }

            for (var item : list) {
                printQuestionItem(out, item, locale, clock);
            }
        } else {
            out.println(MessageCatalogue.format(locale, "operator.questions.none", Map.of()));
        }
        out.flush();
    }

    private static void printQuestionItem(PrintWriter out, Object item, String locale, Clock clock) {
        if (!(item instanceof Map<?, ?> q)) {
            out.println("  " + OutputSafety.stripControlSequences(String.valueOf(item)));
            return;
        }
        var id = text(q.get("id"));
        var scope = scopeOf(q);
        var title = titleOf(q);
        var waitingStr = waitingSince(q, locale, clock);
        if (isWaiver(q, id)) {
            printWaiver(out, q, locale, id, scope, title, waitingStr);
        } else {
            printDecision(out, q, locale, id, scope, title, waitingStr);
        }
    }

    private static void printWaiver(PrintWriter out, Map<?, ?> q, String locale, String id, String scope,
                        String title, String waitingStr) {
        var gate = q.get("waiver_gate");
        if (gate == null) {
            gate = q.get("gate");
        }
        var gateFallback = MessageCatalogue.format(locale, "operator.questions.gate_fallback", Map.of());
        var gateStr = gate != null
                            ? Display.gate(locale, OutputSafety.stripControlSequences(gate.toString()))
                            : gateFallback;
        var waiverPrefix = MessageCatalogue.format(locale, "operator.questions.waiver_prefix", Map.of());
        out.printf("  %-4s  %-15s %s %s%n", id, scope, waiverPrefix, gateStr);
        if (!title.isEmpty()) {
            out.printf("        \"%s\"%s%n", title, waitingStr);
        }
    }

    private static void printDecision(PrintWriter out, Map<?, ?> q, String locale, String id, String scope,
                        String title, String waitingStr) {
        var phase = q.get("asking_phase");
        if (phase == null) {
            phase = q.get("phase");
        }
        var phaseStr = phase != null ? OutputSafety.stripControlSequences(phase.toString()) : "";

        var blocks = q.get("blocks");
        var blocksKey = blocks != null
                            ? "decision.blocks." + blocks.toString().toLowerCase(Locale.ROOT)
                            : "decision.blocks.plan";
        var blocksWord = MessageCatalogue.format(locale,
                            MessageCatalogue.hasKey(locale, blocksKey) ? blocksKey : COMMON_UNKNOWN,
                            Map.of());

        var rev = q.get("reversibility");
        var revKey = rev != null
                            ? "decision.reversibility." + rev.toString().toLowerCase(Locale.ROOT)
                            : "decision.reversibility.reversible";
        var revWord = MessageCatalogue.format(locale,
                            MessageCatalogue.hasKey(locale, revKey) ? revKey : COMMON_UNKNOWN,
                            Map.of());

        var metaParts = List.of(phaseStr, blocksWord, revWord).stream()
                            .filter(s -> s != null && !s.isBlank()).toList();
        out.printf("  %-6s %-15s %s%n", id, scope, String.join(" \u00b7 ", metaParts));
        out.printf("         \"%s\"%s%n", title, waitingStr);
    }

    private static boolean isWaiver(Map<?, ?> q, String id) {
        return Boolean.TRUE.equals(q.get("is_waiver"))
                            || id.startsWith("WR-")
                            || "waiver".equalsIgnoreCase(String.valueOf(q.get("kind")));
    }

    private static String scopeOf(Map<?, ?> q) {
        var scopeObj = q.get("subject");
        if (scopeObj == null) {
            var planVal = q.get("plan");
            scopeObj = planVal != null ? "plan " + planVal : q.get("scope");
        }
        return text(scopeObj);
    }

    private static String titleOf(Map<?, ?> q) {
        var titleObj = q.get("title");
        if (titleObj == null) {
            titleObj = q.get("question");
        }
        return text(titleObj);
    }

    private static String waitingSince(Map<?, ?> q, String locale, Clock clock) {
        var deliveredAt = q.get("delivered_at");
        if (deliveredAt == null) {
            return "";
        }
        var ws = Display.eventTime(OutputSafety.stripControlSequences(deliveredAt.toString()), clock);
        return ws.isEmpty() ? "" : "   " + MessageCatalogue.format(locale, "operator.questions.waiting_since",
                            Map.of("time", ws));
    }

    private static String text(@Nullable Object raw) {
        return raw != null ? OutputSafety.stripControlSequences(raw.toString()) : "";
    }
}
