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
 * The body of {@code POST …/chat/proposal/accept} and {@code …/chat/proposal/reject}
 * ({@code 01-processes-and-transport.adoc#chat-routes}, The Proposal and the End): it names the
 * proposal the operator saw by the proposal's own {@code turn_id}.
 * <p>
 * The runtime refuses with {@code conflict} an answer that names a replaced proposal, an accept of a
 * proposal made against an older instance version of its plan, and an accept of a plan's revision
 * while another revision of that plan is already queued; in the last two the proposal stays visible
 * and its reject is taken. Each answer is answered with the chat as {@code GET} reads it
 * ({@link ChatResource}). The stop of a reply ({@code POST …/chat/turns/{turn_id}/stop}) and the end of
 * the chat ({@code POST …/chat/end}) carry no body.
 *
 * @param turnId the turn id of the proposal answered
 */
public record ProposalAnswerRequest(String turnId) {

    /** The wire name of the proposal's turn id. */
    public static final String FIELD_TURN_ID = "turn_id";

    private static final String TYPE = "ProposalAnswerRequest";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_TURN_ID);

    /**
     * @param turnId the proposal's turn id, not blank
     */
    public ProposalAnswerRequest {
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
     * @return the parsed request
     * @throws IOException if a member is unknown or missing
     */
    public static ProposalAnswerRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        return ChatJson.construct(TYPE, () -> new ProposalAnswerRequest(turnId));
    }

    /**
     * @param json the JSON object string
     * @return the parsed request
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ProposalAnswerRequest fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
