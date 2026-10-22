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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Resource representation of one machine-wide harness enrolment
 * ({@code GET /api/v1/harnesses/{client_id}}, {@code POST /api/v1/harnesses/{client_id}/enrol}):
 * the absolute executable path, the version the harness reports, and the SHA-256 digest of the
 * binary, and nothing else. Neither an environment value nor a credential is ever part of this
 * representation (PM-EXEC-1, CLI &amp; Security § Harness Enrolment).
 *
 * @param client         the client id of the harness's client profile
 * @param executablePath the normalized absolute path of the enrolled executable
 * @param version        the version the harness's version probe reported
 * @param sha256Digest   the lowercase-hex SHA-256 digest of the executable
 * @param status         the state of the enrolment
 */
public record HarnessEnrolmentResource(
        String client,
        String executablePath,
        String version,
        String sha256Digest,
        Status status
) {
    public static final String FIELD_CLIENT = "client";
    public static final String FIELD_EXECUTABLE_PATH = "executable_path";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_SHA256_DIGEST = "sha256_digest";
    public static final String FIELD_STATUS = "status";

    private static final Pattern CLIENT_ID = Pattern.compile("^[a-z][a-z0-9-]{0,31}$");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final int MAX_VERSION_LENGTH = 128;

    /**
     * The state of a harness enrolment.
     */
    public enum Status {

        /** The executable was detected and is shown for confirmation, not yet recorded. */
        DETECTED("detected"),

        /** The harness was enrolled in the machine's harness store. */
        ENROLLED("enrolled"),

        /** A changed executable was re-baselined in the machine's harness store. */
        UPDATED("updated");

        private final String wire;

        Status(String wire) {
            this.wire = wire;
        }

        /**
         * @return the wire representation of this status
         */
        public String wire() {
            return wire;
        }

        /**
         * @param wire the wire representation
         * @return the matching status, or {@code null} if none matches
         */
        public static @Nullable Status fromWire(@Nullable String wire) {
            if (wire == null) {
                return null;
            }
            for (var status : values()) {
                if (status.wire.equalsIgnoreCase(wire)) {
                    return status;
                }
            }
            return null;
        }

        /**
         * @param wire the wire representation
         * @return the matching status
         * @throws IllegalArgumentException if none matches (fail-closed)
         */
        public static Status requireWire(String wire) {
            var status = fromWire(wire);
            if (status == null) {
                throw new IllegalArgumentException("Unknown harness enrolment status: " + wire);
            }
            return status;
        }

        /**
         * @return the wire representations of every status, in declaration order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(Status::wire).toList();
        }
    }

    public HarnessEnrolmentResource {
        requireClient(client);
        executablePath = requireAbsolutePath(executablePath);
        requireText(version, FIELD_VERSION, MAX_VERSION_LENGTH);
        if (sha256Digest == null || !SHA256.matcher(sha256Digest).matches()) {
            throw new IllegalArgumentException(
                    "sha256Digest must be 64 lowercase hexadecimal characters");
        }
        Objects.requireNonNull(status, FIELD_STATUS);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CLIENT, client);
        map.put(FIELD_EXECUTABLE_PATH, executablePath);
        map.put(FIELD_VERSION, version);
        map.put(FIELD_SHA256_DIGEST, sha256Digest);
        map.put(FIELD_STATUS, status.wire());
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
    public static HarnessEnrolmentResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var client = JsonTree.string(map, FIELD_CLIENT);
        var executablePath = JsonTree.string(map, FIELD_EXECUTABLE_PATH);
        var version = JsonTree.string(map, FIELD_VERSION);
        var digest = JsonTree.string(map, FIELD_SHA256_DIGEST);
        var wireStatus = JsonTree.string(map, FIELD_STATUS);
        var status = Status.fromWire(wireStatus);
        if (status == null) {
            throw new JsonFormatException("Unknown harness enrolment status: " + wireStatus);
        }
        try {
            return new HarnessEnrolmentResource(client, executablePath, version, digest, status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JsonFormatException("Invalid HarnessEnrolmentResource: " + e.getMessage());
        }
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static HarnessEnrolmentResource fromJson(String json) throws IOException {
        return fromMap(ResourceJson.objectOf(json, "HarnessEnrolmentResource"));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static HarnessEnrolmentResource readFrom(JsonParser parser) throws IOException {
        return fromMap(ResourceJson.objectOf(parser, "HarnessEnrolmentResource"));
    }

    private static void requireClient(String client) {
        requireText(client, FIELD_CLIENT, 32);
        if (!CLIENT_ID.matcher(client).matches()) {
            throw new IllegalArgumentException(
                    "client must match " + CLIENT_ID.pattern() + ": " + client);
        }
    }

    private static String requireAbsolutePath(@Nullable String executablePath) {
        if (executablePath == null || executablePath.isBlank()) {
            throw new IllegalArgumentException("executablePath must not be blank");
        }
        try {
            var path = Path.of(executablePath);
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException("executablePath must be absolute: " + executablePath);
            }
            return path.normalize().toString();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("executablePath is not a valid path: " + executablePath, e);
        }
    }

    private static void requireText(@Nullable String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(field + " is longer than " + maximumLength + " characters");
        }
    }
}
