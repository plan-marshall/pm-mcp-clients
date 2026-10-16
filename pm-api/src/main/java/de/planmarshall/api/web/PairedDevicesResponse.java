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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Container response for paired devices and network listener status.
 *
 * @param devices        active paired devices
 * @param lanListenerOn  whether the network (LAN) listener is active
 */
public record PairedDevicesResponse(List<PairedDevice> devices, boolean lanListenerOn) {

    public PairedDevicesResponse {
        devices = devices != null ? List.copyOf(devices) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        var list = new ArrayList<Map<String, Object>>(devices.size());
        for (var d : devices) {
            list.add(d.toMap());
        }
        map.put("devices", list);
        map.put("lan_listener_on", lanListenerOn);
        return map;
    }

    /**
     * Serializes to compact JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON string (supports either an array of devices or a full container object).
     *
     * @param json the JSON string
     * @return parsed response
     * @throws IOException on parse failure
     */
    public static PairedDevicesResponse fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof List<?> list) {
            var devs = new ArrayList<PairedDevice>(list.size());
            for (var item : list) {
                var map = JsonTree.asObject(item);
                if (map != null) {
                    devs.add(PairedDevice.fromMap(map));
                }
            }
            return new PairedDevicesResponse(devs, true);
        }
        var obj = JsonTree.asObject(parsed);
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object or array for PairedDevicesResponse");
        }
        var devicesListObj = JsonTree.asArray(obj.get("devices"));
        var devs = new ArrayList<PairedDevice>();
        if (devicesListObj != null) {
            for (var item : devicesListObj) {
                var map = JsonTree.asObject(item);
                if (map != null) {
                    devs.add(PairedDevice.fromMap(map));
                }
            }
        }
        var lan = Boolean.TRUE.equals(obj.get("lan_listener_on")) || Boolean.TRUE.equals(obj.get("lan"));
        return new PairedDevicesResponse(devs, lan);
    }
}
