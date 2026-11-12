/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The wire form of the chat's message keys and the rendering key each maps to
 * ({@code doc/design/shared/work-actions-chat-texts.adoc#texts}): the runtime sends a key of its own
 * namespace, a client renders it through a {@code control.chat.*} key of the {@code control} pack.
 * <ul>
 * <li>a narrative line {@code chat.line.<name>} renders as {@code control.chat.line.<name>}; a
 * {@code chat.line.decision_answered} whose arguments carry no {@code device} as
 * {@code control.chat.line.decision_answered_no_device};</li>
 * <li>an entry {@code chat.knows.<item>} of what the role knows renders as {@code control.chat.knows.item}
 * with that {@code item};</li>
 * <li>the point a queued revision waits for, {@code chat.applies.after_task} or {@code chat.applies.on_start},
 * renders as {@code control.chat.revision_queued} with {@code applies} {@code after_task} or
 * {@code on_start};</li>
 * <li>a step of a reply {@code chat.step.<read|search|check>} renders as {@code control.chat.step.<…>};</li>
 * <li>a failed turn's reason {@code chat.reason.<lost|too_long|infeasible>} renders as
 * {@code control.chat.lost}, {@code control.chat.too_long_reply} or {@code control.chat.infeasible};</li>
 * <li>a refused proposal's finding {@code chat.finding.<check_failed|changed_since|lapsed|already_queued>}
 * renders as {@code control.chat.<…>}.</li>
 * </ul>
 * A key outside these families, or a name the design table does not hold, is not mapped and is refused.
 */
public final class ChatMessageKeys {

    /** The argument of a {@code chat.line.decision_answered} that names the answering device. */
    public static final String ARG_DEVICE = "device";
    /** The argument of {@code control.chat.knows.item} that names the entry. */
    public static final String ARG_ITEM = "item";
    /** The argument of {@code control.chat.revision_queued} that names the point it waits for. */
    public static final String ARG_APPLIES = "applies";

    private static final String RUNTIME_PREFIX = "chat.";
    private static final String RENDERING_PREFIX = "control.chat.";

    private static final String LINE = "line";
    private static final String KNOWS = "knows";
    private static final String APPLIES = "applies";
    private static final String STEP = "step";
    private static final String REASON = "reason";
    private static final String FINDING = "finding";

    private static final String DECISION_ANSWERED = "decision_answered";
    private static final String DECISION_ANSWERED_NO_DEVICE = "control.chat.line.decision_answered_no_device";
    private static final String KNOWS_ITEM = "control.chat.knows.item";
    private static final String REVISION_QUEUED = "control.chat.revision_queued";

    private static final Set<String> LINES = Set.of("phase_changed", "task_ended", "decision_opened",
            DECISION_ANSWERED, "decision_closed", "hold_opened", "hold_cleared", "plan_pausing", "plan_paused",
            "plan_started", "version_changed", "pr_opened", "pr_merged", "pr_closed", "row_started", "row_landed",
            "row_failed", "auto_changed", "source_added", "source_removed", "source_flagged", "waiver_granted",
            "waiver_revoked", "replan_triggered", "register_added", "row_staged", "row_cancelled", "item_disposed");
    private static final Set<String> APPLIES_POINTS = Set.of("after_task", "on_start");
    private static final Set<String> STEPS = Set.of("read", "search", "check");
    private static final Map<String, String> REASONS = Map.of(
            "lost", "control.chat.lost",
            "too_long", "control.chat.too_long_reply",
            "infeasible", "control.chat.infeasible");
    private static final Set<String> FINDINGS = Set.of("check_failed", "changed_since", "lapsed", "already_queued");

    private ChatMessageKeys() {
        // Utility class
    }

    /**
     * @param key a runtime message key
     * @return {@code true} if the key is one of the mapped families with a name the design table holds
     */
    public static boolean maps(String key) {
        Objects.requireNonNull(key, "key");
        return split(key).filter(ChatMessageKeys::known).isPresent();
    }

    /**
     * @param text a runtime message key with its arguments
     * @return the rendering key with the arguments it is formatted with
     * @throws IllegalArgumentException if the key is not mapped
     */
    public static MessageText render(MessageText text) {
        Objects.requireNonNull(text, "text");
        var key = text.key();
        var parts = split(key).filter(ChatMessageKeys::known)
                .orElseThrow(() -> new IllegalArgumentException("Unmapped chat message key: " + key));
        var name = parts.name();
        var args = text.args();
        return switch (parts.family()) {
            case LINE -> DECISION_ANSWERED.equals(name) && !args.containsKey(ARG_DEVICE)
                    ? new MessageText(DECISION_ANSWERED_NO_DEVICE, args)
                    : new MessageText(RENDERING_PREFIX + LINE + "." + name, args);
            case KNOWS -> new MessageText(KNOWS_ITEM, with(args, ARG_ITEM, name));
            case APPLIES -> new MessageText(REVISION_QUEUED, with(args, ARG_APPLIES, name));
            case STEP -> new MessageText(RENDERING_PREFIX + STEP + "." + name, args);
            case REASON -> new MessageText(REASONS.get(name), args);
            default -> new MessageText(RENDERING_PREFIX + name, args);
        };
    }

    /** A runtime key {@code chat.<family>.<name>} taken apart. */
    private record Parts(String family, String name) {
    }

    /**
     * @return the family and the name, empty if the key has another shape
     */
    private static Optional<Parts> split(String key) {
        if (!key.startsWith(RUNTIME_PREFIX)) {
            return Optional.empty();
        }
        var rest = key.substring(RUNTIME_PREFIX.length());
        var dot = rest.indexOf('.');
        if (dot <= 0 || dot == rest.length() - 1 || rest.indexOf('.', dot + 1) >= 0) {
            return Optional.empty();
        }
        return Optional.of(new Parts(rest.substring(0, dot), rest.substring(dot + 1)));
    }

    private static boolean known(Parts parts) {
        var name = parts.name();
        return switch (parts.family()) {
            case LINE -> LINES.contains(name);
            case KNOWS -> true;
            case APPLIES -> APPLIES_POINTS.contains(name);
            case STEP -> STEPS.contains(name);
            case REASON -> REASONS.containsKey(name);
            case FINDING -> FINDINGS.contains(name);
            default -> false;
        };
    }

    private static Map<String, Object> with(Map<String, Object> args, String name, String value) {
        var copy = new LinkedHashMap<>(args);
        copy.put(name, value);
        return copy;
    }
}
