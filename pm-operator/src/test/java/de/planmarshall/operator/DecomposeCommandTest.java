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

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("DecomposeCommand")
class DecomposeCommandTest {

    private static final String BASE = "/api/v1/projects/pm-mcp-clients/epics/ep-001/decomposition";

    private static final String CUT_WITH_OVERLAP = """
                        {"epic_id":"relay-hardening","status":"check","revision":3,
                         "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                           "staged_plans":[{"plan_id":"relay-cursor-order","title":"relay-cursor-order",
                             "expected_surface":["pm-relay/src/cursor","pm-api/src/cursor"],"dependencies":[],
                             "status":"staged"}]},
                         {"id":"WS-2","name":"Authentication","order":2,
                           "staged_plans":[{"plan_id":"relay-auth-v2","title":"relay-auth-v2",
                             "objective":"a token exchange","expected_surface":["pm-api/src/cursor"],
                             "dependencies":[],"status":"staged"}]}],
                         "findings":[{"severity":"error","rule":"surface_overlap",
                           "affected_plans":["relay-cursor-order","relay-auth-v2"],
                           "message":"surface overlaps relay-auth-v2 (pm-api/src/cursor)"}]}
                        """;

    private static final String CUT_PASSING = """
                        {"epic_id":"relay-hardening","status":"review","revision":3,
                         "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                           "staged_plans":[{"plan_id":"relay-cursor-order","title":"relay-cursor-order",
                             "expected_surface":["pm-relay/src/cursor"],"dependencies":[],"status":"staged"}]}],
                         "findings":[]}
                        """;

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
    @DisplayName("epic decompose prints the workstreams, plans, and the failing check")
    void decomposePrintsTree() throws Exception {
        fixture.start((request, response) -> {
            if (BASE.equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, CUT_WITH_OVERLAP);
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "decompose", "ep-001");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("relay-hardening \u00b7 decomposition"), output),
                            () -> assertTrue(output.contains("WS-1")),
                            () -> assertTrue(output.contains("relay-auth-v2")),
                            () -> assertTrue(output.contains("1 check fails"), output)
        );
    }

    @Test
    @DisplayName("epic decompose drafts again with an instruction")
    void decomposeWithInstruction() throws Exception {
        var server = fixture.start((request, response) -> {
            if (BASE.equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, CUT_WITH_OVERLAP);
            } else if ((BASE + "/instruct").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, CUT_WITH_OVERLAP);
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "decompose", "ep-001", "--instruction", "split the token work off");
        assertEquals(0, code);
        server.awaitRequest(Duration.ofSeconds(1));
        var request = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(request.bodyText().contains("split the token work off"));
        assertTrue(request.bodyText().contains("\"rewrite\""));
    }

    @Test
    @DisplayName("epic review --agree commits a cut whose checks pass")
    void reviewAgreesAndCommits() throws Exception {
        fixture.start((request, response) -> {
            if ((BASE + "/check").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, CUT_PASSING);
            } else if ((BASE + "/commit").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"status\":\"committed\",\"ledger_revision\":4,\"launchable_plan_count\":1}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "review", "ep-001", "--agree");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("every check passes"), output),
                            () -> assertTrue(output.contains("relay-hardening: 1 workstream, 1 plan committed"), output)
        );
    }

    @Test
    @DisplayName("epic review --agree refuses a cut whose checks fail")
    void reviewRefusesFailingCut() throws Exception {
        fixture.start((request, response) -> {
            if ((BASE + "/check").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, CUT_WITH_OVERLAP);
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "review", "ep-001", "--agree");
        assertEquals(1, code);
        assertTrue(out.toString().contains("1 check fails"));
    }

    @Test
    @DisplayName("epic instruct shows the read-back, old against new")
    void instructShowsReadBack() throws Exception {
        var rewrite = """
                            {"epic_id":"relay-hardening","status":"read_back","revision":3,
                             "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                               "staged_plans":[{"plan_id":"relay-cursor-order","title":"relay-cursor-order",
                                 "expected_surface":["pm-relay/src/cursor","pm-api/src/cursor"],"dependencies":[],
                                 "status":"staged"}]},
                             {"id":"WS-2","name":"Authentication","order":2,
                               "staged_plans":[{"plan_id":"relay-auth-v2","title":"relay-auth-v2",
                                 "objective":"the exchange belongs into pm-relay",
                                 "expected_surface":["pm-relay/src/relay-auth-v2"],"dependencies":[],
                                 "status":"changed"}]}],
                             "findings":[]}
                            """;
        var server = fixture.start((request, response) -> {
            if (BASE.equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, CUT_WITH_OVERLAP);
            } else if ((BASE + "/instruct").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, rewrite);
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "instruct", "ep-001", "--plan", "relay-auth-v2", "--instruction",
                            "the exchange belongs into pm-relay");
        var output = out.toString();
        server.awaitRequest(Duration.ofSeconds(1));
        var request = server.awaitRequest(Duration.ofSeconds(1));
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("old against new"), output),
                            () -> assertTrue(output.contains("- Surface"), output),
                            () -> assertTrue(output.contains("+ Surface"), output),
                            () -> assertTrue(request.bodyText().contains("relay-auth-v2"))
        );
    }

    @Test
    @DisplayName("epic instruct requires words")
    void instructRequiresWords() throws Exception {
        var code = execute("epic", "instruct", "ep-001", "--plan", "relay-auth-v2");
        assertEquals(1, code);
        assertTrue(out.toString().contains("What should change?"));
    }
}
