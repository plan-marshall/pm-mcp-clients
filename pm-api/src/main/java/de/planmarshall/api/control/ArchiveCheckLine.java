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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One line of an epic's archive check ({@code epic.archive-check}, design <em>Archiving: Is It
 * Through?</em>).
 *
 * <p>A line is a checkmark ({@link #passed()}), an open finding that holds the archive, or a note
 * of what is carried into it. A failing line leads to what is open through {@link #action()}.
 *
 * @param label  what is checked ({@code key}, resolved by the client's message catalogue)
 * @param passed whether the check passes
 * @param note   the count or detail shown beside the line, or {@code null}
 * @param action the act that leads to what is open ({@code review-findings}), or {@code null}
 */
public record ArchiveCheckLine(
        String label,
        boolean passed,
        @Nullable String note,
        @Nullable String action
) {

    public ArchiveCheckLine {
        Objects.requireNonNull(label, "label");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("label", label);
        map.put("passed", passed);
        if (note != null) {
            map.put("note", note);
        }
        if (action != null) {
            map.put("action", action);
        }
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
     * @return the parsed line
     * @throws IOException if a required field is missing
     */
    public static ArchiveCheckLine fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var label = JsonTree.string(map, "label");
        if (label == null) {
            throw new JsonFormatException("Missing label in ArchiveCheckLine");
        }
        return new ArchiveCheckLine(label, ControlJson.bool(map, "passed"),
                JsonTree.string(map, "note"), JsonTree.string(map, "action"));
    }

    /**
     * @param json the JSON object string
     * @return the parsed line
     * @throws IOException if the value is not an object
     */
    public static ArchiveCheckLine fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ArchiveCheckLine");
        }
        return fromMap(obj);
    }
}
