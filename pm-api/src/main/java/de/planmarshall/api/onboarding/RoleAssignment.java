/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One role's effective assignment: its harness, model, and effort, with where the value comes
 * from (UC-ONB-6; Role Assignment, Role Validation).
 *
 * @param role      the role id, e.g. {@code implement}, {@code outline}
 * @param harnessId the client id of the assigned harness, or {@code null} where none is assigned
 * @param modelId   the assigned model id, or {@code null} where none is assigned
 * @param effort    the assigned effort, or {@code null} where the harness has no effort setting
 * @param origin    where the value comes from: {@code default}, {@code steward}, or {@code changed}
 */
public record RoleAssignment(
        String role,
        @Nullable String harnessId,
        @Nullable String modelId,
        @Nullable String effort,
        String origin
) {
    public RoleAssignment {
        Objects.requireNonNull(role, "role");
        origin = origin != null && !origin.isBlank() ? origin : "default";
    }

    /**
     * @return {@code true} when the role names a harness and a model
     */
    public boolean assigned() {
        return harnessId != null && !harnessId.isBlank() && modelId != null && !modelId.isBlank();
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("role", role);
        if (harnessId != null) {
            map.put("harness_id", harnessId);
        }
        if (modelId != null) {
            map.put("model_id", modelId);
        }
        if (effort != null) {
            map.put("effort", effort);
        }
        map.put("origin", origin);
        return map;
    }

    /**
     * Serializes to a JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map map representation
     * @return parsed record
     * @throws IOException on format failure
     */
    public static RoleAssignment fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var role = JsonTree.string(map, "role");
        if (role == null) {
            role = JsonTree.string(map, "role_id");
        }
        if (role == null) {
            throw new JsonFormatException("Missing role in RoleAssignment");
        }
        return new RoleAssignment(role,
                JsonTree.string(map, "harness_id"),
                JsonTree.string(map, "model_id"),
                JsonTree.string(map, "effort"),
                JsonTree.string(map, "origin"));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed record
     * @throws IOException on format failure
     */
    public static RoleAssignment fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for RoleAssignment");
        }
        return fromMap(obj);
    }
}
