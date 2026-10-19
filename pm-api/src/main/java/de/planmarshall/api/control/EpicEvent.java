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
 * One event in an epic's history (design <em>Epic View</em>, the events).
 *
 * <p>The text is the epic's own line, e.g. {@code relay-reconnect-backoff landed;
 * relay-reconnect-metrics re-ordered} or {@code you switched automatic off}; the instant is when it
 * happened, as an ISO-8601 timestamp.
 *
 * @param at   the instant, ISO-8601
 * @param text the event line
 */
public record EpicEvent(String at, String text) {

    public EpicEvent {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(text, "text");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("at", at);
        map.put("text", text);
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
     * @return the parsed event
     * @throws IOException if a required field is missing
     */
    public static EpicEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var at = JsonTree.string(map, "at");
        var text = JsonTree.string(map, "text");
        if (at == null || text == null) {
            throw new JsonFormatException("Missing required fields for EpicEvent: at, text");
        }
        return new EpicEvent(at, text);
    }

    /**
     * @param json the JSON object string
     * @return the parsed event
     * @throws IOException if the value is not an object
     */
    public static EpicEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EpicEvent");
        }
        return fromMap(obj);
    }
}
