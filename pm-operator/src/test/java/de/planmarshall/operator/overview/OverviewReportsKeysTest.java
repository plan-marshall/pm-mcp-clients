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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.reports.ReportsLevel;
import de.planmarshall.operator.system.SystemLevel;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiScreenDriver;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The keys of the reports level ({@code doc/design/terminal-client/24-reports.adoc#full-screen}),
 * driven through the view's key handler: what {@code Enter}, {@code m}, {@code M}, {@code p} and
 * {@code e} open and send, and what the status line says when the runtime refuses.
 */
@DisplayName("OverviewReportsKeysTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class OverviewReportsKeysTest {

    private static final String REPORTS_PATH = "/api/v1/reports";

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final List<String> calls = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> statusOf = new ConcurrentHashMap<>();

    private RuntimeFixture fixture;
    private VirtualScreenTerminal virtual;
    private OverviewFullScreenApp app;
    private TuiScreenDriver.KeyHandler keys;

    private String project;
    private String plan;
    private String epic;

    @BeforeEach
    void setUp() throws IOException {
        project = "proj-" + Generators.letterStrings(4, 8).next().toLowerCase();
        plan = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
        epic = "epic-" + Generators.letterStrings(4, 8).next().toLowerCase();
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

    private void open(Map<String, String> extraEnvironment) throws IOException {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        env.putAll(extraEnvironment);
        var context = new OperatorContext(env, fixture.base().getParent(), true, ClientEnvironment.capture(env),
                            virtual::terminal, null);
        app = new OverviewFullScreenApp(context, virtual.terminal(), OverviewFullScreenApp.InitialSelection.DEFAULT,
                            clock, new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
        app.loadInitialData(context.runtime(false));
        keys = app.keyHandler();
        assertTrue(keys.handle(TuiKeyAction.REPORTS));
        assertEquals(TuiLevel.Kind.REPORTS, app.stack().level().kind());
    }

    private String reports() {
        var item = "{\"id\":\"%s\",\"from\":{%s},\"kind\":\"%s\",\"subject\":\"%s\","
                            + "\"created_at\":\"2026-10-20T11:%s:00Z\",\"sent_at\":\"2026-10-20T11:%s:00Z\"}";
        var items = List.of(
                            item.formatted("rep-ended", "\"project_id\":\"" + project + "\",\"plan_id\":\"" + plan + "\"",
                                                "plan.ended", plan + " ended", "50", "50"),
                            item.formatted("rep-epic", "\"project_id\":\"" + project + "\",\"epic_id\":\"" + epic + "\"",
                                                "epic.launchable", epic + " launchable", "40", "40"),
                            item.formatted("rep-onboarded", "\"project_id\":\"" + project + "\"",
                                                "project.onboarded", project + " onboarded", "30", "30"),
                            item.formatted("rep-refused", "\"project_id\":\"refused-project\"",
                                                "project.onboarded", "refused onboarded", "20", "20"),
                            item.formatted("rep-cleanup", "\"project_id\":\"system\"",
                                                "system.cleanup_due", "cleanup due", "10", "10"));
        return "{\"items\":[" + String.join(",", items) + "],\"unread_count\":5,\"has_older\":false,"
                            + "\"older_count\":0}";
    }

    private void handle(FakeRuntimeServer.Request request, FakeRuntimeServer.Response response) throws IOException {
        var path = request.target();
        if (!"GET".equals(request.method())) {
            calls.add(request.method() + " " + path + (request.bodyText().isEmpty() ? "" : " " + request.bodyText()));
        }
        var status = statusOf.get(path);
        if (status != null) {
            response.json(status, "{\"error\":\"internal_fault\"}");
        } else if ("/api/v1/projects".equals(path)) {
            response.json(200, "{\"projects\":[{\"project_id\":\"" + project + "\",\"display_name\":\"" + project
                                + "\",\"active_plans_count\":0,\"active_epics_count\":0,\"needs_operator_count\":0,"
                                + "\"setup_needs_operator\":false}]}");
        } else if (("/api/v1/projects/" + project).equals(path)) {
            response.json(200, "{\"project_id\":\"" + project + "\",\"display_name\":\"" + project
                                + "\",\"plans\":[],\"epics\":[],\"alongside_landings\":[]}");
        } else if (("/api/v1/plans/" + plan + "/completion").equals(path)) {
            response.json(200, "{\"plan_id\":\"" + plan + "\",\"title\":\"" + plan + "\",\"from\":{\"project_id\":\""
                                + project + "\",\"plan_id\":\"" + plan + "\"},\"outcome\":\"merged\","
                                + "\"started_at\":\"2026-10-20T08:00:00Z\",\"ended_at\":\"2026-10-20T11:50:00Z\","
                                + "\"deliverables\":[],\"replanning_records\":[],\"decision_records\":[]}");
        } else if (REPORTS_PATH.equals(path)) {
            response.json(200, reports());
        } else if (path.startsWith(REPORTS_PATH + "/")) {
            response.send(204, null, "");
        } else {
            response.send(404, null, "{\"error\":\"not_found\"}");
        }
    }

    private void selectReport(String reportId) {
        app.stack().select(ReportsLevel.REPORT_PREFIX + reportId);
        assertEquals(ReportsLevel.REPORT_PREFIX + reportId, app.stack().selected().id());
    }

    private String statusLine() {
        var rows = app.buildScreen().rows();
        return rows.getLast().line().trim();
    }

    private List<String> reportRowIds() {
        return app.stack().rows().stream().map(row -> row.id())
                            .filter(id -> id.startsWith(ReportsLevel.REPORT_PREFIX)).toList();
    }

    @Nested
    @DisplayName("Enter opens the detail and reads the report")
    class Detail {

        @BeforeEach
        void openReports() throws IOException {
            open(Map.of());
        }

        @Test
        @DisplayName("an ended plan's report opens its completion report, and Esc returns to the reports")
        void endedPlan() {
            selectReport("rep-ended");

            assertTrue(keys.handle(TuiKeyAction.OPEN));

            assertEquals(TuiLevel.Kind.PLAN_COMPLETION, app.stack().level().kind());
            assertEquals(List.of("POST /api/v1/reports/rep-ended/read {}"), calls);
            assertTrue(keys.handle(TuiKeyAction.BACK));
            assertEquals(TuiLevel.Kind.REPORTS, app.stack().level().kind());
            assertTrue(reportRowIds().contains(ReportsLevel.REPORT_PREFIX + "rep-epic"));
        }

        @Test
        @DisplayName("a cleanup that is due opens the cleanup section of the system view")
        void cleanupDue() {
            selectReport("rep-cleanup");

            keys.handle(TuiKeyAction.OPEN);

            assertEquals(TuiLevel.Kind.SYSTEM, app.stack().level().kind());
            assertEquals(SystemLevel.levelId(SystemLevel.Section.CLEANUP), app.stack().level().id());
            assertEquals(List.of("POST /api/v1/reports/rep-cleanup/read {}"), calls);
        }

        @Test
        @DisplayName("an epic the runtime does not serve and a project's report open the project")
        void projectOfReport() {
            selectReport(Generators.fixedValues("rep-epic", "rep-onboarded").next());

            keys.handle(TuiKeyAction.OPEN);

            assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
            assertEquals(project, app.stack().level().id());
            assertEquals(1, calls.size(), calls.toString());
        }

        @Test
        @DisplayName("a project the runtime does not serve opens nothing and leaves the report unread")
        void refusedProjectKeepsReportUnread() {
            selectReport("rep-refused");

            keys.handle(TuiKeyAction.OPEN);

            assertEquals(TuiLevel.Kind.REPORTS, app.stack().level().kind());
            assertEquals("Failed to load reports", statusLine());
            assertEquals(List.of(), calls);
        }

        @Test
        @DisplayName("a read the runtime refuses says so after the detail opened")
        void refusedRead() {
            statusOf.put(REPORTS_PATH + "/rep-onboarded/read", 500);
            selectReport("rep-onboarded");

            keys.handle(TuiKeyAction.OPEN);

            assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
            assertEquals("Failed to mark report read", statusLine());
        }
    }

    @Nested
    @DisplayName("m and M mark reports read")
    class MarkRead {

        @BeforeEach
        void openReports() throws IOException {
            open(Map.of());
        }

        @Test
        @DisplayName("m reads the selected report, which leaves the level")
        void markOne() {
            selectReport("rep-epic");

            keys.handle(TuiKeyAction.MARK_READ);

            assertEquals(List.of("POST /api/v1/reports/rep-epic/read {}"), calls);
            assertTrue(!reportRowIds().contains(ReportsLevel.REPORT_PREFIX + "rep-epic"), reportRowIds().toString());
            assertTrue(app.stack().selected().id().startsWith(ReportsLevel.REPORT_PREFIX));
        }

        @Test
        @DisplayName("m the runtime refuses keeps the report and says so")
        void markOneRefused() {
            statusOf.put(REPORTS_PATH + "/rep-epic/read", 500);
            selectReport("rep-epic");

            keys.handle(TuiKeyAction.MARK_READ);

            assertEquals("Failed to mark report read", statusLine());
            assertTrue(reportRowIds().contains(ReportsLevel.REPORT_PREFIX + "rep-epic"));
        }

        @Test
        @DisplayName("M reads every report the level shows by its ids")
        void markAllShown() {
            keys.handle(TuiKeyAction.MARK_ALL_READ);

            assertEquals(1, calls.size(), calls.toString());
            var call = calls.getFirst();
            assertTrue(call.startsWith("POST /api/v1/reports/read "), call);
            for (var id : List.of("rep-ended", "rep-epic", "rep-onboarded", "rep-refused", "rep-cleanup")) {
                assertTrue(call.contains("\"" + id + "\""), call);
            }
            assertEquals(List.of(), reportRowIds());
        }

        @Test
        @DisplayName("M the runtime refuses keeps the reports and says so")
        void markAllRefused() {
            statusOf.put(REPORTS_PATH + "/read", 500);

            keys.handle(TuiKeyAction.MARK_ALL_READ);

            assertEquals("Failed to mark all reports read", statusLine());
            assertEquals(5, reportRowIds().size());
        }
    }

    @Nested
    @DisplayName("p and e open the report's project and epic without reading it")
    class ProjectAndEpic {

        @BeforeEach
        void openReports() throws IOException {
            open(Map.of());
        }

        @Test
        @DisplayName("p opens the report's project")
        void project() {
            selectReport("rep-ended");

            keys.handle(TuiKeyAction.PROJECT);

            assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
            assertEquals(List.of(), calls);
        }

        @Test
        @DisplayName("e opens the project's view by epic when the runtime does not serve the epic")
        void epicFallsBackToProjectByEpic() {
            selectReport("rep-epic");

            keys.handle(TuiKeyAction.EPIC);

            assertEquals(TuiLevel.Kind.PROJECT_BY_EPIC, app.stack().level().kind());
            assertEquals(List.of(), calls);
        }

        @Test
        @DisplayName("e on a report without an epic and p on the machine's report stay on the reports")
        void nothingToOpen() {
            selectReport("rep-onboarded");
            keys.handle(TuiKeyAction.EPIC);
            selectReport("rep-cleanup");
            keys.handle(TuiKeyAction.PROJECT);

            assertEquals(TuiLevel.Kind.REPORTS, app.stack().level().kind());
            assertEquals(List.of(), calls);
        }
    }

    @Test
    @DisplayName("an act the operator gate refuses names the refusal, not a failure of the runtime")
    void gateRefusalStands() throws IOException {
        open(Map.of("CLAUDECODE", "1"));

        selectReport("rep-epic");
        keys.handle(TuiKeyAction.MARK_READ);
        var single = statusLine();
        keys.handle(TuiKeyAction.MARK_ALL_READ);
        var all = statusLine();

        var refusal = "Operator acts are refused here: this terminal belongs to a coding agent (CLAUDECODE is set). "
                            + "Run pm-operator in a terminal of your own.";
        assertEquals(refusal, single);
        assertEquals(refusal, all);
        assertEquals(List.of(), calls);
        assertEquals(5, reportRowIds().size());
    }
}
