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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
                                () -> assertEquals(List.of("POST /api/v1/plans/" + subject + "/chat"),
                                                    calls.stream().filter(c -> !c.endsWith("/events")).toList()));
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

    @Nested
    @DisplayName("streaming reply, s stop, and O9 line input (D3)")
    class StreamingReplyAndStop {

        @Test
        @DisplayName("opens SSE stream before reading chat and streams steps and lines above writing prompt")
        void streamOpenedBeforeChatAndSanitizesUntrusted() throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var requestTargets = new CopyOnWriteArrayList<String>();
            var eventsReady = new CountDownLatch(1);
            var turnPosted = new CountDownLatch(1);
            var done = new CountDownLatch(1);
            var streamHolder = new AtomicReference<FakeRuntimeServer.EventStream>();
            var emptyChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":[]}";
            var repliedChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":["
                                + chatTurnJson("T-1", "operator", "hello", "2026-10-16T10:00:00Z") + ","
                                + chatTurnJson("T-2", "replan", "Line one\\nPart two", "2026-10-16T10:00:02Z")
                                + "]}";
            fixture.start((request, response) -> {
                requestTargets.add(request.method() + " " + request.target());
                var path = request.target();
                if (path.equals("/api/v1/plans/" + subject + "/events")) {
                    try (var stream = response.events(200)) {
                        streamHolder.set(stream);
                        eventsReady.countDown();
                        assertTrue(turnPosted.await(5, TimeUnit.SECONDS));
                        stream.send("chat.turn.started", startedData(subject, "T-2", true));
                        stream.send("chat.turn.progress",
                                            eventData("chat.turn.progress", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-2",
                                                                                    "step", Map.of("key", "chat.step.read",
                                                                                                        "args", Map.of("target", "the outline")))));
                        stream.send("chat.turn.delta",
                                            eventData("chat.turn.delta", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-2",
                                                                                    "text", "Line one\nPart")));
                        awaitScreenContains(virtual, "s) stop the reply", "Line one");
                        assertFalse(screenText(virtual).contains("Part"),
                                            "partial line must not render before newline: " + screenText(virtual));
                        stream.send("chat.turn.delta",
                                            eventData("chat.turn.delta", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-2",
                                                                                    "text", " two \u001b[31mred\u001b[0m\n")));
                        stream.send("chat.turn.completed",
                                            eventData("chat.turn.completed", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-2",
                                                                                    "kind", "message", "status", "complete",
                                                                                    "untrusted", true, "text", "Line one\nPart two red",
                                                                                    "refs", List.of(), "lines", List.of())));
                        done.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                } else if (path.equals("/api/v1/plans/" + subject + "/chat")) {
                    response.json(200, turnPosted.getCount() == 0 ? repliedChat : emptyChat);
                } else if (path.equals("/api/v1/plans/" + subject + "/chat/turns")) {
                    response.json(202, "{\"turn_id\":\"T-1\"}");
                    turnPosted.countDown();
                } else {
                    response.send(404, null, "");
                }
            });
            var out = new StringWriter();
            var exitRef = new AtomicInteger(-1);
            var runner = Thread.ofPlatform().start(() -> {
                try {
                    exitRef.set(WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                } finally {
                    done.countDown();
                }
            });
            virtual.type("hello\n\n");
            assertTrue(eventsReady.await(5, TimeUnit.SECONDS));
            awaitScreenContains(virtual, "Line one", "Part two red");
            virtual.type("q\n");
            virtual.endOfInput();
            runner.join(5000);

            var screen = screenText(virtual);
            var combined = out + "\n" + screen;
            int eventsIdx = requestTargets.indexOf("GET /api/v1/plans/" + subject + "/events");
            int openIdx = requestTargets.indexOf("POST /api/v1/plans/" + subject + "/chat");
            assertAll(
                                () -> assertEquals(0, exitRef.get(), combined),
                                () -> assertTrue(eventsIdx >= 0 && openIdx > eventsIdx,
                                                    "SSE must open before chat: " + requestTargets),
                                () -> assertTrue(combined.contains("replan answers"), combined),
                                () -> assertTrue(combined.contains("reads the outline"), combined),
                                () -> assertTrue(screen.contains("Part two red"), screen),
                                () -> assertFalse(screen.contains("\u001b["), screen),
                                () -> assertTrue(screen.contains("\""), "untrusted turn must be quoted: " + screen),
                                () -> assertTrue(combined.contains("Left the chat; it stays with " + subject + "."),
                                                    combined));
        }

        @Test
        @DisplayName("s + Enter stops the reply and prints control.chat.stopped")
        void sKeyStopsReply() throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var stopped = new AtomicBoolean();
            var stopCalled = new CountDownLatch(1);
            var turnPosted = new CountDownLatch(1);
            var done = new CountDownLatch(1);
            var emptyChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":[]}";
            var stoppedChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":["
                                + chatTurnJson("T-1", "operator", "hi", "2026-10-16T10:00:00Z") + ","
                                + "{\"turn_id\":\"T-2\",\"kind\":\"message\",\"author\":\"replan\",\"status\":\"stopped\","
                                + "\"format\":\"markdown\",\"untrusted\":false,\"text\":\"Partial answer\",\"steps\":[],\"refs\":[],"
                                + "\"at\":\"2026-10-16T10:00:01Z\"}]}";
            fixture.start((request, response) -> {
                var path = request.target();
                if (path.equals("/api/v1/plans/" + subject + "/events")) {
                    try (var stream = response.events(200)) {
                        assertTrue(turnPosted.await(5, TimeUnit.SECONDS));
                        stream.send("chat.turn.started", startedData(subject, "T-2", false));
                        stream.send("chat.turn.delta",
                                            eventData("chat.turn.delta", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-2",
                                                                                    "text", "Partial answer\n")));
                        assertTrue(stopCalled.await(5, TimeUnit.SECONDS));
                        stream.send("chat.turn.completed",
                                            eventData("chat.turn.completed", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-2",
                                                                                    "kind", "message", "status", "stopped",
                                                                                    "untrusted", false, "text", "Partial answer",
                                                                                    "refs", List.of(), "lines", List.of())));
                        done.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                } else if (path.equals("/api/v1/plans/" + subject + "/chat/turns/T-2/stop")
                                    && "POST".equals(request.method())) {
                    stopped.set(true);
                    response.json(200, stoppedChat);
                    stopCalled.countDown();
                } else if (path.equals("/api/v1/plans/" + subject + "/chat")) {
                    response.json(200, stopped.get() ? stoppedChat : emptyChat);
                } else if (path.equals("/api/v1/plans/" + subject + "/chat/turns")) {
                    response.json(202, "{\"turn_id\":\"T-1\"}");
                    turnPosted.countDown();
                } else {
                    response.send(404, null, "");
                }
            });
            var out = new StringWriter();
            var exitRef = new AtomicInteger(-1);
            var runner = Thread.ofPlatform().start(() -> {
                try {
                    exitRef.set(WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                } finally {
                    done.countDown();
                }
            });
            virtual.type("hi\n\n");
            awaitScreenContains(virtual, "s) stop the reply", "Partial answer");
            virtual.type("s\n");
            awaitScreenContains(virtual, "Stopped; the reply so far is kept.");
            virtual.type("q\n");
            virtual.endOfInput();
            runner.join(5000);

            var combined = out + "\n" + screenText(virtual);
            assertAll(
                                () -> assertEquals(0, exitRef.get(), combined),
                                () -> assertTrue(stopped.get(), "stop route should be called"),
                                () -> assertTrue(combined.contains("Stopped; the reply so far is kept."), combined));
        }

        @Test
        @DisplayName("q + Enter while writing leaves without ending or stopping")
        void qWhileWritingLeavesImmediately() throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var turnPosted = new CountDownLatch(1);
            var left = new CountDownLatch(1);
            var emptyChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":[]}";
            var server = fixture.start((request, response) -> {
                var path = request.target();
                if (path.equals("/api/v1/plans/" + subject + "/events")) {
                    try (var stream = response.events(200)) {
                        assertTrue(turnPosted.await(5, TimeUnit.SECONDS));
                        stream.send("chat.turn.started", startedData(subject, "T-2", false));
                        left.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                } else if (path.equals("/api/v1/plans/" + subject + "/chat")) {
                    response.json(200, emptyChat);
                } else if (path.equals("/api/v1/plans/" + subject + "/chat/turns")) {
                    response.json(202, "{\"turn_id\":\"T-1\"}");
                    turnPosted.countDown();
                } else {
                    response.send(404, null, "");
                }
            });
            var out = new StringWriter();
            var exitRef = new AtomicInteger(-1);
            var runner = Thread.ofPlatform().start(() -> {
                try {
                    exitRef.set(WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
            virtual.type("hi\n\n");
            awaitScreenContains(virtual, "s) stop the reply");
            virtual.type("q\n");
            virtual.endOfInput();
            runner.join(5000);
            left.countDown();

            assertAll(
                                () -> assertEquals(0, exitRef.get(), out.toString()),
                                () -> assertTrue(out.toString().contains("Left the chat; it stays with " + subject + "."),
                                                    out.toString()),
                                () -> assertTrue(server.requests().stream().noneMatch(
                                                    req -> req.target().endsWith("/end") || req.target().endsWith("/stop")),
                                                    server.requests().toString()));
        }

        @Test
        @DisplayName("another line entered while writing is held and offered as the next first line")
        void heldLineOfferedAsNextFirstLine() throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var heldMessage = "held-" + Generators.letterStrings(5, 9).next();
            var turnTexts = new CopyOnWriteArrayList<String>();
            var firstPosted = new CountDownLatch(1);
            var heldTyped = new CountDownLatch(1);
            var secondPosted = new CountDownLatch(1);
            var done = new CountDownLatch(1);
            var emptyChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":[]}";
            var secondChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":["
                                + chatTurnJson("T-1", "operator", "first", "2026-10-16T10:00:00Z") + ","
                                + chatTurnJson("T-2", "replan", "done1", "2026-10-16T10:00:01Z") + "]}";
            fixture.start((request, response) -> {
                var path = request.target();
                if (path.equals("/api/v1/plans/" + subject + "/events")) {
                    try (var stream = response.events(200)) {
                        assertTrue(firstPosted.await(5, TimeUnit.SECONDS));
                        stream.send("chat.turn.started", startedData(subject, "T-2", false));
                        assertTrue(heldTyped.await(5, TimeUnit.SECONDS));
                        stream.send("chat.turn.completed",
                                            eventData("chat.turn.completed", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-2",
                                                                                    "kind", "message", "status", "complete",
                                                                                    "untrusted", false, "text", "done1",
                                                                                    "refs", List.of(), "lines", List.of())));
                        assertTrue(secondPosted.await(5, TimeUnit.SECONDS));
                        stream.send("chat.turn.started", startedData(subject, "T-4", false));
                        stream.send("chat.turn.completed",
                                            eventData("chat.turn.completed", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-4",
                                                                                    "kind", "message", "status", "complete",
                                                                                    "untrusted", false, "text", "done2",
                                                                                    "refs", List.of(), "lines", List.of())));
                        done.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                } else if (path.equals("/api/v1/plans/" + subject + "/chat")) {
                    response.json(200, turnTexts.isEmpty() ? emptyChat : secondChat);
                } else if (path.equals("/api/v1/plans/" + subject + "/chat/turns")) {
                    var parsed = JsonTree.parse(request.bodyText());
                    if (parsed instanceof Map<?, ?> map && map.get("text") instanceof String txt) {
                        turnTexts.add(txt);
                    }
                    if (turnTexts.size() == 1) {
                        response.json(202, "{\"turn_id\":\"T-1\"}");
                        firstPosted.countDown();
                    } else {
                        response.json(202, "{\"turn_id\":\"T-3\"}");
                        secondPosted.countDown();
                    }
                } else {
                    response.send(404, null, "");
                }
            });
            var out = new StringWriter();
            var exitRef = new AtomicInteger(-1);
            var runner = Thread.ofPlatform().start(() -> {
                try {
                    exitRef.set(WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                } finally {
                    done.countDown();
                }
            });
            virtual.type("first\n\n");
            awaitScreenContains(virtual, "s) stop the reply");
            virtual.type(heldMessage + "\n");
            heldTyped.countDown();
            awaitScreenContains(virtual, "done1", heldMessage);
            virtual.type("\n\n");
            awaitScreenContains(virtual, "done2");
            virtual.type("q\n");
            virtual.endOfInput();
            runner.join(5000);

            assertAll(
                                () -> assertEquals(0, exitRef.get(), out.toString()),
                                () -> assertEquals(List.of("first", heldMessage), turnTexts, turnTexts.toString()));
        }

        @Test
        @DisplayName("worker replacement prints restarted notice, and no-delta harness prints full reply on completion")
        void workerReplacementAndNoDeltas() throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var turnPosted = new CountDownLatch(1);
            var done = new CountDownLatch(1);
            var emptyChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":[]}";
            var repliedChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":["
                                + chatTurnJson("T-1", "operator", "go", "2026-10-16T10:00:00Z") + ","
                                + chatTurnJson("T-3", "replan", "Fresh full reply", "2026-10-16T10:00:02Z") + "]}";
            fixture.start((request, response) -> {
                var path = request.target();
                if (path.equals("/api/v1/plans/" + subject + "/events")) {
                    try (var stream = response.events(200)) {
                        assertTrue(turnPosted.await(5, TimeUnit.SECONDS));
                        stream.send("chat.turn.started", startedData(subject, "T-2", false));
                        stream.send("chat.turn.delta",
                                            eventData("chat.turn.delta", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-2",
                                                                                    "text", "Old draft\n")));
                        stream.send("chat.turn.started", startedData(subject, "T-3", false));
                        stream.send("chat.turn.completed",
                                            eventData("chat.turn.completed", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-3",
                                                                                    "kind", "message", "status", "complete",
                                                                                    "untrusted", false, "text", "Fresh full reply",
                                                                                    "refs", List.of(), "lines", List.of())));
                        done.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                } else if (path.equals("/api/v1/plans/" + subject + "/chat")) {
                    response.json(200, turnPosted.getCount() == 0 ? repliedChat : emptyChat);
                } else if (path.equals("/api/v1/plans/" + subject + "/chat/turns")) {
                    response.json(202, "{\"turn_id\":\"T-1\"}");
                    turnPosted.countDown();
                } else {
                    response.send(404, null, "");
                }
            });
            var out = new StringWriter();
            var exitRef = new AtomicInteger(-1);
            var runner = Thread.ofPlatform().start(() -> {
                try {
                    exitRef.set(WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                } finally {
                    done.countDown();
                }
            });
            virtual.type("go\n\n");
            awaitScreenContains(virtual, "The reply starts again.", "Fresh full reply");
            virtual.type("q\n");
            virtual.endOfInput();
            runner.join(5000);

            var combined = out + "\n" + screenText(virtual);
            assertAll(
                                () -> assertEquals(0, exitRef.get(), combined),
                                () -> assertTrue(combined.contains("The reply starts again."), combined),
                                () -> assertTrue(combined.contains("Fresh full reply"), combined));
        }

        @ParameterizedTest(name = "failed reply {0} prints {1}")
        @CsvSource({
                            "chat.reason.infeasible, cannot change that",
                            "chat.reason.lost, The reply was lost",
                            "chat.reason.too_long, too long"
        })
        @DisplayName("failed reply prints infeasible, lost, or too_long notice")
        void failedReplyPrintsNotice(String reasonKey, String expectedText) throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var turnPosted = new CountDownLatch(1);
            var done = new CountDownLatch(1);
            var emptyChat = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":[]}";
            fixture.start((request, response) -> {
                var path = request.target();
                if (path.equals("/api/v1/plans/" + subject + "/events")) {
                    try (var stream = response.events(200)) {
                        assertTrue(turnPosted.await(5, TimeUnit.SECONDS));
                        stream.send("chat.turn.started", startedData(subject, "T-2", false));
                        stream.send("chat.turn.completed",
                                            eventData("chat.turn.completed", subject,
                                                                Map.of("chat_id", "C-1", "turn_id", "T-2",
                                                                                    "kind", "message", "status", "failed",
                                                                                    "untrusted", false, "text", "",
                                                                                    "reason", Map.of("key", reasonKey,
                                                                                                        "args", Map.of("role", "replan", "reason", "busy")),
                                                                                    "refs", List.of(), "lines", List.of())));
                        done.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                } else if (path.equals("/api/v1/plans/" + subject + "/chat")) {
                    response.json(200, emptyChat);
                } else if (path.equals("/api/v1/plans/" + subject + "/chat/turns")) {
                    response.json(202, "{\"turn_id\":\"T-1\"}");
                    turnPosted.countDown();
                } else {
                    response.send(404, null, "");
                }
            });
            var out = new StringWriter();
            var exitRef = new AtomicInteger(-1);
            var runner = Thread.ofPlatform().start(() -> {
                try {
                    exitRef.set(WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                } finally {
                    done.countDown();
                }
            });
            virtual.type("try\n\n");
            awaitScreenContains(virtual, expectedText);
            virtual.type("q\n");
            virtual.endOfInput();
            runner.join(5000);

            var combined = out + "\n" + screenText(virtual);
            assertAll(
                                () -> assertEquals(0, exitRef.get(), combined),
                                () -> assertTrue(combined.contains(expectedText), combined));
        }

        @Test
        @DisplayName("failing readChat after turn was recorded keeps the last snapshot instead of throwing")
        void failingReadChatAfterTurnKeepsLastSnapshot() throws Exception {
            var subject = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var gets = new AtomicInteger();
            var snapshot = "{\"id\":\"C-1\",\"subject\":{\"kind\":\"plan\",\"id\":\"" + subject
                                + "\"},\"role\":{\"name\":\"replan\",\"model\":\"sonnet\"},\"scope\":\"plan\","
                                + "\"state\":\"chat.turn\",\"context\":{\"knows\":[],\"as_of\":\"2026-10-16T10:00:00Z\"},"
                                + "\"turns\":["
                                + chatTurnJson("T-1", "operator", "hi", "2026-10-16T10:00:00Z") + ","
                                + "{\"turn_id\":\"T-2\",\"kind\":\"message\",\"author\":\"replan\",\"status\":\"streaming\","
                                + "\"format\":\"markdown\",\"untrusted\":false,\"text\":\"still writing\",\"steps\":[],\"refs\":[],"
                                + "\"at\":\"2026-10-16T10:00:01Z\"}]}";
            fixture.start((request, response) -> {
                var path = request.target();
                if (path.endsWith("/chat/turns")) {
                    response.json(202, "{\"turn_id\":\"T-1\"}");
                } else if (path.endsWith("/chat")) {
                    if ("POST".equals(request.method()) || gets.incrementAndGet() == 1) {
                        response.json(200, snapshot);
                    } else {
                        response.json(503, "{\"error\":\"unavailable\"}");
                    }
                } else {
                    response.send(404, null, "");
                }
            });
            virtual.type("hi\n\nq\n");
            virtual.endOfInput();
            var out = new StringWriter();

            int exitCode = WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false);

            assertAll(
                                () -> assertEquals(0, exitCode, out.toString()),
                                () -> assertTrue(out.toString().contains("Left the chat; it stays with " + subject + "."),
                                                    out.toString()));
        }

        @Test
        @DisplayName("running-work mock runtime streams ordered reply over SSE")
        void mockRuntimeOrderedStreamsReply() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var env = new HashMap<>(runtime.environment());
                env.put("LANG", "en_US.UTF-8");
                var self = ProcessHandle.current();
                Files.writeString(runtime.paths().runtimeRecord(),
                                    "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                                                        + self.info().startInstant().orElseThrow() + "\"}");
                var mockContext = new OperatorContext(env, Files.createTempDirectory("pm-wd3-mock"), true,
                                    ClientEnvironment.capture(env), virtual::terminal, null);
                var out = new StringWriter();
                var exitRef = new AtomicInteger(-1);
                var runner = Thread.ofPlatform().start(() -> {
                    try {
                        exitRef.set(WorkChatDialogue.run(mockContext, new PrintWriter(out),
                                            "plan", "relay-auth-v2", false, false));
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
                virtual.type("Please check\n\n");
                awaitScreenContains(virtual, "answers");
                virtual.type("q\n");
                virtual.endOfInput();
                runner.join(5000);

                var combined = out + "\n" + screenText(virtual);
                assertAll(
                                    () -> assertEquals(0, exitRef.get(), combined),
                                    () -> assertTrue(combined.contains("Left the chat; it stays with relay-auth-v2."),
                                                        combined));
            }
        }

        private static String startedData(String subjectId, String turnId, boolean untrusted) {
            return eventData("chat.turn.started", subjectId, Map.of(
                                "chat_id", "C-1",
                                "turn_id", turnId,
                                "kind", "message",
                                "author", "replan",
                                "format", "markdown",
                                "untrusted", untrusted,
                                "model", "sonnet",
                                "context", Map.of("knows", List.of(), "as_of", "2026-10-16T10:00:00Z")));
        }

        private static String eventData(String eventName, String subjectId, Map<String, Object> payload) {
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("event", eventName);
            map.put("at", "2026-10-16T10:00:01Z");
            map.put("scope", Map.of("kind", "plan", "id", subjectId));
            map.putAll(payload);
            return JsonTree.write(map);
        }

        private static String screenText(VirtualScreenTerminal term) {
            return String.join("\n", term.screen().lines());
        }

        private static void awaitScreenContains(VirtualScreenTerminal term, String... expected)
                            throws InterruptedException {
            var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                var screen = screenText(term);
                if (java.util.Arrays.stream(expected).allMatch(screen::contains)) {
                    return;
                }
                Thread.sleep(20);
            }
        }
    }
}
