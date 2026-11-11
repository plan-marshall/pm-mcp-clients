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

import de.planmarshall.api.json.JsonTree;

/**
 * The chat as {@code GET …/chat} reads it, and as {@code POST …/chat} (open), accept, reject and end
 * answer with it ({@code 01-processes-and-transport.adoc#chat-routes}): one chat for every subject, its
 * routed role with the model it runs on, its scope, its state, what the role knows, the open proposal,
 * and the newest page of its turns, oldest first.
 * <p>
 * A long transcript is read back a page at a time: {@code before} names the turn to pass as
 * {@code ?before=<turn_id>} for the page before this one, and is absent when this page holds the oldest
 * turn.
 *
 * @param id       the chat's id
 * @param subject  what the chat is about
 * @param role     the routed role with its model
 * @param scope    the chat's scope
 * @param state    the chat's state
 * @param context  what the role knows
 * @param proposal the open proposal, or {@code null}
 * @param turns    the page of turns, oldest first
 * @param before   the turn id of the page before, or {@code null} when there is none
 */
public record ChatResource(
        String id,
        ChatSubject subject,
        RoutedRole role,
        ChatScopeKind scope,
        ChatStatus state,
        ChatContext context,
        @Nullable ChatProposal proposal,
        List<ChatTurn> turns,
        @Nullable String before
) {

    /** The wire name of the chat's id. */
    public static final String FIELD_ID = "id";
    /** The wire name of the subject. */
    public static final String FIELD_SUBJECT = "subject";
    /** The wire name of the routed role. */
    public static final String FIELD_ROLE = "role";
    /** The wire name of the scope. */
    public static final String FIELD_SCOPE = "scope";
    /** The wire name of the state. */
    public static final String FIELD_STATE = "state";
    /** The wire name of the context. */
    public static final String FIELD_CONTEXT = "context";
    /** The wire name of the open proposal. */
    public static final String FIELD_PROPOSAL = "proposal";
    /** The wire name of the page of turns. */
    public static final String FIELD_TURNS = "turns";
    /** The wire name of the page before. */
    public static final String FIELD_BEFORE = "before";

    private static final String TYPE = "ChatResource";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_ID, FIELD_SUBJECT, FIELD_ROLE, FIELD_SCOPE,
            FIELD_STATE, FIELD_CONTEXT, FIELD_PROPOSAL, FIELD_TURNS, FIELD_BEFORE);

    /**
     * @param id       the id, not blank
     * @param subject  the subject
     * @param role     the routed role
     * @param scope    the scope
     * @param state    the state
     * @param context  the context
     * @param proposal the open proposal, or {@code null}
     * @param turns    the turns, {@code null} read as none
     * @param before   the page before, or {@code null}; never blank
     */
    public ChatResource {
        Objects.requireNonNull(id, FIELD_ID);
        Objects.requireNonNull(subject, FIELD_SUBJECT);
        Objects.requireNonNull(role, FIELD_ROLE);
        Objects.requireNonNull(scope, FIELD_SCOPE);
        Objects.requireNonNull(state, FIELD_STATE);
        Objects.requireNonNull(context, FIELD_CONTEXT);
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (before != null && before.isBlank()) {
            throw new IllegalArgumentException("before must not be blank");
        }
        turns = turns == null ? List.of() : List.copyOf(turns);
    }

    /**
     * @return {@code true} if a proposal is open and awaits accept or reject
     */
    public boolean hasProposal() {
        return proposal != null && proposal.isOpen();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        map.put(FIELD_SUBJECT, subject.toMap());
        map.put(FIELD_ROLE, role.toMap());
        map.put(FIELD_SCOPE, scope.wireValue());
        map.put(FIELD_STATE, state.wireValue());
        map.put(FIELD_CONTEXT, context.toMap());
        if (proposal != null) {
            map.put(FIELD_PROPOSAL, proposal.toMap());
        }
        map.put(FIELD_TURNS, turns.stream().map(ChatTurn::toMap).toList());
        if (before != null) {
            map.put(FIELD_BEFORE, before);
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
     * @return the parsed chat
     * @throws IOException if a member is unknown, missing, or malformed
     */
    public static ChatResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var id = ChatJson.requireText(map, FIELD_ID, TYPE);
        var subject = ChatJson.requireRecord(map, FIELD_SUBJECT, ChatSubject::fromMap, TYPE);
        var role = ChatJson.requireRecord(map, FIELD_ROLE, RoutedRole::fromMap, TYPE);
        var scope = ChatJson.requireEnum(map, FIELD_SCOPE, ChatScopeKind::fromWireOrNull, TYPE);
        var state = ChatJson.requireEnum(map, FIELD_STATE, ChatStatus::fromWireOrNull, TYPE);
        var context = ChatJson.requireRecord(map, FIELD_CONTEXT, ChatContext::fromMap, TYPE);
        var proposal = ChatJson.optionalRecord(map, FIELD_PROPOSAL, ChatProposal::fromMap, TYPE);
        var turns = ChatJson.readRecords(map, FIELD_TURNS, ChatTurn::fromMap, TYPE);
        var before = ChatJson.optionalText(map, FIELD_BEFORE, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatResource(id, subject, role, scope, state, context, proposal,
                turns, before));
    }

    /**
     * @param json the JSON object string
     * @return the parsed chat
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatResource fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
