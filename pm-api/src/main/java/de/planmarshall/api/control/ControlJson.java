/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.UtilityClass;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Shared JSON tree helpers for the control records' codecs.
 */
@UtilityClass
final class ControlJson {

    /**
     * Reads the named field of a record as a list of strings. An absent field yields an empty list;
     * a present {@code null}, a non-array value, or a structured element fails closed.
     *
     * @param record the record's map
     * @param key    the field key
     * @return the strings, never {@code null}
     * @throws IOException if a present value is not a scalar array
     */
    static List<String> readStrings(Map<String, Object> record, String key) throws IOException {
        if (!record.containsKey(key)) {
            return List.of();
        }
        return strings(record.get(key), key);
    }

    /**
     * Reads the named field of a record as a list of objects. An absent field yields an empty list;
     * a present {@code null}, a non-array value, or an element that is not an object fails closed
     * instead of being dropped silently.
     *
     * @param record the record's map
     * @param key    the field key
     * @return the object maps, never {@code null}
     * @throws IOException if a present value is not an object array
     */
    static List<Map<String, Object>> readObjects(Map<String, Object> record, String key) throws IOException {
        if (!record.containsKey(key)) {
            return List.of();
        }
        return objects(record.get(key), key);
    }

    private static List<String> strings(Object raw, String key) throws IOException {
        var array = array(raw, key);
        for (var item : array) {
            if (item instanceof Map<?, ?> || item instanceof List<?>) {
                throw new JsonFormatException("Expected scalars for " + key);
            }
        }
        return array.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    private static List<Map<String, Object>> objects(Object raw, String key) throws IOException {
        var array = array(raw, key);
        var objects = new ArrayList<Map<String, Object>>();
        for (var item : array) {
            var object = JsonTree.asObject(item);
            if (object == null) {
                throw new JsonFormatException("Expected an object in " + key);
            }
            objects.add(object);
        }
        return objects;
    }

    private static List<Object> array(Object raw, String key) throws IOException {
        var array = raw != null ? JsonTree.asArray(raw) : null;
        if (array == null) {
            throw new JsonFormatException("Expected an array for " + key);
        }
        return array;
    }

    /**
     * @param map the object
     * @param key the field key
     * @return the integer value, or {@code null}
     */
    static Integer optInteger(Map<String, Object> map, String key) {
        var value = JsonTree.number(map, key);
        return value != null ? value.intValue() : null;
    }

    /**
     * @param map the object
     * @param key the field key
     * @return the long value, or {@code null}
     */
    static Long optLong(Map<String, Object> map, String key) {
        var value = JsonTree.number(map, key);
        return value != null ? value.longValue() : null;
    }

    /**
     * @param map the object
     * @param key the field key
     * @return the boolean value, or {@code false}
     */
    static boolean bool(Map<String, Object> map, String key) {
        return Boolean.TRUE.equals(JsonTree.bool(map, key));
    }
}
