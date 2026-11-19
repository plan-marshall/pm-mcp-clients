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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * One stream rule: a refusal of a verb run as one shell command goes to stderr, and the verbs that
 * already printed there keep doing so. The interactive console keeps its one stream and is covered
 * by its own transcript tests.
 */
@DisplayName("a refusal of a one-shot verb goes to stderr")
@EnabledOnOs({OS.MAC, OS.LINUX})
class RefusalStreamTest {

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
        fixture.close();
    }

    private int execute(String... args) {
        var context = new OperatorContext(
                            fixture.environment(),
                            tempDir,
                            true,
                            ClientEnvironment.capture(fixture.environment()),
                            null,
                            _ -> {
                            }
        );
        return PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), args);
    }

    private void refuseEverything() throws IOException {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(404, null,
                            "{\"error\":\"scope_not_found\",\"message\":\"nothing carries this id\"}"));
    }

    @Test
    @DisplayName("plan <unknown id> prints its refusal on stderr and nothing on stdout")
    void planRefusalIsOnStderr() throws IOException {
        refuseEverything();
        assertEquals(1, execute("plan", "nope"));
        assertTrue(err.toString().contains("\u2717 This entry does not exist"), err.toString());
        assertFalse(out.toString().contains("This entry does not exist"), out.toString());
    }

    @Test
    @DisplayName("epic <unknown id> prints its refusal on stderr and nothing on stdout")
    void epicRefusalIsOnStderr() throws IOException {
        refuseEverything();
        assertEquals(1, execute("epic", "nope"));
        assertTrue(err.toString().contains("\u2717 This entry does not exist"), err.toString());
        assertFalse(out.toString().contains("This entry does not exist"), out.toString());
    }

    @Test
    @DisplayName("config cleanup prints its refusal on stderr and nothing on stdout")
    void configCleanupRefusalIsOnStderr() throws IOException {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(500, null, ""));

        assertEquals(1, execute("config", "cleanup"));
        assertTrue(err.toString().contains("\u2717"), err.toString());
        assertFalse(out.toString().contains("\u2717"), out.toString());
    }

    @Test
    @DisplayName("a verb that already printed its refusal on stderr keeps doing so (the positive control)")
    void refusalThatAlreadyWentToStderrStays() throws IOException {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(500, null,
                            "{\"error\":\"internal_error\",\"message\":\"the runtime failed\"}"));

        assertEquals(1, execute("roles", "list"));
        assertTrue(err.toString().contains("the runtime failed"), err.toString());
        assertFalse(out.toString().contains("the runtime failed"), out.toString());
    }
}
