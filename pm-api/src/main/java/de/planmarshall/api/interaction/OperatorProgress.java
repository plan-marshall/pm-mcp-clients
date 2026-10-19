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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The progress of an operator state where its unit counts its steps: the {@code progress} member of
 * {@link OperatorStateResource}, {@code {done, of}}.
 *
 * @param done the steps done so far ({@code >= 0})
 * @param of   the number of steps ({@code >= 1})
 */
public record OperatorProgress(long done, long of) {
    public static final String FIELD_DONE = "done";
    public static final String FIELD_OF = "of";

    public OperatorProgress {
        if (done < 0) {
            throw new IllegalArgumentException("done cannot be negative: " + done);
        }
        if (of < 1) {
            throw new IllegalArgumentException("of must be at least 1: " + of);
        }
        if (done > of) {
            throw new IllegalArgumentException("done cannot exceed of: " + done + " > " + of);
        }
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_DONE, done);
        map.put(FIELD_OF, of);
        return map;
    }

    /**
     * Writes this progress to a streaming generator.
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
     * @return parsed progress
     * @throws IOException on format failure
     */
    public static OperatorProgress fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        for (var key : map.keySet()) {
            if (!FIELD_DONE.equals(key) && !FIELD_OF.equals(key)) {
                throw new JsonFormatException("Unknown field in OperatorProgress: " + key);
            }
        }
        var done = JsonTree.integralNumber(map, FIELD_DONE, 0, Long.MAX_VALUE);
        var of = JsonTree.integralNumber(map, FIELD_OF, 1, Long.MAX_VALUE);
        try {
            return new OperatorProgress(done, of);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid OperatorProgress: " + e.getMessage(), e);
        }
    }

    /**
     * Reads one progress from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed progress
     * @throws IOException on format failure
     */
    public static OperatorProgress readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for OperatorProgress");
        }
        return fromMap(map);
    }
}
