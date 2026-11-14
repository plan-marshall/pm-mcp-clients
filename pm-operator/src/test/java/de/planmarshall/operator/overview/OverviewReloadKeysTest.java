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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
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

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;
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
 * {@code r} reads the level shown again from the runtime
 * ({@code doc/design/terminal-client/15-overview.adoc#full-screen-keys}): the plan view reads the plan's
 * process again, a worker's activity its activity; a read the runtime refuses keeps what the level shows.
 */
@DisplayName("OverviewReloadKeysTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class OverviewReloadKeysTest {

    private static final String TASK = "TASK-003";

    @TempDir
    Path tempDir;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final Map<String, String> bodyOf = new ConcurrentHashMap<>();

    private RuntimeFixture fixture;
    private OverviewFullScreenApp app;
    private TuiScreenDriver.KeyHandler keys;

    private String plan;
    private String processPath;
    private String activityPath;

    @BeforeEach
    void setUp() throws IOException {
        plan = "plan-" + Generators.letterStrings(4, 8).next().toLowerCase();
        processPath = "/api/v1/plans/" + plan + "/process";
        activityPath = "/api/v1/plans/" + plan + "/tasks/" + TASK + "/activity";
        bodyOf.put("/api/v1/reports", "{\"unread_count\":0,\"items\":[]}");
        bodyOf.put("/api/v1/projects", "{\"projects\":[]}");
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
            response.send(500, null, "{\"error\":\"internal_fault\"}");
        }
    }

    private void openPlan() throws IOException {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        var context = new OperatorContext(env, tempDir, true, ClientEnvironment.capture(env), () -> terminal, null);
        app = new OverviewFullScreenApp(context, terminal, OverviewFullScreenApp.InitialSelection.plan(plan), clock,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
        app.loadInitialData(context.runtime(false));
        keys = app.keyHandler();
        assertEquals(TuiLevel.Kind.PLAN, app.stack().level().kind());
    }

    private String process(String phaseTitle) {
        return "{\"plan_id\":\"" + plan + "\",\"title\":\"" + plan + "\",\"project\":{\"project_id\":\"proj\","
                            + "\"display_name\":\"proj\"},\"current_phase\":\"execute\",\"phase_state\":\"running\","
                            + "\"nodes\":[{\"node_id\":\"execute\",\"title\":\"" + phaseTitle + "\",\"kind\":\"phase\","
                            + "\"state\":\"running\",\"sub_nodes\":[{\"node_id\":\"execute.envelope\","
                            + "\"title\":\"execute.envelope\",\"kind\":\"tasks\",\"state\":\"running\",\"leaf_tasks\":["
                            + "{\"task_id\":\"" + TASK + "\",\"role\":\"implement\",\"harness\":\"claude\","
                            + "\"model\":\"sonnet\",\"attempt\":1,\"status\":\"running\"}]}]}],"
                            + "\"current_position_path\":[\"execute\",\"execute.envelope\",\"" + TASK + "\"]}";
    }

    private static String activity(String... contents) {
        var entries = new StringBuilder();
        for (var content : contents) {
            if (!entries.isEmpty()) {
                entries.append(',');
            }
            entries.append("{\"timestamp\":\"14:02:11\",\"kind\":\"text\",\"content\":\"").append(content).append("\"}");
        }
        return "{\"task_id\":\"" + TASK + "\",\"role\":\"implement\",\"harness\":\"claude\",\"model\":\"sonnet\","
                            + "\"attempt\":1,\"entries\":[" + entries + "]}";
    }

    private String text() {
        return String.join("\n", app.stack().rows().stream().map(TuiRow::line).toList());
    }

    @Nested
    @DisplayName("on the plan view")
    class PlanView {

        @Test
        @DisplayName("r reads the plan's process again and shows what changed")
        void reloadShowsChange() throws Exception {
            var before = "phase " + Generators.letterStrings(5, 10).next();
            var after = "phase " + Generators.letterStrings(5, 10).next();
            bodyOf.put(processPath, process(before));
            openPlan();
            assertTrue(text().contains(before), text());

            bodyOf.put(processPath, process(after));
            assertTrue(keys.handle(TuiKeyAction.RELOAD));

            assertTrue(text().contains(after), text());
            assertFalse(text().contains(before), text());
        }

        @Test
        @DisplayName("r the runtime refuses keeps the plan as the level shows it")
        void refusedReloadKeepsRows() throws Exception {
            var before = "phase " + Generators.letterStrings(5, 10).next();
            bodyOf.put(processPath, process(before));
            openPlan();
            var shown = text();

            bodyOf.remove(processPath);
            assertTrue(keys.handle(TuiKeyAction.RELOAD));

            assertEquals(TuiLevel.Kind.PLAN, app.stack().level().kind());
            assertEquals(shown, text());
        }
    }

    @Nested
    @DisplayName("on a worker's activity")
    class Activity {

        private String first;

        @BeforeEach
        void openActivity() throws IOException {
            first = Generators.letterStrings(5, 10).next();
            bodyOf.put(processPath, process("phase execute"));
            bodyOf.put(activityPath, activity(first));
            openPlan();
            keys.handle(TuiKeyAction.OPEN);
            assertEquals(TuiLevel.Kind.PLAN_ACTIVITY, app.stack().level().kind());
        }

        @Test
        @DisplayName("r reads the activity again and follows its last entry")
        void reloadFollowsLastEntry() {
            var second = Generators.letterStrings(5, 10).next();
            bodyOf.put(activityPath, activity(first, second));

            assertTrue(keys.handle(TuiKeyAction.RELOAD));

            assertTrue(text().contains(second), text());
            assertEquals(app.stack().rows().getLast().id(), app.stack().selected().id());
        }

        @Test
        @DisplayName("r after the operator moved up reads the activity again but keeps the selection")
        void reloadKeepsSelectionAfterMoveUp() {
            var second = Generators.letterStrings(5, 10).next();
            var third = Generators.letterStrings(5, 10).next();
            bodyOf.put(activityPath, activity(first, second));
            keys.handle(TuiKeyAction.RELOAD);
            keys.handle(TuiKeyAction.MOVE_UP);
            var selected = app.stack().selected().id();
            bodyOf.put(activityPath, activity(first, second, third));

            keys.handle(TuiKeyAction.RELOAD);

            assertTrue(text().contains(third), text());
            assertEquals(selected, app.stack().selected().id());
        }

        @Test
        @DisplayName("r the runtime refuses keeps the activity as the level shows it")
        void refusedReloadKeepsRows() {
            var shown = text();
            bodyOf.remove(activityPath);

            assertTrue(keys.handle(TuiKeyAction.RELOAD));

            assertEquals(TuiLevel.Kind.PLAN_ACTIVITY, app.stack().level().kind());
            assertEquals(shown, text());
        }
    }
}
