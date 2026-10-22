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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Result of interpreting an operator's free-text own words (UC-PLN-2).
 *
 * @param interpretationType    result kind ({@code OPTION}, {@code OPTION_WITH_CONDITION}, {@code OWN_OPTION}, {@code DELEGATION}, {@code UNCLEAR})
 * @param mappedOptionKey       chosen option key if mapped to an existing option
 * @param condition             condition text if mapped with condition
 * @param readBackText          one-sentence summary of what was understood
 * @param clarificationQuestion clarifying question if interpretation is unclear
 * @param underlyingPassages    passages from the operator's words on which the interpretation rests
 */
public record DecisionInterpretation(
        InterpretationType interpretationType,
        @Nullable String mappedOptionKey,
        @Nullable String condition,
        @Nullable String readBackText,
        @Nullable String clarificationQuestion,
        List<String> underlyingPassages
) {
    public DecisionInterpretation {
        Objects.requireNonNull(interpretationType, "interpretationType");
        underlyingPassages = underlyingPassages != null ? List.copyOf(underlyingPassages) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("interpretation_type", interpretationType.value());
        if (mappedOptionKey != null) {
            map.put("mapped_option_key", mappedOptionKey);
        }
        if (condition != null) {
            map.put("condition", condition);
        }
        if (readBackText != null) {
            map.put("read_back_text", readBackText);
        }
        if (clarificationQuestion != null) {
            map.put("clarification_question", clarificationQuestion);
        }
        map.put("underlying_passages", underlyingPassages);
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
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed DecisionInterpretation
     * @throws IOException on error
     */
    public static DecisionInterpretation fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        var map = JsonTree.asObject(parsed);
        if (map == null) throw new JsonFormatException("Expected JSON object for DecisionInterpretation");
        return fromMap(map);
    }

    /**
     * Parses from a JSON map.
     *
     * @param map tree map
     * @return parsed interpretation
     */
    public static DecisionInterpretation fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var typeStr = JsonTree.string(map, "interpretation_type");
        var type = typeStr != null ? InterpretationType.fromValue(typeStr) : InterpretationType.OPTION;
        var optKey = JsonTree.string(map, "mapped_option_key");
        if (optKey == null) optKey = JsonTree.string(map, "option");
        var cond = JsonTree.string(map, "condition");
        var readBack = JsonTree.string(map, "read_back_text");
        if (readBack == null) readBack = JsonTree.string(map, "reading");
        var clarify = JsonTree.string(map, "clarification_question");
        if (clarify == null) clarify = JsonTree.string(map, "question");

        var passages = new ArrayList<String>();
        if (map.get("underlying_passages") instanceof List<?> list) {
            for (var item : list) {
                if (item != null) passages.add(item.toString());
            }
        }
        return new DecisionInterpretation(type, optKey, cond, readBack, clarify, passages);
    }
}
