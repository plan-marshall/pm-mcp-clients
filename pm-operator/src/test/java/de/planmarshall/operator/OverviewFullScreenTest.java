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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.tui.TuiScreenDriver;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("OverviewFullScreenTest (VirtualScreenTerminal)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class OverviewFullScreenTest {

    private static final String STATUS_JSON = """
                        {
                          "status": "ready",
                          "version": "0.9.1",
                          "listener": "unix",
                          "running_since": "08:14",
                          "workers_count": 2,
                          "build_slots_used": 1,
                          "build_slots_total": 2
                        }
                        """;

    private static final String PROJECTS_JSON = """
                        {
                          "projects": [
                            {
                              "project_id": "pm-mcp-clients",
                              "display_name": "pm-mcp-clients",
                              "active_plans_count": 3,
                              "active_epics_count": 1,
                              "needs_operator_count": 2,
                              "needs_operator_summary": "decision",
                              "setup_needs_operator": false
                            },
                            {
                              "project_id": "pm-mcp-core",
                              "display_name": "pm-mcp-core",
                              "active_plans_count": 2,
                              "active_epics_count": 0,
                              "needs_operator_count": 0,
                              "needs_operator_summary": null,
                              "setup_needs_operator": false
                            },
                            {
                              "project_id": "plan-marshall",
                              "display_name": "plan-marshall",
                              "active_plans_count": 0,
                              "active_epics_count": 1,
                              "needs_operator_count": 1,
                              "needs_operator_summary": "setup",
                              "setup_needs_operator": true
                            }
                          ]
                        }
                        """;

    private static final String DETAIL_CLIENTS_JSON = """
                        {
                          "project_id": "pm-mcp-clients",
                          "display_name": "pm-mcp-clients",
                          "plans": [
                            {
                              "plan_id": "pln-console-history",
                              "title": "Console history of answers",
                              "current_phase": "refine",
                              "phase_state": "waiting_for_you",
                              "needs_operator_kind": "decision",
                              "needs_operator_count": 1,
                              "phase_entered_at": "2026-10-16T15:26:00Z",
                              "tasks_done": 0,
                              "tasks_total": 0
                            },
                            {
                              "plan_id": "pln-overview-resource",
                              "title": "Overview resource of the runtime",
                              "current_phase": "execute",
                              "phase_state": "running",
                              "tasks_done": 7,
                              "tasks_total": 12,
                              "epic_id": "epc-client-work",
                              "epic_title": "client work",
                              "pull_request_number": 214,
                              "pull_request_url": "https://github.com/plan-marshall/pm-mcp-clients/pull/214"
                            },
                            {
                              "plan_id": "pln-message-catalogue",
                              "title": "Message catalogue",
                              "current_phase": "outline",
                              "phase_state": "pausing",
                              "epic_id": "epc-client-work",
                              "epic_title": "client work",
                              "off_track_signal": "repeating"
                            }
                          ],
                          "epics": [
                            {
                              "epic_id": "epc-client-work",
                              "title": "client work",
                              "running_plans_count": 2,
                              "staged_plans_count": 3,
                              "plans": [
                                {
                                  "plan_id": "pln-overview-resource",
                                  "title": "Overview resource of the runtime",
                                  "current_phase": "execute",
                                  "phase_state": "running",
                                  "tasks_done": 7,
                                  "tasks_total": 12
                                },
                                {
                                  "plan_id": "pln-message-catalogue",
                                  "title": "Message catalogue",
                                  "current_phase": "outline",
                                  "phase_state": "pausing",
                                  "off_track_signal": "repeating"
                                }
                              ]
                            }
                          ],
                          "alongside_landings": [
                            {
                              "unit": "remediation.ci-failure",
                              "state": "failed",
                              "kind": "unit",
                              "hold_kind": "landing_failed"
                            },
                            {
                              "state": "running",
                              "count": 2,
                              "kind": "removal"
                            }
                          ]
                        }
                        """;

    private static final String DETAIL_CORE_JSON = """
                        {
                          "project_id": "pm-mcp-core",
                          "display_name": "pm-mcp-core",
                          "plans": [
                            {
                              "plan_id": "pln-session-log",
                              "title": "Session log and generations",
                              "current_phase": "execute",
                              "phase_state": "running",
                              "pull_request_number": 198,
                              "pull_request_url": "https://github.com/plan-marshall/pm-mcp-core/pull/198"
                            }
                          ],
                          "epics": [],
                          "alongside_landings": []
                        }
                        """;

    private static final String ENDED_CLIENTS_PAGE1_JSON = """
                        {
                          "project_id": "pm-mcp-clients",
                          "page": 1,
                          "total_pages": 2,
                          "has_newer": false,
                          "has_older": true,
                          "items": [
                            {
                              "item_id": "pln-relay-drops",
                              "kind": "plan",
                              "title": "Relay drops events on reconnect",
                              "outcome": "merged",
                              "ended_at": "2026-10-14T10:00:00Z",
                              "pull_request_number": 211,
                              "pull_request_url": "https://github.com/plan-marshall/pm-mcp-clients/pull/211"
                            },
                            {
                              "item_id": "epc-bootstrap",
                              "kind": "epic",
                              "title": "client bootstrap",
                              "terminal_reason": "completed",
                              "ended_at": "2026-10-02T10:00:00Z"
                            }
                          ]
                        }
                        """;

    private static final String ENDED_CLIENTS_PAGE2_JSON = """
                        {
                          "project_id": "pm-mcp-clients",
                          "page": 2,
                          "total_pages": 2,
                          "has_newer": true,
                          "has_older": false,
                          "items": [
                            {
                              "item_id": "pln-page-2",
                              "kind": "plan",
                              "title": "Page 2 plan",
                              "outcome": "merged",
                              "ended_at": "2026-10-01T10:00:00Z"
                            }
                          ]
                        }
                        """;

    private RuntimeFixture fixture;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final AtomicReference<FakeRuntimeServer.EventStream> activeStream = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        activeStream.set(null);
        fixture = RuntimeFixture.create();
        writeLiveRecord();
        fixture.writeToken("test-token");
        setupServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private void writeLiveRecord() throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
    }

    private void setupServer() throws IOException {
        fixture.start((request, response) -> {
            var path = request.target();
            if ("/api/v1/status".equals(path) || path.startsWith("/api/v1/status?")) {
                response.json(200, STATUS_JSON);
            } else if ("/api/v1/projects".equals(path)) {
                response.json(200, PROJECTS_JSON);
            } else if ("/api/v1/projects/pm-mcp-clients".equals(path)) {
                response.json(200, DETAIL_CLIENTS_JSON);
            } else if ("/api/v1/projects/pm-mcp-core".equals(path)) {
                response.json(200, DETAIL_CORE_JSON);
            } else if ("/api/v1/projects/plan-marshall".equals(path)) {
                response.json(200, "{\"project_id\":\"plan-marshall\",\"display_name\":\"plan-marshall\",\"plans\":[],\"epics\":[],\"alongside_landings\":[]}");
            } else if (path.startsWith("/api/v1/projects/pm-mcp-clients/ended")) {
                if (path.contains("page=2")) {
                    response.json(200, ENDED_CLIENTS_PAGE2_JSON);
                } else {
                    response.json(200, ENDED_CLIENTS_PAGE1_JSON);
                }
            } else if ("/api/v1/reports".equals(path)) {
                response.json(200, "{\"unread_count\":3,\"items\":[]}");
            } else if ("/api/v1/events".equals(path)) {
                var stream = response.events(200);
                activeStream.set(stream);
            } else {
                response.send(404, null, "{\"error\":\"not_found\"}");
            }
        });
    }

    private OperatorContext createContext(VirtualScreenTerminal virtual) {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        return new OperatorContext(
                            env,
                            fixture.base().getParent(),
                            true,
                            ClientEnvironment.capture(env),
                            virtual::terminal,
                            null
        );
    }

    private static void delay(long millis) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        var deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("the view did not reach the expected state in time");
            }
            delay(20L);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    private static void join(Thread thread) throws InterruptedException {
        thread.join(5_000L);
        assertFalse(thread.isAlive(), "the driver did not leave the view");
    }

    @Test
    @DisplayName("projects overview renders header, selection, attention badges and footer keys at 120x30")
    void shouldRenderProjectsOverviewLayout() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var app = new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT, clock,
                                new OverviewStateStore(context.stateDirectory()),
                                new ConsoleNoticeBuffer());

            virtual.type("q");
            var out = new StringWriter();
            var result = app.run(new PrintWriter(out));

            assertEquals(TuiScreenDriver.Result.LEFT, result);
            var screen = virtual.screen();
            assertFalse(screen.alternateScreenActive());
            assertEquals(1, screen.alternation().entries());
            assertEquals(1, screen.alternation().exits());

            var output = virtual.rawOutput();
            assertTrue(output.contains("Running work"), output);
            assertTrue(output.contains("pm-mcp-clients"), output);
            assertTrue(output.contains("! 2"), output);
            assertTrue(output.contains("3 plans · 1 epic"), output);
            assertTrue(output.contains("pm-mcp-core"), output);
            assertTrue(output.contains("setup needs you"), output);
        }
    }

    @Test
    @DisplayName("navigation: Enter to project, Tab toggles by-epic, e jumps to epic, Esc pops back")
    void shouldNavigateEnterTabAndJumpToEpic() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var app = new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT, clock,
                                new OverviewStateStore(context.stateDirectory()),
                                new ConsoleNoticeBuffer());

            var out = new StringWriter();
            var thread = new Thread(() -> {
                try {
                    app.run(new PrintWriter(out));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("pm-mcp-clients")));

            // 1. Enter to open pm-mcp-clients (by plan)
            virtual.type("\r");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("by plan")));

            // 2. Tab to switch to by epic
            virtual.type("\t");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("by epic")));

            // 3. Tab to switch back to by plan
            virtual.type("\t");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("by plan")));

            // 4. Move down to second plan (pln-overview-resource, belongs to epc-client-work)
            virtual.type("j");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("▶") && l.contains("Overview resource")));

            // 5. Press 'e' -> jumps to parent epic in by epic view!
            virtual.type("e");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("by epic"))
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("client work")));

            // 6. Press Esc -> returns to projects overview
            virtual.type("\u001b");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("projects")));

            // 7. Exit with q
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("filtering: / filter prompt filters rows, Esc clears filter")
    void shouldFilterAndClearWithEsc() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var app = new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT, clock,
                                new OverviewStateStore(context.stateDirectory()),
                                new ConsoleNoticeBuffer());

            var out = new StringWriter();
            var thread = new Thread(() -> {
                try {
                    app.run(new PrintWriter(out));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("pm-mcp-clients")));

            // Type / then "core\r"
            virtual.type("/core\r");
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("pm-mcp-core"))
                                && virtual.screen().lines().stream().noneMatch(l -> l.contains("pm-mcp-clients")));

            // Assert filtered: pm-mcp-clients is not visible
            var filteredLines = virtual.screen().lines();
            assertTrue(filteredLines.stream().anyMatch(l -> l.contains("pm-mcp-core")));
            assertFalse(filteredLines.stream().anyMatch(l -> l.contains("pm-mcp-clients")));

            // Press Esc to clear filter
            virtual.type("\u001b");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("pm-mcp-clients")));

            // Exit with q
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("ended work: E opens ended work level and scroll triggers auto-pagination")
    void shouldOpenEndedWorkAndPaginate() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var app = new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT, clock,
                                new OverviewStateStore(context.stateDirectory()),
                                new ConsoleNoticeBuffer());

            var out = new StringWriter();
            var thread = new Thread(() -> {
                try {
                    app.run(new PrintWriter(out));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("pm-mcp-clients")));

            // Press E -> opens EndedWorkLevel
            virtual.type("E");
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("Ended work")));

            assertTrue(virtual.screen().lines().stream().anyMatch(l -> l.contains("Relay drops events on reconnect")));

            // Press G -> jumps to bottom, triggering pagination to load page 2
            virtual.type("G");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("Page 2 plan")));

            // Press Esc -> pops back to projects level
            virtual.type("\u001b");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("projects")));

            // Exit with q
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("responsive widths: 80x24 hides detail lines (? shows them); 50x20 falls back to status tree")
    void shouldRespectWidthThresholds() throws Exception {
        // 1. 80x24: detail lines hidden until ? is typed
        try (var virtual = VirtualScreenTerminal.create(80, 24)) {
            var context = createContext(virtual);
            var app = new OverviewFullScreenApp(context, virtual.terminal(), new OverviewFullScreenApp.InitialSelection("pm-mcp-clients", "plan", false), clock,
                                new OverviewStateStore(context.stateDirectory()),
                                new ConsoleNoticeBuffer());

            var out = new StringWriter();
            var thread = new Thread(() -> {
                try {
                    app.run(new PrintWriter(out));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("Console history of answers")));

            // Move to second plan: pln-overview-resource (which has PR #214)
            virtual.type("j");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("▶") && l.contains("Overview resource")));

            // Before ?: PR URL not in screen lines
            assertFalse(virtual.screen().lines().stream().anyMatch(l -> l.contains("https://")));

            // Type ? to show help/details
            virtual.type("?");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("https://")));

            // Type ? to hide help
            virtual.type("?");
            await(() -> virtual.screen().lines().stream().noneMatch(l -> l.contains("https://")));

            // Type q to exit
            virtual.type("q");
            join(thread);
        }

        // 2. 50x20: too narrow! Returns TOO_NARROW and prints too-narrow text and status tree
        try (var virtual = VirtualScreenTerminal.create(50, 20)) {
            var context = createContext(virtual);
            var out = new StringWriter();
            int exitCode = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()), "plans");

            assertEquals(0, exitCode);
            var raw = virtual.rawOutput();
            assertTrue(raw.contains("too narrow") || out.toString().contains("too narrow"),
                                "expected too-narrow warning; raw=" + raw + ", out=" + out);
            assertTrue(raw.contains("pm-mcp-clients") || out.toString().contains("pm-mcp-clients"),
                                "expected status tree output; raw=" + raw + ", out=" + out);
        }
    }

    @Test
    @DisplayName("live SSE update preserves selection by plan ID")
    void shouldPreserveSelectionOnLiveSseUpdate() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var app = new OverviewFullScreenApp(context, virtual.terminal(), new OverviewFullScreenApp.InitialSelection("pm-mcp-clients", "plan", false), clock,
                                new OverviewStateStore(context.stateDirectory()),
                                new ConsoleNoticeBuffer());

            var out = new StringWriter();
            var thread = new Thread(() -> {
                try {
                    app.run(new PrintWriter(out));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("Console history of answers")));

            // Move to second plan: pln-overview-resource
            virtual.type("j");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("▶") && l.contains("Overview resource")));

            // Trigger SSE event
            await(() -> activeStream.get() != null);
            var stream = activeStream.get();
            if (stream != null) {
                try {
                    stream.send("phase.changed", "{\"project_id\":\"pm-mcp-clients\",\"plan_id\":\"pln-console-history\",\"phase\":\"execute\"}");
                } catch (IOException _) {
                    // ignore if closed
                }
            }
            delay(100L);

            // Selection remains on Overview resource
            assertTrue(virtual.screen().lines().stream().anyMatch(l -> l.contains("▶") && l.contains("Overview resource")));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("persists saved state to overview.json with 0600 permissions and restores on reopen")
    void shouldPersistAndRestoreOverviewState() throws Exception {
        var contextForPath = createContext(VirtualScreenTerminal.create(120, 30));
        var stateFile = contextForPath.stateDirectory().overviewFile();
        Files.deleteIfExists(stateFile);

        // Run 1: navigate to pm-mcp-clients, toggle by epic, exit with q
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = new Thread(() -> {
                PmOperator.execute(context, new PrintWriter(new StringWriter()), new PrintWriter(new StringWriter()), "plans");
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("pm-mcp-clients")));

            virtual.type("\r");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("by plan")));

            virtual.type("\t");
            await(() -> virtual.screen().lines().stream().anyMatch(l -> l.contains("by epic")));

            virtual.type("q");
            join(thread);
        }

        assertTrue(Files.exists(stateFile), "overview.json should exist");
        var perms = Files.getPosixFilePermissions(stateFile);
        assertEquals(PosixFilePermissions.fromString("rw-------"), perms, "overview.json permissions must be 0600");

        var store = new OverviewStateStore(contextForPath.stateDirectory());
        var saved = store.read().orElseThrow();
        assertEquals("pm-mcp-clients", saved.projectId());
        assertEquals("epic", saved.projectView());

        // Run 2: start plans with no args; should open directly at pm-mcp-clients by epic
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = new Thread(() -> {
                PmOperator.execute(context, new PrintWriter(new StringWriter()), new PrintWriter(new StringWriter()), "plans");
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && virtual.screen().lines().stream().anyMatch(l -> l.contains("by epic")));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("notice buffer is drained and printed to out on exit")
    void shouldDrainNoticeBufferOnExit() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var noticeBuffer = new ConsoleNoticeBuffer();
            noticeBuffer.activate();
            noticeBuffer.hold(ConsoleNoticeBuffer.Category.DECISION, 1);
            noticeBuffer.holdLine("Decision requested: approve PR");

            var app = new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT, clock,
                                new OverviewStateStore(context.stateDirectory()),
                                noticeBuffer);

            virtual.type("q");
            var out = new StringWriter();
            var result = app.run(new PrintWriter(out));

            assertEquals(TuiScreenDriver.Result.LEFT, result);
            var outText = out.toString();
            assertTrue(outText.contains("While the overview was open"), outText);
            assertTrue(outText.contains("Decision requested: approve PR"), outText);
        }
    }
}
