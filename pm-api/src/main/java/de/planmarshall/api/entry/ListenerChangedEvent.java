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
 * Event payload emitted when a listener opens or closes ({@code listener.changed}); both clients
 * follow it to show the loopback and LAN access alike (03-web-server.adoc#two-listeners).
 *
 * @param listener    the listener that changed
 * @param enabled     whether it is now open
 * @param url         the listener's URL, or {@code null} when it closed
 * @param fingerprint the LAN certificate fingerprint, or {@code null} when it closed or is loopback
 */
public record ListenerChangedEvent(
        ListenerKind listener,
        boolean enabled,
        @Nullable String url,
        @Nullable String fingerprint
) {

    public static final String EVENT_NAME = "listener.changed";

    private static final String FIELD_LISTENER = "listener";
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_URL = "url";
    private static final String FIELD_FINGERPRINT = "fingerprint";

    public ListenerChangedEvent {
        Objects.requireNonNull(listener, FIELD_LISTENER);
    }

    /**
     * Converts to a JSON tree map, omitting absent optional fields.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LISTENER, listener.wireValue());
        map.put(FIELD_ENABLED, enabled);
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
     * Parses a listener-changed event from a JSON tree map.
     *
     * @param map the input map
     * @return the parsed event
     * @throws IOException on a missing listener
     */
    public static ListenerChangedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var listener = JsonTree.string(map, FIELD_LISTENER);
        if (listener == null) {
            throw new JsonFormatException("Missing listener in ListenerChangedEvent");
        }
        return new ListenerChangedEvent(
                ListenerKind.fromWire(listener),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_ENABLED)),
                JsonTree.string(map, FIELD_URL),
                JsonTree.string(map, FIELD_FINGERPRINT));
    }

    /**
     * Parses a listener-changed event from a JSON string.
     *
     * @param json the JSON string
     * @return the parsed event
     * @throws IOException on parse failure
     */
    public static ListenerChangedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ListenerChangedEvent");
        }
        return fromMap(obj);
    }
}
