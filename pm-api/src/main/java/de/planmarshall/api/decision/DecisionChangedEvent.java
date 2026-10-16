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
 * Event payload emitted when an open decision is modified or revised ({@code decision.changed}).
 *
 * @param decision      the revised decision envelope
 * @param changeSummary description of what changed
 */
public record DecisionChangedEvent(DecisionEnvelope decision, @Nullable String changeSummary) {

    public static final String EVENT_NAME = "decision.changed";
    private static final String FIELD_DECISION = "decision";
    private static final String FIELD_CHANGE_SUMMARY = "change_summary";

    public DecisionChangedEvent {
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
        if (changeSummary != null) {
            map.put(FIELD_CHANGE_SUMMARY, changeSummary);
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
    public static DecisionChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var decObj = map.get(FIELD_DECISION);
        var decMap = JsonTree.asObject(decObj);
        if (decMap == null) {
            throw new JsonFormatException("Missing " + FIELD_DECISION + " in DecisionChangedEvent");
        }
        var summary = JsonTree.string(map, FIELD_CHANGE_SUMMARY);
        if (summary == null) summary = JsonTree.string(map, "change");
        return new DecisionChangedEvent(DecisionEnvelope.fromMap(decMap), summary);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on error
     */
    public static DecisionChangedEvent fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        var map = JsonTree.asObject(parsed);
        if (map == null) throw new JsonFormatException("Expected JSON object for DecisionChangedEvent");
        return fromMap(map);
    }
}
