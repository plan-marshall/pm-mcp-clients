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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jline.terminal.impl.DumbTerminal;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.reports.ReportsLevel;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiMenu;
import de.planmarshall.operator.tui.TuiMenuEntry;
import de.planmarshall.operator.tui.TuiScreenDriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * What {@code a} opens on the levels of ended work, a plan's activity and completion report, and
 * an alongside landing, and what the entry of that menu hands over to
 * ({@code doc/design/terminal-client/19-actions.adoc#full-screen}). The dialogues an entry hands
 * over to read from a terminal whose input has ended, so each leaves at once; what it opened is
 * read from the calls the runtime received.
 */
@DisplayName("OverviewActionsMenuTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class OverviewActionsMenuTest {

    @TempDir
    Path tempDir;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final List<String> calls = new CopyOnWriteArrayList<>();
    private final Map<String, String> bodyOf = new ConcurrentHashMap<>();

    private RuntimeFixture fixture;
    private OverviewFullScreenApp app;
    private TuiScreenDriver.KeyHandler keys;
    private String project;
    private String plan;

    @BeforeEach
    void setUp() throws IOException {
        project = "proj-" + Generators.letterStrings(4, 8).next().toLowerCase();
        plan = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
        bodyOf.put("/api/v1/projects", "{\"projects\":[{\"project_id\":\"" + project + "\",\"display_name\":\""
                            + project + "\",\"active_plans_count\":1,\"active_epics_count\":0,\"needs_operator_count\":0,"
                            + "\"setup_needs_operator\":false}]}");
        bodyOf.put("/api/v1/reports", "{\"unread_count\":0,\"items\":[]}");
        fixture = RuntimeFixture.create();
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        fixture.writeToken("test-token");
        fixture.start(this::handle);
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private void handle(FakeRuntimeServer.Request request, FakeRuntimeServer.Response response) throws IOException {
        var path = request.target();
        calls.add(request.method() + " " + path);
        var body = bodyOf.get(path);
        if (body != null) {
            response.json(200, body);
        } else {
            response.send(404, null, "{\"error\":\"not_found\"}");
        }
    }

    private void open(OverviewFullScreenApp.InitialSelection selection) throws IOException {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        var context = new OperatorContext(env, tempDir, true, ClientEnvironment.capture(env), () -> terminal, null);
        app = new OverviewFullScreenApp(context, terminal, selection, clock,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
        app.loadInitialData(context.runtime(false));
        keys = app.keyHandler();
    }

    private TuiMenu openMenu() {
        calls.clear();
        assertTrue(keys.handle(TuiKeyAction.ACTIONS));
        var menu = app.buildScreen().menu();
        assertNotNull(menu, "a opens a menu");
        return menu;
    }

    private static TuiMenuEntry entryFor(TuiMenu menu, String action) {
        return menu.entries().stream().filter(entry -> action.equals(entry.action())).findFirst()
                            .orElseThrow(() -> new AssertionError("no entry " + action + ": " + menu.entries()));
    }

    private void run(TuiMenuEntry entry) {
        calls.clear();
        assertTrue(keys.menu(entry.trigger()));
    }

    private List<String> callsContaining(String fragment) {
        return calls.stream().filter(call -> call.contains(fragment)).toList();
    }

    private String lastLine() {
        return app.buildScreen().rows().getLast().line().trim();
    }

    private static String endedPage(String projectId, String itemId, String kind) {
        return "{\"project_id\":\"" + projectId + "\",\"page\":1,\"total_pages\":1,\"has_newer\":false,"
                            + "\"has_older\":false,\"items\":[{\"id\":\"" + itemId + "\",\"kind\":\"" + kind
                            + "\",\"title\":\"" + itemId + "\",\"outcome\":\"merged\","
                            + "\"ended_at\":\"2026-10-19T10:00:00Z\"}]}";
    }

    private void openEnded(String itemId, String kind) throws IOException {
        bodyOf.put("/api/v1/projects/" + project + "/ended?page=1", endedPage(project, itemId, kind));
        open(OverviewFullScreenApp.InitialSelection.DEFAULT);
        app.stack().select(project);
        keys.handle(TuiKeyAction.ENDED);
        assertEquals(TuiLevel.Kind.ENDED, app.stack().level().kind());
        app.stack().select(itemId);
    }

    @Nested
    @DisplayName("a on ended work offers the read-only chat alone")
    class EndedWork {

        @Test
        @DisplayName("an ended plan's chat reads the plan's chat and returns to the ended work")
        void endedPlan() throws IOException {
            openEnded(plan, "plan");

            var menu = openMenu();

            assertEquals(1, menu.entries().size(), menu.entries().toString());
            assertTrue(menu.entries().getFirst().label().contains(plan), menu.entries().toString());
            assertEquals(List.of(), calls, "opening the menu of ended work asks the runtime nothing");

            run(entryFor(menu, "chat"));

            assertFalse(callsContaining("GET /api/v1/plans/" + plan + "/chat").isEmpty(), calls.toString());
            assertNull(app.buildScreen().menu(), "the entry closes the menu");
            assertEquals(TuiLevel.Kind.ENDED, app.stack().level().kind());
        }

        @Test
        @DisplayName("an ended epic's chat reads the epic's chat, never a plan's")
        void endedEpic() throws IOException {
            var epic = "epic-" + Generators.letterStrings(4, 8).next().toLowerCase();
            openEnded(epic, "epic");

            run(entryFor(openMenu(), "chat"));

            assertFalse(callsContaining("GET /api/v1/epics/" + epic + "/chat").isEmpty(), calls.toString());
            assertEquals(List.of(), callsContaining("/api/v1/plans/"));
        }

        @Test
        @DisplayName("the plan view of an ended plan offers the same chat")
        void endedPlanView() throws IOException {
            bodyOf.put("/api/v1/plans/" + plan + "/process", process(plan, "merged"));
            openEnded(plan, "plan");
            keys.handle(TuiKeyAction.OPEN);
            assertEquals(TuiLevel.Kind.PLAN, app.stack().level().kind());

            var menu = openMenu();

            assertEquals(1, menu.entries().size(), menu.entries().toString());
            assertEquals(List.of(), callsContaining("/actions"), "an ended plan has no actions to read");
            run(entryFor(menu, "chat"));
            assertFalse(callsContaining("GET /api/v1/plans/" + plan + "/chat").isEmpty(), calls.toString());
        }
    }

    private String process(String planId, String phaseState) {
        return "{\"plan_id\":\"" + planId + "\",\"title\":\"" + planId + "\",\"project\":{\"project_id\":\"" + project
                            + "\",\"display_name\":\"" + project + "\"},\"current_phase\":\"finalize\","
                            + "\"phase_state\":\"" + phaseState + "\",\"nodes\":[{\"node_id\":\"execute\","
                            + "\"title\":\"phase execute\",\"kind\":\"phase\",\"state\":\"done\",\"leaf_tasks\":[{"
                            + "\"task_id\":\"TASK-1\",\"role\":\"implement\",\"harness\":\"claude\",\"model\":\"sonnet\","
                            + "\"attempt\":1,\"status\":\"done\",\"duration_seconds\":60,\"tokens_so_far\":100}]}],"
                            + "\"current_position_path\":[\"execute\",\"TASK-1\"]}";
    }

    @Test
    @DisplayName("a on a completion report offers the chat about its ended plan")
    void completionReport() throws IOException {
        bodyOf.put("/api/v1/reports", "{\"items\":[{\"id\":\"rep-1\",\"from\":{\"project_id\":\"" + project
                            + "\",\"plan_id\":\"" + plan + "\"},\"kind\":\"plan.ended\",\"subject\":\"ended\","
                            + "\"created_at\":\"2026-10-20T11:00:00Z\",\"sent_at\":\"2026-10-20T11:00:00Z\"}],"
                            + "\"unread_count\":1,\"has_older\":false,\"older_count\":0}");
        bodyOf.put("/api/v1/plans/" + plan + "/completion", "{\"plan_id\":\"" + plan + "\",\"title\":\"" + plan
                            + "\",\"from\":{\"project_id\":\"" + project + "\",\"plan_id\":\"" + plan + "\"},"
                            + "\"outcome\":\"merged\",\"started_at\":\"2026-10-20T08:00:00Z\","
                            + "\"ended_at\":\"2026-10-20T11:50:00Z\",\"deliverables\":[],\"replanning_records\":[],"
                            + "\"decision_records\":[]}");
        open(OverviewFullScreenApp.InitialSelection.DEFAULT);
        keys.handle(TuiKeyAction.REPORTS);
        app.stack().select(ReportsLevel.REPORT_PREFIX + "rep-1");
        keys.handle(TuiKeyAction.OPEN);
        assertEquals(TuiLevel.Kind.PLAN_COMPLETION, app.stack().level().kind());

        var menu = openMenu();

        assertEquals(1, menu.entries().size(), menu.entries().toString());
        assertTrue(menu.entries().getFirst().label().contains(plan), menu.entries().toString());
        run(entryFor(menu, "chat"));
        assertFalse(callsContaining("GET /api/v1/plans/" + plan + "/chat").isEmpty(), calls.toString());
        assertEquals(TuiLevel.Kind.PLAN_COMPLETION, app.stack().level().kind());
    }

    @Test
    @DisplayName("a on a task's activity reads the actions of the task's plan")
    void taskActivity() throws IOException {
        bodyOf.put("/api/v1/plans/" + plan + "/process", process(plan, "running")
                            .replace("\"state\":\"done\"", "\"state\":\"running\"")
                            .replace("\"status\":\"done\"", "\"status\":\"running\""));
        bodyOf.put("/api/v1/plans/" + plan + "/tasks/TASK-1/activity", "{\"task_id\":\"TASK-1\",\"role\":\"implement\","
                            + "\"harness\":\"claude\",\"model\":\"sonnet\",\"attempt\":1,\"duration_seconds\":60,"
                            + "\"tokens_so_far\":100,\"entries\":[]}");
        bodyOf.put("/api/v1/plans/" + plan + "/actions", "{\"subject_id\":\"" + plan + "\",\"subject_kind\":\"plan\","
                            + "\"state\":\"running\",\"actions\":[{\"action\":\"pause\",\"available\":true}]}");
        open(OverviewFullScreenApp.InitialSelection.plan(plan));
        keys.handle(TuiKeyAction.OPEN);
        assertEquals(TuiLevel.Kind.PLAN_ACTIVITY, app.stack().level().kind());

        var menu = openMenu();

        assertFalse(callsContaining("GET /api/v1/plans/" + plan + "/actions").isEmpty(), calls.toString());
        assertEquals("pause", menu.entries().getFirst().action());
    }

    @Nested
    @DisplayName("the keys of an open menu")
    class MenuKeys {

        private String reason;

        @BeforeEach
        void runningPlan() throws IOException {
            reason = Generators.letterStrings(8, 16).next();
            bodyOf.put("/api/v1/plans/" + plan + "/process", process(plan, "running")
                                .replace("\"state\":\"done\"", "\"state\":\"running\""));
            bodyOf.put("/api/v1/plans/" + plan + "/actions", "{\"subject_id\":\"" + plan
                                + "\",\"subject_kind\":\"plan\",\"state\":\"running\",\"actions\":["
                                + "{\"action\":\"pause\",\"available\":false,\"reason\":\"" + reason + "\"},"
                                + "{\"action\":\"chat\",\"available\":true}]}");
            open(OverviewFullScreenApp.InitialSelection.plan(plan));
        }

        @Test
        @DisplayName("a disabled entry names its reason, keeps the menu open and sends nothing")
        void disabledEntry() {
            run(entryFor(openMenu(), "pause"));

            assertEquals(reason, lastLine());
            assertNotNull(app.buildScreen().menu());
            assertEquals(List.of(), calls);
        }

        @Test
        @DisplayName("a key no entry carries leaves the menu open and sends nothing")
        void unknownKey() {
            openMenu();
            calls.clear();

            assertTrue(keys.menu("§"));

            assertNotNull(app.buildScreen().menu());
            assertEquals(List.of(), calls);
        }

        @Test
        @DisplayName("closing the menu sends nothing and keeps the plan's row selection")
        void closeMenu() {
            openMenu();
            var row = app.stack().selected().id();
            calls.clear();

            assertTrue(keys.menu(""));

            assertNull(app.buildScreen().menu());
            assertEquals(row, app.stack().selected().id());
            assertEquals(List.of(), calls);
        }
    }

    @Test
    @DisplayName("a on a plan the runtime offers no action for names that, and opens no menu")
    void noActions() throws IOException {
        bodyOf.put("/api/v1/plans/" + plan + "/process", process(plan, "running")
                            .replace("\"state\":\"done\"", "\"state\":\"running\""));
        bodyOf.put("/api/v1/plans/" + plan + "/actions", "{\"subject_id\":\"" + plan
                            + "\",\"subject_kind\":\"plan\",\"state\":\"running\",\"actions\":[]}");
        open(OverviewFullScreenApp.InitialSelection.plan(plan));

        assertTrue(keys.handle(TuiKeyAction.ACTIONS));

        assertNull(app.buildScreen().menu());
        assertEquals(MessageCatalogue.format("en", "operator.actions.none", Map.of("name", plan)), lastLine());
    }

    @Nested
    @DisplayName("a on a plan that waits for a decision")
    class Decide {

        @Test
        @DisplayName("offers the decision first, and its entry opens that decision")
        void decideOpensTheDecision() throws IOException {
            bodyOf.put("/api/v1/plans/" + plan + "/process", process(plan, "waiting")
                                .replace("\"state\":\"done\"", "\"state\":\"running\""));
            bodyOf.put("/api/v1/plans/" + plan + "/actions", "{\"subject_id\":\"" + plan
                                + "\",\"subject_kind\":\"plan\",\"state\":\"waiting\",\"actions\":["
                                + "{\"action\":\"chat\",\"available\":true}]}");
            bodyOf.put("/api/v1/plans/" + plan + "/decisions", "[{\"id\":\"Q-17\"}]");
            open(OverviewFullScreenApp.InitialSelection.plan(plan));

            var menu = openMenu();

            assertEquals("decide", menu.entries().getFirst().action(), menu.entries().toString());
            run(entryFor(menu, "decide"));
            assertFalse(callsContaining("/api/v1/decisions/Q-17").isEmpty(), calls.toString());
        }
    }

    @Nested
    @DisplayName("a on an alongside landing")
    class Landing {

        private static final String UNIT = "remediation.ci-failure";

        @BeforeEach
        void landing() throws IOException {
            bodyOf.put("/api/v1/projects/" + project, "{\"project_id\":\"" + project + "\",\"display_name\":\""
                                + project + "\",\"plans\":[],\"epics\":[],\"alongside_landings\":[{\"kind\":\"landing\","
                                + "\"unit\":\"" + UNIT + "\",\"state\":\"failed\",\"hold_kind\":\"landing_failed\"}]}");
            open(OverviewFullScreenApp.InitialSelection.DEFAULT);
            app.stack().select(project);
            keys.handle(TuiKeyAction.OPEN);
            app.stack().select("landing:" + UNIT);
        }

        @Test
        @DisplayName("the pull request entry names the pull request and sends nothing")
        void showPullRequest() {
            var entry = entryFor(openMenu(), "show_pr");

            run(entry);

            assertEquals(entry.label(), lastLine());
            assertEquals(List.of(), callsContaining("POST"));
        }

        @Test
        @DisplayName("retry sends the landing's act to its project")
        void retry() {
            run(entryFor(openMenu(), "retry"));

            assertEquals(List.of("POST /api/v1/projects/" + project + "/landings/" + UNIT + "/actions/retry"),
                                callsContaining("POST"));
        }
    }
}
