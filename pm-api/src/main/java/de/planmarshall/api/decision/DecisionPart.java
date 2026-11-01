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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;

/**
 * A constituent question or part of a decision (UC-PLN-2).
 *
 * @param partId                  part identifier (e.g. {@code "1"} or {@code "store-location"})
 * @param question                the specific question asked
 * @param answerType              cardinality/type of answers accepted
 * @param options                 offered options
 * @param recommendation          key of the recommended option if any
 * @param recommendationRationale why the recommendation was made
 * @param recommendingRole        role that recommended the option (e.g. {@code "refine"}, {@code "implement"})
 * @param dependency              dependency condition if this part depends on another part's answer
 * @param allowOwnWords           whether free-text answers in own words are permitted
 * @param allowNote               whether accompanying notes are permitted
 */
public record DecisionPart(
        String partId,
        String question,
        AnswerType answerType,
        List<DecisionOption> options,
        @Nullable String recommendation,
        @Nullable String recommendationRationale,
        @Nullable String recommendingRole,
        @Nullable String dependency,
        boolean allowOwnWords,
        boolean allowNote
) {
    public DecisionPart {
        Objects.requireNonNull(partId, "partId");
        Objects.requireNonNull(question, "question");
        Objects.requireNonNull(answerType, "answerType");
        options = options != null ? List.copyOf(options) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("part_id", partId);
        map.put("question", question);
        map.put("answer_type", answerType.value());
        var optsList = new ArrayList<Map<String, Object>>();
        for (var opt : options) {
            optsList.add(opt.toMap());
        }
        map.put("options", optsList);
        if (recommendation != null) {
            map.put("recommendation", recommendation);
        }
        if (recommendationRationale != null) {
            map.put("recommendation_rationale", recommendationRationale);
        }
        if (recommendingRole != null) {
            map.put("recommending_role", recommendingRole);
        }
        if (dependency != null) {
            map.put("dependency", dependency);
        }
        map.put("allow_own_words", allowOwnWords);
        map.put("allow_note", allowNote);
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed DecisionPart
     */
    public static DecisionPart fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var partId = JsonTree.string(map, "part_id");
        if (partId == null) {
            partId = JsonTree.string(map, "id");
            if (partId == null) partId = "1";
        }
        var question = JsonTree.string(map, "question");
        if (question == null) question = "";
        var atStr = JsonTree.string(map, "answer_type");
        var answerType = atStr != null ? AnswerType.fromValue(atStr) : AnswerType.ONE_OPTION;

        var optionsList = new ArrayList<DecisionOption>();
        if (map.get("options") instanceof List<?> list) {
            for (var item : list) {
                var om = JsonTree.asObject(item);
                if (om != null) {
                    optionsList.add(DecisionOption.fromMap(om));
                }
            }
        }

        var recommendation = JsonTree.string(map, "recommendation");
        var recRationale = JsonTree.string(map, "recommendation_rationale");
        if (recRationale == null) recRationale = JsonTree.string(map, "rationale");
        var recRole = JsonTree.string(map, "recommending_role");
        if (recRole == null) recRole = JsonTree.string(map, "role");
        var dependency = JsonTree.string(map, "dependency");
        var allowOwnWords = Boolean.TRUE.equals(JsonTree.bool(map, "allow_own_words"));
        var allowNote = Boolean.TRUE.equals(JsonTree.bool(map, "allow_note"));

        return new DecisionPart(partId, question, answerType, optionsList, recommendation, recRationale, recRole, dependency, allowOwnWords, allowNote);
    }
}
