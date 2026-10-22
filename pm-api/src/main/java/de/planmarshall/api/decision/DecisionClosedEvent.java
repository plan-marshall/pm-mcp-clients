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
 * Event payload emitted when a decision is closed (e.g. answered, waived, declined, withdrawn) ({@code decision.closed}).
 *
 * @param id      identifier of the decision
 * @param outcome outcome status ({@code "answered"}, {@code "waived"}, {@code "declined"}, {@code "withdrawn"})
 * @param reason  optional reason or detail
 */
public record DecisionClosedEvent(
        String id,
        String outcome,
        @Nullable String reason
) {
    public static final String EVENT_NAME = "decision.closed";

    public DecisionClosedEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(outcome, "outcome");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("outcome", outcome);
        if (reason != null) {
            map.put("reason", reason);
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
    public static DecisionClosedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, "id");
        if (id == null) throw new JsonFormatException("Missing id in DecisionClosedEvent");
        var outcome = JsonTree.string(map, "outcome");
        if (outcome == null) outcome = "answered";
        var reason = JsonTree.string(map, "reason");
        return new DecisionClosedEvent(id, outcome, reason);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static DecisionClosedEvent fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        var map = JsonTree.asObject(parsed);
        if (map == null) throw new JsonFormatException("Expected JSON object for DecisionClosedEvent");
        return fromMap(map);
    }
}
