/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Shared readers of the resource records: the object envelope of {@code fromJson}/{@code readFrom}
 * and the strict string-array reader. The helpers keep the records' fail-closed behaviour in one
 * place instead of repeating it in each record.
 */
final class ResourceJson {

    private ResourceJson() {
        // Utility class
    }

    /**
     * Parses one JSON object of a resource.
     *
     * @param json     the JSON text
     * @param resource the resource name for the diagnostic
     * @return the object
     * @throws IOException if the text is no JSON object
     */
    static Map<String, Object> objectOf(String json, String resource) throws IOException {
        return objectOf(JsonTree.parse(json), resource);
    }

    /**
     * Reads one JSON object of a resource from a streaming parser.
     *
     * @param parser   the parser
     * @param resource the resource name for the diagnostic
     * @return the object
     * @throws IOException if the next value is no JSON object
     */
    static Map<String, Object> objectOf(JsonParser parser, String resource) throws IOException {
        return objectOf(JsonTree.readValue(parser), resource);
    }

    /**
     * Reads a string array under {@code field} or its alias. A present value that is no array of
     * strings is refused rather than coerced to an empty or partial list.
     *
     * @param map   the object
     * @param field the member name
     * @param alias the alias read when {@code field} is absent
     * @return the strings, empty when neither member is present
     * @throws IOException if the value is no array of strings
     */
    static List<String> strings(Map<String, Object> map, String field, String alias) throws IOException {
        var value = map.get(field);
        if (value == null) {
            value = map.get(alias);
        }
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new JsonFormatException(field + " must be a JSON array of strings");
        }
        var strings = new ArrayList<String>(list.size());
        for (var element : list) {
            if (!(element instanceof String text)) {
                throw new JsonFormatException(field + " must hold only strings");
            }
            strings.add(text);
        }
        return strings;
    }

    /**
     * @param map   the object
     * @param field the member name
     * @param alias the alias read when {@code field} is absent
     * @return the first non-null string, or {@code null}
     */
    static @Nullable String firstString(Map<String, Object> map, String field, String alias) {
        var value = JsonTree.string(map, field);
        return value != null ? value : JsonTree.string(map, alias);
    }

    private static Map<String, Object> objectOf(Object tree, String resource) throws JsonFormatException {
        if (tree instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var object = (Map<String, Object>) map;
            return object;
        }
        throw new JsonFormatException("Expected a JSON object for " + resource);
    }
}
