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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The recorded and current size and SHA-256 of a store, as the operator's {@code accept-edit} shows
 * them side by side before the confirmation (Store Schemas § Store Digest Verification, PM-SEC-3).
 * <p>
 * The recorded values come from the store's digest sidecar, the current values from the file on
 * disk. {@link #mismatch()} is derived: it is {@code true} exactly when the two differ.
 *
 * @param storePath      the store's path relative to its scope root
 * @param recordedSize   the size the sidecar records, in bytes
 * @param recordedSha256 the SHA-256 the sidecar records
 * @param currentSize    the size the file currently has, in bytes
 * @param currentSha256  the SHA-256 the file currently has
 * @param mismatchDetails a human-readable description of the difference, or {@code null}
 */
public record StoreDigestComparison(
        String storePath,
        long recordedSize,
        String recordedSha256,
        long currentSize,
        String currentSha256,
        @Nullable String mismatchDetails
) {
    public static final String FIELD_STORE_PATH = "store_path";
    public static final String FIELD_RECORDED_SIZE = "recorded_size";
    public static final String FIELD_RECORDED_SHA256 = "recorded_sha256";
    public static final String FIELD_CURRENT_SIZE = "current_size";
    public static final String FIELD_CURRENT_SHA256 = "current_sha256";
    public static final String FIELD_MISMATCH = "mismatch";
    public static final String FIELD_MISMATCH_DETAILS = "mismatch_details";

    public StoreDigestComparison {
        Objects.requireNonNull(storePath, "storePath");
        recordedSha256 = recordedSha256 == null ? "" : recordedSha256;
        currentSha256 = currentSha256 == null ? "" : currentSha256;
    }

    /**
     * @return whether the current file differs from the recorded digest
     */
    public boolean mismatch() {
        return recordedSize != currentSize || !recordedSha256.equals(currentSha256);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STORE_PATH, storePath);
        map.put(FIELD_RECORDED_SIZE, recordedSize);
        map.put(FIELD_RECORDED_SHA256, recordedSha256);
        map.put(FIELD_CURRENT_SIZE, currentSize);
        map.put(FIELD_CURRENT_SHA256, currentSha256);
        map.put(FIELD_MISMATCH, mismatch());
        if (mismatchDetails != null) {
            map.put(FIELD_MISMATCH_DETAILS, mismatchDetails);
        }
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
     * @return parsed comparison
     * @throws IOException on format failure
     */
    public static StoreDigestComparison fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var storePath = JsonTree.string(map, FIELD_STORE_PATH);
        if (storePath == null) {
            storePath = JsonTree.string(map, "path");
        }
        if (storePath == null || storePath.isBlank()) {
            throw new JsonFormatException("Missing store_path in StoreDigestComparison");
        }

        var recordedSize = size(map, FIELD_RECORDED_SIZE);
        var currentSize = size(map, FIELD_CURRENT_SIZE);
        var recordedSha256 = stringOrDefault(map, FIELD_RECORDED_SHA256, "recorded_digest");
        var currentSha256 = stringOrDefault(map, FIELD_CURRENT_SHA256, "current_digest");
        var mismatchDetails = stringOrDefault(map, FIELD_MISMATCH_DETAILS, "details");

        return new StoreDigestComparison(storePath, recordedSize, recordedSha256,
                currentSize, currentSha256, mismatchDetails);
    }

    private static long size(Map<String, Object> map, String key) {
        var number = JsonTree.number(map, key);
        return number != null ? number.longValue() : 0L;
    }

    private static @Nullable String stringOrDefault(Map<String, Object> map, String key1, String key2) {
        var value = JsonTree.string(map, key1);
        return value != null ? value : JsonTree.string(map, key2);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed comparison
     * @throws IOException on format failure
     */
    public static StoreDigestComparison fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for StoreDigestComparison");
    }
}
