/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.changes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.ChangeSize;

/**
 * The list of the files a plan changed, at the plan's last capture
 * ({@code 01-processes-and-transport.adoc#plan-changes}).
 * <p>
 * The list names the capture's {@code head} and {@code base_ref} and the files in path order; its
 * totals are derived from the files, never carried beside them, so that the list and the plan's
 * {@link ChangeSize} can only ever be the same measurement. The answer as a whole, refusals
 * included, is a {@link PlanChangesAnswer}.
 */
public record PlanChanges(String head, String baseRef, List<PlanChangedFile> files) {

    private static final String FIELD_HEAD = "head";
    private static final String FIELD_BASE_REF = "base_ref";
    private static final String FIELD_FILES = "files";

    public PlanChanges {
        Objects.requireNonNull(head, "head");
        Objects.requireNonNull(baseRef, "baseRef");
        files = files != null ? List.copyOf(files) : List.of();
    }

    /**
     * Adds up the files into the size of the change the plan carries.
     *
     * @param files the changed files
     * @return the size, with the files beyond the declared footprint counted as unplanned
     */
    public static ChangeSize totalsOf(List<PlanChangedFile> files) {
        var added = 0;
        var removed = 0;
        var unplanned = 0;
        for (var file : files) {
            added += file.added();
            removed += file.removed();
            if (file.unplanned()) {
                unplanned++;
            }
        }
        return new ChangeSize(files.size(), added, removed, unplanned);
    }

    /**
     * @return the size of the change, derived from the files
     */
    public ChangeSize totals() {
        return totalsOf(files);
    }

    /**
     * @return the map representation, in the order of the wire contract
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_HEAD, head);
        map.put(FIELD_BASE_REF, baseRef);
        var rows = new ArrayList<Map<String, Object>>(files.size());
        for (var file : files) {
            rows.add(file.toMap());
        }
        map.put(FIELD_FILES, rows);
        return map;
    }

    /**
     * @return the JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the map representation
     * @return the parsed list
     * @throws IOException if a required member is missing or a file does not parse
     */
    public static PlanChanges fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var head = JsonTree.string(map, FIELD_HEAD);
        var baseRef = JsonTree.string(map, FIELD_BASE_REF);
        if (head == null || baseRef == null) {
            throw new JsonFormatException("A plan's changes name their head and base_ref");
        }
        var files = new ArrayList<PlanChangedFile>();
        var paths = new HashSet<String>();
        var rows = JsonTree.asArray(map.get(FIELD_FILES));
        if (rows == null && map.containsKey(FIELD_FILES)) {
            throw new JsonFormatException(FIELD_FILES + " must be an array");
        }
        if (rows != null) {
            for (var row : rows) {
                var file = JsonTree.asObject(row);
                if (file == null) {
                    throw new JsonFormatException("A changed file is a JSON object");
                }
                var changed = PlanChangedFile.fromMap(file);
                if (!paths.add(changed.path())) {
                    throw new JsonFormatException("The list names " + changed.path() + " twice");
                }
                files.add(changed);
            }
        }
        return new PlanChanges(head, baseRef, files);
    }

    /**
     * @param json the JSON object
     * @return the parsed list
     * @throws IOException if the value is no object, a required member is missing, or a file does
     *                     not parse
     */
    public static PlanChanges fromJson(String json) throws IOException {
        var parsed = JsonTree.asObject(JsonTree.parse(json));
        if (parsed == null) {
            throw new JsonFormatException("Expected a JSON object for a plan's changes");
        }
        return fromMap(parsed);
    }
}
