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
 * Event payload emitted when a paired device is revoked ({@code device.revoked}).
 *
 * @param deviceId the identifier of the revoked device
 */
public record DeviceRevokedEvent(String deviceId) {

    public static final String EVENT_NAME = "device.revoked";

    public DeviceRevokedEvent {
        Objects.requireNonNull(deviceId, "deviceId");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("device_id", deviceId);
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
    public static DeviceRevokedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var deviceId = JsonTree.string(map, "device_id");
        if (deviceId == null) {
            deviceId = JsonTree.string(map, "deviceId");
        }
        if (deviceId == null) {
            throw new JsonFormatException("Missing device_id in DeviceRevokedEvent");
        }
        return new DeviceRevokedEvent(deviceId);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on parse failure
     */
    public static DeviceRevokedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DeviceRevokedEvent");
        }
        return fromMap(obj);
    }
}
