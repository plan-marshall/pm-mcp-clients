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

import de.planmarshall.api.json.JsonTree;

/**
 * What the role knows, {@code {knows[], as_of}} ({@code state-catalogue.adoc#chat-context}, The
 * Context Summary): what the role was last given, as message keys {@code chat.knows.*}, rendered from
 * the store when the chat opens and refreshed with each turn of the role, {@code as_of} the instant of
 * the latest.
 *
 * @param knows what the role is given, in order
 * @param asOf  the instant it was rendered, ISO-8601
 */
public record ChatContext(List<MessageText> knows, String asOf) {

    /** The wire name of what the role knows. */
    public static final String FIELD_KNOWS = "knows";
    /** The wire name of the instant. */
    public static final String FIELD_AS_OF = "as_of";

    private static final String TYPE = "ChatContext";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_KNOWS, FIELD_AS_OF);

    /**
     * @param knows what the role is given, {@code null} read as nothing
     * @param asOf  the instant, not blank
     */
    public ChatContext {
        knows = knows == null ? List.of() : List.copyOf(knows);
        Objects.requireNonNull(asOf, FIELD_AS_OF);
        if (asOf.isBlank()) {
            throw new IllegalArgumentException("as_of must not be blank");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KNOWS, knows.stream().map(MessageText::toMap).toList());
        map.put(FIELD_AS_OF, asOf);
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
     * @return the parsed context
     * @throws IOException if a member is unknown, missing, or malformed
     */
    public static ChatContext fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var knows = ChatJson.readRecords(map, FIELD_KNOWS, MessageText::fromMap, TYPE);
        var asOf = ChatJson.requireText(map, FIELD_AS_OF, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatContext(knows, asOf));
    }

    /**
     * @param json the JSON object string
     * @return the parsed context
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatContext fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
