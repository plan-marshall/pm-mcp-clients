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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.stream.Stream;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every read verb exits {@code 1} when the runtime refuses the read
 * ({@code doc/design/terminal-client/01-commands.adoc#exit-codes}).
 */
@DisplayName("A read verb exits 1 when the runtime refuses the read")
class ReadVerbExitCodesTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (fixture != null) {
            fixture.close();
        }
    }

    /** The read verbs and the arguments that make them read. */
    static Stream<Arguments> readVerbs() {
        return Stream.of(
                            Arguments.of("status", new String[]{"status"}),
                            Arguments.of("status --ended", new String[]{"status", "--ended"}),
                            Arguments.of("system", new String[]{"system"}),
                            Arguments.of("reports", new String[]{"reports"}),
                            Arguments.of("projects", new String[]{"projects"}),
                            Arguments.of("questions", new String[]{"questions"}),
                            Arguments.of("roles list", new String[]{"roles", "list"}),
                            Arguments.of("roles show", new String[]{"roles", "show", "planner"}),
                            Arguments.of("config", new String[]{"config"}),
                            Arguments.of("config show", new String[]{"config", "show"}),
                            Arguments.of("client status", new String[]{"client", "status"}),
                            Arguments.of("cleanup", new String[]{"cleanup"}),
                            Arguments.of("cleanup --kept", new String[]{"cleanup", "--kept"}),
                            Arguments.of("web devices list", new String[]{"web", "devices", "list"}),
                            Arguments.of("plan report", new String[]{"plan", "report", "fix-relay"}),
                            Arguments.of("plan", new String[]{"plan", "fix-relay"}),
                            Arguments.of("epic", new String[]{"epic", "fix-relay"}),
                            Arguments.of("epic findings", new String[]{"epic", "findings", "fix-relay"}),
                            Arguments.of("epic watches", new String[]{"epic", "watches", "fix-relay"}));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("readVerbs")
    @DisplayName("exits 1 on a refused read")
    void exitsOneOnARefusedRead(String label, String[] args) throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(500, null, "{\"error\":\"unavailable\"}"));

        var context = new OperatorContext(fixture.environment(), tempDir, true);
        int code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);

        assertEquals(1, code, label + " stdout: " + out + " stderr: " + err);
    }
}
