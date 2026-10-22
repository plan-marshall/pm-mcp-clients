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

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The connection status of the entry: the loopback URL, the LAN listener and its fingerprint,
 * and how many devices can act on the machine (03-web-server.adoc#two-listeners).
 *
 * @param loopbackUrl        the loopback URL of this machine, or {@code null} when the listener is closed
 * @param loopbackOpen       whether the loopback listener is open
 * @param lanEnabled         whether the LAN listener is open
 * @param lanUrl             the LAN URL, or {@code null} when it is closed
 * @param lanFingerprint     the LAN certificate fingerprint, or {@code null} when it is closed
 * @param pairedDevicesCount how many devices are paired
 * @param onboarded          whether the machine's onboarding has completed
 */
public record ConnectionStatus(
        @Nullable String loopbackUrl,
        boolean loopbackOpen,
        boolean lanEnabled,
        @Nullable String lanUrl,
        @Nullable String lanFingerprint,
        int pairedDevicesCount,
        boolean onboarded
) {
    private static final String FIELD_LOOPBACK_URL = "loopback_url";
    private static final String FIELD_LOOPBACK_OPEN = "loopback_open";
    private static final String FIELD_LAN_ENABLED = "lan_enabled";
    private static final String FIELD_LAN_URL = "lan_url";
    private static final String FIELD_LAN_FINGERPRINT = "lan_fingerprint";
    private static final String FIELD_PAIRED_DEVICES = "paired_devices_count";
    private static final String FIELD_ONBOARDED = "onboarded";

    /**
     * Converts to a JSON tree map, omitting absent optional fields.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (loopbackUrl != null) {
            map.put(FIELD_LOOPBACK_URL, loopbackUrl);
        }
        map.put(FIELD_LOOPBACK_OPEN, loopbackOpen);
        map.put(FIELD_LAN_ENABLED, lanEnabled);
        if (lanUrl != null) {
            map.put(FIELD_LAN_URL, lanUrl);
        }
        if (lanFingerprint != null) {
            map.put(FIELD_LAN_FINGERPRINT, lanFingerprint);
        }
        map.put(FIELD_PAIRED_DEVICES, pairedDevicesCount);
        map.put(FIELD_ONBOARDED, onboarded);
        return map;
    }

    /**
     * @return compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this connection status to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses a connection status from a JSON tree map.
     *
     * @param map the input map
     * @return the connection status
     */
    public static ConnectionStatus fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var count = JsonTree.integer(map, FIELD_PAIRED_DEVICES);
        return new ConnectionStatus(
                JsonTree.string(map, FIELD_LOOPBACK_URL),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_LOOPBACK_OPEN)),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_LAN_ENABLED)),
                JsonTree.string(map, FIELD_LAN_URL),
                JsonTree.string(map, FIELD_LAN_FINGERPRINT),
                count != null ? count : 0,
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_ONBOARDED)));
    }

    /**
     * Parses a connection status from a JSON string.
     *
     * @param json the JSON string
     * @return the connection status
     * @throws IOException on parse failure
     */
    public static ConnectionStatus fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ConnectionStatus");
        }
        return fromMap(obj);
    }

    /**
     * Reads one connection status from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the connection status
     * @throws IOException on parse failure
     */
    public static ConnectionStatus readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ConnectionStatus");
        }
        return fromMap(obj);
    }
}
