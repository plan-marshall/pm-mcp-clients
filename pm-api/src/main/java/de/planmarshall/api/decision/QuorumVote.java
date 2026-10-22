/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decision;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;

/**
 * Individual model vote within a decision quorum (UC-PLN-2).
 *
 * @param role              role casting the vote (e.g. {@code "architecture"}, {@code "quorum-a"})
 * @param model             model identifier (e.g. {@code "gemini-2.5-pro"})
 * @param confidencePercent self-reported confidence integer (0-100)
 * @param reason            optional voting rationale
 */
public record QuorumVote(
        String role,
        String model,
        @Nullable Integer confidencePercent,
        @Nullable String reason
) {
    public QuorumVote {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(model, "model");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("role", role);
        map.put("model", model);
        if (confidencePercent != null) {
            map.put("confidence_percent", confidencePercent);
        }
        if (reason != null) {
            map.put("reason", reason);
        }
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed vote
     */
    public static QuorumVote fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var role = JsonTree.string(map, "role");
        if (role == null) role = "";
        var model = JsonTree.string(map, "model");
        if (model == null) model = "";
        var conf = JsonTree.integer(map, "confidence_percent");
        if (conf == null) {
            conf = JsonTree.integer(map, "confidence");
        }
        var reason = JsonTree.string(map, "reason");
        return new QuorumVote(role, model, conf, reason);
    }
}
