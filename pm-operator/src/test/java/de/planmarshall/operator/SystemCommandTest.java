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

@DisplayName("SystemCommand")
class SystemCommandTest {

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
    @DisplayName("system reports not running when runtime record is absent")
    void systemNotRunning() {
        int code = execute("system");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("runtime   not running"))
        );
    }

    @Test
    @DisplayName("system prints system view when running")
    void systemRunning() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/system".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {
                                      "version": "pm-mcpd 0.9.1",
                                      "build": "2026-10-02 (a1b2c3d)",
                                      "uptime": "today 08:14 · 6 h 12 min",
                                      "memory": "412 MB in use of 1 GB",
                                      "work": "plans 3 running · epics 1 running",
                                      "workers": "5 · 3 busy · 2 idle",
                                      "jobs": "build 1 of 2 slots · locks 3 held",
                                      "cleanup": "due: 12 plans"
                                    }
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("system");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("System")),
                            () -> assertTrue(output.contains("pm-mcpd 0.9.1")),
                            () -> assertTrue(output.contains("412 MB in use of 1 GB")),
                            () -> assertTrue(output.contains("plans 3 running"))
        );
    }
}
