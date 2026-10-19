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
 * The live state of one web listener as the runtime status carries it
 * (03-web-server.adoc#two-listeners, 03-web-server.adoc#web-listener): its kind, whether it is
 * open, the port it bound, its URL, and for the LAN listener the certificate fingerprint.
 *
 * @param listener    the listener kind
 * @param state       the listener's state
 * @param port        the port it is bound to, or {@code null} when the status carries none
 * @param url         the URL it is reached at, or {@code null} when it is closed <em>or</em> the
 *                    status carries none
 * @param fingerprint the SHA-256 fingerprint of the DER certificate, or {@code null} when the
 *                    listener is closed, is the loopback listener, or the status carries none
 */
public record ListenerResource(
        ListenerKind listener,
        ListenerState state,
        @Nullable Integer port,
        @Nullable String url,
        @Nullable String fingerprint
) {

    private static final String FIELD_LISTENER = "listener";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_PORT = "port";
    private static final String FIELD_URL = "url";
    private static final String FIELD_FINGERPRINT = "fingerprint";

    public ListenerResource {
        Objects.requireNonNull(listener, FIELD_LISTENER);
        Objects.requireNonNull(state, FIELD_STATE);
        if (port != null && (port < 0 || port > 65535)) {
            throw new IllegalArgumentException("port out of range in ListenerResource: " + port);
        }
    }

    /**
     * @return {@code true} when the listener is serving
     */
    public boolean isOpen() {
        return state == ListenerState.OPEN;
    }

    /**
     * Converts to a JSON tree map, omitting absent optional fields.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LISTENER, listener.wireValue());
        map.put(FIELD_STATE, state.wireValue());
        if (port != null) {
            map.put(FIELD_PORT, port);
        }
        if (url != null) {
            map.put(FIELD_URL, url);
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
     * Writes this resource to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses a listener resource from a JSON tree map.
     *
     * @param map the input map
     * @return the listener resource
     * @throws IOException on a missing or unknown member
     */
    public static ListenerResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var listener = ListenerKind.fromWireOrNull(JsonTree.string(map, FIELD_LISTENER));
        if (listener == null) {
            throw new JsonFormatException("Missing or unknown listener in ListenerResource");
        }
        var state = ListenerState.fromWireOrNull(JsonTree.string(map, FIELD_STATE));
        if (state == null) {
            throw new JsonFormatException("Missing or unknown state in ListenerResource");
        }
        return new ListenerResource(
                listener,
                state,
                JsonTree.integer(map, FIELD_PORT),
                JsonTree.string(map, FIELD_URL),
                JsonTree.string(map, FIELD_FINGERPRINT));
    }

    /**
     * Parses a listener resource from a JSON string.
     *
     * @param json the JSON string
     * @return the listener resource
     * @throws IOException on parse failure
     */
    public static ListenerResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ListenerResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one listener resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the listener resource
     * @throws IOException on parse failure
     */
    public static ListenerResource readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ListenerResource");
        }
        return fromMap(obj);
    }
}
