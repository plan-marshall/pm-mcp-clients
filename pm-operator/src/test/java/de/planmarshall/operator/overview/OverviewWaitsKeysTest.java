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
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.projects.ProjectsExplorerLevel;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * What {@code !} opens ({@code doc/design/terminal-client/15-overview.adoc#full-screen-keys}) and
 * where {@code Esc} on the {@code plans} root leads
 * ({@code doc/design/terminal-client/15-overview.adoc#full-screen-levels}), driven through the
 * view's key handler. The dialogues a key hands over to read from a terminal whose input has ended,
 * so each leaves at once; what it opened is read from the calls the runtime received.
 */
@DisplayName("OverviewWaitsKeysTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class OverviewWaitsKeysTest {

    private static final String NEEDS_PATH = "/api/v1/machine/needs-operator";

    @TempDir
    Path tempDir;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final List<String> reads = new CopyOnWriteArrayList<>();
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
                            + project + "\",\"active_plans_count\":1,\"active_epics_count\":0,\"needs_operator_count\":1,"
                            + "\"needs_operator_summary\":\"decision\",\"setup_needs_operator\":false}]}");
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
        reads.add(request.method() + " " + path);
        var body = bodyOf.get(path);
        if (body != null) {
            response.json(200, body);
        } else {
            response.send(404, null, "{\"error\":\"not_found\"}");
        }
    }

    private void open() throws IOException {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        var context = new OperatorContext(env, tempDir, true, ClientEnvironment.capture(env), () -> terminal, null);
        app = new OverviewFullScreenApp(context, terminal, OverviewFullScreenApp.InitialSelection.DEFAULT, clock,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
        app.loadInitialData(context.runtime(false));
        keys = app.keyHandler();
        reads.clear();
    }

    private void needs(String... items) {
        bodyOf.put(NEEDS_PATH, "{\"page\":1,\"total_pages\":1,\"has_newer\":false,\"has_older\":false,\"total\":"
                            + items.length + ",\"items\":[" + String.join(",", items) + "]}");
    }

    private static String item(String kind, String fields) {
        return "{\"kind\":\"" + kind + "\"" + (fields.isEmpty() ? "" : "," + fields)
                            + ",\"since\":\"2026-10-20T11:00:00Z\",\"blocks\":\"plan\"}";
    }

    private String rowNaming(String text) {
        return app.stack().visibleRows().stream().filter(row -> row.line().contains(text)).map(TuiRow::id)
                            .findFirst().orElseThrow(() -> new AssertionError("no row names " + text + ": "
                                                + app.stack().visibleRows()));
    }

    private List<String> readsContaining(String fragment) {
        return reads.stream().filter(read -> read.contains(fragment)).toList();
    }

    @Nested
    @DisplayName("! on an item of what waits opens its act")
    class NeedsItems {

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
            "decision with its link | decision | \"link\":\"/api/v1/decisions/Q-71\",\"plan\":{\"id\":\"PLAN\",\"title\":\"Waiting plan\"} | /api/v1/decisions/Q-71",
            "waiver with its link | waiver | \"link\":\"/api/v1/decisions/W-72\",\"plan\":{\"id\":\"PLAN\",\"title\":\"Waiting plan\"} | /api/v1/decisions/W-72",
            "decision of a plan | decision | \"plan\":{\"id\":\"PLAN\",\"title\":\"Waiting plan\"} | /api/v1/plans/PLAN/decisions",
            "hold of a plan | held | \"plan\":{\"id\":\"PLAN\",\"title\":\"Waiting plan\"} | /api/v1/plans/PLAN/holds",
            "failed landing of a plan | landing_failed | \"plan\":{\"id\":\"PLAN\",\"title\":\"Waiting plan\"} | /api/v1/plans/PLAN/holds",
            "stalled plan | stalled | \"plan\":{\"id\":\"PLAN\",\"title\":\"Waiting plan\"} | /api/v1/plans/PLAN/process",
            "condition of a plan | condition | \"plan\":{\"id\":\"PLAN\",\"title\":\"Waiting plan\"} | /api/v1/plans/PLAN/process",
            "setup of a project | setup | \"project\":{\"id\":\"PROJECT\",\"title\":\"Waiting project\"} | PROJECT",
            "findings of an epic | findings | \"epic\":{\"id\":\"epic-waits\",\"title\":\"Waiting epic\"} | epic-waits",
            "hold of an epic | held | \"epic\":{\"id\":\"epic-held\",\"title\":\"Waiting epic\"} | epic-held"
        })
        @DisplayName("an item names its subject, and ! reads the subject's act")
        void opensTheSubjectsAct(String name, String kind, String fields, String expected) throws IOException {
            needs(item(kind, fields.replace("PLAN", plan).replace("PROJECT", project)));
            open();
            app.stack().select(rowNaming("Waiting"));

            assertTrue(keys.handle(TuiKeyAction.WAITS));

            var fragment = expected.replace("PLAN", plan).replace("PROJECT", project);
            assertFalse(readsContaining(fragment).isEmpty(), name + ": " + reads);
        }

        @Test
        @DisplayName("! on a plan's condition opens the plan's view where the runtime serves its process")
        void conditionOpensPlanView() throws IOException {
            needs(item("condition", "\"plan\":{\"id\":\"" + plan + "\",\"title\":\"Waiting plan\"}"));
            bodyOf.put("/api/v1/plans/" + plan + "/process", "{\"plan_id\":\"" + plan + "\",\"project_id\":\"" + project
                                + "\",\"title\":\"Waiting plan\",\"nodes\":[]}");
            open();
            app.stack().select(rowNaming("Waiting"));

            keys.handle(TuiKeyAction.WAITS);

            assertEquals(TuiLevel.Kind.PLAN, app.stack().level().kind());
            assertEquals(plan, app.stack().level().id());
        }

        @Test
        @DisplayName("Enter on an item opens the same act as !")
        void enterOpensTheAct() throws IOException {
            needs(item("held", "\"plan\":{\"id\":\"" + plan + "\",\"title\":\"Waiting plan\"}"));
            open();
            app.stack().select(rowNaming("Waiting"));

            keys.handle(TuiKeyAction.OPEN);

            assertFalse(readsContaining("/api/v1/plans/" + plan + "/holds").isEmpty(), reads.toString());
            assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());
        }
    }

    @Test
    @DisplayName("! on a row that waits for nothing moves to the next row that does, and reads nothing")
    void jumpsToTheNextWaitingRow() throws IOException {
        needs(item("decision", "\"link\":\"/api/v1/decisions/Q-80\",\"plan\":{\"id\":\"" + plan
                            + "\",\"title\":\"Waiting plan\"}"));
        open();
        var waiting = rowNaming("Waiting");
        var other = app.stack().visibleRows().stream().map(TuiRow::id)
                            .filter(id -> !id.equals(waiting) && !app.stack().visibleRows().stream()
                                                .filter(row -> row.id().equals(id)).findFirst().orElseThrow().needsOperator())
                            .findFirst().orElseThrow();
        app.stack().select(other);

        keys.handle(TuiKeyAction.WAITS);

        assertEquals(waiting, app.stack().selected().id());
        assertEquals(List.of(), reads);
    }

    @Test
    @DisplayName("! on a project whose setup waits for the operator starts its setup")
    void projectSetup() throws IOException {
        bodyOf.put("/api/v1/projects", bodyOf.get("/api/v1/projects").replace("\"setup_needs_operator\":false",
                            "\"setup_needs_operator\":true"));
        needs();
        open();
        app.stack().select(project);

        keys.handle(TuiKeyAction.WAITS);

        assertFalse(readsContaining(project).isEmpty(), reads.toString());
    }

    @Nested
    @DisplayName("Esc on the roots")
    class Roots {

        @Test
        @DisplayName("Esc on the plans root opens the projects root, and Esc there stays")
        void plansRootLeadsToProjects() throws IOException {
            bodyOf.put("/api/v1/projects", "{\"favourites\":[],\"recent\":[],\"all_projects\":[{\"project_id\":\""
                                + project + "\",\"display_name\":\"" + project + "\",\"readiness\":\"ready\"}],"
                                + "\"being_added\":[],\"projects\":[{\"project_id\":\"" + project + "\",\"display_name\":\""
                                + project + "\",\"active_plans_count\":0,\"active_epics_count\":0,"
                                + "\"needs_operator_count\":0,\"setup_needs_operator\":false}]}");
            needs();
            open();
            assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());

            assertTrue(keys.handle(TuiKeyAction.BACK));

            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());
            assertEquals(1, app.stack().depth());
            assertNotNull(app.stack().selected());
            assertEquals(ProjectsExplorerLevel.allProjectRowId(project), app.stack().selected().id());
            assertTrue(keys.handle(TuiKeyAction.BACK));
            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());
        }

        @Test
        @DisplayName("Esc on the plans root with the projects unreadable shows the explorer with the refusal")
        void refusedExplorer() throws IOException {
            needs();
            open();
            bodyOf.remove("/api/v1/projects");

            assertTrue(keys.handle(TuiKeyAction.BACK));

            assertEquals(TuiLevel.Kind.PROJECTS, app.stack().level().kind());
            assertFalse(app.stack().rows().isEmpty());
        }

        @Test
        @DisplayName("Esc first closes the help and clears the filter before it leaves the level")
        void helpAndFilterFirst() throws IOException {
            needs();
            open();
            keys.handle(TuiKeyAction.HELP);
            keys.handle(TuiKeyAction.BACK);
            keys.filter(project.substring(0, 3));
            keys.handle(TuiKeyAction.BACK);

            assertEquals(TuiLevel.Kind.OVERVIEW, app.stack().level().kind());
            assertEquals("", app.stack().filter());
            assertFalse(app.stack().helpVisible());
        }
    }
}
