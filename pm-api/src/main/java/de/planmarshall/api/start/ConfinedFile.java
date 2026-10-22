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

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One entry of the confined file listing (State Catalogue, Documents as a Source): a file, a
 * directory, or a link, with its size and whether the picker may use it.
 * <p>
 * The path is always relative to the listing's root; the listing never carries an absolute path of
 * the machine. A link is listed with its target's name only, never resolved here, and a link that
 * leads out of the root is refused by the caller.
 *
 * @param name      the entry's name within its directory
 * @param path      the path relative to the root
 * @param directory whether the entry is a directory
 * @param size      the size in bytes ({@code 0} for a directory)
 * @param readable  whether the entry may be read
 * @param refusal   why the entry cannot be used, or {@code null} while it can
 */
public record ConfinedFile(
        String name,
        String path,
        boolean directory,
        long size,
        boolean readable,
        @Nullable SourceFailure refusal
) {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_DIRECTORY = "directory";
    public static final String FIELD_SIZE = "size";
    public static final String FIELD_READABLE = "readable";
    public static final String FIELD_REFUSAL = "refusal";

    private static final Set<String> KNOWN_FIELDS =
            Set.of(FIELD_NAME, FIELD_PATH, FIELD_DIRECTORY, FIELD_SIZE, FIELD_READABLE, FIELD_REFUSAL);

    public ConfinedFile {
        Objects.requireNonNull(name, FIELD_NAME);
        Objects.requireNonNull(path, FIELD_PATH);
        if (size < 0) {
            throw new IllegalArgumentException("A file's size cannot be negative: " + size);
        }
        if (refusal != null && readable) {
            throw new IllegalArgumentException("A refused file is not readable: " + path);
        }
    }

    /**
     * @return {@code true} if the entry may be picked
     */
    public boolean pickable() {
        return readable && refusal == null;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_NAME, name);
        map.put(FIELD_PATH, path);
        map.put(FIELD_DIRECTORY, directory);
        map.put(FIELD_SIZE, size);
        map.put(FIELD_READABLE, readable);
        if (refusal != null) {
            map.put(FIELD_REFUSAL, refusal.toMap());
        }
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this file to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed file
     * @throws IOException if a required field is missing or a field is malformed
     */
    public static ConfinedFile fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "ConfinedFile");
        var name = JsonTree.string(map, FIELD_NAME);
        var path = JsonTree.string(map, FIELD_PATH);
        if (name == null || path == null) {
            throw new JsonFormatException("Missing required fields for ConfinedFile: name, path");
        }
        var size = StartJson.optLong(map, FIELD_SIZE);
        var refusalMap = JsonTree.objectOrNull(map, FIELD_REFUSAL, "ConfinedFile");
        try {
            return new ConfinedFile(name, path, StartJson.bool(map, FIELD_DIRECTORY), size != null ? size : 0L,
                    StartJson.bool(map, FIELD_READABLE),
                    refusalMap != null ? SourceFailure.fromMap(refusalMap) : null);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid ConfinedFile: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed file
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static ConfinedFile fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ConfinedFile");
        }
        return fromMap(obj);
    }

    /**
     * Reads one file from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed file
     * @throws IOException on format failure
     */
    public static ConfinedFile readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for ConfinedFile");
        }
        return fromMap(map);
    }
}
