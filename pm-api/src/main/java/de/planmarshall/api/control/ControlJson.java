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
     * Reads a JSON array of scalars as a list of strings. An absent value yields an empty list, but
     * a present value that is not an array fails closed.
     *
     * @param raw the raw tree value
     * @return the strings, never {@code null}
     * @throws IOException if a present value is not an array
     */
    static List<String> readStrings(Object raw) throws IOException {
        if (raw == null) {
            return List.of();
        }
        var array = JsonTree.asArray(raw);
        if (array == null) {
            throw new JsonFormatException("Expected an array in a control record");
        }
        return array.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    /**
     * Reads a JSON array of objects as a list of maps. An absent value yields an empty list, but a
     * present value that is not an array, or an element that is not an object, fails closed instead
     * of being dropped silently.
     *
     * @param raw the raw tree value
     * @return the object maps, never {@code null}
     * @throws IOException if a present value is not an array or an element is not an object
     */
    static List<Map<String, Object>> readObjects(Object raw) throws IOException {
        if (raw == null) {
            return List.of();
        }
        var array = JsonTree.asArray(raw);
        if (array == null) {
            throw new JsonFormatException("Expected an array in a control record");
        }
        var objects = new ArrayList<Map<String, Object>>();
        for (var item : array) {
            var object = JsonTree.asObject(item);
            if (object == null) {
                throw new JsonFormatException("Expected an object in a control array");
            }
            objects.add(object);
        }
        return objects;
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
