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

@DisplayName("EpicCommand")
class EpicCommandTest {

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
    @DisplayName("epic list lists running epics")
    void epicList() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                        [
                          {"id":"ep-001","state":"running","title":"Hardening Relay"},
                          {"id":"ep-002","state":"staged","title":"Storage Migration"}
                        ]
                        """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic");
        var output = out.toString();
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(output.contains("Epics:")),
                () -> assertTrue(output.contains("ep-001")),
                () -> assertTrue(output.contains("Hardening Relay")),
                () -> assertTrue(output.contains("ep-002"))
        );
    }

    @Test
    @DisplayName("epic next triggers next plan")
    void epicNext() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/next".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "next", "ep-001", "--raise-scope");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ Next plan for ep-001 started"))
        );
    }

    @Test
    @DisplayName("epic auto updates auto mode")
    void epicAuto() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/auto".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "auto", "ep-001", "on");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ Automatic for ep-001: on"))
        );
    }

    @Test
    @DisplayName("epic cancel cancels epic")
    void epicCancel() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/cancel".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "cancel", "ep-001", "--reason", "Scope cancelled");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ ep-001 cancelled"))
        );
    }
}
