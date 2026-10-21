/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.epicview;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.EpicTokens;
import de.planmarshall.api.control.RegisterSummary;
import de.planmarshall.api.control.TokenFigure;
import de.planmarshall.api.control.WorkRow;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.operator.OutputSafety;

/**
 * Pure formatting of an epic's view: the state and mode of the head, the status and the cause a
 * row waits for, the counts of a workstream, the tokens with their floor and coverage, and the
 * events' times.
 *
 * <p>Every runtime text passes through {@link OutputSafety} before it is composed. Nothing here
 * reads or writes anything.
 */
public final class EpicViewFormatting {

    private static final String DOT = " \u00b7 ";
    private static final String PULL_REQUEST = "#";

    private EpicViewFormatting() {
    }

    /**
     * @param text the runtime text
     * @return the text without control sequences or line breaks
     */
    public static String clean(@Nullable String text) {
        if (text == null) {
            return "";
        }
        return OutputSafety.stripControlSequences(text).replace('\n', ' ').replace('\t', ' ');
    }

    /**
     * @param locale the interface locale
     * @param epic   the epic
     * @return the state of the head, from the closed list
     */
    public static String stateText(String locale, EpicResource epic) {
        var state = "ended".equals(epic.state()) && epic.end() != null ? "ended" : epic.state();
        return MessageCatalogue.format(locale, "overview.epic.state",
                            Map.of("state", clean(state).toLowerCase(Locale.ROOT)));
    }

    /**
     * @param locale the interface locale
     * @param epic   the epic
     * @return {@code automatic} or {@code manual}
     */
    public static String modeText(String locale, EpicResource epic) {
        return MessageCatalogue.format(locale, "overview.epic.mode",
                            Map.of("mode", epic.autoStart() ? "automatic" : "manual"));
    }

    /**
     * @param locale  the interface locale
     * @param running how many plans run
     * @param scope   how many may run at once
     * @return the {@code 3 of 3 at once} phrase
     */
    public static String atOnce(String locale, int running, int scope) {
        return MessageCatalogue.format(locale, "overview.epic.at_once",
                            Map.of("running", running, "scope", scope));
    }

    /**
     * @param locale the interface locale
     * @param row    the row
     * @return the row's status from the closed list
     */
    public static String statusText(String locale, WorkRow row) {
        return MessageCatalogue.format(locale, "overview.epic.status",
                            Map.of("status", statusKey(row.status())));
    }

    /**
     * @param locale   the interface locale
     * @param row      the row
     * @param running  how many plans run
     * @param scope    how many may run at once
     * @return what the row waits for, or {@code can start}
     */
    public static String waitText(String locale, WorkRow row, int running, int scope) {
        var reason = row.waits();
        if (reason == null || "none".equals(reason.value())) {
            return MessageCatalogue.format(locale, "overview.epic.waits.none", Map.of());
        }
        var params = new LinkedHashMap<String, Object>();
        params.put("plan", clean(row.waitPlanId() != null ? row.waitPlanId() : row.planId()));
        params.put("lock", clean(row.waitPlanId() != null ? row.waitPlanId() : ""));
        params.put("running", running);
        params.put("scope", scope);
        var key = switch (reason.value()) {
            case "dependency" -> "overview.epic.waits.dependency";
            case "lock" -> "overview.epic.waits.lock";
            case "scope" -> "overview.epic.waits.scope";
            case "overlap" -> "overview.epic.waits.overlap";
            default -> "overview.epic.waits.none";
        };
        return MessageCatalogue.format(locale, key, params);
    }

    /**
     * @param locale the interface locale
     * @param row    the row
     * @return why that row cannot start, in one line
     */
    public static String waitReasonLine(String locale, WorkRow row) {
        var reason = row.waits();
        var other = clean(row.waitPlanId() != null ? row.waitPlanId() : "");
        var key = reason == null ? "held" : reason.value();
        var params = new LinkedHashMap<String, Object>();
        params.put("plan", clean(row.planId()));
        params.put("other", other);
        var message = switch (key) {
            case "dependency" -> "operator.actions.epic.reason.dependency";
            case "lock" -> "operator.actions.epic.reason.lock";
            case "scope" -> "operator.actions.epic.reason.scope";
            case "overlap" -> "operator.actions.epic.reason.overlap";
            case "stale" -> "operator.actions.epic.reason.stale";
            case "held" -> "operator.actions.epic.reason.held";
            default -> null;
        };
        return message == null ? "" : MessageCatalogue.format(locale, message, params);
    }

