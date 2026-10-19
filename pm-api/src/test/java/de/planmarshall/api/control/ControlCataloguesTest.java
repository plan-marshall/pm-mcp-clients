/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verbatim tests of the closed catalogues of the control contract. Each enumeration value is pinned
 * to its exact wire string so a silent rename fails here first, where the clients and the runtime
 * read it.
 */
@DisplayName("ControlCatalogues")
class ControlCataloguesTest {

    @Test
    @DisplayName("pins the pause points to now, the task, and the phase")
    void pausePoints() {
        assertEquals(List.of("now", "task", "phase"),
                Arrays.stream(PausePoint.values()).map(PausePoint::value).toList());
        assertEquals(PausePoint.AFTER_TASK, PausePoint.fromValue("task"));
        assertEquals(PausePoint.AFTER_PHASE, PausePoint.fromValue("AFTER_PHASE"));
    }

    @Test
    @DisplayName("pins the plan outcomes to the closed list of How a Plan Ends")
    void planOutcomes() {
        assertEquals(List.of("merged", "no_change", "handed_back", "returned", "cancelled", "abandoned",
                        "released", "aborted"),
                Arrays.stream(PlanOutcome.values()).map(PlanOutcome::value).toList());
        assertEquals(PlanOutcome.HANDED_BACK, PlanOutcome.fromValue("handed_back"));
    }

    @Test
    @DisplayName("pins the epic terminal reasons to completed, cancelled, and released")
    void epicTerminalReasons() {
        assertEquals(List.of("completed", "cancelled", "released"),
                Arrays.stream(EpicTerminalReason.values()).map(EpicTerminalReason::value).toList());
    }

    @Test
    @DisplayName("pins what a staged plan waits for, including none")
    void epicWaitReasons() {
        assertEquals(List.of("none", "dependency", "lock", "scope", "overlap", "stale", "held"),
                Arrays.stream(EpicWaitReason.values()).map(EpicWaitReason::value).toList());
        assertEquals(EpicWaitReason.OVERLAP, EpicWaitReason.fromValue("overlap"));
    }

    @Test
    @DisplayName("pins the review dispositions to the closed list")
    void reviewDispositions() {
        assertEquals(List.of("open", "kept", "watch", "dropped", "promoted"),
                Arrays.stream(ReviewDisposition.values()).map(ReviewDisposition::value).toList());
    }

    @Test
    @DisplayName("pins the chat roles, scopes, and states")
    void chatCatalogues() {
        assertEquals(List.of("replan", "decompose", "refine"),
                Arrays.stream(ChatRole.values()).map(ChatRole::value).toList());
        assertEquals(List.of("plan", "epic", "decomposition", "register_item", "register", "archive", "request"),
                Arrays.stream(ChatScope.values()).map(ChatScope::value).toList());
        assertEquals(List.of("open", "turn", "read-back", "apply", "end"),
                Arrays.stream(ChatState.values()).map(ChatState::wireValue).toList());
        assertEquals("chat.read-back", ChatState.READ_BACK.stateId());
        assertEquals(ChatState.READ_BACK, ChatState.fromWire("chat.read-back"));
        assertEquals(ChatState.OPEN, ChatState.fromWire("open"));
    }

    @Test
    @DisplayName("rejects unknown wire values and parses the canonical ones")
    void rejectsUnknownValues() {
        assertNull(PausePoint.fromValue("never"));
        assertNull(PlanOutcome.fromValue(null));
        assertNull(EpicWaitReason.fromValue(" "));
        assertNull(ReviewDisposition.fromValue("fixed"));
        assertNull(ChatRole.fromValue("planner"));
        assertNull(ChatScope.fromValue("decision"));
        assertThrows(IllegalArgumentException.class, () -> PausePoint.parse("x"));
        assertThrows(IllegalArgumentException.class, () -> PlanOutcome.parse("x"));
        assertThrows(IllegalArgumentException.class, () -> EpicTerminalReason.parse("x"));
        assertThrows(IllegalArgumentException.class, () -> EpicWaitReason.parse("x"));
        assertThrows(IllegalArgumentException.class, () -> ReviewDisposition.parse("x"));
        assertThrows(IllegalArgumentException.class, () -> ChatRole.parse("x"));
        assertThrows(IllegalArgumentException.class, () -> ChatScope.parse("x"));
        assertThrows(IllegalArgumentException.class, () -> ChatState.fromWire("x"));
    }
}
