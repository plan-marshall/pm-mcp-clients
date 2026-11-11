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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.planmarshall.api.event.EventName;
import de.planmarshall.api.json.JsonTree;

/**
 * The payload of {@code chat.turn.delta} ({@code 06-event-catalogue.adoc#chat-events}): the next piece
 * of a reply being written, appended in the order of the stream.
 *
 * @param chatId the chat's id
 * @param turnId the turn of the reply
 * @param text   the next piece of the reply's text
 */
public record ChatTurnDeltaEvent(String chatId, String turnId, String text) {

    /** The event's name in the catalogue. */
    public static final String EVENT_NAME = EventName.CHAT_TURN_DELTA.wire();

    /** The wire name of the chat's id. */
    public static final String FIELD_CHAT_ID = "chat_id";
    /** The wire name of the turn's id. */
    public static final String FIELD_TURN_ID = "turn_id";
    /** The wire name of the piece of text. */
    public static final String FIELD_TEXT = "text";

    private static final String TYPE = "ChatTurnDeltaEvent";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_CHAT_ID, FIELD_TURN_ID, FIELD_TEXT);

    /**
     * @param chatId the chat's id, not blank
     * @param turnId the turn's id, not blank
     * @param text   the piece of text
     */
    public ChatTurnDeltaEvent {
        Objects.requireNonNull(chatId, FIELD_CHAT_ID);
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        Objects.requireNonNull(text, FIELD_TEXT);
        if (chatId.isBlank() || turnId.isBlank()) {
            throw new IllegalArgumentException("chat_id and turn_id must not be blank");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CHAT_ID, chatId);
        map.put(FIELD_TURN_ID, turnId);
        map.put(FIELD_TEXT, text);
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
     * @throws IOException if a member is unknown, missing, or malformed
     */
    public static ChatTurnDeltaEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var chatId = ChatJson.requireText(map, FIELD_CHAT_ID, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        var text = ChatJson.requireString(map, FIELD_TEXT, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatTurnDeltaEvent(chatId, turnId, text));
    }

    /**
     * @param json the JSON object string
     * @return the parsed payload
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatTurnDeltaEvent fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
