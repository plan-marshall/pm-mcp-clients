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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.event.EventKind;
import de.planmarshall.api.json.JsonTree;
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
    private static final String OPTION = "option";
    private static final String REASON = "reason";
    private static final String CONDITION = "condition";
    private static final String COUNT = "count";
    private static final String SUBJECT = "subject";
    private static final String KEY_PROJECT = "project";
    private static final String DETAIL = "detail";

    /** The channel of an answer this console posted itself (02-operator-channel.adoc). */
    private static final String CHANNEL_CLI = "cli";

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
                                Map.of(OPTION, TerminalSanitizer.sanitize(recommendation)))).append('\n');
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
                                Map.of("id", id, SCOPE, scope, "gate", TerminalSanitizer.sanitize(gate))));
        } else {
            sb.append(MessageCatalogue.format(locale, "operator.console.notice.waiver_request_plain",
                                Map.of("id", id, SCOPE, scope)));
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
                                Map.of("plan", scope, REASON, safeDetail));
            case "stalled" -> MessageCatalogue.format(locale, "operator.console.notice.stalled",
                                Map.of("plan", scope, REASON, safeDetail));
            case CONDITION -> MessageCatalogue.format(locale, "operator.console.notice.condition",
                                Map.of("plan", scope, CONDITION, safeDetail));
            case "held" -> MessageCatalogue.format(locale, "operator.console.notice.held",
                                Map.of("epic", scope, DETAIL, safeDetail));
            case "landing_failed" -> MessageCatalogue.format(locale, "operator.console.notice.landing_failed",
                                Map.of("epic", scope, REASON, safeDetail));
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
                                                    OPTION, safeAnswer));
        }
        return DASH + MessageCatalogue.format(locale, "operator.console.question.answered_elsewhere_plain",
                            Map.of("id", id, "channel", channel, OPTION, safeAnswer));
    }

    /**
     * Parses a generic event map from the SSE stream and formats the corresponding notice, if any.
     *
     * @param event the event data map
     * @return formatted notice, or {@code null} if event requires no notice
     */
    public static @Nullable String fromEventMap(@Nullable Map<String, Object> event) {
        return fromEventMap(DEFAULT_LOCALE, null, event);
    }

    /**
     * Parses a generic event map from the SSE stream and formats the corresponding notice, if any.
     *
     * @param locale the interface locale
     * @param event  the event data map
     * @return formatted notice, or {@code null} if event requires no notice
     */
    public static @Nullable String fromEventMap(String locale, @Nullable Map<String, Object> event) {
        return fromEventMap(locale, null, event);
    }

    /**
     * Formats the notice an SSE event becomes: the event name decides the notice through the
     * shared table of {@link EventKind}, and the {@code type} member of the data is the fallback
     * only (CLI-UC-P-04).
     *
     * @param locale    the interface locale
     * @param eventName the SSE event name, or {@code null} when the caller has none
     * @param event     the event data map
     * @return formatted notice, or {@code null} if event requires no notice
     */
    public static @Nullable String fromEventMap(String locale, @Nullable String eventName,
                        @Nullable Map<String, Object> event) {
        if (event == null) {
            return null;
        }
        var payload = payloadOf(event);
        var kind = noticeKindOf(eventName, event);
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case QUESTION -> questionNotice(locale, payload);
            case WAIVER_REQUEST -> waiverNotice(locale, payload);
            case ANSWERED_ELSEWHERE -> answeredElsewhere(locale, payload);
            case PAIRED -> formatPairedNotice(locale,
                                String.valueOf(payload.getOrDefault("label", "unknown")));
            case FRESH_CODE -> formatFreshCodeNotice(locale,
                                TerminalSanitizer.sanitize(String.valueOf(payload.get("code"))),
                                TerminalSanitizer.sanitize(String.valueOf(payload.getOrDefault("valid_until", ""))),
                                String.valueOf(payload.getOrDefault("url", "")));
            case OPERATOR_WAIT, INTEGRITY_PAUSE, STALLED, CONDITION, HELD, LANDING_FAILED, FINDINGS, SETUP ->
                formatHoldNotice(locale, kind.notice(), scopeOf(payload), detailOf(payload));
            case REPORT -> formatReportNotice(locale, reportOf(payload));
            case HOLD -> null;
        };
    }

    /**
     * The data of one streamed event: the runtime frames every event in its envelope
     * ({@code event}, {@code id}, {@code at}, {@code scope}, {@code data}), so the payload is the
     * {@code data} member; a legacy event that carries its fields at the top is its own payload.
     *
     * @param event the parsed event frame
     * @return the payload to render from
     */
    public static Map<String, Object> payloadOf(Map<String, Object> event) {
        var data = JsonTree.object(event, "data");
        return data != null ? data : event;
    }

    /**
     * Resolves the notice kind of one event: the event name first, the data's {@code type} only as
     * the fallback, and the hold family's own {@code kind} for a hold.
     *
     * @param eventName the SSE event name, or {@code null}
     * @param event     the event data
     * @return the notice kind, or {@code null} when the event becomes no notice
     */
    public static @Nullable EventKind noticeKindOf(@Nullable String eventName, Map<String, Object> event) {
        var payload = payloadOf(event);
        var byName = EventKind.fromEventName(eventName);
        if (byName == null) {
            byName = EventKind.fromEventName(JsonTree.string(event, "event"));
        }
        if (byName == EventKind.HOLD) {
            return EventKind.fromType(stringOf(payload, "kind"));
        }
        if (byName != null) {
            return byName == EventKind.QUESTION && isWaiver(payload) ? EventKind.WAIVER_REQUEST : byName;
        }
        var byType = EventKind.fromType(textOr(payload, "type", ""));
        return byType == EventKind.HOLD ? EventKind.fromType(stringOf(payload, "kind")) : byType;
    }

    /**
     * Formats the answered-elsewhere notice of a decision that another channel answered, or
     * {@code null} for an answer over the console's own channel: a console cannot tell its own
     * answer from another console's, and it never claims that nothing was sent from here about an
     * answer it may have sent itself.
     *
     * @param locale the interface locale
     * @param event  the event data
     * @return the notice, or {@code null}
     */
    private static @Nullable String answeredElsewhere(String locale, Map<String, Object> event) {
        var channel = stringOf(event, "channel");
        if (channel == null || channel.isBlank() || CHANNEL_CLI.equalsIgnoreCase(channel)) {
            return null;
        }
        return formatAnsweredElsewhere(locale,
                            TerminalSanitizer.sanitize(textOr(event, "id", "Q-???")),
                            channelText(locale, channel),
                            stringOf(event, "device"),
                            stringOf(event, "answer"));
    }

    private static String channelText(String locale, String channel) {
        var key = "operator.console.channel." + channel;
        return MessageCatalogue.hasKey(locale, key)
                            ? MessageCatalogue.format(locale, key, Map.of())
                            : TerminalSanitizer.sanitize(channel);
    }

    /**
     * Formats the question notice of a decision: the envelope the runtime pushes nests it under
     * {@code decision}, the legacy data carries the fields at the top; the summary is the shared
     * {@code decision.blocks.<what>} label and the recommendation names its option's label.
     *
     * @param locale the interface locale
     * @param event  the event data
     * @return the question notice
     */
    private static String questionNotice(String locale, Map<String, Object> event) {
        var source = decisionSource(event);
        return formatQuestion(locale,
                            TerminalSanitizer.sanitize(textOr(source, "id", textOr(event, "id", "Q-???"))),
                            TerminalSanitizer.sanitize(firstText(source, event, SCOPE, "subject", "plan")),
                            summaryOf(locale, source),
                            firstText(source, event, "description", "text", null),
                            recommendationOf(source));
    }

    /**
     * Formats the waiver notice: a waiver request shares the decision envelope (and the
     * {@code decision.opened} event) with a question, and names its gate instead of a recommendation.
     *
     * @param locale the interface locale
     * @param event  the event data
     * @return the waiver notice
     */
    private static String waiverNotice(String locale, Map<String, Object> event) {
        var source = decisionSource(event);
        return formatWaiverRequest(locale,
                            TerminalSanitizer.sanitize(textOr(source, "id", textOr(event, "id", "WR-?"))),
                            TerminalSanitizer.sanitize(firstText(source, event, SCOPE, "subject", "plan")),
                            firstText(source, event, "waiver_gate", "gate", null));
    }

    /**
     * @param event the event data
     * @return the decision envelope the event nests, or the data itself when it is the envelope
     */
    private static Map<String, Object> decisionSource(Map<String, Object> event) {
        var decision = JsonTree.object(event, "decision");
        if (decision == null) {
            decision = JsonTree.object(event, "question");
        }
        return decision != null ? decision : event;
    }

    /**
     * @param event the event data, or the decision envelope
     * @return {@code true} when the runtime opened a waiver request rather than a question
     */
    private static boolean isWaiver(Map<String, Object> event) {
        var source = decisionSource(event);
        return Boolean.TRUE.equals(JsonTree.bool(source, "is_waiver"))
                            || "waiver".equals(stringOf(source, "kind"))
                            || stringOf(source, "waiver_gate") != null
                            || textOr(source, "id", "").startsWith("WR-");
    }

    private static @Nullable String summaryOf(String locale, Map<String, Object> source) {
        var blocks = stringOf(source, "blocks");
        if (blocks != null) {
            var key = "decision.blocks." + blocks;
            return MessageCatalogue.hasKey(locale, key) ? MessageCatalogue.format(locale, key, Map.of()) : blocks;
        }
        return firstText(source, source, "summary", "title", null);
    }

    private static @Nullable String recommendationOf(Map<String, Object> source) {
        var parts = JsonTree.asArray(source.get("parts"));
        if (parts != null && !parts.isEmpty()) {
            var part = JsonTree.asObject(parts.get(0));
            if (part != null) {
                var key = stringOf(part, "recommendation");
                var label = key != null ? optionLabel(part, key) : null;
                if (label != null) {
                    return key + ") " + label;
                }
            }
        }
        return stringOf(source, "recommendation");
    }

    private static @Nullable String optionLabel(Map<String, Object> part, String key) {
        var options = JsonTree.asArray(part.get("options"));
        if (options == null) {
            return null;
        }
        for (var option : options) {
            var map = JsonTree.asObject(option);
            if (map != null && key.equals(stringOf(map, "key"))) {
                return stringOf(map, "label");
            }
        }
        return null;
    }

    /**
     * @param source the map to read first
     * @param event  the map to read when {@code source} carries no value
     * @param first  the first key to try
     * @param second the second key to try
     * @param absent the value to answer when neither map carries either key
     * @return the first present text
     */
    private static @Nullable String firstText(Map<String, Object> source, Map<String, Object> event,
                        String first, String second, @Nullable String absent) {
        var value = stringOf(source, first);
        if (value == null) {
            value = stringOf(source, second);
        }
        if (value == null && source != event) {
            value = firstText(event, event, first, second, absent);
        }
        return value != null ? value : absent;
    }

    private static String textOr(Map<String, Object> map, String key, String absent) {
        var value = stringOf(map, key);
        return value != null ? value : absent;
    }

    /**
     * @param event the event frame, its payload, or a decision envelope
     * @return the scope a notice names: the subject of a hold, the subject of a decision, the plan,
     *         epic, or scope it carries, or the scope the runtime's envelope declares
     */
    static String scopeOf(Map<String, Object> event) {
        var payload = payloadOf(event);
        var candidates = List.of(event, payload, decisionSource(payload), decisionSource(event));
        for (var candidate : candidates) {
            var found = firstScopeString(candidate);
            if (found != null) {
                return found;
            }
        }
        var declared = JsonTree.object(event, "scope");
        if (declared != null) {
            var id = stringOf(declared, "id");
            if (id != null && !id.isBlank()) {
                return id;
            }
            var projectId = stringOf(declared, "project_id");
            if (projectId != null && !projectId.isBlank()) {
                return projectId;
            }
        }
        var project = stringOf(event, KEY_PROJECT);
        return project != null && !project.isBlank() ? project : "";
    }

    private static @Nullable String firstScopeString(Map<String, Object> map) {
        for (var key : List.of(SCOPE, "subject_id", "subject", "epic", "plan", "plan_id", "epic_id")) {
            var value = stringOf(map, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * @param event the event data
     * @return the detail a notice names: what the hold holds it for, else the reason it carries
     */
    private static String detailOf(Map<String, Object> event) {
        for (var key : List.of(DETAIL, REASON, "cause")) {
            var value = stringOf(event, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /**
     * @param event the event data
     * @return the report the event carries, or the data itself when it is the report
     */
    private static Map<String, Object> reportOf(Map<String, Object> event) {
        var report = JsonTree.object(event, "report");
        return report != null ? report : event;
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
        if (event.containsKey(COUNT) && event.get(COUNT) instanceof Number num && num.intValue() > 1) {
            var scope = String.valueOf(event.getOrDefault(SCOPE, event.getOrDefault(KEY_PROJECT, "plan-marshall")));
            return QUIET + MessageCatalogue.format(locale, "operator.console.notice.reports_many",
                                Map.of(COUNT, num.intValue(), SCOPE, TerminalSanitizer.sanitize(scope)));
        }
        var id = String.valueOf(event.getOrDefault("id", ""));
        var subject = stringOf(event, SUBJECT);
        var leaf = extractLeaf(event);

        if (subject == null || subject.isBlank()) {
            subject = stringOf(event, "summary");
        }
        if (subject == null || subject.isBlank()) {
            subject = MessageCatalogue.format(locale, "operator.console.notice.report_new", Map.of());
        }
        var safeLeaf = TerminalSanitizer.sanitize(leaf);
        var safeSubject = TerminalSanitizer.sanitize(subject);
        if (id.isBlank()) {
            return QUIET + MessageCatalogue.format(locale, "operator.console.notice.report_line",
                                Map.of("leaf", safeLeaf, SUBJECT, safeSubject));
        }
        return QUIET + MessageCatalogue.format(locale, "operator.console.notice.report_line_open",
                            Map.of("leaf", safeLeaf, SUBJECT, safeSubject, "id", TerminalSanitizer.sanitize(id)));
    }

    private static @Nullable String stringOf(Map<String, Object> event, String key) {
        var value = event.get(key);
        return value instanceof String text ? text : null;
    }

    private static String extractLeaf(Map<String, Object> event) {
        var from = stringOf(event, "from");
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
