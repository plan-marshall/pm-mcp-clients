/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import io.nayuki.qrcodegen.QrCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.PmOperator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * A plan chat turn end to end on a real pseudo-terminal (PLAN-138, D1): {@code plan chat <id>}
 * opens the dialogue via {@code POST …/chat}, posts a B1 {@code …/chat/turns} body carrying the
 * client's own {@code client_turn_id} beside the message, and renders the reply it read.
 */
@EnableGeneratorController
@DisplayName("pm-operator plan chat turn against the recording runtime")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PlanChatIT {

    private static final String SCRIPT = "/usr/bin/script";

    /** The role's second reply, which the sent message draws. */
    private static final String REPLY = "Here is what I would change.";

    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class, QrCode.class);

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Test
    @DisplayName("a message is sent as a B1 turn carrying it, and the role's reply is rendered")
    void planChatTurn() throws Exception {
        assertTrue(Files.isExecutable(Path.of(SCRIPT)), SCRIPT + " is not executable");
        var message = "message-" + Generators.letterStrings(6, 12).next();
        var turnBodies = new CopyOnWriteArrayList<String>();
        writeLiveRecord();
        var emptyChat = """
                            {"id":"C-1","subject":{"kind":"plan","id":"fix-relay"},
                             "role":{"name":"replan","model":"sonnet"},"scope":"plan","state":"chat.turn",
                             "context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},"turns":[]}
                            """;
        fixture.start((request, response) -> {
            if ("POST".equals(request.method()) && request.target().endsWith("/chat/turns")) {
                turnBodies.add(request.bodyText());
                response.json(202, "{\"turn_id\":\"T-1\"}");
            } else if (request.target().endsWith("/chat")) {
                response.json(200, "POST".equals(request.method()) && turnBodies.isEmpty()
                                    ? emptyChat : chatJson(message));
            } else {
                response.send(404, null, "");
            }
        });

        var environment = new HashMap<>(fixture.environment());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "dialogue-host");
        environment.put("LANG", "en_US.UTF-8");
        environment.put("TERM", "xterm-256color");
        environment.put("PATH", "/usr/bin:/bin");
        environment.put("XDG_STATE_HOME", tempDir.resolve("xdg-state").toString());
        try (var session = PtySession.start(binary.with("plan", "chat", "fix-relay"),
                                 environment, Path.of("").toAbsolutePath(), 120, 30)) {
            assertTrue(session.awaitText("you  ›", DialogueHarness.TIMEOUT), session.rawOutput());
            session.send(message + "\n\n");
            assertTrue(session.awaitText(REPLY, DialogueHarness.TIMEOUT), session.screenText());
            session.send("q\n");
            assertTrue(session.awaitExit(DialogueHarness.TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
        }

        assertEquals(1, turnBodies.size(), "expected one chat turn, got " + turnBodies);
        var body = JsonTree.asObject(JsonTree.parse(turnBodies.getFirst()));
        var clientTurnId = body != null ? JsonTree.string(body, "client_turn_id") : null;
        assertAll(
                            () -> assertNotNull(body,
                                                "the turn's body must be a JSON object, got " + turnBodies.getFirst()),
                            () -> assertEquals(message, JsonTree.string(body, "text"),
                                                "the turn's body must carry the message: " + turnBodies.getFirst()),
                            () -> assertTrue(clientTurnId != null && !clientTurnId.isBlank(),
                                                "the turn's body must carry a non-blank client_turn_id: "
                                                                    + turnBodies.getFirst()));
    }

    /** @param message the operator's message @return the transcript the runtime serves for it */
    private static String chatJson(String message) {
        return """
                            {"id":"C-1","subject":{"kind":"plan","id":"fix-relay"},
                             "role":{"name":"replan","model":"sonnet"},"scope":"plan","state":"chat.turn",
                             "context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},"turns":[
                               {"turn_id":"T-1","kind":"message","author":"operator","status":"complete",
                                "format":"plain","untrusted":false,"text":"%s","steps":[],"refs":[],
                                "at":"2026-10-16T10:00:00Z"},
                               {"turn_id":"T-2","kind":"message","author":"replan","status":"complete",
                                "format":"markdown","untrusted":false,"model":"sonnet",
                                "text":"%s","steps":[],"refs":[],
                                "at":"2026-10-16T10:01:00Z"}]}
                            """.formatted(message, REPLY);
    }

    private void writeLiveRecord() throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
    }
}
