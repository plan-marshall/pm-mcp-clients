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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * An epic's decomposition draft as the control contract shows it (UC-STA-6, design <em>Starting
 * Work, The Decomposition</em>): its state, revision, workstreams, and the checks of the last run.
 *
 * <p>{@link #state()} is one of {@code draft}, {@code check}, {@code review}, {@code read_back},
 * {@code commit}, or {@code held}; the read-back of an edited plan is carried by its
 * {@link DecompositionPlan#change()}.
 *
 * @param epicId      the epic this cut belongs to
 * @param state       the state the decomposition is in
 * @param revision    the ledger revision the cut is based on
 * @param workstreams the workstreams, in order
 * @param checks      the findings of the last check
 */
public record DecompositionResource(
        String epicId,
        String state,
        long revision,
        List<DecompositionWorkstream> workstreams,
        List<DecompositionCheck> checks
) {

    public DecompositionResource {
        Objects.requireNonNull(epicId, "epicId");
        Objects.requireNonNull(state, "state");
        workstreams = workstreams != null ? List.copyOf(workstreams) : List.of();
        checks = checks != null ? List.copyOf(checks) : List.of();
    }

    /**
     * @return {@code true} if every check passes and the cut can be agreed
     */
    public boolean everyCheckPasses() {
        return checks.stream().noneMatch(DecompositionCheck::isError);
    }

    /**
     * @return the count of plans across all workstreams
     */
    public int planCount() {
        return workstreams.stream().mapToInt(workstream -> workstream.plans().size()).sum();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("epic_id", epicId);
        map.put("state", state);
        map.put("revision", revision);
        map.put("workstreams", workstreams.stream().map(DecompositionWorkstream::toMap).toList());
        map.put("checks", checks.stream().map(DecompositionCheck::toMap).toList());
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
     * @return the parsed decomposition
     * @throws IOException if a required field is missing
     */
    public static DecompositionResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var epicId = JsonTree.string(map, "epic_id");
        var state = JsonTree.string(map, "state");
        if (epicId == null || state == null) {
            throw new JsonFormatException("Missing required fields for DecompositionResource: epic_id, state");
        }
        var revision = ControlJson.optLong(map, "revision");
        return new DecompositionResource(epicId, state, revision != null ? revision : 0L,
                readWorkstreams(map), readChecks(map));
    }

    /**
     * @param json the JSON object string
     * @return the parsed decomposition
     * @throws IOException if the value is not an object
     */
    public static DecompositionResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DecompositionResource");
        }
        return fromMap(obj);
    }

    private static List<DecompositionWorkstream> readWorkstreams(Map<String, Object> map) throws IOException {
        var workstreams = new ArrayList<DecompositionWorkstream>();
        for (var object : ControlJson.readObjects(map, "workstreams")) {
            workstreams.add(DecompositionWorkstream.fromMap(object));
        }
        return workstreams;
    }

    private static List<DecompositionCheck> readChecks(Map<String, Object> map) throws IOException {
        var checks = new ArrayList<DecompositionCheck>();
        for (var object : ControlJson.readObjects(map, "checks")) {
            checks.add(DecompositionCheck.fromMap(object));
        }
        return checks;
    }
}
