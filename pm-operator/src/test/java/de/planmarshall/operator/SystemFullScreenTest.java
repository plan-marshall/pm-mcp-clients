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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("SystemFullScreenTest (VirtualScreenTerminal)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class SystemFullScreenTest {

    private static final String STATUS_JSON = """
                        {"status":"ready","version":"0.9.1","listener":"unix"}
                        """;

    private static final String PROJECTS_JSON = """
                        {
                          "projects": [
                            {
                              "project_id": "pm-mcp-clients",
                              "display_name": "pm-mcp-clients",
                              "active_plans_count": 2,
                              "active_epics_count": 1,
                              "needs_operator_count": 0,
                              "setup_needs_operator": false
                            }
                          ]
                        }
                        """;

    private static final String SYSTEM_JSON = """
                        {
                          "runtime": {
                            "version": "0.9.1",
                            "build": "2026-10-02 (a1b2c3d)",
                            "started_at": "today 08:14",
                            "uptime_seconds": 22320,
                            "heap_used_bytes": 432013312,
                            "heap_max_bytes": 1073741824,
                            "resident_memory_bytes": 713031680,
                            "cpu_percent": 4.0,
                            "machine_store_bytes": 2469606195,
                            "disk_free_bytes": 40802189312,
                            "measured_at": "13:40",
                            "socket_listener": true,
                            "loopback_listener": "127.0.0.1:7420",
                            "network_listener": "192.168.1.20:7421",
                            "paired_devices_count": 2,
                            "relays_behind_count": 0,
                            "newer_version": "0.10.0",
                            "newer_version_date": "2026-10-16",
                            "newer_version_checked_at": "today 08:14",
                            "release_notes_link": "/api/v1/system/release-notes?version=0.10.0",
                            "upgrade_supported": true
                          },
                          "work": {
                            "plans_by_state": {"running": 3, "waiting_for_you": 1, "held": 1, "paused": 2},
                            "epics_by_state": {"running": 1, "waiting_for_you": 1},
                            "tasks_running": 4,
                            "tasks_queued": 6,
                            "active_projects_count": 3,
                            "recovering_projects": []
                          },
                          "workers": [
                            {"role": "planner", "harness": "claude", "model": "opus-4.5", "state": "busy",
                             "current_plan_title": "fix-relay"},
                            {"role": "implementer", "harness": "claude", "model": "sonnet-4.5", "state": "idle"}
                          ],
                          "jobs": {
                            "build": {"slots_used": 1, "slots_total": 2, "queued": 1},
                            "test": {"slots_used": 0, "slots_total": 1, "queued": 0},
                            "locks_held": 3
                          },
                          "models": [
                            {"harness_and_model": "claude:opus-4.5", "role": "planner", "calls_count": 42,
                             "tokens_total": 2020000, "tokens_in": 1900000, "tokens_out": 120000,
                             "errors_count": 0, "retries_count": 1, "is_floor": false, "unmeasured_calls": 0},
                            {"harness_and_model": "claude:sonnet-4.5", "role": "implementer", "calls_count": 118,
                             "tokens_total": 3610000, "tokens_in": 3200000, "tokens_out": 410000,
                             "errors_count": 0, "retries_count": 0, "is_floor": false, "unmeasured_calls": 0},
                            {"harness_and_model": "codex:gpt-5", "role": "implementer", "calls_count": 12,
                             "tokens_total": 0, "tokens_in": 0, "tokens_out": 0,
                             "errors_count": 1, "retries_count": 0, "is_floor": true, "unmeasured_calls": 12}
                          ],
                          "cleanup": {
                            "plans_count": 12, "epics_count": 2, "logs_count": 40, "reports_count": 5,
                            "projects_count": 1, "freed_bytes": 1932735283, "kept_for_good_count": 4,
                            "last_sweep_at": "today 03:00", "next_check_at": "tomorrow 03:00"
                          },
                          "window": "hour"
                        }
                        """;

    private RuntimeFixture fixture;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final List<String> systemTargets = Collections.synchronizedList(new ArrayList<>());
    private final BlockingQueue<String[]> pendingEvents = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() throws IOException {
        systemTargets.clear();
        pendingEvents.clear();
        fixture = RuntimeFixture.create();
        writeLiveRecord();
        fixture.writeToken("test-token");
        fixture.start((request, response) -> {
            var path = request.target();
            if ("/api/v1/status".equals(path)) {
                response.json(200, STATUS_JSON);
            } else if ("/api/v1/projects".equals(path)) {
                response.json(200, PROJECTS_JSON);
            } else if (path.startsWith("/api/v1/system")) {
                systemTargets.add(path);
                response.json(200, SYSTEM_JSON);
            } else if ("/api/v1/reports".equals(path)) {
                response.json(200, "{\"items\":[],\"unread_count\":0,\"has_older\":false,\"older_count\":0}");
            } else if ("/api/v1/events".equals(path)) {
                streamEvents(response.events(200));
            } else {
                response.send(404, null, "{\"error\":\"not_found\"}");
            }
        });
    }

    /**
     * Keeps the event stream open and forwards the events the test queued, one at a time.
     *
     * @param stream the open stream
     */
    private void streamEvents(FakeRuntimeServer.EventStream stream) {
        try {
            while (true) {
                var event = pendingEvents.poll(200L, TimeUnit.MILLISECONDS);
                if (event != null) {
                    stream.send(event[0], event[1]);
                }
            }
        } catch (IOException _) {
            // The client closed the stream.
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendEvent(String name, String data) {
        pendingEvents.add(new String[]{name, data});
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

    private OperatorContext createContext(VirtualScreenTerminal virtual) {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        return new OperatorContext(env, fixture.base().getParent(), true, ClientEnvironment.capture(env),
                            virtual::terminal, null);
    }

    private OverviewFullScreenApp createApp(OperatorContext context, VirtualScreenTerminal virtual) {
        return new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT,
                            clock, new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
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

    private static boolean shows(VirtualScreenTerminal virtual, String text) {
        return virtual.screen().lines().stream().anyMatch(line -> line.contains(text));
    }

    private static String selectedRow(VirtualScreenTerminal virtual) {
        return virtual.screen().lines().stream().filter(line -> line.startsWith("\u25b6")).findFirst().orElse("");
    }

    /** Starts the view and opens the system level with {@code S}. */
    private Thread startAndOpenSystem(VirtualScreenTerminal virtual, OperatorContext context,
                        OverviewFullScreenApp app) throws InterruptedException {
        var thread = new Thread(() -> {
            try {
                app.run(new PrintWriter(new StringWriter()));
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
        thread.start();
        await(() -> virtual.alternateScreenActive() && shows(virtual, "pm-mcp-clients"));
        virtual.type("S");
        await(() -> shows(virtual, "System \u00b7 pm-mcpd 0.9.1"));
        return thread;
    }

    @Test
    @DisplayName("S opens the six figures with the runtime's header and the keys of the view")
    void opensTheSystemView() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenSystem(virtual, context, createApp(context, virtual));

            await(() -> shows(virtual, "Runtime   running since today 08:14"));
            await(() -> shows(virtual, "Work      plans  3 running"));
            await(() -> shows(virtual, "Workers   2 \u00b7 1 busy \u00b7 1 idle"));
            await(() -> shows(virtual, "Jobs      locks  3 held"));
            await(() -> shows(virtual, "Models    Last hour"));
            await(() -> shows(virtual, "Cleanup   due: 12 plans"));
            await(() -> shows(virtual,
                                "\u2191\u2193 move   Enter open   a actions   ? help   q leave   w window   b by role   p per project"));

            assertTrue(shows(virtual, "memory 412 MB in use of 1 GB"), virtual.screen().lines().toString());
            assertTrue(systemTargets.stream().anyMatch(target -> target.contains("window=hour")),
                                systemTargets.toString());

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("w cycles the window, b the grouping, and both stay in overview.json")
    void cyclesTheWindowAndGrouping() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenSystem(virtual, context, createApp(context, virtual));

            virtual.type("w");
            await(() -> shows(virtual, "Models    Today"));
            await(() -> systemTargets.stream().anyMatch(target -> target.contains("window=today")));

            virtual.type("w");
            await(() -> shows(virtual, "Models    7 days"));
            await(() -> systemTargets.stream().anyMatch(target -> target.contains("window=7d")));

            virtual.type("b");
            await(() -> shows(virtual, "b by model"));
            await(() -> shows(virtual, "planner"));

            // Leaving from the overview, not from the system level, must keep the choice too.
            virtual.type("\u001b");
            await(() -> shows(virtual, "Running work"));
            virtual.type("q");
            join(thread);
        }

        var store = new OverviewStateStore(createContext(VirtualScreenTerminal.create(120, 30)).stateDirectory());
        var saved = store.read().orElseThrow();
        assertEquals("7d", saved.systemWindow());
        assertEquals("role", saved.systemGroupBy());
    }

    @Test
    @DisplayName("p shows one project's figures and offers all projects again")
    void togglesPerProject() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenSystem(virtual, context, createApp(context, virtual));

            virtual.type("p");
            await(() -> shows(virtual, "System \u00b7 pm-mcpd 0.9.1 \u00b7 pm-mcp-clients"));
            await(() -> shows(virtual, "p all projects"));
            await(() -> systemTargets.stream()
                                .anyMatch(target -> target.contains("project=pm-mcp-clients")));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("Enter follows a figure into its section and Esc returns")
    void drillsDownIntoASection() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenSystem(virtual, context, createApp(context, virtual));

            await(() -> selectedRow(virtual).contains("Runtime"));
            virtual.type("jj");
            await(() -> selectedRow(virtual).contains("Workers"));
            virtual.type("\r");
            await(() -> shows(virtual, "System \u00b7 pm-mcpd 0.9.1 \u00b7 Workers"));
            await(() -> shows(virtual, "planner"));

            virtual.type("\u001b");
            await(() -> !shows(virtual, "\u00b7 Workers"));

            await(() -> selectedRow(virtual).contains("Workers"));
            virtual.type("j");
            await(() -> selectedRow(virtual).contains("Jobs"));
            virtual.type("\r");
            await(() -> shows(virtual, "\u00b7 Jobs"));
            await(() -> shows(virtual, "locks  3 held"));

            virtual.type("jj");
            await(() -> selectedRow(virtual).contains("locks"));
            virtual.type("\r");
            await(() -> shows(virtual, "\u00b7 Locks"));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("while the runtime drains, the progress line stands below the header")
    void showsTheDrainingLine() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenSystem(virtual, context, createApp(context, virtual));

            sendEvent("runtime.draining", "{\"who\":\"you\",\"remaining_count\":2,"
                                + "\"remaining_items\":[\"build of fix-relay\",\"task of relay-docs\"],"
                                + "\"withdrawable\":true}");

            await(() -> shows(virtual, "Stopping when what runs is done"));
            await(() -> shows(virtual, "2 left: build of fix-relay, task of relay-docs"));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("a opens the runtime's acts")
    void opensTheRuntimeActions() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenSystem(virtual, context, createApp(context, virtual));

            virtual.type("a");
            await(() -> shows(virtual, "Stop\u2026"));
            assertTrue(shows(virtual, "Restart\u2026"), virtual.screen().lines().toString());
            assertTrue(shows(virtual, "Upgrade\u2026"), virtual.screen().lines().toString());

            virtual.type("\u001b");
            await(() -> !shows(virtual, "Stop\u2026"));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("at 80 columns the details are drawn, below them ? shows them")
    void respectsTheWidthThresholds() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(80, 24)) {
            var context = createContext(virtual);
            var thread = startAndOpenSystem(virtual, context, createApp(context, virtual));

            await(() -> shows(virtual, "memory 412 MB in use of 1 GB"));

            virtual.type("q");
            join(thread);
        }

        try (var virtual = VirtualScreenTerminal.create(70, 24)) {
            var context = createContext(virtual);
            var thread = startAndOpenSystem(virtual, context, createApp(context, virtual));

            await(() -> shows(virtual, "Runtime   running since"));
            assertFalse(shows(virtual, "memory 412 MB"), virtual.screen().lines().toString());

            virtual.type("?");
            await(() -> shows(virtual, "memory 412 MB in use of 1 GB"));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("below 60 columns system prints the too-narrow line and the snapshot")
    void fallsBackBelowSixtyColumns() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(50, 20)) {
            var context = createContext(virtual);
            context.setConsoleActive(true);
            var out = new StringWriter();
            var err = new StringWriter();

            var exit = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "system");

            assertEquals(0, exit);
            assertTrue(err.toString().contains("too narrow"), err.toString());
            assertTrue(out.toString().contains("Runtime"), out.toString());
            assertTrue(out.toString().contains("System"), out.toString());
        }
    }

    @Test
    @DisplayName("system --project in the console opens the view of that project")
    void keepsTheProjectOfTheConsoleOption() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            context.setConsoleActive(true);
            var out = new StringWriter();
            var thread = new Thread(() -> PmOperator.execute(context, new PrintWriter(out),
                                new PrintWriter(new StringWriter()), "system", "--project", "pm-mcp-clients"));
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && shows(virtual, "System \u00b7 pm-mcpd 0.9.1 \u00b7 pm-mcp-clients"));
            await(() -> systemTargets.stream()
                                .anyMatch(target -> target.contains("project=pm-mcp-clients")));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("system in the console opens the view and q leaves it")
    void opensFromTheConsole() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            context.setConsoleActive(true);
            var out = new StringWriter();
            var thread = new Thread(() -> PmOperator.execute(context, new PrintWriter(out),
                                new PrintWriter(new StringWriter()), "system"));
            thread.start();
            await(() -> virtual.alternateScreenActive() && shows(virtual, "System \u00b7 pm-mcpd 0.9.1"));

            virtual.type("q");
            join(thread);
            assertFalse(virtual.alternateScreenActive());
        }
    }
}