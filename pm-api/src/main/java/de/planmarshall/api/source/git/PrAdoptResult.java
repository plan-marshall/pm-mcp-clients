/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.git;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The result of adopting a pull request: a plan in the {@code adopt} lane now runs on its branch
 * (UC-STA-7).
 *
 * @param planId the created plan's id
 * @param lane   the lane the plan runs in ({@code adopt})
 * @param branch the branch the plan took over
 * @param status the state the adoption reached ({@code adopted})
 */
public record PrAdoptResult(
        String planId,
        SourceLane lane,
        String branch,
        String status
) {

    /** The status a successful adoption reaches. */
    public static final String STATUS_ADOPTED = "adopted";

    private static final String FIELD_PLAN_ID = "plan_id";
    private static final String FIELD_LANE = "lane";
    private static final String FIELD_BRANCH = "branch";
    private static final String FIELD_STATUS = "status";

    public PrAdoptResult {
        Objects.requireNonNull(planId, "planId");
        lane = lane == null ? SourceLane.ADOPT : lane;
        branch = branch == null ? "" : branch;
        status = status == null || status.isBlank() ? STATUS_ADOPTED : status;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_LANE, lane.wireValue());
        map.put(FIELD_BRANCH, branch);
        map.put(FIELD_STATUS, status);
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
     * @return the parsed result
     * @throws IOException if a required field is missing
     */
    public static PrAdoptResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, FIELD_PLAN_ID);
        if (planId == null) {
            throw new JsonFormatException("Missing required field for PrAdoptResult: plan_id");
        }
        var lane = JsonTree.string(map, FIELD_LANE);
        return new PrAdoptResult(planId, lane != null ? SourceLane.fromWire(lane) : SourceLane.ADOPT,
                JsonTree.string(map, FIELD_BRANCH), JsonTree.string(map, FIELD_STATUS));
    }

    /**
     * @param json the JSON object string
     * @return the parsed result
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static PrAdoptResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PrAdoptResult");
        }
        return fromMap(obj);
    }
}
