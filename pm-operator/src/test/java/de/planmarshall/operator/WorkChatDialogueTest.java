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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

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
            if ("GET".equals(request.method()) && request.target().endsWith("/chat")) {
                response.json(200, CHAT_PROPOSAL);
            } else {
                response.send(500, null, "");
            }
        });
        var input = Generators.letterStrings(8, 12).next();
        virtual.type(input + "\n?\nq\n");
        virtual.endOfInput();
        var out = new StringWriter();

        var followUp = WorkChatDialogue.run(context, new PrintWriter(out), "plan", "p", false, false);

        var text = out.toString();
        assertAll(
                            () -> assertNull(followUp),
                            () -> assertTrue(text.contains(input + " is not offered here: r, q, ?"), text),
                            () -> assertTrue(text.contains("Nothing more is explained here"), text),
                            () -> assertFalse(text.contains("is kept with"), text),
                            () -> assertEquals(0, server.requests().stream()
                                                                    .filter(recorded -> !"GET".equals(recorded.method())).count(),
                                                "nothing is sent: " + server.requests()));
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
            if (path.endsWith("/chat") && "GET".equals(request.method())) {
                int post = posts.get();
                response.json(200, post == 0 ? initial : (post == 1 ? afterFirst : afterSecond));
            } else if (path.endsWith("/chat/turns")) {
                int post = posts.incrementAndGet();
                response.json(202, "{\"turn_id\":\"" + (post == 1 ? "T-1" : "T-3") + "\"}");
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type(msg1 + "\n" + msg2 + "\n\n");
        virtual.endOfInput();
        var out = new StringWriter();

        var followUp = WorkChatDialogue.run(context, new PrintWriter(out), "plan", subject, false, false);

        var text = out.toString();
        var notice = subject + " proposes no change yet";
        var firstIdx = text.indexOf(notice);
        assertAll(
                            () -> assertNull(followUp),
                            () -> assertTrue(text.contains(reply1), text),
                            () -> assertTrue(text.contains(reply2), text),
                            () -> assertTrue(firstIdx >= 0, text),
                            () -> assertEquals(-1, text.indexOf(notice, firstIdx + notice.length()), text),
                            () -> assertTrue(text.contains("The chat is kept with " + subject), text));
    }

    private static String chatTurnJson(String id, String author, String text, String at) {
        return "{\"turn_id\":\"" + id + "\",\"kind\":\"message\",\"author\":\"" + author
                            + "\",\"status\":\"complete\",\"format\":\"plain\",\"untrusted\":false,\"text\":\""
                            + text + "\",\"steps\":[],\"refs\":[],\"at\":\"" + at + "\"}";
    }
}
