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
 * Event payload emitted when a web listener opens or closes ({@code listener.changed}); both
 * clients follow it to show the loopback and LAN access alike
 * (06-event-catalogue.adoc, 03-web-server.adoc#two-listeners).
 *
 * @param listener the listener that changed
 * @param state    its new state ({@code open}, {@code closed}, or {@code port_occupied})
 * @param port     the port it is bound to or was asked to bind
 */
public record ListenerChangedEvent(ListenerKind listener, ListenerState state, int port) {

    public static final String EVENT_NAME = "listener.changed";

    private static final String FIELD_LISTENER = "listener";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_PORT = "port";

    public ListenerChangedEvent {
        Objects.requireNonNull(listener, FIELD_LISTENER);
        Objects.requireNonNull(state, FIELD_STATE);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port out of range in ListenerChangedEvent: " + port);
        }
    }

    /**
     * @return {@code true} when the listener is now serving
     */
    public boolean isOpen() {
        return state == ListenerState.OPEN;
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LISTENER, listener.wireValue());
        map.put(FIELD_STATE, state.wireValue());
        map.put(FIELD_PORT, port);
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
     * Parses a listener-changed event from a JSON tree map.
     *
     * @param map the input map
     * @return the event
     * @throws IOException on a missing or unknown member
     */
    public static ListenerChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var listener = ListenerKind.fromWireOrNull(JsonTree.string(map, FIELD_LISTENER));
        if (listener == null) {
            throw new JsonFormatException("Missing or unknown listener in ListenerChangedEvent");
        }
        var state = ListenerState.fromWireOrNull(JsonTree.string(map, FIELD_STATE));
        if (state == null) {
            throw new JsonFormatException("Missing or unknown state in ListenerChangedEvent");
        }
        var port = JsonTree.integer(map, FIELD_PORT);
        if (port == null) {
            throw new JsonFormatException("Missing port in ListenerChangedEvent");
        }
        try {
            return new ListenerChangedEvent(listener, state, port);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid ListenerChangedEvent: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a listener-changed event from a JSON string.
     *
     * @param json the JSON string
     * @return the event
     * @throws IOException on parse failure
     */
    public static ListenerChangedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ListenerChangedEvent");
        }
        return fromMap(obj);
    }

    /**
     * Reads one listener-changed event from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the event
     * @throws IOException on parse failure
     */
    public static ListenerChangedEvent readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ListenerChangedEvent");
        }
        return fromMap(obj);
    }
}
