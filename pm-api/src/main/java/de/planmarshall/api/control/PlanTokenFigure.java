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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One plan's token figure within an epic's tokens (design <em>Epic View</em>, the figure per plan).
 *
 * @param planId the plan identifier
 * @param figure the plan's token figure
 */
public record PlanTokenFigure(String planId, TokenFigure figure) {

    public PlanTokenFigure {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(figure, "figure");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("figure", figure.toMap());
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
     * @return the parsed entry
     * @throws IOException if a required field is missing
     */
    public static PlanTokenFigure fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        var figure = JsonTree.asObject(map.get("figure"));
        if (planId == null || figure == null) {
            throw new JsonFormatException("Missing required fields for PlanTokenFigure: plan_id, figure");
        }
        return new PlanTokenFigure(planId, TokenFigure.fromMap(figure));
    }

    /**
     * @param json the JSON object string
     * @return the parsed entry
     * @throws IOException if the value is not an object
     */
    public static PlanTokenFigure fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PlanTokenFigure");
        }
        return fromMap(obj);
    }
}
