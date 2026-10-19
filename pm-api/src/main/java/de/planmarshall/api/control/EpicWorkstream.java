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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One workstream of an epic's view: a named group of plan rows, open or closed (design
 * <em>Epic View</em>).
 *
 * @param id   the workstream id ({@code WS-1}, {@code WS-2}, ...)
 * @param name the workstream's name
 * @param open whether the workstream is shown open
 * @param rows the plan rows of the workstream, in the queue's order
 */
public record EpicWorkstream(
        String id,
        String name,
        boolean open,
        List<WorkRow> rows
) {

    public EpicWorkstream {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        rows = rows != null ? List.copyOf(rows) : List.of();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("name", name);
        map.put("open", open);
        map.put("rows", rows.stream().map(WorkRow::toMap).toList());
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
     * @return the parsed workstream
     * @throws IOException if a required field is missing
     */
    public static EpicWorkstream fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, "id");
        var name = JsonTree.string(map, "name");
        if (id == null || name == null) {
            throw new JsonFormatException("Missing required fields for EpicWorkstream: id, name");
        }
        return new EpicWorkstream(id, name, ControlJson.bool(map, "open"), readRows(map.get("rows")));
    }

    /**
     * @param json the JSON object string
     * @return the parsed workstream
     * @throws IOException if the value is not an object
     */
    public static EpicWorkstream fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EpicWorkstream");
        }
        return fromMap(obj);
    }

    private static List<WorkRow> readRows(@Nullable Object raw) throws IOException {
        var rows = new ArrayList<WorkRow>();
        for (var object : ControlJson.readObjects(raw)) {
            rows.add(WorkRow.fromMap(object));
        }
        return rows;
    }
}
