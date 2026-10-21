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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.overview.OverviewFullScreenApp.InitialSelection;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("ProjectsFullScreenTest (VirtualScreenTerminal)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ProjectsFullScreenTest {

    private static final String PROJECTS_JSON = """
                        {
                          "favourites": [
                            {"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients","readiness":"ready","needs_operator_count":2,"active_plans_count":3,"active_epics_count":1,"last_active":"2026-10-20T11:58:00Z","is_favourite":true,"is_released":false}
                          ],
                          "recent": [
                            {"project_id":"plan-marshall","display_name":"plan-marshall","readiness":"setup_needs","setup_needs_summary":"credential sonar","needs_operator_count":1,"active_plans_count":0,"active_epics_count":0,"last_active":"2026-10-19T10:00:00Z","is_favourite":false,"is_released":false}
                          ],
                          "all_projects": [
                            {"project_id":"API-Sheriff","display_name":"API-Sheriff","readiness":"ready","needs_operator_count":0,"active_plans_count":0,"active_epics_count":0,"last_active":"2026-09-12T10:00:00Z","is_favourite":false,"is_released":false},
                            {"project_id":"legacy-portal","display_name":"legacy-portal","readiness":"released","needs_operator_count":0,"active_plans_count":0,"active_epics_count":0,"last_active":"2026-08-17T10:00:00Z","is_favourite":false,"is_released":true},
                            {"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients","readiness":"ready","needs_operator_count":2,"active_plans_count":3,"active_epics_count":1,"last_active":"2026-10-20T11:58:00Z","is_favourite":true,"is_released":false},
                            {"project_id":"plan-marshall","display_name":"plan-marshall","readiness":"setup_needs","setup_needs_summary":"credential sonar","needs_operator_count":1,"active_plans_count":0,"active_epics_count":0,"last_active":"2026-10-19T10:00:00Z","is_favourite":false,"is_released":false}
                          ],
                          "being_added": [
                            {"checkout_path":"~/git/cui-parent","current_step":"Identity","step_number":2,"total_steps":10,"continue_link":"/api/v1/entries/ent-cui/continue","abandon_link":"/api/v1/entries/ent-cui/abandon"}
                          ]
                        }
                        """;

    private static final String DETAIL_CLIENTS_JSON = """
                        {
                          "project_id": "pm-mcp-clients",
                          "display_name": "pm-mcp-clients",
                          "open_entries": [],
                          "plans": [
                            {"plan_id":"fix-relay","title":"Fix the relay","current_phase":"execute","phase_state":"running","tasks_done":2,"tasks_total":4}
                          ],
                          "epics": [],
                          "alongside_landings": []
                        }
                        """;

    private static final String DETAIL_CLIENTS_WITH_ENTRY_JSON = """
                        {
                          "project_id": "pm-mcp-clients",
                          "display_name": "pm-mcp-clients",
                          "open_entries": [
                            {"entry_id":"ent-3f9a","kind":"plan","source_count":2}
                          ],
                          "plans": [],
                          "epics": [],
                          "alongside_landings": []
                        }
                        """;

    private RuntimeFixture fixture;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final AtomicReference<FakeRuntimeServer.EventStream> activeStream = new AtomicReference<>();
    private final AtomicReference<String> projectsBody = new AtomicReference<>(PROJECTS_JSON);
    private final AtomicReference<String> detailBody = new AtomicReference<>(DETAIL_CLIENTS_JSON);
    private final AtomicReference<String> abandonTarget = new AtomicReference<>();
    private final AtomicReference<String> favouriteTarget = new AtomicReference<>();
    private final AtomicReference<String> favouritesBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        activeStream.set(null);
        projectsBody.set(PROJECTS_JSON);
        detailBody.set(DETAIL_CLIENTS_JSON);
        abandonTarget.set(null);
        favouriteTarget.set(null);
        favouritesBody.set(null);
        fixture = RuntimeFixture.create();
        writeLiveRecord();
        fixture.writeToken("test-token");
        fixture.start(this::handle);
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private void handle(FakeRuntimeServer.Request request, FakeRuntimeServer.Response response) throws IOException {
        var path = request.target();
        if ("/api/v1/projects".equals(path)) {
            response.json(200, projectsBody.get());
        } else if (path.endsWith("/favourite")) {
            favouriteTarget.set(request.method() + " " + path);
            response.json(200, "{}");
        } else if (path.startsWith("/api/v1/projects/pm-mcp-clients")) {
            response.json(200, detailBody.get());
        } else if (path.startsWith("/api/v1/projects/plan-marshall")) {
            response.json(200, "{\"project_id\":\"plan-marshall\",\"display_name\":\"plan-marshall\",\"plans\":[],\"epics\":[],\"alongside_landings\":[]}");
        } else if ("/api/v1/explorer/favourites".equals(path)) {
            favouritesBody.set(request.bodyText());
            response.json(200, "{}");
        } else if (path.startsWith("/api/v1/entries/ent-cui/abandon")) {
            abandonTarget.set(request.method() + " " + path);
            response.send(204, null, "");
        } else if ("/api/v1/reports".equals(path)) {
            response.json(200, "{\"unread_count\":0,\"items\":[]}");
        } else if ("/api/v1/events".equals(path)) {
            activeStream.set(response.events(200));
        } else {
            response.send(404, null, "{\"error\":\"not_found\"}");
        }
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

    private OverviewFullScreenApp app(OperatorContext context, VirtualScreenTerminal virtual) {
        return new OverviewFullScreenApp(context, virtual.terminal(), InitialSelection.explorerRoot(), clock,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
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

    private Thread start(OverviewFullScreenApp app, StringWriter out) {
        var thread = new Thread(() -> {
            try {
                app.run(new PrintWriter(out));
            } catch (IOException failure) {
                /*~~(TODO: Throw specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/
                throw new RuntimeException(failure);
            }
        });
        thread.start();
        return thread;
    }

    private static boolean shows(VirtualScreenTerminal virtual, String text) {
        return virtual.screen().lines().stream().anyMatch(line -> line.contains(text));
    }

    private static void selectPmMcpClients(VirtualScreenTerminal virtual) throws InterruptedException {
        // The first selectable row is the onboarding run; one move reaches the favourite project.
        virtual.type("j");
        await(() -> virtual.invertedRows().stream()
                            .map(row -> virtual.screen().lines().get(row))
                            .anyMatch(line -> line.contains("pm-mcp-clients")));
    }

    private static void leaveMenu(VirtualScreenTerminal virtual) throws InterruptedException {
        virtual.type("\u001b");
        await(() -> !shows(virtual, "choose"));
    }

    @Test
    @DisplayName("renders Being added, Favourites, Recent, and All projects at 120x30")
    void rendersAllGroups() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "All projects"));

            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("Being added"), screen);
            assertTrue(screen.contains("~/git/cui-parent"), screen);
            assertTrue(screen.contains("Favourites"), screen);
            assertTrue(screen.contains("\u2605 pm-mcp-clients"), screen);
            assertTrue(screen.contains("! 2") || screen.contains("needs you: 2"), screen);
            assertTrue(screen.contains("3 plans \u00b7 1 epic"), screen);
            assertTrue(screen.contains("Recent"), screen);
            assertTrue(screen.contains("setup needs: credential sonar"), screen);
            assertTrue(screen.contains("All projects \u00b7 by name"), screen);
            assertTrue(screen.contains("released"), screen);
            assertTrue(screen.contains("Enter actions"), screen);
            assertTrue(screen.contains("favourite"), screen);
            assertTrue(screen.contains("sort"), screen);

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("Enter expands the state-specific actions of a ready project")
    void readyProjectActions() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "pm-mcp-clients"));
            selectPmMcpClients(virtual);

            virtual.type("\r");
            await(() -> shows(virtual, "Add an epic"));
            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("e  Add an epic"), screen);
            assertTrue(screen.contains("p  Add a plan"), screen);
            assertTrue(screen.contains("r  Running work"), screen);
            assertTrue(screen.contains("u  Setup"), screen);
            assertTrue(screen.contains("*  Remove from favourites"), screen);
            assertTrue(screen.contains("\u2191\u2193 choose"), screen);

            leaveMenu(virtual);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("Enter on a project that is not ready disables both Add actions with the reason")
    void notReadyProjectActions() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "plan-marshall"));
            selectPmMcpClients(virtual);
            virtual.type("j");
            virtual.type("\r");
            await(() -> shows(virtual, "Continue the setup"));
            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("c  Continue the setup"), screen);
            assertTrue(screen.contains("e  Add an epic (plan-marshall is not ready: credential sonar)"), screen);
            assertTrue(screen.contains("p  Add a plan (plan-marshall is not ready: credential sonar)"), screen);

            leaveMenu(virtual);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("Enter on a project being added offers Continue and Abandon")
    void beingAddedActions() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "cui-parent"));

            virtual.type("\r");
            await(() -> shows(virtual, "Abandon"));
            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("c  Continue"), screen);
            assertTrue(screen.contains("x  Abandon"), screen);

            leaveMenu(virtual);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("Enter on a released project offers Enrol again and Ended work")
    void releasedProjectActions() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "legacy-portal"));

            // Favourites and Recent precede All projects; the header rows are skipped by the view.
            for (int step = 0; step < 4; step++) {
                virtual.type("j");
            }
            await(() -> virtual.invertedRows().stream()
                                .map(row -> virtual.screen().lines().get(row))
                                .anyMatch(line -> line.contains("legacy-portal")));
            virtual.type("\r");
            await(() -> shows(virtual, "Enrol again"));
            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("a  Enrol again"), screen);
            assertTrue(screen.contains("E  Ended work"), screen);
            assertFalse(screen.contains("Running work"), screen);

            leaveMenu(virtual);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("* toggles the selected project's favourite and s switches the All projects order")
    void favouriteAndSort() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "pm-mcp-clients"));
            selectPmMcpClients(virtual);

            virtual.type("*");
            await(() -> favouriteTarget.get() != null);
            assertEquals("PUT /api/v1/projects/pm-mcp-clients/favourite", favouriteTarget.get());

            virtual.type("s");
            await(() -> shows(virtual, "All projects \u00b7 most used"));
            virtual.type("s");
            await(() -> shows(virtual, "All projects \u00b7 by name"));

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("J moves a favourite down and persists the new order")
    void reordersFavourites() throws Exception {
        projectsBody.set("""
                            {
                              "favourites": [
                                {"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients","readiness":"ready","is_favourite":true},
                                {"project_id":"pm-mcp-core","display_name":"pm-mcp-core","readiness":"ready","is_favourite":true}
                              ],
                              "recent": [],
                              "all_projects": [
                                {"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients","readiness":"ready","is_favourite":true},
                                {"project_id":"pm-mcp-core","display_name":"pm-mcp-core","readiness":"ready","is_favourite":true}
                              ],
                              "being_added": []
                            }
                            """);
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "pm-mcp-clients"));

            virtual.type("J");
            await(() -> favouritesBody.get() != null);
            assertTrue(favouritesBody.get().contains("pm-mcp-core"), favouritesBody.get());
            assertTrue(favouritesBody.get().indexOf("pm-mcp-core")
                                < favouritesBody.get().indexOf("pm-mcp-clients"), favouritesBody.get());

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("/ filters the groups into one matching list and Esc restores them")
    void filtersGroups() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "Favourites"));

            virtual.type("/");
            await(() -> shows(virtual, "/ "));
            virtual.type("legacy\n");
            await(() -> shows(virtual, "legacy-portal") && !shows(virtual, "Favourites"));
            assertFalse(shows(virtual, "Being added"));

            virtual.type("\u001b");
            await(() -> shows(virtual, "Favourites"));
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("r opens the selected project's level of the overview")
    void runningWorkLevel() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "pm-mcp-clients"));
            selectPmMcpClients(virtual);

            virtual.type("r");
            await(() -> shows(virtual, "Fix the relay"));
            assertTrue(shows(virtual, "by plan"));

            virtual.type("\u001b");
            await(() -> shows(virtual, "Favourites"));
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("x asks first and deletes the onboarding run of a project being added")
    void abandonsOnboarding() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "cui-parent"));

            virtual.type("x");
            await(() -> shows(virtual, "Nothing was enrolled"));
            virtual.type("y\n");
            await(() -> abandonTarget.get() != null);
            assertEquals("DELETE /api/v1/entries/ent-cui/abandon", abandonTarget.get());

            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("a ready project with an open entry offers Continue starting first")
    void openEntryFirst() throws Exception {
        detailBody.set(DETAIL_CLIENTS_WITH_ENTRY_JSON);
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "pm-mcp-clients"));
            selectPmMcpClients(virtual);

            virtual.type("\r");
            await(() -> shows(virtual, "Continue starting"));
            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("c  Continue starting (2 sources)"), screen);

            leaveMenu(virtual);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("the command opens the explorer full screen when --list is not set")
    void commandOpensFullScreen() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            virtual.type("q");
            var code = PmOperator.execute(context, new PrintWriter(new StringWriter()),
                                new PrintWriter(new StringWriter()), "projects");
            assertEquals(0, code);
            assertEquals(1, virtual.screen().alternation().entries());
            assertTrue(virtual.alternateScreenActive() || virtual.screen().alternation().exits() == 1);
        }
    }

    @Test
    @DisplayName("the command keeps the scrollback list with --list")
    void commandListsWithListFlag() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var out = new StringWriter();
            var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()),
                                "projects", "--list");
            assertEquals(0, code);
            assertTrue(out.toString().contains("All projects (by name)"), out.toString());
            assertEquals(0, virtual.screen().alternation().entries());
        }
    }

    @Test
    @DisplayName("--project ? shows the explorer's projects favourites first and replaces the question")
    void selectsProject() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var context = createContext(virtual);
            var out = new StringWriter();
            var err = new StringWriter();
            virtual.type("2\n");
            var outcome = ProjectSelector.rewrite(context, new PrintWriter(out), new PrintWriter(err),
                                new String[]{"reports", "--project", "?"});
            assertTrue(outcome.proceed(), err.toString());
            assertEquals(List.of("reports", "--project", "plan-marshall"), outcome.args());
            assertTrue(out.toString().contains("Which project?"), out.toString());
            assertTrue(out.toString().contains("1) pm-mcp-clients"), out.toString());
        }
    }
}
