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
 * The answer to a successful pairing redemption: the per-device secret issued once, the new
 * device id, the recorded label, and the listener the redemption arrived on
 * (03-web-server.adoc#web-pairing).
 *
 * @param deviceSecret the opaque per-device secret ({@code pmw_} plus 256 bits)
 * @param deviceId     the identifier of the newly paired device
 * @param label        the label recorded for the device
 * @param pairedFrom   the listener the redemption arrived on ({@code loopback} or {@code lan})
 */
public record PairingRedemptionResponse(
        String deviceSecret,
        String deviceId,
        String label,
        String pairedFrom
) {
    private static final String FIELD_DEVICE_SECRET = "device_secret";
    private static final String FIELD_DEVICE_ID = "device_id";
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_PAIRED_FROM = "paired_from";
    private static final String DEFAULT_ORIGIN = "loopback";

    public PairingRedemptionResponse {
        Objects.requireNonNull(deviceSecret, FIELD_DEVICE_SECRET);
        Objects.requireNonNull(deviceId, FIELD_DEVICE_ID);
        Objects.requireNonNull(label, FIELD_LABEL);
        pairedFrom = pairedFrom == null || pairedFrom.isBlank() ? DEFAULT_ORIGIN : pairedFrom;
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_DEVICE_SECRET, deviceSecret);
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
     * Writes this response to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses a redemption response from a JSON tree map.
     *
     * @param map the input map
     * @return the redemption response
     * @throws IOException on a missing required field
     */
    public static PairingRedemptionResponse fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var deviceSecret = JsonTree.string(map, FIELD_DEVICE_SECRET);
        var deviceId = JsonTree.string(map, FIELD_DEVICE_ID);
        var label = JsonTree.string(map, FIELD_LABEL);
        if (deviceSecret == null || deviceId == null || label == null) {
            throw new JsonFormatException("Missing device_secret, device_id or label in PairingRedemptionResponse");
        }
        return new PairingRedemptionResponse(deviceSecret, deviceId, label, JsonTree.string(map, FIELD_PAIRED_FROM));
    }

    /**
     * Parses a redemption response from a JSON string.
     *
     * @param json the JSON string
     * @return the redemption response
     * @throws IOException on parse failure
     */
    public static PairingRedemptionResponse fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PairingRedemptionResponse");
        }
        return fromMap(obj);
    }

    /**
     * Reads one redemption response from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the redemption response
     * @throws IOException on parse failure
     */
    public static PairingRedemptionResponse readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PairingRedemptionResponse");
        }
        return fromMap(obj);
    }
}
