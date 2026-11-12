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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.operator.overview.OverviewFormatting;

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
                                 .timing(MockTiming.ORDERED)
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
                                () -> assertTrue(output.contains(MessageCatalogue.format("en", "operator.needs.title",
                                                    Map.of("total", 18))), "expected header with 18 items: " + output),
                                () -> assertTrue(output.contains("cleanup"), "should contain command cleanup"),
                                () -> assertTrue(output.contains("credentials"), "should contain command credentials"),
                                () -> assertTrue(output.contains("project setup"), "should contain command project setup")
            );
            // Every kind the scenario serves is named by the catalogue's own label of its row.
            for (var kind : NeedsOperatorKind.values()) {
                if (kind != NeedsOperatorKind.OTHER) {
                    assertTrue(output.contains(rowLabel(kind)), "should contain the " + kind + " row: " + output);
                }
            }

            // Assert runtime order: blocking first, oldest first
            int idxFindings = output.indexOf(rowLabel(NeedsOperatorKind.FINDINGS));
            int idxLanding = output.indexOf(rowLabel(NeedsOperatorKind.LANDING_FAILED));
            int idxDecision = output.indexOf(rowLabel(NeedsOperatorKind.DECISION));
            int idxCleanup = output.indexOf(rowLabel(NeedsOperatorKind.CLEANUP_DUE));
            int idxCredential = output.indexOf(rowLabel(NeedsOperatorKind.CREDENTIAL));

            assertTrue(idxFindings < idxLanding, "findings should precede landing failed");
            assertTrue(idxLanding < idxDecision, "landing failed should precede decision");
            assertTrue(idxDecision < idxCleanup, "blocking items should precede non-blocking machine items");
            assertTrue(idxCleanup < idxCredential, "cleanup due should precede credential (older timestamp)");
        }
    }

    /**
     * @param kind the kind of a row
     * @return the start of the row as the list prints it: the mark and the catalogue's label
     */
    private static String rowLabel(NeedsOperatorKind kind) {
        return "! " + OverviewFormatting.formatNeedsItemKind(kind, "en");
    }

    @Test
    @DisplayName("needs --json outputs JSON payload of needs-operator page")
    void needsJson() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.RUNNING_WORK)
                                 .timing(MockTiming.ORDERED)
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
            assertTrue(output.contains("\"total\":18") || output.contains("\"total\": 18"), "expected JSON with total 18");
            assertTrue(output.contains("\"items\":"), "expected items array in JSON");
        }
    }

    @Test
    @DisplayName("console alias ! dispatches to needs")
    void consoleAliasExclamation() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.RUNNING_WORK)
                                 .timing(MockTiming.ORDERED)
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
            assertTrue(consoleOutput.contains("Needs you · 18"), "console ! should execute needs: " + consoleOutput);
        }
    }

    @Test
    @DisplayName("needs prints empty message when nothing waits")
    void needsEmpty() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.RUNNING_WORK + "#overview.empty")
                                 .timing(MockTiming.ORDERED)
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
            var output = out.toString().strip();
            assertEquals(MessageCatalogue.format("en", "operator.needs.empty", Map.of()), output,
                                "an empty list says so, and nothing else: " + output);
        }
    }

    @Test
    @DisplayName("needs names the end of the loaded pages as Show more, never a silent cut")
    void needsShowsMoreWhenTheRuntimeHasMore() throws Exception {
        var context = new OperatorContext(
                            Map.of(),
                            tempDir,
                            true,
                            ClientEnvironment.capture(Map.of()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                            null
        );
        var page = "{\"page\":1,\"total_pages\":2,\"has_newer\":false,\"has_older\":true,\"total\":3,"
                            + "\"items\":[{\"kind\":\"decision\",\"link\":\"/api/v1/decisions/Q-1\",\"blocks\":\"plan\"}]}";
        NeedsCommand.RuntimeSender sender = req -> new HttpResponse(200, "application/json",
                            page.getBytes(StandardCharsets.UTF_8));

        var withMore = new StringWriter();
        assertEquals(0, new NeedsCommand(context).executeNeeds(sender, new PrintWriter(withMore), "en"));
        assertTrue(withMore.toString().contains("Show more"), withMore.toString());

        var lastPage = page.replace("\"has_older\":true", "\"has_older\":false");
        NeedsCommand.RuntimeSender single = req -> new HttpResponse(200, "application/json",
                            lastPage.getBytes(StandardCharsets.UTF_8));
        var withoutMore = new StringWriter();
        assertEquals(0, new NeedsCommand(context).executeNeeds(single, new PrintWriter(withoutMore), "en"));
        assertFalse(withoutMore.toString().contains("Show more"), withoutMore.toString());
    }

    @Test
    @DisplayName("a page past the last one names the page and what waits, never that nothing waits")
    void needsPageBeyondTheEnd() throws Exception {
        var context = new OperatorContext(
                            Map.of(),
                            tempDir,
                            true,
                            ClientEnvironment.capture(Map.of()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                            null
        );
        var json = ("{\"page\":5,\"total_pages\":1,\"has_newer\":true,\"has_older\":false,\"total\":7,"
                            + "\"items\":[]}").getBytes(StandardCharsets.UTF_8);
        var requested = new AtomicReference<String>();
        NeedsCommand.RuntimeSender sender = req -> {
            requested.set(req.target());
            return new HttpResponse(200, "application/json", json);
        };
        var command = new NeedsCommand(context);
        command.page = 5;

        var english = new StringWriter();
        var german = new StringWriter();
        assertEquals(0, command.executeNeeds(sender, new PrintWriter(english), "en"));
        assertEquals(0, command.executeNeeds(sender, new PrintWriter(german), "de"));

        assertAll(
                            () -> assertEquals("Page 5 is past the end: 7 items wait on 1 page; needs shows the first.",
                                                english.toString().strip()),
                            () -> assertEquals("Seite 5 liegt hinter dem Ende: 7 Einträge warten auf 1 Seite; needs zeigt die erste.",
                                                german.toString().strip()),
                            () -> assertFalse(english.toString().contains(
                                                MessageCatalogue.format("en", "operator.needs.empty", Map.of())),
                                                english.toString()),
                            () -> assertTrue(requested.get().endsWith("?page=5"), requested.get())
        );
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
    @DisplayName("needs renders the title of the operator's language, never another one")
    void needsTitleInTheOperatorLanguage() throws Exception {
        var context = new OperatorContext(
                            Map.of(),
                            tempDir,
                            true,
                            ClientEnvironment.capture(Map.of()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                            null
        );
        var json = ("{\"page\":1,\"total_pages\":1,\"has_older\":false,\"total\":1,"
                            + "\"items\":[{\"kind\":\"decision\",\"link\":\"/api/v1/decisions/Q-1\","
                            + "\"blocks\":\"plan\"}]}").getBytes(StandardCharsets.UTF_8);
        NeedsCommand.RuntimeSender sender = req -> new HttpResponse(200, "application/json", json);

        var english = new StringWriter();
        assertEquals(0, new NeedsCommand(context).executeNeeds(sender, new PrintWriter(english), "en"));
        assertTrue(english.toString().contains(MessageCatalogue.format("en", "operator.needs.title",
                            Map.of("total", 1))), english.toString());

        var german = new StringWriter();
        assertEquals(0, new NeedsCommand(context).executeNeeds(sender, new PrintWriter(german), "de"));
        assertTrue(german.toString().contains(MessageCatalogue.format("de", "operator.needs.title",
                            Map.of("total", 1))), german.toString());
        assertFalse(german.toString().contains("Needs you"), german.toString());
    }
}
