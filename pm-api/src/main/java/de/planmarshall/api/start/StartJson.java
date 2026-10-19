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
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.experimental.UtilityClass;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Shared JSON tree helpers for the start records' codecs.
 */
@UtilityClass
final class StartJson {

    /**
     * A reader of one array element or object that may fail with the codec's checked exception.
     *
     * @param <E> the element type
     * @param <T> the parsed type
     */
    @FunctionalInterface
    interface ElementReader<E, T> {

        /**
         * @param element the element to read
         * @return the parsed value
         * @throws IOException on format failure
         */
        T read(E element) throws IOException;
    }

    /**
     * Refuses a member the record's closed schema does not know.
     *
     * @param object the record's map
     * @param known  the known member names
     * @param type   the record's type name for the refusal
     * @throws IOException if the map carries an unknown member
     */
    static void requireOnlyKnown(Map<String, Object> object, Set<String> known, String type) throws IOException {
        var unknown = object.keySet().stream().filter(key -> !known.contains(key)).findFirst();
        if (unknown.isPresent()) {
            throw new JsonFormatException("Unknown field in " + type + ": " + unknown.get());
        }
    }

    /**
     * Reads the named field as a list of strings. An absent field yields an empty list; a present
     * {@code null}, a non-array value, or a structured element fails closed.
     *
     * @param object the record's map
     * @param key    the field key
     * @return the strings, never {@code null}
     * @throws IOException if a present value is not a scalar array
     */
    static List<String> readStrings(Map<String, Object> object, String key) throws IOException {
        if (!object.containsKey(key)) {
            return List.of();
        }
        var raw = object.get(key);
        var array = raw != null ? JsonTree.asArray(raw) : null;
        if (array == null) {
            throw new JsonFormatException("Expected an array for " + key);
        }
        var structured = array.stream().anyMatch(item -> item instanceof Map<?, ?> || item instanceof List<?>);
        if (structured) {
            throw new JsonFormatException("Expected scalars for " + key);
        }
        return array.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    /**
     * Reads the named field as a list of objects. An absent field yields an empty list; a present
     * {@code null}, a non-array value, or an element that is not an object fails closed.
     *
     * @param object the record's map
     * @param key    the field key
     * @return the object maps, never {@code null}
     * @throws IOException if a present value is not an object array
     */
    static List<Map<String, Object>> readObjects(Map<String, Object> object, String key) throws IOException {
        if (!object.containsKey(key)) {
            return List.of();
        }
        var raw = object.get(key);
        var array = raw != null ? JsonTree.asArray(raw) : null;
        if (array == null) {
            throw new JsonFormatException("Expected an array for " + key);
        }
        return readEach(array, element -> {
            var entry = JsonTree.asObject(element);
            if (entry == null) {
                throw new JsonFormatException("Expected an object in " + key);
            }
            return entry;
        });
    }

    /**
     * Reads every element of an array through a reader, failing closed on the first malformed entry.
     *
     * @param elements the array elements
     * @param reader   the reader of one element
     * @param <E>      the element type
     * @param <T>      the parsed type
     * @return the parsed values, in order
     * @throws IOException if a reader fails
     */
    static <E, T> List<T> readEach(List<E> elements, ElementReader<E, T> reader) throws IOException {
        try {
            return elements.stream().map(element -> {
                try {
                    return reader.read(element);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * @param map the object
     * @param key the field key
     * @return the boolean value, or {@code false}
     */
    static boolean bool(Map<String, Object> map, String key) {
        return Boolean.TRUE.equals(JsonTree.bool(map, key));
    }

    /**
     * @param map the object
     * @param key the field key
     * @return the integer value, or {@code null}
     */
    static Integer optInteger(Map<String, Object> map, String key) {
        return JsonTree.integer(map, key);
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
     * @param value a string
     * @return {@code true} if the value is neither {@code null} nor blank
     */
    static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
