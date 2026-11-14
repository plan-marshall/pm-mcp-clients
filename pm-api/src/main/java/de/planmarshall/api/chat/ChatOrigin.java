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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The origin of a change a chat led to, {@code {kind: chat, chat_id, turn_id}}: the chat and the turn
 * of the accepted proposal that made it.
 * <p>
 * The origin travels on the record of the change: a staged plan row a chat changed
 * ({@code StagedPlanRecord.origin}), a change of the decomposition's draft, an item's disposition or
 * rewrite, a stored description, and the plan resource a chat's revision made a new version of.
 *
 * @param chatId the chat's id
 * @param turnId the turn id of the accepted proposal
 */
public record ChatOrigin(String chatId, String turnId) {

    /** The kind every chat origin carries on the wire. */
    public static final String KIND = "chat";

    /** The wire name of the origin's kind. */
    public static final String FIELD_KIND = "kind";
    /** The wire name of the chat's id. */
    public static final String FIELD_CHAT_ID = "chat_id";
    /** The wire name of the turn's id. */
    public static final String FIELD_TURN_ID = "turn_id";

    private static final String TYPE = "ChatOrigin";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_KIND, FIELD_CHAT_ID, FIELD_TURN_ID);

    /**
     * @param chatId the chat's id, not blank
     * @param turnId the turn's id, not blank
     */
    public ChatOrigin {
        Objects.requireNonNull(chatId, FIELD_CHAT_ID);
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        if (chatId.isBlank() || turnId.isBlank()) {
            throw new IllegalArgumentException("chat_id and turn_id must not be blank");
        }
    }

    /**
     * @return the map representation, its {@code kind} always {@code chat}
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, KIND);
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
     * @return the parsed origin
     * @throws IOException if a member is unknown, missing, or blank, or the kind is not {@code chat}
     */
    public static ChatOrigin fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var kind = ChatJson.requireText(map, FIELD_KIND, TYPE);
        if (!KIND.equals(kind)) {
            throw new JsonFormatException("Unknown kind in " + TYPE + ": " + kind);
        }
        var chatId = ChatJson.requireText(map, FIELD_CHAT_ID, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatOrigin(chatId, turnId));
    }

    /**
     * @param json the JSON object string
     * @return the parsed origin
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatOrigin fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
