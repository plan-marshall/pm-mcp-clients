/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The tree helpers the chat records' codecs share, on {@link JsonTree} alone (no
 * {@code jackson-databind}).
 * <p>
 * Every reader fails closed: an unknown member, a missing required member, a member of the wrong
 * type, and an unknown value of a closed list are refused with a {@link JsonFormatException}; an absent
 * optional member reads as {@code null} or as an empty list, never as a default value.
 */
@UtilityClass
final class ChatJson {

    /**
     * Reads one element of a JSON array.
     *
     * @param <T> the parsed type
     */
    @FunctionalInterface
    interface ElementReader<T> {

        /**
         * @param object the element's object
         * @return the parsed value
         * @throws IOException on format failure
         */
        T read(Map<String, Object> object) throws IOException;
    }

    /**
     * @param object the object
     * @param known  the member names of the record's closed schema
     * @param type   the record's type name for the refusal
     * @throws IOException if the object carries an unknown member
     */
    static void requireOnlyKnown(Map<String, Object> object, Set<String> known, String type) throws IOException {
        for (var key : object.keySet()) {
            if (!known.contains(key)) {
                throw new JsonFormatException("Unknown field in " + type + ": " + key);
            }
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
     * Reads a required string member that may be empty.
     *
     * @param object the object
     * @param key    the member name
     * @param type   the record's type name for the refusal
     * @return the value
     * @throws IOException if the member is absent or not a string
     */
    static String requireString(Map<String, Object> object, String key, String type) throws IOException {
        var value = object.get(key);
        if (!(value instanceof String text)) {
            throw new JsonFormatException("Missing or non-string " + key + " in " + type);
        }
        return text;
    }

    /**
     * Reads an optional string member: absent or {@code null} reads as {@code null}.
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
        return text;
    }

    /**
     * Reads a required boolean member.
     *
     * @param object the object
     * @param key    the member name
     * @param type   the record's type name for the refusal
     * @return the value
     * @throws IOException if the member is absent or not a boolean
     */
    static boolean requireBoolean(Map<String, Object> object, String key, String type) throws IOException {
        if (!(object.get(key) instanceof Boolean flag)) {
            throw new JsonFormatException("Missing or non-boolean " + key + " in " + type);
        }
        return flag;
    }

    /**
     * Reads a required member of a closed list.
     *
     * @param object the object
     * @param key    the member name
     * @param lookup the closed list's lookup, {@code null} for a value it does not name
     * @param type   the record's type name for the refusal
     * @param <E>    the closed list
     * @return the value
     * @throws IOException if the member is absent or names no value of the list
     */
    static <E> E requireEnum(Map<String, Object> object, String key, Function<String, @Nullable E> lookup,
                             String type) throws IOException {
        var text = requireText(object, key, type);
        var value = lookup.apply(text);
        if (value == null) {
            throw new JsonFormatException("Unknown " + key + " in " + type + ": " + text);
        }
        return value;
    }

    /**
     * Reads an optional member of a closed list: absent or {@code null} reads as {@code null}.
     *
     * @param object the object
     * @param key    the member name
     * @param lookup the closed list's lookup, {@code null} for a value it does not name
     * @param type   the record's type name for the refusal
     * @param <E>    the closed list
     * @return the value, or {@code null}
     * @throws IOException if the member is present and names no value of the list
     */
    static <E> @Nullable E optionalEnum(Map<String, Object> object, String key,
                                        Function<String, @Nullable E> lookup, String type) throws IOException {
        if (object.get(key) == null) {
            return null;
        }
        return requireEnum(object, key, lookup, type);
    }

    /**
     * Reads a required nested record.
     *
     * @param object the object
     * @param key    the member name
     * @param reader the nested record's reader
     * @param type   the record's type name for the refusal
     * @param <T>    the nested record
     * @return the nested record
     * @throws IOException if the member is absent, not an object, or malformed
     */
    static <T> T requireRecord(Map<String, Object> object, String key, ElementReader<T> reader, String type)
            throws IOException {
        var member = JsonTree.objectOrNull(object, key, type);
        if (member == null) {
            throw new JsonFormatException("Missing " + key + " in " + type);
        }
        return reader.read(member);
    }

    /**
     * Reads an optional nested record: absent or {@code null} reads as {@code null}.
     *
     * @param object the object
     * @param key    the member name
     * @param reader the nested record's reader
     * @param type   the record's type name for the refusal
     * @param <T>    the nested record
     * @return the nested record, or {@code null}
     * @throws IOException if the member is present and not an object or malformed
     */
    static <T> @Nullable T optionalRecord(Map<String, Object> object, String key, ElementReader<T> reader,
                                          String type) throws IOException {
        var member = JsonTree.objectOrNull(object, key, type);
        return member != null ? reader.read(member) : null;
    }

    /**
     * Reads a list of nested records: absent or {@code null} reads as an empty list.
     *
     * @param object the object
     * @param key    the member name
     * @param reader the element's reader
     * @param type   the record's type name for the refusal
     * @param <T>    the element type
     * @return the elements, in order, never {@code null}
     * @throws IOException if the member is not an array of objects or an element is malformed
     */
    static <T> List<T> readRecords(Map<String, Object> object, String key, ElementReader<T> reader, String type)
            throws IOException {
        if (object.get(key) == null) {
            return List.of();
        }
        var array = JsonTree.asArray(object.get(key));
        if (array == null) {
            throw new JsonFormatException("Expected an array for " + key + " in " + type);
        }
        var result = new ArrayList<T>(array.size());
        for (var element : array) {
            var entry = JsonTree.asObject(element);
            if (entry == null) {
                throw new JsonFormatException("Expected an object in " + key + " of " + type);
            }
            result.add(reader.read(entry));
        }
        return List.copyOf(result);
    }

    /**
     * Reads the arguments of a message key: absent or {@code null} reads as an empty map.
     *
     * @param object the object
     * @param key    the member name
     * @param type   the record's type name for the refusal
     * @return the arguments, never {@code null}
     * @throws IOException if the member is present and not an object
     */
    static Map<String, Object> readArgs(Map<String, Object> object, String key, String type) throws IOException {
        var args = JsonTree.objectOrNull(object, key, type);
        return args != null ? args : Map.of();
    }

    /**
     * Calls a record's constructor and turns its refusal into a format refusal.
     *
     * @param type    the record's type name for the refusal
     * @param factory the constructor call
     * @param <T>     the record
     * @return the record
     * @throws IOException if the constructor refuses the values
     */
    static <T> T construct(String type, Factory<T> factory) throws IOException {
        try {
            return factory.create();
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + type + ": " + e.getMessage(), e);
        }
    }

    /**
     * Parses a JSON object string into its tree map.
     *
     * @param json the JSON text
     * @param type the record's type name for the refusal
     * @return the object map
     * @throws IOException if the text is not a JSON object
     */
    static Map<String, Object> parseObject(String json, String type) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + type);
        }
        return object;
    }

    /**
     * A record's constructor call that may refuse its values.
     *
     * @param <T> the record
     */
    @FunctionalInterface
    interface Factory<T> {

        /**
         * @return the record
         * @throws IOException on format failure of a nested value
         */
        T create() throws IOException;
    }
}
