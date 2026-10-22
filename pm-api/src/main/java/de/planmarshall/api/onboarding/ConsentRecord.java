/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One remembered consent as kept in {@code consents.json} and listed in the machine settings
 * (UC-ONB-10, design Consents, The Remembered Consents).
 *
 * @param kind        the consent kind
 * @param status      the outcome of the consent state
 * @param remember    {@code true} when {@code yes, and don't ask again} was chosen
 * @param grantedAt   when the consent was given, ISO-8601, or {@code null} where unknown
 * @param channel     the channel that gave it, {@code terminal} or the label of a paired device
 * @param project     the project of a kind that belongs to a project, or {@code null}
 * @param scriptCount the number of scripts a {@code run_checkout_scripts} covers, or {@code null}
 */
public record ConsentRecord(
        ConsentKind kind,
        ConsentStatus status,
        boolean remember,
        @Nullable String grantedAt,
        @Nullable String channel,
        @Nullable String project,
        @Nullable Integer scriptCount
) {
    public ConsentRecord {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(status, "status is required");
    }

    /**
     * @return the message key of the kind's title
     */
    public String titleKey() {
        return kind.titleKey();
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("kind", kind.wireValue());
        map.put("status", status.wireValue());
        map.put("remember", remember);
        if (grantedAt != null) {
            map.put("granted_at", grantedAt);
        }
        if (channel != null) {
            map.put("channel", channel);
        }
        if (project != null) {
            map.put("project", project);
        }
        if (scriptCount != null) {
            map.put("script_count", scriptCount);
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
     * @param map map representation
     * @return parsed record
     * @throws IOException on format failure
     */
    public static ConsentRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kind = ConsentKind.fromWire(JsonTree.string(map, "kind"));
        if (kind == null) {
            throw new JsonFormatException("Missing or unknown kind in ConsentRecord");
        }
        var status = ConsentStatus.fromWire(JsonTree.string(map, "status"));
        if (status == null) {
            status = ConsentStatus.REMEMBERED;
        }
        return new ConsentRecord(kind, status,
                Boolean.TRUE.equals(JsonTree.bool(map, "remember")),
                JsonTree.string(map, "granted_at"),
                JsonTree.string(map, "channel"),
                JsonTree.string(map, "project"),
                JsonTree.integer(map, "script_count"));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed record
     * @throws IOException on format failure
     */
    public static ConsentRecord fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ConsentRecord");
        }
        return fromMap(obj);
    }
}
