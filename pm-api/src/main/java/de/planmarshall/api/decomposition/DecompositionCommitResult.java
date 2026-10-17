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
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The result of agreeing a decomposition ({@code decomposition.commit}, UC-STA-6): the cut is
 * written and committed to the epic ledger.
 *
 * @param status              the outcome status ({@code committed})
 * @param ledgerRevision      the ledger revision the cut was committed as
 * @param launchablePlanCount the staged plans that can start now
 */
public record DecompositionCommitResult(
        String status,
        long ledgerRevision,
        int launchablePlanCount
) {

    /** The status of a committed decomposition. */
    public static final String STATUS_COMMITTED = "committed";

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_LEDGER_REVISION = "ledger_revision";
    private static final String FIELD_LAUNCHABLE_PLAN_COUNT = "launchable_plan_count";

    public DecompositionCommitResult {
        status = status == null || status.isBlank() ? STATUS_COMMITTED : status;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STATUS, status);
        map.put(FIELD_LEDGER_REVISION, ledgerRevision);
        map.put(FIELD_LAUNCHABLE_PLAN_COUNT, launchablePlanCount);
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
     * @return the parsed result
     * @throws IOException if the status is missing
     */
    public static DecompositionCommitResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var status = JsonTree.string(map, FIELD_STATUS);
        if (status == null) {
            throw new JsonFormatException("Missing required field for DecompositionCommitResult: status");
        }
        var revision = JsonTree.number(map, FIELD_LEDGER_REVISION);
        var launchable = JsonTree.integer(map, FIELD_LAUNCHABLE_PLAN_COUNT);
        return new DecompositionCommitResult(status, revision != null ? revision.longValue() : 0L,
                launchable != null ? launchable : 0);
    }

    /**
     * @param json the JSON object string
     * @return the parsed result
     * @throws IOException if the value is not an object or a field is missing
     */
    public static DecompositionCommitResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DecompositionCommitResult");
        }
        return fromMap(obj);
    }
}
