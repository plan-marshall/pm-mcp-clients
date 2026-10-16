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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Immutable representation of a paired device authorized to act as an operator channel.
 *
 * @param deviceId   unique identifier of the device
 * @param label      human-readable label
 * @param pairedFrom network origin ("loopback" or "network")
 * @param createdAt  timestamp when device was paired
 * @param lastUsedAt timestamp when device was last active
 */
public record PairedDevice(
String deviceId,
String label,
String pairedFrom,
Instant createdAt,
Instant lastUsedAt
) {
    public PairedDevice {
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(label, "label");
        pairedFrom = pairedFrom == null || pairedFrom.isBlank() ? "loopback" : pairedFrom;
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(lastUsedAt, "lastUsedAt");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("device_id", deviceId);
        map.put("label", label);
        map.put("paired_from", pairedFrom);
        map.put("created_at", createdAt.toString());
        map.put("last_used_at", lastUsedAt.toString());
        return map;
    }

    /**
     * Writes device to compact JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a paired device from map.
     *
     * @param map the input map
     * @return paired device
     */
    public static PairedDevice fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var deviceId = JsonTree.string(map, "device_id");
        if (deviceId == null) {
            deviceId = JsonTree.string(map, "deviceId");
        }
        var label = JsonTree.string(map, "label");
        var pairedFrom = JsonTree.string(map, "paired_from");
        if (pairedFrom == null) {
            pairedFrom = JsonTree.string(map, "pairedFrom");
        }
        var createdAtStr = JsonTree.string(map, "created_at");
        if (createdAtStr == null) {
            createdAtStr = JsonTree.string(map, "createdAt");
        }
        var lastUsedAtStr = JsonTree.string(map, "last_used_at");
        if (lastUsedAtStr == null) {
            lastUsedAtStr = JsonTree.string(map, "lastUsedAt");
        }

        if (deviceId == null || label == null) {
            throw new JsonFormatException("Missing required fields for PairedDevice: device_id or label");
        }
        var createdAt = createdAtStr != null ? Instant.parse(createdAtStr) : Instant.now();
        var lastUsedAt = lastUsedAtStr != null ? Instant.parse(lastUsedAtStr) : Instant.now();
        return new PairedDevice(deviceId, label, pairedFrom != null ? pairedFrom : "loopback", createdAt, lastUsedAt);
    }

    /**
     * Parses a paired device from JSON string.
     *
     * @param json the JSON string
     * @return paired device
     * @throws IOException on parse failure
     */
    public static PairedDevice fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PairedDevice");
        }
        return fromMap(obj);
    }

    /**
     * Parses a list of paired devices from JSON array string.
     *
     * @param json JSON array string
     * @return list of paired devices
     * @throws IOException on parse failure
     */
    public static List<PairedDevice> parseList(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (!(parsed instanceof List<?> list)) {
            throw new JsonFormatException("Expected JSON array for PairedDevice list");
        }
        var result = new ArrayList<PairedDevice>(list.size());
        for (var item : list) {
            var map = JsonTree.asObject(item);
            if (map != null) {
                result.add(fromMap(map));
            }
        }
        return result;
    }

    /**
     * Serializes a list of paired devices to JSON array string.
     *
     * @param devices list of devices
     * @return JSON string
     */
    public static String writeList(List<PairedDevice> devices) {
        var list = new ArrayList<Map<String, Object>>(devices.size());
        for (var d : devices) {
            list.add(d.toMap());
        }
        return JsonTree.write(list);
    }
}
