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
import java.util.Arrays;
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
 * Resource representation of a credential entry's status
 * ({@code GET /api/v1/credentials/{credential_key}}): whether the entry exists, the path of the
 * token it reads, and when that token expires. The secret itself never leaves the runtime and is
 * never part of this representation.
 *
 * @param status    whether the credential entry is present
 * @param tokenPath the path of the token, or {@code null} when no entry is present
 * @param expiry    when the token expires, or {@code null} when there is no known expiry
 * @param links     the links of the resource
 */
public record CredentialStatusResource(
        Availability status,
        @Nullable String tokenPath,
        @Nullable Instant expiry,
        List<ResourceLink> links
) {
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_TOKEN_PATH = "token_path";
    public static final String FIELD_EXPIRY = "expiry";

    /**
     * The closed availability of a credential entry.
     */
    public enum Availability {

        /** The entry exists in the chosen store. */
        PRESENT("present"),

        /** No entry of that key exists; this is a status, never a refusal. */
        NOT_FOUND("not_found");

        private final String wire;

        Availability(String wire) {
            this.wire = wire;
        }

        /**
         * @return the wire representation of this availability
         */
        public String wire() {
            return wire;
        }

        /**
         * Resolves a wire value to its availability.
         *
         * @param wire the wire representation
         * @return the matching availability, or {@code null} if none matches
         */
        public static Availability fromWire(String wire) {
            if (wire == null) {
                return null;
            }
            for (var availability : values()) {
                if (availability.wire.equalsIgnoreCase(wire)) {
                    return availability;
                }
            }
            return null;
        }

        /**
         * @param wire the wire representation
         * @return the matching availability
         * @throws IllegalArgumentException if none matches (fail-closed)
         */
        public static Availability requireWire(String wire) {
            var availability = fromWire(wire);
            if (availability == null) {
                throw new IllegalArgumentException("Unknown credential availability: " + wire);
            }
            return availability;
        }

        /**
         * @return the wire representations of every availability, in declaration order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(Availability::wire).toList();
        }
    }

    public CredentialStatusResource {
        Objects.requireNonNull(status, FIELD_STATUS);
        links = links == null ? List.of() : List.copyOf(links);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STATUS, status.wire());
        if (tokenPath != null) {
            map.put(FIELD_TOKEN_PATH, tokenPath);
        }
        if (expiry != null) {
            map.put(FIELD_EXPIRY, expiry.toString());
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
    public static CredentialStatusResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var wire = JsonTree.string(map, FIELD_STATUS);
        var status = Availability.fromWire(wire);
        if (status == null) {
            throw new JsonFormatException("Unknown credential availability: " + wire);
        }
        return new CredentialStatusResource(
                status,
                JsonTree.string(map, FIELD_TOKEN_PATH),
                instantOf(map, FIELD_EXPIRY),
                ResourceLink.fromArray(map.get(ResourceLink.FIELD_LINKS)));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static CredentialStatusResource fromJson(String json) throws IOException {
        return fromMap(ResourceLink.objectOf(json, "CredentialStatusResource"));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static CredentialStatusResource readFrom(JsonParser parser) throws IOException {
        return fromMap(ResourceLink.objectOf(parser, "CredentialStatusResource"));
    }

    private static @Nullable Instant instantOf(Map<String, Object> map, String field) throws IOException {
        var text = JsonTree.string(map, field);
        if (text == null) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException _) {
            throw new JsonFormatException("Invalid " + field + " in CredentialStatusResource: " + text);
        }
    }
}
