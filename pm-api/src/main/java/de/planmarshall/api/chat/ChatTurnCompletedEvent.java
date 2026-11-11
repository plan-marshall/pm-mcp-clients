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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.event.EventName;
import de.planmarshall.api.json.JsonTree;

/**
 * The payload of {@code chat.turn.completed} ({@code 06-event-catalogue.adoc#chat-events}): a reply
 * completed, was stopped or failed, or the runtime wrote a notice.
 * <p>
 * The payload carries the turn's whole {@code text} and the {@code refs} it names. A {@code failed}
 * turn carries its {@code reason} ({@code infeasible}, {@code lost}, {@code too_long}: the reply exceeded
 * the chat reply bound) as a message key with its arguments; no other status carries one. A notice is
 * announced by this event alone, and a notice about the chat's subject carries the narrative
 * {@code lines} it reports; no message carries lines.
 *
 * @param chatId    the chat's id
 * @param turnId    the turn's id
 * @param kind      {@code message} or {@code notice}
 * @param status    {@code complete}, {@code stopped} or {@code failed}
 * @param untrusted whether the turn came from outside the machine
 * @param text      the turn's whole text, may be empty
 * @param refs      the references the turn names
 * @param reason    the reason of a failed turn, present exactly when the status is {@code failed}
 * @param lines     the narrative lines of a notice, empty for a message
 */
public record ChatTurnCompletedEvent(
        String chatId,
        String turnId,
        TurnKind kind,
        TurnStatus status,
        boolean untrusted,
        String text,
        List<TurnRef> refs,
        @Nullable MessageText reason,
        List<NarrativeLine> lines
) {

    /** The event's name in the catalogue. */
    public static final String EVENT_NAME = EventName.CHAT_TURN_COMPLETED.wire();

    /** The wire name of the chat's id. */
    public static final String FIELD_CHAT_ID = "chat_id";
    /** The wire name of the turn's id. */
    public static final String FIELD_TURN_ID = "turn_id";
    /** The wire name of the turn's kind. */
    public static final String FIELD_KIND = "kind";
    /** The wire name of the turn's status. */
    public static final String FIELD_STATUS = "status";
    /** The wire name of the untrusted mark. */
    public static final String FIELD_UNTRUSTED = "untrusted";
    /** The wire name of the text. */
    public static final String FIELD_TEXT = "text";
    /** The wire name of the references. */
    public static final String FIELD_REFS = "refs";
    /** The wire name of the failed turn's reason. */
    public static final String FIELD_REASON = "reason";
    /** The wire name of a notice's lines. */
    public static final String FIELD_LINES = "lines";

    private static final String TYPE = "ChatTurnCompletedEvent";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_CHAT_ID, FIELD_TURN_ID, FIELD_KIND,
            FIELD_STATUS, FIELD_UNTRUSTED, FIELD_TEXT, FIELD_REFS, FIELD_REASON, FIELD_LINES);

    /**
     * @param chatId    the chat's id, not blank
     * @param turnId    the turn's id, not blank
     * @param kind      {@code message} or {@code notice}
     * @param status    {@code complete}, {@code stopped} or {@code failed}
     * @param untrusted the untrusted mark
     * @param text      the text, {@code null} read as empty
     * @param refs      the references, {@code null} read as none
     * @param reason    the reason, required for {@code failed} and refused otherwise
     * @param lines     the lines, {@code null} read as none, refused for a message
     * @throws IllegalArgumentException if a member is blank, the kind or the status is not one a
     *                                  completed turn has, or the reason or the lines do not fit
     */
    public ChatTurnCompletedEvent {
        Objects.requireNonNull(chatId, FIELD_CHAT_ID);
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(status, FIELD_STATUS);
        if (chatId.isBlank() || turnId.isBlank()) {
            throw new IllegalArgumentException("chat_id and turn_id must not be blank");
        }
        if (kind == TurnKind.PROPOSAL) {
            throw new IllegalArgumentException("a completed turn is a message or a notice, not a proposal");
        }
        if (status == TurnStatus.STREAMING) {
            throw new IllegalArgumentException("a completed turn is not streaming");
        }
        text = text == null ? "" : text;
        refs = refs == null ? List.of() : List.copyOf(refs);
        lines = lines == null ? List.of() : List.copyOf(lines);
        if ((status == TurnStatus.FAILED) != (reason != null)) {
            throw new IllegalArgumentException("a reason is required exactly for a failed turn");
        }
        if (kind != TurnKind.NOTICE && !lines.isEmpty()) {
            throw new IllegalArgumentException("only a notice carries lines");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CHAT_ID, chatId);
        map.put(FIELD_TURN_ID, turnId);
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_STATUS, status.wireValue());
        map.put(FIELD_UNTRUSTED, untrusted);
        map.put(FIELD_TEXT, text);
        map.put(FIELD_REFS, refs.stream().map(TurnRef::toMap).toList());
        if (reason != null) {
            map.put(FIELD_REASON, reason.toMap());
        }
        if (!lines.isEmpty()) {
            map.put(FIELD_LINES, lines.stream().map(NarrativeLine::toMap).toList());
        }
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the input map
     * @return the parsed payload
     * @throws IOException if a member is unknown, missing, malformed, or refused by the kind or status
     */
    public static ChatTurnCompletedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var chatId = ChatJson.requireText(map, FIELD_CHAT_ID, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        var kind = ChatJson.requireEnum(map, FIELD_KIND, TurnKind::fromWireOrNull, TYPE);
        var status = ChatJson.requireEnum(map, FIELD_STATUS, TurnStatus::fromWireOrNull, TYPE);
        var untrusted = ChatJson.requireBoolean(map, FIELD_UNTRUSTED, TYPE);
        var text = ChatJson.requireString(map, FIELD_TEXT, TYPE);
        var refs = ChatJson.readRecords(map, FIELD_REFS, TurnRef::fromMap, TYPE);
        var reason = ChatJson.optionalRecord(map, FIELD_REASON, MessageText::fromMap, TYPE);
        var lines = ChatJson.readRecords(map, FIELD_LINES, NarrativeLine::fromMap, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatTurnCompletedEvent(chatId, turnId, kind, status, untrusted,
                text, refs, reason, lines));
    }

    /**
     * @param json the JSON object string
     * @return the parsed payload
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatTurnCompletedEvent fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
