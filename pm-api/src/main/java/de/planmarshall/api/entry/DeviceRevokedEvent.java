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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a paired device is revoked ({@code device.revoked}); the runtime then
 * ends the device's open streams with {@code stream.closed} and the reason {@code device_revoked}
 * (06-event-catalogue.adoc, 03-web-server.adoc#devices).
 *
 * @param deviceId   the identifier of the revoked device
 * @param label      the label the device had
 * @param pairedFrom the listener the device was paired from ({@code loopback} or {@code lan})
 */
public record DeviceRevokedEvent(String deviceId, String label, String pairedFrom) {

    public static final String EVENT_NAME = "device.revoked";

    private static final String FIELD_DEVICE_ID = "device_id";
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_PAIRED_FROM = "paired_from";

    public DeviceRevokedEvent {
        Objects.requireNonNull(deviceId, FIELD_DEVICE_ID);
        Objects.requireNonNull(label, FIELD_LABEL);
        pairedFrom = pairedFrom == null || pairedFrom.isBlank()
                ? PairedDeviceResource.DEFAULT_PAIRED_FROM : pairedFrom;
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_DEVICE_ID, deviceId);
        map.put(FIELD_LABEL, label);
        map.put(FIELD_PAIRED_FROM, pairedFrom);
        return map;
    }

    /**
     * @return compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this event to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses a revoked event from a JSON tree map.
     *
     * @param map the input map
     * @return the event
     * @throws IOException on a missing required field
     */
    public static DeviceRevokedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var deviceId = JsonTree.string(map, FIELD_DEVICE_ID);
        var label = JsonTree.string(map, FIELD_LABEL);
        if (deviceId == null || label == null) {
            throw new JsonFormatException("Missing device_id or label in DeviceRevokedEvent");
        }
        return new DeviceRevokedEvent(deviceId, label, JsonTree.string(map, FIELD_PAIRED_FROM));
    }

    /**
     * Parses a revoked event from a JSON string.
     *
     * @param json the JSON string
     * @return the event
     * @throws IOException on parse failure
     */
    public static DeviceRevokedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DeviceRevokedEvent");
        }
        return fromMap(obj);
    }

    /**
     * Reads one revoked event from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the event
     * @throws IOException on parse failure
     */
    public static DeviceRevokedEvent readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DeviceRevokedEvent");
        }
        return fromMap(obj);
    }
}
