/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.process;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a plan's token metrics update ({@code tokens.changed}).
 *
 * @param planId plan identifier
 * @param tokens updated token usage summary
 */
public record TokensChangedEvent(
        String planId,
        ProcessTokens tokens
) {
    public static final String EVENT_NAME = "tokens.changed";

    public TokensChangedEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(tokens, "tokens");
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("tokens", tokens.toMap());
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static TokensChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) planId = JsonTree.string(map, "planId");
        if (planId == null) throw new JsonFormatException("Missing plan_id in TokensChangedEvent");

        var tokensObj = JsonTree.asObject(map.get("tokens"));
        if (tokensObj == null) {
            tokensObj = JsonTree.asObject(map.get("tokens_so_far"));
        }
        if (tokensObj == null) throw new JsonFormatException("Missing tokens in TokensChangedEvent");

        return new TokensChangedEvent(planId, ProcessTokens.fromMap(tokensObj));
    }

    public static TokensChangedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) throw new JsonFormatException("Expected JSON object for TokensChangedEvent");
        return fromMap(obj);
    }
}
