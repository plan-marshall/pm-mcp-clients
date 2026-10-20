/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.overview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewSavedState;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("OverviewFullScreenAppTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
class OverviewFullScreenAppTest {

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
                            }
                          ],
                          "epics": [
                            {
                              "epic_id": "epc-client-work",
                              "title": "client work",
                              "running_plans_count": 1,
                              "staged_plans_count": 1,
                              "plans": [
                                {
                                  "plan_id": "pln-overview-resource",
                                  "title": "Overview resource of the runtime",
                                  "current_phase": "execute",
                                  "phase_state": "running",
                                  "tasks_done": 7,
                                  "tasks_total": 12
                                }
                              ]
                            }
                          ],
                          "alongside_landings": []
                        }
                        """;

    private static final String ENDED_CLIENTS_JSON = """
                        {
                          "project_id": "pm-mcp-clients",
                          "page": 1,
                          "total_pages": 1,
                          "has_newer": false,
                          "has_older": false,
                          "items": [
                            {
                              "item_id": "pln-relay-drops",
                              "kind": "plan",
                              "title": "Relay drops events on reconnect",
                              "outcome": "merged",
                              "ended_at": "2026-10-14T10:00:00Z"
                            }
                          ]
                        }
                        """;

    private RuntimeFixture fixture;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        writeLiveRecord();
        fixture.writeToken("test-token");
        fixture.start((request, response) -> {
            var path = request.target();
            if ("/api/v1/status".equals(path)) {
                response.json(200, STATUS_JSON);
            } else if ("/api/v1/projects".equals(path)) {
                response.json(200, PROJECTS_JSON);
            } else if ("/api/v1/projects/pm-mcp-clients".equals(path)) {
                response.json(200, DETAIL_CLIENTS_JSON);
            } else if ("/api/v1/projects/plan-marshall".equals(path)) {
                response.json(200, "{\"project_id\":\"plan-marshall\",\"display_name\":\"plan-marshall\",\"plans\":[],\"epics\":[],\"alongside_landings\":[]}");
            } else if (path.startsWith("/api/v1/projects/pm-mcp-clients/ended")) {
                response.json(200, ENDED_CLIENTS_JSON);
            } else if ("/api/v1/reports".equals(path)) {
                response.json(200, "{\"unread_count\":3,\"items\":[]}");
            } else {
                response.send(404, null, "{\"error\":\"not_found\"}");
            }
        });
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
        return new OperatorContext(
                            env,
                            fixture.base().getParent(),
                            true,
                            ClientEnvironment.capture(env),
                            virtual::terminal,
                            null
        );
    }

    private OverviewFullScreenApp createApp(VirtualScreenTerminal virtual, OverviewFullScreenApp.InitialSelection sel) {
        var context = createContext(virtual);
        var store = new OverviewStateStore(context.stateDirectory());
        return new OverviewFullScreenApp(context, virtual.terminal(), sel, clock, store, context.noticeBuffer());
    }

    @Test
    @DisplayName("isStructuralChange detects prefix matches, specific events, and non-structural events")
    void testIsStructuralChange() {
        assertTrue(OverviewFullScreenApp.isStructuralChange("project.created", ""));
        assertTrue(OverviewFullScreenApp.isStructuralChange("phase.entered", ""));
        assertTrue(OverviewFullScreenApp.isStructuralChange("scope.changed", ""));
        assertTrue(OverviewFullScreenApp.isStructuralChange("hold.acquired", ""));
        assertTrue(OverviewFullScreenApp.isStructuralChange("condition.met", ""));
        assertTrue(OverviewFullScreenApp.isStructuralChange("task.completed", ""));
        assertTrue(OverviewFullScreenApp.isStructuralChange("needs_operator.changed", ""));
        assertTrue(OverviewFullScreenApp.isStructuralChange("reset", ""));

        // By type
        assertTrue(OverviewFullScreenApp.isStructuralChange("", "project.something"));
        assertTrue(OverviewFullScreenApp.isStructuralChange("", "phase.state"));
        assertTrue(OverviewFullScreenApp.isStructuralChange("", "scope.defined"));
        assertTrue(OverviewFullScreenApp.isStructuralChange("", "hold.released"));
        assertTrue(OverviewFullScreenApp.isStructuralChange("", "condition.unmet"));
        assertTrue(OverviewFullScreenApp.isStructuralChange("", "reset"));

        // Non-structural
        assertFalse(OverviewFullScreenApp.isStructuralChange("chat.message", "message"));
        assertFalse(OverviewFullScreenApp.isStructuralChange("", "ping"));
    }

    @Test
    @DisplayName("categoryOf maps event types to notice buffer categories")
    void testCategoryOf() {
        assertEquals(ConsoleNoticeBuffer.Category.DECISION, OverviewFullScreenApp.categoryOf(Map.of("type", "question")));
        assertEquals(ConsoleNoticeBuffer.Category.DECISION, OverviewFullScreenApp.categoryOf(Map.of("type", "decision")));
        assertEquals(ConsoleNoticeBuffer.Category.DECISION, OverviewFullScreenApp.categoryOf(Map.of("type", "waiver_request")));
        assertEquals(ConsoleNoticeBuffer.Category.REPORT, OverviewFullScreenApp.categoryOf(Map.of("type", "report")));
        assertEquals(ConsoleNoticeBuffer.Category.REPORT, OverviewFullScreenApp.categoryOf(Map.of("type", "report.sent")));
        assertEquals(ConsoleNoticeBuffer.Category.NEED, OverviewFullScreenApp.categoryOf(Map.of("type", "random")));
        assertEquals(ConsoleNoticeBuffer.Category.NEED, OverviewFullScreenApp.categoryOf(Map.of()));
    }

    @Test
    @DisplayName("countOf handles report counts and caps at 10000")
    void testCountOf() {
        assertEquals(5, OverviewFullScreenApp.countOf(Map.of("count", 5), ConsoleNoticeBuffer.Category.REPORT));
        assertEquals(10_000, OverviewFullScreenApp.countOf(Map.of("count", 50_000), ConsoleNoticeBuffer.Category.REPORT));
        assertEquals(1, OverviewFullScreenApp.countOf(Map.of("count", 1), ConsoleNoticeBuffer.Category.REPORT));
        assertEquals(1, OverviewFullScreenApp.countOf(Map.of("count", 0), ConsoleNoticeBuffer.Category.REPORT));
        assertEquals(1, OverviewFullScreenApp.countOf(Map.of(), ConsoleNoticeBuffer.Category.REPORT));
        assertEquals(1, OverviewFullScreenApp.countOf(Map.of("count", 10), ConsoleNoticeBuffer.Category.DECISION));
    }

    @Test
    @DisplayName("handleLiveEvent handles empty data, malformed data, and notices")
    void testHandleLiveEvent() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var app = createApp(virtual, OverviewFullScreenApp.InitialSelection.DEFAULT);
        var runtime = createContext(virtual).runtime(false);
        app.loadInitialData(runtime);

        // Blank and null data
        app.handleLiveEvent("ping", null);
        app.handleLiveEvent("ping", "");
        app.handleLiveEvent("ping", "   ");

        // Malformed JSON
        app.handleLiveEvent("event", "{not-valid-json");

        // Notice buffering
        var noticeData = "{\"type\":\"decision\",\"message\":\"Need approval\"}";
        app.handleLiveEvent("decision", noticeData);

        // Structural change event
        app.handleLiveEvent("project.updated", "{\"type\":\"project.updated\",\"project_id\":\"pm-mcp-clients\"}");
        assertNotNull(app.buildScreen());
    }

    @Test
    @DisplayName("handleSseDisconnect marks connected false when stopped")
    void testHandleSseDisconnect() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var app = createApp(virtual, OverviewFullScreenApp.InitialSelection.DEFAULT);
        app.stop();
        app.handleSseDisconnect();
        assertFalse(app.buildScreen().connected());
    }

    @Test
    @DisplayName("fetchTargetDetail and fetchTargetEnded validate kinds and project IDs")
    void testFetchTargetDetailAndEnded() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var runtime = createContext(virtual).runtime(false);

        // fetchTargetDetail
        assertNull(OverviewFullScreenApp.fetchTargetDetail(runtime, TuiLevel.Kind.OVERVIEW, "pm-mcp-clients"));
        assertNull(OverviewFullScreenApp.fetchTargetDetail(runtime, TuiLevel.Kind.PROJECT_BY_PLAN, null));
        assertNotNull(OverviewFullScreenApp.fetchTargetDetail(runtime, TuiLevel.Kind.PROJECT_BY_PLAN, "pm-mcp-clients"));
        assertNotNull(OverviewFullScreenApp.fetchTargetDetail(runtime, TuiLevel.Kind.PROJECT_BY_EPIC, "pm-mcp-clients"));
        assertNull(OverviewFullScreenApp.fetchTargetDetail(runtime, TuiLevel.Kind.PROJECT_BY_PLAN, "unknown-project"));

        // fetchTargetEnded
        assertNull(OverviewFullScreenApp.fetchTargetEnded(runtime, TuiLevel.Kind.OVERVIEW, "pm-mcp-clients"));
        assertNull(OverviewFullScreenApp.fetchTargetEnded(runtime, TuiLevel.Kind.ENDED, null));
        assertNotNull(OverviewFullScreenApp.fetchTargetEnded(runtime, TuiLevel.Kind.ENDED, "pm-mcp-clients"));
        assertNull(OverviewFullScreenApp.fetchTargetEnded(runtime, TuiLevel.Kind.ENDED, "unknown-project"));
    }

    @Test
    @DisplayName("fetchProjects and fetchUnreadReports populate cached data")
    void testFetchProjectsAndReports() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var app = createApp(virtual, OverviewFullScreenApp.InitialSelection.DEFAULT);
        var runtime = createContext(virtual).runtime(false);

        app.fetchProjects(runtime);
        app.fetchUnreadReports(runtime);

        var screen = app.buildScreen();
        assertEquals(3, screen.reportsCount());
    }

    @Test
    @DisplayName("isCurrentProjectSetupNeedingOperator checks cached project summary")
    void testIsCurrentProjectSetupNeedingOperator() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var app = createApp(virtual, OverviewFullScreenApp.InitialSelection.DEFAULT);
        var runtime = createContext(virtual).runtime(false);

        // Initially no cached projects or detail
        assertFalse(app.isCurrentProjectSetupNeedingOperator());

        app.fetchProjects(runtime);
        // Still no detail
        assertFalse(app.isCurrentProjectSetupNeedingOperator());

        // Open pm-mcp-clients (setupNeedsOperator == false)
        app.openProject(runtime, "pm-mcp-clients", false, null);
        assertFalse(app.isCurrentProjectSetupNeedingOperator());

        // Open plan-marshall (setupNeedsOperator == true)
        app.openProject(runtime, "plan-marshall", false, null);
        assertTrue(app.isCurrentProjectSetupNeedingOperator());
    }

    @Test
    @DisplayName("openProject and openEnded handle blank and invalid IDs")
    void testOpenProjectAndEnded() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var app = createApp(virtual, OverviewFullScreenApp.InitialSelection.DEFAULT);
        var runtime = createContext(virtual).runtime(false);

        app.openProject(runtime, null, false, null);
        app.openProject(runtime, "   ", false, null);
        app.openProject(runtime, "unknown-project", false, null);
        assertEquals(0, app.stack().depth());

        app.openEnded(runtime, null);
        app.openEnded(runtime, "   ");
        app.openEnded(runtime, "unknown-project");
        assertEquals(0, app.stack().depth());
    }

    @Test
    @DisplayName("applyRefreshedData updates matching levels")
    void testApplyRefreshedData() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var app = createApp(virtual, OverviewFullScreenApp.InitialSelection.DEFAULT);
        var runtime = createContext(virtual).runtime(false);

        app.loadInitialData(runtime);

        // Apply to OVERVIEW
        app.applyRefreshedData(TuiLevel.Kind.OVERVIEW, null, null, null);
        assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());

        // Mismatched kind does not crash
        app.applyRefreshedData(TuiLevel.Kind.PROJECT_BY_PLAN, "pm-mcp-clients", null, null);
        assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());

        // Drill into project
        app.openProject(runtime, "pm-mcp-clients", false, null);
        var detail = OverviewFullScreenApp.fetchTargetDetail(runtime, TuiLevel.Kind.PROJECT_BY_PLAN, "pm-mcp-clients");
        app.applyRefreshedData(TuiLevel.Kind.PROJECT_BY_PLAN, "pm-mcp-clients", detail, null);
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());

        // Open ended
        app.openEnded(runtime, "pm-mcp-clients");
        var ended = OverviewFullScreenApp.fetchTargetEnded(runtime, TuiLevel.Kind.ENDED, "pm-mcp-clients");
        app.applyRefreshedData(TuiLevel.Kind.ENDED, "pm-mcp-clients", null, ended);
        assertEquals(TuiLevel.Kind.ENDED, app.stack().level().kind());
    }

    @Test
    @DisplayName("saveState persists view state across navigation levels")
    void testSaveState() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var context = createContext(virtual);
        var store = new OverviewStateStore(context.stateDirectory());
        var app = new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT, clock, store, context.noticeBuffer());
        var runtime = context.runtime(false);

        // Empty stack save
        app.saveState();
        assertTrue(store.read().isEmpty());

        // Overview level save
        app.loadInitialData(runtime);
        app.saveState();
        var savedOverview = store.read();
        assertTrue(savedOverview.isPresent());
        assertNull(savedOverview.get().projectId());

        // Project by plan save
        app.openProject(runtime, "pm-mcp-clients", false, null);
        app.saveState();
        var savedPlan = store.read();
        assertTrue(savedPlan.isPresent());
        assertEquals("pm-mcp-clients", savedPlan.get().projectId());
        assertEquals("plan", savedPlan.get().projectView());

        // Project by epic save
        app.openProject(runtime, "pm-mcp-clients", true, null);
        app.saveState();
        var savedEpic = store.read();
        assertTrue(savedEpic.isPresent());
        assertEquals("epic", savedEpic.get().projectView());
    }

    @Test
    @DisplayName("loadInitialData supports initial ended, initial project, and saved state")
    void testLoadInitialDataVariants() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var runtime = createContext(virtual).runtime(false);

        // Initial ended with project
        var endedApp = createApp(virtual, new OverviewFullScreenApp.InitialSelection("pm-mcp-clients", null, true));
        endedApp.loadInitialData(runtime);
        assertEquals(TuiLevel.Kind.ENDED, endedApp.stack().level().kind());

        // Initial ended without project (picks first project)
        var endedFirstApp = createApp(virtual, new OverviewFullScreenApp.InitialSelection(null, null, true));
        endedFirstApp.loadInitialData(runtime);
        assertEquals(TuiLevel.Kind.ENDED, endedFirstApp.stack().level().kind());

        // Initial project by epic
        var epicApp = createApp(virtual, new OverviewFullScreenApp.InitialSelection("pm-mcp-clients", "epic", false));
        epicApp.loadInitialData(runtime);
        assertEquals(TuiLevel.Kind.PROJECT_BY_EPIC, epicApp.stack().level().kind());

        // Saved state matching project
        var context = createContext(virtual);
        var store = new OverviewStateStore(context.stateDirectory());
        store.write(new OverviewSavedState("pm-mcp-clients", "plan", "pln-console-history", null, null, null));
        var savedApp = new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT, clock, store, context.noticeBuffer());
        savedApp.loadInitialData(runtime);
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, savedApp.stack().level().kind());
    }

    @Test
    @DisplayName("keyHandler covers navigation, help, system views, and exit")
    void testKeyHandlerNavigationAndHelp() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var app = createApp(virtual, OverviewFullScreenApp.InitialSelection.DEFAULT);
        var runtime = createContext(virtual).runtime(false);
        app.loadInitialData(runtime);

        var handler = app.keyHandler();

        // Navigation
        assertTrue(handler.handle(TuiKeyAction.MOVE_DOWN));
        assertTrue(handler.handle(TuiKeyAction.MOVE_UP));
        assertTrue(handler.handle(TuiKeyAction.PAGE_DOWN));
        assertTrue(handler.handle(TuiKeyAction.PAGE_UP));
        assertTrue(handler.handle(TuiKeyAction.FIRST));
        assertTrue(handler.handle(TuiKeyAction.LAST));

        // Help toggle on overview
        assertTrue(handler.handle(TuiKeyAction.HELP));
        assertTrue(app.stack().helpVisible());
        assertTrue(handler.handle(TuiKeyAction.BACK)); // back dismisses help
        assertFalse(app.stack().helpVisible());

        // Reports and System views
        assertTrue(handler.handle(TuiKeyAction.REPORTS));
        assertEquals(TuiLevel.Kind.REPORTS, app.stack().level().kind());
        assertTrue(handler.handle(TuiKeyAction.BACK));

        assertTrue(handler.handle(TuiKeyAction.SYSTEM));
        assertEquals(TuiLevel.Kind.SYSTEM, app.stack().level().kind());
        assertTrue(handler.handle(TuiKeyAction.BACK));

        // Reload, Actions, Waits
        assertTrue(handler.handle(TuiKeyAction.RELOAD));
        assertTrue(handler.handle(TuiKeyAction.ACTIONS));
        assertTrue(handler.handle(TuiKeyAction.WAITS));

        // Leave returns false
        assertFalse(handler.handle(TuiKeyAction.LEAVE));
    }

    @Test
    @DisplayName("keyHandler covers view switching, jump to epic, and drill-down")
    void testKeyHandlerViewsAndDrillDown() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var app = createApp(virtual, OverviewFullScreenApp.InitialSelection.DEFAULT);
        var runtime = createContext(virtual).runtime(false);
        app.loadInitialData(runtime);

        var handler = app.keyHandler();

        // Open selected project
        app.stack().first(); // select pm-mcp-clients
        assertTrue(handler.handle(TuiKeyAction.OPEN));
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());

        // Help toggle on project view
        assertTrue(handler.handle(TuiKeyAction.HELP));
        assertTrue(handler.handle(TuiKeyAction.HELP)); // toggle off

        // Switch view to epic
        assertTrue(handler.handle(TuiKeyAction.SWITCH_VIEW));
        assertEquals(TuiLevel.Kind.PROJECT_BY_EPIC, app.stack().level().kind());
        assertTrue(handler.handle(TuiKeyAction.SWITCH_VIEW));
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());

        // Jump to epic from plan
        app.stack().first();
        app.stack().move(1); // select pln-overview-resource (which has epic epc-client-work)
        assertTrue(handler.handle(TuiKeyAction.EPIC));
        assertEquals(TuiLevel.Kind.PROJECT_BY_EPIC, app.stack().level().kind());

        // Switch back to plan
        assertTrue(handler.handle(TuiKeyAction.SWITCH_VIEW));

        // Drill down into work item inspection
        assertTrue(handler.handle(TuiKeyAction.OPEN));
        assertEquals(TuiLevel.Kind.PLAN, app.stack().level().kind());
        assertTrue(handler.handle(TuiKeyAction.BACK));
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
    }

    @Test
    @DisplayName("keyHandler covers ended work navigation and filtering")
    void testKeyHandlerEndedWork() throws IOException {
        var virtual = VirtualScreenTerminal.create(120, 30);
        var app = createApp(virtual, OverviewFullScreenApp.InitialSelection.DEFAULT);
        var runtime = createContext(virtual).runtime(false);
        app.loadInitialData(runtime);

        var handler = app.keyHandler();

        // Open selected project
        app.stack().first();
        assertTrue(handler.handle(TuiKeyAction.OPEN));
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());

        // Open ended level from project
        assertTrue(handler.handle(TuiKeyAction.ENDED));
        assertEquals(TuiLevel.Kind.ENDED, app.stack().level().kind());

        // Help on ended level
        assertTrue(handler.handle(TuiKeyAction.HELP));
        assertTrue(handler.handle(TuiKeyAction.HELP));

        // Filter and clear filter via BACK
        handler.filter("relay");
        assertEquals("relay", app.stack().filter());
        assertTrue(handler.handle(TuiKeyAction.BACK));
        assertEquals("", app.stack().filter());

        // Back to project, then back to overview
        assertTrue(handler.handle(TuiKeyAction.BACK));
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
        assertTrue(handler.handle(TuiKeyAction.BACK));
        assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());

        // Open ended level from overview
        app.stack().first();
        assertTrue(handler.handle(TuiKeyAction.ENDED));
        assertEquals(TuiLevel.Kind.ENDED, app.stack().level().kind());
        assertTrue(handler.handle(TuiKeyAction.BACK));
        assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());
    }
}
