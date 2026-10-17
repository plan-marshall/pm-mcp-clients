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
 * A change of one machine setting, sent against the revision the dialogue was opened with
 * (UC-ONB-10; Concurrent Changes of a Setting).
 *
 * @param key      the setting key, e.g. {@code documents_mode}, {@code cleanup_durations},
 *                 {@code plan_defaults}, or {@code consent:<kind>}
 * @param value    the new value, or {@code null} for a withdrawal
 * @param revision the revision the change was made against
 */
public record MachineSettingsUpdateRequest(
        String key,
        @Nullable String value,
        int revision
) {
    public MachineSettingsUpdateRequest {
        Objects.requireNonNull(key, "key");
    }

    /**
     * @param kind the consent kind to withdraw
     * @param revision the revision the change was made against
     * @return a request that withdraws the remembered consent of the kind
     */
    public static MachineSettingsUpdateRequest withdrawConsent(ConsentKind kind, int revision) {
        return new MachineSettingsUpdateRequest("consent:" + kind.wireValue(), null, revision);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("key", key);
        if (value != null) {
            map.put("value", value);
        }
        map.put("revision", revision);
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
     * @return parsed request
     * @throws IOException on format failure
     */
    public static MachineSettingsUpdateRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var key = JsonTree.string(map, "key");
        if (key == null) {
            key = JsonTree.string(map, "aspect");
        }
        if (key == null) {
            throw new JsonFormatException("Missing key in MachineSettingsUpdateRequest");
        }
        var revision = JsonTree.integer(map, "revision");
        return new MachineSettingsUpdateRequest(key, JsonTree.string(map, "value"),
                revision != null ? revision : 0);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed request
     * @throws IOException on format failure
     */
    public static MachineSettingsUpdateRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for MachineSettingsUpdateRequest");
        }
        return fromMap(obj);
    }
}
