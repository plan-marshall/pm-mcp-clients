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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The live state of the LAN listener: whether it is open, on which port, the host it is reached
 * at, and its certificate fingerprint (03-web-server.adoc#web-listener).
 *
 * @param enabled     whether the LAN listener is open
 * @param port        the port the listener bound ({@code web.lan_port})
 * @param host        the host name the listener is reached at, or {@code null} when closed
 * @param fingerprint the SHA-256 fingerprint of the DER certificate, or {@code null} when closed
 */
public record LanListenerConfig(
        boolean enabled,
        int port,
        @Nullable String host,
        @Nullable String fingerprint
) {
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_PORT = "port";
    private static final String FIELD_HOST = "host";
    private static final String FIELD_FINGERPRINT = "fingerprint";

    /**
     * Converts to a JSON tree map, omitting absent optional fields.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ENABLED, enabled);
        map.put(FIELD_PORT, port);
        if (host != null) {
            map.put(FIELD_HOST, host);
        }
        if (fingerprint != null) {
            map.put(FIELD_FINGERPRINT, fingerprint);
        }
        return map;
    }

    /**
     * @return compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a LAN listener configuration from a JSON tree map.
     *
     * @param map the input map
     * @return the listener configuration
     * @throws IOException on a missing port
     */
    public static LanListenerConfig fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var port = JsonTree.integer(map, FIELD_PORT);
        if (port == null) {
            throw new JsonFormatException("Missing port in LanListenerConfig");
        }
        return new LanListenerConfig(
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_ENABLED)),
                port,
                JsonTree.string(map, FIELD_HOST),
                JsonTree.string(map, FIELD_FINGERPRINT));
    }

    /**
     * Parses a LAN listener configuration from a JSON string.
     *
     * @param json the JSON string
     * @return the listener configuration
     * @throws IOException on parse failure
     */
    public static LanListenerConfig fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LanListenerConfig");
        }
        return fromMap(obj);
    }
}
