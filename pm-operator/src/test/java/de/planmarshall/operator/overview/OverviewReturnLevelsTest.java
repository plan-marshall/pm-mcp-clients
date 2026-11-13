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

import org.jline.terminal.impl.DumbTerminal;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.resource.ErrorTexts;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.epicview.EpicViewLevel;
import de.planmarshall.operator.reports.ReportsLevel;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;
import de.planmarshall.operator.tui.TuiScreenDriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code Esc} returns to the level that opened the one above it, "with its selection, filter, and
 * scroll position unchanged" ({@code doc/design/terminal-client/15-overview.adoc#full-screen-levels}):
 * the keys and the header of the level it returns to read that level's project, plan or epic, not
 * the one a level above opened in between.
 */
@DisplayName("OverviewReturnLevelsTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class OverviewReturnLevelsTest {

    @TempDir
    Path tempDir;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final Map<String, String> bodyOf = new ConcurrentHashMap<>();

    private RuntimeFixture fixture;
    private OverviewFullScreenApp app;
    private TuiScreenDriver.KeyHandler keys;

    private String first;
    private String second;

    @BeforeEach
    void setUp() throws IOException {
        first = "first-" + Generators.letterStrings(4, 8).next().toLowerCase();
        second = "second-" + Generators.letterStrings(4, 8).next().toLowerCase();
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
        var body = bodyOf.get(request.target());
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

    private static String summary(String projectId) {
        return "{\"project_id\":\"" + projectId + "\",\"display_name\":\"" + projectId + "\",\"active_plans_count\":1,"
                            + "\"active_epics_count\":1,\"needs_operator_count\":0,\"setup_needs_operator\":false}";
    }

    private static String detail(String projectId) {
        return "{\"project_id\":\"" + projectId + "\",\"display_name\":\"" + projectId + "\",\"plans\":[{\"plan_id\":\""
                            + projectId + "-plan\",\"title\":\"" + projectId + " plan\",\"current_phase\":\"execute\","
                            + "\"phase_state\":\"running\",\"epic_id\":\"" + projectId + "-epic\",\"epic_title\":\""
                            + projectId + " epic\"}],\"epics\":[{\"epic_id\":\"" + projectId + "-epic\",\"title\":\""
                            + projectId + " epic\",\"running_plans_count\":1,\"staged_plans_count\":0,\"plans\":[]}],"
                            + "\"alongside_landings\":[]}";
    }

    private static String process(String planId, String projectId, String epicId) {
        return "{\"plan_id\":\"" + planId + "\",\"title\":\"" + planId + "\",\"project\":{\"project_id\":\"" + projectId
                            + "\",\"display_name\":\"" + projectId + "\"},\"epic\":{\"epic_id\":\"" + epicId
                            + "\",\"title\":\"" + epicId + "\"},\"current_phase\":\"execute\",\"phase_state\":\"running\","
                            + "\"nodes\":[{\"node_id\":\"execute\",\"title\":\"phase execute\",\"kind\":\"phase\","
                            + "\"state\":\"running\"}],\"current_position_path\":[]}";
    }

    @Test
    @DisplayName("Tab on a project the operator returned to switches that project's view, not the one opened above it")
    void projectLevelKeepsItsProject() throws IOException {
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "," + summary(second) + "]}");
        bodyOf.put("/api/v1/projects/" + first, detail(first));
        bodyOf.put("/api/v1/projects/" + second, detail(second));
        bodyOf.put("/api/v1/reports", "{\"items\":[{\"id\":\"rep-1\",\"from\":{\"project_id\":\"" + second + "\"},"
                            + "\"kind\":\"project.onboarded\",\"subject\":\"onboarded\",\"created_at\":\"2026-10-20T11:00:00Z\","
                            + "\"sent_at\":\"2026-10-20T11:00:00Z\"}],\"unread_count\":1,\"has_older\":false,\"older_count\":0}");
        open(OverviewFullScreenApp.InitialSelection.DEFAULT);
        app.stack().select(first);
        keys.handle(TuiKeyAction.OPEN);
        keys.handle(TuiKeyAction.REPORTS);
        app.stack().select(ReportsLevel.REPORT_PREFIX + "rep-1");
        keys.handle(TuiKeyAction.PROJECT);
        assertEquals(second, app.stack().level().id());
        keys.handle(TuiKeyAction.BACK);
        keys.handle(TuiKeyAction.BACK);
        assertEquals(first, app.stack().level().id());

        keys.handle(TuiKeyAction.SWITCH_VIEW);

        assertEquals(TuiLevel.Kind.PROJECT_BY_EPIC, app.stack().level().kind());
        assertEquals(first, app.stack().level().id());
        var ids = app.stack().rows().stream().map(TuiRow::id).toList();
        assertTrue(ids.contains(first + "-epic"), ids.toString());
        assertFalse(ids.contains(second + "-epic"), ids.toString());
    }

    @Test
    @DisplayName("e on a plan of a filtered project moves to its epic by epic, and Tab back keeps the filter")
    void epicAndSwitchKeepTheFilter() throws IOException {
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/projects/" + first, detail(first));
        open(OverviewFullScreenApp.InitialSelection.DEFAULT);
        app.stack().select(first);
        keys.handle(TuiKeyAction.OPEN);
        keys.filter(first);
        app.stack().select(first + "-plan");

        assertTrue(keys.handle(TuiKeyAction.EPIC));

        assertEquals(TuiLevel.Kind.PROJECT_BY_EPIC, app.stack().level().kind());
        assertEquals(first + "-epic", app.stack().selected().id());
        assertEquals(first, app.stack().filter());
        assertEquals(2, app.stack().depth());

        keys.handle(TuiKeyAction.SWITCH_VIEW);

        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
        assertEquals(first, app.stack().filter());
        assertEquals(2, app.stack().depth());
    }

    @Test
    @DisplayName("Enter on a plan whose process the runtime refuses stays on the project and names the refusal")
    void refusedPlanOfProject() throws IOException {
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/projects/" + first, detail(first));
        open(OverviewFullScreenApp.InitialSelection.DEFAULT);
        app.stack().select(first);
        keys.handle(TuiKeyAction.OPEN);
        app.stack().select(first + "-plan");

        assertTrue(keys.handle(TuiKeyAction.OPEN));

        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
        assertEquals(first, app.stack().level().id());
        assertEquals(ErrorTexts.title("en", "not_found") + " " + ErrorTexts.cause("en", "not_found"),
                            app.buildScreen().rows().getLast().line().trim());
    }

    private static String endedPage(String projectId, int page, boolean hasOlder, String itemId) {
        return "{\"project_id\":\"" + projectId + "\",\"page\":" + page + ",\"total_pages\":2,\"has_newer\":"
                            + (page > 1) + ",\"has_older\":" + hasOlder + ",\"items\":[{\"id\":\"" + itemId
                            + "\",\"kind\":\"plan\",\"title\":\"" + itemId + "\",\"outcome\":\"merged\","
                            + "\"ended_at\":\"2026-10-19T10:00:00Z\"}]}";
    }

    @Test
    @DisplayName("G on the last row of ended work reads the older page and shows its work below")
    void endedWorkReadsTheOlderPage() throws IOException {
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/projects/" + first + "/ended?page=1", endedPage(first, 1, true, "newer-plan"));
        bodyOf.put("/api/v1/projects/" + first + "/ended?page=2", endedPage(first, 2, false, "older-plan"));
        open(OverviewFullScreenApp.InitialSelection.DEFAULT);
        app.stack().select(first);
        keys.handle(TuiKeyAction.ENDED);
        assertEquals(TuiLevel.Kind.ENDED, app.stack().level().kind());
        assertFalse(rowIds().contains("older-plan"), rowIds().toString());

        assertTrue(keys.handle(TuiKeyAction.LAST));

        assertTrue(rowIds().contains("newer-plan"), rowIds().toString());
        assertTrue(rowIds().contains("older-plan"), rowIds().toString());
    }

    @Test
    @DisplayName("G on the last row of ended work whose older page is refused keeps the rows it has")
    void endedWorkOlderPageRefused() throws IOException {
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/projects/" + first + "/ended?page=1", endedPage(first, 1, true, "newer-plan"));
        open(OverviewFullScreenApp.InitialSelection.DEFAULT);
        app.stack().select(first);
        keys.handle(TuiKeyAction.ENDED);
        var before = rowIds();

        keys.handle(TuiKeyAction.LAST);

        assertEquals(before, rowIds());
    }

    @Test
    @DisplayName("e on a plan that belongs to no epic stays on the plan")
    void epicOfPlanWithoutEpic() throws IOException {
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/projects/" + first, detail(first).replace(",\"epic_id\":\"" + first + "-epic\"", ""));
        open(OverviewFullScreenApp.InitialSelection.DEFAULT);
        app.stack().select(first);
        keys.handle(TuiKeyAction.OPEN);
        app.stack().select(first + "-plan");

        keys.handle(TuiKeyAction.EPIC);

        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
        assertEquals(first + "-plan", app.stack().selected().id());
    }

    @Test
    @DisplayName("the header of a plan the operator returned to names that plan, not the one opened from its epic")
    void planLevelKeepsItsPlan() throws IOException {
        var epic = "epic-" + Generators.letterStrings(4, 8).next().toLowerCase();
        var firstPlan = first + "-plan";
        var secondPlan = second + "-plan";
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/plans/" + firstPlan + "/process", process(firstPlan, first, epic));
        bodyOf.put("/api/v1/plans/" + secondPlan + "/process", process(secondPlan, first, epic));
        bodyOf.put("/api/v1/epics/" + epic, "{\"epic_id\":\"" + epic + "\",\"project_id\":\"" + first
                            + "\",\"display_name\":\"" + first + "\",\"state\":\"running\",\"auto_start\":true,"
                            + "\"overlap_max_files\":0,\"scope\":2,\"running\":2,\"workstreams\":[{\"id\":\"WS-1\","
                            + "\"name\":\"One\",\"open\":true,\"rows\":[{\"plan_id\":\"" + firstPlan + "\",\"title\":\""
                            + firstPlan + "\",\"status\":\"running\"},{\"plan_id\":\"" + secondPlan + "\",\"title\":\""
                            + secondPlan + "\",\"status\":\"running\"}]}],\"sources\":[],\"landings\":0,\"events\":[]}");
        open(OverviewFullScreenApp.InitialSelection.plan(firstPlan));
        assertEquals(TuiLevel.Kind.PLAN, app.stack().level().kind());
        keys.handle(TuiKeyAction.EPIC);
        assertEquals(TuiLevel.Kind.EPIC, app.stack().level().kind());
        app.stack().select(EpicViewLevel.PLAN_PREFIX + secondPlan);
        keys.handle(TuiKeyAction.OPEN);
        assertEquals(secondPlan, app.stack().level().id());
        assertTrue(app.buildScreen().breadcrumb().contains(secondPlan), app.buildScreen().breadcrumb());

        keys.handle(TuiKeyAction.BACK);
        keys.handle(TuiKeyAction.BACK);

        assertEquals(firstPlan, app.stack().level().id());
        var header = app.buildScreen().breadcrumb();
        assertTrue(header.contains(firstPlan), header);
        assertFalse(header.contains(secondPlan), header);
    }

    @Test
    @DisplayName("a completion report opened from the reports belongs to its own project, not to the plan view below")
    void completionKeepsItsProject() throws IOException {
        var epic = "epic-" + Generators.letterStrings(4, 8).next().toLowerCase();
        var firstPlan = first + "-plan";
        var endedPlan = second + "-plan";
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/plans/" + firstPlan + "/process", process(firstPlan, first, epic));
        bodyOf.put("/api/v1/reports", "{\"items\":[{\"id\":\"rep-1\",\"from\":{\"project_id\":\"" + second
                            + "\",\"plan_id\":\"" + endedPlan + "\"},\"kind\":\"plan.ended\",\"subject\":\"ended\","
                            + "\"created_at\":\"2026-10-20T11:00:00Z\",\"sent_at\":\"2026-10-20T11:00:00Z\"}],"
                            + "\"unread_count\":1,\"has_older\":false,\"older_count\":0}");
        bodyOf.put("/api/v1/plans/" + endedPlan + "/completion", "{\"plan_id\":\"" + endedPlan + "\",\"title\":\""
                            + endedPlan + "\",\"from\":{\"project_id\":\"" + second + "\",\"plan_id\":\"" + endedPlan
                            + "\"},\"outcome\":\"merged\",\"started_at\":\"2026-10-20T08:00:00Z\","
                            + "\"ended_at\":\"2026-10-20T11:50:00Z\",\"deliverables\":[],\"replanning_records\":[],"
                            + "\"decision_records\":[]}");
        open(OverviewFullScreenApp.InitialSelection.plan(firstPlan));
        assertEquals(first, app.currentProjectId());
        keys.handle(TuiKeyAction.REPORTS);
        app.stack().select(ReportsLevel.REPORT_PREFIX + "rep-1");

        keys.handle(TuiKeyAction.OPEN);

        assertEquals(TuiLevel.Kind.PLAN_COMPLETION, app.stack().level().kind());
        assertEquals(second, app.currentProjectId());
    }

    private static String endedReport(String reportId, String projectId, String planId) {
        return "{\"id\":\"" + reportId + "\",\"from\":{\"project_id\":\"" + projectId + "\",\"plan_id\":\"" + planId
                            + "\"},\"kind\":\"plan.ended\",\"subject\":\"ended\",\"created_at\":\"2026-10-20T11:00:00Z\","
                            + "\"sent_at\":\"2026-10-20T11:00:00Z\"}";
    }

    private static String completion(String planId, String projectId) {
        return "{\"plan_id\":\"" + planId + "\",\"title\":\"" + planId + "\",\"from\":{\"project_id\":\"" + projectId
                            + "\",\"plan_id\":\"" + planId + "\"},\"outcome\":\"merged\",\"started_at\":\"2026-10-20T08:00:00Z\","
                            + "\"ended_at\":\"2026-10-20T11:50:00Z\",\"deliverables\":[],\"replanning_records\":[],"
                            + "\"decision_records\":[]}";
    }

    @Test
    @DisplayName("Esc to a completion report under another one names its own plan and belongs to its own project")
    void completionLevelKeepsItsReport() throws IOException {
        var firstPlan = first + "-plan";
        var secondPlan = second + "-plan";
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/reports", "{\"items\":[" + endedReport("rep-1", first, firstPlan) + ","
                            + endedReport("rep-2", second, secondPlan) + "],\"unread_count\":2,\"has_older\":false,"
                            + "\"older_count\":0}");
        bodyOf.put("/api/v1/plans/" + firstPlan + "/completion", completion(firstPlan, first));
        bodyOf.put("/api/v1/plans/" + secondPlan + "/completion", completion(secondPlan, second));
        open(OverviewFullScreenApp.InitialSelection.DEFAULT);
        keys.handle(TuiKeyAction.REPORTS);
        app.stack().select(ReportsLevel.REPORT_PREFIX + "rep-1");
        keys.handle(TuiKeyAction.OPEN);
        assertEquals(TuiLevel.Kind.PLAN_COMPLETION, app.stack().level().kind());
        keys.handle(TuiKeyAction.REPORTS);
        app.stack().select(ReportsLevel.REPORT_PREFIX + "rep-2");
        keys.handle(TuiKeyAction.OPEN);
        assertEquals(secondPlan, app.stack().level().id());
        assertEquals(second, app.currentProjectId());

        keys.handle(TuiKeyAction.BACK);
        keys.handle(TuiKeyAction.BACK);

        assertEquals(TuiLevel.Kind.PLAN_COMPLETION, app.stack().level().kind());
        assertEquals(firstPlan, app.stack().level().id());
        assertEquals(first, app.currentProjectId());
        var header = app.buildScreen().breadcrumb();
        assertTrue(header.contains(firstPlan), header);
        assertFalse(header.contains(secondPlan), header);
    }

    private static String processWithTask(String planId, String projectId, String taskId) {
        return "{\"plan_id\":\"" + planId + "\",\"title\":\"" + planId + "\",\"project\":{\"project_id\":\"" + projectId
                            + "\",\"display_name\":\"" + projectId + "\"},\"current_phase\":\"execute\","
                            + "\"phase_state\":\"running\",\"nodes\":[{\"node_id\":\"execute\",\"title\":\"phase execute\","
                            + "\"kind\":\"phase\",\"state\":\"running\",\"leaf_tasks\":[{\"task_id\":\"" + taskId
                            + "\",\"role\":\"implement\",\"harness\":\"claude\",\"model\":\"sonnet\",\"attempt\":1,"
                            + "\"status\":\"running\",\"duration_seconds\":60,\"tokens_so_far\":100}]}],"
                            + "\"current_position_path\":[\"execute\",\"" + taskId + "\"]}";
    }

    private static String activity(String taskId) {
        return "{\"task_id\":\"" + taskId + "\",\"role\":\"implement\",\"harness\":\"claude\",\"model\":\"sonnet\","
                            + "\"attempt\":1,\"duration_seconds\":60,\"tokens_so_far\":100,\"entries\":[]}";
    }

    @Test
    @DisplayName("Esc to a task's activity under another one names that task, and a reload keeps it")
    void activityLevelKeepsItsTask() throws IOException {
        var firstPlan = first + "-plan";
        var secondPlan = second + "-plan";
        var firstTask = first + "-task";
        var secondTask = second + "-task";
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/plans/" + firstPlan + "/process", processWithTask(firstPlan, first, firstTask));
        bodyOf.put("/api/v1/plans/" + secondPlan + "/process", processWithTask(secondPlan, second, secondTask));
        bodyOf.put("/api/v1/plans/" + firstPlan + "/tasks/" + firstTask + "/activity", activity(firstTask));
        bodyOf.put("/api/v1/plans/" + secondPlan + "/tasks/" + secondTask + "/activity", activity(secondTask));
        bodyOf.put("/api/v1/reports", "{\"items\":[{\"id\":\"rep-1\",\"from\":{\"project_id\":\"" + second
                            + "\",\"plan_id\":\"" + secondPlan + "\"},\"kind\":\"project.onboarded\",\"subject\":\"onboarded\","
                            + "\"created_at\":\"2026-10-20T11:00:00Z\",\"sent_at\":\"2026-10-20T11:00:00Z\"}],"
                            + "\"unread_count\":1,\"has_older\":false,\"older_count\":0}");
        open(OverviewFullScreenApp.InitialSelection.plan(firstPlan));
        keys.handle(TuiKeyAction.OPEN);
        assertEquals(TuiLevel.Kind.PLAN_ACTIVITY, app.stack().level().kind());
        keys.handle(TuiKeyAction.REPORTS);
        app.stack().select(ReportsLevel.REPORT_PREFIX + "rep-1");
        keys.handle(TuiKeyAction.OPEN);
        assertEquals(secondPlan, app.stack().level().id());
        keys.handle(TuiKeyAction.OPEN);
        assertEquals(secondPlan + "/" + secondTask, app.stack().level().id());

        keys.handle(TuiKeyAction.BACK);
        keys.handle(TuiKeyAction.BACK);
        keys.handle(TuiKeyAction.BACK);
        keys.handle(TuiKeyAction.RELOAD);

        assertEquals(firstPlan + "/" + firstTask, app.stack().level().id());
        var header = app.buildScreen().breadcrumb();
        assertTrue(header.contains(firstTask), header);
        assertFalse(header.contains(secondTask), header);
    }

    private void openEpicOfPlan() throws IOException {
        var epic = "epic-" + Generators.letterStrings(4, 8).next().toLowerCase();
        var plan = first + "-plan";
        bodyOf.put("/api/v1/projects", "{\"projects\":[" + summary(first) + "]}");
        bodyOf.put("/api/v1/plans/" + plan + "/process", process(plan, first, epic));
        bodyOf.put("/api/v1/epics/" + epic, "{\"epic_id\":\"" + epic + "\",\"project_id\":\"" + first
                            + "\",\"display_name\":\"" + first + "\",\"state\":\"running\",\"auto_start\":true,"
                            + "\"overlap_max_files\":0,\"scope\":1,\"running\":1,\"workstreams\":[{\"id\":\"WS-1\","
                            + "\"name\":\"One\",\"open\":true,\"rows\":[{\"plan_id\":\"" + plan + "\",\"title\":\""
                            + plan + "\",\"status\":\"running\"}]}],\"sources\":[],\"landings\":0,\"events\":[]}");
        open(OverviewFullScreenApp.InitialSelection.plan(plan));
        keys.handle(TuiKeyAction.EPIC);
        assertEquals(TuiLevel.Kind.EPIC, app.stack().level().kind());
    }

    @Test
    @DisplayName("close on a plan of an epic moves to its workstream, closes it there, and * opens it again")
    void closeAndExpandInTheEpic() throws IOException {
        openEpicOfPlan();
        var planRow = EpicViewLevel.PLAN_PREFIX + first + "-plan";
        var workstream = EpicViewLevel.WORKSTREAM_PREFIX + "WS-1";
        app.stack().select(planRow);

        assertTrue(keys.handle(TuiKeyAction.CLOSE));
        assertEquals(workstream, app.stack().selected().id());
        assertTrue(rowIds().contains(planRow), rowIds().toString());

        keys.handle(TuiKeyAction.CLOSE);
        assertEquals(workstream, app.stack().selected().id());
        assertFalse(rowIds().contains(planRow), rowIds().toString());

        keys.handle(TuiKeyAction.EXPAND_ALL);
        assertTrue(rowIds().contains(planRow), rowIds().toString());
    }

    private List<String> rowIds() {
        return app.stack().rows().stream().map(TuiRow::id).toList();
    }

    @Test
    @DisplayName("p on an epic opens its project above it, and Esc returns to the epic")
    void projectAboveEpic() throws IOException {
        bodyOf.put("/api/v1/projects/" + first, detail(first));
        openEpicOfPlan();
        var epicLevel = app.stack().level().id();

        keys.handle(TuiKeyAction.PROJECT);
        assertEquals(TuiLevel.Kind.PROJECT_BY_PLAN, app.stack().level().kind());
        assertEquals(first, app.stack().level().id());
        keys.handle(TuiKeyAction.BACK);

        assertEquals(TuiLevel.Kind.EPIC, app.stack().level().kind());
        assertEquals(epicLevel, app.stack().level().id());
    }

    @Test
    @DisplayName("p on an epic whose project the runtime refuses stays on the epic and names the refusal")
    void refusedProjectOfEpic() throws IOException {
        openEpicOfPlan();
        var epicLevel = app.stack().level().id();

        keys.handle(TuiKeyAction.PROJECT);

        assertEquals(TuiLevel.Kind.EPIC, app.stack().level().kind());
        assertEquals(epicLevel, app.stack().level().id());
        var rows = app.buildScreen().rows();
        assertEquals(ErrorTexts.title("en", "not_found") + " " + ErrorTexts.cause("en", "not_found"),
                            rows.getLast().line().trim());
    }
}
