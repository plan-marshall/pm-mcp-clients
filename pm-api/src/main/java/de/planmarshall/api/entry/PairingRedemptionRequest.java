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
 * The body of a pairing redemption ({@code POST /api/v1/pair}): the code the operator read from
 * the terminal and the label the browser proposes for itself (03-web-server.adoc#web-pairing).
 *
 * @param code       the pairing code, with or without the dash and in any case
 * @param label      the proposed device label, or {@code null} to let the runtime choose
 * @param clientType the client kind that redeems ({@code web}), or {@code null}
 */
public record PairingRedemptionRequest(String code, @Nullable String label, @Nullable String clientType) {

    private static final String FIELD_CODE = "code";
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_CLIENT_TYPE = "client_type";

    public PairingRedemptionRequest {
        Objects.requireNonNull(code, FIELD_CODE);
    }

    /**
     * Converts to a JSON tree map, omitting absent optional fields.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CODE, code);
        if (label != null) {
            map.put(FIELD_LABEL, label);
        }
        if (clientType != null) {
            map.put(FIELD_CLIENT_TYPE, clientType);
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
     * Parses a redemption request from a JSON tree map.
     *
     * @param map the input map
     * @return the redemption request
     * @throws IOException on a missing code
     */
    public static PairingRedemptionRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var code = JsonTree.string(map, FIELD_CODE);
        if (code == null) {
            throw new JsonFormatException("Missing code in PairingRedemptionRequest");
        }
        return new PairingRedemptionRequest(
                code, JsonTree.string(map, FIELD_LABEL), JsonTree.string(map, FIELD_CLIENT_TYPE));
    }

    /**
     * Parses a redemption request from a JSON string.
     *
     * @param json the JSON string
     * @return the redemption request
     * @throws IOException on parse failure
     */
    public static PairingRedemptionRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PairingRedemptionRequest");
        }
        return fromMap(obj);
    }
}
