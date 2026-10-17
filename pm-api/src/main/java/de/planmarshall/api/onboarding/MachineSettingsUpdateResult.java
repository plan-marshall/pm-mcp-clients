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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The outcome of a machine settings change: applied, or refused with {@code conflict} against a
 * newer revision, in which case the current value is returned (UC-ONB-10).
 *
 * @param applied      {@code true} when the change was written
 * @param conflict     {@code true} when a newer revision won
 * @param revision     the revision after the attempt
 * @param currentValue the current value when the change was refused, or {@code null}
 */
public record MachineSettingsUpdateResult(
        boolean applied,
        boolean conflict,
        int revision,
        @Nullable String currentValue
) {
    /**
     * @param revision the current revision
     * @return a result naming an applied change
     */
    public static MachineSettingsUpdateResult applied(int revision) {
        return new MachineSettingsUpdateResult(true, false, revision, null);
    }

    /**
     * @param revision     the newer revision
     * @param currentValue the current value
     * @return a result naming a refused change
     */
    public static MachineSettingsUpdateResult conflict(int revision, @Nullable String currentValue) {
        return new MachineSettingsUpdateResult(false, true, revision, currentValue);
    }

    /**
     * Converts to a JSON tree map.
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
     * @return parsed result
     * @throws IOException on format failure
     */
    public static MachineSettingsUpdateResult fromMap(Map<String, Object> map) {
        var revision = JsonTree.integer(map, "revision");
        return new MachineSettingsUpdateResult(
                Boolean.TRUE.equals(JsonTree.bool(map, "applied")),
                Boolean.TRUE.equals(JsonTree.bool(map, "conflict")),
                revision != null ? revision : 0,
                JsonTree.string(map, "current_value"));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed result
     * @throws IOException on format failure
     */
    public static MachineSettingsUpdateResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for MachineSettingsUpdateResult");
        }
        return fromMap(obj);
    }
}
