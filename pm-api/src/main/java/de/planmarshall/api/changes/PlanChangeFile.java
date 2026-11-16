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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One file of a plan's change with its hunks, as {@code GET /api/v1/plans/{plan_id}/changes/file}
 * answers it ({@code 01-processes-and-transport.adoc#plan-changes}).
 * <p>
 * A binary file answers no lines. A file whose lines exceed the runtime's compiled bound answers the
 * hunks up to it and {@code truncated} with the count of the lines left out.
 *
 * @param path         the file's path in the capture
 * @param hunks        the file's hunks in order, empty for a binary file
 * @param binary       whether the file is binary
 * @param truncated    whether the runtime left lines out of the answer
 * @param linesOmitted the count of the lines left out, {@code 0} when nothing was cut
 */
public record PlanChangeFile(
        String path,
        List<PlanChangeHunk> hunks,
        boolean binary,
        boolean truncated,
        int linesOmitted
) {

    private static final String FIELD_PATH = "path";
    private static final String FIELD_HUNKS = "hunks";
    private static final String FIELD_BINARY = "binary";
    private static final String FIELD_TRUNCATED = "truncated";
    private static final String FIELD_LINES_OMITTED = "lines_omitted";

    public PlanChangeFile {
        Objects.requireNonNull(path, "path");
        if (linesOmitted < 0) {
            throw new IllegalArgumentException("A count of omitted lines is never negative");
        }
        hunks = hunks != null ? List.copyOf(hunks) : List.of();
        if (binary && !hunks.isEmpty()) {
            throw new IllegalArgumentException("A binary file carries no hunks");
        }
        if (truncated && linesOmitted == 0) {
            throw new IllegalArgumentException("A cut file names the lines it left out");
        }
        if (!truncated && linesOmitted > 0) {
            throw new IllegalArgumentException("A file that left nothing out names no omitted line");
        }
    }

    /**
     * @return the map representation, in the order of the wire contract
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PATH, path);
        var rows = new ArrayList<Map<String, Object>>(hunks.size());
        for (var hunk : hunks) {
            rows.add(hunk.toMap());
        }
        map.put(FIELD_HUNKS, rows);
        map.put(FIELD_BINARY, binary);
        map.put(FIELD_TRUNCATED, truncated);
        if (truncated) {
            map.put(FIELD_LINES_OMITTED, linesOmitted);
        }
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
     * @throws IOException if a required member is missing or a hunk does not parse
     */
    public static PlanChangeFile fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var path = JsonTree.string(map, FIELD_PATH);
        if (path == null) {
            throw new JsonFormatException("Missing " + FIELD_PATH + " in a plan's change of a file");
        }
        var hunks = new ArrayList<PlanChangeHunk>();
        var rows = JsonTree.asArray(map.get(FIELD_HUNKS));
        if (rows == null && map.containsKey(FIELD_HUNKS)) {
            throw new JsonFormatException(FIELD_HUNKS + " must be an array");
        }
        if (rows != null) {
            for (var row : rows) {
                var hunk = JsonTree.asObject(row);
                if (hunk == null) {
                    throw new JsonFormatException("A hunk is a JSON object");
                }
                hunks.add(PlanChangeHunk.fromMap(hunk));
            }
        }
        var linesOmitted = map.containsKey(FIELD_LINES_OMITTED)
                ? (int) JsonTree.integralNumber(map, FIELD_LINES_OMITTED, 0, Integer.MAX_VALUE)
                : 0;
        try {
            return new PlanChangeFile(
                    path,
                    hunks,
                    PlanChangedFile.requireBoolean(map, FIELD_BINARY),
                    PlanChangedFile.requireBoolean(map, FIELD_TRUNCATED),
                    linesOmitted);
        } catch (IllegalArgumentException incoherent) {
            // A wire answer that contradicts itself is no answer: it is refused like a malformed one.
            throw new JsonFormatException(incoherent.getMessage(), incoherent);
        }
    }

    /**
     * @param json the JSON object
     * @return the parsed file
     * @throws IOException on parse failure or a missing member
     */
    public static PlanChangeFile fromJson(String json) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(json));
        if (map == null) {
            throw new JsonFormatException("Expected a JSON object for a plan's change of a file");
        }
        return fromMap(map);
    }
}
