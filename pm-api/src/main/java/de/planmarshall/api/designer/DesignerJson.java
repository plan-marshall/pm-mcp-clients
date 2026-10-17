/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.designer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

import de.planmarshall.api.json.JsonTree;

/**
 * Shared coercion helpers for the process-designer codecs (UC-PLN-3). Package-private: it is an
 * implementation detail of the {@code designer} records and not part of the published contract.
 */
@UtilityClass
final class DesignerJson {

    /**
     * Reads an array member as a list of strings, ignoring null and non-scalar entries.
     *
     * @param object the object, may be {@code null}
     * @param name   the member name
     * @return the string list, never {@code null}
     */
    static List<String> stringList(Map<String, Object> object, String name) {
        if (object == null || !(object.get(name) instanceof List<?> list)) {
            return List.of();
        }
        var result = new ArrayList<String>(list.size());
        for (var item : list) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Reads an array member as a list of objects, dropping entries that are not objects.
     *
     * @param object the object, may be {@code null}
     * @param name   the member name
     * @return the object list in source order, never {@code null}
     */
    static List<Map<String, Object>> objectList(Map<String, Object> object, String name) {
        if (object == null || !(object.get(name) instanceof List<?> list)) {
            return List.of();
        }
        var result = new ArrayList<Map<String, Object>>(list.size());
        for (var item : list) {
            var map = JsonTree.asObject(item);
            if (map != null) {
                result.add(map);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Reads a string member, falling back to a camelCase spelling when the snake_case one is absent.
     *
     * @param object  the object, may be {@code null}
     * @param name    the snake_case member name
     * @param altName the alternative member name
     * @return the string, or {@code null}
     */
    static String string(Map<String, Object> object, String name, String altName) {
        var value = JsonTree.string(object, name);
        return value != null ? value : JsonTree.string(object, altName);
    }
}
