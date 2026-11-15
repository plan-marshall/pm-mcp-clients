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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One changed file of a plan's change, as the list of the capture answers it
 * ({@code 01-processes-and-transport.adoc#plan-changes}).
 * <p>
 * The path, and the path the file was renamed from, are written by a worker or come from the
 * project: a client shows them as plain text, never as markup and never as a link it follows.
 *
 * @param path        the file's path in the capture
 * @param renamedFrom the path the file was renamed from, or {@code null} for any other status
 * @param status      whether the file was added, modified, deleted or renamed
 * @param added       the lines the change added
 * @param removed     the lines the change removed
 * @param binary      whether the file is binary, and therefore has no lines
 * @param unplanned   whether the file lies beyond the plan's declared footprint
 */
public record PlanChangedFile(
        String path,
        @Nullable String renamedFrom,
        ChangedFileStatus status,
        int added,
        int removed,
        boolean binary,
        boolean unplanned
) {

    private static final String FIELD_PATH = "path";
    private static final String FIELD_RENAMED_FROM = "renamed_from";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ADDED = "added";
    private static final String FIELD_REMOVED = "removed";
    private static final String FIELD_BINARY = "binary";
    private static final String FIELD_UNPLANNED = "unplanned";

    public PlanChangedFile {
        Objects.requireNonNull(path, FIELD_PATH);
        Objects.requireNonNull(status, FIELD_STATUS);
        if (added < 0 || removed < 0) {
            throw new IllegalArgumentException("A count of changed lines is never negative");
        }
    }

    /**
     * @return the map representation, in the order of the wire contract
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PATH, path);
        if (renamedFrom != null) {
            map.put(FIELD_RENAMED_FROM, renamedFrom);
        }
        map.put(FIELD_STATUS, status.wire());
        map.put(FIELD_ADDED, added);
        map.put(FIELD_REMOVED, removed);
        map.put(FIELD_BINARY, binary);
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
     * @param map the map representation
     * @return the parsed file
     * @throws IOException if a required member is missing or not of its type
     */
    public static PlanChangedFile fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var path = JsonTree.string(map, FIELD_PATH);
        if (path == null) {
            throw new JsonFormatException("Missing " + FIELD_PATH + " in a changed file");
        }
        var statusWire = JsonTree.string(map, FIELD_STATUS);
        if (statusWire == null) {
            throw new JsonFormatException("Missing " + FIELD_STATUS + " in a changed file");
        }
        var status = ChangedFileStatus.fromWireOrNull(statusWire);
        if (status == null) {
            throw new JsonFormatException("Unknown " + FIELD_STATUS + " of a changed file: " + statusWire);
        }
        var added = JsonTree.integralNumber(map, FIELD_ADDED, 0, Integer.MAX_VALUE);
        var removed = JsonTree.integralNumber(map, FIELD_REMOVED, 0, Integer.MAX_VALUE);
        return new PlanChangedFile(
                path,
                JsonTree.string(map, FIELD_RENAMED_FROM),
                status,
                (int) added,
                (int) removed,
                Boolean.TRUE.equals(map.get(FIELD_BINARY)),
                Boolean.TRUE.equals(map.get(FIELD_UNPLANNED)));
    }
}
