/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.git;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One file a pull request or the checkout's local changes touch, with its line counts
 * (UC-STA-7, UC-STA-9).
 * <p>
 * A file that comes from outside the machine is marked {@link #untrusted()} unless the operator is
 * its author; untrusted files are stored but never trusted as instructions.
 *
 * @param path             the file's path relative to the checkout
 * @param additions        the lines added
 * @param deletions        the lines removed
 * @param untrusted        whether the file's content comes from outside the machine and is unscreened
 * @param operatorAuthored whether the operator wrote the change
 */
public record ChangedFile(
        String path,
        int additions,
        int deletions,
        boolean untrusted,
        boolean operatorAuthored
) {

    private static final String FIELD_PATH = "path";
    private static final String FIELD_ADDITIONS = "additions";
    private static final String FIELD_DELETIONS = "deletions";
    private static final String FIELD_UNTRUSTED = "untrusted";
    private static final String FIELD_OPERATOR_AUTHORED = "operator_authored";

    public ChangedFile {
        Objects.requireNonNull(path, "path");
        if (additions < 0) {
            additions = 0;
        }
        if (deletions < 0) {
            deletions = 0;
        }
    }

    /**
     * @return the number of lines this file changed in total
     */
    public int changedLines() {
        return additions + deletions;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PATH, path);
        map.put(FIELD_ADDITIONS, additions);
        map.put(FIELD_DELETIONS, deletions);
        map.put(FIELD_UNTRUSTED, untrusted);
        map.put(FIELD_OPERATOR_AUTHORED, operatorAuthored);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the input map
     * @return the parsed file
     * @throws IOException if the path is missing
     */
    public static ChangedFile fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var path = JsonTree.string(map, FIELD_PATH);
        if (path == null) {
            throw new JsonFormatException("Missing required field for ChangedFile: path");
        }
        var additions = JsonTree.integer(map, FIELD_ADDITIONS);
        var deletions = JsonTree.integer(map, FIELD_DELETIONS);
        return new ChangedFile(path, additions != null ? additions : 0,
                deletions != null ? deletions : 0,
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_UNTRUSTED)),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_OPERATOR_AUTHORED)));
    }

    /**
     * @param json the JSON object string
     * @return the parsed file
     * @throws IOException if the value is not an object or the path is missing
     */
    public static ChangedFile fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ChangedFile");
        }
        return fromMap(obj);
    }
}
