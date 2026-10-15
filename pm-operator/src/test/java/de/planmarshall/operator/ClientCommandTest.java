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

@DisplayName("ClientCommand and DrainCommand")
class ClientCommandTest {

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
    @DisplayName("client status lists client bundles")
    void clientStatus() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/client/bundles".equals(request.target())) {
                response.json(200, """
                        [
                          {"name":"claude","status":"installed","version":"2.4.1"},
                          {"name":"codex","status":"available"}
                        ]
                        """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("client", "status");
        var output = out.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("Client Bundles:")),
                () -> assertTrue(output.contains("claude")),
                () -> assertTrue(output.contains("installed")),
                () -> assertTrue(output.contains("codex"))
        );
    }

    @Test
    @DisplayName("client install installs client bundle")
    void clientInstall() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/client/install".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("client", "install", "codex");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ Client bundle installation completed"))
        );
    }

    @Test
    @DisplayName("drain sends stop request with drain flag without starting runtime")
    void drainCommand() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/runtime/stop".equals(request.target()) && "POST".equals(request.method())) {
                response.json(202, "{\"draining\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("drain");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("runtime   draining"))
        );
    }

    @Test
    @DisplayName("drain reports not running when runtime unavailable")
    void drainWhenNotRunning() {
        // Without starting fake server or writing live record
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        int code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "drain");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("runtime   not running"))
        );
    }
}
