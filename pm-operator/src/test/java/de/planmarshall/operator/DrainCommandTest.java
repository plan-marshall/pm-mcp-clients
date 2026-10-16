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
import java.util.concurrent.atomic.AtomicBoolean;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("DrainCommand")
class DrainCommandTest {

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
    @DisplayName("drain reports not running when runtime is absent")
    void drainNotRunning() {
        int code = execute("drain");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("runtime   not running"))
        );
    }

    @Test
    @DisplayName("drain triggers graceful drain")
    void drainRunning() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var called = new AtomicBoolean(false);

        fixture.start((request, response) -> {
            if ("/api/v1/runtime/stop".equals(request.target()) && "POST".equals(request.method())) {
                called.set(true);
                response.json(200, "{\"lifecycle_state\":\"draining\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("drain");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(called.get()),
                            () -> assertTrue(out.toString().contains("runtime   draining"))
        );
    }

    @Test
    @DisplayName("drain --withdraw withdraws graceful drain")
    void drainWithdraw() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var called = new AtomicBoolean(false);

        fixture.start((request, response) -> {
            if ("/api/v1/runtime/stop".equals(request.target()) && "DELETE".equals(request.method())) {
                called.set(true);
                response.json(200, "{\"lifecycle_state\":\"running\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("drain", "--withdraw");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(called.get()),
                            () -> assertTrue(out.toString().contains("runtime   drain withdrawn"))
        );
    }

    @Test
    @DisplayName("drain --withdraw handles inactive drain (409)")
    void drainWithdrawInactive() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);

        fixture.start((request, response) -> {
            if ("/api/v1/runtime/stop".equals(request.target()) && "DELETE".equals(request.method())) {
                response.send(409, null, "{\"error\":\"Not draining\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("drain", "--withdraw");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("runtime   drain not active"))
        );
    }
}
