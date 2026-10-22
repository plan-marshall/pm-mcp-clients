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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Decomposition validation")
class DecompositionValidatorTest {

    private static StagedPlanRecord plan(String id, List<String> surface, List<String> dependencies) {
        return new StagedPlanRecord(id, id, null, surface, dependencies, StagedPlanRecord.STATUS_STAGED);
    }

    private static WorkstreamRecord workstream(String id, int order, StagedPlanRecord... plans) {
        return new WorkstreamRecord(id, id, null, order, List.of(plans));
    }

    @Test
    @DisplayName("passes a cut whose concurrent plans are disjoint and acyclic")
    void passesDisjointAcyclicCut() {
        var workstreams = List.of(
                workstream("WS-1", 1, plan("relay-cursor-order", List.of("pm-relay"), List.of())),
                workstream("WS-2", 2,
                        plan("relay-auth-v2", List.of("pm-api"), List.of("relay-cursor-order"))));

        assertTrue(DecompositionValidator.validate(workstreams).isEmpty());
        assertTrue(DecompositionValidator.passes(workstreams));
    }

    @Test
    @DisplayName("reports a surface overlap of two concurrent plans")
    void reportsSurfaceOverlap() {
        var workstreams = List.of(
                workstream("WS-1", 1, plan("relay-cursor-order", List.of("pm-relay", "pm-api"), List.of())),
                workstream("WS-2", 2, plan("relay-auth-v2", List.of("pm-api"), List.of())));

        var findings = DecompositionValidator.validate(workstreams);

        assertEquals(1, findings.size());
        var finding = findings.getFirst();
        assertEquals(CheckSeverity.ERROR, finding.severity());
        assertEquals(DecompositionCheckFinding.RULE_SURFACE_OVERLAP, finding.rule());
        assertEquals(List.of("relay-cursor-order", "relay-auth-v2"), finding.affectedPlans());
        assertTrue(finding.message().contains("pm-api"));
        assertFalse(DecompositionValidator.passes(workstreams));
    }

    @Test
    @DisplayName("ignores a dropped or locked plan in the overlap and cycle checks")
    void ignoresDroppedAndLockedPlans() {
        var workstreams = List.of(
                workstream("WS-1", 1,
                        new StagedPlanRecord("relay-reconnect-docs", "relay-reconnect-docs", null,
                                List.of("pm-api"), List.of("relay-auth-v2"), StagedPlanRecord.STATUS_DROPPED),
                        new StagedPlanRecord("relay-token-store", "relay-token-store", null,
                                List.of("pm-api"), List.of(), StagedPlanRecord.STATUS_LOCKED)),
                workstream("WS-2", 2, plan("relay-auth-v2", List.of("pm-api"), List.of())));

        assertTrue(DecompositionValidator.validate(workstreams).isEmpty());
    }

    @Test
    @DisplayName("reports a dependency cycle and names its members")
    void reportsDependencyCycle() {
        var workstreams = List.of(workstream("WS-1", 1,
                plan("relay-a", List.of("pm-api"), List.of("relay-b")),
                plan("relay-b", List.of("pm-relay"), List.of("relay-a"))));

        var findings = DecompositionValidator.validate(workstreams);

        assertEquals(1, findings.size());
        var finding = findings.getFirst();
        assertEquals(CheckSeverity.ERROR, finding.severity());
        assertEquals(DecompositionCheckFinding.RULE_DEPENDENCY_CYCLE, finding.rule());
        assertTrue(finding.affectedPlans().containsAll(List.of("relay-a", "relay-b")));
        assertFalse(DecompositionValidator.passes(workstreams));
    }

    @Test
    @DisplayName("reports a duplicate plan id and a duplicate workstream order")
    void reportsIntegrityFindings() {
        var workstreams = List.of(
                workstream("WS-1", 1, plan("relay-cursor-order", List.of("pm-relay"), List.of())),
                workstream("WS-2", 1, plan("relay-cursor-order", List.of("pm-api"), List.of())));

        var findings = DecompositionValidator.validate(workstreams);

        assertTrue(findings.stream().anyMatch(finding -> finding.severity() == CheckSeverity.ERROR
                && DecompositionCheckFinding.RULE_WORKSTREAM_INTEGRITY.equals(finding.rule())));
        assertTrue(findings.stream().anyMatch(finding -> finding.severity() == CheckSeverity.WARNING
                && DecompositionCheckFinding.RULE_WORKSTREAM_INTEGRITY.equals(finding.rule())));
    }

    @Test
    @DisplayName("tolerates a null cut as an empty cut")
    void toleratesNullCut() {
        assertTrue(DecompositionValidator.validate(null).isEmpty());
        assertTrue(DecompositionValidator.passes(null));
    }
}
