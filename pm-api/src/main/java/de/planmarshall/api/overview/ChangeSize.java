/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;

/**
 * The size of a plan's change at its last capture (PM-ARCH-11).
 * <p>
 * Git measured it and the runtime serves it; a client adds nothing up. The size is absent before
 * the first capture, never a zero size, so a client shows the line only where the plan carries one.
 *
 * @param files      number of changed files
 * @param insertions number of inserted lines
 * @param deletions  number of deleted lines
 * @param unplanned  number of changed files beyond the plan's declared footprint
 */
public record ChangeSize(int files, int insertions, int deletions, int unplanned) {

    private static final String FIELD_FILES = "files";
    private static final String FIELD_INSERTIONS = "insertions";
    private static final String FIELD_DELETIONS = "deletions";
    private static final String FIELD_UNPLANNED = "unplanned";

    public ChangeSize {
        if (files < 0 || insertions < 0 || deletions < 0 || unplanned < 0) {
            throw new IllegalArgumentException("A size of change is never negative");
        }
    }

    /**
     * @return the map representation, with the four figures of the design's message key
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_FILES, files);
        map.put(FIELD_INSERTIONS, insertions);
        map.put(FIELD_DELETIONS, deletions);
        map.put(FIELD_UNPLANNED, unplanned);
        return map;
    }

    /**
     * @return the JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses the size from a JSON tree map.
     * <p>
     * The four figures stand together: a map that names no {@code files} figure, or that omits any
     * of the other three, is no measurement and answers {@code null}, never a partial size with an
     * unmeasured figure read as zero.
     *
     * @param map the map representation
     * @return the parsed size, or {@code null} when the map carries no complete measurement
     */
    public static @Nullable ChangeSize fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        var files = JsonTree.integer(map, FIELD_FILES);
        var insertions = JsonTree.integer(map, FIELD_INSERTIONS);
        var deletions = JsonTree.integer(map, FIELD_DELETIONS);
        var unplanned = JsonTree.integer(map, FIELD_UNPLANNED);
        if (files == null || insertions == null || deletions == null || unplanned == null) {
            // A partial capture is no capture: the size is shown whole or not at all.
            return null;
        }
        return new ChangeSize(files, insertions, deletions, unplanned);
    }

    /**
     * Parses the size from a JSON object.
     *
     * @param json the JSON object
     * @return the parsed size, or {@code null} for an empty object
     * @throws IOException on parse failure
     */
    public static @Nullable ChangeSize fromJson(String json) throws IOException {
        return fromMap(JsonTree.asObject(JsonTree.parse(json)));
    }
}
