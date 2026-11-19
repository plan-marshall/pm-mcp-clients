/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.tui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.ConsoleNoticeBuffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@EnableGeneratorController
@DisplayName("ConsoleNoticeBuffer")
class ConsoleNoticeBufferTest {

    @Test
    @DisplayName("prints a notice while no view is open")
    void inactiveBufferPrints() {
        var buffer = new ConsoleNoticeBuffer();
        assertAll(
                            () -> assertFalse(buffer.isActive()),
                            () -> assertFalse(buffer.hold(ConsoleNoticeBuffer.Category.DECISION)),
                            () -> assertFalse(buffer.hasHeldNotices())
        );
    }

    @Test
    @DisplayName("holds notices while the view is open and coalesces them on leaving")
    void holdsAndCoalesces() {
        var buffer = new ConsoleNoticeBuffer();
        buffer.activate();
        assertTrue(buffer.hold(ConsoleNoticeBuffer.Category.DECISION));
        assertTrue(buffer.hold(ConsoleNoticeBuffer.Category.REPORT));
        assertTrue(buffer.hold(ConsoleNoticeBuffer.Category.REPORT));
        assertTrue(buffer.hasHeldNotices());

        var summary = buffer.drain("en").summary();
        assertAll(
                            () -> assertEquals("While the overview was open: 1 decision arrived, 2 new reports.",
                                                summary),
                            () -> assertFalse(buffer.hasHeldNotices()),
                            () -> assertTrue(buffer.isActive())
        );
    }

    @Test
    @DisplayName("counts the other waiting items as well")
    void countsNeeds() {
        var buffer = new ConsoleNoticeBuffer();
        buffer.activate();
        buffer.hold(ConsoleNoticeBuffer.Category.NEED);
        assertEquals("While the overview was open: 1 item waits.", buffer.drain("en").summary());
    }

    @Test
    @DisplayName("the summary renders in German")
    void summaryIsLocalized() {
        var buffer = new ConsoleNoticeBuffer();
        buffer.activate();
        buffer.hold(ConsoleNoticeBuffer.Category.DECISION);
        buffer.hold(ConsoleNoticeBuffer.Category.REPORT);
        assertEquals("W\u00e4hrend die \u00dcbersicht offen war: 1 Entscheidung kam, 1 neuer Bericht.",
                            buffer.drain("de").summary());
    }

    @Test
    @DisplayName("the summary is empty when nothing was held")
    void emptySummary() {
        var buffer = new ConsoleNoticeBuffer();
        assertAll(
                            () -> assertEquals("", buffer.drain("en").summary()),
                            () -> assertEquals("", ConsoleNoticeBuffer.summarize("en", List.of(),
                                                ConsoleNoticeBuffer.overviewPlace("en"))),
                            () -> assertEquals("", ConsoleNoticeBuffer.summarize("en", null,
                                                ConsoleNoticeBuffer.overviewPlace("en")))
        );
    }

    @Test
    @DisplayName("a second drain after a clear keeps the line away")
    void clearDropsHeldNotices() {
        var buffer = new ConsoleNoticeBuffer();
        buffer.activate();
        buffer.hold(ConsoleNoticeBuffer.Category.DECISION);
        buffer.clear();
        assertFalse(buffer.hasHeldNotices());
    }

    @Test
    @DisplayName("a coalesced report count is kept")
    void countsCoalescedReports() {
        var buffer = new ConsoleNoticeBuffer();
        buffer.activate();
        buffer.hold(ConsoleNoticeBuffer.Category.REPORT, 3);
        assertEquals("While the overview was open: 3 new reports.", buffer.drain("en").summary());
    }

    @Test
    @DisplayName("a verbatim line is kept and drained after the summary")
    void keepsVerbatimLines() {
        var buffer = new ConsoleNoticeBuffer();
        buffer.activate();
        buffer.hold(ConsoleNoticeBuffer.Category.NEED);
        var line1 = "paired: " + Generators.letterStrings(4, 10).next();
        var line2 = Generators.letterStrings(10, 25).next();
        assertTrue(buffer.holdLine(line1));
        assertTrue(buffer.holdLine(line2));
        var drained = buffer.drain("en");
        assertAll(
                            () -> assertEquals("While the overview was open: 1 item waits.", drained.summary()),
                            () -> assertEquals(List.of(line1, line2), drained.lines()),
                            () -> assertFalse(buffer.hasHeldNotices())
        );
    }

    @Test
    @DisplayName("a line offered while the view is closed is printed by the caller")
    void closedBufferRejectsLine() {
        assertFalse(new ConsoleNoticeBuffer().holdLine(Generators.letterStrings(4, 10).next()));
    }

    @Test
    @DisplayName("the meanwhile text names the place the operator was in")
    void textNamesThePlace() {
        var place = Generators.letterStrings(5, 10).next();
        assertAll(
                            () -> assertEquals("While the overview was open: 1 decision arrived.",
                                                MessageCatalogue.format("en", "operator.overview.meanwhile",
                                                                    Map.of("where", "the overview",
                                                                                        "events", "1 decision arrived"))),
                            () -> assertEquals("While " + place + " was open: 1 decision arrived.",
                                                MessageCatalogue.format("en", "operator.overview.meanwhile",
                                                                    Map.of("where", place,
                                                                                        "events", "1 decision arrived")))
        );
    }

    @Test
    @DisplayName("a drain names the place it is given, and the overview by default")
    void drainNamesThePlace() {
        var buffer = new ConsoleNoticeBuffer();
        buffer.activate();
        buffer.hold(ConsoleNoticeBuffer.Category.DECISION);
        var place = Generators.letterStrings(5, 10).next();
        assertAll(
                            () -> assertEquals("While " + place + " was open: 1 decision arrived.",
                                                buffer.drain("en", place).summary()),
                            () -> assertEquals("", buffer.drain("en").summary()),
                            () -> assertEquals("While the overview was open: nothing arrived.",
                                                ConsoleNoticeBuffer.leavingLine("en",
                                                                    ConsoleNoticeBuffer.overviewPlace("en"))),
                            () -> assertEquals("While " + place + " was open: nothing arrived.",
                                                ConsoleNoticeBuffer.leavingLine("en", place)),
                            () -> assertEquals("W\u00e4hrend die \u00dcbersicht offen war: nichts kam.",
                                                ConsoleNoticeBuffer.leavingLine("de",
                                                                    ConsoleNoticeBuffer.overviewPlace("de")))
        );
    }
}
