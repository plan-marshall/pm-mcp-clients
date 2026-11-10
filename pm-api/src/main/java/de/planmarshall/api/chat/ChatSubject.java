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
 * What a chat is about, {@code {kind, id}}: the chat's own subject, and the subject of the chat that
 * proposed a plan's {@code queued_revision}.
 *
 * @param kind the subject's kind
 * @param id   the subject's id (a plan id, an epic id, an entry id, or a decision's address
 *             {@code <scope_id>/<question_id>})
 */
public record ChatSubject(ChatSubjectKind kind, String id) {

    /** The wire name of the subject's kind. */
    public static final String FIELD_KIND = "kind";
    /** The wire name of the subject's id. */
    public static final String FIELD_ID = "id";

    private static final String TYPE = "ChatSubject";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_KIND, FIELD_ID);

    /**
     * @param kind the subject's kind
     * @param id   the subject's id, not blank
     */
    public ChatSubject {
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(id, FIELD_ID);
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_ID, id);
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
     * @return the parsed subject
     * @throws IOException if a member is unknown, missing, or names no subject kind
     */
    public static ChatSubject fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var kind = ChatJson.requireEnum(map, FIELD_KIND, ChatSubjectKind::fromWireOrNull, TYPE);
        var id = ChatJson.requireText(map, FIELD_ID, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatSubject(kind, id));
    }

    /**
     * @param json the JSON object string
     * @return the parsed subject
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatSubject fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
