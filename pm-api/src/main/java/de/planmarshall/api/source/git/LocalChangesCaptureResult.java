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
 * The result of capturing the local changes: they were recorded as a snapshot commit and a plan in
 * the {@code land} lane takes them over (UC-STA-9).
 *
 * @param planId        the created plan's id
 * @param lane          the lane the plan runs in ({@code land})
 * @param snapshotCommit the SHA of the snapshot commit that recorded the changes
 * @param status        the state the capture reached ({@code captured})
 */
public record LocalChangesCaptureResult(
        String planId,
        SourceLane lane,
        String snapshotCommit,
        String status
) {

    /** The status a successful capture reaches. */
    public static final String STATUS_CAPTURED = "captured";

    private static final String FIELD_PLAN_ID = "plan_id";
    private static final String FIELD_LANE = "lane";
    private static final String FIELD_SNAPSHOT_COMMIT = "snapshot_commit";
    private static final String FIELD_STATUS = "status";

    public LocalChangesCaptureResult {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(snapshotCommit, "snapshotCommit");
        lane = lane == null ? SourceLane.LAND : lane;
        status = status == null || status.isBlank() ? STATUS_CAPTURED : status;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_LANE, lane.wireValue());
        map.put(FIELD_SNAPSHOT_COMMIT, snapshotCommit);
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
    public static LocalChangesCaptureResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, FIELD_PLAN_ID);
        var snapshotCommit = JsonTree.string(map, FIELD_SNAPSHOT_COMMIT);
        if (planId == null || snapshotCommit == null) {
            throw new JsonFormatException(
                    "Missing required fields for LocalChangesCaptureResult: plan_id or snapshot_commit");
        }
        var lane = JsonTree.string(map, FIELD_LANE);
        return new LocalChangesCaptureResult(planId,
                lane != null ? SourceLane.fromWire(lane) : SourceLane.LAND, snapshotCommit,
                JsonTree.string(map, FIELD_STATUS));
    }

    /**
     * @param json the JSON object string
     * @return the parsed result
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static LocalChangesCaptureResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LocalChangesCaptureResult");
        }
        return fromMap(obj);
    }
}
