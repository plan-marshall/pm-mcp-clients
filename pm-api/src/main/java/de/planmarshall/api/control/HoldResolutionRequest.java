/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The operator's act that resolves a hold ({@code hold.resolve}, UC-CTL-4).
 *
 * @param act    the act value, one of the hold's {@code acts}
 * @param reason the operator's reason, required by the acts that cannot be undone, or {@code null}
 * @param params any further named parameters of the act, never {@code null}
 */
public record HoldResolutionRequest(
        String act,
        @Nullable String reason,
        Map<String, Object> params
) {
    public HoldResolutionRequest {
        Objects.requireNonNull(act, "act");
        params = params != null ? Map.copyOf(params) : Map.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("act", act);
        if (reason != null) {
            map.put("reason", reason);
        }
        map.putAll(params);
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON tree map.
     *
     * @param map map representation
     * @return parsed request
     * @throws IOException on format failure
     */
    public static HoldResolutionRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var act = JsonTree.string(map, "act");
        if (act == null) {
            throw new JsonFormatException("Missing act in HoldResolutionRequest");
        }
        var rest = new LinkedHashMap<String, Object>(map);
        rest.remove("act");
        rest.remove("reason");
        return new HoldResolutionRequest(act, JsonTree.string(map, "reason"), rest);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed request
     * @throws IOException on format failure
     */
    public static HoldResolutionRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for HoldResolutionRequest");
        }
        return fromMap(obj);
    }
}
