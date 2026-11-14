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
 * The answer of a plan whose captured commit is no longer in the repository, with the reason.
 *
 * @param planId the plan whose capture is gone
 * @param reason why the capture cannot be read
 */
public record PlanChangesUnavailable(String planId, String reason) implements PlanChangesAnswer {

    /** The member that carries the reason. */
    public static final String FIELD_REASON = "reason";

    public PlanChangesUnavailable {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(reason, "reason");
    }

    @Override
    public String state() {
        return UNAVAILABLE;
    }

    @Override
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_STATE, state());
        map.put(FIELD_REASON, reason);
        return map;
    }
}
