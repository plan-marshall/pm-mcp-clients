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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("PlanViewFullScreenTest (VirtualScreenTerminal)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PlanViewFullScreenTest {

    private static final String STATUS_JSON = "{\"status\":\"ready\",\"version\":\"0.9.1\","
                        + "\"listener\":\"unix\",\"running_since\":\"08:14\",\"workers_count\":2,"
                        + "\"build_slots_used\":1,\"build_slots_total\":2}";

    private static final String PROJECTS_JSON = """
                        {"projects":[{"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients",
                          "active_plans_count":2,"active_epics_count":1,"needs_operator_count":0,
                          "setup_needs_operator":false}]}
                        """;

    private static final String DETAIL_JSON = """
                        {"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients",
                          "plans":[
                            {"plan_id":"fix-relay","title":"Fix Relay Transport","current_phase":"execute",
                             "phase_state":"running","tasks_done":2,"tasks_total":3,
                             "epic_id":"relay-hardening","epic_title":"Relay Hardening"},
                            {"plan_id":"docs-typo","title":"Fix Documentation Typo","current_phase":"execute",
                             "phase_state":"running","tasks_done":1,"tasks_total":2}],
                          "epics":[{"epic_id":"relay-hardening","title":"Relay Hardening",
                             "running_plans_count":1,"staged_plans_count":1,
                             "plans":[{"plan_id":"fix-relay","title":"Fix Relay Transport",
                               "current_phase":"execute","phase_state":"running",
                               "tasks_done":2,"tasks_total":3}]}],
                          "alongside_landings":[]}
                        """;

    private static final String FIX_RELAY_TREE = """
                        {"plan_id":"fix-relay","title":"Fix Relay Transport",
                          "project":{"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients"},
                          "epic":{"epic_id":"relay-hardening","title":"relay-hardening"},
                          "current_phase":"execute","phase_state":"running","duration_seconds":7800,
                          "pull_request_number":214,"needs_operator":false,
                          "tokens_so_far":{"total":412000,"input":380000,"output":32000,"floor":true,
                            "tasks_total":17,"tasks_measured":15,"none_yet":false,"not_measured":false},
                          "nodes":[
                            {"node_id":"init","title":"phase init","kind":"phase","state":"done",
                             "run_count":1,"current_run":1,"duration_seconds":120},
                            {"node_id":"execute","title":"phase execute","kind":"phase","state":"running",
                             "run_count":1,"current_run":1,"duration_seconds":1500,
                             "sub_nodes":[
                               {"node_id":"execute.envelope","title":"execute.envelope","kind":"tasks",
                                "state":"running","run_count":3,"current_run":3,"duration_seconds":360,
                                "leaf_tasks":[
                                  {"task_id":"TASK-001","role":"implement","harness":"claude","model":"sonnet",
                                   "attempt":1,"status":"done","outcome":"success","duration_seconds":240,
                                   "tokens_so_far":25000},
                                  {"task_id":"TASK-003","role":"implement","harness":"claude","model":"sonnet",
                                   "attempt":2,"status":"running","duration_seconds":360,
                                   "tokens_so_far":48000}]},
                               {"node_id":"remediation.ci-failure","title":"sub-workflow remediation.ci-failure",
                                "kind":"sub_workflow","state":"planned","run_count":0,"current_run":0}]},
                            {"node_id":"finalize","title":"phase finalize","kind":"phase","state":"planned"}],
                          "current_position_path":["execute","execute.envelope","TASK-003"]}
                        """;

    private static final String DOCS_TYPO_TREE = """
                        {"plan_id":"docs-typo","title":"Fix Documentation Typo",
                          "project":{"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients"},
                          "current_phase":"execute","phase_state":"running","duration_seconds":180,
                          "tokens_so_far":{"total":12000,"input":11000,"output":1000,"floor":false,
                            "tasks_total":2,"tasks_measured":2,"none_yet":false,"not_measured":false},
                          "nodes":[
                            {"node_id":"init","title":"phase init","kind":"phase","state":"done",
                             "duration_seconds":60},
                            {"node_id":"execute","title":"phase execute","kind":"phase","state":"running",
                             "duration_seconds":120,"sub_nodes":[
                               {"node_id":"land","title":"sub-workflow land","kind":"sub_workflow",
                                "state":"running","run_count":1,"current_run":1,"duration_seconds":80,
                                "sub_nodes":[
                                  {"node_id":"land.commit","title":"land.commit","kind":"state","state":"done"},
                                  {"node_id":"land.push","title":"land.push","kind":"state","state":"running"}]}]},
                            {"node_id":"refine","title":"refine, outline, plan","kind":"phase",
                             "state":"not_in_plan","prune_reason":"lane_choice"}],
                          "current_position_path":["execute","land","land.push"]}
                        """;

    private static final String ACTIVITY_JSON = """
                        {"task_id":"TASK-003","role":"implement","harness":"claude","model":"sonnet",
                          "attempt":1,"duration_seconds":360,"tokens_so_far":48000,
                          "entries":[
                            {"timestamp":"14:02:11","kind":"text",
                             "content":"\\u001B[31mInjected\\u001B[0m Reading the relay's transport."},
                            {"timestamp":"14:02:20","kind":"tool_call","tool_name":"bash",
                             "tool_target":"./mvnw -pl pm-relay test","tool_failed":true,"size":"4 KiB"}],
                          "off_track":{"signal":"repeating","action":"show","value":5,"threshold":5}}
                        """;

    private static final String COMPLETION_JSON = """
                        {"plan_id":"fix-relay","title":"Fix Relay Transport",
                          "from":{"project_id":"pm-mcp-clients","epic_id":"relay-hardening"},
                          "outcome":"merged","reason":"the relay transport is hardened",
                          "pull_request_number":214,"started_at":"2026-10-18T10:00:00Z",
                          "ended_at":"2026-10-18T12:00:00Z",
                          "deliverables":[{"name":"transport","state":"done"}],
                          "decision_records":[],
                          "token_usage":{"total_tokens":412000,"input_tokens":380000,
                            "output_tokens":32000,"tasks_measured":15,"tasks_total":17}}
                        """;

    private static final String ENDED_JSON = """
                        {"project_id":"pm-mcp-clients","page":1,"total_pages":1,"has_newer":false,
                          "has_older":false,"items":[{"item_id":"fix-relay","kind":"plan",
                            "title":"Fix Relay Transport","outcome":"merged",
                            "ended_at":"2026-10-18T12:00:00Z","pull_request_number":214}]}
                        """;

    private RuntimeFixture fixture;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final AtomicReference<FakeRuntimeServer.EventStream> activeStream = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        activeStream.set(null);
        fixture = RuntimeFixture.create();
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        fixture.writeToken("test-token");
        fixture.start((request, response) -> {
            var path = request.target();
            if (path.startsWith("/api/v1/status")) {
                response.json(200, STATUS_JSON);
            } else if ("/api/v1/projects".equals(path)) {
                response.json(200, PROJECTS_JSON);
            } else if ("/api/v1/projects/pm-mcp-clients".equals(path)) {
                response.json(200, DETAIL_JSON);
            } else if (path.startsWith("/api/v1/projects/pm-mcp-clients/ended")) {
                response.json(200, ENDED_JSON);
            } else if ("/api/v1/plans/fix-relay/process".equals(path)) {
                response.json(200, FIX_RELAY_TREE);
            } else if ("/api/v1/plans/docs-typo/process".equals(path)) {
                response.json(200, DOCS_TYPO_TREE);
            } else if ("/api/v1/plans/fix-relay/tasks/TASK-003/activity".equals(path)) {
                response.json(200, ACTIVITY_JSON);
            } else if ("/api/v1/plans/fix-relay/completion".equals(path)) {
                response.json(200, COMPLETION_JSON);
            } else if ("/api/v1/reports".equals(path)) {
                response.json(200, "{\"unread_count\":0,\"items\":[]}");
            } else if ("/api/v1/events".equals(path)) {
                activeStream.set(response.events(200));
            } else {
                response.send(404, null, "{\"error\":\"not_found\"}");
            }
        });
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private OperatorContext createContext(VirtualScreenTerminal virtual) {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        return new OperatorContext(env, fixture.base().getParent(), true,
                            ClientEnvironment.capture(env), virtual::terminal, null);
    }

    private OverviewFullScreenApp app(OperatorContext context, VirtualScreenTerminal virtual,
                        OverviewFullScreenApp.InitialSelection selection) {
        return new OverviewFullScreenApp(context, virtual.terminal(), selection, clock,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
    }

    private static Thread start(OverviewFullScreenApp app, StringWriter out) {
        var thread = new Thread(() -> {
            try {
                app.run(new PrintWriter(out));
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
        thread.start();
        return thread;
    }

    private static void delay(long millis) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        var deadline = System.nanoTime() + 8_000_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("the plan view did not reach the expected state in time");
            }
            delay(20L);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    private static void join(Thread thread) throws InterruptedException {
        thread.join(8_000L);
        assertFalse(thread.isAlive(), "the driver did not leave the view");
    }

    private static boolean shows(VirtualScreenTerminal virtual, String text) {
        return virtual.screen().lines().stream().anyMatch(line -> line.contains(text));
    }

    @Test
    @DisplayName("plan <id> opens the tree, its path, the running leaf, tokens and the footer keys")
    void shouldOpenPlanDirectly() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var out = new StringWriter();
            var thread = start(app(context, virtual, OverviewFullScreenApp.InitialSelection.plan("fix-relay")), out);

            await(() -> virtual.alternateScreenActive() && shows(virtual, "relay-hardening")
                                && shows(virtual, "execute.envelope"));

            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("pm-mcp-clients \u203a relay-hardening \u203a fix-relay"), screen);
            assertTrue(screen.contains("phase execute"), screen);
            assertTrue(screen.contains("run 3"), screen);
            assertTrue(screen.contains("TASK-003"), screen);
            assertTrue(screen.contains("tokens so far"), screen);
            assertTrue(screen.contains("412 k"), screen);
            assertTrue(screen.contains("p project"), screen);
            assertTrue(screen.contains("e epic"), screen);
            assertFalse(screen.contains("c completion"), screen);
            assertTrue(virtual.screen().lines().stream().anyMatch(line -> line.startsWith("\u25b6") && line.contains("TASK-003")),
                                screen);

            delay(200L);
            virtual.type("q");
            join(thread);
            assertEquals(1, virtual.alternation().entries());
            assertEquals(1, virtual.alternation().exits());
        }
    }

    @Test
    @DisplayName("open, close, open all, where it is and the lane toggle change the tree")
    void shouldUseTreeKeys() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual, OverviewFullScreenApp.InitialSelection.plan("fix-relay")), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "TASK-003"));

            // Close the leaf's tasks, reopen them.
            virtual.type("h");
            await(() -> virtual.screen().lines().stream()
                                .anyMatch(line -> line.startsWith("\u25b6") && line.contains("execute.envelope")));
            virtual.type("h");
            await(() -> !shows(virtual, "TASK-001"));
            virtual.type("l");
            await(() -> shows(virtual, "TASK-001"));

            // Open all below the selected phase.
            virtual.type("h");
            await(() -> !shows(virtual, "TASK-001"));
            virtual.type("h");
            await(() -> virtual.screen().lines().stream()
                                .anyMatch(line -> line.startsWith("\u25b6") && line.contains("phase execute")));
            virtual.type("*");
            await(() -> shows(virtual, "TASK-001"));

            // Jump back to where the plan is.
            virtual.type("G");
            delay(50L);
            virtual.type(".");
            await(() -> virtual.screen().lines().stream()
                                .anyMatch(line -> line.startsWith("\u25b6") && line.contains("TASK-003")));

            delay(200L);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("n hides and shows the phases the land lane does not call")
    void shouldToggleLaneNodes() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual, OverviewFullScreenApp.InitialSelection.plan("docs-typo")), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "not in this plan:"));

            virtual.type("n");
            await(() -> !shows(virtual, "not in this plan:"));
            delay(300L);
            virtual.type("n");
            await(() -> shows(virtual, "not in this plan:"));

            delay(200L);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("t shows and hides the tokens by phase and by role")
    void shouldToggleTokens() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual, OverviewFullScreenApp.InitialSelection.plan("fix-relay")), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "tokens so far"));

            assertFalse(shows(virtual, "by phase"));
            virtual.type("t");
            await(() -> shows(virtual, "by phase") && shows(virtual, "by role")
                                && shows(virtual, "tasks not measured"));
            delay(300L);
            virtual.type("t");
            delay(300L);
            await(() -> !shows(virtual, "by phase"));

            delay(200L);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("Enter on a task opens its worker at work; the stream is plain text")
    void shouldOpenWorkerActivity() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual, OverviewFullScreenApp.InitialSelection.plan("fix-relay")), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "TASK-003"));

            virtual.type("\r");
            await(() -> shows(virtual, "attempt 1") && shows(virtual, "wrote"));

            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("fix-relay \u203a execute.envelope \u203a TASK-003"), screen);
            assertTrue(screen.contains("Injected Reading the relay's transport"), screen);
            assertTrue(screen.contains("bash ./mvnw -pl pm-relay test"), screen);
            assertTrue(screen.contains("failed \u00b7 4 KiB"), screen);
            assertTrue(screen.contains("off track"), screen);
            assertTrue(screen.contains("repeating (5 of 5): shown"), screen);
            assertFalse(virtual.rawOutput().contains("\u001B[31m"), "the stream must not carry the harness's control sequence");

            await(() -> activeStream.get() != null);
            var stream = activeStream.get();
            if (stream != null) {
                try {
                    stream.send("task.off_track", "{\"plan_id\":\"fix-relay\",\"task_id\":\"TASK-003\"}");
                } catch (IOException _) {
                    // the stream may close while the view is leaving
                }
            }
            delay(300L);
            assertTrue(shows(virtual, "Injected Reading the relay's transport"));

            virtual.type("\u001b");
            await(() -> shows(virtual, "execute.envelope") && shows(virtual, "p project"));
            delay(200L);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("p and e lead to the plan's project and epic in one key and Esc returns to the plan")
    void shouldJumpToProjectAndEpic() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual, OverviewFullScreenApp.InitialSelection.plan("fix-relay")), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "p project"));

            virtual.type("p");
            await(() -> shows(virtual, "by plan"));
            delay(200L);
            virtual.type("\u001b");
            await(() -> shows(virtual, "p project"));
            delay(200L);

            virtual.type("e");
            await(() -> shows(virtual, "by epic") && shows(virtual, "Relay Hardening"));
            delay(200L);
            virtual.type("\u001b");
            await(() -> shows(virtual, "p project"));

            delay(200L);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("from the overview, Enter opens the plan and Esc returns to the exact caller level")
    void shouldOpenFromOverviewAndReturn() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual, OverviewFullScreenApp.InitialSelection.DEFAULT), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "pm-mcp-clients"));

            virtual.type("\r");
            await(() -> shows(virtual, "by plan"));
            virtual.type("\r");
            await(() -> shows(virtual, "execute.envelope") && shows(virtual, "p project"));
            // p from a plan opened in the project goes back to that project
            virtual.type("p");
            await(() -> shows(virtual, "by plan"));
            delay(200L);
            virtual.type("\u001b");
            await(() -> shows(virtual, "projects"));
            delay(120L);
            delay(200L);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("e from a plan opened in its epic view returns to that view")
    void shouldReturnToEpicView() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual,
                                new OverviewFullScreenApp.InitialSelection("pm-mcp-clients", "epic", false)),
                                new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "by epic"));
            virtual.type("j");
            await(() -> virtual.screen().lines().stream()
                                .anyMatch(line -> line.startsWith("\u25b6") && line.contains("Fix Relay Transport")));
            virtual.type("\r");
            await(() -> shows(virtual, "p project"));
            virtual.type("e");
            await(() -> shows(virtual, "by epic") && shows(virtual, "Relay Hardening"));
            delay(200L);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("an ended plan shows its outcome, offers c, and opens the completion report")
    void shouldOpenEndedPlanAndCompletion() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual, OverviewFullScreenApp.InitialSelection.DEFAULT), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "pm-mcp-clients"));

            virtual.type("E");
            await(() -> shows(virtual, "Fix Relay Transport") && shows(virtual, "merged"));
            virtual.type("\r");
            await(() -> shows(virtual, "completion report") && shows(virtual, "merged"));
            assertTrue(shows(virtual, "c completion report"), String.join("\n", virtual.screen().lines()));

            virtual.type("c");
            await(() -> shows(virtual, "the relay transport is hardened"));
            assertTrue(shows(virtual, "1 of 1 done"), String.join("\n", virtual.screen().lines()));

            virtual.type("\u001b");
            await(() -> shows(virtual, "c completion report"));
            delay(200L);
            virtual.type("q");
            join(thread);
        }
    }

    @Test
    @DisplayName("a live phase change keeps the selection on its node id")
    void shouldPreserveSelectionOnLiveUpdate() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            var thread = start(app(context, virtual, OverviewFullScreenApp.InitialSelection.plan("fix-relay")), new StringWriter());
            await(() -> virtual.alternateScreenActive() && shows(virtual, "TASK-003"));
            await(() -> activeStream.get() != null);

            var stream = activeStream.get();
            if (stream != null) {
                try {
                    stream.send("phase.changed", "{\"plan_id\":\"fix-relay\",\"phase\":\"execute\"}");
                } catch (IOException _) {
                    // the stream may close while the view is leaving
                }
            }
            delay(300L);
            assertTrue(virtual.screen().lines().stream()
                                                    .anyMatch(line -> line.startsWith("\u25b6") && line.contains("TASK-003")),
                                String.join("\n", virtual.screen().lines()));

            delay(200L);
            virtual.type("q");
            join(thread);
        }
    }


    @Test
    @DisplayName("plan <id> launches the full-screen view through the verb and q leaves it")
    void shouldLaunchThroughPlanCommand() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 40)) {
            var context = createContext(virtual);
            virtual.type("q");
            var out = new StringWriter();
            var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()),
                                "plan", "fix-relay");
            assertEquals(0, code);
            var raw = virtual.rawOutput();
            assertTrue(raw.contains("phase execute"), raw);
            assertTrue(raw.contains("p project"), raw);
            assertEquals(1, virtual.alternation().entries());
            assertEquals(1, virtual.alternation().exits());
        }
    }

    @Test
    @DisplayName("below 60 columns the plan verb falls back to its scrollback tree")
    void shouldFallBackWhenTooNarrow() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(50, 20)) {
            var context = createContext(virtual);
            var out = new StringWriter();
            var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()),
                                "plan", "fix-relay");
            assertEquals(0, code);
            assertTrue(out.toString().contains("phase execute") || virtual.rawOutput().contains("phase execute"),
                                "expected the scrollback tree, out=" + out + ", raw=" + virtual.rawOutput());
        }
    }
}
