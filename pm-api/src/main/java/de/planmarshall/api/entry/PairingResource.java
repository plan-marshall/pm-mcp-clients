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
 * The answer of {@code POST /api/v1/web/pairings}: the code the terminal client shows, the URL that
 * carries it in its fragment, and how long it stays valid
 * (03-web-server.adoc#web-pairing, 03-web-server.adoc#fresh-code).
 *
 * @param code              the pairing code
 * @param url               the pairing URL carrying the code in its fragment
 * @param expiresInSeconds  the remaining validity in seconds
 */
public record PairingResource(PairingCode code, String url, long expiresInSeconds) {

    /** The default pairing code lifetime in seconds (10 minutes). */
    public static final long DEFAULT_LIFETIME_SECONDS = 600L;

    private static final String FIELD_CODE = "code";
    private static final String FIELD_URL = "url";
    private static final String FIELD_EXPIRES = "expires_in_seconds";

    public PairingResource {
        Objects.requireNonNull(code, FIELD_CODE);
        Objects.requireNonNull(url, FIELD_URL);
        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException(
                    "expiresInSeconds must be positive in PairingResource: " + expiresInSeconds);
        }
    }

    /**
     * Convenience factory with the default lifetime.
     *
     * @param code the pairing code
     * @param url  the pairing URL
     * @return the pairing resource
     */
    public static PairingResource of(PairingCode code, String url) {
        return new PairingResource(code, url, DEFAULT_LIFETIME_SECONDS);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CODE, code.display());
        map.put(FIELD_URL, url);
        map.put(FIELD_EXPIRES, expiresInSeconds);
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
     * Parses a pairing resource from a JSON tree map.
     *
     * @param map the input map
     * @return the pairing resource
     * @throws IOException on a missing or malformed member
     */
    public static PairingResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var code = JsonTree.string(map, FIELD_CODE);
        var url = JsonTree.string(map, FIELD_URL);
        if (code == null || url == null) {
            throw new JsonFormatException("Missing code or url in PairingResource");
        }
        final PairingCode pairingCode;
        try {
            pairingCode = new PairingCode(code);
        } catch (IllegalArgumentException _) {
            throw new JsonFormatException("Invalid pairing code in PairingResource: " + code);
        }
        var expires = JsonTree.integer(map, FIELD_EXPIRES);
        return new PairingResource(
                pairingCode,
                url,
                expires != null ? expires : DEFAULT_LIFETIME_SECONDS);
    }

    /**
     * Parses a pairing resource from a JSON string.
     *
     * @param json the JSON string
     * @return the pairing resource
     * @throws IOException on parse failure
     */
    public static PairingResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PairingResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one pairing resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the pairing resource
     * @throws IOException on parse failure
     */
    public static PairingResource readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PairingResource");
        }
        return fromMap(obj);
    }
}
