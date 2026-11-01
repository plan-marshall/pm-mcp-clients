/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.intent;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A dependency a plan's outline adds, with its justification (Store Schemas, Outline Record; design
 * <em>Intent and Outline</em>).
 * <p>
 * An added dependency is never shown as an unqualified line: the deliverable that needs it and the
 * reason are part of the record, so the operator can judge an addition the plan did not inherit.
 *
 * @param name         the dependency's coordinates
 * @param version      the version, or {@code null} where the build manages it
 * @param justification why the deliverable needs it
 * @param deliverable  the number of the deliverable that adds it
 */
public record DependencyJustification(
        String name,
        @Nullable String version,
        String justification,
        int deliverable
) {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_JUSTIFICATION = "justification";
    public static final String FIELD_DELIVERABLE = "deliverable";

    private static final String TYPE = "DependencyJustification";
    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_NAME, FIELD_VERSION, FIELD_JUSTIFICATION, FIELD_DELIVERABLE);

    public DependencyJustification {
        Objects.requireNonNull(name, FIELD_NAME);
        Objects.requireNonNull(justification, FIELD_JUSTIFICATION);
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (justification.isBlank()) {
            throw new IllegalArgumentException("justification must not be blank");
        }
        if (deliverable < 1 || deliverable > 99) {
            throw new IllegalArgumentException("deliverable must be between 1 and 99");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_NAME, name);
        if (version != null) {
            map.put(FIELD_VERSION, version);
        }
        map.put(FIELD_JUSTIFICATION, justification);
        map.put(FIELD_DELIVERABLE, deliverable);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this dependency to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_NAME, name);
        IntentJson.writeOptionalString(generator, FIELD_VERSION, version);
        generator.writeStringField(FIELD_JUSTIFICATION, justification);
        generator.writeNumberField(FIELD_DELIVERABLE, deliverable);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed dependency
     * @throws IOException if a required field is missing or out of range
     */
    public static DependencyJustification fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        try {
            return new DependencyJustification(
                    IntentJson.requireText(map, FIELD_NAME, TYPE),
                    IntentJson.optionalText(map, FIELD_VERSION, TYPE),
                    IntentJson.requireText(map, FIELD_JUSTIFICATION, TYPE),
                    IntentJson.requireInt(map, FIELD_DELIVERABLE, 1, 99, TYPE));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed dependency
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static DependencyJustification fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one dependency from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed dependency
     * @throws IOException on format failure
     */
    public static DependencyJustification readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
