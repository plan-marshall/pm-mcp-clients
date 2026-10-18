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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Resource representation of a runtime stop request ({@code POST /api/v1/operator/runtime/stop}):
 * how the runtime stops and how long it may take to drain before the stop turns immediate.
 *
 * @param mode           how the runtime stops
 * @param timeoutSeconds the drain bound in seconds, {@code 0} for an immediate stop
 * @param links          the links of the resource
 */
public record RuntimeStopResource(
        StopMode mode,
        long timeoutSeconds,
        List<ResourceLink> links
) {
    public static final String FIELD_MODE = "mode";
    public static final String FIELD_TIMEOUT_SECONDS = "timeout_seconds";

    /**
     * The closed ways a runtime stops.
     */
    public enum StopMode {

        /** Stop at once, without waiting for running work. */
        IMMEDIATE("immediate"),

        /** Stop after the running work drained or the drain bound elapsed. */
        DRAIN("drain");

        private final String wire;

        StopMode(String wire) {
            this.wire = wire;
        }

        /**
         * @return the wire representation of this mode
         */
        public String wire() {
            return wire;
        }

        /**
         * Resolves a wire value to its mode.
         *
         * @param wire the wire representation
         * @return the matching mode, or {@code null} if none matches
         */
        public static StopMode fromWire(String wire) {
            if (wire == null) {
                return null;
            }
            for (var mode : values()) {
                if (mode.wire.equalsIgnoreCase(wire)) {
                    return mode;
                }
            }
            return null;
        }

        /**
         * @param wire the wire representation
         * @return the matching mode
         * @throws IllegalArgumentException if no mode matches (fail-closed)
         */
        public static StopMode requireWire(String wire) {
            var mode = fromWire(wire);
            if (mode == null) {
                throw new IllegalArgumentException("Unknown stop mode: " + wire);
            }
            return mode;
        }

        /**
         * @return the wire representations of every mode, in declaration order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(StopMode::wire).toList();
        }
    }

    public RuntimeStopResource {
        Objects.requireNonNull(mode, FIELD_MODE);
        if (timeoutSeconds < 0) {
            throw new IllegalArgumentException(
                    "timeoutSeconds must not be negative in RuntimeStopResource");
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
        map.put(FIELD_MODE, mode.wire());
        map.put(FIELD_TIMEOUT_SECONDS, timeoutSeconds);
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
    public static RuntimeStopResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var wire = JsonTree.string(map, FIELD_MODE);
        var mode = StopMode.fromWire(wire);
        if (mode == null) {
            throw new JsonFormatException("Unknown stop mode in RuntimeStopResource: " + wire);
        }
        return new RuntimeStopResource(
                mode,
                JsonTree.integralNumber(map, FIELD_TIMEOUT_SECONDS, 0, Long.MAX_VALUE),
                ResourceLink.fromArray(map.get(ResourceLink.FIELD_LINKS)));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RuntimeStopResource fromJson(String json) throws IOException {
        return fromMap(ResourceLink.objectOf(json, "RuntimeStopResource"));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static RuntimeStopResource readFrom(JsonParser parser) throws IOException {
        return fromMap(ResourceLink.objectOf(parser, "RuntimeStopResource"));
    }
}
