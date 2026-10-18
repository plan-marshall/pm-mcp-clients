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
 * Result of accepting a manual store edit
 * ({@code POST /api/v1/projects/{project_id}/stores/accept-edit}).
 *
 * @param status   the acceptance outcome, {@code accepted}
 * @param newSha256 the SHA-256 the store's digest sidecar now records
 */
public record AcceptEditResult(
        String status,
        String newSha256
) {
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_NEW_SHA256 = "new_sha256";

    /** The status of a completed edit acceptance. */
    public static final String STATUS_ACCEPTED = "accepted";

    public AcceptEditResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(newSha256, "newSha256");
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STATUS, status);
        map.put(FIELD_NEW_SHA256, newSha256);
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
     * @return parsed result
     * @throws IOException on format failure
     */
    public static AcceptEditResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var status = JsonTree.string(map, FIELD_STATUS);
        if (status == null || status.isBlank()) {
            status = STATUS_ACCEPTED;
        }
        var newSha256 = JsonTree.string(map, FIELD_NEW_SHA256);
        if (newSha256 == null) {
            newSha256 = JsonTree.string(map, "sha256");
        }
        if (newSha256 == null || newSha256.isBlank()) {
            throw new JsonFormatException("Missing new_sha256 in AcceptEditResult");
        }
        return new AcceptEditResult(status, newSha256);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed result
     * @throws IOException on format failure
     */
    public static AcceptEditResult fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for AcceptEditResult");
    }
}
