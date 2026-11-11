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
 * The payload of {@code chat.turn.progress} ({@code 06-event-catalogue.adoc#chat-events}): a step of the
 * role's work on a reply, which the runtime derives from the worker's activity (the target read, the
 * check run), as a message key with its arguments, never the model's own words.
 *
 * @param chatId the chat's id
 * @param turnId the turn of the reply
 * @param step   the step, a {@code control.chat.step.*} key with its arguments
 */
public record ChatTurnProgressEvent(String chatId, String turnId, MessageText step) {

    /** The event's name in the catalogue. */
    public static final String EVENT_NAME = EventName.CHAT_TURN_PROGRESS.wire();

    /** The wire name of the chat's id. */
    public static final String FIELD_CHAT_ID = "chat_id";
    /** The wire name of the turn's id. */
    public static final String FIELD_TURN_ID = "turn_id";
    /** The wire name of the step. */
    public static final String FIELD_STEP = "step";

    private static final String TYPE = "ChatTurnProgressEvent";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_CHAT_ID, FIELD_TURN_ID, FIELD_STEP);

    /**
     * @param chatId the chat's id, not blank
     * @param turnId the turn's id, not blank
     * @param step   the step
     */
    public ChatTurnProgressEvent {
        Objects.requireNonNull(chatId, FIELD_CHAT_ID);
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        Objects.requireNonNull(step, FIELD_STEP);
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
        map.put(FIELD_STEP, step.toMap());
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
    public static ChatTurnProgressEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var chatId = ChatJson.requireText(map, FIELD_CHAT_ID, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        var step = ChatJson.requireRecord(map, FIELD_STEP, MessageText::fromMap, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatTurnProgressEvent(chatId, turnId, step));
    }

    /**
     * @param json the JSON object string
     * @return the parsed payload
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatTurnProgressEvent fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
