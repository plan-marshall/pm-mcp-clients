/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.event;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EventKind")
class EventKindTest {

    @Test
    @DisplayName("every mapped event name resolves to its own kind, and none is mapped twice")
    void eventNamesResolve() {
        var seen = new HashSet<String>();
        for (var kind : EventKind.values()) {
            for (var event : kind.events()) {
                assertAll(
                        () -> assertTrue(seen.add(event), "the event name is mapped twice: " + event),
                        () -> assertEquals(kind, EventKind.fromEventName(event))
                );
            }
        }
    }

    @Test
    @DisplayName("the named events of the one event contract resolve")
    void namedEventsOfTheContract() {
        assertAll(
                () -> assertEquals(EventKind.QUESTION, EventKind.fromEventName("decision.opened")),
                () -> assertEquals(EventKind.ANSWERED_ELSEWHERE, EventKind.fromEventName("decision.answered")),
                () -> assertEquals(EventKind.HOLD, EventKind.fromEventName("hold.opened")),
                () -> assertEquals(EventKind.LANDING_FAILED, EventKind.fromEventName("landing.failed")),
                () -> assertEquals(EventKind.REPORT, EventKind.fromEventName("report.sent"))
        );
    }

    @Test
    @DisplayName("the legacy type tokens resolve, the event names do not")
    void typeTokensAreTheFallback() {
        assertAll(
                () -> assertEquals(EventKind.QUESTION, EventKind.fromType("question")),
                () -> assertEquals(EventKind.QUESTION, EventKind.fromType("decision")),
                () -> assertEquals(EventKind.REPORT, EventKind.fromType("report.sent")),
                () -> assertNull(EventKind.fromEventName("question"))
        );
    }

    @Test
    @DisplayName("a hold's kind resolves to the operator-wait kind of that name")
    void holdKindsResolve() {
        assertAll(
                () -> assertEquals(EventKind.OPERATOR_WAIT, EventKind.fromType("operator_wait")),
                () -> assertEquals(EventKind.STALLED, EventKind.fromType("stalled")),
                () -> assertEquals(EventKind.CONDITION, EventKind.fromType("condition")),
                () -> assertEquals(EventKind.LANDING_FAILED, EventKind.fromType("landing_failed")),
                () -> assertEquals(EventKind.HELD, EventKind.fromType("held")),
                () -> assertEquals(EventKind.SETUP, EventKind.fromType("setup"))
        );
    }

    @Test
    @DisplayName("an unmapped or absent value names no kind")
    void unmappedValues() {
        assertAll(
                () -> assertNull(EventKind.fromEventName("task.off_track")),
                () -> assertNull(EventKind.fromEventName("hold.cleared")),
                () -> assertNull(EventKind.fromEventName(null)),
                () -> assertNull(EventKind.fromEventName("")),
                () -> assertNull(EventKind.fromType("unknown_ping")),
                () -> assertNull(EventKind.fromType(" ")),
                () -> assertNull(EventKind.fromType(null))
        );
    }
}
