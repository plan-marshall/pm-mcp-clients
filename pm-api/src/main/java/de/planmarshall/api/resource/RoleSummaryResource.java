/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Summary representation of one role of the effective role set
 * ({@code GET /api/v1/roles}, {@code GET /api/v1/roles?project=}): the role identifier, its
 * display name and description, its capabilities, and the effective configuration and
 * qualification state the operator sees in {@code pm-operator roles list} (PM-WORK-1, PM-WORK-6,
 * CLI &amp; Security § Roles).
 *
 * @param role                   the role identifier
 * @param displayName            the display name of the role
 * @param description            what the role does
 * @param capabilities           the capabilities of the role, typically its tool set and class
 * @param sourceSet              the role set the effective artifact comes from
 * @param roleClass              the role's class ({@code writer}, {@code read_only}, {@code zero_tool})
 * @param form                   the role's form ({@code warm} or {@code fresh})
 * @param harness                the effective harness
 * @param model                  the effective model
 * @param effort                 the effective effort
 * @param harnessLevel           the level the effective harness comes from, or {@code null}
 * @param modelLevel             the level the effective model comes from, or {@code null}
 * @param effortLevel            the level the effective effort comes from, or {@code null}
 * @param qualificationState     the qualification state of the role
 * @param startFailureGuardHeld  whether the start-failure guard of the role holds
 */
