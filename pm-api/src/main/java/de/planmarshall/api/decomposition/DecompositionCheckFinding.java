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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One finding of a decomposition check ({@code decomposition.check}, UC-STA-6).
 *
 * @param severity      how severe the finding is
 * @param rule          the rule that raised it ({@code surface_overlap}, {@code dependency_cycle},
 *                      {@code workstream_integrity})
 * @param affectedPlans the staged plans the finding concerns
 * @param message       the explanation shown beside the plans
 */
public record DecompositionCheckFinding(
        CheckSeverity severity,
        String rule,
        List<String> affectedPlans,
        String message
) {

    /** Two concurrently startable plans declare overlapping surface. */
    public static final String RULE_SURFACE_OVERLAP = "surface_overlap";

    /** The staged plans' dependencies form a cycle. */
    public static final String RULE_DEPENDENCY_CYCLE = "dependency_cycle";

    /** A workstream or a staged plan is malformed. */
    public static final String RULE_WORKSTREAM_INTEGRITY = "workstream_integrity";

    private static final String FIELD_SEVERITY = "severity";
    private static final String FIELD_RULE = "rule";
    private static final String FIELD_AFFECTED_PLANS = "affected_plans";
    private static final String FIELD_MESSAGE = "message";

    public DecompositionCheckFinding {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(rule, "rule");
        affectedPlans = affectedPlans == null ? List.of() : List.copyOf(affectedPlans);
        message = message == null ? "" : message;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_SEVERITY, severity.wireValue());
        map.put(FIELD_RULE, rule);
        map.put(FIELD_AFFECTED_PLANS, affectedPlans);
        map.put(FIELD_MESSAGE, message);
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
     * @return the parsed finding
     * @throws IOException if a required field is missing or unknown
     */
    public static DecompositionCheckFinding fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var severity = JsonTree.string(map, FIELD_SEVERITY);
        var rule = JsonTree.string(map, FIELD_RULE);
        if (severity == null || rule == null) {
            throw new JsonFormatException("Missing required fields for DecompositionCheckFinding: severity or rule");
        }
        return new DecompositionCheckFinding(CheckSeverity.fromWire(severity), rule,
                StagedPlanRecord.readStrings(map.get(FIELD_AFFECTED_PLANS)),
                JsonTree.string(map, FIELD_MESSAGE));
    }

    /**
     * @param json the JSON object string
     * @return the parsed finding
     * @throws IOException if the value is not an object or a field is missing
     */
    public static DecompositionCheckFinding fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DecompositionCheckFinding");
        }
        return fromMap(obj);
    }
}
