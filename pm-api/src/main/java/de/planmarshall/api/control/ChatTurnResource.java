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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One line of a chat's transcript between the operator and the planning role (design
 * <em>Chatting with the Planning Role</em>).
 *
 * <p>The operator's messages are kept as they are and never interpreted; an item from outside the
 * machine stays marked {@link #untrusted()} and is shown as text, never rendered.
 *
 * @param role      {@code operator}, or the role that answered ({@code replan}, {@code decompose})
 * @param text      the message, as written
 * @param at        the instant, ISO-8601
 * @param untrusted whether the message came from outside the machine
 */
public record ChatTurnResource(
        String role,
        String text,
        String at,
        boolean untrusted
) {

    /** The role of the operator's own messages. */
    public static final String ROLE_OPERATOR = "operator";

    public ChatTurnResource {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(text, "text");
        at = at != null ? at : "";
    }

    /**
     * @return {@code true} if this is the operator's own message
     */
    public boolean isOperator() {
        return ROLE_OPERATOR.equals(role);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("role", role);
        map.put("text", text);
        map.put("at", at);
        map.put("untrusted", untrusted);
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
     * @return the parsed turn
     * @throws IOException if a required field is missing
     */
    public static ChatTurnResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var role = JsonTree.string(map, "role");
        var text = JsonTree.string(map, "text");
        if (role == null || text == null) {
            throw new JsonFormatException("Missing required fields for ChatTurnResource: role, text");
        }
        return new ChatTurnResource(role, text, JsonTree.string(map, "at"),
                ControlJson.bool(map, "untrusted"));
    }

    /**
     * @param json the JSON object string
     * @return the parsed turn
     * @throws IOException if the value is not an object
     */
    public static ChatTurnResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ChatTurnResource");
        }
        return fromMap(obj);
    }
}
