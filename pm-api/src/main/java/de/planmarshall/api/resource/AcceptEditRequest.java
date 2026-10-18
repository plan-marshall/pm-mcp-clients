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
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Request payload of {@code POST /api/v1/projects/{project_id}/stores/accept-edit}: the operator's
 * acceptance of a manual store edit, re-baselining its digest (Store Schemas § Store Digest
 * Verification, PM-SEC-3).
 * <p>
 * The request carries the SHA-256 the operator confirmed; the runtime refuses the acceptance when
 * it does not match the file's current digest, so a confirmation can never be sent for a store the
 * operator did not see.
 *
 * @param storePath       the store's path relative to its scope root
 * @param confirmedSha256 the SHA-256 of the current file the operator confirmed
 */
public record AcceptEditRequest(
        String storePath,
        String confirmedSha256
) {
    public static final String FIELD_STORE_PATH = "store_path";
    public static final String FIELD_CONFIRMED_SHA256 = "confirmed_sha256";

    public AcceptEditRequest {
        Objects.requireNonNull(storePath, "storePath");
        Objects.requireNonNull(confirmedSha256, "confirmedSha256");
        if (storePath.isBlank()) {
            throw new IllegalArgumentException("storePath must not be blank");
        }
        if (confirmedSha256.isBlank()) {
            throw new IllegalArgumentException("confirmedSha256 must not be blank");
        }
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STORE_PATH, storePath);
        map.put(FIELD_CONFIRMED_SHA256, confirmedSha256);
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
     * Parses from a JSON tree map.
     *
     * @param map input map
     * @return parsed request
     * @throws IOException on format failure
     */
    public static AcceptEditRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var storePath = JsonTree.string(map, FIELD_STORE_PATH);
        if (storePath == null) {
            storePath = JsonTree.string(map, "path");
        }
        if (storePath == null || storePath.isBlank()) {
            throw new JsonFormatException("Missing store_path in AcceptEditRequest");
        }
        var confirmedSha256 = JsonTree.string(map, FIELD_CONFIRMED_SHA256);
        if (confirmedSha256 == null) {
            confirmedSha256 = JsonTree.string(map, "sha256");
        }
        if (confirmedSha256 == null || confirmedSha256.isBlank()) {
            throw new JsonFormatException("Missing confirmed_sha256 in AcceptEditRequest");
        }
        return new AcceptEditRequest(storePath, confirmedSha256);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed request
     * @throws IOException on format failure
     */
    public static AcceptEditRequest fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for AcceptEditRequest");
    }
}
