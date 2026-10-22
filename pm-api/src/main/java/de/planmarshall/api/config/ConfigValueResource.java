/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One effective configuration value as {@code GET /api/v1/config} serves it to the operator's
 * clients: the effective key ({@code <file_stem>.<dotted path>}), the value, the {@link ConfigOrigin}
 * it is resolved from, the origin it overrides where it shadows a nearer-to-{@code default} level,
 * and the settings revision the value was last written against.
 * <p>
 * The {@code overrides} origin, when present, is always of a strictly lower {@link ConfigOrigin#rank()}
 * than {@code origin}: a value can only shadow a level farther from the plan. The {@code revision} is
 * the settings revision a write of this value was made against; a change against an older revision is
 * refused {@code conflict}. A value the operator never recorded carries the revision of the file it
 * is read from, {@code 0} for a compiled or derived value.
 *
 * @param key       the effective configuration key
 * @param value     the value as a JSON tree value (string, number, boolean, list, object), or {@code null}
 * @param origin    the origin the value is resolved from
 * @param overrides the origin this value overrides, or {@code null} when it overrides nothing
 * @param revision  the settings revision the value was written against, {@code 0} when none applies
 */
public record ConfigValueResource(
        String key,
        @Nullable Object value,
        ConfigOrigin origin,
        @Nullable ConfigOrigin overrides,
        long revision
) {
    public static final String FIELD_KEY = "key";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_ORIGIN = "origin";
    public static final String FIELD_OVERRIDES = "overrides";
    public static final String FIELD_REVISION = "revision";

    public ConfigValueResource {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank in ConfigValueResource");
        }
        Objects.requireNonNull(origin, FIELD_ORIGIN);
        if (overrides != null && !origin.overrides(overrides)) {
            throw new IllegalArgumentException(
                    "origin " + origin.wire() + " cannot override " + overrides.wire()
                            + " in ConfigValueResource");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative: " + revision);
        }
        value = value == null ? null : JsonTree.immutableCopy(value);
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
        if (overrides != null) {
            map.put(FIELD_OVERRIDES, overrides.wire());
        }
        map.put(FIELD_REVISION, revision);
        return map;
    }

    /**
     * Serializes to a JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this resource to a streaming generator.
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
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static ConfigValueResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var key = JsonTree.string(map, FIELD_KEY);
        if (key == null || key.isBlank()) {
            throw new JsonFormatException("Missing " + FIELD_KEY + " in ConfigValueResource");
        }
        var origin = ConfigOrigin.fromWireOrNull(JsonTree.string(map, FIELD_ORIGIN));
        if (origin == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_ORIGIN + " in ConfigValueResource: " + JsonTree.string(map, FIELD_ORIGIN));
        }
        ConfigOrigin overrides = null;
        if (map.containsKey(FIELD_OVERRIDES) && map.get(FIELD_OVERRIDES) != null) {
            overrides = ConfigOrigin.fromWireOrNull(JsonTree.string(map, FIELD_OVERRIDES));
            if (overrides == null) {
                throw new JsonFormatException(
                        "Unknown " + FIELD_OVERRIDES + " in ConfigValueResource: "
                                + JsonTree.string(map, FIELD_OVERRIDES));
            }
            if (!origin.overrides(overrides)) {
                throw new JsonFormatException(
                        "origin " + origin.wire() + " cannot override " + overrides.wire() + " in ConfigValueResource");
            }
        }
        var revision = JsonTree.integralNumber(map, FIELD_REVISION, 0, Long.MAX_VALUE);
        try {
            return new ConfigValueResource(key, map.get(FIELD_VALUE), origin, overrides, revision);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid ConfigValueResource: " + e.getMessage(), e);
        }
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static ConfigValueResource fromJson(String json) throws IOException {
        return fromMap(objectOf(JsonTree.parse(json)));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static ConfigValueResource readFrom(JsonParser parser) throws IOException {
        return fromMap(objectOf(JsonTree.readValue(parser)));
    }

    private static Map<String, Object> objectOf(Object parsed) throws IOException {
        var map = JsonTree.asObject(parsed);
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for ConfigValueResource");
        }
        return map;
    }
}
