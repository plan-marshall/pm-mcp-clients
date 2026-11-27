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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The chat's proposal prompt answers an input it does not offer instead of leaving the chat: only
 * {@code Enter}, the reject key and the leave key act (design <em>Dialogues</em>, links as keys).
 */
@EnableGeneratorController
@EnabledOnOs({OS.MAC, OS.LINUX})
@DisplayName("WorkChatDialogue")
class WorkChatDialogueTest {

    private static final String CHAT_PROPOSAL = """
                        {"id":"C-1","subject":{"kind":"plan","id":"p"},
                         "role":{"name":"replan","model":"sonnet"},"scope":"archive",
                         "state":"chat.read-back","context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},
                         "turns":[],
                         "proposal":{"turn_id":"T-3","reply_turn_id":"T-2","kind":"follow_up",
                           "summary":"a test per transport","before":"one transport",
                           "after":"websocket and SSE","state":"open"}}
                        """;

    private RuntimeFixture fixture;
    private VirtualScreenTerminal virtual;
    private OperatorContext context;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        fixture.writeToken("test-token");
        virtual = VirtualScreenTerminal.create(160, 40);
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        context = new OperatorContext(env, fixture.base().getParent(), true,
                            ClientEnvironment.capture(env), virtual::terminal, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        virtual.close();
        fixture.close();
    }

    @Test
    @DisplayName("an input the proposal does not offer is answered, and q leaves with nothing sent")
    void unofferedInputAtTheProposal() throws Exception {
        FakeRuntimeServer server = fixture.start((request, response) -> {
            if (("GET".equals(request.method()) || "POST".equals(request.method()))
                                && request.target().endsWith("/chat")) {
                response.json(200, CHAT_PROPOSAL);
            } else {
                response.send(500, null, "");
            }
        });
        var input = Generators.letterStrings(8, 12).next();
        virtual.type(input + "\n?\nq\n");
        virtual.endOfInput();
        var out = new StringWriter();

        var exitCode = WorkChatDialogue.run(context, new PrintWriter(out), "plan", "p", false, false);

        var text = out.toString();
        assertAll(
                            () -> assertEquals(0, exitCode),
                            () -> assertTrue(text.contains(input + " is not offered here: r, q, ?"), text),
                            () -> assertTrue(text.contains("Nothing more is explained here"), text),
                            () -> assertFalse(text.contains("is kept with"), text),
                            () -> assertEquals(0, server.requests().stream()
                                                                    .filter(recorded -> !"GET".equals(recorded.method())
                                                                                        && !"POST".equals(recorded.method()))
                                                                    .count(),
                                                "nothing else is sent: " + server.requests()));
    }

    @Test
    @DisplayName("turns without a proposal print the no-proposal notice once")
    void noProposalNoticePrintedOnceAcrossTurns() throws Exception {
        var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
        var msg1 = Generators.letterStrings(6, 10).next();
        var msg2 = Generators.letterStrings(6, 10).next();
        var reply1 = Generators.letterStrings(6, 10).next();
        var reply2 = Generators.letterStrings(6, 10).next();
        var posts = new AtomicInteger();
        var base = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                            + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                            + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},";
        var t1 = chatTurnJson("T-1", "operator", msg1, "2026-10-16T10:01:00Z");
        var t2 = chatTurnJson("T-2", "replan", reply1, "2026-10-16T10:01:05Z");
        var t3 = chatTurnJson("T-3", "operator", msg2, "2026-10-16T10:02:00Z");
        var t4 = chatTurnJson("T-4", "replan", reply2, "2026-10-16T10:02:05Z");
        var initial = base + "\"turns\":[]}";
        var afterFirst = base + "\"turns\":[" + t1 + "," + t2 + "]}";
        var afterSecond = base + "\"turns\":[" + t1 + "," + t2 + "," + t3 + "," + t4 + "]}";
        fixture.start((request, response) -> {
            var path = request.target();
            if (path.endsWith("/chat") && ("GET".equals(request.method()) || "POST".equals(request.method()))) {
                int post = posts.get();
                response.json(200, post == 0 ? initial : (post == 1 ? afterFirst : afterSecond));
            } else if (path.endsWith("/chat/turns")) {
                int post = posts.incrementAndGet();
                response.json(202, "{\"turn_id\":\"" + (post == 1 ? "T-1" : "T-3") + "\"}");
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type(msg1 + "\n\n" + msg2 + "\n\nq\n");
        virtual.endOfInput();
        var out = new StringWriter();

        var exitCode = WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false);

        var text = out.toString();
        var notice = subject + " proposes no change yet";
        var firstIdx = text.indexOf(notice);
        assertAll(
                            () -> assertEquals(0, exitCode),
                            () -> assertTrue(text.contains(reply1), text),
                            () -> assertTrue(text.contains(reply2), text),
                            () -> assertTrue(firstIdx >= 0, text),
                            () -> assertEquals(-1, text.indexOf(notice, firstIdx + notice.length()), text),
                            () -> assertTrue(text.contains("Left the chat; it stays with " + subject + "."), text));
    }

    private static String chatTurnJson(String id, String author, String text, String at) {
        return "{\"turn_id\":\"" + id + "\",\"kind\":\"message\",\"author\":\"" + author
                            + "\",\"status\":\"complete\",\"format\":\"plain\",\"untrusted\":false,\"text\":\""
                            + text + "\",\"steps\":[],\"refs\":[],\"at\":\"" + at + "\"}";
    }

    @Nested
    @DisplayName("leaving, multi-line composition, and new (D2)")
    class LeavingCompositionAndNew {

        @ParameterizedTest(name = "leave via {0}")
        @ValueSource(strings = {"empty", "q", "eof"})
        @DisplayName("empty first line, q, and Ctrl+D leave without ending and print chat.left")
        void messagePromptLeaveVariants(String variant) throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var chatJson = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":[]}";
            var server = fixture.start((request, response) -> {
                if (request.target().endsWith("/chat") && "POST".equals(request.method())) {
                    response.json(200, chatJson);
                } else {
                    response.send(404, null, "");
                }
            });
            if ("empty".equals(variant)) {
                virtual.type("\n");
            } else if ("q".equals(variant)) {
                virtual.type("q\n");
            }
            virtual.endOfInput();
            var out = new StringWriter();

            int exitCode = WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false);

            var text = out.toString();
            assertAll(
                                () -> assertEquals(0, exitCode),
                                () -> assertTrue(text.contains("Left the chat; it stays with " + subject + "."), text),
                                () -> assertFalse(text.contains("is kept with"), text),
                                () -> assertTrue(server.requests().stream().noneMatch(
                                                    req -> req.target().endsWith("/end") || req.target().endsWith("/turns")),
                                                    "must not end chat or send turn: " + server.requests()));
        }

