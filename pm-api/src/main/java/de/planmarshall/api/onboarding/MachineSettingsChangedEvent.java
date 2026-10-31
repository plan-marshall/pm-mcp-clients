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
 * Broadcast when a machine setting changed, so both clients follow it (UC-ONB-10; Both Clients,
 * One State).
 *
 * @param key      the setting key that changed
 * @param value    the new value, or {@code null} for a withdrawal
 * @param revision the revision after the change
 * @param channel  the channel that made the change, or {@code null}
 */
public record MachineSettingsChangedEvent(
        String key,
        @Nullable String value,
        int revision,
        @Nullable String channel
) {
    /** The SSE event name this event is broadcast under. */
    public static final String EVENT_NAME = "machine.settings_changed";

    public MachineSettingsChangedEvent {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
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
        if (channel != null) {
            map.put("channel", channel);
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
     * @return parsed event
     * @throws IOException on format failure
     */
    public static MachineSettingsChangedEvent fromMap(Map<String, Object> map) throws IOException {
        var key = JsonTree.string(map, "key");
        if (key == null) {
            throw new JsonFormatException("Missing key in MachineSettingsChangedEvent");
        }
        var revision = JsonTree.integer(map, "revision");
        return new MachineSettingsChangedEvent(key, JsonTree.string(map, "value"),
                revision != null ? revision : 0, JsonTree.string(map, "channel"));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static MachineSettingsChangedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for MachineSettingsChangedEvent");
        }
        return fromMap(obj);
    }
}
