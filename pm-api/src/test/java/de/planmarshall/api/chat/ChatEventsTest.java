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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.event.EventName;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

@DisplayName("Chat events: the payloads of the six chat events and plan.revision_queued")
@EnableGeneratorController
class ChatEventsTest {

    @FunctionalInterface
    private interface Codec {
        String roundTrip(String json) throws IOException;
    }

    private static final String GOLDEN_OPERATOR_STARTED = """
            {"chat_id":"chat-7","turn_id":"t-1","kind":"message","author":"operator","format":"plain",
             "untrusted":false,"text":"Split TASK-4"}
            """;

    private static final String GOLDEN_ROLE_STARTED = """
            {"chat_id":"chat-7","turn_id":"t-2","kind":"message","author":"replan","format":"markdown",
             "untrusted":false,"model":"opus",
             "context":{"knows":[{"key":"control.chat.knows.plan","args":{"id":"fix-relay"}}],
                        "as_of":"2026-11-10T18:00:00Z"}}
            """;

    private static final String GOLDEN_DELTA = """
            {"chat_id":"chat-7","turn_id":"t-2","text":"Split TASK-4 "}
            """;

    private static final String GOLDEN_PROGRESS = """
            {"chat_id":"chat-7","turn_id":"t-2","step":{"key":"control.chat.step.reading",
             "args":{"target":"TASK-4"}}}
            """;

    private static final String GOLDEN_COMPLETED = """
            {"chat_id":"chat-7","turn_id":"t-2","kind":"message","status":"complete","untrusted":false,
             "text":"Split TASK-4 into two tasks.","refs":[{"kind":"task","id":"TASK-4"},
             {"kind":"act","act":"resume","subject":{"kind":"plan","id":"fix-relay"}}]}
            """;

    private static final String GOLDEN_FAILED = """
            {"chat_id":"chat-7","turn_id":"t-4","kind":"message","status":"failed","untrusted":false,
             "text":"Half","refs":[],"reason":{"key":"control.chat.reason.too_long","args":{"bound":"64000"}}}
            """;

    private static final String GOLDEN_NOTICE = """
            {"chat_id":"chat-7","turn_id":"t-5","kind":"notice","status":"complete","untrusted":false,
             "text":"","refs":[],
             "lines":[{"key":"control.chat.line.task_done","args":{"task_id":"TASK-3"},
                       "refs":[{"kind":"task","id":"TASK-3"}],"at":"2026-11-10T18:03:30Z"}]}
            """;

    private static final String GOLDEN_PROPOSAL = """
            {"chat_id":"chat-7","turn_id":"t-3","reply_turn_id":"t-2","kind":"revision","state":"refused",
             "finding":{"key":"control.chat.proposal.refused","args":{"check":"overlap"}}}
            """;

    private static final String GOLDEN_ENDED = """
            {"chat_id":"chat-7","ended_by":"operator","channel":"web","device":"iPad"}
            """;

    private static final String GOLDEN_REVISION_QUEUED = """
            {"plan_id":"fix-relay","queued_revision":{"chat_id":"chat-7","turn_id":"t-3",
             "subject":{"kind":"plan","id":"fix-relay"},"chat_open":true,
             "applies":{"key":"control.chat.applies.after_task","args":{"task_id":"TASK-3"}}}}
            """;

    private static final String GOLDEN_REVISION_LEFT = """
            {"plan_id":"fix-relay"}
            """;

    private static String id() {
        return Generators.nonBlankStrings().next();
    }

    private static boolean flag() {
        return Generators.booleans().next();
    }

    private static MessageText message(String key) {
        return new MessageText(key, Map.of("id", id()));
    }

    private static ChatContext context() {
        return new ChatContext(List.of(message("control.chat.knows.plan")), id());
    }

    private static Map<String, Object> with(Map<String, Object> map, String key, Object value) {
        var copy = new LinkedHashMap<>(map);
        copy.put(key, value);
        return copy;
    }

