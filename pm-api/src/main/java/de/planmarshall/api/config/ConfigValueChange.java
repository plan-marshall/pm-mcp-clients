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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A change of one recorded configuration value, sent against the revision the value was read with
 * ({@code CLI, Coordination & Security Specification, Concurrent Changes of a Setting}).
 * <p>
 * The revision travels as a body member, beside the value it changes, so a {@code PUT} and a
 * {@code DELETE} of {@code /api/v1/config/{level}/{key}} carry the same shape: a {@code PUT} names
 * its new {@code value}, a {@code DELETE} carries the revision alone because the key it addresses is
 * already the change.
 *
 * @param value    the new value as a JSON tree value (string, number, boolean, list, object), or
 *                 {@code null} for a deletion or a value that is explicitly cleared
 * @param revision the revision the change was made against, {@code 0} for a value never recorded
 */
public record ConfigValueChange(
        @Nullable Object value,
        long revision,
        boolean deletion
) {
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_REVISION = "revision";

    public ConfigValueChange {
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative: " + revision);
        }
        value = value == null ? null : JsonTree.immutableCopy(value);
    }

    /**
     * @param value    the new value, or {@code null} for an explicitly cleared value
     * @param revision the revision the change was made against
     */
    public ConfigValueChange(@Nullable Object value, long revision) {
        this(value, revision, false);
    }

    /**
     * @param revision the revision a deletion was made against
     * @return a change that carries no value, as a deletion does
     */
    public static ConfigValueChange deletion(long revision) {
        return new ConfigValueChange(null, revision, true);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (!deletion) {
            map.put(FIELD_VALUE, value);
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
     * Parses from a JSON tree map.
     *
     * @param map map representation
     * @return parsed change
     * @throws IOException on format failure
     */
    public static ConfigValueChange fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var revision = JsonTree.integralNumber(map, FIELD_REVISION, 0, Long.MAX_VALUE);
        try {
            return new ConfigValueChange(map.get(FIELD_VALUE), revision, !map.containsKey(FIELD_VALUE));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid ConfigValueChange: " + e.getMessage(), e);
        }
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed change
     * @throws IOException on format failure
     */
    public static ConfigValueChange fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ConfigValueChange");
        }
        return fromMap(obj);
    }
}