        @ParameterizedTest(name = "leave proposal via {0}")
        @ValueSource(strings = {"q", "eof"})
        @DisplayName("q and Ctrl+D at Accept? leave printing chat.left without ending, and returning reprints proposal")
        void proposalLeaveAndReturn(String variant) throws Exception {
            var server = fixture.start((request, response) -> {
                if (request.target().endsWith("/chat") && ("POST".equals(request.method()) || "GET".equals(request.method()))) {
                    response.json(200, CHAT_PROPOSAL);
                } else {
                    response.send(404, null, "");
                }
            });
            if ("q".equals(variant)) {
                virtual.type("q\n");
            }
            virtual.endOfInput();
            var firstOut = new StringWriter();

            int firstExit = WorkChatDialogue.run(context, new PrintWriter(firstOut), "plan", "p", false, false);

            var secondOut = new StringWriter();
            try (var secondTerminal = VirtualScreenTerminal.create(120, 30)) {
                var env = new HashMap<>(fixture.environment());
                env.put("LANG", "en_US.UTF-8");
                var secondContext = new OperatorContext(env, Files.createTempDirectory("pm-wd2"), true,
                                    ClientEnvironment.capture(env), secondTerminal::terminal, null);
                secondTerminal.type("q\n");
                secondTerminal.endOfInput();
                WorkChatDialogue.run(secondContext, new PrintWriter(secondOut), "plan", "p", false, false);
            }

            assertAll(
                                () -> assertEquals(0, firstExit),
                                () -> assertTrue(firstOut.toString().contains("Left the chat; it stays with p."),
                                                    firstOut.toString()),
                                () -> assertTrue(secondOut.toString().contains("Start a plan with it?"),
                                                    secondOut.toString()),
                                () -> assertTrue(server.requests().stream().noneMatch(
                                                    req -> req.target().endsWith("/end")),
                                                    "must not end chat: " + server.requests()));
        }

