/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.report;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatTurnPointer;
import de.planmarshall.api.json.JsonTree;

/**
 * Record of a decision asked and answered during plan execution.
 *
 * @param id        identifier of the decision (e.g. {@code "Q-003"})
 * @param question  question summary or title
 * @param answer    chosen or recorded answer
 * @param by        how decision was resolved ({@code "operator"}, {@code "delegated"}, {@code "quorum"}, {@code "withdrawn"})
 * @param channel   operator channel ({@code "web"} or {@code "cli"})
 * @param device    device label/id
 * @param afterChat the chat after which the decision was taken ({@code after_chat}), or {@code null} when it
 *                  was taken without one
 */
public record DecisionRecord(
        String id,
        @Nullable String question,
        @Nullable String answer,
        String by,
        @Nullable String channel,
        @Nullable String device,
        @Nullable ChatTurnPointer afterChat
) {
    private static final String TYPE = "DecisionRecord";
    private static final String FIELD_ID = "id";
    private static final String FIELD_QUESTION = "question";
    private static final String FIELD_ANSWER = "answer";
    private static final String FIELD_BY = "by";
    private static final String FIELD_CHANNEL = "channel";
    private static final String FIELD_DEVICE = "device";
    private static final String FIELD_AFTER_CHAT = "after_chat";

    public DecisionRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(by, "by");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        if (question != null) map.put(FIELD_QUESTION, question);
        if (answer != null) map.put(FIELD_ANSWER, answer);
        map.put(FIELD_BY, by);
        if (channel != null) map.put(FIELD_CHANNEL, channel);
        if (device != null) map.put(FIELD_DEVICE, device);
        if (afterChat != null) map.put(FIELD_AFTER_CHAT, afterChat.toMap());
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed decision record
     * @throws IOException if {@code after_chat} is present and malformed
     */
    public static DecisionRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, FIELD_ID);
        if (id == null) id = "";
        var question = JsonTree.string(map, FIELD_QUESTION);
        if (question == null) question = JsonTree.string(map, "text");
        var answer = JsonTree.string(map, FIELD_ANSWER);
        var by = JsonTree.string(map, FIELD_BY);
        if (by == null) by = "operator";
        var channel = JsonTree.string(map, FIELD_CHANNEL);
        var device = JsonTree.string(map, FIELD_DEVICE);
        var afterChatMap = JsonTree.objectOrNull(map, FIELD_AFTER_CHAT, TYPE);
        var afterChat = afterChatMap != null ? ChatTurnPointer.fromMap(afterChatMap) : null;
        return new DecisionRecord(id, question, answer, by, channel, device, afterChat);
    }
}
