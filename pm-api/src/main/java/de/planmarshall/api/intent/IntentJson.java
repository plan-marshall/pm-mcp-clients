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
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * The streaming codec core of the intent contract: the {@link JsonGenerator} and {@link JsonParser}
 * helpers every record's {@code writeTo}/{@code readFrom} pair uses.
 * <p>
 * The codec is deliberately databind-free. Writing goes straight to the generator; reading parses one
 * value with the streaming tree of {@link JsonTree} and checks the record's closed schema before a
 * constructor sees it. An absent optional member yields {@code null}; a present member of the wrong
 * type, an unknown member, and an out-of-range number are refused with a {@link JsonFormatException}.
 */
@UtilityClass
final class IntentJson {

    /**
     * Writes one element of a JSON array.
     *
     * @param <T> the element type
     */
    @FunctionalInterface
    interface ArrayWriter<T> {

        /**
         * @param value     the element
         * @param generator the generator, positioned inside the array
         * @throws IOException on write failure
         */
        void write(T value, JsonGenerator generator) throws IOException;
    }

    /**
     * Reads one element of a JSON array.
     *
     * @param <E> the element type
     * @param <T> the parsed type
     */
    @FunctionalInterface
    interface ElementReader<E, T> {

        /**
         * @param element the element
         * @return the parsed value
         * @throws IOException on format failure
         */
        T read(E element) throws IOException;
    }

    /**
     * @param object the object
     * @param known  the member names of the record's closed schema
     * @param type   the record's type name for the refusal
     * @throws IOException if the object carries an unknown member
     */
    static void requireOnlyKnown(Map<String, Object> object, Set<String> known, String type) throws IOException {
        var unknown = object.keySet().stream().filter(key -> !known.contains(key)).findFirst();
        if (unknown.isPresent()) {
            throw new JsonFormatException("Unknown field in " + type + ": " + unknown.get());
        }
    }

    /**
     * Reads a required, non-blank string member.
     *
     * @param object the object
     * @param key    the member name
     * @param type   the record's type name for the refusal
     * @return the value
     * @throws IOException if the member is absent, blank, or not a string
     */
    static String requireText(Map<String, Object> object, String key, String type) throws IOException {
        var value = object.get(key);
        if (value == null) {
            throw new JsonFormatException("Missing " + key + " in " + type);
        }
        if (!(value instanceof String text)) {
            throw new JsonFormatException("Not a string for " + key + " in " + type);
        }
        if (text.isBlank()) {
            throw new JsonFormatException("Blank " + key + " in " + type);
        }
        return text;
    }

