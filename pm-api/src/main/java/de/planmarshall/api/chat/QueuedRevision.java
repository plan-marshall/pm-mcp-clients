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
 * A plan's {@code queued_revision}, {@code {chat_id, turn_id, subject: {kind, id}, chat_open, applies}}
 * ({@code state-catalogue.adoc#chat}, Apply Paths): while a revision accepted in a chat waits for the
 * plan's next task boundary, the plan carries the chat that proposed it, that chat's subject (the plan,
 * or a decision of it), whether that chat is still open (derived on read, never stored), and the point
 * it waits for as a message key ({@code chat.applies.after_task {task_id}} or
 * {@code chat.applies.on_start}).
 *
 * @param chatId   the id of the chat that proposed the revision
 * @param turnId   the turn id of the accepted proposal
 * @param subject  the subject of that chat, a plan or a decision
 * @param chatOpen whether that chat is still open
 * @param applies  the point the revision waits for
 */
public record QueuedRevision(String chatId, String turnId, ChatSubject subject, boolean chatOpen,
                             MessageText applies) {

    /** The wire name of the chat's id. */
    public static final String FIELD_CHAT_ID = "chat_id";
    /** The wire name of the proposal's turn id. */
    public static final String FIELD_TURN_ID = "turn_id";
    /** The wire name of the chat's subject. */
    public static final String FIELD_SUBJECT = "subject";
    /** The wire name of whether the chat is open. */
    public static final String FIELD_CHAT_OPEN = "chat_open";
    /** The wire name of the point it waits for. */
    public static final String FIELD_APPLIES = "applies";

    private static final String TYPE = "QueuedRevision";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_CHAT_ID, FIELD_TURN_ID, FIELD_SUBJECT,
            FIELD_CHAT_OPEN, FIELD_APPLIES);

    /**
     * @param chatId   the chat's id, not blank
     * @param turnId   the turn id, not blank
     * @param subject  the subject, of the kind {@code plan} or {@code decision}
     * @param chatOpen whether the chat is open
     * @param applies  the point it waits for
     */
    public QueuedRevision {
        Objects.requireNonNull(chatId, FIELD_CHAT_ID);
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        Objects.requireNonNull(subject, FIELD_SUBJECT);
        Objects.requireNonNull(applies, FIELD_APPLIES);
        if (chatId.isBlank() || turnId.isBlank()) {
            throw new IllegalArgumentException("chat_id and turn_id must not be blank");
        }
        if (subject.kind() != ChatSubjectKind.PLAN && subject.kind() != ChatSubjectKind.DECISION) {
            throw new IllegalArgumentException(
                    "a queued revision's chat is about a plan or a decision, not " + subject.kind().wireValue());
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CHAT_ID, chatId);
        map.put(FIELD_TURN_ID, turnId);
        map.put(FIELD_SUBJECT, subject.toMap());
        map.put(FIELD_CHAT_OPEN, chatOpen);
        map.put(FIELD_APPLIES, applies.toMap());
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
     * @return the parsed queued revision
     * @throws IOException if a member is unknown, missing, or malformed
     */
    public static QueuedRevision fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var chatId = ChatJson.requireText(map, FIELD_CHAT_ID, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        var subject = ChatJson.requireRecord(map, FIELD_SUBJECT, ChatSubject::fromMap, TYPE);
        var chatOpen = ChatJson.requireBoolean(map, FIELD_CHAT_OPEN, TYPE);
        var applies = ChatJson.requireRecord(map, FIELD_APPLIES, MessageText::fromMap, TYPE);
        return ChatJson.construct(TYPE, () -> new QueuedRevision(chatId, turnId, subject, chatOpen, applies));
    }

    /**
     * @param json the JSON object string
     * @return the parsed queued revision
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static QueuedRevision fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
