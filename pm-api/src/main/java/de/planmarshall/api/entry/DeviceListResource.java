/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.entry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The list of paired devices as {@code GET /api/v1/web/devices} carries it
 * (03-web-server.adoc#devices). On the wire the answer is a JSON array of devices; this resource
 * also accepts an object with a {@code devices} member, so a client can parse either shape.
 *
 * @param devices the paired devices, newest use first
 */
public record DeviceListResource(List<PairedDeviceResource> devices) {

    public static final String FIELD_DEVICES = "devices";

    public DeviceListResource {
        devices = devices == null ? List.of() : List.copyOf(devices);
    }

    /**
     * @return an empty device list
     */
    public static DeviceListResource empty() {
        return new DeviceListResource(List.of());
    }

    /**
     * @return the number of paired devices
     */
    public int size() {
        return devices.size();
    }

    /**
     * Converts to a JSON tree map with a {@code devices} member.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_DEVICES, devices.stream().map(PairedDeviceResource::toMap).toList());
        return map;
    }

    /**
     * Serializes to the wire shape of {@code GET /api/v1/web/devices}, a JSON array.
     *
     * @return compact JSON array
     */
    public String toJson() {
        return JsonTree.write(devices.stream().map(PairedDeviceResource::toMap).toList());
    }

    /**
     * Writes the device array to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, devices.stream().map(PairedDeviceResource::toMap).toList());
    }

    /**
     * Parses a device list from a JSON tree value: an array of devices or an object with a
     * {@code devices} member.
     *
     * @param value the JSON tree value
     * @return the device list
     * @throws IOException on a malformed member
     */
    public static DeviceListResource fromTree(Object value) throws IOException {
        Objects.requireNonNull(value, "value");
        var object = JsonTree.asObject(value);
        var array = object != null ? JsonTree.asArray(object.get(FIELD_DEVICES)) : JsonTree.asArray(value);
        if (array == null) {
            throw new JsonFormatException("Expected a device array or an object with a devices member");
        }
        var devices = new ArrayList<PairedDeviceResource>(array.size());
        for (var element : array) {
            var map = JsonTree.asObject(element);
            if (map == null) {
                throw new JsonFormatException("A device must be a JSON object");
            }
            devices.add(PairedDeviceResource.fromMap(map));
        }
        return new DeviceListResource(devices);
    }

    /**
     * Parses a device list from a JSON string.
     *
     * @param json the JSON string
     * @return the device list
     * @throws IOException on parse failure
     */
    public static DeviceListResource fromJson(String json) throws IOException {
        return fromTree(JsonTree.parse(json));
    }

    /**
     * Reads a device list from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the device list
     * @throws IOException on parse failure
     */
    public static DeviceListResource readFrom(JsonParser parser) throws IOException {
        return fromTree(JsonTree.readValue(parser));
    }
}
