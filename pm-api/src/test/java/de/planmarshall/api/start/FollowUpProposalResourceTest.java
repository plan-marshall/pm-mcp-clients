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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The chat about ended work: a disabled chat names its reason and goes to the role of its subject's
 * kind, and the codec refuses a chat that is not about the archive or that contradicts itself.
 */
@DisplayName("FollowUpProposalResource")
@EnableGeneratorController
class FollowUpProposalResourceTest {

    private static final String REASON_KEY = "start.follow_up.offline";

    @ParameterizedTest(name = "a disabled chat about an ended {0} goes to {1}")
    @CsvSource({"PLAN, REPLAN", "EPIC, DECOMPOSE"})
    @DisplayName("should route a disabled chat by its subject's kind and keep its reason")
    void shouldRouteADisabledChat(EntryKind kind, ChatRole role) throws Exception {
        var chat = FollowUpProposalResource.disabled(Generators.nonBlankStrings().next(), kind, REASON_KEY);

        assertAll(
                () -> assertEquals(role, chat.role()),
                () -> assertFalse(chat.available()),
                () -> assertNull(chat.proposal()),
                () -> assertEquals(REASON_KEY, chat.toMap().get(FollowUpProposalResource.FIELD_DISABLED_REASON_KEY)),
                () -> assertEquals(chat, FollowUpProposalResource.fromJson(chat.toJson())));
    }

    static Stream<Arguments> malformedChats() {
        var unknown = "x-" + Generators.integers(0, 999).next();
        return Stream.of(
                Arguments.of("no subject_id", (Consumer<Map<String, Object>>) map ->
                        map.remove(FollowUpProposalResource.FIELD_SUBJECT_ID)),
                Arguments.of("an unknown subject kind", (Consumer<Map<String, Object>>) map ->
                        map.put(FollowUpProposalResource.FIELD_SUBJECT_KIND, unknown)),
                Arguments.of("an unknown role", (Consumer<Map<String, Object>>) map ->
                        map.put(FollowUpProposalResource.FIELD_ROLE, unknown)),
                Arguments.of("an unknown scope", (Consumer<Map<String, Object>>) map ->
                        map.put(FollowUpProposalResource.FIELD_SCOPE, unknown)),
                Arguments.of("a scope other than the archive", (Consumer<Map<String, Object>>) map ->
                        map.put(FollowUpProposalResource.FIELD_SCOPE, "plan")),
                Arguments.of("a plan's chat routed to decompose", (Consumer<Map<String, Object>>) map ->
                        map.put(FollowUpProposalResource.FIELD_ROLE, "decompose")),
                Arguments.of("an available chat naming a reason", (Consumer<Map<String, Object>>) map ->
                        map.put(FollowUpProposalResource.FIELD_AVAILABLE, true)),
                Arguments.of("a disabled chat naming no reason", (Consumer<Map<String, Object>>) map ->
                        map.remove(FollowUpProposalResource.FIELD_DISABLED_REASON_KEY)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedChats")
    @DisplayName("should refuse a malformed chat about ended work as a format error")
    void shouldRefuseAMalformedChat(String violation, Consumer<Map<String, Object>> breakIt) {
        var map = new HashMap<>(FollowUpProposalResource.disabled(Generators.nonBlankStrings().next(), EntryKind.PLAN,
                REASON_KEY).toMap());
        breakIt.accept(map);

        assertThrows(JsonFormatException.class, () -> FollowUpProposalResource.fromMap(map), violation);
    }

    @Test
    @DisplayName("should refuse a streamed value that is no object")
    void shouldRefuseAStreamedValueThatIsNoObject() throws Exception {
        var text = JsonTree.write(List.of(Generators.nonBlankStrings().next()));

        try (var parser = new JsonFactory().createParser(text)) {
            assertThrows(JsonFormatException.class, () -> FollowUpProposalResource.readFrom(parser));
        }
    }
}
