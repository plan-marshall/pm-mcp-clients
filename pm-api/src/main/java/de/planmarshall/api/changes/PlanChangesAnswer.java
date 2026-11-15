/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.changes;

import java.io.IOException;
import java.util.Map;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * What {@code GET /api/v1/plans/{plan_id}/changes} answers: the list of a capture, or one of the two
 * refusals ({@code 01-processes-and-transport.adoc#plan-changes}).
 * <p>
 * The refusals are answers, never an empty list: before the first capture the answer is
 * {@code not_captured}, and a plan whose captured {@code head} is no longer in the repository answers
 * {@code unavailable} with its reason. The state is the answer's own {@code state} member, so a
 * client branches on it instead of on the presence of a list.
 */
public sealed interface PlanChangesAnswer
        permits PlanChangesCaptured, PlanChangesNotCaptured, PlanChangesUnavailable {

    /** The member that names which answer this is. */
    String FIELD_STATE = "state";

    /** The member that names the plan the answer is about. */
    String FIELD_PLAN_ID = "plan_id";

    /** The state of a capture that exists. */
    String CAPTURED = "captured";

    /** The state before a plan's first capture. */
    String NOT_CAPTURED = "not_captured";

    /** The state of a capture whose commit is no longer in the repository. */
    String UNAVAILABLE = "unavailable";

    /**
     * @return the plan the answer is about
     */
    String planId();

    /**
     * @return the state of this answer
     */
    String state();

    /**
     * @return the map representation, with the plan id and the state first
     */
    Map<String, Object> toMap();

    /**
     * @return the JSON representation
     */
    default String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Reads the answer of a plan's changes, branching on its {@code state} member.
     *
     * @param map the map representation
     * @return the parsed answer
     * @throws IOException              if a member the state requires is missing
     * @throws IllegalArgumentException if the state is none of the three closed values
     */
    static PlanChangesAnswer fromMap(Map<String, Object> map) throws IOException {
        if (map == null) {
            throw new JsonFormatException("A plan's changes are a JSON object");
        }
        var state = JsonTree.string(map, FIELD_STATE);
        if (!CAPTURED.equals(state) && !NOT_CAPTURED.equals(state) && !UNAVAILABLE.equals(state)) {
            throw new JsonFormatException("Unknown state of a plan's changes: " + state);
        }
        var planId = JsonTree.string(map, FIELD_PLAN_ID);
        if (planId == null) {
            throw new JsonFormatException("Missing " + FIELD_PLAN_ID + " in a plan's changes");
        }
        return switch (state) {
            case CAPTURED -> PlanChangesCaptured.fromMap(map);
            case NOT_CAPTURED -> new PlanChangesNotCaptured(planId);
            default -> {
                var reason = JsonTree.string(map, PlanChangesUnavailable.FIELD_REASON);
                if (reason == null) {
                    throw new JsonFormatException("An unavailable plan's changes name their reason");
                }
                yield new PlanChangesUnavailable(planId, reason);
            }
        };
    }

    /**
     * @param json the JSON object
     * @return the parsed answer
     * @throws IOException on parse failure or a missing member
     */
    static PlanChangesAnswer fromJson(String json) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(json));
        if (map == null) {
            throw new JsonFormatException("Expected a JSON object for a plan's changes");
        }
        return fromMap(map);
    }
}
