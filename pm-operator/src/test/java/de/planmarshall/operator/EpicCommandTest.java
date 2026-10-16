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
import java.time.Duration;

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
                                        () -> assertTrue(output.contains("Epics")),
                                        () -> assertTrue(output.contains("ep-001")),
                                        () -> assertTrue(output.contains("Hardening Relay")),
                                        () -> assertTrue(output.contains("ep-002"))
        );
    }

    @Test
    @DisplayName("epic add creates epic")
    void epicAdd() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/epics".equals(request.target()) && "POST".equals(request.method())) {
                response.send(201, null, "{\"id\":\"ep-new\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "add", "--title", "Refactor Auth", "--id", "ep-auth", "--description", "Desc");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ ep-auth has started"));

        var req = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(req.bodyText().contains("\"title\":\"Refactor Auth\""));
        assertTrue(req.bodyText().contains("\"id\":\"ep-auth\""));
    }

    @Test
    @DisplayName("epic add with project routes to project entries")
    void epicAddWithProject() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/my-proj/entries".equals(request.target()) && "POST".equals(request.method())) {
                response.send(200, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "add", "--project", "my-proj", "--adopt", "spec-auth");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ spec-auth has started"));
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
    @DisplayName("epic auto updates common files")
    void epicAutoCommonFiles() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/auto".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "auto", "ep-001", "--common-files", "5");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Common files allowed for ep-001: 5"));
    }

    @Test
    @DisplayName("epic decompose triggers decomposition")
    void epicDecompose() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/decompose".equals(request.target()) && "POST".equals(request.method())) {
                response.send(202, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "decompose", "ep-001", "--instruction", "re-split tasks");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ ep-001 decomposing"));
    }

    @Test
    @DisplayName("epic archive archives ended epic")
    void epicArchive() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/archive".equals(request.target()) && "POST".equals(request.method())) {
                response.send(200, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "archive", "ep-001");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ ep-001 archived"));
    }

    @Test
    @DisplayName("epic chat communicates with epic")
    void epicChat() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/chat".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"reply\":\"hi\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "chat", "ep-001", "--message", "status please");
        assertEquals(0, code);
        assertTrue(out.toString().contains("reply"));
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
