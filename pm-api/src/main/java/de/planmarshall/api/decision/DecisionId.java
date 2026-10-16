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

import java.util.Objects;

/**
 * Coordinate identifier of a decision addressed as {@code <scope_id>/<question_id>} (UC-PLN-2).
 *
 * @param scopeId    scope identifier (plan ID, epic ID, or project ID)
 * @param questionId question identifier (e.g. {@code "Q-007"} or {@code "WR-2"})
 */
public record DecisionId(String scopeId, String questionId) {

    public DecisionId {
        Objects.requireNonNull(scopeId, "scopeId");
        Objects.requireNonNull(questionId, "questionId");
    }

    /**
     * Formats as canonical coordinate {@code <scope_id>/<question_id>} if scopeId is present,
     * otherwise returns questionId alone.
     *
     * @return coordinate string
     */
    public String asCoordinate() {
        if (scopeId.isEmpty()) {
            return questionId;
        }
        return scopeId + "/" + questionId;
    }

    /**
     * Parses a coordinate string.
     *
     * @param coordinate e.g. {@code "fix-relay/Q-007"} or {@code "Q-007"}
     * @return parsed DecisionId
     */
    public static DecisionId parse(String coordinate) {
        Objects.requireNonNull(coordinate, "coordinate");
        var trimmed = coordinate.trim();
        int slash = trimmed.indexOf('/');
        if (slash >= 0) {
            var scope = trimmed.substring(0, slash).trim();
            var qid = trimmed.substring(slash + 1).trim();
            return new DecisionId(scope, qid);
        }
        return new DecisionId("", trimmed);
    }
}
