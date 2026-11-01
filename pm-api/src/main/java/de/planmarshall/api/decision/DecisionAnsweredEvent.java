/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decision;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a decision is answered in any channel ({@code decision.answered}).
 *
 * @param id      identifier of the answered decision
 * @param answer  summary or chosen option of the recorded answer
 * @param channel channel where answered ({@code "cli"} or {@code "web"})
 * @param device  optional device identifier or label
 */
public record DecisionAnsweredEvent(
        String id,
        String answer,
        String channel,
        @Nullable String device
) {
    public static final String EVENT_NAME = "decision.answered";

    public DecisionAnsweredEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(answer, "answer");
        Objects.requireNonNull(channel, "channel");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("answer", answer);
        map.put("channel", channel);
        if (device != null) {
            map.put("device", device);
        }
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed event
     * @throws IOException on error
     */
    public static DecisionAnsweredEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, "id");
        if (id == null) throw new JsonFormatException("Missing id in DecisionAnsweredEvent");
        var answer = JsonTree.string(map, "answer");
        if (answer == null) answer = "";
        var channel = JsonTree.string(map, "channel");
        if (channel == null) channel = "cli";
        var device = JsonTree.string(map, "device");
        return new DecisionAnsweredEvent(id, answer, channel, device);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static DecisionAnsweredEvent fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        var map = JsonTree.asObject(parsed);
        if (map == null) throw new JsonFormatException("Expected JSON object for DecisionAnsweredEvent");
        return fromMap(map);
    }
}
