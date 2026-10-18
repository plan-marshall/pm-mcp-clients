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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Detail representation of one role of the effective role set
 * ({@code GET /api/v1/roles/{role}}, {@code POST /api/v1/roles/{role}/qualify}): the effective
 * artifact, the advisory tool set the harness applies, the narrowed effort, the qualification
 * criteria and record summary, and the remaining configuration parameters
 * (PM-WORK-1, PM-WORK-6, CLI &amp; Security § Roles).
 *
 * @param role                    the role identifier
 * @param displayName             the display name of the role
 * @param description             what the role does
 * @param advisoryToolSet         the tool set the role advises; advisory where the harness cannot enforce it
 * @param narrowedEffort          the effort narrowing the harness applies to the role, or {@code null}
 * @param qualification           the qualification criteria and record summary
 * @param configurationParameters the remaining effective configuration values
 */
public record RoleDetailResource(
        String role,
        String displayName,
        String description,
        List<String> advisoryToolSet,
        @Nullable NarrowedEffort narrowedEffort,
        Qualification qualification,
        Map<String, Object> configurationParameters
) {
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_DISPLAY_NAME = "display_name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_ADVISORY_TOOL_SET = "advisory_tool_set";
    public static final String FIELD_NARROWED_EFFORT = "narrowed_effort";
    public static final String FIELD_QUALIFICATION = "qualification";
    public static final String FIELD_CONFIGURATION_PARAMETERS = "configuration_parameters";

    private static final Pattern ROLE_NAME = Pattern.compile("^[a-z][a-z0-9-]{1,31}$");
    private static final List<String> PARAMETER_FIELDS = List.of(
            "source_set", "class", "form", "harness", "model", "effort",
            "configuration_digest", "qualification_digest", "validation",
            "corpus_topics", "item_counts", "expected_tokens", "expected_time",
            "qualification_record_path");

    /**
     * The effort narrowing a harness applies to a role.
     *
     * @param category    the narrowed effort category
     * @param description what the narrowing changes, or {@code null}
     */
    public record NarrowedEffort(String category, @Nullable String description) {

        public static final String FIELD_CATEGORY = "category";
        public static final String FIELD_DESCRIPTION = "description";

        public NarrowedEffort {
            if (category == null || category.isBlank()) {
                throw new IllegalArgumentException("category must not be blank");
            }
            description = description == null || description.isBlank() ? null : description;
        }

        /**
         * @return map representation
         */
        public Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put(FIELD_CATEGORY, category);
            if (description != null) {
                map.put(FIELD_DESCRIPTION, description);
            }
            return map;
        }
    }

    /**
     * The qualification criteria and record summary of a role.
     *
     * @param state    the qualification state
     * @param reason   why the recorded state holds, or {@code null}
     * @param criteria the criteria the qualification measures
     */
    public record Qualification(RoleQualificationState state, @Nullable String reason,
                                List<String> criteria) {

        public static final String FIELD_STATE = "state";
        public static final String FIELD_REASON = "reason";
        public static final String FIELD_CRITERIA = "criteria";

        public Qualification {
            Objects.requireNonNull(state, FIELD_STATE);
            reason = reason == null || reason.isBlank() ? null : reason;
            criteria = criteria == null ? List.of() : List.copyOf(criteria);
        }

        /**
         * @return map representation
         */
        public Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put(FIELD_STATE, state.wire());
            if (reason != null) {
                map.put(FIELD_REASON, reason);
            }
            map.put(FIELD_CRITERIA, criteria);
            return map;
        }
    }

    public RoleDetailResource {
        requireRole(role);
        displayName = displayName == null || displayName.isBlank() ? role : displayName;
        description = description == null ? "" : description;
        advisoryToolSet = advisoryToolSet == null ? List.of() : List.copyOf(advisoryToolSet);
        Objects.requireNonNull(qualification, FIELD_QUALIFICATION);
        configurationParameters = immutableMap(configurationParameters);
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
        map.put(FIELD_ADVISORY_TOOL_SET, advisoryToolSet);
        if (narrowedEffort != null) {
            map.put(FIELD_NARROWED_EFFORT, narrowedEffort.toMap());
        }
        map.put(FIELD_QUALIFICATION, qualification.toMap());
        map.put(FIELD_CONFIGURATION_PARAMETERS, configurationParameters);
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
    public static RoleDetailResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var role = firstString(map, FIELD_ROLE, "name");
        var displayName = JsonTree.string(map, FIELD_DISPLAY_NAME);
        var description = JsonTree.string(map, FIELD_DESCRIPTION);
        var tools = strings(map, FIELD_ADVISORY_TOOL_SET, "tools");
        var narrowedEffort = narrowedEffort(map);
        var qualification = qualification(map);
        var parameters = parameters(map);
        try {
            return new RoleDetailResource(role, displayName, description, tools, narrowedEffort,
                    qualification, parameters);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JsonFormatException("Invalid RoleDetailResource: " + e.getMessage());
        }
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RoleDetailResource fromJson(String json) throws IOException {
        return fromMap(objectOf(json, "RoleDetailResource"));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RoleDetailResource readFrom(JsonParser parser) throws IOException {
        return fromMap(objectOf(parser, "RoleDetailResource"));
    }

    private static Map<String, Object> objectOf(String json, String resource) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var object = (Map<String, Object>) map;
            return object;
        }
        throw new JsonFormatException("Expected a JSON object for " + resource);
    }

    private static Map<String, Object> objectOf(JsonParser parser, String resource) throws IOException {
        var parsed = JsonTree.readValue(parser);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var object = (Map<String, Object>) map;
            return object;
        }
        throw new JsonFormatException("Expected a JSON object for " + resource);
    }

    private static @Nullable NarrowedEffort narrowedEffort(Map<String, Object> map) throws IOException {
        var value = map.get(FIELD_NARROWED_EFFORT);
        if (value == null) {
            return null;
        }
        if (value instanceof String category) {
            return new NarrowedEffort(category, null);
        }
        if (value instanceof Map<?, ?> nested) {
            var object = JsonTree.asObject(nested);
            var category = JsonTree.string(object, NarrowedEffort.FIELD_CATEGORY);
            if (category == null) {
                category = JsonTree.string(object, "effort");
            }
            if (category == null) {
                throw new JsonFormatException("Missing category in narrowed_effort");
            }
            return new NarrowedEffort(category, JsonTree.string(object, NarrowedEffort.FIELD_DESCRIPTION));
        }
        throw new JsonFormatException("narrowed_effort must be a string or an object");
    }

    private static Qualification qualification(Map<String, Object> map) throws IOException {
        var value = map.get(FIELD_QUALIFICATION);
        String wire = null;
        String reason = null;
        List<String> criteria = List.of();
        if (value instanceof String text) {
            wire = text;
        } else if (value instanceof Map<?, ?> nested) {
            var object = JsonTree.asObject(nested);
            wire = JsonTree.string(object, Qualification.FIELD_STATE);
            reason = JsonTree.string(object, Qualification.FIELD_REASON);
            criteria = strings(object, Qualification.FIELD_CRITERIA, "topics");
        } else if (value != null) {
            throw new JsonFormatException("qualification must be a string or an object");
        }
        if (wire == null) {
            wire = JsonTree.string(map, RoleSummaryResource.FIELD_QUALIFICATION_STATE);
        }
        if (wire == null || wire.isBlank()) {
            return new Qualification(RoleQualificationState.UNQUALIFIED, reason, criteria);
        }
        var state = RoleQualificationState.fromWire(wire);
        if (state == null) {
            throw new JsonFormatException("Unknown role qualification state: " + wire);
        }
        return new Qualification(state, reason, criteria);
    }

    private static Map<String, Object> parameters(Map<String, Object> map) {
        var explicit = JsonTree.object(map, FIELD_CONFIGURATION_PARAMETERS);
        if (explicit != null) {
            return explicit;
        }
        var parameters = new LinkedHashMap<String, Object>();
        for (var field : PARAMETER_FIELDS) {
            var value = map.get(field);
            if (value != null) {
                parameters.put(field, value);
            }
        }
        return parameters;
    }

    private static List<String> strings(Map<String, Object> map, String field, String alias) {
        var value = map.get(field);
        if (value == null) {
            value = map.get(alias);
        }
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        var strings = new ArrayList<String>(list.size());
        for (var element : list) {
            if (element instanceof String text) {
                strings.add(text);
            }
        }
        return strings;
    }

    private static @Nullable String firstString(Map<String, Object> map, String field, String alias) {
        var value = JsonTree.string(map, field);
        return value != null ? value : JsonTree.string(map, alias);
    }

    private static Map<String, Object> immutableMap(@Nullable Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        var copy = new LinkedHashMap<String, Object>();
        map.forEach((key, value) -> copy.put(key, normalize(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object normalize(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case JsonNumber number -> normalizeNumber(number.text());
            case Byte number -> number.longValue();
            case Short number -> number.longValue();
            case Integer number -> number.longValue();
            case Long number -> number;
            case Float number -> number.doubleValue();
            case Double number -> number;
            case BigDecimal number -> number.doubleValue();
            case String text -> text;
            case Boolean flag -> flag;
            case Map<?, ?> nested -> immutableMap(asStringMap(nested));
            case List<?> list -> list.stream().map(RoleDetailResource::normalize).toList();
            default -> throw new IllegalArgumentException(
                    "Not a JSON value in configuration_parameters: " + value.getClass().getName());
        };
    }

    private static Object normalizeNumber(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException _) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not a JSON number: " + text, e);
            }
        }
    }

    private static Map<String, Object> asStringMap(Map<?, ?> map) {
        var copy = new LinkedHashMap<String, Object>();
        map.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return copy;
    }

    private static void requireRole(@Nullable String role) {
        if (role == null || !ROLE_NAME.matcher(role).matches()) {
            throw new IllegalArgumentException("role must match " + ROLE_NAME.pattern() + ": " + role);
        }
    }
}
