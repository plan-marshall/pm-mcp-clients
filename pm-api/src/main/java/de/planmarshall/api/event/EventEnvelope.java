/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.event;

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
 * One event of a client stream, the envelope shared by every stream of the runtime: the closed
 * catalogue {@link EventName} (the SSE {@code event} field), the {@code id} that increases within its
 * stream, and {@code data}, a JSON object carrying {@code event}, the {@code at} instant (RFC 3339
 * UTC), the {@link EventScope}, and the payload the catalogue names for that event. The payload
 * carries identifiers, states, counts, and message keys with their arguments; text of a model, a
 * provider, or a harness travels only in a task's activity stream, marked untrusted.
 * <p>
 * The envelope is written as {@code {"event": <name>, "id": <id>, "data": {"event": <name>,
 * "at": <instant>, "scope": {...}, ...payload}}, and read back fail-closed: an event name that is no
 * catalogue member, or a {@code data.event} that differs from the envelope's {@code event}, is
 * refused with a {@link JsonFormatException}.
 *
 * @param event   the catalogue name of the event
 * @param id      the id of the event within its stream ({@code >= 0})
 * @param at      the instant the event happened
 * @param scope   the scope the event happened in
 * @param payload the event's payload, the members of {@code data} beside {@code event}, {@code at}, and {@code scope}
 */
public record EventEnvelope(
        EventName event,
        long id,
        Instant at,
        EventScope scope,
        Map<String, Object> payload
) {
    public static final String FIELD_EVENT = "event";
    public static final String FIELD_ID = "id";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_AT = "at";
    public static final String FIELD_SCOPE = "scope";

    public EventEnvelope {
        Objects.requireNonNull(event, FIELD_EVENT);
        Objects.requireNonNull(at, FIELD_AT);
        Objects.requireNonNull(scope, FIELD_SCOPE);
        if (id < 0) {
            throw new IllegalArgumentException("id cannot be negative: " + id);
        }
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    /**
     * Converts to a JSON tree map of the envelope.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var data = new LinkedHashMap<String, Object>();
        data.put(FIELD_EVENT, event.wire());
        data.put(FIELD_AT, at.toString());
        data.put(FIELD_SCOPE, scope.toMap());
        if (!payload.isEmpty()) {
            data.putAll(payload);
        }

        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_EVENT, event.wire());
        map.put(FIELD_ID, id);
        map.put(FIELD_DATA, data);
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
     * Writes this envelope to a streaming generator.
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
     * @return parsed envelope
     * @throws IOException on format failure
     */
    public static EventEnvelope fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");

        var eventWire = JsonTree.string(map, FIELD_EVENT);
        var event = EventName.fromWireOrNull(eventWire);
        if (event == null) {
            throw new JsonFormatException("Unknown event name in EventEnvelope: " + eventWire);
        }

        var id = JsonTree.integralNumber(map, FIELD_ID, 0, Long.MAX_VALUE);

        var data = JsonTree.object(map, FIELD_DATA);
        if (data == null) {
            throw new JsonFormatException("Missing " + FIELD_DATA + " in EventEnvelope");
        }

        var dataEvent = JsonTree.string(data, FIELD_EVENT);
        if (!event.wire().equals(dataEvent)) {
            throw new JsonFormatException(
                    "EventEnvelope data.event '" + dataEvent + "' does not match event '" + event.wire() + "'");
        }

        var at = instantOf(data, FIELD_AT);
        var scopeMap = JsonTree.object(data, FIELD_SCOPE);
        if (scopeMap == null) {
            throw new JsonFormatException("Missing " + FIELD_SCOPE + " in EventEnvelope data");
        }
        var scope = EventScope.fromMap(scopeMap);

        var payload = new LinkedHashMap<String, Object>();
        for (var entry : data.entrySet()) {
            if (!FIELD_EVENT.equals(entry.getKey()) && !FIELD_AT.equals(entry.getKey())
                    && !FIELD_SCOPE.equals(entry.getKey())) {
                payload.put(entry.getKey(), entry.getValue());
            }
        }

        return new EventEnvelope(event, id, at, scope, payload);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed envelope
     * @throws IOException on format failure
     */
    public static EventEnvelope fromJson(String json) throws IOException {
        return fromMap(objectOf(JsonTree.parse(json)));
    }

    /**
     * Reads one envelope from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed envelope
     * @throws IOException on format failure
     */
    public static EventEnvelope readFrom(JsonParser parser) throws IOException {
        return fromMap(objectOf(JsonTree.readValue(parser)));
    }

    private static Instant instantOf(Map<String, Object> map, String field) throws IOException {
        var text = JsonTree.string(map, field);
        if (text == null) {
            throw new JsonFormatException("Missing " + field + " in EventEnvelope data");
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException _) {
            throw new JsonFormatException("Invalid " + field + " in EventEnvelope data: " + text);
        }
    }

    private static Map<String, Object> objectOf(Object parsed) throws IOException {
        var map = JsonTree.asObject(parsed);
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for EventEnvelope");
        }
        return map;
    }
}
