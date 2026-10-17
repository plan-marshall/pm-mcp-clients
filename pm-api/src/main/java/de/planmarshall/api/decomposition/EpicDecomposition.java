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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * An epic's decomposition as the operator and the clients see it (UC-STA-6): its state, its
 * revision, the workstreams with their staged plans, and the findings of the last check.
 *
 * @param epicId      the epic this cut belongs to
 * @param status      the state the decomposition is in
 * @param revision    the ledger revision the cut is based on
 * @param workstreams the workstreams, in order
 * @param findings    the findings of the last check
 */
public record EpicDecomposition(
        String epicId,
        DecompositionState status,
        long revision,
        List<WorkstreamRecord> workstreams,
        List<DecompositionCheckFinding> findings
) {

    private static final String FIELD_EPIC_ID = "epic_id";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_REVISION = "revision";
    private static final String FIELD_WORKSTREAMS = "workstreams";
    private static final String FIELD_FINDINGS = "findings";

    public EpicDecomposition {
        Objects.requireNonNull(epicId, "epicId");
        Objects.requireNonNull(status, "status");
        workstreams = workstreams == null ? List.of() : List.copyOf(workstreams);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    /**
     * @return {@code true} if every check passes and the cut can be agreed
     */
    public boolean everyCheckPasses() {
        return findings.stream().noneMatch(finding -> finding.severity() == CheckSeverity.ERROR);
    }

    /**
     * @return the count of staged plans across all workstreams
     */
    public int stagedPlanCount() {
        return workstreams.stream().mapToInt(workstream -> workstream.stagedPlans().size()).sum();
    }

    /**
     * @param planId the plan id
     * @return the staged plan with that id, or {@code null}
     */
    public StagedPlanRecord plan(String planId) {
        for (var workstream : workstreams) {
            for (var plan : workstream.stagedPlans()) {
                if (plan.planId().equals(planId)) {
                    return plan;
                }
            }
        }
        return null;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_EPIC_ID, epicId);
        map.put(FIELD_STATUS, status.wireValue());
        map.put(FIELD_REVISION, revision);
        map.put(FIELD_WORKSTREAMS, workstreams.stream().map(WorkstreamRecord::toMap).toList());
        map.put(FIELD_FINDINGS, findings.stream().map(DecompositionCheckFinding::toMap).toList());
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
     * @return the parsed decomposition
     * @throws IOException if a required field is missing or unknown
     */
    public static EpicDecomposition fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var epicId = JsonTree.string(map, FIELD_EPIC_ID);
        var status = JsonTree.string(map, FIELD_STATUS);
        if (epicId == null || status == null) {
            throw new JsonFormatException("Missing required fields for EpicDecomposition: epic_id or status");
        }
        var revision = JsonTree.number(map, FIELD_REVISION);
        return new EpicDecomposition(epicId, DecompositionState.fromWire(status),
                revision != null ? revision.longValue() : 0L,
                readWorkstreams(map.get(FIELD_WORKSTREAMS)), readFindings(map.get(FIELD_FINDINGS)));
    }

    /**
     * @param json the JSON object string
     * @return the parsed decomposition
     * @throws IOException if the value is not an object or a field is missing
     */
    public static EpicDecomposition fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EpicDecomposition");
        }
        return fromMap(obj);
    }

    private static List<WorkstreamRecord> readWorkstreams(Object raw) throws IOException {
        var array = JsonTree.asArray(raw);
        if (array == null) {
            return List.of();
        }
        var workstreams = new ArrayList<WorkstreamRecord>();
        for (var item : array) {
            var obj = JsonTree.asObject(item);
            if (obj != null) {
                workstreams.add(WorkstreamRecord.fromMap(obj));
            }
        }
        return workstreams;
    }

    private static List<DecompositionCheckFinding> readFindings(Object raw) throws IOException {
        var array = JsonTree.asArray(raw);
        if (array == null) {
            return List.of();
        }
        var findings = new ArrayList<DecompositionCheckFinding>();
        for (var item : array) {
            var obj = JsonTree.asObject(item);
            if (obj != null) {
                findings.add(DecompositionCheckFinding.fromMap(obj));
            }
        }
        return findings;
    }
}
