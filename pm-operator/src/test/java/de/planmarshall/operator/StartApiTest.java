/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.decomposition.DecompositionInstructRequest;
import de.planmarshall.api.decomposition.InstructAction;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryOpenRequest;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The start contract's error surface: every call reports a refusal of the runtime as the dialogue
 * expects it, and the endpoints that answer a benign absence return the documented value.
 */
@DisplayName("start contract client")
@EnableGeneratorController
class StartApiTest {

    private static final String PROJECT = "/api/v1/projects/pm-mcp-clients";
    private static final String DECOMPOSITION = PROJECT + "/epics/relay-hardening/decomposition";
    private static final String ENTRY = "ent-1";
    private static final String CHAT_TURN = "chat.turn";

    private RuntimeFixture fixture;
    private OperatorContext context;
    private StartApi api;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        context = new OperatorContext(fixture.environment(), fixture.base().getParent(), true);
        api = new StartApi(context);
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private void serve(FakeRuntimeServer.Handler handler) throws IOException {
        fixture.start(handler);
    }

    private static FakeRuntimeServer.Handler failure(int status, String body) {
        return (request, response) -> response.send(status, "application/json", body);
    }

    @Test
    @DisplayName("open, fetch, and act report a refusal")
    void openFetchActRefused() throws Exception {
        serve(failure(500, "{\"message\":\"no\"}"));
        assertAll(
                            () -> assertThrows(OperatorRefusedException.class,
                                                () -> api.open("pm-mcp-clients", EntryOpenRequest.of(EntryKind.PLAN))),
                            () -> assertThrows(OperatorRefusedException.class, () -> api.fetch("ent-1")),
                            () -> assertThrows(StartApi.StartActException.class,
                                                () -> api.act("ent-1", "add_issue", Map.of())));
    }

    @Test
    @DisplayName("a link id outside the closed form is refused before the request")
    void unsafeLinkIdRefused() throws Exception {
        serve(failure(500, "{}"));
        assertThrows(IllegalArgumentException.class,
                            () -> api.act("ent-1", "bad id?query", Map.of()));
    }

    @Test
    @DisplayName("a refused act carries the runtime's message")
    void actRefusedCarriesMessage() throws Exception {
        serve(failure(409, "{\"message\":\"the source does not combine\"}"));
        var refused = assertThrows(StartApi.StartActException.class,
                            () -> api.act("ent-1", "add_issue", Map.of()));
        assertTrue(refused.getMessage().contains("the source does not combine"), refused.getMessage());
    }

    @Test
    @DisplayName("the entry chat's open, turn, proposal answer, and end report a refusal")
    void chatRefused() throws Exception {
        serve(failure(409, "{\"message\":\"no chat\"}"));
        assertAll(
                            () -> assertThrows(StartApi.StartActException.class,
                                                () -> api.openEntryChat("ent-1")),
                            () -> assertThrows(StartApi.StartActException.class,
                                                () -> api.entryChatTurn("ent-1", "hello")),
                            () -> assertThrows(StartApi.StartActException.class,
                                                () -> api.answerEntryChatProposal("ent-1", "T-1", true)),
                            () -> assertThrows(StartApi.StartActException.class,
                                                () -> api.answerEntryChatProposal("ent-1", "T-1", false)),
                            () -> assertThrows(StartApi.StartActException.class,
                                                () -> api.endEntryChat("ent-1")));
    }

    @Test
    @DisplayName("a reply still written is read again until it is written")
    void entryChatTurnWaitsForTheReply() throws Exception {
        var text = Generators.letterStrings(8, 24).next();
        var reads = new AtomicInteger();
        serve((request, response) -> {
            if ("POST".equals(request.method())) {
                response.json(202, "{\"turn_id\":\"T-1\"}");
            } else if (reads.incrementAndGet() < 3) {
                response.json(200, chatJson("chat.reply", "streaming", ""));
            } else {
                response.json(200, chatJson(CHAT_TURN, "complete", text));
            }
        });
        var waiting = new StartApi(context, new StartApi.ReplyWait(5, Duration.ZERO));
        var chat = waiting.entryChatTurn(ENTRY, Generators.letterStrings(4, 12).next());
        assertAll(
                            () -> assertEquals(3, reads.get()),
                            () -> assertFalse(StartApi.isWriting(chat)),
                            () -> assertEquals(text, chat.turns().getLast().text()));
    }

    @Test
    @DisplayName("the wait for a reply ends after its reads, with the reply still written")
    void entryChatTurnWaitIsBounded() throws Exception {
        var reads = new AtomicInteger();
        serve((request, response) -> {
            if ("POST".equals(request.method())) {
                response.json(202, "{\"turn_id\":\"T-1\"}");
            } else {
                reads.incrementAndGet();
                response.json(200, chatJson(CHAT_TURN, "streaming", ""));
            }
        });
        var waiting = new StartApi(context, new StartApi.ReplyWait(3, Duration.ZERO));
        var chat = waiting.entryChatTurn(ENTRY, Generators.letterStrings(4, 12).next());
        assertAll(
                            () -> assertEquals(3, reads.get()),
                            () -> assertTrue(StartApi.isWriting(chat)));
    }

