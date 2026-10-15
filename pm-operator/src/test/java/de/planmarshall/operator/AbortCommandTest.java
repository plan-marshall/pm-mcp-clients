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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.planmarshall.api.testsupport.RuntimeFixture;

@DisplayName("AbortCommand")
class AbortCommandTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private int execute(String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    @Test
    @DisplayName("abort plan on running plan cancels and archives")
    void abortRunningPlan() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-001".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "{\"id\":\"p-001\",\"state\":\"running\"}");
            } else if ("/api/v1/plans/p-001/cancel".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("abort", "plan", "p-001", "--reason", "Emergency stop");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ p-001 cancelled and archived"))
        );
    }

    @Test
    @DisplayName("abort plan on paused plan abandons and archives")
    void abortPausedPlan() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-002".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "{\"id\":\"p-002\",\"state\":\"paused\"}");
            } else if ("/api/v1/plans/p-002/abandon".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("abort", "plan", "p-002", "--reason", "Not continuing");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ p-002 abandoned and archived"))
        );
    }

    @Test
    @DisplayName("abort epic cancels epic")
    void abortEpic() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/cancel".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("abort", "epic", "ep-001", "--reason", "Epic superseded");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ ep-001 cancelled"))
        );
    }
}
