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

import de.planmarshall.api.id.DottedIdentifier;
import de.planmarshall.api.json.JsonTree;

/**
 * A message key with its arguments, {@code {key, args}}, never a model's words: an entry of what the
 * role knows ({@code chat.knows.*}), a step of a reply ({@code chat.turn.progress}), a failed turn's
 * {@code reason}, a proposal's {@code applies} and finding, and the point a {@code queued_revision}
 * waits for ({@code chat.applies.*}).
 * <p>
 * The arguments are a JSON object of values; an empty object is not written.
 *
 * @param key  the message key
 * @param args the arguments of the key, never {@code null}
 */
public record MessageText(String key, Map<String, Object> args) {

    /** The wire name of the message key. */
    public static final String FIELD_KEY = "key";
    /** The wire name of the arguments. */
    public static final String FIELD_ARGS = "args";

    private static final String TYPE = "MessageText";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_KEY, FIELD_ARGS);

    /**
     * @param key  a key of the message key grammar
     * @param args the arguments, {@code null} read as none
     */
    public MessageText {
        Objects.requireNonNull(key, FIELD_KEY);
        if (!DottedIdentifier.messageKey(key)) {
            throw new IllegalArgumentException("Invalid message key: " + key);
        }
        args = JsonTree.immutableMap(args);
    }

    /**
     * @param key the message key
     * @return a message text without arguments
     */
    public static MessageText of(String key) {
        return new MessageText(key, Map.of());
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
     * @return the parsed message text
     * @throws IOException if a member is unknown, the key is missing or invalid, or the arguments are
     *                     not an object
     */
    public static MessageText fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var key = ChatJson.requireText(map, FIELD_KEY, TYPE);
        var args = ChatJson.readArgs(map, FIELD_ARGS, TYPE);
        return ChatJson.construct(TYPE, () -> new MessageText(key, args));
    }

    /**
     * @param json the JSON object string
     * @return the parsed message text
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static MessageText fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
