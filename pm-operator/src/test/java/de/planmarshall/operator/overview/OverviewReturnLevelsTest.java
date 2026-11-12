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
