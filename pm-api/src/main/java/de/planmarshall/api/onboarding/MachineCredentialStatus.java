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
 * One credential the machine keeps, with its provider, its kind, and its expiry from the last
 * check (UC-ONB-10; the credential section of the machine's settings).
 *
 * @param key          the credential key, e.g. {@code github}
 * @param provider     the provider that uses it, e.g. {@code GitHub}
 * @param kind         the kind of the credential, e.g. {@code fine-grained}
 * @param expiresInDays the days until it expires, or {@code null} where no expiry is known
 * @param expired      {@code true} when it has expired
 * @param heldPlan     a plan it holds while expired, or {@code null}
 */
public record MachineCredentialStatus(
        String key,
        String provider,
        String kind,
        @Nullable Integer expiresInDays,
        boolean expired,
        @Nullable String heldPlan
) {
    public MachineCredentialStatus {
        Objects.requireNonNull(key, "key");
        provider = provider != null && !provider.isBlank() ? provider : key;
        kind = kind != null && !kind.isBlank() ? kind : "token";
    }

    /**
     * @return {@code true} when the credential expires within fourteen days
     */
    public boolean expiresSoon() {
        return expiresInDays != null && expiresInDays >= 0 && expiresInDays <= 14;
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("key", key);
        map.put("provider", provider);
        map.put("kind", kind);
        if (expiresInDays != null) {
            map.put("expires_in_days", expiresInDays);
        }
        map.put("expired", expired);
        if (heldPlan != null) {
            map.put("held_plan", heldPlan);
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
    public static MachineCredentialStatus fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var key = JsonTree.string(map, "key");
        if (key == null) {
            key = JsonTree.string(map, "credential_key");
        }
        if (key == null) {
            throw new JsonFormatException("Missing key in MachineCredentialStatus");
        }
        return new MachineCredentialStatus(key,
                JsonTree.string(map, "provider"),
                JsonTree.string(map, "kind"),
                JsonTree.integer(map, "expires_in_days"),
                Boolean.TRUE.equals(JsonTree.bool(map, "expired")),
                JsonTree.string(map, "held_plan"));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed record
     * @throws IOException on format failure
     */
    public static MachineCredentialStatus fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for MachineCredentialStatus");
        }
        return fromMap(obj);
    }
}
