/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Event payload emitted when a client event stream is closed by the server ({@code stream.closed}).
 *
 * @param reason    the closure reason (e.g. {@code "device_revoked"}, {@code "listener_closed"}, {@code "device_expired"})
 * @param details   human-readable detail or explanation
 * @param revokedBy actor that revoked the device (e.g. {@code "terminal"}, device label)
 * @param revokedAt timestamp of revocation
 */
public record StreamClosedEvent(
String reason,
@Nullable String details,
@Nullable String revokedBy,
@Nullable String revokedAt
) {

    public static final String EVENT_NAME = "stream.closed";
    public static final String REASON_DEVICE_REVOKED = "device_revoked";
    public static final String REASON_LISTENER_CLOSED = "listener_closed";
    public static final String REASON_DEVICE_EXPIRED = "device_expired";

    public StreamClosedEvent {
        Objects.requireNonNull(reason, "reason");
    }

    public StreamClosedEvent(String reason, @Nullable String details) {
        this(reason, details, null, null);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("reason", reason);
        if (details != null) {
            map.put("details", details);
        }
        if (revokedBy != null) {
            map.put("revoked_by", revokedBy);
        }
        if (revokedAt != null) {
            map.put("revoked_at", revokedAt);
        }
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map tree map
     * @return parsed event
     */
    public static StreamClosedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var reason = JsonTree.string(map, "reason");
        if (reason == null) {
            throw new JsonFormatException("Missing reason in StreamClosedEvent");
        }
        var details = JsonTree.string(map, "details");
        var revokedBy = JsonTree.string(map, "revoked_by");
        if (revokedBy == null) {
            revokedBy = JsonTree.string(map, "revokedBy");
        }
        var revokedAt = JsonTree.string(map, "revoked_at");
        if (revokedAt == null) {
            revokedAt = JsonTree.string(map, "revokedAt");
        }
        return new StreamClosedEvent(reason, details, revokedBy, revokedAt);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed event
     * @throws IOException on parse failure
     */
    public static StreamClosedEvent fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for StreamClosedEvent");
        }
        return fromMap(obj);
    }
}
