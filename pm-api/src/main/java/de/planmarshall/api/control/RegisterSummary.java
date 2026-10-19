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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The counts of one of an epic's registers (findings or watches), as the epic view shows them
 * (design <em>Epic View</em>).
 *
 * @param newCount how many are new since the last review
 * @param open     how many are open, not decided yet
 * @param kept     how many are kept
 */
public record RegisterSummary(int newCount, int open, int kept) {

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("new", newCount);
        map.put("open", open);
        map.put("kept", kept);
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
     * @return the parsed summary
     * @throws IOException if the value is not an object
     */
    public static RegisterSummary fromMap(Map<String, Object> map) throws IOException {
        if (map == null) {
            throw new JsonFormatException("Expected object for RegisterSummary");
        }
        var newCount = ControlJson.optInteger(map, "new");
        var open = ControlJson.optInteger(map, "open");
        var kept = ControlJson.optInteger(map, "kept");
        return new RegisterSummary(
                newCount != null ? newCount : 0,
                open != null ? open : 0,
                kept != null ? kept : 0);
    }

    /**
     * @param json the JSON object string
     * @return the parsed summary
     * @throws IOException if the value is not an object
     */
    public static RegisterSummary fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RegisterSummary");
        }
        return fromMap(obj);
    }
}
