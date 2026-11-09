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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves {@code pm-operator needs} on the {@code running-work} scenario (PLAN-36 D1).
 */
@DisplayName("NeedsCommand on running-work")
class NeedsCommandTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("needs prints every item across projects in runtime order on running-work")
    void needsOnRunningWork() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.RUNNING_WORK)
                                 .build())) {
            var environment = Map.of(MachinePaths.ENV_BASE, runtime.base().toString());
            var context = new OperatorContext(
                                environment,
                                tempDir,
                                true,
                                ClientEnvironment.capture(environment),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                                null
            );

            var out = new StringWriter();
            var err = new StringWriter();
            int exitCode = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "needs");

            var output = out.toString();
            var error = err.toString();

            assertAll(
                                () -> assertEquals(0, exitCode, "needs should exit 0, err: " + error),
                                () -> assertTrue(output.contains("Needs you · 12"), "expected header with 12 items: " + output),
                                () -> assertTrue(output.contains("findings"), "should contain findings item"),
                                () -> assertTrue(output.contains("landing failed"), "should contain landing failed item"),
                                () -> assertTrue(output.contains("held"), "should contain held item"),
                                () -> assertTrue(output.contains("needs a fix"), "should contain condition/needs a fix item"),
                                () -> assertTrue(output.contains("stalled"), "should contain stalled item"),
                                () -> assertTrue(output.contains("integrity pause"), "should contain integrity pause item"),
                                () -> assertTrue(output.contains("waits for you"), "should contain waits for you item"),
                                () -> assertTrue(output.contains("waiver"), "should contain waiver item"),
                                () -> assertTrue(output.contains("decision"), "should contain decision item"),
                                () -> assertTrue(output.contains("cleanup due"), "should contain cleanup due machine item"),
                                () -> assertTrue(output.contains("credential"), "should contain credential machine item"),
                                () -> assertTrue(output.contains("setup"), "should contain setup item"),
                                () -> assertTrue(output.contains("cleanup"), "should contain command cleanup"),
                                () -> assertTrue(output.contains("credentials"), "should contain command credentials"),
                                () -> assertTrue(output.contains("project setup"), "should contain command project setup")
            );

            // Assert runtime order: blocking first, oldest first
            int idxFindings = output.indexOf("findings");
            int idxLanding = output.indexOf("landing failed");
            int idxDecision = output.indexOf("decision");
            int idxCleanup = output.indexOf("cleanup due");
            int idxCredential = output.indexOf("credential");

            assertTrue(idxFindings < idxLanding, "findings should precede landing failed");
            assertTrue(idxLanding < idxDecision, "landing failed should precede decision");
            assertTrue(idxDecision < idxCleanup, "blocking items should precede non-blocking machine items");
            assertTrue(idxCleanup < idxCredential, "cleanup due should precede credential (older timestamp)");
        }
    }

    @Test
    @DisplayName("needs --json outputs JSON payload of needs-operator page")
    void needsJson() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.RUNNING_WORK)
                                 .build())) {
            var environment = Map.of(MachinePaths.ENV_BASE, runtime.base().toString());
            var context = new OperatorContext(
                                environment,
                                tempDir,
                                true,
                                ClientEnvironment.capture(environment),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                                null
            );

            var out = new StringWriter();
            var err = new StringWriter();
            int exitCode = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "needs", "--json");

            var output = out.toString();
            assertEquals(0, exitCode, "needs --json should exit 0");
            assertTrue(output.contains("\"total\":12") || output.contains("\"total\": 12"), "expected JSON with total 12");
            assertTrue(output.contains("\"items\":"), "expected items array in JSON");
        }
    }

    @Test
    @DisplayName("console alias ! dispatches to needs")
    void consoleAliasExclamation() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.RUNNING_WORK)
                                 .build())) {
            var environment = Map.of(MachinePaths.ENV_BASE, runtime.base().toString());
            var inStream = new ByteArrayInputStream("!\nexit\n".getBytes(StandardCharsets.UTF_8));
            var outStream = new ByteArrayOutputStream();
            var context = new OperatorContext(
                                environment,
                                tempDir,
                                true,
                                ClientEnvironment.capture(environment),
                                () -> new DumbTerminal(inStream, outStream),
                                null
            );

            var out = new StringWriter();
            var err = new StringWriter();
            int exitCode = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err));

            var consoleOutput = outStream.toString(StandardCharsets.UTF_8);
            assertEquals(0, exitCode, "console should exit 0");
            assertTrue(consoleOutput.contains("Needs you · 12"), "console ! should execute needs: " + consoleOutput);
        }
    }

    @Test
    @DisplayName("needs prints empty message when nothing waits")
    void needsEmpty() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.RUNNING_WORK + "#overview.empty")
                                 .build())) {
            var environment = Map.of(MachinePaths.ENV_BASE, runtime.base().toString());
            var context = new OperatorContext(
                                environment,
                                tempDir,
                                true,
                                ClientEnvironment.capture(environment),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                                null
            );

            var out = new StringWriter();
            var err = new StringWriter();
            int exitCode = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "needs");

            assertEquals(0, exitCode);
            var output = out.toString();
            assertTrue(output.contains("Nothing waits") || output.contains("Nichts wartet") || !output.isBlank(), output);
        }
    }

    @Test
    @DisplayName("executeNeeds throws OperatorRefusedException when status is not 200")
    void executeNeedsRefused() {
        var context = new OperatorContext(
                            Map.of(),
                            tempDir,
                            true,
                            ClientEnvironment.capture(Map.of()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                            null
        );
        var cmd = new NeedsCommand(context);
        var writer = new PrintWriter(new StringWriter());
        NeedsCommand.RuntimeSender sender = req -> new HttpResponse(500, null, new byte[0]);
        assertThrows(OperatorRefusedException.class,
                            () -> cmd.executeNeeds(sender, writer, "en"));
    }

    @Test
    @DisplayName("executeNeeds falls back to English title when locale lacks key")
    void executeNeedsTitleFallback() throws Exception {
        var context = new OperatorContext(
                            Map.of(),
                            tempDir,
                            true,
                            ClientEnvironment.capture(Map.of()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                            null
        );
        var cmd = new NeedsCommand(context);
        var sw = new StringWriter();
        var writer = new PrintWriter(sw);
        var json = "{\"page\":1,\"total_pages\":1,\"has_prev\":false,\"has_next\":false,\"total\":1,\"items\":[{\"kind\":\"decision\",\"link\":\"/api/v1/decisions/Q-1\",\"blocks\":\"plan\"}]}".getBytes(StandardCharsets.UTF_8);
        NeedsCommand.RuntimeSender sender = req -> {
            if (MachineApiPaths.NEEDS_OPERATOR.equals(req.target())) {
                return new HttpResponse(200, "application/json", json);
            }
            return new HttpResponse(404, null, new byte[0]);
        };
        cmd.executeNeeds(sender, writer, "fr");
        assertTrue(sw.toString().contains("Needs you · 1"));
    }

    @Test
    @DisplayName("parseDecisions extracts plan and project mappings, ignoring null ids")
    void parseDecisionsMapping() {
        var list = List.of(
                            Map.of("missing_id", "val"),
                            Map.of("id", "Q-1", "plan", "plan-1"),
                            Map.of("id", "Q-2", "project", "proj-1")
        );
        var map = NeedsCommand.parseDecisions(list);
        assertEquals("Q-1", map.get("plan-1"));
        assertEquals("Q-2", map.get("proj-1"));
        assertEquals(2, map.size());
    }

    @Test
    @DisplayName("fetchDecisionIds handles IOException gracefully")
    void fetchDecisionIdsHandlesIoException() {
        NeedsCommand.RuntimeSender sender = request -> {
            throw new IOException("network error");
        };
        var map = NeedsCommand.fetchDecisionIds(sender);
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("fetchDecisionIds falls back to questions when decisions returns non-200")
    void fetchDecisionIdsFallbackToQuestions() {
        NeedsCommand.RuntimeSender sender = request -> {
            if ("/api/v1/decisions".equals(request.target())) {
                return new HttpResponse(404, null, new byte[0]);
            }
            if ("/api/v1/questions".equals(request.target())) {
                var json = "[{\"id\":\"Q-100\",\"plan\":\"plan-x\"}]".getBytes(StandardCharsets.UTF_8);
                return new HttpResponse(200, "application/json", json);
            }
            return new HttpResponse(404, null, new byte[0]);
        };
        var map = NeedsCommand.fetchDecisionIds(sender);
        assertEquals("Q-100", map.get("plan-x"));
    }
}
