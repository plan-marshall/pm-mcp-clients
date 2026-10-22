/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.planview;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.report.ChangeMapRecord;
import de.planmarshall.api.report.OwnCallRecord;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.operator.OutputSafety;
import de.planmarshall.operator.tui.TuiRow;

/**
 * An ended plan's completion report as a level of the full-screen view
 * ({@code doc/design/terminal-client/16-plan-view.adoc#ended}).
 *
 * <p>It is read-only: the report's outcome, reason, deliverables, decisions and tokens are shown
 * as they were recorded, and every runtime text is sanitized first.
 */
public record PlanCompletionLevel(PlanCompletionReport report) {

    private static final String INDENT = "  ";
    private static final String LABEL_WIDTH = "%-16s";
    private static final String OWN_CALL_KIND_WIDTH = "%-19s";
    private static final String KEY_SEPARATOR = ": ";

    /**
     * @param report the plan's completion report
     */
    public PlanCompletionLevel {
        Objects.requireNonNull(report, "report");
    }

    /**
     * @return the plan the report belongs to
     */
    public String planId() {
        return report.planId();
    }

    /**
     * @param locale the interface locale
     * @return the header path of the completion report
     */
    public String breadcrumb(String locale) {
        return MessageCatalogue.format(locale, "operator.plan.completion.title",
                            Map.of("plan", PlanViewFormatting.clean(report.planId())));
    }

    /**
     * @param locale the interface locale
     * @return the report's lines
     */
    public List<TuiRow> rows(String locale) {
        var rows = new ArrayList<TuiRow>();
        rows.add(TuiRow.leaf("completion:summary", summary(locale)));
        rows.add(entry("completion:outcome", locale, "report.completion.outcome",
                            description(report.terminalReason() != null ? report.terminalReason() : report.outcome())));
        rows.add(entry("completion:reason", locale, "report.completion.reason",
                            report.reason() != null && !report.reason().isBlank() ? report.reason() : "-"));
        var ran = report.ran();
        if (ran != null && !ran.isBlank()) {
            rows.add(entry("completion:ran", locale, "report.completion.ran", ran));
        } else if (report.durationSeconds() > 0) {
            rows.add(entry("completion:ran", locale, "report.completion.ran",
                                PlanViewFormatting.duration(report.durationSeconds())));
        }
        appendDeliverables(rows, locale);
        appendDecisions(rows, locale);
        appendOwnCalls(rows, locale);
        appendChangeMap(rows, locale);
        appendTokens(rows, locale);
        return rows;
    }

    private String summary(String locale) {
        var parts = new ArrayList<String>();
        parts.add(PlanViewFormatting.clean(report.planId()));
        parts.add(MessageCatalogue.format(locale, "report.completion.title",
                            Map.of("plan", PlanViewFormatting.clean(report.planId()))));
        parts.add(PlanViewFormatting.clean(report.outcome()));
        if (report.pullRequestNumber() != null) {
            parts.add("#" + report.pullRequestNumber());
        }
        if (report.ended() != null && !report.ended().isBlank()) {
            parts.add(PlanViewFormatting.clean(report.ended()));
        }
        return String.join(PlanViewFormatting.DOT_SEPARATOR, parts);
    }

    private void appendDeliverables(List<TuiRow> rows, String locale) {
        var deliverables = report.deliverables();
        if (deliverables.isEmpty()) {
            return;
        }
        long done = deliverables.stream().filter(deliverable -> "done".equalsIgnoreCase(deliverable.state())).count();
        rows.add(TuiRow.leaf("completion:deliverables", INDENT
                            + MessageCatalogue.format(locale, "report.completion.deliverables", Map.of())
                            + KEY_SEPARATOR + MessageCatalogue.format(locale, "report.completion.deliverables_done",
                            Map.of("done", done, "total", deliverables.size()))));
        int index = 0;
        for (var deliverable : deliverables) {
            rows.add(TuiRow.leaf("completion:deliverable:" + index++,
                                INDENT + INDENT + MessageCatalogue.format(locale,
                                                    "operator.plan.completion.deliverable",
                                                    Map.of("state", PlanViewFormatting.clean(deliverable.state()),
                                                                        "name", PlanViewFormatting.clean(deliverable.name())))));
        }
    }

    private void appendDecisions(List<TuiRow> rows, String locale) {
        var decisions = report.decisionRecords();
        if (decisions.isEmpty()) {
            return;
        }
        rows.add(TuiRow.leaf("completion:decisions", INDENT
                            + MessageCatalogue.format(locale, "report.completion.decisions", Map.of())
                            + KEY_SEPARATOR + decisions.size()));
        int index = 0;
        for (var decision : decisions) {
            var text = PlanViewFormatting.clean(decision.id());
            if (decision.question() != null && !decision.question().isBlank()) {
                text += " " + PlanViewFormatting.clean(decision.question());
            }
            rows.add(TuiRow.leaf("completion:decision:" + index++,
                                INDENT + INDENT + text + " (" + PlanViewFormatting.clean(decision.by()) + ")"));
        }
    }

