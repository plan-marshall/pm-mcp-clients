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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("EventStream: the stream of each kind of scope corresponds to its EventScopeKind")
class EventStreamTest {

    static Stream<Arguments> scopeStreams() {
        return Stream.of(
                Arguments.of(EventScopeKind.RUNTIME, "R"),
                Arguments.of(EventScopeKind.PLAN, "P"),
                Arguments.of(EventScopeKind.EPIC, "E"),
                Arguments.of(EventScopeKind.WORKSPACE, "W"),
                Arguments.of(EventScopeKind.MACHINE, "M"),
                Arguments.of(EventScopeKind.ENTRY, "N"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scopeStreams")
    @DisplayName("each kind of scope has its one stream, and that stream names the kind back")
    void eachScopeKindHasItsStream(EventScopeKind kind, String letter) {
        var stream = EventStream.of(kind);

        assertAll(kind.wire(),
                () -> assertEquals(letter, stream.wire()),
                () -> assertEquals(kind, stream.scopeKind()));
    }

    @Test
    @DisplayName("every EventScopeKind has a stream and only the reports and activity streams have no kind")
    void correspondenceIsComplete() {
        var kindsOfStreams = Arrays.stream(EventStream.values()).map(EventStream::scopeKind)
                .filter(Objects::nonNull).toList();
        var streamsWithoutKind = Arrays.stream(EventStream.values()).filter(stream -> stream.scopeKind() == null)
                .toList();

        assertAll(
                () -> assertEquals(List.of(EventScopeKind.values()), kindsOfStreams),
                () -> assertEquals(List.of(EventStream.REPORTS, EventStream.ACTIVITY), streamsWithoutKind),
                () -> assertNull(EventStream.REPORTS.scopeKind()));
    }
}
