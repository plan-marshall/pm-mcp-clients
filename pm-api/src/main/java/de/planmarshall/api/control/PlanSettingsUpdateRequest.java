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

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A change to one aspect of a plan's settings, sent against a revision
 * ({@code plan-settings.value}, {@code plan-settings.apply}, UC-CTL-5).
 *
 * <p>A {@code null} value removes the plan's own value, so that the project's or the machine's
 * applies.
 *
 * @param aspect   the aspect to change
 * @param value    the plan's own value, or {@code null} to remove it
 * @param revision the revision the dialogue was opened with
 */
public record PlanSettingsUpdateRequest(
        String aspect,
        @Nullable String value,
        int revision
) {
    public PlanSettingsUpdateRequest {
        Objects.requireNonNull(aspect, "aspect");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("aspect", aspect);
        if (value != null) {
            map.put("value", value);
        } else {
            map.put("remove", true);
        }
        map.put("revision", revision);
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON tree map.
     *
     * @param map map representation
     * @return parsed request
     * @throws IOException on format failure
     */
    public static PlanSettingsUpdateRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var aspect = JsonTree.string(map, "aspect");
        if (aspect == null) {
            aspect = JsonTree.string(map, "key");
        }
        if (aspect == null) {
            throw new JsonFormatException("Missing aspect in PlanSettingsUpdateRequest");
        }
        var revision = JsonTree.integer(map, "revision");
        return new PlanSettingsUpdateRequest(aspect, JsonTree.string(map, "value"),
                revision != null ? revision : 0);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed request
     * @throws IOException on format failure
     */
    public static PlanSettingsUpdateRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PlanSettingsUpdateRequest");
        }
        return fromMap(obj);
    }
}