    /**
     * Reads an optional string member. A member absent or {@code null} yields {@code null}; a member
     * of another type is refused.
     *
     * @param object the object
     * @param key    the member name
     * @param type   the record's type name for the refusal
     * @return the value, or {@code null}
     * @throws IOException if the member is present and not a string
     */
    static @Nullable String optionalText(Map<String, Object> object, String key, String type) throws IOException {
        var value = object.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new JsonFormatException("Not a string for " + key + " in " + type);
        }
        return text.isBlank() ? null : text;
    }

    /**
     * Reads a required integral number within the given bounds.
     *
     * @param object  the object
     * @param key     the member name
     * @param minimum the inclusive lower bound
     * @param maximum the inclusive upper bound
     * @param type    the record's type name for the refusal
     * @return the value
     * @throws IOException if the member is missing, has a fraction, or is out of range
     */
    static int requireInt(Map<String, Object> object, String key, int minimum, int maximum, String type)
            throws IOException {
        try {
            return Math.toIntExact(JsonTree.integralNumber(object, key, minimum, maximum));
        } catch (ArithmeticException _) {
            throw new JsonFormatException("Out-of-range number for " + key + " in " + type);
        }
    }

    /**
     * Reads an optional integral number. A member absent or {@code null} yields {@code -1}; a member
     * with a fraction or out of range is refused.
     *
     * @param object  the object
     * @param key     the member name
     * @param minimum the inclusive lower bound
     * @param maximum the inclusive upper bound
     * @param type    the record's type name for the refusal
     * @return the value, or {@code -1}
     * @throws IOException if the member is present and not an exact in-range integral number
     */
    static int optionalInt(Map<String, Object> object, String key, int minimum, int maximum, String type)
            throws IOException {
        if (!object.containsKey(key) || object.get(key) == null) {
            return -1;
        }
        return requireInt(object, key, minimum, maximum, type);
    }

    /**
     * Reads a required finite floating-point number.
     *
     * @param object the object
     * @param key    the member name
     * @param type   the record's type name for the refusal
     * @return the value
     * @throws IOException if the member is missing, not a number, or not finite
     */
    static double requireDouble(Map<String, Object> object, String key, String type) throws IOException {
        var value = object.get(key);
        if (value == null) {
            throw new JsonFormatException("Missing " + key + " in " + type);
        }
        final double number;
        try {
            number = switch (value) {
                case JsonNumber(String text) -> Double.parseDouble(text);
                case Number raw -> raw.doubleValue();
                default -> throw new JsonFormatException("Not a number for " + key + " in " + type);
            };
        } catch (NumberFormatException _) {
            throw new JsonFormatException("Not a number for " + key + " in " + type);
        }
        if (!Double.isFinite(number)) {
            throw new JsonFormatException("Not a finite number for " + key + " in " + type);
        }
        return number;
    }

    /**
     * Reads a required boolean member.
     *
     * @param object the object
     * @param key    the member name
     * @param type   the record's type name for the refusal
     * @return the value
     * @throws IOException if the member is missing or not a boolean
     */
    static boolean requireBoolean(Map<String, Object> object, String key, String type) throws IOException {
        var value = object.get(key);
        if (!(value instanceof Boolean flag)) {
            throw new JsonFormatException("Missing or non-boolean " + key + " in " + type);
        }
        return flag;
    }

    /**
     * Reads the named field as a list of strings. An absent field yields an empty list; a present
     * {@code null}, a non-array value, or a structured element fails closed.
     *
     * @param object the object
     * @param key    the member name
     * @return the strings, never {@code null}
     * @throws IOException if a present value is not a scalar array
     */
    static List<String> readStrings(Map<String, Object> object, String key) throws IOException {
        if (!object.containsKey(key) || object.get(key) == null) {
            return List.of();
        }
        var array = JsonTree.asArray(object.get(key));
        if (array == null) {
            throw new JsonFormatException("Expected an array for " + key);
        }
        var result = new ArrayList<String>(array.size());
        for (var element : array) {
            if (!(element instanceof String text)) {
                throw new JsonFormatException("Expected strings for " + key);
            }
            result.add(text);
        }
        return List.copyOf(result);
    }

    /**
     * Reads the named field as a list of integral numbers.
     *
     * @param object the object
     * @param key    the member name
     * @param min    the inclusive lower bound of every element
     * @param max    the inclusive upper bound of every element
     * @return the numbers, never {@code null}
     * @throws IOException if a present value is not an array of in-range integral numbers
     */
    static List<Integer> readInts(Map<String, Object> object, String key, int min, int max) throws IOException {
        if (!object.containsKey(key) || object.get(key) == null) {
            return List.of();
        }
        var array = JsonTree.asArray(object.get(key));
        if (array == null) {
            throw new JsonFormatException("Expected an array for " + key);
        }
        var result = new ArrayList<Integer>(array.size());
        for (var element : array) {
            var probe = new LinkedHashMap<String, Object>();
            probe.put(key, element);
            result.add(requireInt(probe, key, min, max, "array " + key));
        }
        return List.copyOf(result);
    }

    /**
     * Reads the named field as a list of objects. An absent field yields an empty list; a present
     * {@code null}, a non-array value, or an element that is not an object fails closed.
     *
     * @param object the object
     * @param key    the member name
     * @return the object maps, never {@code null}
     * @throws IOException if a present value is not an object array
     */
    static List<Map<String, Object>> readObjects(Map<String, Object> object, String key) throws IOException {
        if (!object.containsKey(key) || object.get(key) == null) {
            return List.of();
        }
        var array = JsonTree.asArray(object.get(key));
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
     * Writes a member as an array of strings.
     *
     * @param generator the generator
     * @param field     the member name
     * @param values    the strings
     * @throws IOException on write failure
     */
    static void writeStrings(JsonGenerator generator, String field, List<String> values) throws IOException {
        generator.writeArrayFieldStart(field);
        for (var value : values) {
            generator.writeString(value);
        }
        generator.writeEndArray();
    }

    /**
     * Writes a member as an array of objects.
     *
     * @param generator the generator
     * @param field     the member name
     * @param values    the values
     * @param writer    the writer of one value
     * @param <T>       the value type
     * @throws IOException on write failure
     */
    static <T> void writeArray(JsonGenerator generator, String field, List<T> values, ArrayWriter<T> writer)
            throws IOException {
        generator.writeArrayFieldStart(field);
        for (var value : values) {
            writer.write(value, generator);
        }
        generator.writeEndArray();
    }

    /**
     * Writes an optional string member when it is present.
     *
     * @param generator the generator
     * @param field     the member name
     * @param value     the value, or {@code null}
     * @throws IOException on write failure
     */
    static void writeOptionalString(JsonGenerator generator, String field, @Nullable String value)
            throws IOException {
        if (value != null) {
            generator.writeStringField(field, value);
        }
    }

    /**
     * Reads one JSON object from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @param type   the record's type name for the refusal
     * @return the object map
     * @throws IOException if the value is not a JSON object
     */
    static Map<String, Object> readObject(JsonParser parser, String type) throws IOException {
        var object = JsonTree.asObject(JsonTree.readValue(parser));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + type);
        }
        return object;
    }
}
