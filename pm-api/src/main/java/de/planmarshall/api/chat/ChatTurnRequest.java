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
 * The body of {@code POST …/chat/turns}, the operator's message
 * ({@code 01-processes-and-transport.adoc#chat-routes}, A Message): its {@code text} of 1 to 8000
 * characters, never interpreted, and the client's own {@code client_turn_id}.
 * <p>
 * The message is answered {@code 202} ({@link ChatTurnAccepted}) as soon as it is recorded, before the
 * role answers. A repeat with the same {@code client_turn_id}, after an answer that was lost, is
 * answered with the same turn and records nothing again, so a client's <em>Try again</em> never sends
 * twice. While a reply is written no message is accepted ({@code act_not_available}).
 *
 * @param text         the operator's message, 1 to 8000 characters
 * @param clientTurnId the client's own id of this message
 */
public record ChatTurnRequest(String text, String clientTurnId) {

    /** The fewest characters a message has. */
    public static final int MIN_TEXT_LENGTH = 1;
    /** The most characters a message has. */
    public static final int MAX_TEXT_LENGTH = 8000;

    /** The wire name of the message. */
    public static final String FIELD_TEXT = "text";
    /** The wire name of the client's own id. */
    public static final String FIELD_CLIENT_TURN_ID = "client_turn_id";

    private static final String TYPE = "ChatTurnRequest";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_TEXT, FIELD_CLIENT_TURN_ID);

    /**
     * @param text         the message, 1 to 8000 characters (counted as code points)
     * @param clientTurnId the client's own id, not blank
     * @throws IllegalArgumentException if the text is outside its bounds or the id is blank
     */
    public ChatTurnRequest {
        Objects.requireNonNull(text, FIELD_TEXT);
        Objects.requireNonNull(clientTurnId, FIELD_CLIENT_TURN_ID);
        var length = text.codePointCount(0, text.length());
        if (length < MIN_TEXT_LENGTH || length > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "text must have " + MIN_TEXT_LENGTH + " to " + MAX_TEXT_LENGTH + " characters, has " + length);
        }
        if (clientTurnId.isBlank()) {
            throw new IllegalArgumentException("client_turn_id must not be blank");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_TEXT, text);
        map.put(FIELD_CLIENT_TURN_ID, clientTurnId);
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
     * @return the parsed request
     * @throws IOException if a member is unknown or missing, or the text is outside its bounds
     */
    public static ChatTurnRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var text = ChatJson.requireString(map, FIELD_TEXT, TYPE);
        var clientTurnId = ChatJson.requireText(map, FIELD_CLIENT_TURN_ID, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatTurnRequest(text, clientTurnId));
    }

    /**
     * @param json the JSON object string
     * @return the parsed request
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatTurnRequest fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
