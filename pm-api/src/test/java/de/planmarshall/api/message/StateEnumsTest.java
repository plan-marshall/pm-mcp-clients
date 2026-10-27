/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.message;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.control.EpicState;
import de.planmarshall.api.decision.GateKind;
import de.planmarshall.api.overview.LandingState;
import de.planmarshall.api.overview.PlanActivity;
import de.planmarshall.api.system.CleanupKind;
import de.planmarshall.api.system.ModelStatus;

/**
 * Pins the wire values of the state enums the shared display vocabulary reads, and the resolution
 * behaviour each of them offers.
 */
class StateEnumsTest {

    @Test
    @DisplayName("Should expose the wire value every state enum carries")
    void shouldExposeTheWireValueEveryStateEnumCarries() {
        assertAll("Wire values",
                () -> assertEquals(
                        List.of("running", "pausing", "paused", "waiting_for_you", "held", "staged", "ended"),
                        wireValues(PlanActivity.values(), PlanActivity::value),
                        "Plan activity should name every phase state"),
                () -> assertEquals(
                        List.of("running", "waiting_for_you", "held", "landing", "ended"),
                        wireValues(EpicState.values(), EpicState::value),
                        "Epic state should name every epic state"),
                () -> assertEquals(
                        List.of("running", "failed", "written", "committed", "landed"),
                        wireValues(LandingState.values(), LandingState::value),
                        "Landing state should name both carriers"),
                () -> assertEquals(
                        List.of("plan", "epic", "read_reports", "unenrolled_project"),
                        wireValues(CleanupKind.values(), CleanupKind::value),
                        "Cleanup kind should name every item kind"),
                () -> assertEquals(
                        List.of("ok", "retry", "error", "not_measured"),
                        wireValues(ModelStatus.values(), ModelStatus::value),
                        "Model status should name every derived status"),
                () -> assertEquals(
                        List.of("ci_check", "coverage"),
                        wireValues(GateKind.values(), GateKind::value),
                        "Gate kind should name every gate the scenarios use"));
    }

    @Test
    @DisplayName("Should resolve a state from its wire value and its constant name")
    void shouldResolveStateFromWireValueAndConstantName() {
        assertAll("Resolution",
                () -> assertEquals(EpicState.WAITING_FOR_YOU, EpicState.fromValue("waiting_for_you"),
                        "Should resolve the wire value"),
                () -> assertEquals(EpicState.WAITING_FOR_YOU, EpicState.fromValue("WAITING_FOR_YOU"),
                        "Should resolve the constant name"),
                () -> assertEquals(CleanupKind.UNENROLLED_PROJECT, CleanupKind.fromValue("unenrolled_project"),
                        "Should resolve a two-word kind"));
    }

    @Test
    @DisplayName("Should read the gate kind of a waiver gate that names its scope")
    void shouldReadGateKindOfWaiverGateWithScope() {
        assertAll("Waiver gate",
                () -> assertEquals(GateKind.CI_CHECK, GateKind.fromValue("ci_check:integration-tests"),
                        "Should read the kind before the separator"),
                () -> assertEquals(GateKind.COVERAGE, GateKind.fromValue("coverage:pm-api"),
                        "Should read the kind before the separator"),
                () -> assertEquals(GateKind.COVERAGE, GateKind.fromValue("coverage"),
                        "Should accept a bare kind"));
    }

    @Test
    @DisplayName("Should refuse an unknown state and answer null from the tolerant reader")
    void shouldRefuseUnknownStateAndAnswerNullFromTolerantReader() {
        assertAll("Unknown values",
                () -> assertNull(EpicState.fromValue("no_such_state"), "Tolerant reader should answer null"),
                () -> assertNull(EpicState.fromValue(null), "Tolerant reader should answer null for null"),
                () -> assertNull(GateKind.fromValue("no_such_gate:scope"), "Tolerant reader should answer null"),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> EpicState.parse("no_such_state"),
                        "Strict reader should refuse an unknown state"),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> LandingState.parse("no_such_state"),
                        "Strict reader should refuse an unknown state"),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> CleanupKind.parse("no_such_kind"),
                        "Strict reader should refuse an unknown kind"),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ModelStatus.parse("no_such_status"),
                        "Strict reader should refuse an unknown status"),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> GateKind.parse("no_such_gate"),
                        "Strict reader should refuse an unknown gate"));
    }

    /**
     * @param constants the enum constants
     * @param value     the constant's wire value
     * @param <E>       the enum type
     * @return the wire values, in declaration order
     */
    private static <E extends Enum<E>> List<String> wireValues(E[] constants, Function<E, String> value) {
        return Arrays.stream(constants).map(value).toList();
    }
}
