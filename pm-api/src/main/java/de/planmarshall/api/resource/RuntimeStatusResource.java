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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Resource representation of {@code GET /api/v1/status} for the daemon's runtime: its lifecycle
 * state, version, process id, start instant, and socket path, with the links that reach the
 * runtime's other resources.
 *
 * @param state      the lifecycle state of the runtime ({@code running}, {@code draining}, …)
 * @param version    the runtime's version
 * @param pid        the process id of the runtime
 * @param startedAt  the instant the runtime started
 * @param socketPath the path of the runtime's Unix domain socket
 * @param links      the links of the resource
 */
public record RuntimeStatusResource(
        String state,
        String version,
        long pid,
        Instant startedAt,
        String socketPath,
        List<ResourceLink> links
) {
    public static final String FIELD_STATE = "state";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_PID = "pid";
    public static final String FIELD_STARTED_AT = "started_at";
    public static final String FIELD_SOCKET_PATH = "socket_path";

    public RuntimeStatusResource {
        requireText(state, FIELD_STATE);
        requireText(version, FIELD_VERSION);
        requireText(socketPath, FIELD_SOCKET_PATH);
        Objects.requireNonNull(startedAt, FIELD_STARTED_AT);
        links = links == null ? List.of() : List.copyOf(links);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STATE, state);
        map.put(FIELD_VERSION, version);
        map.put(FIELD_PID, pid);
        map.put(FIELD_STARTED_AT, startedAt.toString());
        map.put(FIELD_SOCKET_PATH, socketPath);
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
    public static RuntimeStatusResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        return new RuntimeStatusResource(
                text(map, FIELD_STATE),
                text(map, FIELD_VERSION),
                JsonTree.integralNumber(map, FIELD_PID, 0, Long.MAX_VALUE),
                instantOf(map, FIELD_STARTED_AT),
                text(map, FIELD_SOCKET_PATH),
                ResourceLink.fromArray(map.get(ResourceLink.FIELD_LINKS)));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RuntimeStatusResource fromJson(String json) throws IOException {
        return fromMap(ResourceLink.objectOf(json, "RuntimeStatusResource"));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RuntimeStatusResource readFrom(JsonParser parser) throws IOException {
        return fromMap(ResourceLink.objectOf(parser, "RuntimeStatusResource"));
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + field + " in RuntimeStatusResource");
        }
    }

    private static String text(Map<String, Object> map, String field) throws IOException {
        var value = JsonTree.string(map, field);
        if (value == null || value.isBlank()) {
            throw new JsonFormatException("Missing " + field + " in RuntimeStatusResource");
        }
        return value;
    }

    private static Instant instantOf(Map<String, Object> map, String field) throws IOException {
        var text = JsonTree.string(map, field);
        if (text == null) {
            throw new JsonFormatException("Missing " + field + " in RuntimeStatusResource");
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException _) {
            throw new JsonFormatException("Invalid " + field + " in RuntimeStatusResource: " + text);
        }
    }
}
