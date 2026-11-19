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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import picocli.CommandLine.Command;

/**
 * Checks exit codes of verbs across success, refusal, usage, and needs-input
 * as specified in {@code doc/design/terminal-client/01-commands.adoc#exit-codes}.
 */
@EnableGeneratorController
@DisplayName("Exit codes of operator verbs")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ExitCodesTest {

    private static final String RANDOM_ID = Generators.letterStrings(4, 10).next();

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        System.setProperty("pm.test.no.browser", "true");
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        System.clearProperty("pm.test.no.browser");
        if (fixture != null) {
            fixture.close();
        }
    }

    private int execute(String... args) {
        var context = new OperatorContext(
                            fixture.environment(),
                            tempDir,
                            true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                            _ -> {
                            }
        );
        return PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), args);
    }

    static List<String> registeredVerbs() {
        return Arrays.stream(PmOperator.class.getAnnotation(Command.class).subcommands())
                            .map(type -> type.getAnnotation(Command.class).name())
                            .toList();
    }

    @Nested
    @DisplayName("Exit code 2: Usage errors")
    class UsageErrors {

        @Test
        @DisplayName("Unknown top-level verb exits 2")
        void unknownVerbExitsTwo() {
            int code = execute("bogus");
            assertEquals(2, code, "stdout: " + out + " stderr: " + err);
        }

        @ParameterizedTest(name = "verb: {0}")
        @MethodSource("de.planmarshall.operator.ExitCodesTest#registeredVerbs")
        @DisplayName("Every registered verb exits 2 on an unknown option")
        void unknownOptionExitsTwo(String verb) {
            int code = execute(verb, "--unknown-option-xyz");
            assertEquals(2, code, "Verb " + verb + " did not exit 2. stdout: " + out + " stderr: " + err);
        }
    }

    @Nested
    @DisplayName("Exit code 1: Refusals and runtime errors")
    class Refusals {

        static Stream<Arguments> refusedCommandInvocations() {
            return Stream.of(
                                Arguments.of("status", new String[]{"status"}),
                                Arguments.of("system", new String[]{"system"}),
                                Arguments.of("plans", new String[]{"plans"}),
                                Arguments.of("plan", new String[]{"plan", RANDOM_ID}),
                                Arguments.of("epic", new String[]{"epic", RANDOM_ID}),
                                Arguments.of("questions", new String[]{"questions"}),
                                Arguments.of("answer", new String[]{"answer", RANDOM_ID, "1"}),
                                Arguments.of("reports", new String[]{"reports"}),
                                Arguments.of("projects", new String[]{"projects"}),
                                Arguments.of("project", new String[]{"project", RANDOM_ID}),
                                Arguments.of("roles list", new String[]{"roles", "list"}),
                                Arguments.of("roles show", new String[]{"roles", "show", "planner"}),
                                Arguments.of("config", new String[]{"config"}),
                                Arguments.of("config show", new String[]{"config", "show"}),
                                Arguments.of("client status", new String[]{"client", "status"}),
                                Arguments.of("cleanup", new String[]{"cleanup"}),
                                Arguments.of("cleanup --kept", new String[]{"cleanup", "--kept"}),
                                Arguments.of("find", new String[]{"find", RANDOM_ID}),
                                Arguments.of("lesson check", new String[]{"lesson", "check", RANDOM_ID}),
                                Arguments.of("epic findings", new String[]{"epic", "findings", RANDOM_ID}),
                                Arguments.of("epic watches", new String[]{"epic", "watches", RANDOM_ID}),
                                Arguments.of("plan findings", new String[]{"plan", "findings", RANDOM_ID}),
                                Arguments.of("plan watches", new String[]{"plan", "watches", RANDOM_ID}),
                                Arguments.of("plan settings", new String[]{"plan", "settings", RANDOM_ID})
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("refusedCommandInvocations")
        @DisplayName("Refused command exits 1 when runtime answers with an error")
        void refusedCommandExitsOne(String label, String[] args) throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> response.send(500, null,
                                "{\"error\":\"internal_error\",\"reason\":\"runtime failure\"}"));

            int code = execute(args);
            assertEquals(1, code, "Command " + label + " did not exit 1. stdout: " + out + " stderr: " + err);
        }

        @Test
        @DisplayName("plan with unknown id exits 1 with ScopeNotFound refusal")
        void planNotFoundExitsOne() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> response.send(404, null,
                                "{\"error\":\"scope_not_found\",\"reason\":\"plan not found\"}"));

            int code = execute("plan", "nope");
            assertEquals(1, code, "plan nope did not exit 1. stdout: " + out + " stderr: " + err);
        }
    }

    @Nested
    @DisplayName("Exit code 0: Success")
    class Success {

        @Test
        @DisplayName("-h and --help exit 0")
        void helpExitsZero() {
            assertAll(
                                () -> assertEquals(0, execute("-h")),
                                () -> assertEquals(0, execute("--help"))
            );
        }

        @Test
        @DisplayName("-V and --version exit 0")
        void versionExitsZero() {
            assertAll(
                                () -> assertEquals(0, execute("-V")),
                                () -> assertEquals(0, execute("--version"))
            );
        }

        @Test
        @DisplayName("status exits 0 when runtime is not running")
        void statusNotRunningExitsZero() {
            assertEquals(0, execute("status"));
        }
    }

    @Nested
    @DisplayName("Exit code 3: Choice not answered (--no-input)")
    class NeedsInput {

        @Test
        @DisplayName("Command with required choice stops with exit code 3 when --no-input is passed")
        void noInputExitsThree() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                if ("/api/v1/projects".equals(request.target())) {
                    response.json(200, "{\"projects\":[]}");
                } else {
                    response.send(404, null, "");
                }
            });
            int code = execute("project", "add", "--no-input");
            assertEquals(3, code, "stdout: " + out + " stderr: " + err);
        }
    }

    @Nested
    @DisplayName("Exit code 4: Operator security checks")
    class OperatorChecks {

        @Test
        @DisplayName("Act command in coding agent environment exits 4")
        void agentEnvironmentExitsFour() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var env = new HashMap<>(fixture.environment());
            env.put("CLAUDE_CODE", "1");
            var context = new OperatorContext(
                                env,
                                tempDir,
                                true,
                                ClientEnvironment.capture(env),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                                _ -> {
                                }
            );
            int code = PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), "runtime", "stop");
            assertEquals(4, code, "stdout: " + out + " stderr: " + err);
        }

        @Test
        @DisplayName("Act command on non-interactive terminal exits 4")
        void nonInteractiveTerminalExitsFour() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var context = new OperatorContext(
                                fixture.environment(),
                                tempDir,
                                false,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                                _ -> {
                                }
            );
            int code = PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), "runtime", "stop");
            assertEquals(4, code, "stdout: " + out + " stderr: " + err);
        }
    }
}
