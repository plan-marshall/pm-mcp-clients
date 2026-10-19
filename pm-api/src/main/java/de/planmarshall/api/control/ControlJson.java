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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.UtilityClass;

import de.planmarshall.api.json.JsonTree;

/**
 * Shared JSON tree helpers for the control records' codecs.
 */
@UtilityClass
final class ControlJson {

    /**
     * Reads a JSON array of scalars as a list of strings.
     *
     * @param raw the raw tree value
     * @return the strings, never {@code null}
     */
    static List<String> readStrings(Object raw) {
        var array = JsonTree.asArray(raw);
        if (array == null) {
            return List.of();
        }
        return array.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    /**
     * Reads a JSON array of objects as a list of maps.
     *
     * @param raw the raw tree value
     * @return the object maps, never {@code null}
     */
    static List<Map<String, Object>> readObjects(Object raw) {
        var array = JsonTree.asArray(raw);
        if (array == null) {
            return List.of();
        }
        var objects = new ArrayList<Map<String, Object>>();
        for (var item : array) {
            var object = JsonTree.asObject(item);
            if (object != null) {
                objects.add(object);
            }
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
