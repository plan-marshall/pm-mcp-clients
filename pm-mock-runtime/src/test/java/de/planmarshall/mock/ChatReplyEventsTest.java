/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatEndedEvent;
import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnCompletedEvent;
import de.planmarshall.api.chat.ChatTurnDeltaEvent;
import de.planmarshall.api.chat.ChatTurnProgressEvent;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ChatTurnStartedEvent;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.SseReader;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

/**
 * The reply of a chat played as events: {@code chat.turn.started} with the model, a
 * {@code chat.turn.progress} per step, the text as {@code chat.turn.delta} pieces and
 * {@code chat.turn.completed} with the whole text, on the stream of the chat's subject and on
 * {@code /api/v1/events}. Ordered timing sends them at once and in order, realistic timing spaces the
 * pieces at a reading pace; a stop ends the reply with its text so far, a replaced worker restarts it
 * from the working line, and {@code chat.ended} names each of its five causes.
 */
@DisplayName("ChatReplyEvents")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatReplyEventsTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String CHAT_EVENT_PREFIX = "chat.";
    private static final String RUNTIME_EVENTS = "/api/v1/events";
    private static final String REPLAN = "replan";
    private static final String KIND = "kind";
    private static final String ID = "id";

    /**
     * Half the reading pace of the realistic timing (80 ms a piece): the floor the spacing of the
     * pieces is measured against, so the scheduler's jitter does not decide the test.
     */
    private static final long HALF_READING_PACE_MS = 40;

    /**
     * One chat event as a stream delivered it.
     *
     * @param name    the event's name
     * @param scope   the envelope's scope
     * @param payload the payload, without the envelope's members
     * @param nanos   when the test read it
     */
    record Seen(String name, Map<String, Object> scope, Map<String, Object> payload, long nanos) {

        String chatId() {
            return Objects.requireNonNullElse(JsonTree.string(payload, ChatEndedEvent.FIELD_CHAT_ID), "");
        }
    }

    @Nested
    @DisplayName("Ordered timing")
    class Ordered {

        @Test
        @DisplayName("sends the operator's message, then the reply's start, steps, pieces and completion in order")
        void playsInOrder() throws Exception {
            var played = play();

            var names = played.sequence().stream().map(Seen::name).toList();
            assertEquals(ChatTurnStartedEvent.EVENT_NAME, names.get(0));
            assertEquals(ChatTurnStartedEvent.EVENT_NAME, names.get(1));
            assertEquals(ChatTurnProgressEvent.EVENT_NAME, names.get(2));
            assertEquals(ChatTurnCompletedEvent.EVENT_NAME, names.getLast());
            var middle = names.subList(3, names.size() - 1);
            assertFalse(middle.isEmpty(), "the reply's text comes in pieces");
            assertTrue(middle.stream().allMatch(ChatTurnDeltaEvent.EVENT_NAME::equals), names::toString);
            assertTrue(played.sequence().stream()
                            .allMatch(seen -> "plan".equals(JsonTree.string(seen.scope(), KIND))
                                    && played.planId().equals(JsonTree.string(seen.scope(), ID))),
                    "every event is on the plan's stream");
            assertEquals(names, played.everywhere().stream().map(Seen::name).toList(),
                    "/api/v1/events carries the same sequence");
        }

        @Test
        @DisplayName("announces the operator's message and the role's reply with the turns the chat records")
        void announcesTheTurns() throws Exception {
            var played = play();

            var sequence = played.sequence();
            var operator = ChatTurnStartedEvent.fromMap(sequence.get(0).payload());
            var reply = ChatTurnStartedEvent.fromMap(sequence.get(1).payload());
            var progress = ChatTurnProgressEvent.fromMap(sequence.get(2).payload());
            var completed = ChatTurnCompletedEvent.fromMap(sequence.getLast().payload());
            assertAll("the turns the events announce",
                    () -> assertTrue(operator.isOperator()),
                    () -> assertEquals(played.operatorTurnId(), operator.turnId()),
                    () -> assertEquals(played.message().text(), operator.text()),
                    () -> assertNull(operator.model()),
                    () -> assertEquals(REPLAN, reply.author()),
                    () -> assertEquals(played.chat().role().model(), reply.model()),
                    () -> assertNotNull(reply.context()),
                    () -> assertNotEquals(operator.turnId(), reply.turnId()),
                    () -> assertEquals(reply.turnId(), progress.turnId()),
                    () -> assertEquals(reply.turnId(), completed.turnId()),
                    () -> assertEquals(TurnStatus.COMPLETE, completed.status()),
                    () -> assertEquals(joinedDeltas(sequence), completed.text()));
            var written = played.written();
            assertEquals(ChatStatus.TURN, written.state());
            assertEquals(completed.text(), written.turns().getLast().text());
            assertEquals(TurnStatus.COMPLETE, written.turns().getLast().status());
        }

        /**
         * Sends one message to a new plan chat under ordered timing and reads the events it plays, on the
         * plan's stream and on {@code /api/v1/events}, and the chat after the reply.
         */
        private Played play() throws Exception {
            var planId = id();
            var routes = ChatRoutes.plan(planId);
            try (var runtime = start(MockTiming.ORDERED);
                 var planStream = open(runtime, "/api/v1/plans/" + planId + "/events");
                 var runtimeStream = open(runtime, RUNTIME_EVENTS)) {
                var chat = openChat(runtime, routes);
                var message = message();
                var accepted = post(runtime, routes.turns(), message.toJson());
                assertEquals(ChatTurnAccepted.HTTP_STATUS, accepted.status(), accepted.bodyText());
                var operatorTurnId = ChatTurnAccepted.fromJson(accepted.bodyText()).turnId();
                var sequence = untilCompleted(planStream.events(), chat.id());
                var everywhere = untilCompleted(runtimeStream.events(), chat.id());
                return new Played(planId, chat, message, operatorTurnId, sequence, everywhere,
                        read(runtime, routes));
            }
        }
    }

    /**
     * What one message to a plan chat played under ordered timing.
     *
     * @param planId         the plan
     * @param chat           the chat as it was opened
     * @param message        the operator's message
     * @param operatorTurnId the turn the message was recorded as
     * @param sequence       the chat's events on the plan's stream, up to the reply's completion
     * @param everywhere     the chat's events on {@code /api/v1/events}, up to the reply's completion
     * @param written        the chat after the reply
     */
    private record Played(String planId, ChatResource chat, ChatTurnRequest message, String operatorTurnId,
    List<Seen> sequence, List<Seen> everywhere, ChatResource written) {
    }

    @Nested
    @DisplayName("Realistic timing")
    class Realistic {

        @Test
        @DisplayName("answers the message before the reply is written and spaces its pieces at a reading pace")
        void spacesThePieces() throws Exception {
            var planId = id();
            var routes = ChatRoutes.plan(planId);
            try (var runtime = start(MockTiming.REALISTIC);
                 var planStream = open(runtime, "/api/v1/plans/" + planId + "/events")) {
                var chat = openChat(runtime, routes);

                assertEquals(ChatTurnAccepted.HTTP_STATUS, post(runtime, routes.turns(), message().toJson()).status());
                var writing = read(runtime, routes);
                assertEquals(ChatStatus.REPLY, writing.state(), "the 202 arrives before the reply is written");
                assertEquals(TurnStatus.STREAMING, writing.turns().getLast().status());

                var sequence = untilCompleted(planStream.events(), chat.id());
                var deltas = sequence.stream().filter(seen -> ChatTurnDeltaEvent.EVENT_NAME.equals(seen.name()))
                        .toList();
                assertTrue(deltas.size() > 1, "the reply comes in more than one piece");
                var span = TimeUnit.NANOSECONDS.toMillis(deltas.getLast().nanos() - deltas.getFirst().nanos());
                assertTrue(span >= (deltas.size() - 1) * HALF_READING_PACE_MS,
                        "the pieces are spaced at a reading pace, " + deltas.size() + " pieces in " + span + " ms");
                assertEquals(joinedDeltas(sequence),
                        ChatTurnCompletedEvent.fromMap(sequence.getLast().payload()).text());
            }
        }

        @Test
        @DisplayName("a stop ends the reply with its text so far")
        void stopKeepsTheTextSoFar() throws Exception {
            var planId = id();
            var routes = ChatRoutes.plan(planId);
            try (var runtime = start(MockTiming.REALISTIC);
                 var planStream = open(runtime, "/api/v1/plans/" + planId + "/events")) {
                var chat = openChat(runtime, routes);
                assertEquals(ChatTurnAccepted.HTTP_STATUS, post(runtime, routes.turns(), message().toJson()).status());
                var events = planStream.events();
                var seen = untilFirstDelta(events, chat.id());
                var replyTurnId = replyStarted(seen).turnId();

                var stopped = post(runtime, routes.stop(replyTurnId), "");
                assertEquals(200, stopped.status(), stopped.bodyText());

                seen.addAll(untilCompleted(events, chat.id()));
                var completed = ChatTurnCompletedEvent.fromMap(seen.getLast().payload());
                assertAll("the stopped reply",
                        () -> assertEquals(replyTurnId, completed.turnId()),
                        () -> assertEquals(TurnStatus.STOPPED, completed.status()),
                        () -> assertFalse(completed.text().isEmpty()),
                        () -> assertEquals(joinedDeltas(seen), completed.text()));
                var afterStop = read(runtime, routes);
                assertEquals(ChatStatus.TURN, afterStop.state());
                assertEquals(TurnStatus.STOPPED, afterStop.turns().getLast().status());
                assertEquals(completed.text(), afterStop.turns().getLast().text());
            }
        }

        @Test
        @DisplayName("a replaced worker announces the reply again and writes it from the working line")
        void replacedWorkerRestarts() throws Exception {
            var planId = id();
            var routes = ChatRoutes.plan(planId);
            try (var runtime = start(MockTiming.REALISTIC);
                 var planStream = open(runtime, "/api/v1/plans/" + planId + "/events")) {
                var chat = openChat(runtime, routes);
                assertEquals(ChatTurnAccepted.HTTP_STATUS, post(runtime, routes.turns(), message().toJson()).status());
                var events = planStream.events();
                var replyTurnId = replyStarted(untilFirstDelta(events, chat.id())).turnId();

                assertTrue(runtime.engine().replaceChatWorker(new ChatSubject(ChatSubjectKind.PLAN, planId)));

                Seen restart;
                do {
                    restart = nextOf(events, chat.id());
                } while (!ChatTurnStartedEvent.EVENT_NAME.equals(restart.name()));
                var restarted = ChatTurnStartedEvent.fromMap(restart.payload());
                assertEquals(replyTurnId, restarted.turnId(), "the same reply is announced again");

                var rest = untilCompleted(events, chat.id());
                assertEquals(ChatTurnProgressEvent.EVENT_NAME, rest.getFirst().name(), "from the working line");
                var completed = ChatTurnCompletedEvent.fromMap(rest.getLast().payload());
                assertEquals(TurnStatus.COMPLETE, completed.status());
                assertEquals(joinedDeltas(rest), completed.text(), "the text is written anew from its start");
                assertEquals(2, read(runtime, routes).turns().size(), "a restart adds no turn");
            }
        }
    }

    @Nested
    @DisplayName("chat.ended")
    class Ended {

        @Test
        @DisplayName("names the operator when the operator ends the chat")
        void operator() throws Exception {
            var routes = ChatRoutes.plan(id());
            try (var runtime = start(MockTiming.ORDERED);
                 var stream = open(runtime, RUNTIME_EVENTS)) {
                var chat = openChat(runtime, routes);

                assertEquals(200, post(runtime, routes.end(), "").status());

                assertEquals(ChatEndedEvent.EndedBy.OPERATOR, endedBy(stream, chat.id()));
            }
        }

        @Test
        @DisplayName("names the decision answered elsewhere")
        void answeredElsewhere() throws Exception {
            var question = "Q-" + Generators.letterStrings(4, 8).next();
            var routes = ChatRoutes.planDecision(id(), question);
            try (var runtime = start(MockTiming.ORDERED);
                 var stream = open(runtime, RUNTIME_EVENTS)) {
                var chat = openChat(runtime, routes);

                runtime.engine().broadcast("decision.answered", Map.of(ID, question));

                assertEquals(ChatEndedEvent.EndedBy.ANSWERED_ELSEWHERE, endedBy(stream, chat.id()));
                assertEquals(ChatStatus.END, read(runtime, routes).state());
            }
        }

        @Test
        @DisplayName("names the subject's end, and a chat opened after it is about the archived record")
        void subjectEnded() throws Exception {
            var planId = id();
            var routes = ChatRoutes.plan(planId);
            try (var runtime = start(MockTiming.ORDERED);
                 var stream = open(runtime, RUNTIME_EVENTS)) {
                var chat = openChat(runtime, routes);

                runtime.engine().broadcast("scope.ended", Map.of("plan_id", planId));

                assertEquals(ChatEndedEvent.EndedBy.SUBJECT_ENDED, endedBy(stream, chat.id()));
                assertEquals(ChatScopeKind.ARCHIVE, openChat(runtime, routes).scope());
            }
        }

        @Test
        @DisplayName("names the entry's close")
        void entryClosed() throws Exception {
            var entryId = "ent-" + id();
            var routes = ChatRoutes.entry(entryId);
            try (var runtime = start(MockTiming.ORDERED);
                 var stream = open(runtime, RUNTIME_EVENTS)) {
                var chat = openChat(runtime, routes);

                runtime.engine().broadcast("entry.abandoned", Map.of("entry_id", entryId));

                assertEquals(ChatEndedEvent.EndedBy.ENTRY_CLOSED, endedBy(stream, chat.id()));
                assertEquals(ChatStatus.END, read(runtime, routes).state());
            }
        }

        @Test
        @DisplayName("names the entry's close when the entry's plan is created")
        void entryClosedByPlanCreate() throws Exception {
            assertEntryClosedByCreate("start.chat", "ent-3f9a2c41", true);
        }

        @Test
        @DisplayName("names the entry's close when the entry's epic is created")
        void entryClosedByEpicCreate() throws Exception {
            assertEntryClosedByCreate("start.epic", "ent-5c1b7d20", false);
        }

        /**
         * Creates the entry's work while its chat is open; the chat a state seeds is read, every other
         * one is opened.
         */
        private void assertEntryClosedByCreate(String stateId, String entryId, boolean seeded) throws Exception {
            var routes = ChatRoutes.entry(entryId);
            try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                         .scenario(StandardScenarios.PROJECTS_AND_START + "#" + stateId)
                         .timing(MockTiming.ORDERED).build());
                 var stream = open(runtime, RUNTIME_EVENTS)) {
                var chat = seeded ? read(runtime, routes) : openChat(runtime, routes);
                assertNotEquals(ChatStatus.END, chat.state(), "the entry's chat is open before the create");

                var created = post(runtime, "/api/v1/entries/" + entryId + "/links/create", "{}");
                assertEquals(200, created.status(), created.bodyText());

                assertEquals(ChatEndedEvent.EndedBy.ENTRY_CLOSED, endedBy(stream, chat.id()));
                assertEquals(ChatStatus.END, read(runtime, routes).state());
            }
        }

        @Test
        @DisplayName("names a decision's carried proposal")
        void proposalCarried() throws Exception {
            var question = "Q-" + Generators.letterStrings(4, 8).next();
            var routes = ChatRoutes.planDecision("fix-relay", question);
            try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                         .initialScenario(withDecisionChat(question)).timing(MockTiming.ORDERED).build());
                 var stream = open(runtime, RUNTIME_EVENTS)) {
                var chat = read(runtime, routes);
                assertEquals(ChatStatus.READ_BACK, chat.state());
                var answer = new ProposalAnswerRequest(Objects.requireNonNull(chat.proposal()).turnId()).toJson();

                var accepted = post(runtime, routes.accept(), answer);
                assertEquals(200, accepted.status(), accepted.bodyText());
                assertEquals(ChatStatus.CARRIED, ChatResource.fromJson(accepted.bodyText()).state());

                assertEquals(ChatEndedEvent.EndedBy.PROPOSAL_CARRIED, endedBy(stream, chat.id()));
            }
        }
    }

    /**
     * The state {@code control.chat} with one more chat: a decision of the plan {@code fix-relay}
     * whose role proposes an answer, built from the plan's own chat with its open proposal.
     */
    private static Scenario withDecisionChat(String question) throws Exception {
        ChatResource source;
        try (var runtime = start(MockTiming.ORDERED)) {
            source = read(runtime, ChatRoutes.plan("fix-relay"));
        }
        var proposal = Objects.requireNonNull(source.proposal());
        var decisionChat = new ChatResource("C-" + id(),
                new ChatSubject(ChatSubjectKind.DECISION, "fix-relay/" + question), source.role(),
                ChatScopeKind.DECISION, ChatStatus.READ_BACK, source.context(),
                new ChatProposal(proposal.turnId(), proposal.replyTurnId(), ProposalKind.ANSWER, proposal.summary(),
                        proposal.applies(), proposal.before(), proposal.after(), proposal.state(), null),
                source.turns(), null);

        var base = new ScenarioRegistry().resolve(CHAT_STATE);
        var state = Objects.requireNonNull(base.state(base.start()));
        var representation = new LinkedHashMap<>(Objects.requireNonNull(JsonTree.asObject(state.representation())));
        // The state's own fixture is merged over the scenario's base one, its control member by member.
        var facts = member(representation, "facts");
        var fixture = member(facts, "runtime");
        var control = member(fixture, "control");
        var chats = new ArrayList<Object>();
        if (control.get("chats") instanceof List<?> seeded) {
            chats.addAll(seeded);
        }
        chats.add(decisionChat.toMap());
        control.put("chats", chats);
        fixture.put("control", control);
        facts.put("runtime", fixture);
        representation.put("facts", facts);
        var states = new LinkedHashMap<>(base.states());
        states.put(base.start(), new StateDefinition(representation, state.links(), state.events(), state.auto()));
        return new Scenario(base.formatVersion(), base.name(), base.description(), base.start(), base.pairingCode(),
                base.routes(), states);
    }

    /**
     * @return a copy of the object member of a map, empty when the map has none
     */
    private static Map<String, Object> member(Map<String, Object> map, String key) {
        var copy = new LinkedHashMap<String, Object>();
        if (map.get(key) instanceof Map<?, ?> value) {
            value.forEach((name, member) -> copy.put(String.valueOf(name), member));
        }
        return copy;
    }

    private static MockRuntime start(MockTiming timing) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(CHAT_STATE).timing(timing).build());
    }

    private static String id() {
        return Generators.letterStrings(4, 10).next().toLowerCase();
    }

    private static ChatTurnRequest message() {
        return new ChatTurnRequest(Generators.letterStrings(4, 40).next(), Generators.letterStrings(12, 16).next());
    }

    private static ChatResource openChat(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var opened = post(runtime, routes.chat(), "");
        assertEquals(201, opened.status(), opened.bodyText());
        return ChatResource.fromJson(opened.bodyText());
    }

    private static ChatResource read(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var response = new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(routes.chat()).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
        return ChatResource.fromJson(response.bodyText());
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpExchange open(MockRuntime runtime, String path) throws IOException {
        var exchange = new UnixSocketHttpClient(runtime.socketPath())
                .open(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertTrue(exchange.isEventStream(), path);
        return exchange;
    }

    /**
     * @return the next event of the chat on the stream; every other event is passed over
     */
    private static Seen nextOf(SseReader events, String chatId) throws IOException {
        while (true) {
            var event = events.next();
            assertNotNull(event, "the stream ended");
            if (!event.event().startsWith(CHAT_EVENT_PREFIX)) {
                continue;
            }
            var data = new LinkedHashMap<>(JsonTree.asObject(JsonTree.parse(event.data())));
            var scope = JsonTree.object(data, "scope");
            data.remove("event");
            data.remove("at");
            data.remove("scope");
            var seen = new Seen(event.event(), scope, data, System.nanoTime());
            if (chatId.equals(seen.chatId())) {
                return seen;
            }
        }
    }

    private static List<Seen> untilCompleted(SseReader events, String chatId) throws IOException {
        var seen = new ArrayList<Seen>();
        Seen next;
        do {
            next = nextOf(events, chatId);
            seen.add(next);
        } while (!ChatTurnCompletedEvent.EVENT_NAME.equals(next.name()));
        return seen;
    }

    private static List<Seen> untilFirstDelta(SseReader events, String chatId) throws IOException {
        var seen = new ArrayList<Seen>();
        Seen next;
        do {
            next = nextOf(events, chatId);
            seen.add(next);
        } while (!ChatTurnDeltaEvent.EVENT_NAME.equals(next.name()));
        return seen;
    }

    private static ChatTurnStartedEvent replyStarted(List<Seen> seen) throws IOException {
        for (var event : seen) {
            if (ChatTurnStartedEvent.EVENT_NAME.equals(event.name())) {
                var started = ChatTurnStartedEvent.fromMap(event.payload());
                if (!started.isOperator()) {
                    return started;
                }
            }
        }
        throw new AssertionError("no reply was announced: " + seen);
    }

    private static String joinedDeltas(List<Seen> seen) {
        return seen.stream().filter(event -> ChatTurnDeltaEvent.EVENT_NAME.equals(event.name()))
                .map(event -> JsonTree.string(event.payload(), ChatTurnDeltaEvent.FIELD_TEXT))
                .collect(Collectors.joining());
    }

    private static ChatEndedEvent.EndedBy endedBy(HttpExchange stream, String chatId) throws IOException {
        var events = stream.events();
        Seen next;
        do {
            next = nextOf(events, chatId);
        } while (!ChatEndedEvent.EVENT_NAME.equals(next.name()));
        return ChatEndedEvent.fromMap(next.payload()).endedBy();
    }
}
