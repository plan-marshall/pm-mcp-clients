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
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatMessageKeys;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.runtime.MockTiming;

/**
 * Every chat of the mock runtime on the routes {@link ChatRoutes} builds, one route test per subject
 * kind: open, read, page, turn ({@code 202}, a repeated {@code client_turn_id}, refused while a reply is
 * written), accept, reject and end.
 */
@DisplayName("ChatScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class ChatScenarioTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String FOLLOW_UP_STATE = StandardScenarios.PROJECTS_AND_START + "#start.follow-up";
    private static final String START_CHAT_STATE = StandardScenarios.PROJECTS_AND_START + "#start.chat";
    private static final String ERROR = "error";
    private static final String ACT_NOT_AVAILABLE = "act_not_available";
    private static final String CONFLICT = "conflict";
    private static final String TARGET_NOT_FOUND = "target_not_found";
    private static final String INVALID_ARGUMENTS = "invalid_arguments";
    private static final String REPLAN = "replan";
    private static final String DECOMPOSE = "decompose";
    private static final String REFINE = "refine";

    /**
     * One subject kind of a chat and what a chat opened about it is.
     *
     * @param name      the subject kind's name in the report
     * @param state     the scenario state the chat is opened in
     * @param routes    the routes of the subject, from a generated id
     * @param kind      the subject's kind
     * @param subjectId the subject's id, from the generated id
     * @param role      the role the chat is routed to
     * @param scope     the chat's scope
     */
    record Subject(String name, String state, Function<String, ChatRoutes> routes, ChatSubjectKind kind,
    Function<String, String> subjectId, String role, ChatScopeKind scope) {

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Subject> subjects() {
        var question = "Q-" + Generators.letterStrings(4, 8).next();
        return Stream.of(
                new Subject("plan", CHAT_STATE, ChatRoutes::plan, ChatSubjectKind.PLAN, id -> id, REPLAN,
                        ChatScopeKind.PLAN),
                new Subject("epic", CHAT_STATE, ChatRoutes::epic, ChatSubjectKind.EPIC, id -> id, DECOMPOSE,
                        ChatScopeKind.EPIC),
                new Subject("entry", CHAT_STATE, ChatRoutes::entry, ChatSubjectKind.ENTRY, id -> id, REFINE,
                        ChatScopeKind.ENTRY),
                new Subject("decision of a plan", CHAT_STATE, id -> ChatRoutes.planDecision(id, question),
                        ChatSubjectKind.DECISION, id -> id + "/" + question, REPLAN, ChatScopeKind.DECISION),
                new Subject("decision of an epic", CHAT_STATE, id -> ChatRoutes.epicDecision(id, question),
                        ChatSubjectKind.DECISION, id -> id + "/" + question, DECOMPOSE, ChatScopeKind.DECISION),
                new Subject("decision of an entry", CHAT_STATE, id -> ChatRoutes.entryDecision(id, question),
                        ChatSubjectKind.DECISION, id -> id + "/" + question, REFINE, ChatScopeKind.DECISION),
                new Subject("decision of a project's workspace", CHAT_STATE,
                        id -> ChatRoutes.workspaceDecision(id, question), ChatSubjectKind.DECISION,
                        id -> id + "/" + question, REFINE, ChatScopeKind.DECISION),
                new Subject("decision of the machine", CHAT_STATE, id -> ChatRoutes.machineDecision(question),
                        ChatSubjectKind.DECISION, id -> "machine/" + question, REFINE, ChatScopeKind.DECISION),
                new Subject("ended plan", FOLLOW_UP_STATE, id -> ChatRoutes.endedPlan("codeberg-issues"),
                        ChatSubjectKind.PLAN, id -> "codeberg-issues", REPLAN, ChatScopeKind.ARCHIVE),
                new Subject("ended epic", FOLLOW_UP_STATE, id -> ChatRoutes.endedEpic("relay-hardening"),
                        ChatSubjectKind.EPIC, id -> "relay-hardening", DECOMPOSE, ChatScopeKind.ARCHIVE));
    }

    @Nested
    @DisplayName("A chat of every subject kind")
    class EverySubject {

        @ParameterizedTest(name = "{0}")
        @MethodSource("de.planmarshall.mock.ChatScenarioTest#subjects")
        @DisplayName("opens, reads, records a message once, refuses an answer without a proposal and ends")
        void opensReadsTurnsAndEnds(Subject subject) throws Exception {
            var id = Generators.letterStrings(4, 10).next().toLowerCase();
            var routes = subject.routes().apply(id);
            try (var runtime = ordered(subject.state())) {
                assertError(get(runtime, routes.chat()), 404, TARGET_NOT_FOUND);

                var opened = post(runtime, routes.chat(), "");
                assertEquals(201, opened.status(), opened.bodyText());
                var chat = ChatResource.fromJson(opened.bodyText());
                assertAll(subject.name(),
                        () -> assertEquals(subject.kind(), chat.subject().kind()),
                        () -> assertEquals(subject.subjectId().apply(id), chat.subject().id()),
                        () -> assertEquals(subject.role(), chat.role().name()),
                        () -> assertNotNull(chat.role().model()),
                        () -> assertEquals(subject.scope(), chat.scope()),
                        () -> assertEquals(ChatStatus.TURN, chat.state()),
                        () -> assertFalse(chat.context().knows().isEmpty()),
                        () -> assertTrue(chat.context().knows().stream()
                                .allMatch(known -> ChatMessageKeys.maps(known.key())), chat.context().toJson()),
                        () -> assertEquals(subject.scope() == ChatScopeKind.ARCHIVE,
                                "chat.knows.other".equals(chat.context().knows().getFirst().key()),
                                "only ended work knows its archived record: " + chat.context().toJson()),
                        () -> assertTrue(chat.turns().isEmpty()),
                        () -> assertNull(chat.proposal()));

                assertEquals(chat.id(), read(runtime, routes).id());
                var again = post(runtime, routes.chat(), "");
                assertEquals(200, again.status());
                assertEquals(chat.id(), ChatResource.fromJson(again.bodyText()).id(), "one chat per subject");

                var message = message();
                var accepted = post(runtime, routes.turns(), message.toJson());
                assertEquals(ChatTurnAccepted.HTTP_STATUS, accepted.status(), accepted.bodyText());
                var turnId = ChatTurnAccepted.fromJson(accepted.bodyText()).turnId();
                var repeated = post(runtime, routes.turns(), message.toJson());
                assertEquals(ChatTurnAccepted.HTTP_STATUS, repeated.status());
                assertEquals(turnId, ChatTurnAccepted.fromJson(repeated.bodyText()).turnId());

                var answered = read(runtime, routes);
                assertEquals(2, answered.turns().size(), "a repeated client_turn_id records nothing");
                var operatorTurn = answered.turns().getFirst();
                assertAll("the operator's message and the role's reply",
                        () -> assertEquals(turnId, operatorTurn.turnId()),
                        () -> assertTrue(operatorTurn.isOperator()),
                        () -> assertEquals(message.text(), operatorTurn.text()),
                        () -> assertNull(operatorTurn.model()),
                        () -> assertEquals(subject.role(), answered.turns().get(1).author()),
                        () -> assertEquals(TurnStatus.COMPLETE, answered.turns().get(1).status()),
                        () -> assertEquals(ChatStatus.TURN, answered.state()));

                var answer = new ProposalAnswerRequest(turnId).toJson();
                assertError(post(runtime, routes.accept(), answer), 409, ACT_NOT_AVAILABLE);
                assertError(post(runtime, routes.reject(), answer), 409, ACT_NOT_AVAILABLE);

                var ended = post(runtime, routes.end(), "");
                assertEquals(200, ended.status());
                assertEquals(ChatStatus.END, ChatResource.fromJson(ended.bodyText()).state());
                assertError(post(runtime, routes.turns(), message().toJson()), 409, ACT_NOT_AVAILABLE);
                assertError(post(runtime, routes.end(), ""), 409, ACT_NOT_AVAILABLE);

                var reopened = post(runtime, routes.chat(), "");
                assertEquals(201, reopened.status());
                var next = ChatResource.fromJson(reopened.bodyText());
                assertNotEquals(chat.id(), next.id(), "a chat opened after the end is a new chat");
                assertTrue(next.turns().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("The transcript")
    class Transcript {

        @Test
        @DisplayName("reads a long transcript a page of 50 turns at a time, oldest first")
        void pages() throws Exception {
            var routes = ChatRoutes.plan(Generators.letterStrings(4, 10).next().toLowerCase());
            try (var runtime = ordered(CHAT_STATE)) {
                assertEquals(201, post(runtime, routes.chat(), "").status());
                var messages = 26;
                for (var index = 0; index < messages; index++) {
                    assertEquals(ChatTurnAccepted.HTTP_STATUS, post(runtime, routes.turns(), message().toJson())
                            .status());
                }

                var newest = read(runtime, routes);
                assertEquals(50, newest.turns().size());
                var before = Objects.requireNonNull(newest.before());
                assertEquals(newest.turns().getFirst().turnId(), before);

                // Each message adds itself and the reply; the second reply adds its proposal's own turn.
                var older = ChatResource.fromJson(get(runtime, routes.page(before)).bodyText());
                assertEquals(3, older.turns().size());
                assertNull(older.before(), "the oldest page names no page before it");

                var all = new ArrayList<ChatTurn>(older.turns());
                all.addAll(newest.turns());
                assertEquals(2 * messages + 1, all.stream().map(ChatTurn::turnId).distinct().count());
                assertTrue(all.getFirst().isOperator());
                assertFalse(all.getLast().isOperator());

                assertError(get(runtime, routes.page("T-" + Generators.letterStrings(4, 8).next())), 404,
                        TARGET_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("refuses a message outside the limits of its text")
        void turnLimits() throws Exception {
            try (var runtime = ordered(CHAT_STATE)) {
                var routes = ChatRoutes.plan("fix-relay");
                var tooLong = JsonTree.write(Map.of(ChatTurnRequest.FIELD_TEXT,
                        Generators.letterStrings(ChatTurnRequest.MAX_TEXT_LENGTH + 1,
                                ChatTurnRequest.MAX_TEXT_LENGTH + 1).next(),
                        ChatTurnRequest.FIELD_CLIENT_TURN_ID, Generators.letterStrings(8, 12).next()));
                var empty = JsonTree.write(Map.of(ChatTurnRequest.FIELD_TEXT, "",
                        ChatTurnRequest.FIELD_CLIENT_TURN_ID, Generators.letterStrings(8, 12).next()));

                assertError(post(runtime, routes.turns(), tooLong), 400, INVALID_ARGUMENTS);
                assertError(post(runtime, routes.turns(), empty), 400, INVALID_ARGUMENTS);
                assertError(post(runtime, routes.turns(), "{}"), 400, INVALID_ARGUMENTS);
                assertEquals(3, read(runtime, routes).turns().size(), "a refused message records nothing");
            }
        }
    }

    @Nested
    @DisplayName("A reply being written")
    class ReplyBeingWritten {

        private final ChatRoutes routes = ChatRoutes.plan("relay-docs");

        @Test
        @DisplayName("refuses a message while the reply is written and takes one after it is stopped")
        void refusedWhileWriting() throws Exception {
            try (var runtime = ordered(CHAT_STATE)) {
                var chat = read(runtime, routes);
                assertEquals(ChatStatus.REPLY, chat.state());
                assertEquals(TurnStatus.STREAMING, chat.turns().getLast().status());

                assertError(post(runtime, routes.turns(), message().toJson()), 409, ACT_NOT_AVAILABLE);

                var stopped = post(runtime, routes.stop("T-0009"), "");
                assertEquals(200, stopped.status(), stopped.bodyText());
                var afterStop = ChatResource.fromJson(stopped.bodyText());
                var reply = afterStop.turns().getLast();
                assertAll("the stopped reply keeps its text so far",
                        () -> assertEquals(ChatStatus.TURN, afterStop.state()),
                        () -> assertEquals(TurnStatus.STOPPED, reply.status()),
                        () -> assertEquals(chat.turns().getLast().text(), reply.text()));

                assertError(post(runtime, routes.stop("T-0009"), ""), 409, ACT_NOT_AVAILABLE);
                assertError(post(runtime, routes.stop("T-0008"), ""), 409, ACT_NOT_AVAILABLE);
                assertError(post(runtime, routes.stop("T-" + Generators.letterStrings(4, 8).next()), ""), 404,
                        TARGET_NOT_FOUND);
                assertEquals(ChatTurnAccepted.HTTP_STATUS, post(runtime, routes.turns(), message().toJson())
                        .status());
            }
        }
    }

    @Nested
    @DisplayName("The proposal")
    class Proposal {

        private final ChatRoutes plan = ChatRoutes.plan("fix-relay");

        @Test
        @DisplayName("accepts a plan's revision for its next task boundary, once")
        void acceptRevision() throws Exception {
            try (var runtime = ordered(CHAT_STATE)) {
                var chat = read(runtime, plan);
                assertEquals(ChatStatus.READ_BACK, chat.state());
                assertTrue(chat.hasProposal());
                var answer = new ProposalAnswerRequest(Objects.requireNonNull(chat.proposal()).turnId()).toJson();

                var accepted = post(runtime, plan.accept(), answer);
                assertEquals(200, accepted.status(), accepted.bodyText());
                var afterAccept = ChatResource.fromJson(accepted.bodyText());
                assertEquals(ChatStatus.TURN, afterAccept.state());
                assertFalse(afterAccept.hasProposal());
                assertEquals(ProposalState.ACCEPTED, Objects.requireNonNull(afterAccept.proposal()).state());

                assertError(post(runtime, plan.accept(), answer), 409, ACT_NOT_AVAILABLE);
                assertError(post(runtime, plan.reject(), answer), 409, ACT_NOT_AVAILABLE);
            }
        }

        @Test
        @DisplayName("applies an entry's description at once")
        void acceptDescription() throws Exception {
            try (var runtime = ordered(START_CHAT_STATE)) {
                var entry = ChatRoutes.entry("ent-3f9a2c41");
                var chat = read(runtime, entry);
                assertEquals(REFINE, chat.role().name());
                assertEquals(ChatScopeKind.ENTRY, chat.scope());
                var answer = new ProposalAnswerRequest(Objects.requireNonNull(chat.proposal()).turnId()).toJson();

                var accepted = post(runtime, entry.accept(), answer);
                assertEquals(200, accepted.status(), accepted.bodyText());
                assertEquals(ProposalState.APPLIED,
                        Objects.requireNonNull(ChatResource.fromJson(accepted.bodyText()).proposal()).state());
            }
        }

        @Test
        @DisplayName("rejects the open proposal and refuses an answer naming another one")
        void reject() throws Exception {
            try (var runtime = ordered(CHAT_STATE)) {
                var proposalTurnId = Objects.requireNonNull(read(runtime, plan).proposal()).turnId();
                var other = new ProposalAnswerRequest("T-" + Generators.letterStrings(4, 8).next()).toJson();

                assertError(post(runtime, plan.accept(), other), 409, CONFLICT);
                assertError(post(runtime, plan.reject(), other), 409, CONFLICT);
                assertError(post(runtime, plan.reject(), "{}"), 400, INVALID_ARGUMENTS);

                var rejected = post(runtime, plan.reject(), new ProposalAnswerRequest(proposalTurnId).toJson());
                assertEquals(200, rejected.status(), rejected.bodyText());
                var afterReject = ChatResource.fromJson(rejected.bodyText());
                assertEquals(ChatStatus.TURN, afterReject.state());
                assertEquals(ProposalState.REJECTED, Objects.requireNonNull(afterReject.proposal()).state());
            }
        }
    }

    /**
     * Starts the runtime in a state with ordered timing, so a reply has been played to its end when
     * the message's {@code 202} arrives.
     */
    private static MockRuntime ordered(String state) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(state).timing(MockTiming.ORDERED).build());
    }

    private static ChatTurnRequest message() {
        return new ChatTurnRequest(Generators.letterStrings(4, 40).next(), Generators.letterStrings(12, 16).next());
    }

    private static ChatResource read(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var response = get(runtime, routes.chat());
        assertEquals(200, response.status(), response.bodyText());
        return ChatResource.fromJson(response.bodyText());
    }

    private static void assertError(HttpResponse response, int status, String error) throws Exception {
        assertEquals(status, response.status(), response.bodyText());
        assertEquals(error, JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), ERROR));
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
