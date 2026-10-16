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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a new decision or waiver request is opened ({@code decision.opened}).
 *
 * @param decision the opened decision envelope
 */
public record DecisionOpenedEvent(DecisionEnvelope decision) {

    public static final String EVENT_NAME = "decision.opened";
    private static final String FIELD_DECISION = "decision";

    public DecisionOpenedEvent {
        Objects.requireNonNull(decision, FIELD_DECISION);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_DECISION, decision.toMap());
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
    public static DecisionOpenedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var decObj = map.get(FIELD_DECISION);
        var decMap = JsonTree.asObject(decObj);
        if (decMap == null) {
            throw new JsonFormatException("Missing " + FIELD_DECISION + " in DecisionOpenedEvent");
        }
        return new DecisionOpenedEvent(DecisionEnvelope.fromMap(decMap));
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static DecisionOpenedEvent fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        var map = JsonTree.asObject(parsed);
        if (map == null) throw new JsonFormatException("Expected JSON object for DecisionOpenedEvent");
        return fromMap(map);
    }
}
