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

import de.planmarshall.api.json.JsonTree;

/**
 * A pointer to one turn of a chat, {@code {chat_id, turn_id}}: the form of a decision record's
 * {@code after_chat} (the chat after which the decision was taken) and of a re-planning record's
 * {@code chat} block (the chat that led to the re-planning).
 *
 * @param chatId the chat's id
 * @param turnId the turn's id
 */
public record ChatTurnPointer(String chatId, String turnId) {

    /** The wire name of the chat's id. */
    public static final String FIELD_CHAT_ID = "chat_id";
    /** The wire name of the turn's id. */
    public static final String FIELD_TURN_ID = "turn_id";

    private static final String TYPE = "ChatTurnPointer";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_CHAT_ID, FIELD_TURN_ID);

    /**
     * @param chatId the chat's id, not blank
     * @param turnId the turn's id, not blank
     */
    public ChatTurnPointer {
        Objects.requireNonNull(chatId, FIELD_CHAT_ID);
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
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
     * @return the parsed pointer
     * @throws IOException if a member is unknown, missing, or blank
     */
    public static ChatTurnPointer fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var chatId = ChatJson.requireText(map, FIELD_CHAT_ID, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatTurnPointer(chatId, turnId));
    }

    /**
     * @param json the JSON object string
     * @return the parsed pointer
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatTurnPointer fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