    private void appendOwnCalls(List<TuiRow> rows, String locale) {
        var ownCalls = report.ownCalls();
        if (ownCalls.isEmpty()) {
            return;
        }
        rows.add(TuiRow.leaf("completion:own-calls", INDENT
                            + String.format(LABEL_WIDTH, MessageCatalogue.format(locale, "report.completion.own_calls", Map.of()))
                            + KEY_SEPARATOR + ownCalls.size()));
        int index = 0;
        for (var ownCall : ownCalls) {
            var kind = MessageCatalogue.format(locale, "report.completion.own_call_kind",
                                Map.of("kind", ownCall.kind()));
            var description = PlanViewFormatting.clean(ownCall.description());
            var judgment = judgmentText(locale, ownCall);
            var line = INDENT + INDENT + String.format(OWN_CALL_KIND_WIDTH, kind) + description;
            if (hasShortcutDetails(ownCall)) {
                rows.add(TuiRow.leaf("completion:own-call:" + index++, line));
                var details = new ArrayList<String>();
                if (ownCall.limit() != null && !ownCall.limit().isBlank()) {
                    details.add(MessageCatalogue.format(locale, "report.completion.own_call_limit",
                                        Map.of("limit", PlanViewFormatting.clean(ownCall.limit()))));
                }
                if (ownCall.endsWhen() != null && !ownCall.endsWhen().isBlank()) {
                    details.add(MessageCatalogue.format(locale, "report.completion.own_call_ends",
                                        Map.of("condition", PlanViewFormatting.clean(ownCall.endsWhen()))));
                }
                details.add(judgment);
                rows.add(TuiRow.leaf("completion:own-call:" + index++ + ":detail",
                                    INDENT + INDENT + INDENT + String.join(PlanViewFormatting.DOT_SEPARATOR, details)));
            } else {
                rows.add(TuiRow.leaf("completion:own-call:" + index++, line
                                    + PlanViewFormatting.DOT_SEPARATOR + judgment));
            }
        }
    }

    private static boolean hasShortcutDetails(OwnCallRecord ownCall) {
        return ownCall.limit() != null && !ownCall.limit().isBlank()
                            || ownCall.endsWhen() != null && !ownCall.endsWhen().isBlank();
    }

    private static String judgmentText(String locale, OwnCallRecord ownCall) {
        if ("conflicts".equalsIgnoreCase(ownCall.judgment())) {
            var target = ownCall.conflictTarget();
            var item = target != null && !target.isBlank() ? PlanViewFormatting.clean(target) : "-";
            return MessageCatalogue.format(locale, "report.completion.own_call_conflicts", Map.of("item", item));
        }
        if ("consistent".equalsIgnoreCase(ownCall.judgment())) {
            return MessageCatalogue.format(locale, "report.completion.own_call_consistent", Map.of());
        }
        return PlanViewFormatting.clean(ownCall.judgment());
    }

    private void appendChangeMap(List<TuiRow> rows, String locale) {
        var changeMap = report.changeMap();
        if (changeMap == null) {
            return;
        }
        var value = changeMapState(locale, changeMap);
        if (changeMap.cause() != null && !changeMap.cause().isBlank()) {
            value += PlanViewFormatting.DOT_SEPARATOR
                                + MessageCatalogue.format(locale, "report.completion.change_map_cause",
                                Map.of("cause", PlanViewFormatting.clean(changeMap.cause())));
        }
        rows.add(TuiRow.leaf("completion:change-map", INDENT
                            + String.format(LABEL_WIDTH, MessageCatalogue.format(locale, "report.completion.change_map", Map.of()))
                            + KEY_SEPARATOR + value));
        var lines = changeMap.diagram().split("\n", -1);
        int index = 0;
        for (var line : lines) {
            rows.add(TuiRow.leaf("completion:change-map:" + index++,
                                INDENT + INDENT + diagramLine(line)));
        }
    }

    private static String changeMapState(String locale, ChangeMapRecord changeMap) {
        return MessageCatalogue.format(locale, "report.completion.change_map_state",
                            Map.of("state", changeMap.state().toLowerCase(Locale.ROOT)));
    }

    private static String diagramLine(String line) {
        return OutputSafety.stripControlSequences(line);
    }

    private void appendTokens(List<TuiRow> rows, String locale) {
        var usage = report.tokenUsage();
        if (usage == null) {
            return;
        }
        var split = TokenFormat.formatSplit(usage.total(), usage.input(), usage.output(), usage.floor(), locale);
        rows.add(TuiRow.leaf("completion:tokens", INDENT
                            + MessageCatalogue.format(locale, "report.completion.tokens", Map.of())
                            + KEY_SEPARATOR + split));
        var unmeasured = Math.max(0, usage.tasksTotal() - usage.tasksMeasured());
        rows.add(TuiRow.leaf("completion:coverage", INDENT + INDENT
                            + MessageCatalogue.format(locale, "report.tokens.coverage", Map.of())
                            + KEY_SEPARATOR + unmeasured + " / " + usage.tasksTotal()));
    }

    private static TuiRow entry(String id, String locale, String labelKey, @Nullable String value) {
        return TuiRow.leaf(id, INDENT
                            + String.format(LABEL_WIDTH, MessageCatalogue.format(locale, labelKey, Map.of()))
                            + PlanViewFormatting.clean(value));
    }

    private static String description(@Nullable String value) {
        return value != null ? value : "-";
    }
}
