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
 * One hunk of a file's change: where it starts and ends on both sides, and its lines.
 *
 * @param oldStart the hunk's first line in the base
 * @param oldLen   the hunk's line count in the base
 * @param newStart the hunk's first line in the capture
 * @param newLen   the hunk's line count in the capture
 * @param lines    the hunk's lines in order
 */
public record PlanChangeHunk(
        int oldStart,
        int oldLen,
        int newStart,
        int newLen,
        List<PlanChangeLine> lines
) {

    private static final String FIELD_OLD_START = "old_start";
    private static final String FIELD_OLD_LEN = "old_len";
    private static final String FIELD_NEW_START = "new_start";
    private static final String FIELD_NEW_LEN = "new_len";
    private static final String FIELD_LINES = "lines";

    public PlanChangeHunk {
        if (oldStart < 0 || oldLen < 0 || newStart < 0 || newLen < 0) {
            throw new IllegalArgumentException("A hunk's position and length are never negative");
        }
        lines = lines != null ? List.copyOf(lines) : List.of();
    }

    /**
     * @return the map representation, in the order of the wire contract
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_OLD_START, oldStart);
        map.put(FIELD_OLD_LEN, oldLen);
        map.put(FIELD_NEW_START, newStart);
        map.put(FIELD_NEW_LEN, newLen);
        var rows = new ArrayList<Map<String, Object>>(lines.size());
        for (var line : lines) {
            rows.add(line.toMap());
        }
        map.put(FIELD_LINES, rows);
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
     * @return the parsed hunk
     * @throws IOException if a required member is missing or a line does not parse
     */
    public static PlanChangeHunk fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var lines = new ArrayList<PlanChangeLine>();
        var rows = JsonTree.asArray(map.get(FIELD_LINES));
        if (rows != null) {
            for (var row : rows) {
                var line = JsonTree.asObject(row);
                if (line == null) {
                    throw new JsonFormatException("A changed line is a JSON object");
                }
                lines.add(PlanChangeLine.fromMap(line));
            }
        }
        return new PlanChangeHunk(
                requireCount(map, FIELD_OLD_START),
                requireCount(map, FIELD_OLD_LEN),
                requireCount(map, FIELD_NEW_START),
                requireCount(map, FIELD_NEW_LEN),
                lines);
    }

    private static int requireCount(Map<String, Object> map, String field) throws IOException {
        var value = JsonTree.integer(map, field);
        if (value == null) {
            throw new JsonFormatException("Missing " + field + " in a hunk");
        }
        return value;
    }
}
