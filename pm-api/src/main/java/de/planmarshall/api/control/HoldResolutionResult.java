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
 * The result of applying a hold's act ({@code hold.apply}, UC-CTL-4).
 *
 * <p>Released, the plan, the epic, or the landing goes on; still held, the hold is shown again with
 * what still holds and its cause.
 *
 * @param holdId   the hold the act applied to
 * @param released {@code true} if the subject goes on
 * @param cause    what still holds when it was not released, or {@code null}
 * @param subject  the plan, epic, or landing that was held
 */
public record HoldResolutionResult(
        String holdId,
        boolean released,
        @Nullable String cause,
        String subject
) {
    public HoldResolutionResult {
        Objects.requireNonNull(holdId, "holdId");
        subject = subject != null ? subject : holdId;
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("hold_id", holdId);
        map.put("released", released);
        map.put("subject", subject);
        if (cause != null) {
            map.put("cause", cause);
        }
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
     * @return parsed result
     * @throws IOException on format failure
     */
    public static HoldResolutionResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var holdId = JsonTree.string(map, "hold_id");
        if (holdId == null) {
            throw new JsonFormatException("Missing hold_id in HoldResolutionResult");
        }
        return new HoldResolutionResult(
                holdId,
                Boolean.TRUE.equals(JsonTree.bool(map, "released")),
                JsonTree.string(map, "cause"),
                JsonTree.string(map, "subject") != null ? JsonTree.string(map, "subject") : holdId
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed result
     * @throws IOException on format failure
     */
    public static HoldResolutionResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for HoldResolutionResult");
        }
        return fromMap(obj);
    }
}
