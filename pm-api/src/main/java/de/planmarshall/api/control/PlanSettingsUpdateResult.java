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
 * The outcome of applying a plan's settings change ({@code plan-settings.apply}, UC-CTL-5).
 *
 * @param applied      {@code true} if the plan's value was written
 * @param conflict     {@code true} if the change was refused against an older revision
 * @param revision     the settings revision after the change, or the current one on a conflict
 * @param currentValue the current value shown on a conflict, or {@code null}
 * @param restoreState the plan's state name when the change was refused as too late, or {@code null}
 */
public record PlanSettingsUpdateResult(
        boolean applied,
        boolean conflict,
        int revision,
        @Nullable String currentValue,
        @Nullable String restoreState
) {
    /**
     * @param revision the new revision
     * @return a result that records an applied change
     */
    public static PlanSettingsUpdateResult applied(int revision) {
        return new PlanSettingsUpdateResult(true, false, revision, null, null);
    }

    /**
     * @param revision     the current revision
     * @param currentValue the current value
     * @return a result that records a conflict against an older revision
     */
    public static PlanSettingsUpdateResult conflict(int revision, String currentValue) {
        return new PlanSettingsUpdateResult(false, true, revision, currentValue, null);
    }

    /**
     * @param state the plan's state
     * @return a result that records a change refused as too late
     */
    public static PlanSettingsUpdateResult tooLate(String state) {
        return new PlanSettingsUpdateResult(false, false, 0, null, state);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("applied", applied);
        map.put("conflict", conflict);
        map.put("revision", revision);
        if (currentValue != null) {
            map.put("current_value", currentValue);
        }
        if (restoreState != null) {
            map.put("state", restoreState);
        }
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
     * @return parsed result
     * @throws IOException on format failure
     */
    public static PlanSettingsUpdateResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var revision = JsonTree.integer(map, "revision");
        return new PlanSettingsUpdateResult(
                Boolean.TRUE.equals(JsonTree.bool(map, "applied")),
                Boolean.TRUE.equals(JsonTree.bool(map, "conflict")),
                revision != null ? revision : 0,
                JsonTree.string(map, "current_value"),
                JsonTree.string(map, "state")
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed result
     * @throws IOException on format failure
     */
    public static PlanSettingsUpdateResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PlanSettingsUpdateResult");
        }
        return fromMap(obj);
    }
}
