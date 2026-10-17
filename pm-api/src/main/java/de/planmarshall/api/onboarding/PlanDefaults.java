/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The machine's plan defaults: how a plan runs when its project and the plan itself say nothing
 * else (UC-ONB-9; Plan Behaviour Axes).
 *
 * @param landing how a plan's work lands ({@code landing.pull-request}, {@code landing.main},
 *                {@code landing.files})
 * @param method  when the tests are written ({@code method.test-first}, {@code method.tests-after})
 * @param asking  how often the operator is asked ({@code ask.every-phase}, {@code ask.key-points},
 *                {@code ask.questions-only})
 * @param posture the optional posture of a plan ({@code minimal}, {@code standard}, {@code full}),
 *                or {@code null} while it is unset, so that a plan asks it at its start
 */
public record PlanDefaults(
        String landing,
        String method,
        String asking,
        @Nullable String posture
) {
    /** The shipped landing variant: branch and pull request. */
    public static final String LANDING_PULL_REQUEST = "landing.pull-request";

    /** The landing variant that commits directly on the base branch. */
    public static final String LANDING_MAIN = "landing.main";

    /** The landing variant that commits nothing. */
    public static final String LANDING_FILES = "landing.files";

    /** The method variant that writes tests before the code. */
    public static final String METHOD_TEST_FIRST = "method.test-first";

    /** The method variant that writes tests after the code. */
    public static final String METHOD_TESTS_AFTER = "method.tests-after";

    /** The asking variant that confirms every phase. */
    public static final String ASK_EVERY_PHASE = "ask.every-phase";

    /** The asking variant that asks at the key points. */
    public static final String ASK_KEY_POINTS = "ask.key-points";

    /** The asking variant that asks only questions. */
    public static final String ASK_QUESTIONS_ONLY = "ask.questions-only";

    public PlanDefaults {
        Objects.requireNonNull(landing, "landing");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(asking, "asking");
    }

    /**
     * The shipped defaults: a pull request, tests first, and the key points.
     *
     * @return the shipped plan defaults with no posture set
     */
    public static PlanDefaults shipped() {
        return new PlanDefaults(LANDING_PULL_REQUEST, METHOD_TEST_FIRST, ASK_KEY_POINTS, null);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("landing", landing);
        map.put("method", method);
        map.put("asking", asking);
        if (posture != null) {
            map.put("posture", posture);
        }
        return map;
    }

    /**
     * Serializes to a JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map map representation
     * @return parsed record
     * @throws IOException on format failure
     */
    public static PlanDefaults fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var landing = orDefault(JsonTree.string(map, "landing"), LANDING_PULL_REQUEST);
        var method = orDefault(JsonTree.string(map, "method"), METHOD_TEST_FIRST);
        var asking = orDefault(JsonTree.string(map, "asking"), ASK_KEY_POINTS);
        return new PlanDefaults(landing, method, asking, JsonTree.string(map, "posture"));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed record
     * @throws IOException on format failure
     */
    public static PlanDefaults fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PlanDefaults");
        }
        return fromMap(obj);
    }

    private static String orDefault(@Nullable String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
