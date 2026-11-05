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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
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
 * Every read verb exits {@code 1} when the runtime refuses the read, whatever the refusal says
 * ({@code doc/design/terminal-client/01-commands.adoc#exit-codes}): the runtime answers the
 * attempts it made before it gave up, the read that names no credential, and the read of a resource
 * it does not know with the same status the operator reads.
 */
@EnableGeneratorController
@DisplayName("A read verb exits 1 when the runtime refuses the read")
class ReadVerbExitCodesTest {

    private static final String PLAN_ID = Generators.letterStrings(4, 10).next();

    private static final List<Integer> REFUSALS = List.of(500, 401, 404);

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

    /**
     * Every read verb, against each status the runtime may refuse the read with.
     *
     * <p>The list is written by hand, and the reason is the registry: {@code @OperatorVerb} marks
     * every verb alike and says nothing about whether it reads or acts, so no reflection over the
     * command tree can produce the read population. What the hand list must not do is miss a read:
     * {@code project <id>} was the one it missed (CodeRabbit, PR 92), and it is here now.
     */
    static Stream<Arguments> refusedReads() {
        var reads = List.of(
                            Arguments.of("status", new String[]{"status"}),
                            Arguments.of("status --ended", new String[]{"status", "--ended"}),
                            Arguments.of("system", new String[]{"system"}),
                            Arguments.of("reports", new String[]{"reports"}),
                            Arguments.of("projects", new String[]{"projects"}),
                            Arguments.of("project", new String[]{"project", PLAN_ID}),
                            Arguments.of("questions", new String[]{"questions"}),
                            Arguments.of("roles list", new String[]{"roles", "list"}),
                            Arguments.of("roles show", new String[]{"roles", "show", "planner"}),
                            Arguments.of("config", new String[]{"config"}),
                            Arguments.of("config show", new String[]{"config", "show"}),
                            Arguments.of("client status", new String[]{"client", "status"}),
                            Arguments.of("cleanup", new String[]{"cleanup"}),
                            Arguments.of("cleanup --kept", new String[]{"cleanup", "--kept"}),
                            Arguments.of("web devices list", new String[]{"web", "devices", "list"}),
                            Arguments.of("plan report", new String[]{"plan", "report", PLAN_ID}),
                            Arguments.of("plan", new String[]{"plan", PLAN_ID}),
                            Arguments.of("plans", new String[]{"plans"}),
                            Arguments.of("lesson check", new String[]{"lesson", "check", PLAN_ID}),
                            Arguments.of("epic", new String[]{"epic", PLAN_ID}),
                            Arguments.of("epic findings", new String[]{"epic", "findings", PLAN_ID}),
                            Arguments.of("epic watches", new String[]{"epic", "watches", PLAN_ID}),
                            Arguments.of("plan findings", new String[]{"plan", "findings", PLAN_ID}),
                            Arguments.of("plan watches", new String[]{"plan", "watches", PLAN_ID}));
        return reads.stream().flatMap(read -> REFUSALS.stream()
                            .map(status -> Arguments.of(read.get()[0] + " answering " + status,
                                                (String[]) read.get()[1], status)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refusedReads")
    @DisplayName("exits 1 on a refused read")
    void exitsOneOnARefusedRead(String label, String[] args, Integer status) throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(status, null,
                            "{\"error\":\"unavailable\",\"reason\":\"" + request.target() + "\"}"));

        var context = new OperatorContext(fixture.environment(), tempDir, true);
        int code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);

        assertEquals(1, code, label + " stdout: " + out + " stderr: " + err);
    }
}
