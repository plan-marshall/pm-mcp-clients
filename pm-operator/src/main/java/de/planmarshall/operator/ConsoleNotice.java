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

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Formats notice blocks printed above the prompt (via {@code LineReader.printAbove(...)})
 * when runtime events arrive or asynchronous notifications are displayed (04-console.adoc#pushed).
 * Enforces output safety by stripping control sequences and ANSI escape codes from data fields.
 */
public final class ConsoleNotice {

    private static final Pattern ANSI_PATTERN = Pattern.compile(
                                    "\\x1B(?:\\[[0-?]*[ -/]*[@-~]|\\].*?(?:\\x07|\\x1B\\\\))");
    private static final Pattern CONTROL_CHARS = Pattern.compile(
                                    "[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");
    private static final String SCOPE = "scope";
    private static final String KEY_PROJECT = "project";
    private static final String PLAN_PREFIX = "◆ Plan ";
    private static final String EPIC_PREFIX = "◆ Epic ";

    private ConsoleNotice() {
    }

    /**
     * Sanitizes untrusted model or remote text by removing ANSI escape sequences and control characters.
     *
     * @param text the raw text
     * @return sanitized text safe for terminal rendering
     */
    public static String stripControlSequences(@Nullable String text) {
        if (text == null) {
            return "";
        }
        var noAnsi = ANSI_PATTERN.matcher(text).replaceAll("");
        return CONTROL_CHARS.matcher(noAnsi).replaceAll("");
    }

    /**
     * Formats a decision / question notice block.
     *
     * @param id             the question id (e.g. "Q-007")
     * @param scope          the plan or epic scope (e.g. "plan fix-relay")
     * @param summary        summary note (e.g. "blocks the plan")
     * @param text           the question body text
     * @param recommendation recommendation string or {@code null}
     * @return multi-line formatted notice
     */
    public static String formatQuestion(String id, String scope, @Nullable String summary, @Nullable String text,
                                    @Nullable String recommendation) {
        var sb = new StringBuilder();
        sb.append("◆ Question ").append(Objects.requireNonNull(id, "id"))
                                        .append(" · ").append(Objects.requireNonNull(scope, SCOPE));
        if (summary != null && !summary.isBlank()) {
            sb.append(" · ").append(stripControlSequences(summary));
        }
        sb.append('\n');
        if (text != null && !text.isBlank()) {
            sb.append("  ").append(stripControlSequences(text)).append('\n');
        }
        if (recommendation != null && !recommendation.isBlank()) {
            sb.append("  Recommended: ").append(stripControlSequences(recommendation)).append('\n');
        }
        sb.append("  answer ").append(id).append("   ·   questions");
        return sb.toString();
    }

    /**
     * Formats a waiver request notice block.
     *
     * @param id    the waiver request id (e.g. "WR-2")
     * @param scope the plan scope (e.g. "plan fix-relay")
     * @param gate  the target gate (e.g. "ci_check:integration-tests")
     * @return multi-line formatted notice
     */
    public static String formatWaiverRequest(String id, String scope, @Nullable String gate) {
        var sb = new StringBuilder();
        sb.append("◆ Waiver request ").append(Objects.requireNonNull(id, "id"))
                                        .append(" · ").append(Objects.requireNonNull(scope, SCOPE));
        if (gate != null && !gate.isBlank()) {
            sb.append(" · gate ").append(stripControlSequences(gate));
        }
        sb.append('\n');
        sb.append("  answer ").append(id).append("   ·   questions");
        return sb.toString();
    }

    /**
     * Formats a notice for holds, pauses, waits, or findings.
     *
     * @param kind   the notice kind
     * @param scope  the plan or epic scope
     * @param detail additional explanation
     * @return formatted single-line notice
     */
    public static String formatHoldNotice(String kind, String scope, @Nullable String detail) {
        var sanitizedDetail = stripControlSequences(detail);
        return switch (kind) {
            case "operator_wait" -> PLAN_PREFIX + scope + " waits for you: " + sanitizedDetail;
            case "integrity_pause" -> "◆ Integrity pause · plan " + scope + ": " + sanitizedDetail;
            case "stalled" -> PLAN_PREFIX + scope + " stalled: " + sanitizedDetail;
            case "condition" -> PLAN_PREFIX + scope + " needs a fix: " + sanitizedDetail;
            case "held" -> EPIC_PREFIX + scope + ": " + sanitizedDetail;
            case "landing_failed" -> EPIC_PREFIX + scope + ": the landing failed: " + sanitizedDetail;
            case "findings" -> EPIC_PREFIX + scope + ": " + sanitizedDetail;
            case "setup" -> "◆ Project " + scope + ": the setup needs you: " + sanitizedDetail;
            default -> "◆ " + kind + " · " + scope + ": " + sanitizedDetail;
        };
    }

    /**
     * Formats a browser pairing notification.
     *
     * @param label device label (e.g. "local (Firefox on mbp-oliver)")
     * @return formatted notice
     */
    public static String formatPairedNotice(String label) {
        return "✓ Paired: " + stripControlSequences(label);
    }

    /**
     * Formats a fresh pairing code notification after previous code was redeemed.
     *
     * @param code       fresh pairing code
     * @param validUntil validity time string
     * @param url        pairing URL
     * @return formatted notice
     */
    public static String formatFreshCodeNotice(String code, String validUntil, String url) {
        return "✓ New code: " + code + ", valid until " + validUntil
                                        + ". The previous code no longer works.\n  " + url;
    }

    /**
     * Formats an answered-elsewhere notice.
     *
     * @param id      question ID
     * @param channel channel name ("web app", "console")
     * @param device  device description
     * @param answer   chosen option or answer
     * @return formatted notice
     */
    public static String formatAnsweredElsewhere(String id, String channel, @Nullable String device,
                                    @Nullable String answer) {
        var dev = device != null && !device.isBlank() ? " (" + stripControlSequences(device) + ")" : "";
        return "– " + id + " was answered in the " + channel + dev + ": "
                                        + stripControlSequences(answer) + ". Nothing was sent from here.";
    }

    /**
     * Parses a generic event map from the SSE stream and formats the corresponding notice, if any.
     *
     * @param event the event data map
     * @return formatted notice, or {@code null} if event requires no notice
     */
    public static @Nullable String fromEventMap(@Nullable Map<String, Object> event) {
        if (event == null) {
            return null;
        }
        var type = String.valueOf(event.getOrDefault("type", event.getOrDefault("event", "")));
        return switch (type) {
            case "question", "decision" -> formatQuestion(
                                            String.valueOf(event.getOrDefault("id", "Q-???")),
                                            String.valueOf(event.getOrDefault(SCOPE, "plan")),
                                            (String) event.get("summary"),
                                            (String) event.get("text"),
                                            (String) event.get("recommendation"));
            case "waiver_request" -> formatWaiverRequest(
                                            String.valueOf(event.getOrDefault("id", "WR-?")),
                                            String.valueOf(event.getOrDefault(SCOPE, "plan")),
                                            (String) event.get("gate"));
            case "paired" -> formatPairedNotice(
                                            String.valueOf(event.getOrDefault("label", "unknown")));
            case "fresh_code" -> formatFreshCodeNotice(
                                            String.valueOf(event.get("code")),
                                            String.valueOf(event.getOrDefault("valid_until", "")),
                                            String.valueOf(event.getOrDefault("url", "")));
            case "operator_wait", "integrity_pause", "stalled", "condition", "held", "landing_failed", "findings", "setup" ->
                formatHoldNotice(
                                                type,
                                                String.valueOf(event.getOrDefault(SCOPE, "")),
                                                String.valueOf(event.getOrDefault("detail", event.getOrDefault("reason", ""))));
            case "report.sent", "report" -> formatReportNotice(event);
            default -> null;
        };
    }

    /**
     * Formats a quiet notice for an incoming report pushed over SSE.
     *
     * @param event the event map
     * @return formatted quiet notice string
     */
    public static @Nullable String formatReportNotice(Map<String, Object> event) {
        if (event.containsKey("count") && event.get("count") instanceof Number num && num.intValue() > 1) {
            var scope = String.valueOf(event.getOrDefault(SCOPE, event.getOrDefault(KEY_PROJECT, "plan-marshall")));
            return "▪ " + num.intValue() + " new reports · " + stripControlSequences(scope) + " · reports";
        }
        var id = String.valueOf(event.getOrDefault("id", ""));
        var subject = (String) event.get("subject");
        var leaf = extractLeaf(event);

        if (subject == null || subject.isBlank()) {
            subject = (String) event.get("summary");
        }
        if (subject == null || subject.isBlank()) {
            subject = "New report";
        }

        var sb = new StringBuilder("▪ ");
        sb.append(stripControlSequences(leaf)).append(": ").append(stripControlSequences(subject));
        if (!id.isBlank()) {
            sb.append(" · reports open ").append(stripControlSequences(id));
        }
        return sb.toString();
    }

    private static String extractLeaf(Map<String, Object> event) {
        var from = (String) event.get("from");
        if (from != null && !from.isBlank()) {
            if (from.contains(">")) {
                return from.substring(from.lastIndexOf('>') + 1).trim();
            } else if (from.contains("›")) {
                return from.substring(from.lastIndexOf('›') + 1).trim();
            }
            return from.trim();
        }
        if (event.get("plan") != null) {
            return String.valueOf(event.get("plan"));
        }
        if (event.get(KEY_PROJECT) != null) {
            return String.valueOf(event.get(KEY_PROJECT));
        }
        return "plan";
    }
}

