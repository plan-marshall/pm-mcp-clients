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

import de.planmarshall.api.json.JsonTree;

/**
 * The change a chat's role proposes, read back old against new before it applies
 * ({@code 01-processes-and-transport.adoc#chat-routes}, {@code chat.read-back}).
 * <p>
 * A proposal is a turn of its own after the reply that made it; accept and reject name it by its
 * {@code turn_id}. A {@link ProposalState#REFUSED refused} proposal carries the finding of the check
 * that failed; no other state carries one.
 *
 * @param turnId      the proposal's own turn id
 * @param replyTurnId the turn id of the reply that proposed it
 * @param kind        the kind of change
 * @param summary     the change in one sentence
 * @param applies     when the change applies, as a message key, or {@code null}
 * @param before      the text before the change, or {@code null}
 * @param after       the text after the change, or {@code null}
 * @param state       the proposal's state
 * @param finding     the finding of a refused proposal, present exactly when the state is
 *                    {@code refused}
 */
public record ChatProposal(
        String turnId,
        String replyTurnId,
        ProposalKind kind,
        String summary,
        @Nullable MessageText applies,
        @Nullable String before,
        @Nullable String after,
        ProposalState state,
        @Nullable MessageText finding
) {

    /** The wire name of the proposal's turn id. */
    public static final String FIELD_TURN_ID = "turn_id";
    /** The wire name of the reply's turn id. */
    public static final String FIELD_REPLY_TURN_ID = "reply_turn_id";
    /** The wire name of the proposal's kind. */
    public static final String FIELD_KIND = "kind";
    /** The wire name of the summary. */
    public static final String FIELD_SUMMARY = "summary";
    /** The wire name of when it applies. */
    public static final String FIELD_APPLIES = "applies";
    /** The wire name of the text before. */
    public static final String FIELD_BEFORE = "before";
    /** The wire name of the text after. */
    public static final String FIELD_AFTER = "after";
    /** The wire name of the state. */
    public static final String FIELD_STATE = "state";
    /** The wire name of a refused proposal's finding. */
    public static final String FIELD_FINDING = "finding";

    private static final String TYPE = "ChatProposal";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_TURN_ID, FIELD_REPLY_TURN_ID, FIELD_KIND,
            FIELD_SUMMARY, FIELD_APPLIES, FIELD_BEFORE, FIELD_AFTER, FIELD_STATE, FIELD_FINDING);

    /**
     * @param turnId      the proposal's turn id, not blank
     * @param replyTurnId the reply's turn id, not blank
     * @param kind        the kind
     * @param summary     the summary, not blank
     * @param applies     when it applies, or {@code null}
     * @param before      the text before, or {@code null}
     * @param after       the text after, or {@code null}
     * @param state       the state
     * @param finding     the finding, required for {@code refused} and refused otherwise
     */
    public ChatProposal {
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        Objects.requireNonNull(replyTurnId, FIELD_REPLY_TURN_ID);
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(summary, FIELD_SUMMARY);
        Objects.requireNonNull(state, FIELD_STATE);
        if (turnId.isBlank() || replyTurnId.isBlank() || summary.isBlank()) {
            throw new IllegalArgumentException("turn_id, reply_turn_id and summary must not be blank");
        }
        if ((state == ProposalState.REFUSED) != (finding != null)) {
            throw new IllegalArgumentException("a finding is required exactly for a refused proposal");
        }
    }

    /**
     * @return {@code true} while the proposal waits for accept or reject
     */
    public boolean isOpen() {
        return state == ProposalState.OPEN;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_TURN_ID, turnId);
        map.put(FIELD_REPLY_TURN_ID, replyTurnId);
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_SUMMARY, summary);
        if (applies != null) {
            map.put(FIELD_APPLIES, applies.toMap());
        }
        if (before != null) {
            map.put(FIELD_BEFORE, before);
        }
        if (after != null) {
            map.put(FIELD_AFTER, after);
        }
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
     * @return the parsed proposal
     * @throws IOException if a member is unknown, missing, malformed, or refused by the state
     */
    public static ChatProposal fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        var replyTurnId = ChatJson.requireText(map, FIELD_REPLY_TURN_ID, TYPE);
        var kind = ChatJson.requireEnum(map, FIELD_KIND, ProposalKind::fromWireOrNull, TYPE);
        var summary = ChatJson.requireText(map, FIELD_SUMMARY, TYPE);
        var applies = ChatJson.optionalRecord(map, FIELD_APPLIES, MessageText::fromMap, TYPE);
        var before = ChatJson.optionalText(map, FIELD_BEFORE, TYPE);
        var after = ChatJson.optionalText(map, FIELD_AFTER, TYPE);
        var state = ChatJson.requireEnum(map, FIELD_STATE, ProposalState::fromWireOrNull, TYPE);
        var finding = ChatJson.optionalRecord(map, FIELD_FINDING, MessageText::fromMap, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatProposal(turnId, replyTurnId, kind, summary, applies,
                before, after, state, finding));
    }

    /**
     * @param json the JSON object string
     * @return the parsed proposal
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatProposal fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
