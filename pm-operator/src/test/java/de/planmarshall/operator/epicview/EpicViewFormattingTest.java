/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.epicview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.util.List;
import java.util.stream.Stream;

import de.planmarshall.api.control.EpicWaitReason;
import de.planmarshall.api.control.WorkRow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pins what a staged row's wait reads in the epic's overview: every closed reason has its own line,
 * and no reason other than {@code none} is ever read as {@code can start}.
 */
@DisplayName("EpicViewFormatting")
@EnableGeneratorController
class EpicViewFormattingTest {

    /** The line a row that can start reads; no waiting row may show it. */
    private static final String CAN_START = "can start";

    private static WorkRow row(String waitReason) {
        return new WorkRow(Generators.nonBlankStrings().next(), null, "staged", waitReason, null, null,
                            List.of(), null, null, null, null, List.of(), List.of(), List.of());
    }

    private static Stream<Arguments> closedReasons() {
        return Stream.of(EpicWaitReason.values())
                            .filter(reason -> reason != EpicWaitReason.NONE)
                            .map(reason -> Arguments.of(reason.value()));
    }

    @ParameterizedTest
    @MethodSource("closedReasons")
    @DisplayName("every closed reason but none reads as its own wait, never as can start")
    void everyClosedReasonReadsItsOwnWait(String reason) {
        var text = EpicViewFormatting.waitText("en", row(reason), null, 3);

        assertFalse(text.isBlank(), "The reason '" + reason + "' must format to a line");
        assertNotEquals(CAN_START, text, "The reason '" + reason + "' must not read as can start");
    }

    @ParameterizedTest
    @MethodSource("closedReasons")
    @DisplayName("every closed reason but none has its wait text in German too")
    void everyClosedReasonReadsItsOwnWaitInGerman(String reason) {
        assertFalse(EpicViewFormatting.waitText("de", row(reason), null, 3).isBlank(),
                            "The reason '" + reason + "' must format to a line in German");
    }

    @Test
    @DisplayName("a held row says it is held, never that it can start")
    void heldRowSaysItIsHeld() {
        assertEquals("waits: held", EpicViewFormatting.waitText("en", row("held"), null, 3));
    }

    @Test
    @DisplayName("an unreadable row says its specification cannot be read, never that it can start")
    void unreadableRowSaysItsSpecificationCannotBeRead() {
        assertEquals("waits: its specification cannot be read",
                            EpicViewFormatting.waitText("en", row("unreadable"), null, 3));
    }

    @Test
    @DisplayName("a reason outside the closed set is echoed, never read as can start")
    void unknownReasonIsEchoed() {
        var reason = "unlisted-" + Generators.letterStrings(6, 10).next();

        assertEquals("waits: " + reason, EpicViewFormatting.waitText("en", row(reason), null, 3));
    }

    @Test
    @DisplayName("a row that waits for nothing reads as can start")
    void rowWithoutAWaitReadsAsCanStart() {
        assertEquals(CAN_START, EpicViewFormatting.waitText("en", row("none"), null, 3));
        assertEquals(CAN_START, EpicViewFormatting.waitText("en", row(null), null, 3));
    }
}
