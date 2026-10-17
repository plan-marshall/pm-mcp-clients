/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decomposition;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One staged plan of an epic's decomposition (UC-STA-6).
 * <p>
 * Its objective, its expected surface, and its dependencies are what the checks read; its
 * {@code status} carries the plan's change marker on a later run ({@code unchanged},
 * {@code changed}, {@code new}, {@code dropped}) or the marker of a first run
 * ({@code staged}), and {@code locked} for a plan the decomposition may not touch.
 *
 * @param planId          the plan id
 * @param title           the plan's title
 * @param objective       the plan's objective, or {@code null}
 * @param expectedSurface the files or directories the plan is expected to change
 * @param dependencies    the ids of staged plans that must land before this one starts
 * @param status          the plan's change or lifecycle marker
 */
public record StagedPlanRecord(
        String planId,
        String title,
        @Nullable String objective,
        List<String> expectedSurface,
        List<String> dependencies,
        String status
) {

    /** A plan of a first decomposition, not part of a later run's marking. */
    public static final String STATUS_STAGED = "staged";

    /** A plan that already exists and did not change on a later run. */
    public static final String STATUS_UNCHANGED = "unchanged";

    /** A plan whose workstream, surface, or dependencies changed on a later run. */
    public static final String STATUS_CHANGED = "changed";

    /** A plan added on a later run. */
    public static final String STATUS_NEW = "new";

    /** A plan dropped on a later run. */
    public static final String STATUS_DROPPED = "dropped";

    /** A shipped, running, pausing, or paused plan; the decomposition may not touch it. */
    public static final String STATUS_LOCKED = "locked";

    /** A plan that has been committed to the epic ledger. */
    public static final String STATUS_COMMITTED = "committed";

    private static final String FIELD_PLAN_ID = "plan_id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_OBJECTIVE = "objective";
    private static final String FIELD_EXPECTED_SURFACE = "expected_surface";
    private static final String FIELD_DEPENDENCIES = "dependencies";
    private static final String FIELD_STATUS = "status";

    public StagedPlanRecord {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(title, "title");
        expectedSurface = expectedSurface == null ? List.of() : List.copyOf(expectedSurface);
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        status = status == null || status.isBlank() ? STATUS_STAGED : status;
    }

    /**
     * @return {@code true} if the plan carries no work any more and the checks ignore it
     */
    public boolean isDropped() {
        return STATUS_DROPPED.equals(status);
    }

    /**
     * @return {@code true} if the decomposition may not change the plan
     */
    public boolean isLocked() {
        return STATUS_LOCKED.equals(status);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_TITLE, title);
        if (objective != null) {
            map.put(FIELD_OBJECTIVE, objective);
        }
        map.put(FIELD_EXPECTED_SURFACE, expectedSurface);
        map.put(FIELD_DEPENDENCIES, dependencies);
        map.put(FIELD_STATUS, status);
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
     * @return the parsed plan
     * @throws IOException if a required field is missing
     */
    public static StagedPlanRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, FIELD_PLAN_ID);
        var title = JsonTree.string(map, FIELD_TITLE);
        if (planId == null || title == null) {
            throw new JsonFormatException("Missing required fields for StagedPlanRecord: plan_id or title");
        }
        return new StagedPlanRecord(planId, title, JsonTree.string(map, FIELD_OBJECTIVE),
                readStrings(map.get(FIELD_EXPECTED_SURFACE)), readStrings(map.get(FIELD_DEPENDENCIES)),
                JsonTree.string(map, FIELD_STATUS));
    }

    /**
     * @param json the JSON object string
     * @return the parsed plan
     * @throws IOException if the value is not an object or a field is missing
     */
    public static StagedPlanRecord fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for StagedPlanRecord");
        }
        return fromMap(obj);
    }

    static List<String> readStrings(Object raw) {
        var array = JsonTree.asArray(raw);
        if (array == null) {
            return List.of();
        }
        return array.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }
}
