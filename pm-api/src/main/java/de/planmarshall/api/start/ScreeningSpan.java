/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One span the screener flagged in a stored source (State Catalogue, {@code source-<kind>.flagged};
 * design <em>Starting Work</em>, an issue).
 * <p>
 * The span is quoted as plain text with the screener's reason. A client never renders it as markup
 * and never follows a link inside it: the text comes from outside the machine.
 *
 * @param start  the inclusive character offset of the span in the stored text ({@code >= 0})
 * @param end    the exclusive character offset of the span ({@code >= start})
 * @param text   the flagged text, verbatim
 * @param reason why the screener flagged it
 */
public record ScreeningSpan(
        int start,
        int end,
        String text,
        String reason
) {

    public static final String FIELD_START = "start";
    public static final String FIELD_END = "end";
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_REASON = "reason";

    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_START, FIELD_END, FIELD_TEXT, FIELD_REASON);

    public ScreeningSpan {
        if (start < 0) {
            throw new IllegalArgumentException("A screening span starts at or after 0: " + start);
        }
        if (end < start) {
            throw new IllegalArgumentException("A screening span ends at or after its start: " + end + " < " + start);
        }
        Objects.requireNonNull(text, FIELD_TEXT);
        Objects.requireNonNull(reason, FIELD_REASON);
    }

    /**
     * @return the number of characters the span covers
     */
    public int length() {
        return end - start;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_START, start);
        map.put(FIELD_END, end);
        map.put(FIELD_TEXT, text);
        map.put(FIELD_REASON, reason);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this span to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed span
     * @throws IOException if a required field is missing or a field is malformed
     */
    public static ScreeningSpan fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "ScreeningSpan");
        var start = StartJson.optInteger(map, FIELD_START);
        var end = StartJson.optInteger(map, FIELD_END);
        var text = JsonTree.string(map, FIELD_TEXT);
        var reason = JsonTree.string(map, FIELD_REASON);
        if (start == null || end == null || text == null || reason == null) {
            throw new JsonFormatException("Missing required fields for ScreeningSpan: start, end, text, reason");
        }
        try {
            return new ScreeningSpan(start, end, text, reason);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid ScreeningSpan: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed span
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static ScreeningSpan fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ScreeningSpan");
        }
        return fromMap(obj);
    }

    /**
     * Reads one span from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed span
     * @throws IOException on format failure
     */
    public static ScreeningSpan readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for ScreeningSpan");
        }
        return fromMap(map);
    }
}
