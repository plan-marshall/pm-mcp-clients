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

import de.planmarshall.api.display.Display;
import de.planmarshall.api.event.EventKind;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.NeedsOperatorKind;

/**
 * Formats notice blocks printed above the prompt (via {@code LineReader.printAbove(...)}) when
 * runtime events arrive or asynchronous notifications are displayed (04-console.adoc#pushed).
 *
 * <p>Every text is a key of the message catalogue (area {@code operator}, {@code
 * operator.console.*}), and every value taken from the runtime is put through {@link
 * TerminalSanitizer} before it is composed, so a crafted value cannot move the cursor, write the
 * clipboard, set the window title, or fake a prompt. A value that fills a cell of a one-line
 * template is reduced to a single line; the one untrusted multi-line value, the body of a question,
 * is printed behind the notice's indent one line at a time, so no line of it starts at the
 * beginning of a terminal line.
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
    private static final String ID = "id";
    private static final String LABEL = "label";
    private static final String CHANNEL = "channel";
    private static final String KEY_PROJECT = "project";
    private static final String DETAIL = "detail";
    private static final String EPIC = "epic";

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
                            Map.of(ID, id, SCOPE, scope)));
        if (summary != null && !summary.isBlank()) {
            sb.append(" \u00b7 ").append(TerminalSanitizer.sanitizeLine(summary));
        }
        sb.append('\n');
        if (text != null && !text.isBlank()) {
            appendBody(sb, locale, text);
        }
        if (recommendation != null && !recommendation.isBlank()) {
            sb.append(MessageCatalogue.format(locale, "operator.console.question.recommended",
                                Map.of(OPTION, TerminalSanitizer.sanitizeLine(recommendation)))).append('\n');
        }
        sb.append(MessageCatalogue.format(locale, "operator.console.question.hint", Map.of(ID, id)));
        return sb.toString();
    }

    /**
     * Appends an untrusted multi-line body behind the notice's indent, one line at a time: every
     * line of the sanitized text, the first included, goes through the catalogue's body template,
     * so no line of it starts at the beginning of a terminal line and none can imitate a prompt
     * (04-console.adoc#output-safety). The line structure of a real multi-line text is kept.
     *
     * @param sb     the notice being composed
     * @param locale the interface locale
     * @param text   the raw body text
     */
    private static void appendBody(StringBuilder sb, String locale, String text) {
        for (var line : TerminalSanitizer.sanitize(text).split("\n")) {
            sb.append(MessageCatalogue.format(locale, "operator.console.question.body",
                                Map.of("text", line))).append('\n');
        }
    }

    /**
     * Formats a waiver request notice block in English.
     *
     * @param id    the waiver request id (e.g. "WR-2")
     * @param scope the plan scope (e.g. "plan fix-relay")
     * @param gate  the target gate (e.g. "integration-tests")
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
     * @param gate   the target gate (e.g. "integration-tests")
     * @return multi-line formatted notice
     */
    public static String formatWaiverRequest(String locale, String id, String scope, @Nullable String gate) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, SCOPE);
        var sb = new StringBuilder(BULLET);
        if (gate != null && !gate.isBlank()) {
            sb.append(MessageCatalogue.format(locale, "operator.console.notice.waiver",
                                Map.of(ID, id, SCOPE, scope, "gate", Display.gate(locale, TerminalSanitizer.sanitizeLine(gate)))));
        } else {
            sb.append(MessageCatalogue.format(locale, "operator.console.notice.waiver_plain",
                                Map.of(ID, id, SCOPE, scope)));
        }
        sb.append('\n');
        sb.append(MessageCatalogue.format(locale, "operator.console.question.hint", Map.of(ID, id)));
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
        var safeScope = TerminalSanitizer.sanitizeLine(scope);
        var safeDetail = TerminalSanitizer.sanitizeLine(detail);
        var text = switch (kind) {
            case "operator_wait" -> MessageCatalogue.format(locale, "operator.console.notice.operator_wait",
                                Map.of("name", safeScope, "what", safeDetail));
            case "integrity_pause" -> MessageCatalogue.format(locale, "operator.console.notice.integrity_pause",
                                Map.of("plan", safeScope, REASON, safeDetail));
            case "stalled" -> MessageCatalogue.format(locale, "operator.console.notice.stalled",
                                Map.of("plan", safeScope, REASON, safeDetail));
            case CONDITION -> MessageCatalogue.format(locale, "operator.console.notice.condition",
                                Map.of("plan", safeScope, CONDITION, safeDetail));
            case "held" -> MessageCatalogue.format(locale, "operator.console.notice.held",
                                Map.of("epic", safeScope, DETAIL, safeDetail));
            case "landing_failed" -> MessageCatalogue.format(locale, "operator.console.notice.landing_failed",
                                Map.of("epic", safeScope, REASON, safeDetail));
            case "findings" -> MessageCatalogue.format(locale, "operator.console.notice.findings",
                                Map.of("epic", safeScope, DETAIL, safeDetail));
            case "setup" -> MessageCatalogue.format(locale, "operator.console.notice.setup",
                                Map.of(KEY_PROJECT, safeScope, "what", safeDetail));
            case "credential" -> MessageCatalogue.format(locale, "operator.console.notice.credential",
                                Map.of("key", credentialKey(locale, safeScope, safeDetail)));
            case "cleanup_due" -> MessageCatalogue.format(locale, "operator.console.notice.cleanup_due",
                                Map.of());
            default -> MessageCatalogue.format(locale, "operator.console.notice.generic",
                                Map.of("kind", otherKindLabel(locale), SCOPE, safeScope, DETAIL, safeDetail));
        };
        return BULLET + text;
    }

    /**
     * The word a notice names a kind by that has no notice of its own: {@code other}, the client's
     * fallback for a reason this version does not know. The catalogue's text, never the wire word,
     * which a newer runtime chose and no operator reads.
     *
     * @param locale the interface locale
     * @return the catalogue's text of the fallback kind
     */
    private static String otherKindLabel(String locale) {
        return MessageCatalogue.format(locale, "overview.needs." + NeedsOperatorKind.OTHER.value(), Map.of());
    }

    /**
     * @param locale     the interface locale
     * @param scope      the sanitized scope, the key where it names one
     * @param safeDetail the sanitized detail
     * @return the credential's key, or the catalogue's own word for one the event does not name
     */
    private static String credentialKey(String locale, String scope, String safeDetail) {
        if (!scope.isBlank()) {
            return scope;
        }
        if (!safeDetail.isBlank()) {
            return safeDetail;
        }
        return MessageCatalogue.format(locale, "common.unknown", Map.of());
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
                            Map.of(LABEL, TerminalSanitizer.sanitizeLine(label)));
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
        // The catalogue's line carries its own check mark.
        return MessageCatalogue.format(locale, "operator.console.new_code",
                            Map.of("code", code, "time", validUntil))
                            + "\n" + MessageCatalogue.format(locale, "operator.console.url_indent",
                            Map.of("url", TerminalSanitizer.sanitizeLine(url)));
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
        var safeAnswer = TerminalSanitizer.sanitizeLine(answer);
        if (device != null && !device.isBlank()) {
            return DASH + MessageCatalogue.format(locale, "operator.console.question.answered_elsewhere",
                                Map.of(ID, id, CHANNEL, channel, "device", TerminalSanitizer.sanitizeLine(device),
                                                    OPTION, safeAnswer));
        }
        return DASH + MessageCatalogue.format(locale, "operator.console.question.answered_elsewhere_plain",
                            Map.of(ID, id, CHANNEL, channel, OPTION, safeAnswer));
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
            case WAIVER -> waiverNotice(locale, payload);
            case ANSWERED_ELSEWHERE -> answeredElsewhere(locale, payload);
            case PAIRED -> formatPairedNotice(locale, pairedLabel(locale, payload));
            case FRESH_CODE -> freshCodeNotice(locale, payload);
            case OPERATOR_WAIT -> formatOperatorWait(locale, scopeOf(event), detailOf(payload),
                                isEpicScope(event, payload));
            case INTEGRITY_PAUSE, STALLED, CONDITION, HELD, LANDING_FAILED, FINDINGS, SETUP, CREDENTIAL, CLEANUP_DUE ->
                formatHoldNotice(locale, kind.notice(), scopeOf(event), detailOf(payload));
            case REPORT -> formatReportNotice(locale, reportOf(payload));
            case HOLD -> null;
        };
    }

    /**
     * @param locale  the interface locale
     * @param payload the event data
     * @return the label the paired device carries, or the catalogue's own word when it names none
     */
    private static String pairedLabel(String locale, Map<String, Object> payload) {
        var label = stringOf(payload, LABEL);
        return label != null && !label.isBlank() ? label : MessageCatalogue.format(locale, "common.unknown", Map.of());
    }

    /**
     * @param locale  the interface locale
     * @param payload the event data
     * @return the fresh code's notice, or {@code null} when the event names no code: a code the
     *         event does not carry is no code to show
     */
    private static @Nullable String freshCodeNotice(String locale, Map<String, Object> payload) {
        var code = stringOf(payload, "code");
        if (code == null || code.isBlank()) {
            return null;
        }
        return formatFreshCodeNotice(locale, TerminalSanitizer.sanitize(code),
                            TerminalSanitizer.sanitize(textOr(payload, "valid_until", "")),
                            textOr(payload, "url", ""));
    }

    /**
     * Formats the notice of an operator wait, which names its level: an epic's wait reads
     * {@code Epic <id> waits for you}, a plan's {@code Plan <id> waits for you} (design <em>Console</em>,
     * {@code operator.console.notice.operator_wait}).
     *
     * @param locale the interface locale
     * @param scope  the plan or epic that waits
     * @param detail what it waits for
     * @param epic   whether the wait is an epic's
     * @return the notice line
     */
    static String formatOperatorWait(String locale, String scope, @Nullable String detail, boolean epic) {
        return BULLET + MessageCatalogue.format(locale, epic ? "operator.console.notice.operator_wait.epic"
                                                : "operator.console.notice.operator_wait",
                            Map.of("name", TerminalSanitizer.sanitizeLine(scope), "what", TerminalSanitizer.sanitize(detail)));
    }

    /**
     * @return {@code true} when the event's subject is an epic: the hold names it as its
     *         {@code subject_kind}, or the runtime's envelope declares an epic scope
     */
    private static boolean isEpicScope(Map<String, Object> event, Map<String, Object> payload) {
        if (EPIC.equals(stringOf(payload, "subject_kind"))) {
            return true;
        }
        var declared = JsonTree.object(event, SCOPE);
        return declared != null && EPIC.equals(stringOf(declared, "kind"));
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
            return byName == EventKind.QUESTION && isWaiver(payload) ? EventKind.WAIVER : byName;
        }
        var byType = EventKind.fromType(textOr(payload, "type", ""));
        if (byType == EventKind.QUESTION && isWaiver(payload)) {
            return EventKind.WAIVER;
        }
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
        var channel = stringOf(event, CHANNEL);
        if (channel == null || channel.isBlank() || CHANNEL_CLI.equalsIgnoreCase(channel)) {
            return null;
        }
        return formatAnsweredElsewhere(locale,
                            TerminalSanitizer.sanitizeLine(textOr(event, ID, "Q-???")),
                            channelText(locale, channel),
                            stringOf(event, "device"),
                            stringOf(event, "answer"));
    }

    private static String channelText(String locale, String channel) {
        var key = "operator.console.channel." + channel;
        return MessageCatalogue.hasKey(locale, key)
                            ? MessageCatalogue.format(locale, key, Map.of())
                            : TerminalSanitizer.sanitizeLine(channel);
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
                            TerminalSanitizer.sanitizeLine(textOr(source, ID, textOr(event, ID, "Q-???"))),
                            TerminalSanitizer.sanitizeLine(firstText(source, event, SCOPE, SUBJECT, "plan")),
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
                            TerminalSanitizer.sanitizeLine(textOr(source, ID, textOr(event, ID, "WR-?"))),
                            TerminalSanitizer.sanitizeLine(firstText(source, event, SCOPE, SUBJECT, "plan")),
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
                            || textOr(source, ID, "").startsWith("WR-");
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
                return stringOf(map, LABEL);
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
        var declared = JsonTree.object(event, SCOPE);
        if (declared != null) {
            var id = stringOf(declared, ID);
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
        for (var key : List.of(SCOPE, "subject_id", SUBJECT, "epic", "plan", "plan_id", "epic_id", "credential_key")) {
            var value = stringOf(map, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        var link = stringOf(map, "link");
        if (link != null && link.startsWith("/api/v1/credentials/")) {
            return link.substring("/api/v1/credentials/".length());
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
                                Map.of(COUNT, num.intValue(), SCOPE, TerminalSanitizer.sanitizeLine(scope)));
        }
        var id = String.valueOf(event.getOrDefault(ID, ""));
        var subject = stringOf(event, SUBJECT);
        var leaf = extractLeaf(event);

        if (subject == null || subject.isBlank()) {
            subject = stringOf(event, "summary");
        }
        if (subject == null || subject.isBlank()) {
            subject = MessageCatalogue.format(locale, "operator.console.notice.report_new", Map.of());
        }
        var safeLeaf = TerminalSanitizer.sanitizeLine(leaf);
        var safeSubject = TerminalSanitizer.sanitizeLine(subject);
        if (id.isBlank()) {
            return QUIET + MessageCatalogue.format(locale, "operator.console.notice.report_line",
                                Map.of("leaf", safeLeaf, SUBJECT, safeSubject));
        }
        return QUIET + MessageCatalogue.format(locale, "operator.console.notice.report_line_open",
                            Map.of("leaf", safeLeaf, SUBJECT, safeSubject, ID, TerminalSanitizer.sanitizeLine(id)));
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