public record RoleSummaryResource(
        String role,
        String displayName,
        String description,
        List<String> capabilities,
        @Nullable String sourceSet,
        @Nullable String roleClass,
        @Nullable String form,
        @Nullable String harness,
        @Nullable String model,
        @Nullable String effort,
        @Nullable String harnessLevel,
        @Nullable String modelLevel,
        @Nullable String effortLevel,
        RoleQualificationState qualificationState,
        boolean startFailureGuardHeld
) {
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_DISPLAY_NAME = "display_name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CAPABILITIES = "capabilities";
    public static final String FIELD_SOURCE_SET = "source_set";
    public static final String FIELD_CLASS = "class";
    public static final String FIELD_FORM = "form";
    public static final String FIELD_HARNESS = "harness";
    public static final String FIELD_MODEL = "model";
    public static final String FIELD_EFFORT = "effort";
    public static final String FIELD_HARNESS_LEVEL = "harness_level";
    public static final String FIELD_MODEL_LEVEL = "model_level";
    public static final String FIELD_EFFORT_LEVEL = "effort_level";
    public static final String FIELD_QUALIFICATION_STATE = "qualification_state";
    public static final String FIELD_START_FAILURE_GUARD_HELD = "start_failure_guard_held";

    private static final Pattern ROLE_NAME = Pattern.compile("^[a-z][a-z0-9-]{1,31}$");

    public RoleSummaryResource {
        requireRole(role);
        displayName = displayName == null || displayName.isBlank() ? role : displayName;
        description = description == null ? "" : description;
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        sourceSet = blankToNull(sourceSet);
        roleClass = blankToNull(roleClass);
        form = blankToNull(form);
        harness = blankToNull(harness);
        model = blankToNull(model);
        effort = blankToNull(effort);
        harnessLevel = blankToNull(harnessLevel);
        modelLevel = blankToNull(modelLevel);
        effortLevel = blankToNull(effortLevel);
        Objects.requireNonNull(qualificationState, FIELD_QUALIFICATION_STATE);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ROLE, role);
        map.put(FIELD_DISPLAY_NAME, displayName);
        map.put(FIELD_DESCRIPTION, description);
        map.put(FIELD_CAPABILITIES, capabilities);
        putIfPresent(map, FIELD_SOURCE_SET, sourceSet);
        putIfPresent(map, FIELD_CLASS, roleClass);
        putIfPresent(map, FIELD_FORM, form);
        putIfPresent(map, FIELD_HARNESS, harness);
        putIfPresent(map, FIELD_MODEL, model);
        putIfPresent(map, FIELD_EFFORT, effort);
        putIfPresent(map, FIELD_HARNESS_LEVEL, harnessLevel);
        putIfPresent(map, FIELD_MODEL_LEVEL, modelLevel);
        putIfPresent(map, FIELD_EFFORT_LEVEL, effortLevel);
        map.put(FIELD_QUALIFICATION_STATE, qualificationState.wire());
        map.put(FIELD_START_FAILURE_GUARD_HELD, startFailureGuardHeld);
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
     * Writes this resource to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map input map
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RoleSummaryResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var role = ResourceJson.firstString(map, FIELD_ROLE, "name");
        var displayName = JsonTree.string(map, FIELD_DISPLAY_NAME);
        var description = JsonTree.string(map, FIELD_DESCRIPTION);
        var capabilities = ResourceJson.strings(map, FIELD_CAPABILITIES, "tools");
        var sourceSet = ResourceJson.firstString(map, FIELD_SOURCE_SET, "set");
        var roleClass = ResourceJson.firstString(map, FIELD_CLASS, "role_class");
        var form = JsonTree.string(map, FIELD_FORM);
        var harness = JsonTree.string(map, FIELD_HARNESS);
        var model = JsonTree.string(map, FIELD_MODEL);
        var effort = JsonTree.string(map, FIELD_EFFORT);
        var harnessLevel = JsonTree.string(map, FIELD_HARNESS_LEVEL);
        var modelLevel = JsonTree.string(map, FIELD_MODEL_LEVEL);
        var effortLevel = JsonTree.string(map, FIELD_EFFORT_LEVEL);
        var state = qualificationState(map);
        var guardHeld = requireBoolean(map, FIELD_START_FAILURE_GUARD_HELD);
        try {
            return new RoleSummaryResource(role, displayName, description, capabilities, sourceSet,
                    roleClass, form, harness, model, effort, harnessLevel, modelLevel,
                    effortLevel, state, guardHeld);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JsonFormatException("Invalid RoleSummaryResource: " + e.getMessage());
        }
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RoleSummaryResource fromJson(String json) throws IOException {
        return fromMap(ResourceJson.objectOf(json, "RoleSummaryResource"));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RoleSummaryResource readFrom(JsonParser parser) throws IOException {
        return fromMap(ResourceJson.objectOf(parser, "RoleSummaryResource"));
    }

    /**
     * Parses a list of roles from a JSON array or an object with a {@code roles} array.
     *
     * @param json JSON string
     * @return parsed roles
     * @throws IOException on format failure
     */
    public static List<RoleSummaryResource> listFromJson(String json) throws IOException {
        return listFromTree(JsonTree.parse(json));
    }

    /**
     * Parses a list of roles from a parsed JSON tree, accepting either a bare array or an object
     * whose {@code roles} member holds the array.
     *
     * @param tree the parsed JSON tree
     * @return parsed roles
     * @throws IOException on format failure
     */
    public static List<RoleSummaryResource> listFromTree(Object tree) throws IOException {
        var array = tree;
        if (tree instanceof Map<?, ?> map) {
            array = map.get("roles");
        }
        if (!(array instanceof List<?> list)) {
            throw new JsonFormatException("Expected a JSON array of roles");
        }
        var roles = new ArrayList<RoleSummaryResource>(list.size());
        for (var element : list) {
            if (!(element instanceof Map<?, ?> map)) {
                throw new JsonFormatException("Expected a JSON object in the roles array");
            }
            @SuppressWarnings("unchecked")
            var object = (Map<String, Object>) map;
            roles.add(fromMap(object));
        }
        return List.copyOf(roles);
    }

    private static boolean requireBoolean(Map<String, Object> map, String field) throws IOException {
        if (map.get(field) instanceof Boolean flag) {
            return flag;
        }
        throw new JsonFormatException("Missing or non-boolean " + field);
    }

    private static RoleQualificationState qualificationState(Map<String, Object> map) throws IOException {
        var wire = JsonTree.string(map, FIELD_QUALIFICATION_STATE);
        if (wire == null) {
            var qualification = map.get("qualification");
            if (qualification instanceof String text) {
                wire = text;
            } else if (qualification instanceof Map<?, ?> nested) {
                wire = JsonTree.string(JsonTree.asObject(nested), "state");
            }
        }
        if (wire == null || wire.isBlank()) {
            return RoleQualificationState.UNQUALIFIED;
        }
        var state = RoleQualificationState.fromWire(wire);
        if (state == null) {
            throw new JsonFormatException("Unknown role qualification state: " + wire);
        }
        return state;
    }

    private static void putIfPresent(Map<String, Object> map, String field, @Nullable String value) {
        if (value != null) {
            map.put(field, value);
        }
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void requireRole(@Nullable String role) {
        if (role == null || !ROLE_NAME.matcher(role).matches()) {
            throw new IllegalArgumentException("role must match " + ROLE_NAME.pattern() + ": " + role);
        }
    }
}
