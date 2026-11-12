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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.ModelMetric;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.api.system.WorkerState;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.system.SystemLevel;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiMenuEntry;
import de.planmarshall.operator.tui.TuiScreenDriver;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The keys of the system level ({@code doc/design/terminal-client/25-system.adoc#full-screen}),
 * driven through the view's key handler: the figures {@code Enter} follows, the window, grouping
 * and project the snapshot is read with, and the menu of the runtime's acts.
 */
@DisplayName("OverviewSystemKeysTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class OverviewSystemKeysTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final List<String> systemReads = new CopyOnWriteArrayList<>();
    private final AtomicReference<@Nullable String> snapshotJson = new AtomicReference<>();
    private final AtomicReference<String> projectsJson = new AtomicReference<>();

    private RuntimeFixture fixture;
    private VirtualScreenTerminal virtual;
    private OverviewFullScreenApp app;
    private TuiScreenDriver.KeyHandler keys;
    private String project;

    @BeforeEach
    void setUp() throws IOException {
        project = "proj-" + Generators.letterStrings(4, 8).next().toLowerCase();
        projectsJson.set("{\"projects\":[{\"project_id\":\"" + project + "\",\"display_name\":\"" + project
                            + "\",\"active_plans_count\":1,\"active_epics_count\":0,\"needs_operator_count\":0,"
                            + "\"setup_needs_operator\":false}]}");
        snapshotJson.set(snapshot("0.10.0", true));
        fixture = RuntimeFixture.create();
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        fixture.writeToken("test-token");
        fixture.start(this::handle);
        virtual = VirtualScreenTerminal.create(120, 30);
    }

    @AfterEach
    void tearDown() throws IOException {
        virtual.close();
        fixture.close();
    }

    private static String snapshot(@Nullable String newerVersion, boolean upgradeSupported) {
        var runtime = new RuntimeMetrics("0.9.1", "2026-10-02 (a1b2c3d)", "today 08:14", 22320L, 432013312L,
                            1073741824L, 713031680L, 4.0, 2469606195L, 40802189312L, "13:40", true, "127.0.0.1:7420",
                            "192.168.1.20:7421", 2, 0, null, newerVersion, null, "today 08:14", null, upgradeSupported,
                            null);
        var work = new WorkMetrics(Map.of("running", 3), Map.of("running", 1), 4, 6, 3, List.of());
        var workers = List.of(new WorkerMetric("planner", "claude", "opus-4.5", WorkerState.BUSY, "fix-relay"));
        var jobs = Map.<String, Object>of("build", Map.of("slots_used", 1, "slots_total", 2, "queued", 1),
                            "locks_held", 3);
        var models = List.of(new ModelMetric("claude:opus-4.5", "planner", 42, 2020000L, 1900000L, 120000L, 0, 1,
                            false, 0));
        var cleanup = new CleanupSummary(12, 2, 40, 5, 1, 1932735283L, 4, "today 03:00", "tomorrow 03:00");
        return JsonTree.write(new SystemSnapshot(runtime, work, workers, jobs, models, cleanup, SystemWindow.HOUR, null)
                            .toMap());
    }

    private void handle(FakeRuntimeServer.Request request, FakeRuntimeServer.Response response) throws IOException {
        var path = request.target();
        var snapshotBody = snapshotJson.get();
        if (path.startsWith("/api/v1/system?")) {
            systemReads.add(path);
            if (snapshotBody != null) {
                response.json(200, snapshotBody);
            } else {
                response.json(500, "{\"error\":\"internal_fault\"}");
            }
        } else if ("/api/v1/projects".equals(path)) {
            response.json(200, projectsJson.get());
        } else if (("/api/v1/projects/" + project).equals(path)) {
            response.json(200, "{\"project_id\":\"" + project + "\",\"display_name\":\"" + project
                                + "\",\"plans\":[],\"epics\":[],\"alongside_landings\":[]}");
        } else if ("/api/v1/reports".equals(path)) {
            response.json(200, "{\"unread_count\":0,\"items\":[]}");
        } else {
            response.send(404, null, "{\"error\":\"not_found\"}");
        }
    }

    private void open() throws IOException {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var context = new OperatorContext(env, fixture.base().getParent(), true, ClientEnvironment.capture(env),
                            virtual::terminal, null);
        app = new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT,
                            clock, new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
        app.loadInitialData(context.runtime(false));
        keys = app.keyHandler();
    }

    private void openSystem() throws IOException {
        open();
        assertTrue(keys.handle(TuiKeyAction.SYSTEM));
        assertEquals(TuiLevel.Kind.SYSTEM, app.stack().level().kind());
        systemReads.clear();
    }

    private void enterOn(String rowId) {
        app.stack().select(rowId);
        assertEquals(rowId, app.stack().selected().id());
        assertTrue(keys.handle(TuiKeyAction.OPEN));
    }

    private String statusLine() {
        return app.buildScreen().rows().getLast().line().trim();
    }

    @Nested
    @DisplayName("Enter follows the selected figure")
    class Figures {

        @BeforeEach
        void system() throws IOException {
            openSystem();
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"sys:workers, WORKERS", "sys:jobs, JOBS", "sys:models, MODELS", "sys:cleanup, CLEANUP"})
        @DisplayName("a figure with details opens them as a level, and Esc returns to the figures")
        void section(String rowId, SystemLevel.Section section) {
            enterOn(rowId);

            assertEquals(TuiLevel.Kind.SYSTEM, app.stack().level().kind());
            assertEquals(SystemLevel.levelId(section), app.stack().level().id());
            assertTrue(keys.handle(TuiKeyAction.BACK));
            assertEquals(SystemLevel.LEVEL_ID, app.stack().level().id());
            assertTrue(app.stack().rows().stream().anyMatch(row -> SystemLevel.ROW_WORK.equals(row.id())));
        }

        @Test
        @DisplayName("the locks open from their row of the jobs and lead no further")
        void locks() {
            enterOn(SystemLevel.ROW_JOBS);

            enterOn(SystemLevel.ROW_LOCKS);
            assertEquals(SystemLevel.levelId(SystemLevel.Section.LOCKS), app.stack().level().id());
            var depth = app.stack().depth();
            assertTrue(keys.handle(TuiKeyAction.OPEN));

            assertEquals(depth, app.stack().depth());
            assertEquals(SystemLevel.levelId(SystemLevel.Section.LOCKS), app.stack().level().id());
        }

        @Test
        @DisplayName("the work figure returns to the overview the system level was opened from")
        void workBackToOverview() {
            enterOn(SystemLevel.ROW_WORK);

            assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());
            assertEquals(1, app.stack().depth());
        }
    }

    @Test
    @DisplayName("the work figure opens the overview as a level where the system sits above a project")
    void workOpensOverviewAboveProject() throws IOException {
        open();
        app.stack().select(project);
        keys.handle(TuiKeyAction.OPEN);
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
        keys.handle(TuiKeyAction.SYSTEM);

        enterOn(SystemLevel.ROW_WORK);

        assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());
        assertEquals(4, app.stack().depth());
        assertTrue(app.stack().rows().stream().anyMatch(row -> project.equals(row.id())));
    }

    @Nested
    @DisplayName("w, b and p read the snapshot with the operator's choice")
    class Choice {

        @BeforeEach
        void system() throws IOException {
            openSystem();
        }

        @Test
        @DisplayName("w moves the window from the last hour to today, then 7 days, then back")
        void window() {
            keys.handle(TuiKeyAction.WINDOW);
            keys.handle(TuiKeyAction.WINDOW);
            keys.handle(TuiKeyAction.WINDOW);

            assertEquals(List.of("/api/v1/system?window=today", "/api/v1/system?window=7d",
                                "/api/v1/system?window=hour"), systemReads);
        }

        @Test
        @DisplayName("b groups the models by role and keeps the window")
        void groupBy() {
            assertTrue(modelsLine().contains("per model"), modelsLine());

            keys.handle(TuiKeyAction.GROUP_BY);

            assertEquals(List.of("/api/v1/system?window=hour"), systemReads);
            assertTrue(modelsLine().contains("per role"), modelsLine());
        }

        private String modelsLine() {
            return app.stack().rows().stream().filter(row -> SystemLevel.ROW_MODELS.equals(row.id()))
                                .map(row -> row.line()).findFirst().orElse("");
        }

        @Test
        @DisplayName("p narrows to the first project with running work, and p again shows all projects")
        void perProject() {
            keys.handle(TuiKeyAction.PER_PROJECT);
            keys.handle(TuiKeyAction.PER_PROJECT);

            assertEquals(List.of("/api/v1/system?window=hour&project=" + project, "/api/v1/system?window=hour"),
                                systemReads);
        }

        @Test
        @DisplayName("a snapshot the runtime does not serve keeps the choice and says the figures are unavailable")
        void unavailableSnapshot() {
            snapshotJson.set(null);

            keys.handle(TuiKeyAction.WINDOW);

            assertEquals(List.of("/api/v1/system?window=today"), systemReads);
            assertTrue(keys.handle(TuiKeyAction.WINDOW));
            assertEquals("/api/v1/system?window=7d", systemReads.getLast());
        }

        @Test
        @DisplayName("w, b and p below the system level read nothing")
        void notAtSystemLevel() {
            keys.handle(TuiKeyAction.BACK);
            assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());

            keys.handle(TuiKeyAction.WINDOW);
            keys.handle(TuiKeyAction.GROUP_BY);
            keys.handle(TuiKeyAction.PER_PROJECT);

            assertEquals(List.of(), systemReads);
        }
    }

    @Test
    @DisplayName("p without a project with running work shows all projects and says why")
    void perProjectWithoutProject() throws IOException {
        projectsJson.set("{\"projects\":[]}");
        openSystem();

        keys.handle(TuiKeyAction.PER_PROJECT);

        assertEquals("No project with running work; the view shows all projects.", statusLine());
        assertEquals(List.of(), systemReads);
    }

    @Nested
    @DisplayName("the acts on the runtime")
    class RuntimeActs {

        private List<String> triggers() {
            var menu = app.buildScreen().menu();
            assertNotNull(menu, "the menu is open");
            return menu.entries().stream().map(TuiMenuEntry::trigger).toList();
        }

        @Test
        @DisplayName("a and Enter on the runtime offer stop, restart and the upgrade the runtime supports")
        void upgradeOffered() throws IOException {
            openSystem();

            keys.handle(Generators.fixedValues(TuiKeyAction.ACTIONS, TuiKeyAction.OPEN).next());

            assertEquals(List.of("s", "r", "u"), triggers());
        }

        @Test
        @DisplayName("a runtime that knows no newer version offers the check instead of the upgrade")
        void checkOffered() throws IOException {
            snapshotJson.set(snapshot(null, false));
            openSystem();

            keys.handle(TuiKeyAction.ACTIONS);

            assertEquals(List.of("s", "r", "c"), triggers());
        }

        @Test
        @DisplayName("a key the menu does not hold leaves it open, the arrows move in it, and Esc closes it")
        void modalMenu() throws IOException {
            openSystem();
            keys.handle(TuiKeyAction.ACTIONS);

            assertTrue(keys.menu(Generators.fixedValues("x", "q", "z", "a").next()));
            assertNotNull(app.buildScreen().menu());
            assertTrue(keys.menuNavigate(1));
            assertEquals(1, app.buildScreen().menu().selectedIndex());

            assertTrue(keys.menu(""));
            assertNull(app.buildScreen().menu());
            assertEquals(List.of(), systemReads);
        }
    }
}