    private static Map<String, Object> without(Map<String, Object> map, String key) {
        var copy = new LinkedHashMap<>(map);
        copy.remove(key);
        return copy;
    }

    private static ChatTurnCompletedEvent completed(TurnKind kind, TurnStatus status) {
        var reason = status == TurnStatus.FAILED ? message("control.chat.reason.lost") : null;
        var lines = kind == TurnKind.NOTICE
                ? List.of(new NarrativeLine("control.chat.line.task_done", Map.of("task_id", id()),
                List.of(TurnRef.of(RefKind.TASK, id())), id()))
                : List.<NarrativeLine>of();
        return new ChatTurnCompletedEvent(id(), id(), kind, status, flag(), id(),
                List.of(TurnRef.of(RefKind.FILE, id())), reason, lines);
    }

    private static ChatProposalEvent proposal(ProposalState state) {
        var finding = state == ProposalState.REFUSED ? message("control.chat.proposal.refused") : null;
        return new ChatProposalEvent(id(), id(), id(), ProposalKind.REVISION, state, finding);
    }

    static Stream<Arguments> goldenPayloads() {
        return Stream.of(
                Arguments.of("chat.turn.started, the operator's message", GOLDEN_OPERATOR_STARTED,
                        (Codec) json -> ChatTurnStartedEvent.fromJson(json).toJson()),
                Arguments.of("chat.turn.started, a role's turn", GOLDEN_ROLE_STARTED,
                        (Codec) json -> ChatTurnStartedEvent.fromJson(json).toJson()),
                Arguments.of("chat.turn.delta", GOLDEN_DELTA,
                        (Codec) json -> ChatTurnDeltaEvent.fromJson(json).toJson()),
                Arguments.of("chat.turn.progress", GOLDEN_PROGRESS,
                        (Codec) json -> ChatTurnProgressEvent.fromJson(json).toJson()),
                Arguments.of("chat.turn.completed, a message", GOLDEN_COMPLETED,
                        (Codec) json -> ChatTurnCompletedEvent.fromJson(json).toJson()),
                Arguments.of("chat.turn.completed, failed with its reason", GOLDEN_FAILED,
                        (Codec) json -> ChatTurnCompletedEvent.fromJson(json).toJson()),
                Arguments.of("chat.turn.completed, a notice with lines", GOLDEN_NOTICE,
                        (Codec) json -> ChatTurnCompletedEvent.fromJson(json).toJson()),
                Arguments.of("chat.proposal, refused with its finding", GOLDEN_PROPOSAL,
                        (Codec) json -> ChatProposalEvent.fromJson(json).toJson()),
                Arguments.of("chat.ended by the operator", GOLDEN_ENDED,
                        (Codec) json -> ChatEndedEvent.fromJson(json).toJson()),
                Arguments.of("plan.revision_queued, queued", GOLDEN_REVISION_QUEUED,
                        (Codec) json -> PlanRevisionQueuedEvent.fromJson(json).toJson()),
                Arguments.of("plan.revision_queued, left the queue", GOLDEN_REVISION_LEFT,
                        (Codec) json -> PlanRevisionQueuedEvent.fromJson(json).toJson()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenPayloads")
    @DisplayName("the golden payload of the catalogue reads and writes back unchanged")
    void goldenPayloadRoundTrips(String name, String golden, Codec codec) throws Exception {
        var written = codec.roundTrip(golden);

        assertEquals(JsonTree.parse(golden), JsonTree.parse(written), name);
    }

    @Test
    @DisplayName("each payload names its own catalogue event")
    void eventNames() {
        assertAll(
                () -> assertEquals(ChatTurnStartedEvent.EVENT_NAME, EventName.CHAT_TURN_STARTED.wire()),
                () -> assertEquals(ChatTurnDeltaEvent.EVENT_NAME, EventName.CHAT_TURN_DELTA.wire()),
                () -> assertEquals(ChatTurnProgressEvent.EVENT_NAME, EventName.CHAT_TURN_PROGRESS.wire()),
                () -> assertEquals(ChatTurnCompletedEvent.EVENT_NAME, EventName.CHAT_TURN_COMPLETED.wire()),
                () -> assertEquals(ChatProposalEvent.EVENT_NAME, EventName.CHAT_PROPOSAL.wire()),
                () -> assertEquals(ChatEndedEvent.EVENT_NAME, EventName.CHAT_ENDED.wire()),
                () -> assertEquals(PlanRevisionQueuedEvent.EVENT_NAME, EventName.PLAN_REVISION_QUEUED.wire()));
    }

    @Nested
    @DisplayName("chat.turn.started")
    class Started {

        @Test
        @DisplayName("the operator's message and a role's turn round-trip, each with its own members")
        void roundTrips() throws Exception {
            var operator = new ChatTurnStartedEvent(id(), id(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR,
                    TurnFormat.PLAIN, flag(), null, null, id());
            var role = new ChatTurnStartedEvent(id(), id(), TurnKind.MESSAGE, id(), TurnFormat.MARKDOWN, false,
                    id(), context(), null);
            var roleWithoutModel = new ChatTurnStartedEvent(id(), id(), TurnKind.MESSAGE, id(),
                    TurnFormat.PLAIN, flag(), null, context(), null);

            assertAll(
                    () -> assertEquals(operator, ChatTurnStartedEvent.fromJson(operator.toJson())),
                    () -> assertEquals(role, ChatTurnStartedEvent.fromJson(role.toJson())),
                    () -> assertEquals(roleWithoutModel, ChatTurnStartedEvent.fromJson(roleWithoutModel.toJson())),
                    () -> assertTrue(operator.isOperator()),
                    () -> assertFalse(role.isOperator()),
                    () -> assertFalse(operator.toMap().containsKey("context")),
                    () -> assertFalse(role.toMap().containsKey("text")),
                    () -> assertFalse(roleWithoutModel.toMap().containsKey("model")));
        }

        @Test
        @DisplayName("a kind other than message and members against the author are refused")
        void refusals() throws Exception {
            var operator = ChatTurnStartedEvent.fromJson(GOLDEN_OPERATOR_STARTED).toMap();
            var role = ChatTurnStartedEvent.fromJson(GOLDEN_ROLE_STARTED).toMap();

            assertAll(
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnStartedEvent.fromMap(with(operator, "kind", "notice"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnStartedEvent.fromMap(without(operator, "text"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnStartedEvent.fromMap(with(operator, "model", id()))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnStartedEvent.fromMap(without(role, "context"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnStartedEvent.fromMap(with(role, "text", id()))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnStartedEvent.fromMap(without(role, "untrusted"))),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurnStartedEvent(id(), " ", TurnKind.MESSAGE, id(), TurnFormat.PLAIN,
                                    false, null, context(), null)));
        }

        @Test
        @DisplayName("the operator's message and an untrusted turn are plain; markdown is refused")
        void alwaysPlain() throws Exception {
            var operator = ChatTurnStartedEvent.fromJson(GOLDEN_OPERATOR_STARTED).toMap();
            var untrustedRole = with(ChatTurnStartedEvent.fromJson(GOLDEN_ROLE_STARTED).toMap(), "untrusted", true);
            var untrustedPlain = new ChatTurnStartedEvent(id(), id(), TurnKind.MESSAGE, id(), TurnFormat.PLAIN, true,
                    id(), context(), null);

            assertAll(
                    () -> assertEquals(untrustedPlain, ChatTurnStartedEvent.fromJson(untrustedPlain.toJson())),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurnStartedEvent(id(), id(), TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR,
                                    TurnFormat.MARKDOWN, false, null, null, id())),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurnStartedEvent(id(), id(), TurnKind.MESSAGE, id(), TurnFormat.MARKDOWN,
                                    true, id(), context(), null)),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnStartedEvent.fromMap(with(operator, "format", "markdown"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnStartedEvent.fromMap(untrustedRole)),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnStartedEvent.fromJson(JsonTree.write(untrustedRole))));
        }
    }

    @Nested
    @DisplayName("chat.turn.delta and chat.turn.progress")
    class DeltaAndProgress {

        @Test
        @DisplayName("a piece of text, an empty piece and a step round-trip")
        void roundTrips() throws Exception {
            var delta = new ChatTurnDeltaEvent(id(), id(), id());
            var empty = new ChatTurnDeltaEvent(id(), id(), "");
            var progress = new ChatTurnProgressEvent(id(), id(), message("control.chat.step.reading"));
            var bare = new ChatTurnProgressEvent(id(), id(), MessageText.of("control.chat.step.writing"));

            assertAll(
                    () -> assertEquals(delta, ChatTurnDeltaEvent.fromJson(delta.toJson())),
                    () -> assertEquals(empty, ChatTurnDeltaEvent.fromJson(empty.toJson())),
                    () -> assertEquals(progress, ChatTurnProgressEvent.fromJson(progress.toJson())),
                    () -> assertEquals(bare, ChatTurnProgressEvent.fromJson(bare.toJson())));
        }

        @Test
        @DisplayName("a missing text or step, a blank turn id and an unknown member are refused")
        void refusals() throws Exception {
            var delta = ChatTurnDeltaEvent.fromJson(GOLDEN_DELTA).toMap();
            var progress = ChatTurnProgressEvent.fromJson(GOLDEN_PROGRESS).toMap();

            assertAll(
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnDeltaEvent.fromMap(without(delta, "text"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnDeltaEvent.fromMap(with(delta, "turn_id", " "))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnProgressEvent.fromMap(without(progress, "step"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnProgressEvent.fromMap(with(progress, "extra", id()))),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurnDeltaEvent(" ", id(), id())));
        }
    }

    @Nested
    @DisplayName("chat.turn.completed")
    class Completed {

        @Test
        @DisplayName("a message and a notice in each completed status round-trip")
        void eachKindAndStatus() throws Exception {
            for (var kind : List.of(TurnKind.MESSAGE, TurnKind.NOTICE)) {
                for (var status : List.of(TurnStatus.COMPLETE, TurnStatus.STOPPED, TurnStatus.FAILED)) {
                    var event = completed(kind, status);
                    assertEquals(event, ChatTurnCompletedEvent.fromJson(event.toJson()), kind + "/" + status);
                }
            }
        }

        @Test
        @DisplayName("absent text and refs read as empty; absent lines and reason are not written")
        void absentMembers() throws Exception {
            var event = new ChatTurnCompletedEvent(id(), id(), TurnKind.MESSAGE, TurnStatus.STOPPED, flag(), null,
                    null, null, null);
            var map = event.toMap();

            assertAll(
                    () -> assertEquals("", event.text()),
                    () -> assertEquals(List.of(), event.refs()),
                    () -> assertFalse(map.containsKey("reason")),
                    () -> assertFalse(map.containsKey("lines")),
                    () -> assertEquals(event, ChatTurnCompletedEvent.fromMap(without(map, "refs"))));
        }

        @Test
        @DisplayName("a failed turn without reason is refused, and so is a reason on any other status")
        void reasonIsTiedToFailed() throws Exception {
            var failed = ChatTurnCompletedEvent.fromJson(GOLDEN_FAILED).toMap();
            var complete = ChatTurnCompletedEvent.fromJson(GOLDEN_COMPLETED).toMap();

            assertAll(
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnCompletedEvent.fromMap(without(failed, "reason"))),
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurnCompletedEvent.fromMap(
                            with(complete, "reason", MessageText.of("control.chat.reason.lost").toMap()))),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatTurnCompletedEvent(id(), id(), TurnKind.MESSAGE, TurnStatus.FAILED,
                                    false, id(), null, null, null)));
        }

        @Test
        @DisplayName("lines on a message, a proposal kind and the streaming status are refused")
        void kindAndStatusRefusals() throws Exception {
            var complete = ChatTurnCompletedEvent.fromJson(GOLDEN_COMPLETED).toMap();
            var notice = ChatTurnCompletedEvent.fromJson(GOLDEN_NOTICE).toMap();

            assertAll(
                    () -> assertThrows(JsonFormatException.class, () -> ChatTurnCompletedEvent.fromMap(
                            with(complete, "lines", notice.get("lines")))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnCompletedEvent.fromMap(with(complete, "kind", "proposal"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnCompletedEvent.fromMap(with(complete, "status", "streaming"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnCompletedEvent.fromMap(without(complete, "text"))));
        }
    }

    @Nested
    @DisplayName("chat.proposal")
    class Proposal {

        @Test
        @DisplayName("a proposal in each state round-trips, a refused one with its finding")
        void eachState() throws Exception {
            for (var state : ProposalState.values()) {
                var event = proposal(state);
                assertEquals(event, ChatProposalEvent.fromJson(event.toJson()), state.wireValue());
                assertEquals(state == ProposalState.REFUSED, event.toMap().containsKey("finding"), state.wireValue());
            }
        }

        @Test
        @DisplayName("a finding is tied to refused, and an unknown kind or state is refused")
        void refusals() throws Exception {
            var refused = ChatProposalEvent.fromJson(GOLDEN_PROPOSAL).toMap();

            assertAll(
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatProposalEvent.fromMap(without(refused, "finding"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatProposalEvent.fromMap(with(refused, "state", "open"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatProposalEvent.fromMap(with(refused, "kind", "zz_" + id()))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatProposalEvent.fromMap(without(refused, "reply_turn_id"))));
        }
    }

    @Nested
    @DisplayName("chat.ended")
    class Ended {

        @Test
        @DisplayName("ended_by is the closed catalogue list, and a chat ended each way round-trips")
        void eachEndedBy() throws Exception {
            assertEquals(List.of("operator", "proposal_carried", "answered_elsewhere", "subject_ended",
                    "entry_closed"), ChatEndedEvent.EndedBy.wireValues());
            for (var endedBy : ChatEndedEvent.EndedBy.values()) {
                var event = new ChatEndedEvent(id(), endedBy, null, null);
                assertEquals(event, ChatEndedEvent.fromJson(event.toJson()), endedBy.wireValue());
                assertEquals(endedBy, ChatEndedEvent.EndedBy.fromWire(endedBy.wireValue()));
            }
            var byOperator = new ChatEndedEvent(id(), ChatEndedEvent.EndedBy.OPERATOR, id(), id());

            assertEquals(byOperator, ChatEndedEvent.fromJson(byOperator.toJson()));
        }

        @Test
        @DisplayName("an unknown ended_by is refused, and only the operator's end carries a channel")
        void refusals() throws Exception {
            var ended = ChatEndedEvent.fromJson(GOLDEN_ENDED).toMap();
            var unknown = "zz_" + Generators.letterStrings(3, 10).next();

            assertAll(
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatEndedEvent.fromMap(with(ended, "ended_by", unknown))),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> ChatEndedEvent.EndedBy.fromWire(unknown)),
                    () -> assertNull(ChatEndedEvent.EndedBy.fromWireOrNull(unknown)),
                    () -> assertNull(ChatEndedEvent.EndedBy.fromWireOrNull(null)),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatEndedEvent.fromMap(with(ended, "ended_by", "subject_ended"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatEndedEvent.fromMap(without(ended, "ended_by"))),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new ChatEndedEvent(id(), ChatEndedEvent.EndedBy.ENTRY_CLOSED, null, id())));
        }
    }

    @Nested
    @DisplayName("plan.revision_queued")
    class RevisionQueued {

        @Test
        @DisplayName("a plan with and without a queued revision round-trips; an absent one is not written")
        void roundTrips() throws Exception {
            var revision = new QueuedRevision(id(), id(), new ChatSubject(ChatSubjectKind.DECISION, id()), flag(),
                    MessageText.of("control.chat.applies.on_start"));
            var queued = new PlanRevisionQueuedEvent(id(), revision);
            var left = new PlanRevisionQueuedEvent(id(), null);

            assertAll(
                    () -> assertEquals(queued, PlanRevisionQueuedEvent.fromJson(queued.toJson())),
                    () -> assertEquals(left, PlanRevisionQueuedEvent.fromJson(left.toJson())),
                    () -> assertEquals(List.of("plan_id"), List.copyOf(left.toMap().keySet())));
        }

        @Test
        @DisplayName("a missing or blank plan id, a malformed revision and an unknown member are refused")
        void refusals() throws Exception {
            var queued = PlanRevisionQueuedEvent.fromJson(GOLDEN_REVISION_QUEUED).toMap();

            assertAll(
                    () -> assertThrows(JsonFormatException.class,
                            () -> PlanRevisionQueuedEvent.fromMap(without(queued, "plan_id"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> PlanRevisionQueuedEvent.fromMap(with(queued, "plan_id", " "))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> PlanRevisionQueuedEvent.fromMap(with(queued, "queued_revision", Map.of()))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> PlanRevisionQueuedEvent.fromMap(with(queued, "extra", id()))),
                    () -> assertThrows(JsonFormatException.class, () -> PlanRevisionQueuedEvent.fromJson("[]")),
                    () -> assertThrows(IllegalArgumentException.class, () -> new PlanRevisionQueuedEvent(" ", null)));
        }
    }

    @Nested
    @DisplayName("Origins and pointers carried by the records")
    class OriginsAndPointers {

        @Test
        @DisplayName("a chat origin round-trips with its kind chat; another kind is refused")
        void chatOrigin() throws Exception {
            var origin = new ChatOrigin(id(), id());
            var map = origin.toMap();

            assertAll(
                    () -> assertEquals(origin, ChatOrigin.fromJson(origin.toJson())),
                    () -> assertEquals("chat", map.get("kind")),
                    () -> assertThrows(JsonFormatException.class, () -> ChatOrigin.fromMap(with(map, "kind", "plan"))),
                    () -> assertThrows(JsonFormatException.class, () -> ChatOrigin.fromMap(without(map, "kind"))),
                    () -> assertThrows(IllegalArgumentException.class, () -> new ChatOrigin(id(), " ")));
        }

        @Test
        @DisplayName("a turn pointer round-trips; a missing turn and an unknown member are refused")
        void turnPointer() throws Exception {
            var pointer = new ChatTurnPointer(id(), id());
            var map = pointer.toMap();

            assertAll(
                    () -> assertEquals(pointer, ChatTurnPointer.fromJson(pointer.toJson())),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnPointer.fromMap(without(map, "turn_id"))),
                    () -> assertThrows(JsonFormatException.class,
                            () -> ChatTurnPointer.fromMap(with(map, "kind", "chat"))),
                    () -> assertThrows(IllegalArgumentException.class, () -> new ChatTurnPointer(" ", id())));
        }
    }

    @Test
    @DisplayName("a payload that is not a JSON object is refused by every event")
    void notAnObject() {
        assertAll(
                () -> assertThrows(JsonFormatException.class, () -> ChatTurnStartedEvent.fromJson("[]")),
                () -> assertThrows(JsonFormatException.class, () -> ChatTurnDeltaEvent.fromJson("[]")),
                () -> assertThrows(JsonFormatException.class, () -> ChatTurnProgressEvent.fromJson("[]")),
                () -> assertThrows(JsonFormatException.class, () -> ChatTurnCompletedEvent.fromJson("[]")),
                () -> assertThrows(JsonFormatException.class, () -> ChatProposalEvent.fromJson("[]")),
                () -> assertThrows(JsonFormatException.class, () -> ChatEndedEvent.fromJson("[]")));
    }
}
