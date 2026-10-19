/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.control.ChatProposalResource;
import de.planmarshall.api.control.ChatRole;
import de.planmarshall.api.control.ChatScope;
import de.planmarshall.api.control.ChatState;
import de.planmarshall.api.control.ChatTurnResource;
import de.planmarshall.api.id.DottedIdentifier;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The start chat of an entry as the client shows it (State Catalogue, Chatting with the Planning
 * Role; design <em>Starting Work</em>, chatting about the request).
 * <p>
 * The chat is the one dialogue of both clients: its scope is the entry ({@link ChatScope#REQUEST}),
 * its role is {@code refine} for a plan's entry and {@code decompose} for an epic's, and it is
 * available once at least one source is stored and every source from outside the machine is
 * screened. It is disabled with its reason while no configuration of the role qualifies, and it is
 * absent for a recipe. Starting never waits for the chat: <em>Continue</em> stays as it is while a
 * chat is open, and leaving the entry keeps the chat with it.
 * <p>
 * Its one change is a description: a rewrite of one description of the entry or an addition to it,
 * read back old against new and stored through {@code start.store-description}. A start chat never
 * changes, adds, or removes another source.
 *
 * @param entryId            the entry the chat is about
 * @param role               the routed role ({@code refine} or {@code decompose})
 * @param scope              the scope of the chat ({@code request})
 * @param available          whether the chat may be opened now
 * @param disabledReasonKey  the message key of the reason it may not, or {@code null}
 * @param chatId             the id of the opened chat, or {@code null} while none is open
 * @param state              the state of the opened chat, or {@code null}
 * @param turns              the transcript, oldest first
 * @param proposal           the one proposed change, or {@code null}
 * @param transcript         where the transcript stands
 * @param transcriptPath     the transcript's location relative to the entry or the created scope,
 *                           or {@code null} before a chat was opened
 */
public record StartChatResource(
        String entryId,
        ChatRole role,
        ChatScope scope,
        boolean available,
        @Nullable String disabledReasonKey,
        @Nullable String chatId,
        @Nullable ChatState state,
        List<ChatTurnResource> turns,
        @Nullable ChatProposalResource proposal,
        StartTranscriptState transcript,
        @Nullable String transcriptPath
) {

    public static final String FIELD_ENTRY_ID = "entry_id";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_SCOPE = "scope";
    public static final String FIELD_AVAILABLE = "available";
    public static final String FIELD_DISABLED_REASON_KEY = "disabled_reason_key";
    public static final String FIELD_CHAT_ID = "chat_id";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_TURNS = "turns";
    public static final String FIELD_PROPOSAL = "proposal";
    public static final String FIELD_TRANSCRIPT = "transcript";
    public static final String FIELD_TRANSCRIPT_PATH = "transcript_path";

    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_ENTRY_ID, FIELD_ROLE, FIELD_SCOPE, FIELD_AVAILABLE, FIELD_DISABLED_REASON_KEY,
            FIELD_CHAT_ID, FIELD_STATE, FIELD_TURNS, FIELD_PROPOSAL, FIELD_TRANSCRIPT,
            FIELD_TRANSCRIPT_PATH);

    public StartChatResource {
        Objects.requireNonNull(entryId, FIELD_ENTRY_ID);
        Objects.requireNonNull(role, FIELD_ROLE);
        Objects.requireNonNull(scope, FIELD_SCOPE);
        transcript = transcript == null ? StartTranscriptState.WITH_ENTRY : transcript;
        turns = turns == null ? List.of() : List.copyOf(turns);
        if (available && disabledReasonKey != null) {
            throw new IllegalArgumentException("An available chat names no reason it is disabled");
        }
        if (!available && disabledReasonKey == null) {
            throw new IllegalArgumentException("A disabled chat names its reason");
        }
        if (chatId == null && (state != null || proposal != null || !turns.isEmpty())) {
            throw new IllegalArgumentException("A chat that was never opened carries no state, turns, or proposal");
        }
        if (disabledReasonKey != null && !DottedIdentifier.messageKey(disabledReasonKey)) {
            throw new IllegalArgumentException("Invalid disabled reason key: " + disabledReasonKey);
        }
    }

    /**
     * @param entryId     the entry
     * @param role        the routed role
     * @param available   whether the chat may be opened
     * @param reasonKey   the reason it may not, or {@code null}
     * @return the chat link of an entry
     */
    public static StartChatResource link(String entryId, ChatRole role, boolean available,
                                         @Nullable String reasonKey) {
        return new StartChatResource(entryId, role, ChatScope.REQUEST, available, reasonKey, null, null,
                List.of(), null, StartTranscriptState.WITH_ENTRY, null);
    }

    /**
     * @return {@code true} if the routed role is the one an entry of the kind goes to
     */
    public boolean routedByKind(EntryKind kind) {
        return kind.epic() ? role == ChatRole.DECOMPOSE : role == ChatRole.REFINE;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ENTRY_ID, entryId);
        map.put(FIELD_ROLE, role.value());
        map.put(FIELD_SCOPE, scope.value());
        map.put(FIELD_AVAILABLE, available);
        if (disabledReasonKey != null) {
            map.put(FIELD_DISABLED_REASON_KEY, disabledReasonKey);
        }
        if (chatId != null) {
            map.put(FIELD_CHAT_ID, chatId);
        }
        if (state != null) {
            map.put(FIELD_STATE, state.wireValue());
        }
        map.put(FIELD_TURNS, turns.stream().map(ChatTurnResource::toMap).toList());
        if (proposal != null) {
            map.put(FIELD_PROPOSAL, proposal.toMap());
        }
        map.put(FIELD_TRANSCRIPT, transcript.wireValue());
        if (transcriptPath != null) {
            map.put(FIELD_TRANSCRIPT_PATH, transcriptPath);
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
     * Writes this chat to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed chat
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static StartChatResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "StartChatResource");
        var entryId = JsonTree.string(map, FIELD_ENTRY_ID);
        if (entryId == null) {
            throw new JsonFormatException("Missing required field for StartChatResource: entry_id");
        }
        var role = ChatRole.fromValue(JsonTree.string(map, FIELD_ROLE));
        if (role == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_ROLE + " in StartChatResource: " + JsonTree.string(map, FIELD_ROLE));
        }
        var scope = ChatScope.fromValue(JsonTree.string(map, FIELD_SCOPE));
        if (scope == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_SCOPE + " in StartChatResource: " + JsonTree.string(map, FIELD_SCOPE));
        }
        var stateWire = JsonTree.string(map, FIELD_STATE);
        ChatState state = null;
        if (stateWire != null) {
            try {
                state = ChatState.fromWire(stateWire);
            } catch (IllegalArgumentException e) {
                throw new JsonFormatException("Unknown " + FIELD_STATE + " in StartChatResource: " + stateWire, e);
            }
        }
        var transcriptWire = JsonTree.string(map, FIELD_TRANSCRIPT);
        var transcript = transcriptWire != null ? StartTranscriptState.fromWireOrNull(transcriptWire) : null;
        if (transcriptWire != null && transcript == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_TRANSCRIPT + " in StartChatResource: " + transcriptWire);
        }
        var proposalMap = JsonTree.objectOrNull(map, FIELD_PROPOSAL, "StartChatResource");
        try {
            return new StartChatResource(entryId, role, scope, StartJson.bool(map, FIELD_AVAILABLE),
                    JsonTree.string(map, FIELD_DISABLED_REASON_KEY), JsonTree.string(map, FIELD_CHAT_ID), state,
                    readTurns(map), proposalMap != null ? ChatProposalResource.fromMap(proposalMap) : null,
                    transcript, JsonTree.string(map, FIELD_TRANSCRIPT_PATH));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid StartChatResource: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed chat
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static StartChatResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for StartChatResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one chat from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed chat
     * @throws IOException on format failure
     */
    public static StartChatResource readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for StartChatResource");
        }
        return fromMap(map);
    }

    private static List<ChatTurnResource> readTurns(Map<String, Object> map) throws IOException {
        var turns = new ArrayList<ChatTurnResource>();
        for (var object : StartJson.readObjects(map, FIELD_TURNS)) {
            turns.add(ChatTurnResource.fromMap(object));
        }
        return turns;
    }
}
