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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.ChangeSize;

/**
 * The answer of a plan whose change was captured: the list of the capture, with the plan it belongs
 * to.
 *
 * @param planId the plan the capture belongs to
 * @param head   the commit the capture measured
 * @param baseRef the base the capture measured against
 * @param files  the changed files, in path order
 */
public record PlanChangesCaptured(
        String planId,
        String head,
        String baseRef,
        List<PlanChangedFile> files
) implements PlanChangesAnswer {

    public PlanChangesCaptured {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(head, "head");
        Objects.requireNonNull(baseRef, "baseRef");
        files = files != null ? List.copyOf(files) : List.of();
    }

    @Override
    public String state() {
        return CAPTURED;
    }

    /**
     * @return the size of the change, derived from the files
     */
    public ChangeSize totals() {
        return PlanChanges.totalsOf(files);
    }

    @Override
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_STATE, state());
        map.putAll(new PlanChanges(head, baseRef, files).toMap());
        return map;
    }

    /**
     * @param map the map representation
     * @return the parsed answer
     * @throws IOException if a required member is missing or a file does not parse
     */
    public static PlanChangesCaptured fromMap(Map<String, Object> map) throws IOException {
        var planId = JsonTree.string(map, FIELD_PLAN_ID);
        if (planId == null) {
            throw new JsonFormatException("Missing " + FIELD_PLAN_ID + " in a plan's changes");
        }
        var changes = PlanChanges.fromMap(map);
        return new PlanChangesCaptured(planId, changes.head(), changes.baseRef(), changes.files());
    }
}
