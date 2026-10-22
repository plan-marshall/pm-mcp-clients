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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

@DisplayName("Decomposition record contracts")
class DecompositionRecordsTest {

    @Nested
    @DisplayName("Closed lists")
    class ClosedLists {

        @Test
        @DisplayName("DecompositionState resolves wire values and full state ids")
        void decompositionState() {
            var wireValues = Stream.of(DecompositionState.values())
                    .map(DecompositionState::wireValue)
                    .collect(Collectors.toCollection(TreeSet::new));
            assertEquals(Set.of("check", "commit", "draft", "held", "read_back", "review"), wireValues);

            assertEquals(DecompositionState.DRAFT, DecompositionState.fromWire("draft"));
            assertEquals(DecompositionState.READ_BACK, DecompositionState.fromWire("READ_BACK"));
            assertEquals(DecompositionState.REVIEW, DecompositionState.fromWire("decomposition.review"));
            assertEquals(DecompositionState.READ_BACK, DecompositionState.fromWire("decomposition.read-back"));
            assertEquals("decomposition.review", DecompositionState.REVIEW.stateId());
            assertThrows(IllegalArgumentException.class, () -> DecompositionState.fromWire("bogus"));
            assertThrows(IllegalArgumentException.class, () -> DecompositionState.fromWire(null));
        }

        @Test
        @DisplayName("CheckSeverity resolves its wire values")
        void checkSeverity() {
            assertEquals(Set.of("info", "warning", "error"),
                    Stream.of(CheckSeverity.values()).map(CheckSeverity::wireValue)
                            .collect(Collectors.toCollection(TreeSet::new)));
            assertEquals(CheckSeverity.ERROR, CheckSeverity.fromWire("ERROR"));
            assertThrows(IllegalArgumentException.class, () -> CheckSeverity.fromWire("fatal"));
            assertThrows(IllegalArgumentException.class, () -> CheckSeverity.fromWire(null));
        }

        @Test
        @DisplayName("InstructAction resolves its wire values")
        void instructAction() {
            assertEquals(Set.of("edit", "add", "rewrite"),
                    Stream.of(InstructAction.values()).map(InstructAction::wireValue)
                            .collect(Collectors.toCollection(TreeSet::new)));
            assertEquals(InstructAction.ADD, InstructAction.fromWire("add"));
            assertThrows(IllegalArgumentException.class, () -> InstructAction.fromWire("delete"));
            assertThrows(IllegalArgumentException.class, () -> InstructAction.fromWire(null));
        }
    }

    @Test
    @DisplayName("StagedPlanRecord round-trips and defaults its collections and status")
    void stagedPlanRoundTrips() throws Exception {
        var full = new StagedPlanRecord("relay-auth-v2", "relay-auth-v2", "a token exchange",
                List.of("pm-api"), List.of("relay-cursor-order"), StagedPlanRecord.STATUS_CHANGED);
        assertEquals(full, StagedPlanRecord.fromJson(full.toJson()));

        var minimal = new StagedPlanRecord("relay-docs", "relay-docs", null, null, null, null);
        assertTrue(minimal.expectedSurface().isEmpty());
        assertTrue(minimal.dependencies().isEmpty());
        assertEquals(StagedPlanRecord.STATUS_STAGED, minimal.status());
        assertFalse(minimal.toMap().containsKey("objective"));
        assertNull(StagedPlanRecord.fromJson(minimal.toJson()).objective());
        assertEquals(minimal, StagedPlanRecord.fromJson(minimal.toJson()));
    }

    @Test
    @DisplayName("StagedPlanRecord reports dropped and locked plans")
    void stagedPlanMarkers() {
        assertTrue(new StagedPlanRecord("a", "a", null, List.of(), List.of(),
                StagedPlanRecord.STATUS_DROPPED).isDropped());
        assertTrue(new StagedPlanRecord("b", "b", null, List.of(), List.of(),
                StagedPlanRecord.STATUS_LOCKED).isLocked());
    }

    @Test
    @DisplayName("WorkstreamRecord round-trips with its ordered plans")
    void workstreamRoundTrips() throws Exception {
        var workstream = new WorkstreamRecord("WS-1", "Reconnect", "the reconnect work", 1,
                List.of(new StagedPlanRecord("relay-cursor-order", "relay-cursor-order", null,
                        List.of("pm-relay", "pm-api"), List.of(), StagedPlanRecord.STATUS_STAGED)));
        assertEquals(workstream, WorkstreamRecord.fromJson(workstream.toJson()));
    }

    @Test
    @DisplayName("DecompositionCheckFinding round-trips with its severity")
    void findingRoundTrips() throws Exception {
        var finding = new DecompositionCheckFinding(CheckSeverity.ERROR,
                DecompositionCheckFinding.RULE_SURFACE_OVERLAP, List.of("relay-auth-v2", "relay-cursor-order"),
                "surface overlaps relay-cursor-order (pm-api)");
        assertEquals(finding, DecompositionCheckFinding.fromJson(finding.toJson()));
    }

