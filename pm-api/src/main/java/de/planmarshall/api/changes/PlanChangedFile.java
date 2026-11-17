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
        if (status == ChangedFileStatus.RENAMED && renamedFrom == null) {
            throw new IllegalArgumentException("A renamed file names the path it came from");
        }
        if (status != ChangedFileStatus.RENAMED && renamedFrom != null) {
            throw new IllegalArgumentException("Only a renamed file names the path it came from");
        }
        if (binary && (added > 0 || removed > 0)) {
            throw new IllegalArgumentException("A binary file carries no counts of lines");
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
        try {
            return new PlanChangedFile(
                    path,
                    JsonTree.string(map, FIELD_RENAMED_FROM),
                    status,
                    (int) added,
                    (int) removed,
                    requireBoolean(map, FIELD_BINARY),
                    requireBoolean(map, FIELD_UNPLANNED));
        } catch (IllegalArgumentException incoherent) {
            // A wire answer that contradicts itself is no answer: it is refused like a malformed one.
            throw new JsonFormatException(incoherent.getMessage(), incoherent);
        }
    }

    /**
     * @param map   the map representation
     * @param field the mark's field
     * @return the mark, which the wire must carry as a JSON boolean
     * @throws IOException when the mark is missing or is no boolean
     */
    static boolean requireBoolean(Map<String, Object> map, String field) throws IOException {
        if (map.get(field) instanceof Boolean mark) {
            return mark;
        }
        throw new JsonFormatException("Missing or non-boolean " + field + " in a change");
    }

    /**
     * @param json the JSON object
     * @return the parsed file
     * @throws IOException on parse failure or a missing member
     */
    public static PlanChangedFile fromJson(String json) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(json));
        if (map == null) {
            throw new JsonFormatException("Expected a JSON object for a changed file");
        }
        return fromMap(map);
    }
}
