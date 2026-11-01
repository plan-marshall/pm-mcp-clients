/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A chat with the planning role as the control contract shows it (design <em>Chatting with the
 * Planning Role</em>): its subject, scope, routed role, state, transcript, and the one proposal.
 *
 * <p>The chat is one dialogue wherever it is opened; a plan's chat is a sub-workflow call within
 * the phase it is called in, and the chat about ended work is read-only and never changes the
 * archive. The transcript is kept with the subject, among its events.
 *
 * @param chatId      the chat's identifier
 * @param subjectId   the plan, epic, decomposition, or register the chat is about
 * @param subjectKind {@code plan}, {@code epic}, {@code decomposition}, or {@code review}
 * @param role        the routed role ({@link ChatRole})
 * @param scope       the scope ({@link ChatScope})
 * @param state       the state ({@link ChatState})
 * @param turns       the transcript, oldest first
 * @param proposal    the one proposed change, or {@code null}
 */
public record ChatReadBackResource(
        String chatId,
        String subjectId,
        String subjectKind,
        String role,
        String scope,
        String state,
        List<ChatTurnResource> turns,
        @Nullable ChatProposalResource proposal
) {

    public ChatReadBackResource {
        Objects.requireNonNull(chatId, "chatId");
        Objects.requireNonNull(subjectId, "subjectId");
        subjectKind = subjectKind != null && !subjectKind.isBlank() ? subjectKind : "plan";
        role = role != null && !role.isBlank() ? role : ChatRole.REPLAN.value();
        scope = scope != null && !scope.isBlank() ? scope : ChatScope.PLAN.value();
        state = state != null && !state.isBlank() ? state : ChatState.OPEN.wireValue();
        turns = turns != null ? List.copyOf(turns) : List.of();
    }

    /**
     * @return the routed role as a closed enum
     */
    public ChatRole chatRole() {
        return ChatRole.parse(role);
    }

    /**
     * @return the scope as a closed enum
     */
    public ChatScope chatScope() {
        return ChatScope.parse(scope);
    }

    /**
     * @return the state as a closed enum
     */
    public ChatState chatState() {
        return ChatState.fromWire(state);
    }

    /**
     * @return {@code true} if a proposal is open and awaits accept or reject
     */
    public boolean hasProposal() {
        return proposal != null;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("chat_id", chatId);
        map.put("subject_id", subjectId);
        map.put("subject_kind", subjectKind);
        map.put("role", role);
        map.put("scope", scope);
        map.put("state", state);
        map.put("turns", turns.stream().map(ChatTurnResource::toMap).toList());
        if (proposal != null) {
            map.put("proposal", proposal.toMap());
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
     * @throws IOException if a required field is missing
     */
    public static ChatReadBackResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var chatId = JsonTree.string(map, "chat_id");
        var subjectId = JsonTree.string(map, "subject_id");
        if (chatId == null || subjectId == null) {
            throw new JsonFormatException("Missing required fields for ChatReadBackResource: chat_id, subject_id");
        }
        var proposal = JsonTree.asObject(map.get("proposal"));
        return new ChatReadBackResource(chatId, subjectId, JsonTree.string(map, "subject_kind"),
                JsonTree.string(map, "role"), JsonTree.string(map, "scope"), JsonTree.string(map, "state"),
                readTurns(map),
                proposal != null ? ChatProposalResource.fromMap(proposal) : null);
    }

    /**
     * @param json the JSON object string
     * @return the parsed chat
     * @throws IOException if the value is not an object
     */
    public static ChatReadBackResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ChatReadBackResource");
        }
        return fromMap(obj);
    }

    private static List<ChatTurnResource> readTurns(Map<String, Object> map) throws IOException {
        var turns = new ArrayList<ChatTurnResource>();
        for (var object : ControlJson.readObjects(map, "turns")) {
            turns.add(ChatTurnResource.fromMap(object));
        }
        return turns;
    }
}
