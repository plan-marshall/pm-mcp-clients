/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.control.ChatRole;
import de.planmarshall.api.control.ChatScope;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The chat link of a start: where an entry's chat is routed, what an unopened or disabled chat may
 * carry, and the wire shapes its codec refuses.
 */
@DisplayName("StartChatResource")
@EnableGeneratorController
class StartChatResourceTest {

    private static final String REASON_KEY = "start.chat.offline";

    private static Map<String, Object> link() {
        return new HashMap<>(StartChatResource.link(Generators.nonBlankStrings().next(), ChatRole.REFINE, true, null)
                .toMap());
    }

    @ParameterizedTest(name = "a {0} entry routed to {1}: {2}")
    @CsvSource({"PLAN, REFINE, true", "PLAN, DECOMPOSE, false", "EPIC, DECOMPOSE, true", "EPIC, REFINE, false"})
    @DisplayName("should route a plan's entry to refine and an epic's to decompose")
    void shouldRouteByKind(EntryKind kind, ChatRole role, boolean routed) {
        var chat = StartChatResource.link(Generators.nonBlankStrings().next(), role, true, null);

        assertEquals(routed, chat.routedByKind(kind));
    }

    @Test
    @DisplayName("should keep the transcript with the entry and show no turns when neither is given")
    void shouldDefaultTranscriptAndTurns() throws Exception {
        var map = link();
        map.remove(StartChatResource.FIELD_TRANSCRIPT);
        map.remove(StartChatResource.FIELD_TURNS);

        var parsed = StartChatResource.fromMap(map);
        var constructed = new StartChatResource(Generators.nonBlankStrings().next(), ChatRole.REPLAN,
                ChatScope.REQUEST, false, REASON_KEY, null, null, null, null, null, null);

        assertAll(
                () -> assertEquals(StartTranscriptState.WITH_ENTRY, parsed.transcript()),
                () -> assertTrue(parsed.turns().isEmpty()),
                () -> assertEquals(StartTranscriptState.WITH_ENTRY, constructed.transcript()),
                () -> assertTrue(constructed.turns().isEmpty()),
                () -> assertEquals(constructed, StartChatResource.fromJson(constructed.toJson())));
    }

    static Stream<Arguments> malformedChats() {
        var unknown = "x-" + Generators.integers(0, 999).next();
        return Stream.of(
                Arguments.of("no entry_id", (Consumer<Map<String, Object>>) map ->
                        map.remove(StartChatResource.FIELD_ENTRY_ID)),
                Arguments.of("an unknown role", (Consumer<Map<String, Object>>) map ->
                        map.put(StartChatResource.FIELD_ROLE, unknown)),
                Arguments.of("an unknown scope", (Consumer<Map<String, Object>>) map ->
                        map.put(StartChatResource.FIELD_SCOPE, unknown)),
                Arguments.of("an unknown state", (Consumer<Map<String, Object>>) map -> {
                    map.put(StartChatResource.FIELD_CHAT_ID, unknown);
                    map.put(StartChatResource.FIELD_STATE, unknown);
                }),
                Arguments.of("an unknown transcript", (Consumer<Map<String, Object>>) map ->
                        map.put(StartChatResource.FIELD_TRANSCRIPT, unknown)),
                Arguments.of("an available chat naming a reason", (Consumer<Map<String, Object>>) map ->
                        map.put(StartChatResource.FIELD_DISABLED_REASON_KEY, REASON_KEY)),
                Arguments.of("a disabled chat naming no reason", (Consumer<Map<String, Object>>) map ->
                        map.put(StartChatResource.FIELD_AVAILABLE, false)),
                Arguments.of("a disabled chat naming a reason that is no message key",
                        (Consumer<Map<String, Object>>) map -> {
                            map.put(StartChatResource.FIELD_AVAILABLE, false);
                            map.put(StartChatResource.FIELD_DISABLED_REASON_KEY, "Offline");
                        }),
                Arguments.of("an unopened chat with a state", (Consumer<Map<String, Object>>) map ->
                        map.put(StartChatResource.FIELD_STATE, "open")),
                Arguments.of("turns that are no array", (Consumer<Map<String, Object>>) map ->
                        map.put(StartChatResource.FIELD_TURNS, unknown)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedChats")
    @DisplayName("should refuse a malformed chat as a format error")
    void shouldRefuseAMalformedChat(String violation, Consumer<Map<String, Object>> breakIt) {
        var map = link();
        breakIt.accept(map);

        assertThrows(JsonFormatException.class, () -> StartChatResource.fromMap(map), violation);
    }

    @Test
    @DisplayName("should refuse a streamed value that is no object")
    void shouldRefuseAStreamedValueThatIsNoObject() throws Exception {
        var text = JsonTree.write(List.of(Generators.nonBlankStrings().next()));

        try (var parser = new JsonFactory().createParser(text)) {
            assertThrows(JsonFormatException.class, () -> StartChatResource.readFrom(parser));
        }
    }
}