    /**
     * @param locale the interface locale
     * @param rows   the workstream's rows
     * @return the counts of the rows, in the order of the closed status list
     */
    public static String countsText(String locale, List<WorkRow> rows) {
        var counts = new LinkedHashMap<String, Integer>();
        for (var row : rows) {
            counts.merge(statusKey(row.status()), 1, Integer::sum);
        }
        var parts = new ArrayList<String>();
        for (var entry : counts.entrySet()) {
            var key = countsKey(entry.getKey());
            if (key != null) {
                parts.add(MessageCatalogue.format(locale, key, Map.of("count", entry.getValue())));
            }
        }
        return String.join(DOT, parts);
    }

    /**
     * @param locale the interface locale
     * @param row    the row
     * @return the status glyph the row carries
     */
    public static String statusGlyph(WorkRow row) {
        return switch (statusKey(row.status())) {
            case "shipped" -> "\u2713";
            case "running", "pausing" -> "\u25b6";
            case "paused" -> "\u23f8";
            case "staged" -> "\u25cb";
            case "cancelled", "abandoned", "returned" -> "\u2717";
            case "held", "waiting" -> "!";
            default -> "\u2022";
        };
    }

    /**
     * @param row the row
     * @return the pull request by its number, or an empty string
     */
    public static String pullRequest(WorkRow row) {
        return row.pullRequestNumber() != null ? PULL_REQUEST + row.pullRequestNumber() : "";
    }

    /**
     * @param locale the interface locale
     * @param tokens the epic's tokens
     * @param plans  the number of plans of the epic
     * @return the tokens line with the split, the floor, and the coverage
     */
    public static String tokensText(String locale, EpicTokens tokens, int plans) {
        var scope = MessageCatalogue.format(locale, "overview.epic.tokens_scope", Map.of("plans", plans));
        var total = tokens.total();
        String figure;
        if (!total.measured()) {
            figure = MessageCatalogue.format(locale, "operator.epic.not_measured", Map.of());
        } else {
            figure = TokenFormat.formatSplit(total.total(), total.input(), total.output(), total.floor(), locale);
        }
        return MessageCatalogue.format(locale, "operator.epic.tokens_line",
                            Map.of("split", figure, "scope", scope));
    }

    /**
     * @param locale the interface locale
     * @param figure the figure
     * @return the figure with its split
     */
    public static String tokenFigure(String locale, TokenFigure figure) {
        if (!figure.measured()) {
            return MessageCatalogue.format(locale, "operator.epic.not_measured", Map.of());
        }
        return TokenFormat.formatSplit(figure.total(), figure.input(), figure.output(), figure.floor(), locale);
    }

    /**
     * @param locale    the interface locale
     * @param findings  the findings register
     * @param register  {@code findings} or {@code watches}
     * @return the register's counts
     */
    public static String registerCounts(String locale, RegisterSummary findings, String register) {
        if ("watches".equals(register)) {
            return MessageCatalogue.format(locale, "overview.epic.watches_counts",
                                Map.of("open", findings.open(), "kept", findings.kept()));
        }
        return MessageCatalogue.format(locale, "overview.epic.findings_counts",
                            Map.of("new", findings.newCount(), "open", findings.open(), "kept", findings.kept()));
    }

    /**
     * @param at the event's time, an ISO instant or an already short time
     * @return the time as {@code HH:mm}, or the text unchanged when it cannot be read as an instant
     */
    public static String eventTime(@Nullable String at) {
        if (at == null || at.isBlank()) {
            return "";
        }
        try {
            return DateTimeFormatter.ofPattern("HH:mm")
                                .format(Instant.parse(at).atZone(ZoneId.systemDefault()));
        } catch (DateTimeParseException _) {
            return clean(at);
        }
    }

    /**
     * @param status the runtime's status
     * @return the key of the closed list
     */
    public static String statusKey(@Nullable String status) {
        if (status == null || status.isBlank()) {
            return "staged";
        }
        var key = clean(status).trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return "waiting_for_you".equals(key) ? "waiting" : key;
    }

    private static @Nullable String countsKey(String status) {
        return switch (status) {
            case "staged", "running", "pausing", "paused", "waiting", "shipped", "cancelled", "abandoned",
                "held" -> "overview.epic.counts." + status;
            default -> null;
        };
    }
}
