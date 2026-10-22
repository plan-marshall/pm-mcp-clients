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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Resource representation of the runtime-wide event stream ({@code GET /api/v1/events}): whether the
 * stream is open, how many listeners hold it, and the id of the last event it announced, so a client
 * can resume where it left off.
 *
 * @param status          the status of the event stream ({@code open}, {@code closed}, …)
 * @param activeListeners the number of listeners that hold the stream
 * @param lastEventId     the id of the last announced event, or {@code null} when none was announced
 * @param links           the links of the resource
 */
public record RuntimeEventStreamResource(
        String status,
        int activeListeners,
        @Nullable String lastEventId,
        List<ResourceLink> links
) {
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_ACTIVE_LISTENERS = "active_listeners";
    public static final String FIELD_LAST_EVENT_ID = "last_event_id";

    public RuntimeEventStreamResource {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Missing status in RuntimeEventStreamResource");
        }
        if (activeListeners < 0) {
            throw new IllegalArgumentException(
                    "activeListeners must not be negative in RuntimeEventStreamResource");
        }
        links = links == null ? List.of() : List.copyOf(links);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STATUS, status);
        map.put(FIELD_ACTIVE_LISTENERS, activeListeners);
        if (lastEventId != null) {
            map.put(FIELD_LAST_EVENT_ID, lastEventId);
        }
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
    public static RuntimeEventStreamResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var status = JsonTree.string(map, FIELD_STATUS);
        if (status == null || status.isBlank()) {
            throw new JsonFormatException("Missing status in RuntimeEventStreamResource");
        }
        return new RuntimeEventStreamResource(
                status,
                Math.toIntExact(JsonTree.integralNumber(
                        map, FIELD_ACTIVE_LISTENERS, 0, Integer.MAX_VALUE)),
                JsonTree.string(map, FIELD_LAST_EVENT_ID),
                ResourceLink.fromArray(map.get(ResourceLink.FIELD_LINKS)));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RuntimeEventStreamResource fromJson(String json) throws IOException {
        return fromMap(ResourceLink.objectOf(json, "RuntimeEventStreamResource"));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RuntimeEventStreamResource readFrom(JsonParser parser) throws IOException {
        return fromMap(ResourceLink.objectOf(parser, "RuntimeEventStreamResource"));
    }
}