    @Test
    @DisplayName("accept and reject post the proposal's turn to the entry chat's proposal routes")
    void proposalAnswerCarriesTheTurn() throws Exception {
        var turnId = "T-" + Generators.integers(1, 9999).next();
        var posted = new CopyOnWriteArrayList<String>();
        serve((request, response) -> {
            posted.add(request.target() + " " + request.bodyText());
            response.json(200, chatJson(CHAT_TURN, "complete", "done"));
        });
        api.answerEntryChatProposal(ENTRY, turnId, true);
        api.answerEntryChatProposal(ENTRY, turnId, false);
        var body = "{\"turn_id\":\"" + turnId + "\"}";
        assertEquals(List.of(ChatRoutes.entry(ENTRY).accept() + " " + body,
                            ChatRoutes.entry(ENTRY).reject() + " " + body), posted);
    }

    @Test
    @DisplayName("the page before a turn is read on the entry chat's page route, and a refusal is reported")
    void entryChatPageReadsBeforeTheTurn() throws Exception {
        var turnId = "T-" + Generators.integers(1, 9999).next();
        var text = Generators.letterStrings(8, 24).next();
        var read = new CopyOnWriteArrayList<String>();
        serve((request, response) -> {
            read.add(request.method() + " " + request.target());
            if (read.size() == 1) {
                response.json(200, chatJson(CHAT_TURN, "complete", text));
            } else {
                response.json(409, "{\"message\":\"no chat\"}");
            }
        });
        var page = api.entryChatPage(ENTRY, turnId);
        var refused = assertThrows(StartApi.StartActException.class, () -> api.entryChatPage(ENTRY, turnId));
        assertAll(
                            () -> assertEquals("GET " + ChatRoutes.entry(ENTRY).page(turnId), read.getFirst()),
                            () -> assertEquals(text, page.turns().getLast().text()),
                            () -> assertTrue(refused.getMessage().contains("no chat"), refused.getMessage()));
    }

    @Test
    @DisplayName("a reply wait reads at least once and pauses no negative time")
    void replyWaitBounds() {
        var negative = Duration.ofMillis(-1);
        assertAll(
                            () -> assertThrows(IllegalArgumentException.class,
                                                () -> new StartApi.ReplyWait(0, Duration.ZERO)),
                            () -> assertThrows(IllegalArgumentException.class,
                                                () -> new StartApi.ReplyWait(1, negative)));
    }

    /** A chat of the entry with one reply of the role in the given state and status. */
    private static String chatJson(String state, String status, String text) {
        return """
                            {"id":"C-1","subject":{"kind":"entry","id":"ent-1"},
                            "role":{"name":"refine","model":"sonnet"},"scope":"entry","state":"%s",
                            "context":{"knows":[{"key":"chat.knows.description"}],"as_of":"2026-10-19T10:00:05Z"},
                            "turns":[{"turn_id":"T-2","kind":"message","author":"refine","status":"%s",
                            "format":"markdown","untrusted":false,"model":"sonnet","text":"%s","steps":[],
                            "refs":[],"at":"2026-10-19T10:00:05Z"}]}
                            """.formatted(state, status, text);
    }

    @Test
    @DisplayName("projects and recipes treat a refusal as empty and failed")
    void projectsAndRecipes() throws Exception {
        serve(failure(500, "{}"));
        assertAll(
                            () -> assertTrue(api.projects().isEmpty()),
                            () -> assertThrows(OperatorRefusedException.class, () -> api.recipes()));
    }

    @Test
    @DisplayName("the decomposition is absent, and a failed check or commit is reported")
    void decompositionPaths() throws Exception {
        serve((request, response) -> {
            if (DECOMPOSITION.equals(request.target()) && "GET".equals(request.method())) {
                response.send(500, "application/json", "{}");
            } else if ((DECOMPOSITION + "/check").equals(request.target())) {
                response.send(404, "application/json", "{}");
            } else if ((DECOMPOSITION + "/instruct").equals(request.target())) {
                response.send(409, "application/json", "{}");
            } else if ((DECOMPOSITION + "/commit").equals(request.target())) {
                response.send(409, "application/json", "{}");
            } else {
                response.send(404, "application/json", "{}");
            }
        });
        assertAll(
                            () -> assertThrows(OperatorRefusedException.class,
                                                () -> api.decomposition("pm-mcp-clients", "relay-hardening")),
                            () -> assertNull(api.checkDecomposition("pm-mcp-clients", "relay-hardening")),
                            () -> assertThrows(OperatorRefusedException.class,
                                                () -> api.instruct("pm-mcp-clients", "relay-hardening",
                                                                    new DecompositionInstructRequest(null, "x",
                                                                                        InstructAction.EDIT))),
                            () -> assertNull(api.commit("pm-mcp-clients", "relay-hardening")));
    }

    @Test
    @DisplayName("an absent decomposition is null")
    void decompositionAbsent() throws Exception {
        serve(failure(404, "{}"));
        assertNull(api.decomposition("pm-mcp-clients", "relay-hardening"));
    }

    @Test
    @DisplayName("the projects endpoint reads an object and a bare array")
    void projectsReadsBothShapes() throws Exception {
        serve((request, response) -> response.json(200,
                            "{\"projects\":[{\"project_id\":\"pm-mcp-clients\"},{\"project_id\":\"pm-api\"}]}"));
        assertTrue(api.projects().containsAll(List.of("pm-mcp-clients", "pm-api")));
    }
}
