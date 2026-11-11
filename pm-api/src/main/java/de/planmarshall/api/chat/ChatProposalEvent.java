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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.event.EventName;
import de.planmarshall.api.json.JsonTree;

/**
 * The payload of {@code chat.proposal} ({@code 06-event-catalogue.adoc#chat-events}): a proposal of a
 * chat was made or changed its state ({@code 01-processes-and-transport.adoc#chat-proposal-states}).
 * <p>
 * A {@link ProposalState#REFUSED refused} proposal carries the finding of the check that failed; no
 * other state carries one. A plan's revision {@code accepted} is the plan's {@code queued_revision}
 * until it is {@code applied}, {@code refused} or {@code lapsed}, each announced by
 * {@code plan.revision_queued} ({@link PlanRevisionQueuedEvent}).
 *
 * @param chatId      the chat's id
 * @param turnId      the proposal's own turn id
 * @param replyTurnId the turn id of the reply that proposed it
 * @param kind        the kind of change
 * @param state       the proposal's state
 * @param finding     the finding of a refused proposal, present exactly when the state is
 *                    {@code refused}
 */
public record ChatProposalEvent(
        String chatId,
        String turnId,
        String replyTurnId,
        ProposalKind kind,
        ProposalState state,
        @Nullable MessageText finding
) {

    /** The event's name in the catalogue. */
    public static final String EVENT_NAME = EventName.CHAT_PROPOSAL.wire();

    /** The wire name of the chat's id. */
    public static final String FIELD_CHAT_ID = "chat_id";
    /** The wire name of the proposal's turn id. */
    public static final String FIELD_TURN_ID = "turn_id";
    /** The wire name of the reply's turn id. */
    public static final String FIELD_REPLY_TURN_ID = "reply_turn_id";
    /** The wire name of the proposal's kind. */
    public static final String FIELD_KIND = "kind";
    /** The wire name of the state. */
    public static final String FIELD_STATE = "state";
    /** The wire name of a refused proposal's finding. */
    public static final String FIELD_FINDING = "finding";

    private static final String TYPE = "ChatProposalEvent";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_CHAT_ID, FIELD_TURN_ID, FIELD_REPLY_TURN_ID,
            FIELD_KIND, FIELD_STATE, FIELD_FINDING);

    /**
     * @param chatId      the chat's id, not blank
     * @param turnId      the proposal's turn id, not blank
     * @param replyTurnId the reply's turn id, not blank
     * @param kind        the kind
     * @param state       the state
     * @param finding     the finding, required for {@code refused} and refused otherwise
     */
    public ChatProposalEvent {
        Objects.requireNonNull(chatId, FIELD_CHAT_ID);
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        Objects.requireNonNull(replyTurnId, FIELD_REPLY_TURN_ID);
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(state, FIELD_STATE);
        if (chatId.isBlank() || turnId.isBlank() || replyTurnId.isBlank()) {
            throw new IllegalArgumentException("chat_id, turn_id and reply_turn_id must not be blank");
        }
        if ((state == ProposalState.REFUSED) != (finding != null)) {
            throw new IllegalArgumentException("a finding is required exactly for a refused proposal");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CHAT_ID, chatId);
        map.put(FIELD_TURN_ID, turnId);
        map.put(FIELD_REPLY_TURN_ID, replyTurnId);
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_STATE, state.wireValue());
        if (finding != null) {
            map.put(FIELD_FINDING, finding.toMap());
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
     * @throws IOException if a member is unknown, missing, malformed, or refused by the state
     */
    public static ChatProposalEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var chatId = ChatJson.requireText(map, FIELD_CHAT_ID, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        var replyTurnId = ChatJson.requireText(map, FIELD_REPLY_TURN_ID, TYPE);
        var kind = ChatJson.requireEnum(map, FIELD_KIND, ProposalKind::fromWireOrNull, TYPE);
        var state = ChatJson.requireEnum(map, FIELD_STATE, ProposalState::fromWireOrNull, TYPE);
        var finding = ChatJson.optionalRecord(map, FIELD_FINDING, MessageText::fromMap, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatProposalEvent(chatId, turnId, replyTurnId, kind, state,
                finding));
    }

    /**
     * @param json the JSON object string
     * @return the parsed payload
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatProposalEvent fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
