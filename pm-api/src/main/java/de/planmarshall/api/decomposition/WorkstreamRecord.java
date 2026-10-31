/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decomposition;

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
 * One workstream of an epic's decomposition (UC-STA-6): a named group of staged plans that belong
 * together and are ordered against each other.
 *
 * @param id          the workstream id ({@code WS-1}, {@code WS-2}, ...)
 * @param name        the workstream's name
 * @param description what the workstream covers in one line, or {@code null}
 * @param order       the workstream's position in the cut, from {@code 1}
 * @param stagedPlans the plans of the workstream, in the order they should run
 */
public record WorkstreamRecord(
        String id,
        String name,
        @Nullable String description,
        int order,
        List<StagedPlanRecord> stagedPlans
) {

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_ORDER = "order";
    private static final String FIELD_STAGED_PLANS = "staged_plans";

    public WorkstreamRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        stagedPlans = stagedPlans == null ? List.of() : List.copyOf(stagedPlans);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        map.put(FIELD_NAME, name);
        if (description != null) {
            map.put(FIELD_DESCRIPTION, description);
        }
        map.put(FIELD_ORDER, order);
        map.put(FIELD_STAGED_PLANS, stagedPlans.stream().map(StagedPlanRecord::toMap).toList());
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
    public static WorkstreamRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, FIELD_ID);
        var name = JsonTree.string(map, FIELD_NAME);
        if (id == null || name == null) {
            throw new JsonFormatException("Missing required fields for WorkstreamRecord: id or name");
        }
        var order = JsonTree.integer(map, FIELD_ORDER);
        return new WorkstreamRecord(id, name, JsonTree.string(map, FIELD_DESCRIPTION),
                order != null ? order : 0, readPlans(map.get(FIELD_STAGED_PLANS)));
    }

    /**
     * @param json the JSON object string
     * @return the parsed workstream
     * @throws IOException if the value is not an object or a field is missing
     */
    public static WorkstreamRecord fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for WorkstreamRecord");
        }
        return fromMap(obj);
    }

    private static List<StagedPlanRecord> readPlans(Object raw) throws IOException {
        var array = JsonTree.asArray(raw);
        if (array == null) {
            return List.of();
        }
        var plans = new ArrayList<StagedPlanRecord>();
        for (var item : array) {
            var obj = JsonTree.asObject(item);
            if (obj != null) {
                plans.add(StagedPlanRecord.fromMap(obj));
            }
        }
        return plans;
    }
}
