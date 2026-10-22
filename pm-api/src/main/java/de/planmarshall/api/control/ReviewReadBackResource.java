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
 * The read-back of an edit or a redraft in a review ({@code review.read-back},
 * {@code review.read-back-plan}, design <em>Reviewing Findings or Watches</em>).
 *
 * <p>It shows the change old against new, with <em>Accept</em>, <em>Instruct again</em>, and
 * <em>Discard</em>; nothing applies without <em>Accept</em>.
 *
 * @param kind   what was read back: {@code statement}, {@code rules}, or {@code plan}
 * @param before the text or draft before the change, or {@code null}
 * @param after  the text or draft after the change, or {@code null}
 * @param plan   the redrafted plan where the read-back is a plan, or {@code null}
 */
public record ReviewReadBackResource(
        String kind,
        @Nullable String before,
        @Nullable String after,
        @Nullable DraftedPlan plan
) {

    /** The statement of an item was rewritten. */
    public static final String KIND_STATEMENT = "statement";

    /** The rules of an item were rewritten. */
    public static final String KIND_RULES = "rules";

    /** A drafted plan was redrafted. */
    public static final String KIND_PLAN = "plan";

    public ReviewReadBackResource {
        Objects.requireNonNull(kind, "kind");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("kind", kind);
        if (before != null) {
            map.put("before", before);
        }
        if (after != null) {
            map.put("after", after);
        }
        if (plan != null) {
            map.put("plan", plan.toMap());
        }
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the input map
     * @return the parsed read-back
     * @throws IOException if a required field is missing
     */
    public static ReviewReadBackResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kind = JsonTree.string(map, "kind");
        if (kind == null) {
            throw new JsonFormatException("Missing kind in ReviewReadBackResource");
        }
        var plan = JsonTree.asObject(map.get("plan"));
        return new ReviewReadBackResource(kind, JsonTree.string(map, "before"),
                JsonTree.string(map, "after"), plan != null ? DraftedPlan.fromMap(plan) : null);
    }

    /**
     * @param json the JSON object string
     * @return the parsed read-back
     * @throws IOException if the value is not an object
     */
    public static ReviewReadBackResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ReviewReadBackResource");
        }
        return fromMap(obj);
    }
}
