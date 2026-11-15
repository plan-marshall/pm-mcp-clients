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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The answer of a plan before its first capture: {@code not_captured}, never an empty list.
 *
 * @param planId the plan that was captured never
 */
public record PlanChangesNotCaptured(String planId) implements PlanChangesAnswer {

    public PlanChangesNotCaptured {
        Objects.requireNonNull(planId, "planId");
    }

    @Override
    public String state() {
        return NOT_CAPTURED;
    }

    @Override
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_STATE, state());
        return map;
    }
}
