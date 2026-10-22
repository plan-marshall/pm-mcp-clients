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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.control.ChatProposalResource;
import de.planmarshall.api.control.ChatRole;
import de.planmarshall.api.control.ChatScope;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The one proposal of a chat about ended work: a follow-up plan (design <em>Actions on Plans and
 * Epics</em>, a chat about ended work; State Catalogue, Chatting with the Planning Role).
 * <p>
 * The archive never changes. The proposal is a description that names the ended plan or epic,
 * read back old against new; <em>Start a plan with it</em> opens the start of a new plan in the same
 * project with that description stored, and the operator goes on at {@code start.sources} as with
 * any start. The follow-up plan is the entry the accepted proposal opens
 * ({@code start-follow-up}).
 *
 * @param subjectId   the ended plan or epic the chat is about
 * @param subjectKind {@code plan} or {@code epic}
 * @param role        the routed role ({@code replan} for a plan, {@code decompose} for an epic)
 * @param scope       the scope of the chat ({@code archive})
 * @param available   whether the chat may be opened now
 * @param disabledReasonKey the message key of the reason it may not, or {@code null}
 * @param proposal    the one proposed follow-up description, or {@code null}
 */
public record FollowUpProposalResource(
        String subjectId,
        EntryKind subjectKind,
        ChatRole role,
        ChatScope scope,
        boolean available,
        @Nullable String disabledReasonKey,
        @Nullable ChatProposalResource proposal
) {

    public static final String FIELD_SUBJECT_ID = "subject_id";
    public static final String FIELD_SUBJECT_KIND = "subject_kind";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_SCOPE = "scope";
    public static final String FIELD_AVAILABLE = "available";
    public static final String FIELD_DISABLED_REASON_KEY = "disabled_reason_key";
    public static final String FIELD_PROPOSAL = "proposal";

    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_SUBJECT_ID, FIELD_SUBJECT_KIND, FIELD_ROLE, FIELD_SCOPE, FIELD_AVAILABLE,
            FIELD_DISABLED_REASON_KEY, FIELD_PROPOSAL);

    public FollowUpProposalResource {
        Objects.requireNonNull(subjectId, FIELD_SUBJECT_ID);
        Objects.requireNonNull(subjectKind, FIELD_SUBJECT_KIND);
        Objects.requireNonNull(role, FIELD_ROLE);
        Objects.requireNonNull(scope, FIELD_SCOPE);
        if (scope != ChatScope.ARCHIVE) {
            throw new IllegalArgumentException("A chat about ended work runs at the archive scope: " + scope.value());
        }
        if (role != (subjectKind.epic() ? ChatRole.DECOMPOSE : ChatRole.REPLAN)) {
            throw new IllegalArgumentException(
                    "Ended work goes to replan for a plan and decompose for an epic: " + role.value());
        }
        if (available && disabledReasonKey != null) {
            throw new IllegalArgumentException("An available chat names no reason it is disabled");
        }
        if (!available && disabledReasonKey == null) {
            throw new IllegalArgumentException("A disabled chat names its reason");
        }
    }

    /**
     * @param subjectId   the ended plan or epic
     * @param subjectKind its kind
     * @param proposal    the proposed follow-up description
     * @return the chat of ended work with its proposal
     */
    public static FollowUpProposalResource proposed(String subjectId, EntryKind subjectKind,
                                                    ChatProposalResource proposal) {
        return new FollowUpProposalResource(subjectId, subjectKind,
                subjectKind.epic() ? ChatRole.DECOMPOSE : ChatRole.REPLAN, ChatScope.ARCHIVE, true, null, proposal);
    }

    /**
     * @param subjectId   the ended plan or epic
     * @param subjectKind its kind
     * @param reasonKey   why the chat is disabled
     * @return the chat of ended work, disabled with its reason
     */
    public static FollowUpProposalResource disabled(String subjectId, EntryKind subjectKind, String reasonKey) {
        return new FollowUpProposalResource(subjectId, subjectKind,
                subjectKind.epic() ? ChatRole.DECOMPOSE : ChatRole.REPLAN, ChatScope.ARCHIVE, false, reasonKey, null);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_SUBJECT_ID, subjectId);
        map.put(FIELD_SUBJECT_KIND, subjectKind.wireValue());
        map.put(FIELD_ROLE, role.value());
        map.put(FIELD_SCOPE, scope.value());
        map.put(FIELD_AVAILABLE, available);
        if (disabledReasonKey != null) {
            map.put(FIELD_DISABLED_REASON_KEY, disabledReasonKey);
        }
        if (proposal != null) {
            map.put(FIELD_PROPOSAL, proposal.toMap());
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
     * Writes this proposal to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed proposal
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static FollowUpProposalResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "FollowUpProposalResource");
        var subjectId = JsonTree.string(map, FIELD_SUBJECT_ID);
        if (subjectId == null) {
            throw new JsonFormatException("Missing required field for FollowUpProposalResource: subject_id");
        }
        var subjectKind = EntryKind.fromWireOrNull(JsonTree.string(map, FIELD_SUBJECT_KIND));
        if (subjectKind == null) {
            throw new JsonFormatException("Unknown " + FIELD_SUBJECT_KIND + " in FollowUpProposalResource: "
                    + JsonTree.string(map, FIELD_SUBJECT_KIND));
        }
        var role = ChatRole.fromValue(JsonTree.string(map, FIELD_ROLE));
        if (role == null) {
            throw new JsonFormatException("Unknown " + FIELD_ROLE + " in FollowUpProposalResource: "
                    + JsonTree.string(map, FIELD_ROLE));
        }
        var scope = ChatScope.fromValue(JsonTree.string(map, FIELD_SCOPE));
        if (scope == null) {
            throw new JsonFormatException("Unknown " + FIELD_SCOPE + " in FollowUpProposalResource: "
                    + JsonTree.string(map, FIELD_SCOPE));
        }
        var proposalMap = JsonTree.objectOrNull(map, FIELD_PROPOSAL, "FollowUpProposalResource");
        try {
            return new FollowUpProposalResource(subjectId, subjectKind, role, scope,
                    StartJson.bool(map, FIELD_AVAILABLE), JsonTree.string(map, FIELD_DISABLED_REASON_KEY),
                    proposalMap != null ? ChatProposalResource.fromMap(proposalMap) : null);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid FollowUpProposalResource: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed proposal
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static FollowUpProposalResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for FollowUpProposalResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one proposal from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed proposal
     * @throws IOException on format failure
     */
    public static FollowUpProposalResource readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for FollowUpProposalResource");
        }
        return fromMap(map);
    }
}
