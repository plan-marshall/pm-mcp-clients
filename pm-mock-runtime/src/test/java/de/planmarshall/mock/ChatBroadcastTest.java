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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatEndedEvent;
import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatProposalEvent;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnCompletedEvent;
import de.planmarshall.api.chat.ChatTurnDeltaEvent;
import de.planmarshall.api.chat.ChatTurnProgressEvent;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ChatTurnStartedEvent;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.TurnFormat;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.event.EventName;
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
 * A second device played through {@code POST /mock/broadcast}: a chat event names its chat by
 * {@code chat_id}, is applied to the chat and reaches the stream of the chat's subject — an operator's
 * message, an accepted proposal and the chat's end — and every payload, a name no chat event has and an
 * act the chat no longer offers is refused.
 */
@DisplayName("ChatBroadcast")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatBroadcastTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String BROADCAST = "/mock/broadcast";
    private static final String CHAT_PREFIX = "chat.";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    /** The refusal of a broadcast {@code chat.*} name no chat event has, followed by the name. */
    private static final String UNKNOWN_CHAT_EVENT = "No chat event is named ";
    private static final String INVALID_ARGUMENTS = "invalid_arguments";
    private static final String ACT_NOT_AVAILABLE = "act_not_available";
    private static final String CONFLICT = "conflict";
    /** The seeded chat of {@code relay-docs}, whose reply is being written. */
    private static final String SEEDED_REPLY_CHAT = "C-0004";
    /** The plan whose seeded chat holds an open proposal. */
    private static final String SEEDED_PROPOSAL_PLAN = "fix-relay";

    /**
     * One chat event as the plan's stream delivered it.
     *
     * @param name    the event's name
     * @param scope   the envelope's scope
     * @param payload the payload, without the envelope's members
     */
    record Seen(String name, Map<String, Object> scope, Map<String, Object> payload) {
    }

    @Test
    @DisplayName("a message of another device reaches the plan's stream and joins the transcript")
    void message() throws Exception {
        var planId = id();
        var routes = ChatRoutes.plan(planId);
        try (var runtime = ordered();
             var stream = open(runtime, planId)) {
            var chat = openChat(runtime, routes);
            var turnId = "T-" + Generators.letterStrings(6, 10).next();
            var text = Generators.letterStrings(4, 40).next();
            var started = new ChatTurnStartedEvent(chat.id(), turnId, TurnKind.MESSAGE, ChatTurn.AUTHOR_OPERATOR,
                    TurnFormat.PLAIN, false, null, null, text);

            var broadcast = broadcast(runtime, ChatTurnStartedEvent.EVENT_NAME, started.toMap());

            assertEquals(200, broadcast.status(), broadcast.bodyText());
            var seen = next(stream.events(), chat.id());
            assertOnPlanStream(seen, planId, ChatTurnStartedEvent.EVENT_NAME);
            assertEquals(started, ChatTurnStartedEvent.fromMap(seen.payload()));
            var turn = read(runtime, routes).turns().getLast();
            assertAll("the message of the other device",
                    () -> assertEquals(turnId, turn.turnId()),
                    () -> assertTrue(turn.isOperator()),
                    () -> assertEquals(text, turn.text()));
        }
    }

    @Test
    @DisplayName("a proposal accepted on another device reaches the plan's stream and answers the chat's proposal")
    void acceptedProposal() throws Exception {
        var planId = id();
        var routes = ChatRoutes.plan(planId);
        try (var runtime = ordered()) {
            var chat = openChat(runtime, routes);
            send(runtime, routes);
            send(runtime, routes);
            var proposal = Objects.requireNonNull(read(runtime, routes).proposal());
            assertEquals(ProposalKind.REVISION, proposal.kind());
            var accepted = new ChatProposalEvent(chat.id(), proposal.turnId(), proposal.replyTurnId(),
                    proposal.kind(), ProposalState.ACCEPTED, null);
            // The stream is opened once the replies are written, so the test does not have to drain them.
            try (var stream = open(runtime, planId)) {

                var broadcast = broadcast(runtime, ChatProposalEvent.EVENT_NAME, accepted.toMap());

                assertEquals(200, broadcast.status(), broadcast.bodyText());
                var seen = untilProposal(stream.events(), chat.id(), ProposalState.ACCEPTED);
                assertOnPlanStream(seen, planId, ChatProposalEvent.EVENT_NAME);
                assertEquals(accepted, ChatProposalEvent.fromMap(seen.payload()));
            }
            var answered = read(runtime, routes);
            assertEquals(ProposalState.ACCEPTED, Objects.requireNonNull(answered.proposal()).state());
            assertEquals(ChatStatus.TURN, answered.state());
            var again = post(runtime, routes.accept(), new ProposalAnswerRequest(proposal.turnId()).toJson());
            assertEquals(409, again.status(), "the proposal is answered for good");
        }
    }

    @Test
    @DisplayName("the end on another device reaches the plan's stream and ends the chat")
    void end() throws Exception {
        var planId = id();
        var routes = ChatRoutes.plan(planId);
        try (var runtime = ordered();
             var stream = open(runtime, planId)) {
            var chat = openChat(runtime, routes);
            var ended = new ChatEndedEvent(chat.id(), ChatEndedEvent.EndedBy.OPERATOR, "web",
                    Generators.letterStrings(4, 12).next());

            var broadcast = broadcast(runtime, ChatEndedEvent.EVENT_NAME, ended.toMap());

            assertEquals(200, broadcast.status(), broadcast.bodyText());
            var seen = next(stream.events(), chat.id());
            assertOnPlanStream(seen, planId, ChatEndedEvent.EVENT_NAME);
            assertEquals(ended, ChatEndedEvent.fromMap(seen.payload()));
            assertEquals(ChatStatus.END, read(runtime, routes).state());
            assertEquals(409, post(runtime, routes.end(), "").status(), "the chat has ended");
        }
    }

    @Test
    @DisplayName("refuses a chat nobody opened and a payload its event's record refuses")
    void refuses() throws Exception {
        try (var runtime = ordered()) {
            var chat = openChat(runtime, ChatRoutes.plan(id()));
            var unknown = new ChatEndedEvent("C-" + Generators.letterStrings(6, 10).next(),
                    ChatEndedEvent.EndedBy.OPERATOR, null, null);

            assertRefused(broadcast(runtime, ChatEndedEvent.EVENT_NAME, unknown.toMap()), 404, "target_not_found");
            assertRefused(broadcast(runtime, ChatEndedEvent.EVENT_NAME,
                    Map.of(ChatEndedEvent.FIELD_CHAT_ID, chat.id(), ChatEndedEvent.FIELD_ENDED_BY,
                            Generators.letterStrings(6, 10).next())), 400, INVALID_ARGUMENTS);
        }
    }

    static Stream<String> relayedEvents() {
        return Stream.of(ChatTurnProgressEvent.EVENT_NAME, ChatTurnDeltaEvent.EVENT_NAME,
                ChatTurnCompletedEvent.EVENT_NAME);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("relayedEvents")
    @DisplayName("refuses a relayed chat event whose payload its record refuses")
    void refusesInvalidRelayedEvent(String event) throws Exception {
        try (var runtime = ordered()) {
            var chat = openChat(runtime, ChatRoutes.plan(id()));

            assertRefused(broadcast(runtime, event, Map.of(ChatEndedEvent.FIELD_CHAT_ID, chat.id())), 400,
                    INVALID_ARGUMENTS);
        }
    }

    @Test
    @DisplayName("relays a valid chat.turn.delta and refuses a chat event name the catalogue does not have")
    void refusesUnknownChatEvent() throws Exception {
        try (var runtime = ordered()) {
            var chat = openChat(runtime, ChatRoutes.plan(id()));
            var delta = new ChatTurnDeltaEvent(chat.id(), "T-" + Generators.letterStrings(6, 10).next(),
                    Generators.letterStrings(4, 20).next());

            assertEquals(200, broadcast(runtime, ChatTurnDeltaEvent.EVENT_NAME, delta.toMap()).status());
            var unknown = CHAT_PREFIX + Generators.letterStrings(6, 10).next().toLowerCase();
            var refused = broadcast(runtime, unknown, Map.of(ChatEndedEvent.FIELD_CHAT_ID, chat.id()));
            assertRefused(refused, 400, INVALID_ARGUMENTS);
            assertEquals(UNKNOWN_CHAT_EVENT + unknown,
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(refused.body())), MESSAGE));
        }
    }

    @Test
    @DisplayName("reads every chat event of the catalogue through its record, none as a name no chat event has")
    void readsEveryCatalogueChatEvent() throws Exception {
        var chatEvents = Arrays.stream(EventName.values()).filter(name -> name.wire().startsWith(CHAT_PREFIX))
                .toList();
        assertFalse(chatEvents.isEmpty(), "the catalogue has chat events");
        try (var runtime = ordered()) {
            var chat = openChat(runtime, ChatRoutes.plan(id()));
            var payload = Map.<String, Object>of(ChatEndedEvent.FIELD_CHAT_ID, chat.id());

            var messages = new LinkedHashMap<EventName, String>();
            for (var name : chatEvents) {
                var response = broadcast(runtime, name.wire(), payload);
                messages.put(name, String.valueOf(
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), MESSAGE)));
            }

            assertAll("every chat event of the catalogue has a parser",
                    messages.entrySet().stream().map(entry -> (Executable) () -> assertNotEquals(
                            UNKNOWN_CHAT_EVENT + entry.getKey().wire(), entry.getValue(),
                            entry.getKey() + " is read as a name no chat event has")));
        }
    }

    @Test
    @DisplayName("refuses a message while the chat has ended or a reply is written, and a turn id that exists")
    void refusesMessage() throws Exception {
        try (var runtime = ordered()) {
            var routes = ChatRoutes.plan(id());
            var chat = openChat(runtime, routes);
            var started = operatorMessage(chat.id());
            assertEquals(200, broadcast(runtime, ChatTurnStartedEvent.EVENT_NAME, started.toMap()).status());

            assertRefused(broadcast(runtime, ChatTurnStartedEvent.EVENT_NAME, started.toMap()), 409, CONFLICT);
            assertRefused(broadcast(runtime, ChatTurnStartedEvent.EVENT_NAME,
                    operatorMessage(SEEDED_REPLY_CHAT).toMap()), 409, ACT_NOT_AVAILABLE);
            assertEquals(200, post(runtime, routes.end(), "").status());
            assertRefused(broadcast(runtime, ChatTurnStartedEvent.EVENT_NAME, operatorMessage(chat.id()).toMap()),
                    409, ACT_NOT_AVAILABLE);
            assertEquals(1, read(runtime, routes).turns().size(), "a refused message records nothing");
        }
    }

    @Test
    @DisplayName("refuses an answer without an open proposal or naming another proposal")
    void refusesAnswer() throws Exception {
        try (var runtime = ordered()) {
            var chat = openChat(runtime, ChatRoutes.plan(id()));
            var seeded = read(runtime, ChatRoutes.plan(SEEDED_PROPOSAL_PLAN));
            var open = Objects.requireNonNull(seeded.proposal());

            assertRefused(broadcast(runtime, ChatProposalEvent.EVENT_NAME, new ChatProposalEvent(chat.id(),
                    "T-" + Generators.letterStrings(6, 10).next(), "T-" + Generators.letterStrings(6, 10).next(),
                    ProposalKind.REVISION,
                    ProposalState.ACCEPTED, null).toMap()), 409, ACT_NOT_AVAILABLE);
            assertRefused(broadcast(runtime, ChatProposalEvent.EVENT_NAME, new ChatProposalEvent(seeded.id(),
                    "T-" + Generators.letterStrings(6, 10).next(), open.replyTurnId(), open.kind(),
                    ProposalState.ACCEPTED, null).toMap()), 409, CONFLICT);
            assertEquals(ProposalState.OPEN,
                    Objects.requireNonNull(read(runtime, ChatRoutes.plan(SEEDED_PROPOSAL_PLAN)).proposal()).state());
        }
    }

    @Test
    @DisplayName("broadcasting ACCEPTED passes through revisionRefusal, never overwrites a queued revision, and refuses missing applies without throwing")
    void broadcastAcceptedRevisionGuards() throws Exception {
        try (var runtime = ordered()) {
            var planRoutes = ChatRoutes.plan(SEEDED_PROPOSAL_PLAN);
            var decisionRoutes = ChatRoutes.planDecision(SEEDED_PROPOSAL_PLAN, "Q-007");
            var planChat = read(runtime, planRoutes);
            var planProposal = Objects.requireNonNull(planChat.proposal());
            var decisionChat = read(runtime, decisionRoutes);
            var decisionProposal = Objects.requireNonNull(decisionChat.proposal());

            // 1. Accept decision chat's revision so fix-relay has a queued revision.
            assertEquals(200, post(runtime, decisionRoutes.accept(),
                    new ProposalAnswerRequest(decisionProposal.turnId()).toJson()).status());

            // 2. Broadcast ACCEPTED on plan chat: refused conflict, does NOT overwrite queued revision.
            var conflict = broadcast(runtime, ChatProposalEvent.EVENT_NAME, new ChatProposalEvent(planChat.id(),
                    planProposal.turnId(), planProposal.replyTurnId(), ProposalKind.REVISION,
                    ProposalState.ACCEPTED, null).toMap());
            var process = JsonTree.asObject(JsonTree.parse(new UnixSocketHttpClient(runtime.socketPath())
                    .send(HttpRequest.get("/api/v1/plans/" + SEEDED_PROPOSAL_PLAN + "/process")
                            .withHeader(AUTH_HEADER, "Bearer " + runtime.token())).body()));
            var queued = JsonTree.object(process, "queued_revision");
            assertAll("conflict does not overwrite queued revision",
                    () -> assertEquals(409, conflict.status(), conflict.bodyText()),
                    () -> assertEquals(CONFLICT, JsonTree.string(JsonTree.asObject(JsonTree.parse(conflict.body())), ERROR)),
                    () -> assertEquals(decisionChat.id(), JsonTree.string(Objects.requireNonNull(queued), "chat_id")),
                    () -> assertEquals(ProposalState.OPEN,
                            Objects.requireNonNull(read(runtime, planRoutes).proposal()).state()));

            // 3. After queued revision applies, plan moved to version 2: broadcast ACCEPTED on stale plan proposal is refused.
            assertEquals(200, broadcast(runtime, "task.ended",
                    Map.of("plan_id", SEEDED_PROPOSAL_PLAN, "task_id", "TASK-003", "status", "done")).status());
            var stale = broadcast(runtime, ChatProposalEvent.EVENT_NAME, new ChatProposalEvent(planChat.id(),
                    planProposal.turnId(), planProposal.replyTurnId(), ProposalKind.REVISION,
                    ProposalState.ACCEPTED, null).toMap());
            assertAll("stale version refused",
                    () -> assertEquals(409, stale.status(), stale.bodyText()),
                    () -> assertEquals(CONFLICT, JsonTree.string(JsonTree.asObject(JsonTree.parse(stale.body())), ERROR)),
                    () -> assertEquals(ProposalState.OPEN,
                            Objects.requireNonNull(read(runtime, planRoutes).proposal()).state()));
        }

        // 4. Broadcast ACCEPTED on a revision proposal with null applies: refused invalid_arguments (no uncaught NPE).
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                     .initialScenario(withoutAppliesOnFixRelayProposal()).timing(MockTiming.ORDERED).build())) {
            var planRoutes = ChatRoutes.plan(SEEDED_PROPOSAL_PLAN);
            var chat = read(runtime, planRoutes);
            var proposal = Objects.requireNonNull(chat.proposal());

            var missingApplies = broadcast(runtime, ChatProposalEvent.EVENT_NAME, new ChatProposalEvent(chat.id(),
                    proposal.turnId(), proposal.replyTurnId(), ProposalKind.REVISION, ProposalState.ACCEPTED, null)
                    .toMap());
            assertRefused(missingApplies, 400, INVALID_ARGUMENTS);
        }

        // 5. Broadcast REJECTED on a REFUSED proposal moves it cleanly to REJECTED.
        try (var runtime = ordered()) {
            var routes = ChatRoutes.plan(id());
            var opened = openChat(runtime, routes);
            send(runtime, routes);
            send(runtime, routes);
            var proposal = Objects.requireNonNull(read(runtime, routes).proposal());
            assertEquals(200, broadcast(runtime, "task.ended",
                    Map.of("plan_id", opened.subject().id(), "task_id", "TASK-4", "status", "done")).status());
            assertEquals(ProposalState.REFUSED,
                    Objects.requireNonNull(ChatResource.fromJson(post(runtime, routes.accept(),
                            new ProposalAnswerRequest(proposal.turnId()).toJson()).bodyText()).proposal()).state());

            var rejected = broadcast(runtime, ChatProposalEvent.EVENT_NAME, new ChatProposalEvent(opened.id(),
                    proposal.turnId(), proposal.replyTurnId(), ProposalKind.REVISION, ProposalState.REJECTED, null)
                    .toMap());
            assertEquals(200, rejected.status(), rejected.bodyText());
            assertEquals(ProposalState.REJECTED,
                    Objects.requireNonNull(read(runtime, routes).proposal()).state());
        }
    }

    @SuppressWarnings("unchecked")
    private static Scenario withoutAppliesOnFixRelayProposal() {
        var base = new ScenarioRegistry().resolve(StandardScenarios.RUNNING_WORK);
        var stateId = base.start();
        var state = Objects.requireNonNull(base.state(stateId));
        var representation = (Map<String, Object>) copyValue(state.representation());
        var control = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) representation
                .get("facts")).get("runtime")).get("control");
        var chats = (List<Object>) control.get("chats");
        var fixRelay = (Map<String, Object>) chats.getFirst();
        ((Map<String, Object>) fixRelay.get("proposal")).remove(ChatProposal.FIELD_APPLIES);
        var states = new LinkedHashMap<>(base.states());
        states.put(stateId, new StateDefinition(representation, state.links(), state.events(), state.auto(),
                state.chatFailures()));
        return new Scenario(base.formatVersion(), base.name(), base.description(), stateId, base.pairingCode(),
                base.routes(), states);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            map.forEach((key, member) -> copy.put(String.valueOf(key), copyValue(member)));
            return copy;
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list.stream().map(ChatBroadcastTest::copyValue).toList());
        }
        return value;
    }

    @Test
    @DisplayName("refuses the end of a chat that has ended")
    void refusesSecondEnd() throws Exception {
        try (var runtime = ordered()) {
            var routes = ChatRoutes.plan(id());
            var chat = openChat(runtime, routes);
            assertEquals(200, post(runtime, routes.end(), "").status());

            assertRefused(broadcast(runtime, ChatEndedEvent.EVENT_NAME,
                    new ChatEndedEvent(chat.id(), ChatEndedEvent.EndedBy.OPERATOR, null, null).toMap()), 409,
                    ACT_NOT_AVAILABLE);
        }
    }

    private static ChatTurnStartedEvent operatorMessage(String chatId) {
        return new ChatTurnStartedEvent(chatId, "T-" + Generators.letterStrings(6, 10).next(), TurnKind.MESSAGE,
                ChatTurn.AUTHOR_OPERATOR, TurnFormat.PLAIN, false, null, null,
                Generators.letterStrings(4, 40).next());
    }

    private static void assertOnPlanStream(Seen seen, String planId, String name) {
        assertAll("an event of the plan's chat",
                () -> assertEquals(name, seen.name()),
                () -> assertEquals("plan", JsonTree.string(seen.scope(), "kind")),
                () -> assertEquals(planId, JsonTree.string(seen.scope(), "id")));
    }

    private static void assertRefused(HttpResponse response, int status, String error) throws Exception {
        assertEquals(status, response.status(), response.bodyText());
        assertEquals(error, JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), ERROR));
    }

    /**
     * Reads the chat's events, the reply's among them, until {@code chat.proposal} names the state.
     */
    private static Seen untilProposal(SseReader events, String chatId, ProposalState state) throws IOException {
        Seen seen;
        do {
            seen = next(events, chatId);
        } while (!(ChatProposalEvent.EVENT_NAME.equals(seen.name())
                && state.wireValue().equals(JsonTree.string(seen.payload(), ChatProposalEvent.FIELD_STATE))));
        return seen;
    }

    private static Seen next(SseReader events, String chatId) throws IOException {
        while (true) {
            var event = events.next();
            assertNotNull(event, "the stream ended");
            if (!event.event().startsWith(CHAT_PREFIX)) {
                continue;
            }
            var data = new LinkedHashMap<>(JsonTree.asObject(JsonTree.parse(event.data())));
            var scope = JsonTree.object(data, "scope");
            data.remove("event");
            data.remove("at");
            data.remove("scope");
            if (chatId.equals(JsonTree.string(data, ChatEndedEvent.FIELD_CHAT_ID))) {
                return new Seen(event.event(), scope, data);
            }
        }
    }

    private static HttpResponse broadcast(MockRuntime runtime, String event, Map<String, Object> data)
            throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", BROADCAST, JsonTree.write(Map.of("event", event, "data", data))));
    }

    private static MockRuntime ordered() throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(CHAT_STATE).timing(MockTiming.ORDERED).build());
    }

    private static String id() {
        return Generators.letterStrings(4, 10).next().toLowerCase();
    }

    private static ChatResource openChat(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var opened = post(runtime, routes.chat(), "");
        assertEquals(201, opened.status(), opened.bodyText());
        return ChatResource.fromJson(opened.bodyText());
    }

    private static void send(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var message = new ChatTurnRequest(Generators.letterStrings(4, 40).next(),
                Generators.letterStrings(12, 16).next());
        assertEquals(ChatTurnAccepted.HTTP_STATUS, post(runtime, routes.turns(), message.toJson()).status());
    }

    private static ChatResource read(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var response = new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(routes.chat()).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
        return ChatResource.fromJson(response.bodyText());
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpExchange open(MockRuntime runtime, String planId) throws IOException {
        var exchange = new UnixSocketHttpClient(runtime.socketPath())
                .open(HttpRequest.get("/api/v1/plans/" + planId + "/events")
                        .withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertTrue(exchange.isEventStream());
        return exchange;
    }
}
