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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * Formats notice blocks printed above the prompt (via {@code LineReader.printAbove(...)}) when
 * runtime events arrive or asynchronous notifications are displayed (04-console.adoc#pushed).
 *
 * <p>Every text is a key of the message catalogue (area {@code operator}, {@code
 * operator.console.*}), and every value taken from the runtime is put through {@link
 * TerminalSanitizer} before it is composed, so a crafted value cannot move the cursor, write the
 * clipboard, set the window title, or fake a prompt.
 */
public final class ConsoleNotice {

    static final String DEFAULT_LOCALE = "en";
    private static final String BULLET = "\u25c6 ";
    private static final String CHECK = "\u2713 ";
    private static final String DASH = "\u2013 ";
    private static final String QUIET = "\u25aa ";
    private static final String SCOPE = "scope";
    private static final String KEY_PROJECT = "project";
    private static final String DETAIL = "detail";

    private ConsoleNotice() {
    }

    /**
     * Sanitizes untrusted model or remote text by removing ANSI escape sequences and control
     * characters. The single rule lives in {@link TerminalSanitizer}; this is its console-facing
     * name.
     *
     * @param text the raw text
     * @return sanitized text safe for terminal rendering
     */
    public static String stripControlSequences(@Nullable String text) {
        return TerminalSanitizer.sanitize(text);
    }

    /**
     * Formats a decision / question notice block in English.
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
        return formatQuestion(DEFAULT_LOCALE, id, scope, summary, text, recommendation);
    }

    /**
     * Formats a decision / question notice block.
     *
     * @param locale         the interface locale
     * @param id             the question id (e.g. "Q-007")
     * @param scope          the plan or epic scope (e.g. "plan fix-relay")
     * @param summary        summary note (e.g. "blocks the plan")
     * @param text           the question body text
     * @param recommendation recommendation string or {@code null}
     * @return multi-line formatted notice
     */
    public static String formatQuestion(String locale, String id, String scope, @Nullable String summary,
                        @Nullable String text, @Nullable String recommendation) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, SCOPE);
        var sb = new StringBuilder(BULLET);
        sb.append(MessageCatalogue.format(locale, "operator.console.question.notice",
                            Map.of("id", id, SCOPE, scope)));
        if (summary != null && !summary.isBlank()) {
            sb.append(" \u00b7 ").append(TerminalSanitizer.sanitize(summary));
        }
        sb.append('\n');
        if (text != null && !text.isBlank()) {
            sb.append(MessageCatalogue.format(locale, "operator.console.question.body",
                                Map.of("text", TerminalSanitizer.sanitize(text)))).append('\n');
        }
        if (recommendation != null && !recommendation.isBlank()) {
            sb.append(MessageCatalogue.format(locale, "operator.console.question.recommended",
                                Map.of("option", TerminalSanitizer.sanitize(recommendation)))).append('\n');
        }
        sb.append(MessageCatalogue.format(locale, "operator.console.question.hint", Map.of("id", id)));
        return sb.toString();
    }

    /**
     * Formats a waiver request notice block in English.
     *
     * @param id    the waiver request id (e.g. "WR-2")
     * @param scope the plan scope (e.g. "plan fix-relay")
     * @param gate  the target gate (e.g. "ci_check:integration-tests")
     * @return multi-line formatted notice
     */
    public static String formatWaiverRequest(String id, String scope, @Nullable String gate) {
        return formatWaiverRequest(DEFAULT_LOCALE, id, scope, gate);
    }

    /**
     * Formats a waiver request notice block.
     *
     * @param locale the interface locale
     * @param id     the waiver request id (e.g. "WR-2")
     * @param scope  the plan scope (e.g. "plan fix-relay")
     * @param gate   the target gate (e.g. "ci_check:integration-tests")
     * @return multi-line formatted notice
     */
    public static String formatWaiverRequest(String locale, String id, String scope, @Nullable String gate) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, SCOPE);
        var sb = new StringBuilder(BULLET);
        if (gate != null && !gate.isBlank()) {
            sb.append(MessageCatalogue.format(locale, "operator.console.notice.waiver_request",
                                Map.of("id", id, "scope", scope, "gate", TerminalSanitizer.sanitize(gate))));
        } else {
            sb.append(MessageCatalogue.format(locale, "operator.console.notice.waiver_request_plain",
                                Map.of("id", id, "scope", scope)));
        }
        sb.append('\n');
        sb.append(MessageCatalogue.format(locale, "operator.console.question.hint", Map.of("id", id)));
        return sb.toString();
    }

    /**
     * Formats a notice for holds, pauses, waits, or findings in English.
     *
     * @param kind   the notice kind
     * @param scope  the plan or epic scope
     * @param detail additional explanation
     * @return formatted single-line notice
     */
    public static String formatHoldNotice(String kind, String scope, @Nullable String detail) {
        return formatHoldNotice(DEFAULT_LOCALE, kind, scope, detail);
    }

    /**
     * Formats a notice for holds, pauses, waits, or findings.
     *
     * @param locale the interface locale
     * @param kind   the notice kind
     * @param scope  the plan or epic scope
     * @param detail additional explanation
     * @return formatted single-line notice
     */
    public static String formatHoldNotice(String locale, String kind, String scope, @Nullable String detail) {
        var safeDetail = TerminalSanitizer.sanitize(detail);
        var text = switch (kind) {
            case "operator_wait" -> MessageCatalogue.format(locale, "operator.console.notice.operator_wait",
                                Map.of("name", scope, "what", safeDetail));
            case "integrity_pause" -> MessageCatalogue.format(locale, "operator.console.notice.integrity_pause",
                                Map.of("plan", scope, "reason", safeDetail));
            case "stalled" -> MessageCatalogue.format(locale, "operator.console.notice.stalled",
                                Map.of("plan", scope, "reason", safeDetail));
            case "condition" -> MessageCatalogue.format(locale, "operator.console.notice.condition",
                                Map.of("plan", scope, "condition", safeDetail));
            case "held" -> MessageCatalogue.format(locale, "operator.console.notice.held",
                                Map.of("epic", scope, DETAIL, safeDetail));
            case "landing_failed" -> MessageCatalogue.format(locale, "operator.console.notice.landing_failed",
                                Map.of("epic", scope, "reason", safeDetail));
            case "findings" -> MessageCatalogue.format(locale, "operator.console.notice.findings",
                                Map.of("epic", scope, DETAIL, safeDetail));
            case "setup" -> MessageCatalogue.format(locale, "operator.console.notice.setup",
                                Map.of(KEY_PROJECT, scope, "what", safeDetail));
            default -> MessageCatalogue.format(locale, "operator.console.notice.generic",
                                Map.of("kind", kind, SCOPE, scope, DETAIL, safeDetail));
        };
        return BULLET + text;
    }

    /**
     * Formats a browser pairing notification in English.
     *
     * @param label device label (e.g. "local (Firefox on mbp-oliver)")
     * @return formatted notice
     */
    public static String formatPairedNotice(String label) {
        return formatPairedNotice(DEFAULT_LOCALE, label);
    }

    /**
     * Formats a browser pairing notification.
     *
     * @param locale the interface locale
     * @param label  device label (e.g. "local (Firefox on mbp-oliver)")
     * @return formatted notice
     */
    public static String formatPairedNotice(String locale, String label) {
        return CHECK + MessageCatalogue.format(locale, "operator.console.paired",
                            Map.of("label", TerminalSanitizer.sanitize(label)));
    }

    /**
     * Formats a fresh pairing code notification after the previous code was redeemed.
     *
     * @param code       fresh pairing code
     * @param validUntil validity time string
     * @param url        pairing URL
     * @return formatted notice
     */
    public static String formatFreshCodeNotice(String code, String validUntil, String url) {
        return formatFreshCodeNotice(DEFAULT_LOCALE, code, validUntil, url);
    }

    /**
     * Formats a fresh pairing code notification after the previous code was redeemed.
     *
     * @param locale     the interface locale
     * @param code       fresh pairing code
     * @param validUntil validity time string
     * @param url        pairing URL
     * @return formatted notice
     */
    public static String formatFreshCodeNotice(String locale, String code, String validUntil, String url) {
        return CHECK + MessageCatalogue.format(locale, "operator.console.new_code",
                            Map.of("code", code, "time", validUntil))
                            + "\n" + MessageCatalogue.format(locale, "operator.console.url_indent",
                            Map.of("url", TerminalSanitizer.sanitize(url)));
    }

    /**
     * Formats an answered-elsewhere notice in English.
     *
     * @param id      question ID
     * @param channel channel name ("web app", "console")
     * @param device  device description
     * @param answer  chosen option or answer
     * @return formatted notice
     */
    public static String formatAnsweredElsewhere(String id, String channel, @Nullable String device,
                        @Nullable String answer) {
        return formatAnsweredElsewhere(DEFAULT_LOCALE, id, channel, device, answer);
    }

    /**
     * Formats an answered-elsewhere notice.
     *
     * @param locale  the interface locale
     * @param id      question ID
     * @param channel channel name ("web app", "console")
     * @param device  device description
     * @param answer  chosen option or answer
     * @return formatted notice
     */
    public static String formatAnsweredElsewhere(String locale, String id, String channel, @Nullable String device,
                        @Nullable String answer) {
        var safeAnswer = TerminalSanitizer.sanitize(answer);
        if (device != null && !device.isBlank()) {
            return DASH + MessageCatalogue.format(locale, "operator.console.question.answered_elsewhere",
                                Map.of("id", id, "channel", channel, "device", TerminalSanitizer.sanitize(device),
                                                    "option", safeAnswer));
        }
        return DASH + MessageCatalogue.format(locale, "operator.console.question.answered_elsewhere_plain",
                            Map.of("id", id, "channel", channel, "option", safeAnswer));
    }

    /**
     * Parses a generic event map from the SSE stream and formats the corresponding notice, if any.
     *
     * @param event the event data map
     * @return formatted notice, or {@code null} if event requires no notice
     */
    public static @Nullable String fromEventMap(@Nullable Map<String, Object> event) {
        return fromEventMap(DEFAULT_LOCALE, event);
    }

    /**
     * Parses a generic event map from the SSE stream and formats the corresponding notice, if any.
     *
     * @param locale the interface locale
     * @param event  the event data map
     * @return formatted notice, or {@code null} if event requires no notice
     */
    public static @Nullable String fromEventMap(String locale, @Nullable Map<String, Object> event) {
        if (event == null) {
            return null;
        }
        var type = String.valueOf(event.getOrDefault("type", event.getOrDefault("event", "")));
        return switch (type) {
            case "question", "decision" -> formatQuestion(locale,
                                String.valueOf(event.getOrDefault("id", "Q-???")),
                                String.valueOf(event.getOrDefault(SCOPE, "plan")),
                                (String) event.get("summary"),
                                (String) event.get("text"),
                                (String) event.get("recommendation"));
            case "waiver_request" -> formatWaiverRequest(locale,
                                String.valueOf(event.getOrDefault("id", "WR-?")),
                                String.valueOf(event.getOrDefault(SCOPE, "plan")),
                                (String) event.get("gate"));
            case "paired" -> formatPairedNotice(locale,
                                String.valueOf(event.getOrDefault("label", "unknown")));
            case "fresh_code" -> formatFreshCodeNotice(locale,
                                String.valueOf(event.get("code")),
                                String.valueOf(event.getOrDefault("valid_until", "")),
                                String.valueOf(event.getOrDefault("url", "")));
            case "operator_wait", "integrity_pause", "stalled", "condition", "held", "landing_failed", "findings", "setup" ->
                formatHoldNotice(locale,
                                    type,
                                    String.valueOf(event.getOrDefault(SCOPE, "")),
                                    String.valueOf(event.getOrDefault(DETAIL, event.getOrDefault("reason", ""))));
            case "report.sent", "report" -> formatReportNotice(locale, event);
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
        return formatReportNotice(DEFAULT_LOCALE, event);
    }

    /**
     * Formats a quiet notice for an incoming report pushed over SSE.
     *
     * @param locale the interface locale
     * @param event  the event map
     * @return formatted quiet notice string
     */
    public static @Nullable String formatReportNotice(String locale, Map<String, Object> event) {
        if (event.containsKey("count") && event.get("count") instanceof Number num && num.intValue() > 1) {
            var scope = String.valueOf(event.getOrDefault(SCOPE, event.getOrDefault(KEY_PROJECT, "plan-marshall")));
            return QUIET + MessageCatalogue.format(locale, "operator.console.notice.reports_many",
                                Map.of("count", num.intValue(), SCOPE, TerminalSanitizer.sanitize(scope)));
        }
        var id = String.valueOf(event.getOrDefault("id", ""));
        var subject = (String) event.get("subject");
        var leaf = extractLeaf(event);

        if (subject == null || subject.isBlank()) {
            subject = (String) event.get("summary");
        }
        if (subject == null || subject.isBlank()) {
            subject = MessageCatalogue.format(locale, "operator.console.notice.report_new", Map.of());
        }
        var safeLeaf = TerminalSanitizer.sanitize(leaf);
        var safeSubject = TerminalSanitizer.sanitize(subject);
        if (id.isBlank()) {
            return QUIET + MessageCatalogue.format(locale, "operator.console.notice.report_line",
                                Map.of("leaf", safeLeaf, "subject", safeSubject));
        }
        return QUIET + MessageCatalogue.format(locale, "operator.console.notice.report_line_open",
                            Map.of("leaf", safeLeaf, "subject", safeSubject, "id", TerminalSanitizer.sanitize(id)));
    }

    private static String extractLeaf(Map<String, Object> event) {
        var from = (String) event.get("from");
        if (from != null && !from.isBlank()) {
            if (from.contains(">")) {
                return from.substring(from.lastIndexOf('>') + 1).trim();
            } else if (from.contains("\u203a")) {
                return from.substring(from.lastIndexOf('\u203a') + 1).trim();
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
