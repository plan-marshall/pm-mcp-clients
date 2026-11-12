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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatProposalEvent;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalKind;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.RefKind;
import de.planmarshall.api.chat.TurnFormat;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.chat.TurnRef;
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.SseReader;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceResource;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.mock.runtime.MockTiming;

/**
 * The scripted conversations of the mock's chats and the proposals that act: per proposal kind, the
 * role's second reply proposes it, accept changes what the design says it changes and a second answer is
 * refused, and reject changes nothing. In an epic's chat the third reply proposes a change to the draft,
 * which replaces the rows proposal still open.
 */
@DisplayName("ChatProposals")
@EnabledOnOs({OS.MAC, OS.LINUX})
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@EnableGeneratorController
class ChatProposalsTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String CHAT_STATE = StandardScenarios.RUNNING_WORK + "#control.chat";
    private static final String START_CHAT_STATE = StandardScenarios.PROJECTS_AND_START + "#start.chat";
    private static final String FOLLOW_UP_STATE = StandardScenarios.PROJECTS_AND_START + "#start.follow-up";
    private static final String ENTRY_ID = "ent-3f9a2c41";
    private static final String EPIC_ID = "relay-hardening";
    private static final String DECOMPOSITION = "/api/v1/projects/pm-mcp-clients/epics/" + EPIC_ID
            + "/decomposition";
    private static final String ERROR = "error";
    private static final String ACT_NOT_AVAILABLE = "act_not_available";
    private static final String APPLIED_DESCRIPTION = "Fix the relay's reconnect: keep the cursor across a restart.";

    @Nested
    @DisplayName("The scripted conversation")
    class Script {

        @Test
        @DisplayName("the first reply holds a list, a code block, a table, a Mermaid block, a step and four references")
        void richFirstReply() throws Exception {
            var routes = ChatRoutes.plan(id());
            try (var runtime = ordered(CHAT_STATE)) {
                openChat(runtime, routes);

                send(runtime, routes);

                var reply = read(runtime, routes).turns().getLast();
                assertAll("the first reply",
                        () -> assertEquals(TurnFormat.MARKDOWN, reply.format()),
                        () -> assertFalse(reply.untrusted()),
                        () -> assertTrue(reply.text().contains("- TASK-4 has no test"), reply.text()),
                        () -> assertTrue(reply.text().contains("```java"), reply.text()),
                        () -> assertTrue(reply.text().contains("|------|-------|"), reply.text()),
                        () -> assertTrue(reply.text().contains("```mermaid"), reply.text()),
                        () -> assertEquals(List.of("chat.step.read"),
                                reply.steps().stream().map(MessageText::key).toList()),
                        () -> assertEquals(List.of(RefKind.TASK, RefKind.FILE, RefKind.DECISION, RefKind.PLAN),
                                reply.refs().stream().map(TurnRef::kind).toList()),
                        () -> assertTrue(Objects.requireNonNull(reply.refs().get(1).id()).contains(":"),
                                "a file is referenced with its line"));
            }
        }

        @Test
        @DisplayName("the second reply proposes as a turn of its own, the third is an untrusted plain turn")
        void proposalAndUntrustedTurn() throws Exception {
            var routes = ChatRoutes.plan(id());
            try (var runtime = ordered(CHAT_STATE)) {
                openChat(runtime, routes);

                send(runtime, routes);
                send(runtime, routes);
                var proposed = read(runtime, routes);
                var proposal = Objects.requireNonNull(proposed.proposal());
                var proposalTurn = proposed.turns().getLast();
                var reply = proposed.turns().get(proposed.turns().size() - 2);
                assertAll("the proposal",
                        () -> assertEquals(ChatStatus.READ_BACK, proposed.state()),
                        () -> assertEquals(TurnKind.PROPOSAL, proposalTurn.kind()),
                        () -> assertEquals(proposalTurn.turnId(), proposal.turnId()),
                        () -> assertEquals(reply.turnId(), proposal.replyTurnId()),
                        () -> assertEquals(List.of("chat.step.search", "chat.step.check"),
                                reply.steps().stream().map(MessageText::key).toList()),
                        () -> assertEquals(ProposalState.OPEN, proposal.state()));

                send(runtime, routes);
                var untrusted = read(runtime, routes).turns().getLast();
                assertAll("the untrusted turn",
                        () -> assertTrue(untrusted.untrusted()),
                        () -> assertEquals(TurnFormat.PLAIN, untrusted.format()),
                        () -> assertFalse(untrusted.isOperator()));
            }
        }
    }

    @Nested
    @DisplayName("Accept")
    class Accept {

        @Test
        @DisplayName("queues a plan's revision for its next task boundary, once")
        void revision() throws Exception {
            var routes = ChatRoutes.plan(id());
            try (var runtime = ordered(CHAT_STATE)) {
                openChat(runtime, routes);
                var proposal = proposeAt(runtime, routes, 2, ProposalKind.REVISION);
                assertEquals("chat.applies.after_task", Objects.requireNonNull(proposal.applies()).key());

                var accepted = answer(runtime, routes.accept(), proposal);

                assertEquals(ProposalState.ACCEPTED, Objects.requireNonNull(accepted.proposal()).state());
                assertEquals(ChatStatus.TURN, accepted.state());
                assertRefusedTwice(runtime, routes, proposal);
            }
        }

        @Test
        @DisplayName("applies an entry's description: the entry's state shows it as its description")
        void description() throws Exception {
            var routes = ChatRoutes.entry(ENTRY_ID);
            try (var runtime = ordered(START_CHAT_STATE)) {
                var proposal = proposeAt(runtime, routes, 2, ProposalKind.DESCRIPTION);
                assertEquals(APPLIED_DESCRIPTION, proposal.after());
                assertFalse(descriptions(runtime).contains(APPLIED_DESCRIPTION));

                var accepted = answer(runtime, routes.accept(), proposal);

                assertEquals(ProposalState.APPLIED, Objects.requireNonNull(accepted.proposal()).state());
                assertEquals(List.of(APPLIED_DESCRIPTION), descriptions(runtime));
                assertRefusedTwice(runtime, routes, proposal);
            }
        }

        @Test
        @DisplayName("applies a change to an epic's staged rows: the decomposition moves on by one revision")
        void rows() throws Exception {
            var routes = ChatRoutes.epic(EPIC_ID);
            try (var runtime = ordered(CHAT_STATE)) {
                assertEquals(ChatScopeKind.EPIC, openChat(runtime, routes).scope());
                var proposal = proposeAt(runtime, routes, 2, ProposalKind.ROWS);
                var before = decompositionRevision(runtime);

                var accepted = answer(runtime, routes.accept(), proposal);

                assertEquals(ProposalState.APPLIED, Objects.requireNonNull(accepted.proposal()).state());
                assertEquals(before + 1, decompositionRevision(runtime));
                assertRefusedTwice(runtime, routes, proposal);
            }
        }

        @Test
        @DisplayName("replaces an open rows proposal by the draft proposal, and applies the draft")
        void draftReplacesRows() throws Exception {
            var routes = ChatRoutes.epic(EPIC_ID);
            try (var runtime = ordered(CHAT_STATE);
                 var stream = open(runtime, "/api/v1/epics/" + EPIC_ID + "/events")) {
                var chat = openChat(runtime, routes);
                var rows = proposeAt(runtime, routes, 2, ProposalKind.ROWS);
                var draft = proposeAt(runtime, routes, 3, ProposalKind.DRAFT);
                var events = stream.events();

                // A proposal the seeded chat held is replaced by the rows proposal first; only the rows and
                // the draft proposal are looked at.
                var announced = new ArrayList<ChatProposalEvent>();
                ChatProposalEvent next;
                do {
                    next = nextProposal(events, chat.id());
                    if (next.turnId().equals(rows.turnId()) || next.turnId().equals(draft.turnId())) {
                        announced.add(next);
                    }
                } while (!next.turnId().equals(draft.turnId()));
                assertAll("chat.proposal announces the open, the replaced and the new proposal",
                        () -> assertEquals(List.of(rows.turnId(), rows.turnId(), draft.turnId()),
                                announced.stream().map(ChatProposalEvent::turnId).toList()),
                        () -> assertEquals(List.of(ProposalState.OPEN, ProposalState.REPLACED, ProposalState.OPEN),
                                announced.stream().map(ChatProposalEvent::state).toList()));
                assertRefused(post(runtime, routes.accept(), new ProposalAnswerRequest(rows.turnId()).toJson()),
                        409, "conflict");
                var before = decompositionRevision(runtime);

                var accepted = answer(runtime, routes.accept(), draft);

                assertEquals(ProposalState.APPLIED, Objects.requireNonNull(accepted.proposal()).state());
                assertEquals(before + 1, decompositionRevision(runtime));
            }
        }

        @Test
        @DisplayName("carries an answer in a decision's chat to the decision, which ends the chat")
        void decisionAnswer() throws Exception {
            var routes = ChatRoutes.planDecision(id(), "Q-" + Generators.letterStrings(4, 8).next());
            try (var runtime = ordered(CHAT_STATE)) {
                openChat(runtime, routes);
                var proposal = proposeAt(runtime, routes, 2, ProposalKind.ANSWER);

                var accepted = answer(runtime, routes.accept(), proposal);

                assertEquals(ProposalState.APPLIED, Objects.requireNonNull(accepted.proposal()).state());
                assertEquals(ChatStatus.CARRIED, accepted.state());
                assertRefusedTwice(runtime, routes, proposal);
            }
        }

        @Test
        @DisplayName("applies a follow-up plan of ended work")
        void followUp() throws Exception {
            var routes = ChatRoutes.endedPlan("codeberg-issues");
            try (var runtime = ordered(FOLLOW_UP_STATE)) {
                assertEquals(ChatScopeKind.ARCHIVE, openChat(runtime, routes).scope());
                var proposal = proposeAt(runtime, routes, 2, ProposalKind.FOLLOW_UP);

                var accepted = answer(runtime, routes.accept(), proposal);

                assertEquals(ProposalState.APPLIED, Objects.requireNonNull(accepted.proposal()).state());
                assertRefusedTwice(runtime, routes, proposal);
            }
        }
    }

    @Nested
    @DisplayName("Reject")
    class Reject {

        @Test
        @DisplayName("leaves an entry's description as it was")
        void description() throws Exception {
            var routes = ChatRoutes.entry(ENTRY_ID);
            try (var runtime = ordered(START_CHAT_STATE)) {
                var proposal = proposeAt(runtime, routes, 2, ProposalKind.DESCRIPTION);
                var before = descriptions(runtime);

                var rejected = answer(runtime, routes.reject(), proposal);

                assertEquals(ProposalState.REJECTED, Objects.requireNonNull(rejected.proposal()).state());
                assertEquals(before, descriptions(runtime));
                assertRefusedTwice(runtime, routes, proposal);
            }
        }

        @Test
        @DisplayName("leaves an epic's decomposition at its revision")
        void rows() throws Exception {
            var routes = ChatRoutes.epic(EPIC_ID);
            try (var runtime = ordered(CHAT_STATE)) {
                openChat(runtime, routes);
                var proposal = proposeAt(runtime, routes, 2, ProposalKind.ROWS);
                var before = decompositionRevision(runtime);

                var rejected = answer(runtime, routes.reject(), proposal);

                assertEquals(ProposalState.REJECTED, Objects.requireNonNull(rejected.proposal()).state());
                assertEquals(ChatStatus.TURN, rejected.state());
                assertEquals(before, decompositionRevision(runtime));
            }
        }

        @Test
        @DisplayName("leaves a decision's chat open with nothing carried")
        void decisionAnswer() throws Exception {
            var routes = ChatRoutes.planDecision(id(), "Q-" + Generators.letterStrings(4, 8).next());
            try (var runtime = ordered(CHAT_STATE)) {
                openChat(runtime, routes);
                var proposal = proposeAt(runtime, routes, 2, ProposalKind.ANSWER);

                var rejected = answer(runtime, routes.reject(), proposal);

                assertEquals(ProposalState.REJECTED, Objects.requireNonNull(rejected.proposal()).state());
                assertEquals(ChatStatus.TURN, rejected.state());
            }
        }
    }

    /**
     * Sends messages until the role has written the reply of the given number (the second reply is
     * {@code 2}), counting the replies a seeded chat already holds, and returns the proposal it made.
     *
     * @return the open proposal
     */
    private static ChatProposal proposeAt(MockRuntime runtime, ChatRoutes routes, int reply, ProposalKind kind)
            throws Exception {
        var written = read(runtime, routes).turns().stream()
                .filter(turn -> turn.kind() == TurnKind.MESSAGE && !turn.isOperator()).count();
        for (var index = written; index < reply; index++) {
            send(runtime, routes);
        }
        var chat = read(runtime, routes);
        var proposal = Objects.requireNonNull(chat.proposal(), "the reply proposes");
        assertEquals(kind, proposal.kind());
        assertEquals(ProposalState.OPEN, proposal.state());
        return proposal;
    }

    private static ChatResource answer(MockRuntime runtime, String path, ChatProposal proposal) throws Exception {
        var answered = post(runtime, path, new ProposalAnswerRequest(proposal.turnId()).toJson());
        assertEquals(200, answered.status(), answered.bodyText());
        return ChatResource.fromJson(answered.bodyText());
    }

    private static void assertRefusedTwice(MockRuntime runtime, ChatRoutes routes, ChatProposal proposal)
            throws Exception {
        var answer = new ProposalAnswerRequest(proposal.turnId()).toJson();
        assertRefused(post(runtime, routes.accept(), answer), 409, ACT_NOT_AVAILABLE);
        assertRefused(post(runtime, routes.reject(), answer), 409, ACT_NOT_AVAILABLE);
    }

    private static void assertRefused(HttpResponse response, int status, String error) throws Exception {
        assertEquals(status, response.status(), response.bodyText());
        assertEquals(error, JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), ERROR));
    }

    private static List<String> descriptions(MockRuntime runtime) throws Exception {
        var response = get(runtime, "/api/v1/entries/" + ENTRY_ID);
        assertEquals(200, response.status(), response.bodyText());
        return StartStateResource.fromJson(response.bodyText()).entry().sources().stream()
                .filter(source -> source.kind() == SourceKind.DESCRIPTION).map(SourceResource::reference)
                .toList();
    }

    private static long decompositionRevision(MockRuntime runtime) throws Exception {
        var response = get(runtime, DECOMPOSITION);
        assertEquals(200, response.status(), response.bodyText());
        var revision = JsonTree.number(JsonTree.asObject(JsonTree.parse(response.body())), "revision");
        assertNotNull(revision, response.bodyText());
        return revision.longValue();
    }

    private static ChatProposalEvent nextProposal(SseReader events, String chatId) throws IOException {
        while (true) {
            var event = events.next();
            assertNotNull(event, "the stream ended");
            if (!ChatProposalEvent.EVENT_NAME.equals(event.event())) {
                continue;
            }
            var data = new LinkedHashMap<>(JsonTree.asObject(JsonTree.parse(event.data())));
            data.remove("event");
            data.remove("at");
            data.remove("scope");
            var proposal = ChatProposalEvent.fromMap(data);
            if (chatId.equals(proposal.chatId())) {
                return proposal;
            }
        }
    }

    private static MockRuntime ordered(String state) throws IOException {
        return MockRuntime.start(MockRuntimeConfig.builder().scenario(state).timing(MockTiming.ORDERED).build());
    }

    private static String id() {
        return Generators.letterStrings(4, 10).next().toLowerCase();
    }

    private static ChatResource openChat(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var opened = post(runtime, routes.chat(), "");
        assertTrue(opened.status() == 200 || opened.status() == 201, opened.bodyText());
        return ChatResource.fromJson(opened.bodyText());
    }

    private static void send(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var message = new ChatTurnRequest(Generators.letterStrings(4, 40).next(),
                Generators.letterStrings(12, 16).next());
        var accepted = post(runtime, routes.turns(), message.toJson());
        assertEquals(ChatTurnAccepted.HTTP_STATUS, accepted.status(), accepted.bodyText());
    }

    private static ChatResource read(MockRuntime runtime, ChatRoutes routes) throws Exception {
        var response = get(runtime, routes.chat());
        assertEquals(200, response.status(), response.bodyText());
        return ChatResource.fromJson(response.bodyText());
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpExchange open(MockRuntime runtime, String path) throws IOException {
        var exchange = new UnixSocketHttpClient(runtime.socketPath())
                .open(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
        assertTrue(exchange.isEventStream(), path);
        return exchange;
    }
}
