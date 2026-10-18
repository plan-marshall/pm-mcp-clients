/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Resource representation of the runtime's web access: whether it is on, the port of its listener,
 * whether the listener is confined to the loopback address, and how many devices are paired. Its
 * links reach the listeners and the paired devices.
 *
 * @param status             the web access status ({@code on}, {@code off}, …)
 * @param port               the port of the web listener
 * @param loopbackOnly       whether the listener is confined to the loopback address
 * @param pairedDevicesCount the number of paired devices
 * @param links              the links of the resource
 */
public record WebAccessResource(
        String status,
        int port,
        boolean loopbackOnly,
        int pairedDevicesCount,
        List<ResourceLink> links
) {
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_PORT = "port";
    public static final String FIELD_LOOPBACK_ONLY = "loopback_only";
    public static final String FIELD_PAIRED_DEVICES_COUNT = "paired_devices_count";

    public static final String STATUS_ON = "on";
    public static final String STATUS_OFF = "off";

    public WebAccessResource {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Missing status in WebAccessResource");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port out of range in WebAccessResource: " + port);
        }
        if (pairedDevicesCount < 0) {
            throw new IllegalArgumentException(
                    "pairedDevicesCount must not be negative in WebAccessResource");
        }
        links = links == null ? List.of() : List.copyOf(links);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STATUS, status);
        map.put(FIELD_PORT, port);
        map.put(FIELD_LOOPBACK_ONLY, loopbackOnly);
        map.put(FIELD_PAIRED_DEVICES_COUNT, pairedDevicesCount);
        map.put(ResourceLink.FIELD_LINKS, ResourceLink.toMaps(links));
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
     * Writes this resource to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map input map
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static WebAccessResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var status = JsonTree.string(map, FIELD_STATUS);
        if (status == null || status.isBlank()) {
            throw new JsonFormatException("Missing status in WebAccessResource");
        }
        return new WebAccessResource(
                status,
                intOf(map, FIELD_PORT),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_LOOPBACK_ONLY)),
                intOf(map, FIELD_PAIRED_DEVICES_COUNT),
                ResourceLink.fromArray(map.get(ResourceLink.FIELD_LINKS)));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static WebAccessResource fromJson(String json) throws IOException {
        return fromMap(ResourceLink.objectOf(json, "WebAccessResource"));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static WebAccessResource readFrom(JsonParser parser) throws IOException {
        return fromMap(ResourceLink.objectOf(parser, "WebAccessResource"));
    }

    private static int intOf(Map<String, Object> map, String field) throws IOException {
        var number = JsonTree.number(map, field);
        if (number == null) {
            throw new JsonFormatException("Missing " + field + " in WebAccessResource");
        }
        return number.intValue();
    }
}
