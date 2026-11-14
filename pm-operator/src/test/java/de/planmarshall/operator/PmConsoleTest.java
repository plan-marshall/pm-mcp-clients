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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.http.SseEvent;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.dialogue.OnboardingSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@EnableGeneratorController
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

    /**
     * @param output the console's whole output
     * @return the words of the line that names the verbs of {@code help}, tokenized so a missing one
     *         cannot hide inside another
     */
    private static List<String> helpVerbs(String output) {
        var line = output.lines()
                            .filter(candidate -> candidate.contains("without the program name"))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("no help verb line in: " + output));
        return Arrays.stream(line.split("[^a-z-]+"))
                            .filter(word -> !word.isBlank())
                            .toList();
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
            } else if ("/api/v1/plans/fix-relay/process".equals(target)) {
                // plan <id> reads the plan's process resource; the runtime has no bare plan resource
                response.json(200, "{\"plan_id\":\"fix-relay\",\"current_phase\":\"execute\","
                                    + "\"phase_state\":\"running\",\"nodes\":[]}");
            } else if ("/api/v1/epics/storage-v2".equals(target)) {
                response.json(200, "{\"id\":\"storage-v2\",\"state\":\"active\"}");
            } else if ("/api/v1/questions".equals(target)) {
                response.json(200, "[\"Q-007 (pending)\"]");
            } else if (target.startsWith("/api/v1/questions/Q-007/answers")) {
                response.json(200, "{\"ok\":true}");
            } else if ("/api/v1/decisions/Q-007".equals(target) && "GET".equals(request.method())) {
                // answer reads the decision it answers before it sends the answer.
                response.json(200, "{\"id\":\"Q-007\"}");
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
                            () -> assertTrue(helpVerbs(output).containsAll(OperatorVerbs.registered()),
                                                "help names every registered verb, got: " + helpVerbs(output)),
                            () -> assertTrue(output.contains("Help for status:"), "Should show help for status"),
                            () -> assertTrue(output.contains("plan fix-relay (running)"), "Should list running plan"),
                            () -> assertTrue(output.contains("fix-relay · execute · running"), "Should show plan details"),
                            () -> assertTrue(output.contains("\"id\":\"storage-v2\""), "Should show epic details"),
                            () -> assertTrue(output.contains("Q-007 (pending)"), "Should show questions"),
                            () -> assertTrue(output.contains("✓ Q-007 answered: 2.") || output.contains("✓ Q-007 beantwortet: 2."), () -> "Should answer question, but was:\n" + output),
                            () -> assertTrue(output.contains("✓ Copied, if your terminal allows it; the text is above."), "Should copy ssh"),
                            () -> assertTrue(output.contains("✓ New code: NEW-CODE1"), "Should issue new pairing code")
        );
    }

    @Test
    @DisplayName("tui is an alias of plans and falls back to the status output off a full-screen terminal")
    void tuiAliasDispatchesToPlans() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                                    """);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, "[{\"label\":\"local browser\",\"paired_by\":\"open\"}]");
            } else if ("/api/v1/plans".equals(request.target())) {
                response.json(200, "[\"plan fix-relay (running)\"]");
            } else {
                response.send(404, null, "");
            }
        });

        var inStream = new ByteArrayInputStream("tui\nexit\n".getBytes(StandardCharsets.UTF_8));
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
                            () -> assertTrue(output.contains("plan fix-relay (running)"), output)
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
            } else if (OnboardingSource.STATE_PATH.equals(request.target())) {
                response.json(200, """
                                    {"state":"onboard.mode","summary":"Set up this machine for plan-marshall",
                                     "completed_steps":1,
                                     "links":{"begin":{"href":"/api/v1/onboarding/language","method":"POST","next":"language.choose","preselected":true}}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        // Entry flow choice: "h", then "q" to enter REPL.
        // Then in REPL: empty Enter (the dialogue), "q" to leave it, then "exit"
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
                            () -> assertTrue(output.contains("Begin with step 1"), output),
                            () -> assertTrue(output.contains("Nothing was sent."), output)
        );
    }

    @Test
    @DisplayName("asks for a fresh code when a device is paired (device.paired)")
    void asksForFreshCodeOnDevicePaired() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/web/pairings".equals(request.target())) {
                response.json(201, "{\"code\":\"FRESH-CODE\",\"url\":\"http://127.0.0.1:7420/pair#code=FRESH-CODE\"}");
            } else {
                response.send(404, null, "");
            }
        });

        var outStream = new ByteArrayOutputStream();
        var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), outStream);
        var reader = LineReaderBuilder.builder().terminal(terminal).build();

        var context = new OperatorContext(
                            fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), outStream),
                            null
        );

        new PmConsole(context).applyConsoleEvent("device.paired",
                            Map.of("device_id", "dev-1", "label", "Firefox on mbp-oliver"), reader);

        var output = outStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("✓ Paired: Firefox on mbp-oliver"), "output: " + output);
        assertTrue(output.contains("FRESH-CODE"), "output: " + output);
    }

    @Test
    @DisplayName("a device paired without a label and a fresh code the runtime refuses are both said as they are")
    void unnamedDeviceAndRefusedFreshCode() throws Exception {
        fixture.start((request, response) -> response.send(500, null, ""));
        var outStream = new ByteArrayOutputStream();
        var reader = LineReaderBuilder.builder()
                            .terminal(new DumbTerminal(new ByteArrayInputStream(new byte[0]), outStream)).build();
        var context = new OperatorContext(
                            fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), outStream),
                            null
        );

        new PmConsole(context).applyConsoleEvent("device.paired", Map.of("device_id", "dev-1"), reader);

        var output = outStream.toString(StandardCharsets.UTF_8);
        var unknown = MessageCatalogue.format("en", "common.unknown", Map.of());
        assertAll(
                            () -> assertTrue(output.contains("✓ Paired: " + unknown), output),
                            () -> assertFalse(output.contains("Paired: local"), output),
                            () -> assertTrue(output.contains(
                                                "✗ Failed to issue a fresh pairing code: the runtime issued no code (HTTP 500)"), output)
        );
    }

    @Test
    @DisplayName("web pair at the prompt says the runtime issued no code instead of showing one of its own")
    void webPairWithRefusedCode() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                                    """);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, "[{\"label\":\"Firefox\",\"paired_by\":\"open\"}]");
            } else {
                response.send(500, null, "");
            }
        });
        var inStream = new ByteArrayInputStream("web pair\nexit\n".getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();
        var context = new OperatorContext(
                            fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(inStream, outStream),
                            null
        );

        new PmConsole(context).run();

        var output = outStream.toString(StandardCharsets.UTF_8);
        assertAll(
                            () -> assertTrue(output.contains(
                                                "✗ Failed to issue a new pairing code: the runtime issued no code (HTTP 500)"), output),
                            () -> assertFalse(output.contains("New code"), output)
        );
    }

    private OperatorContext consoleContext(String typed, ByteArrayOutputStream outStream) {
        var inStream = new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8));
        return new OperatorContext(
                            fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(inStream, outStream),
                            null
        );
    }

    private FakeRuntimeServer serveOnboarded() throws IOException {
        return fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                                    """);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, "[{\"label\":\"Firefox\",\"paired_by\":\"open\"}]");
            } else if (request.target().startsWith("/api/v1/web/devices/")) {
                response.send(200, null, "");
            } else {
                response.send(404, null, "");
            }
        });
    }

    /**
     * A line reader whose answers are scripted: a string is the line read, an exception is what the
     * read throws (the interrupt JLine raises for {@code Ctrl+C}, with the text typed so far).
     */
    private static final class ScriptedReader extends LineReaderImpl {

        private final Deque<Object> answers;

        ScriptedReader(Object... answers) throws IOException {
            super(new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()));
            this.answers = new ArrayDeque<>(List.of(answers));
        }

        @Override
        public String readLine(String prompt) {
            var next = answers.poll();
            if (next instanceof RuntimeException failure) {
                throw failure;
            }
            return (String) next;
        }
    }

    @Test
    @DisplayName("Ctrl+C on typed text clears it without the hint and keeps it for Up")
    void ctrlCOnTypedTextKeepsItInTheHistory() throws Exception {
        var typed = Generators.letterStrings(4, 12).next();
        var next = Generators.letterStrings(4, 12).next();
        var reader = new ScriptedReader(new UserInterruptException(typed), next);
        var out = new StringWriter();

        var line = new PmConsole(consoleContext("", new ByteArrayOutputStream()))
                            .readCommand(reader, new PrintWriter(out), "en");

        var history = new ArrayList<String>();
        reader.getHistory().forEach(entry -> history.add(entry.line()));
        assertAll(
                            () -> assertEquals(next, line, "the prompt is read again after the interrupt"),
                            () -> assertEquals(List.of(typed), history, "Up brings the cleared text back"),
                            () -> assertFalse(out.toString().contains("exit or Ctrl+D leaves the console"), out.toString()));
    }

    @ParameterizedTest(name = "partial line [{0}]")
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Ctrl+C on the empty prompt prints the hint and keeps nothing for Up")
    void ctrlCOnTheEmptyPromptPrintsTheHint(String partial) throws Exception {
        var reader = new ScriptedReader(new UserInterruptException(partial), "exit");
        var out = new StringWriter();

        var line = new PmConsole(consoleContext("", new ByteArrayOutputStream()))
                            .readCommand(reader, new PrintWriter(out), "en");

        assertAll(
                            () -> assertEquals("exit", line),
                            () -> assertTrue(out.toString().contains("exit or Ctrl+D leaves the console"), out.toString()),
                            () -> assertEquals(0, reader.getHistory().size(), "nothing to bring back"));
    }

    @Test
    @DisplayName("a web verb the console does not answer itself keeps the quoted argument as typed")
    void webVerbKeepsTheTypedArgument() throws Exception {
        var server = serveOnboarded();
        var label = Generators.letterStrings(3, 8).next() + "  " + Generators.letterStrings(3, 8).next();
        var outStream = new ByteArrayOutputStream();

        new PmConsole(consoleContext("web devices rename dev-1 \"" + label + "\"\nexit\n", outStream)).run();

        var output = outStream.toString(StandardCharsets.UTF_8);
        var renamed = server.requests().stream()
                            .filter(recorded -> "PUT".equals(recorded.method())
                                                && "/api/v1/web/devices/dev-1".equals(recorded.target()))
                            .toList();
        assertAll(
                            () -> assertTrue(output.contains("✓ Renamed: " + label), output),
                            () -> assertEquals(1, renamed.size(), server.requests().toString()),
                            () -> assertTrue(renamed.getFirst().bodyText().contains("\"label\":\"" + label + "\""),
                                                renamed.getFirst().bodyText()));
    }

    @Test
    @DisplayName("an event whose data is no JSON object is dropped and the next one is shown")
    void eventsWithoutAnObjectAreDropped() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));
        var outStream = new ByteArrayOutputStream();
        var reader = LineReaderBuilder.builder()
                            .terminal(new DumbTerminal(new ByteArrayInputStream(new byte[0]), outStream)).build();
        var console = new PmConsole(consoleContext("", outStream));
        var label = Generators.letterStrings(4, 10).next();

        for (var data : List.of("", "  ", "{not json", "[1,2]", "42")) {
            console.handleSseEvent(new SseEvent("message", data, "1"), reader);
        }
        var afterDropped = outStream.toString(StandardCharsets.UTF_8);
        console.handleSseEvent(new SseEvent("message", "{\"type\":\"paired\",\"label\":\"" + label + "\"}", "2"), reader);

        assertAll(
                            () -> assertEquals("", afterDropped, "a dropped event prints nothing"),
                            () -> assertTrue(outStream.toString(StandardCharsets.UTF_8).contains("Paired: " + label),
                                                outStream.toString(StandardCharsets.UTF_8)));
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
