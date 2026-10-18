/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.interaction;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One value a state proposes to the operator, with the {@link ConfigOrigin} it comes from: the
 * {@code proposal} member of {@link OperatorStateResource}. A value the machine holds but the project
 * has not set yet is the machine's value proposed at the nearest level the operator may write.
 *
 * @param key    the configuration key the proposal is about
 * @param value  the proposed value as a JSON tree value
 * @param origin the origin of the proposed value
 */
public record OperatorProposal(
        String key,
        Object value,
        ConfigOrigin origin
) {
    public static final String FIELD_KEY = "key";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_ORIGIN = "origin";

    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_KEY, FIELD_VALUE, FIELD_ORIGIN);

    public OperatorProposal {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank in OperatorProposal");
        }
        Objects.requireNonNull(value, FIELD_VALUE);
        value = JsonTree.immutableCopy(value);
        Objects.requireNonNull(origin, FIELD_ORIGIN);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KEY, key);
        map.put(FIELD_VALUE, value);
        map.put(FIELD_ORIGIN, origin.wire());
        return map;
    }

    /**
     * Writes this proposal to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map input map
     * @return parsed proposal
     * @throws IOException on format failure
     */
    public static OperatorProposal fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        for (var key : map.keySet()) {
            if (!KNOWN_FIELDS.contains(key)) {
                throw new JsonFormatException("Unknown field in OperatorProposal: " + key);
            }
        }
        var key = JsonTree.string(map, FIELD_KEY);
        if (key == null || key.isBlank()) {
            throw new JsonFormatException("Missing " + FIELD_KEY + " in OperatorProposal");
        }
        if (!map.containsKey(FIELD_VALUE) || map.get(FIELD_VALUE) == null) {
            throw new JsonFormatException("Missing " + FIELD_VALUE + " in OperatorProposal");
        }
        var origin = ConfigOrigin.fromWireOrNull(JsonTree.string(map, FIELD_ORIGIN));
        if (origin == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_ORIGIN + " in OperatorProposal: " + JsonTree.string(map, FIELD_ORIGIN));
        }
        try {
            return new OperatorProposal(key, map.get(FIELD_VALUE), origin);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid OperatorProposal: " + e.getMessage(), e);
        }
    }

    /**
     * Reads one proposal from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed proposal
     * @throws IOException on format failure
     */
    public static OperatorProposal readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for OperatorProposal");
        }
        return fromMap(map);
    }
}
