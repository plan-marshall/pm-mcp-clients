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

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The request payload of a plan or epic action ({@code plan.pause-when}, {@code plan.confirm-end},
 * UC-CTL-1 and UC-CTL-2).
 *
 * @param point   for a pause, where the plan holds: {@code now}, {@code task}, or {@code phase};
 *                otherwise {@code null}
 * @param reason  the reason of a cancel or an abandon, or {@code null}
 * @param closePr whether an open pull request is closed with the end, or {@code null} to leave it
 * @param raiseScope for an epic's {@code next}, whether the scope is raised by one, or {@code null}
 * @param withPlans  for an epic's {@code cancel}, whether its plans are cancelled too, or {@code null}
 * @param extra   any further named parameters of the action, never {@code null}
 */
public record PlanActionRequest(
        @Nullable String point,
        @Nullable String reason,
        @Nullable Boolean closePr,
        @Nullable Boolean raiseScope,
        @Nullable Boolean withPlans,
        Map<String, Object> extra
) {
    public PlanActionRequest {
        extra = extra != null ? Map.copyOf(extra) : Map.of();
    }

    /**
     * @return an empty request, for actions that need no parameters
     */
    public static PlanActionRequest empty() {
        return new PlanActionRequest(null, null, null, null, null, Map.of());
    }

    /**
     * @param point the pause point
     * @return a request that pauses at the given point
     */
    public static PlanActionRequest pauseAt(String point) {
        return new PlanActionRequest(point, null, null, null, null, Map.of());
    }

    /**
     * @param reason  the reason
     * @param closePr whether the pull request is closed
     * @return a request that ends the plan with the given reason
     */
    public static PlanActionRequest ending(String reason, boolean closePr) {
        return new PlanActionRequest(null, reason, closePr, null, null, Map.of());
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (point != null) {
            map.put("point", point);
        }
        if (reason != null) {
            map.put("reason", reason);
        }
        if (closePr != null) {
            map.put("close_pr", closePr);
        }
        if (raiseScope != null) {
            map.put("raise_scope", raiseScope);
        }
        if (withPlans != null) {
            map.put("with_plans", withPlans);
        }
        map.putAll(extra);
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
    public static PlanActionRequest fromMap(Map<String, Object> map) throws IOException {
        var source = map != null ? map : Map.<String, Object>of();
        var known = new LinkedHashMap<String, Object>();
        for (var entry : source.entrySet()) {
            switch (entry.getKey()) {
                case "point", "reason", "close_pr", "raise_scope", "with_plans" -> {
                    // handled below
                }
                default -> known.put(entry.getKey(), entry.getValue());
            }
        }
        return new PlanActionRequest(
                JsonTree.string(source, "point"),
                JsonTree.string(source, "reason"),
                JsonTree.bool(source, "close_pr"),
                JsonTree.bool(source, "raise_scope"),
                JsonTree.bool(source, "with_plans"),
                known
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed request
     * @throws IOException on format failure
     */
    public static PlanActionRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PlanActionRequest");
        }
        return fromMap(obj);
    }
}
