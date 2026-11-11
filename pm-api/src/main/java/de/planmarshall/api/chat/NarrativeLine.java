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

import de.planmarshall.api.id.DottedIdentifier;
import de.planmarshall.api.json.JsonTree;

/**
 * One line of the subject's narrative a notice reports, {@code {key, args, refs, at}}
 * ({@code state-catalogue.adoc#chat-narrative}): a message key {@code chat.line.*} with its arguments
 * and the references it names, never a model's words and never untrusted text.
 *
 * @param key  the line's message key
 * @param args the arguments of the key, never {@code null}
 * @param refs the references the line names, in order
 * @param at   the instant of the event the line is derived from, ISO-8601
 */
public record NarrativeLine(String key, Map<String, Object> args, List<TurnRef> refs, String at) {

    /** The wire name of the line's key. */
    public static final String FIELD_KEY = "key";
    /** The wire name of the arguments. */
    public static final String FIELD_ARGS = "args";
    /** The wire name of the references. */
    public static final String FIELD_REFS = "refs";
    /** The wire name of the instant. */
    public static final String FIELD_AT = "at";

    private static final String TYPE = "NarrativeLine";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_KEY, FIELD_ARGS, FIELD_REFS, FIELD_AT);

    /**
     * @param key  a key of the message key grammar
     * @param args the arguments, {@code null} read as none
     * @param refs the references, {@code null} read as none
     * @param at   the instant, not blank
     */
    public NarrativeLine {
        Objects.requireNonNull(key, FIELD_KEY);
        if (!DottedIdentifier.messageKey(key)) {
            throw new IllegalArgumentException("Invalid message key: " + key);
        }
        args = JsonTree.immutableMap(args);
        refs = refs == null ? List.of() : List.copyOf(refs);
        Objects.requireNonNull(at, FIELD_AT);
        if (at.isBlank()) {
            throw new IllegalArgumentException("at must not be blank");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KEY, key);
        if (!args.isEmpty()) {
            map.put(FIELD_ARGS, args);
        }
        map.put(FIELD_REFS, refs.stream().map(TurnRef::toMap).toList());
        map.put(FIELD_AT, at);
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
     * @return the parsed line
     * @throws IOException if a member is unknown, missing, or malformed
     */
    public static NarrativeLine fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var key = ChatJson.requireText(map, FIELD_KEY, TYPE);
        var args = ChatJson.readArgs(map, FIELD_ARGS, TYPE);
        var refs = ChatJson.readRecords(map, FIELD_REFS, TurnRef::fromMap, TYPE);
        var at = ChatJson.requireText(map, FIELD_AT, TYPE);
        return ChatJson.construct(TYPE, () -> new NarrativeLine(key, args, refs, at));
    }

    /**
     * @param json the JSON object string
     * @return the parsed line
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static NarrativeLine fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
