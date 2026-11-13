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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

@DisplayName("EventName: the chat's events and the streams of every member")
@EnableGeneratorController
class EventNameTest {

    static Stream<Arguments> chatEvents() {
        return Stream.of(
                Arguments.of("chat.turn.started", EventName.CHAT_TURN_STARTED, List.of("P", "E", "W", "N")),
                Arguments.of("chat.turn.delta", EventName.CHAT_TURN_DELTA, List.of("P", "E", "W", "N")),
                Arguments.of("chat.turn.progress", EventName.CHAT_TURN_PROGRESS, List.of("P", "E", "W", "N")),
                Arguments.of("chat.turn.completed", EventName.CHAT_TURN_COMPLETED, List.of("P", "E", "W", "N")),
                Arguments.of("chat.proposal", EventName.CHAT_PROPOSAL, List.of("P", "E", "W", "N")),
                Arguments.of("chat.ended", EventName.CHAT_ENDED, List.of("P", "E", "W", "N")),
                Arguments.of("plan.revision_queued", EventName.PLAN_REVISION_QUEUED, List.of("R", "P", "E")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("chatEvents")
    @DisplayName("each of the seven names is a member and is sent on exactly its catalogue streams")
    void sevenNamesWithTheirStreams(String wire, EventName expected, List<String> streams) {
        var name = EventName.fromWire(wire);

        assertAll(
                () -> assertEquals(expected, name),
                () -> assertEquals(wire, name.wire()),
                () -> assertEquals(streams, name.streams().stream().map(EventStream::wire).toList()));
    }

    @Test
    @DisplayName("every member declares at least one stream, and the declaration cannot be changed")
    void everyMemberHasAStream() {
        for (var name : EventName.values()) {
            assertFalse(name.streams().isEmpty(), name.wire());
        }
        var streams = EventName.ACTIVITY.streams();

        assertThrows(UnsupportedOperationException.class, () -> streams.add(EventStream.RUNTIME));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("streamControlEvents")
    @DisplayName("a stream-control event is sent on every stream, so a stream added later is covered")
    void streamControlOnEveryStream(String wire, EventName name) {
        assertEquals(EnumSet.allOf(EventStream.class), EnumSet.copyOf(name.streams()), wire);
    }

    static Stream<Arguments> streamControlEvents() {
        return Stream.of(Arguments.of("reset", EventName.RESET), Arguments.of("stream.closed", EventName.STREAM_CLOSED));
    }

    @Test
    @DisplayName("an empty stream set is refused, and a set given is kept as given")
    void emptyStreamSetIsRefused() {
        var empty = EnumSet.noneOf(EventStream.class);
        var given = EnumSet.of(EventStream.PLAN, EventStream.ENTRY);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> EventName.requireStreams(empty)),
                () -> assertEquals(given, EventName.requireStreams(given)));
    }

    @Test
    @DisplayName("a name outside the closed list is refused")
    void nameOutsideTheListIsRefused() {
        var unknown = "chat.zz_" + Generators.letterStrings(3, 10).next().toLowerCase(Locale.ROOT);

        assertAll(
                () -> assertNull(EventName.fromWireOrNull(unknown)),
                () -> assertFalse(EventName.isCatalogueMember(unknown)),
                () -> assertThrows(IllegalArgumentException.class, () -> EventName.fromWire(unknown)),
                () -> assertThrows(IllegalArgumentException.class, () -> EventName.fromWire("chat.turn")));
    }

    @Test
    @DisplayName("the streams are the closed catalogue list, and an unknown letter is refused")
    void closedStreamList() {
        var unknown = "zz" + Generators.letterStrings(3, 10).next();

        assertAll(
                () -> assertEquals(List.of("R", "P", "E", "W", "M", "N", "Rep", "A"), EventStream.wireValues()),
                () -> assertEquals(EventStream.ENTRY, EventStream.fromWire("N")),
                () -> assertEquals(EventStream.REPORTS, EventStream.fromWireOrNull("Rep")),
                () -> assertNull(EventStream.fromWireOrNull(unknown)),
                () -> assertNull(EventStream.fromWireOrNull(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> EventStream.fromWire(unknown)));
    }
}
