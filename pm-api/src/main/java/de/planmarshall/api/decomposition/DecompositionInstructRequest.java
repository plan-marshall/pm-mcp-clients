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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The operator's words that shape a decomposition ({@code decomposition.instruct}, UC-STA-6).
 *
 * @param planId      the plan the words concern, or {@code null} to draft the whole cut again
 * @param instruction the operator's words, kept as they are
 * @param action      what the words ask the planning role to do
 */
public record DecompositionInstructRequest(
        @Nullable String planId,
        String instruction,
        InstructAction action
) {

    private static final String FIELD_PLAN_ID = "plan_id";
    private static final String FIELD_INSTRUCTION = "instruction";
    private static final String FIELD_ACTION = "action";

    public DecompositionInstructRequest {
        Objects.requireNonNull(instruction, "instruction");
        action = action == null ? InstructAction.EDIT : action;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (planId != null) {
            map.put(FIELD_PLAN_ID, planId);
        }
        map.put(FIELD_INSTRUCTION, instruction);
        map.put(FIELD_ACTION, action.wireValue());
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
     * @return the parsed request
     * @throws IOException if the instruction is missing or the action is unknown
     */
    public static DecompositionInstructRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var instruction = JsonTree.string(map, FIELD_INSTRUCTION);
        if (instruction == null) {
            throw new JsonFormatException("Missing required field for DecompositionInstructRequest: instruction");
        }
        var action = JsonTree.string(map, FIELD_ACTION);
        return new DecompositionInstructRequest(JsonTree.string(map, FIELD_PLAN_ID), instruction,
                action != null ? InstructAction.fromWire(action) : InstructAction.EDIT);
    }

    /**
     * @param json the JSON object string
     * @return the parsed request
     * @throws IOException if the value is not an object or a field is missing
     */
    public static DecompositionInstructRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DecompositionInstructRequest");
        }
        return fromMap(obj);
    }
}
