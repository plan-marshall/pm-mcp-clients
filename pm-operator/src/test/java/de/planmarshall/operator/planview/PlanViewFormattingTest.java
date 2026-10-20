/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.planview;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.planmarshall.api.process.LeafTask;
import de.planmarshall.api.process.NodeState;
import de.planmarshall.api.process.ProcessTokens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlanViewFormatting")
class PlanViewFormattingTest {

    @Test
    @DisplayName("clean strips control sequences and line breaks")
    void clean() {
        assertAll(
                            () -> assertEquals("", PlanViewFormatting.clean(null)),
                            () -> assertEquals("plain", PlanViewFormatting.clean("\u001B[31mplain\u001B[0m")),
                            () -> assertEquals("a b", PlanViewFormatting.clean("a\nb")),
                            () -> assertEquals("x", PlanViewFormatting.clean("  x  "))
        );
    }

    @Test
    @DisplayName("duration covers seconds, minutes, hours and zero")
    void duration() {
        assertAll(
                            () -> assertEquals("", PlanViewFormatting.duration(0)),
                            () -> assertEquals("45 s", PlanViewFormatting.duration(45)),
                            () -> assertEquals("2 min", PlanViewFormatting.duration(120)),
                            () -> assertEquals("1 h 5 min", PlanViewFormatting.duration(3900))
        );
    }

    @Test
    @DisplayName("every node state has its label, including the run counts")
    void nodeStates() {
        assertAll(
                            () -> assertEquals("done", PlanViewFormatting.nodeState("en", NodeState.DONE, 1, 1)),
                            () -> assertEquals("done \u00d72", PlanViewFormatting.nodeState("en", NodeState.DONE, 2, 1)),
                            () -> assertEquals("running", PlanViewFormatting.nodeState("en", NodeState.RUNNING, 1, 1)),
                            () -> assertEquals("running \u00b7 run 3",
                                                PlanViewFormatting.nodeState("en", NodeState.RUNNING, 1, 3)),
                            () -> assertEquals("waiting for you",
                                                PlanViewFormatting.nodeState("en", NodeState.WAITING_FOR_OPERATOR, 1, 1)),
                            () -> assertEquals("failed", PlanViewFormatting.nodeState("en", NodeState.FAILED, 1, 1)),
                            () -> assertEquals("not taken", PlanViewFormatting.nodeState("en", NodeState.NOT_TAKEN, 1, 1)),
                            () -> assertEquals("not in this plan",
                                                PlanViewFormatting.nodeState("en", NodeState.NOT_IN_PLAN, 1, 1)),
                            () -> assertEquals("planned", PlanViewFormatting.nodeState("en", NodeState.PLANNED, 1, 1))
        );
    }

    @Test
    @DisplayName("phase states map the closed runtime list and keep unknown values")
    void phaseStates() {
        assertAll(
                            () -> assertEquals("done", PlanViewFormatting.phaseState("en", "done")),
                            () -> assertEquals("running", PlanViewFormatting.phaseState("en", "running")),
                            () -> assertEquals("running", PlanViewFormatting.phaseState("en", null)),
                            () -> assertEquals("waiting for you", PlanViewFormatting.phaseState("en", "waiting")),
                            () -> assertEquals("waiting for you", PlanViewFormatting.phaseState("en", "waiting_for_you")),
                            () -> assertEquals("waiting for you",
                                                PlanViewFormatting.phaseState("en", "waiting_for_operator")),
                            () -> assertEquals("failed", PlanViewFormatting.phaseState("en", "failed")),
                            () -> assertEquals("planned", PlanViewFormatting.phaseState("en", "planned")),
                            () -> assertEquals("paused", PlanViewFormatting.phaseState("en", "paused")),
                            () -> assertEquals("held", PlanViewFormatting.phaseState("en", "held")),
                            () -> assertEquals("pausing after the phase", PlanViewFormatting.phaseState("en", "pausing")),
                            () -> assertEquals("custom", PlanViewFormatting.phaseState("en", "custom"))
        );
    }

    @Test
    @DisplayName("tokens show none yet, a floor, or a plain split")
    void tokens() {
        assertAll(
                            () -> assertEquals("none yet", PlanViewFormatting.tokens("en",
                                                new ProcessTokens(0, 0, 0, false, 0, 0, true, false))),
                            () -> assertTrue(PlanViewFormatting.tokens("en",
                                                new ProcessTokens(412000, 380000, 32000, true, 17, 15, false, false))
                                                .startsWith("\u2265")),
                            () -> assertEquals("12 k (11 k in \u00b7 1.0 k out)",
                                                PlanViewFormatting.tokens("en",
                                                                    new ProcessTokens(12000, 11000, 1000, false, 2, 2,
                                                                                        false, false)))
        );
    }

    @Test
    @DisplayName("a task line carries id, role, harness, model, attempt, status, outcome and duration")
    void taskLine() {
        var task = new LeafTask("TASK-003", "implement", "claude", "sonnet", 2, "running", null, 360, 48000, null, null);
        var line = PlanViewFormatting.taskLine("en", task);
        assertAll(
                            () -> assertTrue(line.contains("TASK-003")),
                            () -> assertTrue(line.contains("implement")),
                            () -> assertTrue(line.contains("claude sonnet")),
                            () -> assertTrue(line.contains("attempt 2")),
                            () -> assertTrue(line.contains("running")),
                            () -> assertTrue(line.contains("6 min"))
        );
        assertEquals("TASK-1 \u00b7 implement \u00b7 agent default \u00b7 attempt 1 \u00b7 planned \u00b7 -",
                            PlanViewFormatting.taskLine("en",
                                                new LeafTask("TASK-1", "implement", null, null, 1, "planned", null, 0,
                                                                    0, null, null)));
        assertEquals("\u2713 line", PlanViewFormatting.withGlyph("\u2713", "line"));
    }

    @Test
    @DisplayName("off track names the signal with or without a measured value")
    void offTrack() {
        var measured = PlanViewFormatting.offTrack("en", "repeating", "show", 5, 5);
        var unmeasured = PlanViewFormatting.offTrack("en", "no_progress", "yield", null, null);
        assertAll(
                            () -> assertTrue(measured.contains("repeating"), measured),
                            () -> assertTrue(measured.contains("5 of 5"), measured),
                            () -> assertTrue(measured.contains("shown"), measured),
                            () -> assertTrue(unmeasured.contains("no progress"), unmeasured),
                            () -> assertTrue(unmeasured.contains("yielded"), unmeasured)
        );
    }

    @Test
    @DisplayName("a signal names its closed-list value and falls back to the raw value")
    void signals() {
        assertAll(
                            () -> assertEquals("repeating", PlanViewFormatting.signal("en", "repeating")),
                            () -> assertEquals("outside footprint", PlanViewFormatting.signal("en", "outside_footprint")),
                            () -> assertEquals("custom", PlanViewFormatting.signal("en", "custom")),
                            () -> assertEquals("", PlanViewFormatting.signal("en", null))
        );
    }
}
