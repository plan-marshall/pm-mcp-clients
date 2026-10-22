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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("ReportsFullScreenTest (VirtualScreenTerminal)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ReportsFullScreenTest {

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
                            },
                            {
                              "project_id": "pm-mcp-core",
                              "display_name": "pm-mcp-core",
                              "active_plans_count": 0,
                              "active_epics_count": 0,
                              "needs_operator_count": 0,
                              "setup_needs_operator": false
                            }
                          ]
                        }
                        """;

    private static final String PROJECT_JSON = """
                        {
                          "project_id": "pm-mcp-clients",
                          "display_name": "pm-mcp-clients",
                          "plans": [
                            {
                              "plan_id": "fix-relay",
                              "title": "fix-relay",
                              "current_phase": "execute",
                              "phase_state": "running"
                            }
                          ],
                          "epics": [
                            {
                              "epic_id": "relay-hardening",
                              "title": "relay-hardening",
                              "running_plans_count": 1,
                              "staged_plans_count": 1,
                              "plans": [
                                {
                                  "plan_id": "fix-relay",
                                  "title": "fix-relay",
                                  "current_phase": "execute",
                                  "phase_state": "running"
                                }
                              ]
                            }
                          ],
                          "alongside_landings": []
                        }
                        """;

    private static final String COMPLETION_JSON = """
                        {
                          "plan_id": "fix-relay",
                          "title": "fix-relay",
                          "from": {"project_id": "pm-mcp-clients", "epic_id": "relay-hardening", "plan_id": "fix-relay"},
                          "outcome": "merged",
                          "terminal_reason": "the pull request #214 was merged into main",
                          "pull_request_number": 214,
                          "pull_request_url": "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                          "reason": "—",
                          "started_at": "2026-10-16T08:40:00Z",
                          "ended_at": "2026-10-16T11:20:00Z",
                          "duration_seconds": 9600,
                          "paused_seconds": 0,
                          "deliverables": [{"name": "cursor write order", "state": "done"}],
                          "replanning_records": [],
                          "decision_records": []
                        }
                        """;

    private static final String REPORTS_JSON = """
                        {
                          "items": [
                            {
                              "id": "rep-0142",
                              "from": {"project_id": "pm-mcp-clients", "epic_id": "relay-hardening", "plan_id": "fix-relay"},
                              "kind": "plan.ended",
                              "subject": "fix-relay ended: merged (#214)",
                              "body": "the pull request #214 was merged into main",
                              "created_at": "2026-10-16T11:20:00Z",
                              "sent_at": "2026-10-16T11:20:00Z"
                            },
                            {
                              "id": "rep-0143",
                              "from": {"project_id": "pm-mcp-clients", "epic_id": "relay-hardening", "plan_id": "relay-token-store"},
                              "kind": "plan.started",
                              "subject": "relay-token-store started by relay-hardening (2 of 3)",
                              "created_at": "2026-10-16T11:21:00Z",
                              "sent_at": "2026-10-16T11:21:00Z"
                            },
                            {
                              "id": "rep-0139",
                              "from": {"project_id": "pm-mcp-clients", "plan_id": "docs-typo"},
                              "kind": "plan.ended",
                              "subject": "docs-typo ended: no change",
                              "body": "the plan produced no change",
                              "created_at": "2026-10-16T09:02:00Z",
                              "sent_at": "2026-10-16T09:40:00Z"
                            },
                            {
                              "id": "rep-0131",
                              "from": {"project_id": "pm-mcp-core"},
                              "kind": "project.onboarded",
                              "subject": "pm-mcp-core is onboarded",
                              "created_at": "2026-10-15T18:40:00Z",
                              "sent_at": "2026-10-15T18:40:00Z"
                            }
                          ],
                          "unread_count": 4,
                          "has_older": false,
                          "older_count": 0
                        }
                        """;

    private static final String REPORTS_WITH_ARRIVAL_JSON = """
                        {
                          "items": [
                            {
                              "id": "rep-0150",
                              "from": {"project_id": "pm-mcp-clients", "epic_id": "relay-hardening", "plan_id": "relay-cursor"},
                              "kind": "plan.ended",
                              "subject": "relay-cursor ended: merged (#220)",
                              "created_at": "2026-10-16T12:00:00Z",
                              "sent_at": "2026-10-16T12:00:00Z"
                            },
                            {
                              "id": "rep-0142",
                              "from": {"project_id": "pm-mcp-clients", "epic_id": "relay-hardening", "plan_id": "fix-relay"},
                              "kind": "plan.ended",
                              "subject": "fix-relay ended: merged (#214)",
                              "created_at": "2026-10-16T11:20:00Z",
                              "sent_at": "2026-10-16T11:20:00Z"
                            },
                            {
                              "id": "rep-0143",
                              "from": {"project_id": "pm-mcp-clients", "epic_id": "relay-hardening", "plan_id": "relay-token-store"},
                              "kind": "plan.started",
                              "subject": "relay-token-store started by relay-hardening (2 of 3)",
                              "created_at": "2026-10-16T11:21:00Z",
                              "sent_at": "2026-10-16T11:21:00Z"
                            }
                          ],
                          "unread_count": 5,
                          "has_older": false,
                          "older_count": 0
                        }
                        """;

    private RuntimeFixture fixture;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final AtomicReference<String> reportsJson = new AtomicReference<>(REPORTS_JSON);
    private final AtomicReference<FakeRuntimeServer.EventStream> activeStream = new AtomicReference<>();
    private final List<String> readIds = Collections.synchronizedList(new ArrayList<>());
    private final List<String> bulkReads = Collections.synchronizedList(new ArrayList<>());
    private final BlockingQueue<String[]> pendingEvents = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() throws IOException {
        reportsJson.set(REPORTS_JSON);
        activeStream.set(null);
        readIds.clear();
        bulkReads.clear();
        pendingEvents.clear();
        fixture = RuntimeFixture.create();
        writeLiveRecord();
        fixture.writeToken("test-token");
        fixture.start(this::handle);
    }

    private void handle(FakeRuntimeServer.Request request, FakeRuntimeServer.Response response) throws IOException {
        var path = request.target();
        if ("/api/v1/status".equals(path)) {
            response.json(200, STATUS_JSON);
        } else if ("/api/v1/projects".equals(path)) {
            response.json(200, PROJECTS_JSON);
        } else if ("/api/v1/projects/pm-mcp-clients".equals(path)) {
            response.json(200, PROJECT_JSON);
        } else if ("/api/v1/plans/fix-relay/completion".equals(path)) {
            response.json(200, COMPLETION_JSON);
        } else if ("/api/v1/reports".equals(path)) {
            response.json(200, reportsJson.get());
        } else if ("/api/v1/reports/read".equals(path) && "POST".equals(request.method())) {
            bulkReads.add(request.bodyText());
            response.json(200, "{\"marked_count\":4}");
        } else if (path.startsWith("/api/v1/reports/") && path.endsWith("/read")) {
            readIds.add(path.substring("/api/v1/reports/".length(), path.length() - "/read".length()));
            response.send(204, null, "");
        } else if (path.startsWith("/api/v1/epics/")) {
            response.send(404, null, "{\"error\":\"not_found\"}");
        } else if ("/api/v1/events".equals(path)) {
            var stream = response.events(200);
            activeStream.set(stream);
            streamEvents(stream);
        } else {
            response.send(404, null, "{\"error\":\"not_found\"}");
        }
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

    /**
     * @param virtual the terminal
     * @return the line of the selected row, read from the cursor of one parse of the screen
     */
    private static String selectedRow(VirtualScreenTerminal virtual) {
        return virtual.screen().lines().stream().filter(line -> line.startsWith("\u25b6")).findFirst().orElse("");
    }

    private void sendEvent(String name, String data) {
        pendingEvents.add(new String[]{name, data});
    }

    private Thread startView(VirtualScreenTerminal virtual, OperatorContext context, OverviewFullScreenApp app)
                        throws InterruptedException {
        var thread = new Thread(() -> {
            try {
                app.run(new PrintWriter(new StringWriter()));
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
        thread.start();
        await(() -> virtual.alternateScreenActive() && shows(virtual, "pm-mcp-clients"));
        return thread;
    }

    /** Starts the view and opens the reports with {@code R}. */
    private Thread startAndOpenReports(VirtualScreenTerminal virtual, OperatorContext context,
                        OverviewFullScreenApp app) throws InterruptedException {
        var thread = startView(virtual, context, app);
        virtual.type("R");
        await(() -> shows(virtual, "Reports \u00b7 4 unread"));
        return thread;
    }

    @Test
    @DisplayName("R opens the reports grouped by project, epic, and plan, with the selection on a report")
    void opensTheGroupedReports() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenReports(virtual, context, createApp(context, virtual));

            await(() -> shows(virtual, "\u25c7 relay-hardening"));
            await(() -> shows(virtual, "\u25b8 fix-relay"));
            await(() -> shows(virtual, "\u25aa project"));
            await(() -> shows(virtual,
                                "\u2191\u2193 move   Enter details   m read   M read all shown   p project   e epic   Esc back"));
            await(() -> selectedRow(virtual).contains("\u25b8 fix-relay"));
            assertTrue(shows(virtual, "Reports \u00b7 4 unread"), virtual.screen().lines().toString());

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("m reads the selected report, drops its row, and moves the selection to its neighbour")
    void marksOneReportRead() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenReports(virtual, context, createApp(context, virtual));

            await(() -> selectedRow(virtual).contains("fix-relay"));
            virtual.type("m");
            await(() -> !shows(virtual, "\u25b8 fix-relay"));
            await(() -> selectedRow(virtual).contains("relay-token-store"));

            assertEquals(List.of("rep-0142"), readIds);

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("M reads all shown reports by their ids and leaves the level empty")
    void marksAllShownRead() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenReports(virtual, context, createApp(context, virtual));

            virtual.type("M");
            await(() -> shows(virtual, "Nothing new"));
            await(() -> shows(virtual, "Reports \u00b7 0 unread"));

            assertEquals(1, bulkReads.size());
            var body = bulkReads.getFirst();
            assertTrue(body.contains("rep-0142"), body);
            assertTrue(body.contains("rep-0143"), body);
            assertTrue(body.contains("rep-0139"), body);
            assertTrue(body.contains("rep-0131"), body);
            assertFalse(body.contains("\"all\""), body);

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("p and e open the project and the epic without reading the report")
    void navigatesWithoutReading() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenReports(virtual, context, createApp(context, virtual));

            await(() -> selectedRow(virtual).contains("fix-relay"));
            virtual.type("p");
            await(() -> shows(virtual, "by plan"));
            assertTrue(readIds.isEmpty(), readIds.toString());

            virtual.type("\u001b");
            await(() -> shows(virtual, "Reports \u00b7 4 unread"));

            virtual.type("e");
            await(() -> shows(virtual, "by epic"));
            assertTrue(readIds.isEmpty(), readIds.toString());

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("Enter opens an ended plan's completion report and reads the report in the same act")
    void opensTheCompletionReportAndReads() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenReports(virtual, context, createApp(context, virtual));

            await(() -> selectedRow(virtual).contains("fix-relay"));
            virtual.type("\r");
            await(() -> shows(virtual, "completion report"));
            await(() -> shows(virtual, "Deliverables"));
            await(() -> readIds.contains("rep-0142"));

            virtual.type("\u001b");
            await(() -> shows(virtual, "Reports \u00b7 3 unread"));
            await(() -> !shows(virtual, "\u25b8 fix-relay"));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("a report that arrives live is inserted at its place and the selection stays on its report")
    void keepsTheSelectionOnALiveArrival() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenReports(virtual, context, createApp(context, virtual));

            await(() -> selectedRow(virtual).contains("fix-relay"));
            await(() -> activeStream.get() != null);
            reportsJson.set(REPORTS_WITH_ARRIVAL_JSON);
            sendEvent("report.sent", "{\"report_id\":\"rep-0150\",\"kind\":\"plan.ended\"}");

            await(() -> shows(virtual, "relay-cursor"));
            await(() -> selectedRow(virtual).contains("fix-relay"));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("? shows the selected report's body, its created and sent times, and its id")
    void showsTheReportDetails() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = startAndOpenReports(virtual, context, createApp(context, virtual));

            await(() -> selectedRow(virtual).contains("fix-relay"));
            virtual.type("jj");
            await(() -> selectedRow(virtual).contains("docs-typo"));
            virtual.type("?");
            await(() -> shows(virtual, "the plan produced no change"));

            var text = String.join("\n", virtual.screen().lines());
            assertTrue(text.contains("rep-0139"), text);
            assertTrue(text.contains("sent "), text);

            virtual.type("q");
            join(thread);
        }
    }

    @Nested
    @DisplayName("empty reports")
    class Empty {
        @Test
        @DisplayName("R on a machine without unread reports says there is nothing new")
        void showsTheEmptyLine() throws Exception {
            reportsJson.set("{\"items\":[],\"unread_count\":0,\"has_older\":false,\"older_count\":0}");
            try (var virtual = VirtualScreenTerminal.create(120, 30)) {
                var context = createContext(virtual);
                var thread = startView(virtual, context, createApp(context, virtual));

                virtual.type("R");
                await(() -> shows(virtual, "Reports \u00b7 0 unread"));
                await(() -> shows(virtual, "Nothing new"));

                virtual.type("q");
                join(thread);
            }
        }
    }
}
