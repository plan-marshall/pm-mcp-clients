/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.designer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A request to validate a workflow unit (UC-PLN-3).
 * <p>
 * {@code POST /api/v1/workflows/validate} with the text, the unit's kind and name, and the project
 * it is checked against; with no project it is validated against the machine target and composed
 * against every enrolled project the unit would reach. Nothing is started or written.
 *
 * @param text     the unit's DSL text
 * @param unitKind the unit's kind (e.g. {@code subworkflow}), or {@code null} when unknown
 * @param unitName the unit's id from its header, or {@code null} when the header is absent
 * @param project  the project the unit is checked against, or {@code null} for the machine target
 * @param digest   the digest of the unit the draft replaces, or {@code null}
 */
public record WorkflowValidationRequest(
        String text,
        String unitKind,
        String unitName,
        String project,
        String digest
) {
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_UNIT_KIND = "unit_kind";
    private static final String FIELD_UNIT_NAME = "unit_name";
    private static final String FIELD_PROJECT = "project";
    private static final String FIELD_DIGEST = "digest";

    /**
     * Compact constructor validating required properties.
     */
    public WorkflowValidationRequest {
        Objects.requireNonNull(text, FIELD_TEXT);
    }

    /**
     * @return this request as a JSON tree
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_TEXT, text);
        if (unitKind != null) {
            map.put(FIELD_UNIT_KIND, unitKind);
        }
        if (unitName != null) {
            map.put(FIELD_UNIT_NAME, unitName);
        }
        if (project != null) {
            map.put(FIELD_PROJECT, project);
        }
        if (digest != null) {
            map.put(FIELD_DIGEST, digest);
        }
        return map;
    }

    /**
     * @return this request as compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a request from a JSON tree.
     *
     * @param map the input tree
     * @return the request
     * @throws IOException on a malformed tree
     */
    public static WorkflowValidationRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var text = JsonTree.string(map, FIELD_TEXT);
        if (text == null) {
            throw new JsonFormatException("WorkflowValidationRequest requires text");
        }
        return new WorkflowValidationRequest(
                text,
                JsonTree.string(map, FIELD_UNIT_KIND),
                JsonTree.string(map, FIELD_UNIT_NAME),
                JsonTree.string(map, FIELD_PROJECT),
                JsonTree.string(map, FIELD_DIGEST)
        );
    }

    /**
     * Parses a request from JSON.
     *
     * @param json the JSON text
     * @return the request
     * @throws IOException on a malformed document
     */
    public static WorkflowValidationRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected a JSON object for WorkflowValidationRequest");
        }
        return fromMap(obj);
    }
}
