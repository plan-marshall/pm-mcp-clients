/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a paired device is renamed ({@code device.renamed}).
 *
 * @param deviceId the identifier of the device
 * @param label    the new human-readable label
 */
public record DeviceRenamedEvent(String deviceId, String label) {

    public static final String EVENT_NAME = "device.renamed";
    private static final String FIELD_DEVICE_ID = "device_id";
    private static final String FIELD_LABEL = "label";

    public DeviceRenamedEvent {
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(label, FIELD_LABEL);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_DEVICE_ID, deviceId);
        map.put(FIELD_LABEL, label);
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map tree map
     * @return parsed event
     */
    public static DeviceRenamedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var deviceId = JsonTree.string(map, FIELD_DEVICE_ID);
        if (deviceId == null) {
            deviceId = JsonTree.string(map, "deviceId");
        }
        var label = JsonTree.string(map, FIELD_LABEL);
        if (deviceId == null || label == null) {
            throw new JsonFormatException("Missing device_id or label in DeviceRenamedEvent");
        }
        return new DeviceRenamedEvent(deviceId, label);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on parse failure
     */
    public static DeviceRenamedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DeviceRenamedEvent");
        }
        return fromMap(obj);
    }
}
