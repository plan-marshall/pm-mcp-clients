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
 * The payload of {@code chat.turn.started} ({@code 06-event-catalogue.adoc#chat-events}): a turn of a
 * chat started, the operator's message or a role's reply.
 * <p>
 * The operator's message carries its whole {@code text} and is announced by this event alone; a role's
 * turn carries the chat's {@code context} as refreshed for this turn and the {@code model} it runs on,
 * and its text follows as {@code chat.turn.delta}. The kind of a started turn is always
 * {@link TurnKind#MESSAGE}: a proposal is announced by {@code chat.proposal}, a notice by
 * {@code chat.turn.completed}.
 *
 * @param chatId    the chat's id
 * @param turnId    the turn's id
 * @param kind      the turn's kind, always {@code message}
 * @param author    {@code operator}, or the role that writes the reply
 * @param format    the format of the turn's text
 * @param untrusted whether the turn came from outside the machine
 * @param model     the model of a role's turn, {@code null} for the operator's (the one rule of an
 *                  operator's turn, kept by {@link ChatTurn})
 * @param context   the chat's context of a role's turn, {@code null} for the operator's
 * @param text      the operator's message, {@code null} for a role's turn
 */
public record ChatTurnStartedEvent(
        String chatId,
        String turnId,
        TurnKind kind,
        String author,
        TurnFormat format,
        boolean untrusted,
        @Nullable String model,
        @Nullable ChatContext context,
        @Nullable String text
) {

    /** The event's name in the catalogue. */
    public static final String EVENT_NAME = EventName.CHAT_TURN_STARTED.wire();

    /** The wire name of the chat's id. */
    public static final String FIELD_CHAT_ID = "chat_id";
    /** The wire name of the turn's id. */
    public static final String FIELD_TURN_ID = "turn_id";
    /** The wire name of the turn's kind. */
    public static final String FIELD_KIND = "kind";
    /** The wire name of the author. */
    public static final String FIELD_AUTHOR = "author";
    /** The wire name of the format. */
    public static final String FIELD_FORMAT = "format";
    /** The wire name of the untrusted mark. */
    public static final String FIELD_UNTRUSTED = "untrusted";
    /** The wire name of the model. */
    public static final String FIELD_MODEL = "model";
    /** The wire name of the context. */
    public static final String FIELD_CONTEXT = "context";
    /** The wire name of the operator's text. */
    public static final String FIELD_TEXT = "text";

    private static final String TYPE = "ChatTurnStartedEvent";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_CHAT_ID, FIELD_TURN_ID, FIELD_KIND,
            FIELD_AUTHOR, FIELD_FORMAT, FIELD_UNTRUSTED, FIELD_MODEL, FIELD_CONTEXT, FIELD_TEXT);

    /**
     * @param chatId    the chat's id, not blank
     * @param turnId    the turn's id, not blank
     * @param kind      the kind, {@code message}
     * @param author    the author, not blank
     * @param format    the format
     * @param untrusted the untrusted mark
     * @param model     the model, refused on the operator's turn
     * @param context   the context, required on a role's turn and refused on the operator's
     * @param text      the text, required on the operator's turn and refused on a role's
     * @throws IllegalArgumentException if a member is blank, the kind is not {@code message}, or a member
     *                                  is missing or present against the author, or an operator's or
     *                                  untrusted turn is not {@code plain}
     */
    public ChatTurnStartedEvent {
        Objects.requireNonNull(chatId, FIELD_CHAT_ID);
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(author, FIELD_AUTHOR);
        Objects.requireNonNull(format, FIELD_FORMAT);
        if (chatId.isBlank() || turnId.isBlank() || author.isBlank()) {
            throw new IllegalArgumentException("chat_id, turn_id and author must not be blank");
        }
        if (kind != TurnKind.MESSAGE) {
            throw new IllegalArgumentException("a started turn is a message, not " + kind.wireValue());
        }
        var operator = ChatTurn.AUTHOR_OPERATOR.equals(author);
        ChatTurn.requireOperatorTurnRule(author, model, List.of());
        if (operator && (text == null || context != null)) {
            throw new IllegalArgumentException("the operator's message carries its text and no context");
        }
        if (!operator && (text != null || context == null)) {
            throw new IllegalArgumentException("a role's turn carries the chat's context and no text");
        }
        if (format != TurnFormat.PLAIN && (operator || untrusted)) {
            throw new IllegalArgumentException("an operator's turn and an untrusted turn are always plain");
        }
    }

    /**
     * @return {@code true} if this is the operator's own message
     */
    public boolean isOperator() {
        return ChatTurn.AUTHOR_OPERATOR.equals(author);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CHAT_ID, chatId);
        map.put(FIELD_TURN_ID, turnId);
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_AUTHOR, author);
        map.put(FIELD_FORMAT, format.wireValue());
        map.put(FIELD_UNTRUSTED, untrusted);
        if (model != null) {
            map.put(FIELD_MODEL, model);
        }
        if (context != null) {
            map.put(FIELD_CONTEXT, context.toMap());
        }
        if (text != null) {
            map.put(FIELD_TEXT, text);
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
     * @throws IOException if a member is unknown, missing, malformed, or refused by the author
     */
    public static ChatTurnStartedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var chatId = ChatJson.requireText(map, FIELD_CHAT_ID, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        var kind = ChatJson.requireEnum(map, FIELD_KIND, TurnKind::fromWireOrNull, TYPE);
        var author = ChatJson.requireText(map, FIELD_AUTHOR, TYPE);
        var format = ChatJson.requireEnum(map, FIELD_FORMAT, TurnFormat::fromWireOrNull, TYPE);
        var untrusted = ChatJson.requireBoolean(map, FIELD_UNTRUSTED, TYPE);
        var model = ChatJson.optionalText(map, FIELD_MODEL, TYPE);
        var context = ChatJson.optionalRecord(map, FIELD_CONTEXT, ChatContext::fromMap, TYPE);
        var text = ChatJson.optionalText(map, FIELD_TEXT, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatTurnStartedEvent(chatId, turnId, kind, author, format,
                untrusted, model, context, text));
    }

    /**
     * @param json the JSON object string
     * @return the parsed payload
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatTurnStartedEvent fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
