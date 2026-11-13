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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Clock;
import java.util.HashMap;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The needs label of a row of {@code pm-operator status} (shared/texts-and-languages.adoc): the
 * runtime's kind word is resolved to the contract's kind before the label is read, so an alias
 * reads as its kind, and a word this version does not know reads as another reason — never as the
 * wire word and never as the text of a catalogue key that the word happens to name.
 */
@EnableGeneratorController
@EnabledOnOs({OS.MAC, OS.LINUX})
@DisplayName("The needs label of a row of the status tree")
class StatusPlanRowKindTest {

    private static final String PLAN_TITLE = "Console history of answers";
    private static final String EPIC_TITLE = "client work";
    private static final String OTHER_REASON = "another reason";

    private static final String STATUS_JSON = """
                        {
                          "status": "ready",
                          "version": "0.9.1",
                          "listener": "unix",
                          "running_since": "08:14",
                          "workers_count": 1,
                          "build_slots_used": 0,
                          "build_slots_total": 2
                        }
                        """;

    private static final String PROJECTS_JSON = """
                        {
                          "projects": [
                            {
                              "project_id": "pm-mcp-clients",
                              "display_name": "pm-mcp-clients",
                              "active_plans_count": 1,
                              "active_epics_count": 1,
                              "needs_operator_count": 1,
                              "needs_operator_summary": "decision",
                              "setup_needs_operator": false
                            }
                          ]
                        }
                        """;

    private static final String DETAIL_TEMPLATE = """
                        {
                          "project_id": "pm-mcp-clients",
                          "display_name": "pm-mcp-clients",
                          "plans": [
                            {
                              "plan_id": "pln-console-history",
                              "title": "%1$s",
                              "current_phase": "refine",
                              "phase_state": "waiting",
                              "needs_operator_kind": "%3$s",
                              "needs_operator_count": 1,
                              "tasks_done": 0,
                              "tasks_total": 0
                            }
                          ],
                          "epics": [
                            {
                              "epic_id": "epc-client-work",
                              "title": "%2$s",
                              "running_plans_count": 1,
                              "staged_plans_count": 0,
                              "needs_operator_kind": "%3$s",
                              "needs_operator_count": 1,
                              "plans": []
                            }
                          ],
                          "alongside_landings": []
                        }
                        """;

    private RuntimeFixture fixture;
    private StringWriter out;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    @ParameterizedTest(name = "{0} reads as {1}")
    @CsvSource({
                        "waiver_request, waiver",
                        "operator_wait, waits for you",
                        "title, another reason"
    })
    @DisplayName("names the plan row's kind by the resolved kind's label")
    void planRowNamesTheResolvedKind(String wireKind, String expectedLabel) throws Exception {
        serve(wireKind);

        var code = run("status");
        var output = out.toString();

        assertAll(
                            () -> assertEquals(0, code, output),
                            () -> assertTrue(output.contains("! " + expectedLabel), output),
                            () -> assertTrue(output.contains("plan  " + PLAN_TITLE), output),
                            () -> assertFalse(output.contains("! " + wireKind + " "), output),
                            () -> assertFalse(output.contains("What waits"), output)
        );
    }

    @ParameterizedTest(name = "{0} reads as {1}")
    @CsvSource({
                        "waiver_request, waiver",
                        "operator_wait, waits for you",
                        "title, another reason"
    })
    @DisplayName("names the epic row's kind by the resolved kind's label")
    void epicRowNamesTheResolvedKind(String wireKind, String expectedLabel) throws Exception {
        serve(wireKind);

        var code = run("status", "--by-epic");
        var output = out.toString();

        assertAll(
                            () -> assertEquals(0, code, output),
                            () -> assertTrue(output.contains("! " + expectedLabel), output),
                            () -> assertTrue(output.contains("epic  " + EPIC_TITLE), output),
                            () -> assertFalse(output.contains("! " + wireKind + " "), output),
                            () -> assertFalse(output.contains("What waits"), output)
        );
    }

    @Test
    @DisplayName("names a kind this version does not know as another reason, never by its word")
    void unknownKindReadsAsAnotherReason() throws Exception {
        var unknown = "zz" + Generators.letterStrings(8, 14).next();
        serve(unknown);

        var byPlan = run("status");
        var planOutput = out.toString();
        out.getBuffer().setLength(0);
        var byEpic = run("status", "--by-epic");
        var epicOutput = out.toString();

        assertAll(
                            () -> assertEquals(0, byPlan, planOutput),
                            () -> assertEquals(0, byEpic, epicOutput),
                            () -> assertTrue(planOutput.contains("! " + OTHER_REASON), planOutput),
                            () -> assertTrue(epicOutput.contains("! " + OTHER_REASON), epicOutput),
                            () -> assertFalse(planOutput.contains(unknown), planOutput),
                            () -> assertFalse(epicOutput.contains(unknown), epicOutput)
        );
    }

    private int run(String... args) {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var context = new OperatorContext(
                            env,
                            fixture.base().getParent(),
                            true,
                            ClientEnvironment.capture(env),
                            null,
                            _ -> {
                            },
                            (Clock) null
        );
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()), args);
    }

    private void serve(String wireKind) throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        fixture.writeToken("test-token");
        var detail = DETAIL_TEMPLATE.formatted(PLAN_TITLE, EPIC_TITLE, wireKind);
        fixture.start((request, response) -> {
            var path = request.target();
            if ("/api/v1/status".equals(path) || path.startsWith("/api/v1/status?")) {
                response.json(200, STATUS_JSON);
            } else if ("/api/v1/projects".equals(path)) {
                response.json(200, PROJECTS_JSON);
            } else if ("/api/v1/projects/pm-mcp-clients".equals(path)) {
                response.json(200, detail);
            } else {
                response.send(404, null, "{\"error\":\"not_found\"}");
            }
        });
    }
}
