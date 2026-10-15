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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.impl.DumbTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.planmarshall.api.testsupport.RuntimeFixture;

@DisplayName("PmConsole")
class PmConsoleTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Test
    @DisplayName("exits cleanly with code 0 on 'exit'")
    void exitsOnExitCommand() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                        {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                        """);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, "[{\"label\":\"local browser\",\"paired_by\":\"open\"}]");
            } else {
                response.send(404, null, "");
            }
        });

        var inStream = new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();

        var context = new OperatorContext(
                fixture.environment(), tempDir, true,
                ClientEnvironment.capture(fixture.environment()),
                () -> new DumbTerminal(inStream, outStream),
                null
        );

        int exitCode = new PmConsole(context).run();

        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("runs help, status, plans, and web commands in console REPL")
    void replCommandsExecution() throws Exception {
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/status".equals(target)) {
                response.json(200, """
                        {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                        """);
            } else if ("/api/v1/web/devices".equals(target)) {
                response.json(200, "[{\"label\":\"local browser\",\"paired_by\":\"open\"}]");
            } else if ("/api/v1/plans".equals(target)) {
                response.json(200, "[\"plan fix-relay (running)\"]");
            } else if ("/api/v1/plans/fix-relay".equals(target)) {
                response.json(200, "{\"id\":\"fix-relay\",\"state\":\"running\"}");
            } else if ("/api/v1/epics/storage-v2".equals(target)) {
                response.json(200, "{\"id\":\"storage-v2\",\"state\":\"active\"}");
            } else if ("/api/v1/questions".equals(target)) {
                response.json(200, "[\"Q-007 (pending)\"]");
            } else if (target.startsWith("/api/v1/questions/Q-007/answers")) {
                response.json(200, "{\"ok\":true}");
            } else if ("/api/v1/web/pairings".equals(target)) {
                response.json(201, "{\"code\":\"NEW-CODE1\",\"url\":\"http://127.0.0.1:7420/pair#code=NEW-CODE1\"}");
            } else {
                response.send(404, null, "");
            }
        });

        var commands = String.join("\n",
                "help",
                "help status",
                "status",
                "plans",
                "plan fix-relay",
                "epic storage-v2",
                "questions",
                "answer Q-007 2",
                "web copy ssh",
                "web qr",
                "web pair",
                "exit\n"
        );

        var inStream = new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();

        var context = new OperatorContext(
                fixture.environment(), tempDir, true,
                ClientEnvironment.capture(fixture.environment()),
                () -> new DumbTerminal(inStream, outStream),
                null
        );

        int exitCode = new PmConsole(context).run();
        var output = outStream.toString(StandardCharsets.UTF_8);

        assertAll(
                () -> assertEquals(0, exitCode),
                () -> assertTrue(output.contains("Commands:"), "Should show help list"),
                () -> assertTrue(output.contains("Help for status:"), "Should show help for status"),
                () -> assertTrue(output.contains("plan fix-relay (running)"), "Should list running plan"),
                () -> assertTrue(output.contains("\"id\":\"fix-relay\""), "Should show plan details"),
                () -> assertTrue(output.contains("\"id\":\"storage-v2\""), "Should show epic details"),
                () -> assertTrue(output.contains("Q-007 (pending)"), "Should show questions"),
                () -> assertTrue(output.contains("✓ Answered Q-007 with option 2."), "Should answer question"),
                () -> assertTrue(output.contains("✓ Copied, if your terminal allows it; the text is above."), "Should copy ssh"),
                () -> assertTrue(output.contains("✓ New code: NEW-CODE1"), "Should issue new pairing code")
        );
    }

    @Test
    @DisplayName("empty Enter continues onboarding when setup badge is present")
    void emptyEnterContinuesSetup() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                        {"status":"onboarding","onboarded":false,"version":"0.9.0","onboard":{"step":1},"web":{"enabled":true,"port":7420}}
                        """);
            } else {
                response.send(404, null, "");
            }
        });

        // Entry flow choice: "h", then "q" to enter REPL.
        // Then in REPL: empty Enter ("\n"), then "q" to abort continuation, then "exit"
        var input = "h\nq\n\nq\nexit\n";
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();

        var context = new OperatorContext(
                fixture.environment(), tempDir, true,
                ClientEnvironment.capture(fixture.environment()),
                () -> new DumbTerminal(inStream, outStream),
                null
        );

        int exitCode = new PmConsole(context).run();
        var output = outStream.toString(StandardCharsets.UTF_8);

        assertAll(
                () -> assertEquals(0, exitCode),
                () -> assertTrue(output.contains("Left the setup before step 1. Nothing was sent. Enter continues the setup."))
        );
    }

    @Test
    @DisplayName("readPassword masks input with '*' character")
    void readPasswordMasksInput() throws Exception {
        var inStream = new ByteArrayInputStream("my-secret-token\n".getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();
        var terminal = new DumbTerminal(inStream, outStream);
        var reader = LineReaderBuilder.builder().terminal(terminal).build();

        var secret = PmConsole.readPassword(reader, "Enter token: ");

        assertEquals("my-secret-token", secret);
    }

    @Test
    @DisplayName("creates and persists command history file")
    void createsHistoryFile() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                        {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                        """);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, "[]");
            } else if ("/api/v1/web/pairings".equals(request.target())) {
                response.json(201, "{\"code\":\"HIST-CODE\",\"url\":\"http://127.0.0.1:7420/pair#code=HIST-CODE\"}");
            } else {
                response.send(404, null, "");
            }
        });

        var inStream = new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();

        var context = new OperatorContext(
                fixture.environment(), tempDir, true,
                ClientEnvironment.capture(fixture.environment()),
                () -> new DumbTerminal(inStream, outStream),
                null
        );

        new PmConsole(context).run();

        var historyFile = context.stateDirectory().historyFile();
        assertTrue(Files.exists(historyFile), "History file should exist");
    }
}
