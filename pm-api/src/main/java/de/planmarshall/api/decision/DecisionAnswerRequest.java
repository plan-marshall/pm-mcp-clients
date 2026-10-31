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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Payload sent by an operator client to answer, waive, decline, or withdraw a decision (UC-PLN-2).
 *
 * @param partAnswers map of part ID to chosen option key or entered value
 * @param option      single choice option if answering single-part decision directly
 * @param ownWords    free-text answer in operator's own words
 * @param note        optional accompanying note
 * @param action      explicit action ({@code "answer"}, {@code "waive"}, {@code "decline"}, {@code "withdraw"})
 * @param reason      mandatory operator reason when action is {@code "waive"} or {@code "decline"}
 * @param channel     operator channel ({@code "cli"} or {@code "web"})
 * @param device      optional device identifier or label
 */
public record DecisionAnswerRequest(
        Map<String, Object> partAnswers,
        @Nullable String option,
        @Nullable String ownWords,
        @Nullable String note,
        @Nullable String action,
        @Nullable String reason,
        String channel,
        @Nullable String device
) {
    public DecisionAnswerRequest {
        Objects.requireNonNull(channel, "channel");
        partAnswers = partAnswers != null ? Map.copyOf(partAnswers) : Map.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (!partAnswers.isEmpty()) {
            map.put("part_answers", partAnswers);
        }
        if (option != null) {
            map.put("option", option);
        }
        if (ownWords != null) {
            map.put("words", ownWords);
        }
        if (note != null) {
            map.put("note", note);
        }
        if (action != null) {
            map.put("action", action);
        }
        if (reason != null) {
            map.put("reason", reason);
        }
        map.put("channel", channel);
        if (device != null) {
            map.put("device", device);
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
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed DecisionAnswerRequest
     * @throws IOException on error
     */
    public static DecisionAnswerRequest fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        var map = JsonTree.asObject(parsed);
        if (map == null) throw new JsonFormatException("Expected JSON object for DecisionAnswerRequest");
        return fromMap(map);
    }

    /**
     * Parses from a JSON map.
     *
     * @param map tree map
     * @return parsed request
     */
    public static DecisionAnswerRequest fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var channel = JsonTree.string(map, "channel");
        if (channel == null) channel = "cli";
        var device = JsonTree.string(map, "device");
        var option = JsonTree.string(map, "option");
        var ownWords = JsonTree.string(map, "words");
        if (ownWords == null) ownWords = JsonTree.string(map, "own_words");
        var note = JsonTree.string(map, "note");
        var action = JsonTree.string(map, "action");
        var reason = JsonTree.string(map, "reason");

        var answers = new LinkedHashMap<String, Object>();
        var paObj = map.get("part_answers");
        if (paObj instanceof Map<?, ?> pm) {
            for (var entry : pm.entrySet()) {
                answers.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        } else if (option != null) {
            answers.put("1", option);
        }

        return new DecisionAnswerRequest(answers, option, ownWords, note, action, reason, channel, device);
    }
}
