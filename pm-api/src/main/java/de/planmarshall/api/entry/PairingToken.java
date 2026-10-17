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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A short-lived pairing code the terminal client asks for and shows
 * (03-web-server.adoc#web-pairing). The code never reaches a browser except in the URL fragment.
 *
 * @param code              the eight-character pairing code, printed as {@code XXXX-XXXX}
 * @param url               the pairing URL carrying the code in its fragment
 * @param expiresInSeconds  the remaining validity in seconds
 */
public record PairingToken(String code, String url, long expiresInSeconds) {

    /** The default pairing code lifetime in seconds (10 minutes). */
    public static final long DEFAULT_LIFETIME_SECONDS = 600L;

    private static final String FIELD_CODE = "code";
    private static final String FIELD_URL = "url";
    private static final String FIELD_EXPIRES = "expires_in_seconds";

    public PairingToken {
        Objects.requireNonNull(code, FIELD_CODE);
        Objects.requireNonNull(url, FIELD_URL);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CODE, code);
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
     * Parses a pairing token from a JSON tree map.
     *
     * @param map the input map
     * @return the pairing token
     * @throws IOException on a missing required field
     */
    public static PairingToken fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var code = JsonTree.string(map, FIELD_CODE);
        var url = JsonTree.string(map, FIELD_URL);
        if (code == null || url == null) {
            throw new JsonFormatException("Missing code or url in PairingToken");
        }
        var expires = JsonTree.integer(map, FIELD_EXPIRES);
        return new PairingToken(code, url, expires != null ? expires : DEFAULT_LIFETIME_SECONDS);
    }

    /**
     * Parses a pairing token from a JSON string.
     *
     * @param json the JSON string
     * @return the pairing token
     * @throws IOException on parse failure
     */
    public static PairingToken fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PairingToken");
        }
        return fromMap(obj);
    }
}