        @Test
        @DisplayName("multi-line composition joins non-empty lines and sends on blank line")
        void multiLineComposition() throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var line1 = Generators.letterStrings(5, 9).next();
            var line2 = Generators.letterStrings(5, 9).next();
            var turnTexts = new ArrayList<String>();
            var posts = new AtomicInteger();
            var base = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},";
            var initial = base + "\"turns\":[]}";
            var replied = base + "\"turns\":["
                                + chatTurnJson("T-1", "operator", line1 + "\\n" + line2, "2026-10-16T10:01:00Z") + ","
                                + chatTurnJson("T-2", "replan", "received", "2026-10-16T10:01:01Z") + "]}";
            fixture.start((request, response) -> {
                var path = request.target();
                if (path.endsWith("/chat")) {
                    response.json(200, posts.get() == 0 ? initial : replied);
                } else if (path.endsWith("/chat/turns")) {
                    posts.incrementAndGet();
                    var parsed = JsonTree.parse(request.bodyText());
                    if (parsed instanceof Map<?, ?> map && map.get("text") instanceof String txt) {
                        turnTexts.add(txt);
                    }
                    response.json(202, "{\"turn_id\":\"T-1\"}");
                } else {
                    response.send(404, null, "");
                }
            });
            virtual.type(line1 + "\n" + line2 + "\n\nq\n");
            virtual.endOfInput();
            var out = new StringWriter();

            int exitCode = WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false);

            assertAll(
                                () -> assertEquals(0, exitCode),
                                () -> assertEquals(List.of(line1 + "\n" + line2), turnTexts, turnTexts.toString()));
        }

        @Test
        @DisplayName("new followed by y ends the current chat, opens a fresh one, and prints control.chat.ended")
        void newConfirmed() throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var emptyChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":[]}";
            var calls = new ArrayList<String>();
            fixture.start((request, response) -> {
                calls.add(request.method() + " " + request.target());
                if (request.target().endsWith("/chat/end") && "POST".equals(request.method())) {
                    response.json(200, emptyChat);
                } else if (request.target().endsWith("/chat") && "POST".equals(request.method())) {
                    response.json(200, emptyChat);
                } else {
                    response.send(404, null, "");
                }
            });
            virtual.type("new\ny\nq\n");
            virtual.endOfInput();
            var out = new StringWriter();

            int exitCode = WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false);

            var text = out.toString();
            assertAll(
                                () -> assertEquals(0, exitCode),
                                () -> assertTrue(text.contains("Start a new chat?"), text),
                                () -> assertTrue(text.contains("y) yes   n) no [Enter]"), text),
                                () -> assertTrue(text.contains("The earlier chat is kept with " + subject + "."), text),
                                () -> assertTrue(calls.contains("POST /api/v1/plans/" + subject + "/chat/end"),
                                                    calls.toString()),
                                () -> assertEquals(2, calls.stream()
                                                    .filter(c -> c.equals("POST /api/v1/plans/" + subject + "/chat"))
                                                    .count(), calls.toString()),
                                () -> assertTrue(calls.stream().noneMatch(c -> c.endsWith("/chat/turns")),
                                                    calls.toString()));
        }

        @ParameterizedTest(name = "new decline via {0}")
        @ValueSource(strings = {"n", "enter"})
        @DisplayName("new followed by n or Enter calls neither end nor open")
        void newDeclined(String reply) throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var emptyChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":[]}";
            var calls = new ArrayList<String>();
            fixture.start((request, response) -> {
                calls.add(request.method() + " " + request.target());
                if (request.target().endsWith("/chat") && "POST".equals(request.method())) {
                    response.json(200, emptyChat);
                } else {
                    response.send(404, null, "");
                }
            });
            virtual.type("new\n" + ("enter".equals(reply) ? "\n" : "n\n") + "q\n");
            virtual.endOfInput();
            var out = new StringWriter();

            int exitCode = WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false);

            assertAll(
                                () -> assertEquals(0, exitCode),
                                () -> assertTrue(out.toString().contains("Start a new chat?"), out.toString()),
                                () -> assertFalse(out.toString().contains("earlier chat is kept"), out.toString()),
                                () -> assertEquals(List.of("POST /api/v1/plans/" + subject + "/chat"), calls));
        }

        @Test
        @DisplayName("neu while a proposal is open in German prints the drop body naming kind and subject")
        void germanNeuWithOpenProposalShowsBody() throws Exception {
            var endedCalls = new AtomicBoolean();
            var openProposal = """
                                {"id":"C-1","subject":{"kind":"plan","id":"p"},
                                 "role":{"name":"replan","model":"sonnet"},"scope":"plan",
                                 "state":"chat.read-back","context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},
                                 "turns":[],
                                 "proposal":{"turn_id":"T-3","reply_turn_id":"T-2","kind":"revision",
                                   "summary":"revise","state":"open"}}
                                """;
            var fresh = """
                                {"id":"C-2","subject":{"kind":"plan","id":"p"},
                                 "role":{"name":"replan","model":"sonnet"},"scope":"plan",
                                 "state":"chat.turn","context":{"knows":[],"as_of":"2026-10-16T10:00:01Z"},
                                 "turns":[]}
                                """;
            fixture.start((request, response) -> {
                if (request.target().endsWith("/chat/end") && "POST".equals(request.method())) {
                    endedCalls.set(true);
                    response.json(200, fresh);
                } else if (request.target().endsWith("/chat") && "POST".equals(request.method())) {
                    response.json(200, endedCalls.get() ? fresh : openProposal);
                } else {
                    response.send(404, null, "");
                }
            });
            var out = new StringWriter();
            try (var germanTerminal = VirtualScreenTerminal.create(120, 30)) {
                var env = new HashMap<>(fixture.environment());
                env.put("LANG", "de_DE.UTF-8");
                var deContext = new OperatorContext(env, Files.createTempDirectory("pm-wd2-de"), true,
                                    ClientEnvironment.capture(env), germanTerminal::terminal, null);
                germanTerminal.type("neu\nj\nv\n");
                germanTerminal.endOfInput();

                int exitCode = WorkChatDialogue.run(deContext, new PrintWriter(out), "plan", "p", false, false);
                var text = out.toString();
                assertAll(
                                    () -> assertEquals(0, exitCode),
                                    () -> assertTrue(endedCalls.get()),
                                    () -> assertTrue(text.contains("Einen neuen Chat beginnen?"), text),
                                    () -> assertTrue(text.contains("Der offene Vorschlag (Änderung des Plans) wird verworfen"),
                                                        text),
                                    () -> assertTrue(text.contains("Chat verlassen; er bleibt bei p."), text));
            }
        }
    }
}
