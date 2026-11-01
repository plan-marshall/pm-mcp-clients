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
 * An available option within a decision part (UC-PLN-2).
 *
 * @param key                 option selector (e.g. {@code "1"}, {@code "rebase"}, {@code "yes"})
 * @param label               display label
 * @param tradeoffDescription trade-off or rationale description
 * @param consequences        downstream consequences if chosen
 * @param previewDiff         optional unified diff or text preview
 * @param quorumSupport       quorum support metrics and votes if voted upon
 */
public record DecisionOption(
        String key,
        String label,
        @Nullable String tradeoffDescription,
        @Nullable String consequences,
        @Nullable String previewDiff,
        @Nullable QuorumSupport quorumSupport
) {
    public DecisionOption {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("key", key);
        map.put("label", label);
        if (tradeoffDescription != null) {
            map.put("tradeoff_description", tradeoffDescription);
        }
        if (consequences != null) {
            map.put("consequences", consequences);
        }
        if (previewDiff != null) {
            map.put("preview_diff", previewDiff);
        }
        if (quorumSupport != null) {
            map.put("quorum_support", quorumSupport.toMap());
        }
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed DecisionOption
     */
    public static DecisionOption fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var key = JsonTree.string(map, "key");
        if (key == null) key = "";
        var label = JsonTree.string(map, "label");
        if (label == null) label = key;
        var tradeoff = JsonTree.string(map, "tradeoff_description");
        if (tradeoff == null) tradeoff = JsonTree.string(map, "tradeoff");
        var consequences = JsonTree.string(map, "consequences");
        var previewDiff = JsonTree.string(map, "preview_diff");
        if (previewDiff == null) previewDiff = JsonTree.string(map, "diff");

        QuorumSupport quorumSupport = null;
        var qsObj = map.get("quorum_support");
        if (qsObj == null) qsObj = map.get("support");
        var qsMap = JsonTree.asObject(qsObj);
        if (qsMap != null) {
            quorumSupport = QuorumSupport.fromMap(qsMap);
        }

        return new DecisionOption(key, label, tradeoff, consequences, previewDiff, quorumSupport);
    }
}
