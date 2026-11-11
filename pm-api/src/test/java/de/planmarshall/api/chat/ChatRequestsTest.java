/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;

@DisplayName("Chat requests: the message, the 202 answer, the proposal answer and the refusals")
@EnableGeneratorController
class ChatRequestsTest {

    private static String id() {
        return Generators.nonBlankStrings().next();
    }

    private static String textOf(int length) {
        return Generators.letterStrings(length, length).next();
    }

    private static String blank() {
        return Generators.fixedValues("", " ", "\t", "  \n ").next();
    }

    private static Map<String, Object> map(Object... keysAndValues) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    @Nested
    @DisplayName("ChatTurnRequest, the operator's message")
    class TurnRequest {

        @ParameterizedTest(name = "a text of {0} characters is accepted and round-trips")
        @ValueSource(ints = {1, 8000})
        void acceptsTheBounds(int length) throws Exception {
            var request = new ChatTurnRequest(textOf(length), id());

            var parsed = ChatTurnRequest.fromJson(request.toJson());

            assertAll(
                    () -> assertEquals(request, parsed),
                    () -> assertEquals(length, parsed.text().length()));
        }

        @ParameterizedTest(name = "a text of {0} characters is refused")
        @ValueSource(ints = {0, 8001})
        void refusesOutsideTheBounds(int length) {
            var text = length == 0 ? "" : textOf(length);
            var clientTurnId = id();

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurnRequest(text, clientTurnId)),
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurnRequest.fromMap(
                            map("text", text, "client_turn_id", clientTurnId))));
        }

        @Test
        @DisplayName("the limits are 1 and 8000 characters")
        void limits() {
            assertAll(
                    () -> assertEquals(1, ChatTurnRequest.MIN_TEXT_LENGTH),
                    () -> assertEquals(8000, ChatTurnRequest.MAX_TEXT_LENGTH));
        }

        @Test
        @DisplayName("characters are counted as code points, not as UTF-16 units")
        void countsCodePoints() {
            var astral = new String(Character.toChars(0x1F600));
            var clientTurnId = id();

            var tooLong = astral.repeat(8001);

            var request = new ChatTurnRequest(astral.repeat(8000), clientTurnId);

            assertAll(
                    () -> assertEquals(16000, request.text().length()),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurnRequest(tooLong, clientTurnId)));
        }

        @Test
        @DisplayName("a blank client_turn_id is refused, by the record and on the wire")
        void refusesBlankClientTurnId() {
            var text = textOf(12);
            var clientTurnId = blank();

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurnRequest(text, clientTurnId)),
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurnRequest.fromMap(
                            map("text", text, "client_turn_id", clientTurnId))));
        }

        @Test
        @DisplayName("a repeated client_turn_id round-trips unchanged")
        void repeatedClientTurnIdRoundTrips() throws Exception {
            var clientTurnId = id();
            var first = new ChatTurnRequest(textOf(20), clientTurnId);
            var repeat = new ChatTurnRequest(first.text(), clientTurnId);

            var parsedFirst = ChatTurnRequest.fromJson(first.toJson());
            var parsedRepeat = ChatTurnRequest.fromJson(repeat.toJson());

            assertAll(
                    () -> assertEquals(clientTurnId, parsedFirst.clientTurnId()),
                    () -> assertEquals(clientTurnId, parsedRepeat.clientTurnId()),
                    () -> assertEquals(parsedFirst, parsedRepeat),
                    () -> assertEquals(first.toJson(), repeat.toJson()));
        }

        @Test
        @DisplayName("the wire names are text and client_turn_id, in that order")
        void wireNames() {
            var request = new ChatTurnRequest(textOf(5), id());

            assertEquals(List.of("text", "client_turn_id"), List.copyOf(request.toMap().keySet()));
        }

        @Test
        @DisplayName("an unknown, a missing or a mistyped member is refused")
        void refusesMalformedMembers() {
            var text = textOf(8);
            var clientTurnId = id();

            assertAll(
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurnRequest.fromMap(
                            map("text", text, "client_turn_id", clientTurnId, "zz_extra", id()))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnRequest.fromMap(map("client_turn_id", clientTurnId))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnRequest.fromMap(map("text", text))),
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurnRequest.fromMap(
                            map("text", 42L, "client_turn_id", clientTurnId))),
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurnRequest.fromJson("[]")));
        }
    }

    @Nested
    @DisplayName("ChatTurnAccepted, the 202 answer")
    class TurnAccepted {

        @Test
        @DisplayName("the answer is 202 and round-trips")
        void roundTrips() throws Exception {
            var accepted = new ChatTurnAccepted(id());

            var parsed = ChatTurnAccepted.fromJson(accepted.toJson());

            assertAll(
                    () -> assertEquals(202, ChatTurnAccepted.HTTP_STATUS),
                    () -> assertEquals(accepted, parsed),
                    () -> assertEquals(List.of("turn_id"), List.copyOf(accepted.toMap().keySet())));
        }

        @Test
        @DisplayName("the turn id is read off the wire as sent")
        void readsTheWire() throws Exception {
            var turnId = Generators.letterStrings(2, 12).next();

            var parsed = ChatTurnAccepted.fromJson("{\"turn_id\":\"" + turnId + "\"}");

            assertEquals(turnId, parsed.turnId());
        }

        @Test
        @DisplayName("a blank or missing turn id and an unknown member are refused")
        void refusesMalformed() {
            var turnId = blank();

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> new ChatTurnAccepted(turnId)),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnAccepted.fromMap(map("turn_id", turnId))),
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurnAccepted.fromMap(map())),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnAccepted.fromMap(map("turn_id", id(), "zz_extra", id()))));
        }
    }

    @Nested
    @DisplayName("ProposalAnswerRequest, the accept and the reject")
    class ProposalAnswer {

        @Test
        @DisplayName("the answer names the proposal's turn id and round-trips")
        void roundTrips() throws Exception {
            var answer = new ProposalAnswerRequest(id());

            var parsed = ProposalAnswerRequest.fromJson(answer.toJson());

            assertAll(
                    () -> assertEquals(answer, parsed),
                    () -> assertEquals(List.of("turn_id"), List.copyOf(answer.toMap().keySet())),
                    () -> assertEquals(answer.turnId(), answer.toMap().get("turn_id")));
        }

        @Test
        @DisplayName("answers to two proposals differ by their turn ids")
        void distinguishesProposals() {
            var first = new ProposalAnswerRequest(Generators.letterStrings(4, 8).next() + "-a");
            var second = new ProposalAnswerRequest(Generators.letterStrings(4, 8).next() + "-b");

            assertNotEquals(first, second);
        }

        @Test
        @DisplayName("a blank or missing turn id and an unknown member are refused")
        void refusesMalformed() {
            var turnId = blank();

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> new ProposalAnswerRequest(turnId)),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ProposalAnswerRequest.fromMap(map("turn_id", turnId))),
                    () -> assertThrows(JsonFormatException.class, () -> ProposalAnswerRequest.fromMap(map())),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ProposalAnswerRequest.fromMap(map("turn_id", id(), "zz_extra", id()))),
                    () -> assertThrows(JsonFormatException.class, () -> ProposalAnswerRequest.fromJson("\"x\"")));
        }
    }

    @Nested
    @DisplayName("The chat's four refusals against the catalogue")
    class Refusals {

        @ParameterizedTest(name = "{0} is answered {1}")
        @CsvSource(delimiter = '|', textBlock = """
                act_not_available | 409 | This act is not possible now      | Diese Aktion ist gerade nicht möglich
                conflict          | 409 | Something changed in the meantime | Etwas hat sich inzwischen geändert
                target_not_found  | 404 | This target does not exist        | Dieses Ziel gibt es nicht
                invalid_arguments | 400 | The request was not accepted      | Die Anfrage wurde nicht angenommen
                """)
        void refusalIsInTheCatalogue(String wire, int status, String titleEn, String titleDe) {
            var code = ApiErrorCode.fromWire(wire);

            assertAll(
                    () -> assertEquals(wire, code.wire()),
                    () -> assertEquals(status, code.status()),
                    () -> assertEquals(titleEn, ErrorTexts.title("en", code)),
                    () -> assertEquals(titleDe, ErrorTexts.title("de", code)),
                    () -> assertEquals(titleEn, ErrorTexts.title("en", wire)),
                    () -> assertEquals(titleDe, ErrorTexts.title("de", wire)));
        }
    }
}