    @Test
    @DisplayName("EpicDecomposition round-trips and answers the check and count questions")
    void epicDecompositionRoundTrips() throws Exception {
        var first = new StagedPlanRecord("relay-cursor-order", "relay-cursor-order", null,
                List.of("pm-api"), List.of(), StagedPlanRecord.STATUS_STAGED);
        var second = new StagedPlanRecord("relay-auth-v2", "relay-auth-v2", "token exchange",
                List.of("pm-api"), List.of(), StagedPlanRecord.STATUS_STAGED);
        var decomposition = new EpicDecomposition("relay-hardening", DecompositionState.REVIEW, 7,
                List.of(new WorkstreamRecord("WS-1", "Reconnect", null, 1, List.of(first)),
                        new WorkstreamRecord("WS-2", "Authentication", null, 2, List.of(second))),
                List.of(new DecompositionCheckFinding(CheckSeverity.ERROR,
                        DecompositionCheckFinding.RULE_SURFACE_OVERLAP, List.of("relay-auth-v2"),
                        "surface overlaps relay-cursor-order (pm-api)")));

        var roundTrip = EpicDecomposition.fromJson(decomposition.toJson());
        assertEquals(decomposition, roundTrip);
        assertFalse(roundTrip.everyCheckPasses());
        assertEquals(2, roundTrip.stagedPlanCount());
        assertEquals("relay-auth-v2", roundTrip.plan("relay-auth-v2").planId());
        assertNull(roundTrip.plan("no-such-plan"));

        var passing = new EpicDecomposition("relay-hardening", DecompositionState.CHECK, 7,
                decomposition.workstreams(), List.of());
        assertTrue(passing.everyCheckPasses());
    }

    @Test
    @DisplayName("DecompositionInstructRequest round-trips and defaults the action to edit")
    void instructRequestRoundTrips() throws Exception {
        var request = new DecompositionInstructRequest("relay-auth-v2",
                "Keep it out of pm-api: the exchange belongs into pm-relay.", InstructAction.EDIT);
        assertEquals(request, DecompositionInstructRequest.fromJson(request.toJson()));

        var added = new DecompositionInstructRequest(null, "add a docs plan", null);
        assertEquals(InstructAction.EDIT, added.action());
        assertFalse(added.toMap().containsKey("plan_id"));
        assertEquals(added, DecompositionInstructRequest.fromJson(added.toJson()));
    }

    @Test
    @DisplayName("DecompositionCommitResult round-trips and defaults the status to committed")
    void commitResultRoundTrips() throws Exception {
        var result = new DecompositionCommitResult(null, 12, 2);
        assertEquals(DecompositionCommitResult.STATUS_COMMITTED, result.status());
        var roundTrip = DecompositionCommitResult.fromJson(result.toJson());
        assertEquals(result, roundTrip);
        assertEquals(12L, roundTrip.ledgerRevision());
        assertEquals(2, roundTrip.launchablePlanCount());
    }

    @Nested
    @DisplayName("Negative parsing")
    class NegativeParsing {

        @Test
        @DisplayName("refuses a JSON value that is no object")
        void refusesNonObjectJson() {
            assertThrows(JsonFormatException.class, () -> StagedPlanRecord.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> WorkstreamRecord.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> DecompositionCheckFinding.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> EpicDecomposition.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> DecompositionInstructRequest.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> DecompositionCommitResult.fromJson("[]"));
        }

        @Test
        @DisplayName("refuses an object without the required fields")
        void refusesMissingFields() {
            assertThrows(JsonFormatException.class, () -> StagedPlanRecord.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> WorkstreamRecord.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> DecompositionCheckFinding.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> EpicDecomposition.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> DecompositionInstructRequest.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> DecompositionCommitResult.fromJson("{}"));
        }

        @Test
        @DisplayName("refuses a closed-list value that names no member")
        void refusesUnknownClosedListValues() {
            assertThrows(IllegalArgumentException.class, () -> EpicDecomposition.fromJson(
                    "{\"epic_id\":\"e\",\"status\":\"sideways\"}"));
            assertThrows(IllegalArgumentException.class, () -> DecompositionCheckFinding.fromJson(
                    "{\"severity\":\"fatal\",\"rule\":\"surface_overlap\"}"));
            assertThrows(IllegalArgumentException.class, () -> DecompositionInstructRequest.fromJson(
                    "{\"instruction\":\"x\",\"action\":\"delete\"}"));
        }

        @Test
        @DisplayName("refuses required constructor arguments that are null")
        void refusesNullArguments() {
            assertThrows(NullPointerException.class, () -> new StagedPlanRecord(null, "t", null, null, null, null));
            assertThrows(NullPointerException.class, () -> new WorkstreamRecord("WS-1", null, null, 1, null));
            assertThrows(NullPointerException.class, () -> new EpicDecomposition(null, DecompositionState.DRAFT, 0,
                    null, null));
            assertThrows(NullPointerException.class, () -> new DecompositionInstructRequest(null, null,
                    InstructAction.EDIT));
        }
    }
}
