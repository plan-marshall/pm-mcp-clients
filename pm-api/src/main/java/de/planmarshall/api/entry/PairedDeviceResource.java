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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One paired device as {@code GET /api/v1/web/devices} carries it
 * (03-web-server.adoc#devices): its identifier, label, when it was paired and last used, the verb
 * that paired it, and the listener the redemption arrived on.
 *
 * @param deviceId   the identifier of the device
 * @param label      the label recorded for the device
 * @param createdAt  when the device was paired
 * @param lastUsedAt when the device was last used
 * @param pairedBy   the verb that issued the pairing code ({@code pair}, {@code open}, or {@code entry})
 * @param pairedFrom the listener the redemption arrived on ({@code loopback} or {@code lan})
 */
public record PairedDeviceResource(
        String deviceId,
        String label,
        Instant createdAt,
        Instant lastUsedAt,
        String pairedBy,
        String pairedFrom
) {

    /** The default {@code paired_by} when the record names none. */
    public static final String DEFAULT_PAIRED_BY = "pair";

    /** The default {@code paired_from} when the record names none. */
    public static final String DEFAULT_PAIRED_FROM = "loopback";

    public static final String FIELD_DEVICE_ID = "device_id";
    public static final String FIELD_LABEL = "label";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_LAST_USED_AT = "last_used_at";
    public static final String FIELD_PAIRED_BY = "paired_by";
    public static final String FIELD_PAIRED_FROM = "paired_from";

    public PairedDeviceResource {
        Objects.requireNonNull(deviceId, FIELD_DEVICE_ID);
        Objects.requireNonNull(label, FIELD_LABEL);
        Objects.requireNonNull(createdAt, FIELD_CREATED_AT);
        Objects.requireNonNull(lastUsedAt, FIELD_LAST_USED_AT);
        pairedBy = pairedBy == null || pairedBy.isBlank() ? DEFAULT_PAIRED_BY : pairedBy;
        pairedFrom = pairedFrom == null || pairedFrom.isBlank() ? DEFAULT_PAIRED_FROM : pairedFrom;
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
        map.put(FIELD_CREATED_AT, createdAt.toString());
        map.put(FIELD_LAST_USED_AT, lastUsedAt.toString());
        map.put(FIELD_PAIRED_BY, pairedBy);
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
     * Writes this device to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses a paired device from a JSON tree map.
     *
     * @param map the input map
     * @return the paired device
     * @throws IOException on a missing required field or malformed instant
     */
    public static PairedDeviceResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var deviceId = JsonTree.string(map, FIELD_DEVICE_ID);
        var label = JsonTree.string(map, FIELD_LABEL);
        if (deviceId == null || label == null) {
            throw new JsonFormatException("Missing device_id or label in PairedDeviceResource");
        }
        return new PairedDeviceResource(
                deviceId,
                label,
                instantOf(map, FIELD_CREATED_AT),
                instantOf(map, FIELD_LAST_USED_AT),
                JsonTree.string(map, FIELD_PAIRED_BY),
                JsonTree.string(map, FIELD_PAIRED_FROM));
    }

    /**
     * Parses a paired device from a JSON string.
     *
     * @param json the JSON string
     * @return the paired device
     * @throws IOException on parse failure
     */
    public static PairedDeviceResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PairedDeviceResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one paired device from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the paired device
     * @throws IOException on parse failure
     */
    public static PairedDeviceResource readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PairedDeviceResource");
        }
        return fromMap(obj);
    }

    private static Instant instantOf(Map<String, Object> map, String field) throws IOException {
        var text = JsonTree.string(map, field);
        if (text == null) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException _) {
            throw new JsonFormatException("Invalid " + field + " in PairedDeviceResource: " + text);
        }
    }
}
