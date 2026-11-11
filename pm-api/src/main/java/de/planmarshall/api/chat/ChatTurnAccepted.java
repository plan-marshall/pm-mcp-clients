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
 * The {@code 202} answer of {@code POST …/chat/turns}
 * ({@code 01-processes-and-transport.adoc#chat-routes}, A Message): the operator's message is recorded
 * as a turn of its own and its {@code turn_id} is answered before the role answers; the reply reaches
 * the client as the {@code chat.*} events of the subject's stream.
 *
 * @param turnId the id of the recorded message's turn
 */
public record ChatTurnAccepted(String turnId) {

    /** The HTTP status of the answer. */
    public static final int HTTP_STATUS = 202;

    /** The wire name of the turn's id. */
    public static final String FIELD_TURN_ID = "turn_id";

    private static final String TYPE = "ChatTurnAccepted";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_TURN_ID);

    /**
     * @param turnId the turn's id, not blank
     */
    public ChatTurnAccepted {
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        if (turnId.isBlank()) {
            throw new IllegalArgumentException("turn_id must not be blank");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
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
     * @return the parsed answer
     * @throws IOException if a member is unknown or missing
     */
    public static ChatTurnAccepted fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatTurnAccepted(turnId));
    }

    /**
     * @param json the JSON object string
     * @return the parsed answer
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatTurnAccepted fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
