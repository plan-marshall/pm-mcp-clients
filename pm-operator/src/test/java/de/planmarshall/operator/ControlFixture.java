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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

/**
 * The runtime fixture of the actions and epic-view screen tests: the control contract's endpoints
 * for one running epic, a running, a paused, and a held plan, and the actions each state offers.
 */
final class ControlFixture implements AutoCloseable {

    static final String STATUS_JSON = "{\"status\":\"ready\",\"version\":\"0.9.1\","
                        + "\"listener\":\"unix\",\"running_since\":\"08:14\",\"workers_count\":2,"
                        + "\"build_slots_used\":1,\"build_slots_total\":2}";

    static final String PROJECTS_JSON = """
                        {"projects":[{"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients",
                          "active_plans_count":3,"active_epics_count":1,"needs_operator_count":0,
                          "setup_needs_operator":false}]}
                        """;

    static final String DETAIL_JSON = """
                        {"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients",
                          "plans":[
                            {"plan_id":"fix-relay","title":"Fix Relay Transport","current_phase":"execute",
                             "phase_state":"running","tasks_done":2,"tasks_total":3,
                             "epic_id":"relay-hardening","epic_title":"Relay Hardening"},
                            {"plan_id":"relay-metrics","title":"Relay Metrics","current_phase":"execute",
                             "phase_state":"paused","tasks_done":1,"tasks_total":2,
                             "epic_id":"relay-hardening","epic_title":"Relay Hardening"},
                            {"plan_id":"relay-cursor-order","title":"Relay Cursor Order",
                             "current_phase":"execute","phase_state":"held","tasks_done":1,"tasks_total":2,
                             "epic_id":"relay-hardening","epic_title":"Relay Hardening"}],
                          "epics":[{"epic_id":"relay-hardening","title":"Relay Hardening",
                             "running_plans_count":3,"staged_plans_count":2,
                             "plans":[{"plan_id":"fix-relay","title":"Fix Relay Transport",
                               "current_phase":"execute","phase_state":"running",
                               "tasks_done":2,"tasks_total":3}]}],
                          "alongside_landings":[
                            {"kind":"landing","unit":"remediation.ci-failure","state":"failed",
                             "hold_kind":"landing_failed"}]}
                        """;

    static final String EPIC_JSON = """
                        {"epic_id":"relay-hardening","project_id":"pm-mcp-clients",
                         "display_name":"pm-mcp-clients","state":"running","auto_start":true,
                         "overlap_max_files":0,"scope":3,"running":3,
                         "workstreams":[
                           {"id":"WS-1","name":"Reconnect","open":true,"rows":[
                             {"plan_id":"relay-reconnect-backoff","title":"relay-reconnect-backoff",
                              "status":"shipped","pull_request_number":211},
                             {"plan_id":"relay-cursor-order","title":"relay-cursor-order","status":"running"},
                             {"plan_id":"relay-reconnect-docs","title":"relay-reconnect-docs",
                              "status":"paused"},
                             {"plan_id":"relay-reconnect-metrics","title":"relay-reconnect-metrics",
                              "status":"staged","wait_reason":"none","objective":"measure the reconnect",
                              "deliverables":["the reconnect metrics"],
                              "surface":["pm-relay/src/metrics"]}]},
                           {"id":"WS-2","name":"Authentication","open":true,"rows":[
                             {"plan_id":"relay-auth-v2","title":"relay-auth-v2","status":"waiting for you"},
                             {"plan_id":"relay-token-rotation","title":"relay-token-rotation",
                              "status":"staged","wait_reason":"dependency","wait_plan_id":"relay-auth-v2"},
                             {"plan_id":"relay-docs","title":"relay-docs","status":"staged",
                              "wait_reason":"overlap","wait_plan_id":"relay-auth-v2",
                              "overlap_files":["pm-api/src/store/Docs.java"]}]}],
                         "sources":["2 documents","1 issue"],"landings":2,
                         "findings":{"new":3,"open":4,"kept":2},
                         "watches":{"new":1,"open":4,"kept":0},
                         "tokens":{"total":{"total":3860000,"input":3520000,"output":340000,"measured":true,
                                            "floor":true,"measured_tasks":4,"not_measured_tasks":1},
                                   "own":{"total":120000,"input":110000,"output":10000,"measured":true,
                                          "floor":false,"measured_tasks":1,"not_measured_tasks":0},
                                   "plans":[{"plan_id":"relay-reconnect-backoff",
                                             "figure":{"total":1400000,"input":1300000,"output":100000,
                                                       "measured":true,"floor":false,"measured_tasks":2,
                                                       "not_measured_tasks":0}}]},
                         "events":[
                           {"at":"2026-10-16T12:40:00Z","text":"relay-auth-v2 asks a decision"},
                           {"at":"2026-10-16T12:05:00Z",
                            "text":"relay-reconnect-backoff landed; relay-reconnect-metrics re-ordered"},
                           {"at":"2026-10-16T11:55:00Z","text":"you paused relay-reconnect-docs"}]}
                        """;

    private static final String RUNNING_PLAN_ACTIONS = """
                        {"subject_id":"fix-relay","subject_kind":"plan","state":"running",
                         "epic_id":"relay-hardening","actions":[
                           {"action":"pause","available":true},{"action":"cancel","available":true},
                           {"action":"chat","available":true},{"action":"settings","available":true}]}
                        """;

    private static final String PAUSED_PLAN_ACTIONS = """
                        {"subject_id":"relay-metrics","subject_kind":"plan","state":"paused",
                         "epic_id":"relay-hardening","actions":[
                           {"action":"start","available":true},{"action":"abandon","available":true},
                           {"action":"chat","available":true},{"action":"settings","available":true}]}
                        """;

    private static final String HELD_PLAN_ACTIONS = """
                        {"subject_id":"relay-cursor-order","subject_kind":"plan","state":"held",
                         "hold_kind":"integrity_pause","epic_id":"relay-hardening","actions":[
                           {"action":"pause","available":true},{"action":"cancel","available":true},
                           {"action":"chat","available":true},{"action":"settings","available":true}]}
                        """;

    private static final String HOLD_JSON = """
                        [{"hold_id":"H-0001","subject_id":"relay-cursor-order","subject_kind":"plan",
                          "kind":"integrity_pause",
                          "cause":"the plan's store file references.json changed outside the runtime",
                          "attempts":0,"file_path":"references.json","recorded_size":4812,
                          "recorded_digest":"3f9a6b2cc21e","current_size":5034,
                          "current_digest":"b70d41aa9a44","acts":["retry","accept","abort"]}]
                        """;

    private static final String EPIC_ACTIONS = """
                        {"subject_id":"relay-hardening","subject_kind":"epic","state":"running",
                         "actions":[
                           {"action":"next","available":true},{"action":"auto","available":true},
                           {"action":"common_files","available":true},{"action":"decompose","available":true},
                           {"action":"findings","available":true},{"action":"watches","available":true},
                           {"action":"chat","available":true},{"action":"archive","available":true},
                           {"action":"cancel","available":true}]}
                        """;

    private static final String ENDED_JSON = """
                        {"project_id":"pm-mcp-clients","page":1,"total_pages":1,"has_newer":false,
                          "has_older":false,"items":[
                            {"item_id":"fix-relay","kind":"plan","title":"Fix Relay Transport",
                             "outcome":"merged","ended_at":"2026-10-18T12:00:00Z","pull_request_number":214},
                            {"item_id":"relay-docs","kind":"epic","title":"relay-docs",
                             "terminal_reason":"completed","ended_at":"2026-10-07T12:00:00Z"}]}
                        """;

    private static final String FIX_RELAY_TREE = """
                        {"plan_id":"fix-relay","title":"Fix Relay Transport",
                          "project":{"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients"},
                          "epic":{"epic_id":"relay-hardening","title":"relay-hardening"},
                          "current_phase":"execute","phase_state":"running","duration_seconds":7800,
                          "tokens_so_far":{"total":412000,"input":380000,"output":32000,"floor":true,
                            "tasks_total":17,"tasks_measured":15,"none_yet":false,"not_measured":false},
                          "nodes":[{"node_id":"execute","title":"phase execute","kind":"phase",
                                    "state":"running","run_count":1,"current_run":1,
                                    "duration_seconds":1500}],
                          "current_position_path":["execute"]}
                        """;

    private final RuntimeFixture fixture;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private final AtomicReference<FakeRuntimeServer.EventStream> activeStream = new AtomicReference<>();
    private final List<String> requests = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();

    private ControlFixture(RuntimeFixture fixture) {
        this.fixture = fixture;
    }

    static ControlFixture create() throws IOException {
        var fixture = RuntimeFixture.create();
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        fixture.writeToken("test-token");
        var support = new ControlFixture(fixture);
        fixture.start(support::handle);
        return support;
    }

    private void handle(FakeRuntimeServer.Request request, FakeRuntimeServer.Response response) throws IOException {
        var path = request.target();
        if ("POST".equals(request.method())) {
            synchronized (requests) {
                requests.add(request.method() + " " + path);
                bodies.add(request.bodyText() != null ? request.bodyText() : "");
            }
        }
        if ("POST".equals(request.method()) && path.endsWith("/holds/H-0001/resolve")) {
            response.json(200, "{\"hold_id\":\"H-0001\",\"released\":true,"
                                + "\"subject\":\"relay-cursor-order\"}");
        } else if ("POST".equals(request.method()) && path.endsWith("/actions/start")) {
            response.json(200, RUNNING_PLAN_ACTIONS);
        } else if ("POST".equals(request.method()) && path.endsWith("/actions/pause")) {
            response.json(200, RUNNING_PLAN_ACTIONS.replace("\"state\":\"running\"",
                                "\"state\":\"pausing\",\"pausing_point\":\"task\""));
        } else if ("POST".equals(request.method()) && path.endsWith("/actions/auto")) {
            response.json(200, EPIC_ACTIONS);
        } else if (path.startsWith("/api/v1/status")) {
            response.json(200, STATUS_JSON);
        } else if ("/api/v1/projects".equals(path)) {
            response.json(200, PROJECTS_JSON);
        } else if ("/api/v1/projects/pm-mcp-clients".equals(path)) {
            response.json(200, DETAIL_JSON);
        } else if (path.startsWith("/api/v1/projects/pm-mcp-clients/ended")) {
            response.json(200, ENDED_JSON);
        } else if ("/api/v1/epics/relay-hardening".equals(path)) {
            response.json(200, EPIC_JSON);
        } else if ("/api/v1/epics/relay-hardening/actions".equals(path)) {
            response.json(200, EPIC_ACTIONS);
        } else if ("/api/v1/plans/fix-relay/actions".equals(path)) {
            response.json(200, RUNNING_PLAN_ACTIONS);
        } else if ("/api/v1/plans/fix-relay/holds".equals(path)) {
            response.json(200, "[]");
        } else if ("/api/v1/plans/fix-relay/chat".equals(path)) {
            response.json(200, """
                                {"chat_id":"C-1","subject_id":"fix-relay","subject_kind":"plan",
                                 "role":"replan","scope":"plan","state":"turn","turns":[
                                   {"role":"operator","text":"Why?","at":"2026-10-16T10:00:00Z",
                                    "untrusted":false},
                                   {"role":"replan","text":"The cursor is written late.",
                                    "at":"2026-10-16T10:01:00Z","untrusted":false}]}
                                """);
        } else if ("/api/v1/epics/relay-hardening/review".equals(path)
                            || path.startsWith("/api/v1/epics/relay-hardening/review?")) {
            response.json(200, """
                                {"review_id":"R-1","epic_id":"relay-hardening","register":"findings",
                                 "state":"open","decided":0,"total":1,"items":[
                                   {"item_id":"F-1","register":"findings","statement":"The cursor is late",
                                    "rules":[],"origin":"landing","trust":"trusted","state":"open"}]}
                                """);
        } else if (path.contains("/landings/remediation.ci-failure/actions/")) {
            response.json(200, "{}");
        } else if ("/api/v1/plans/relay-metrics/actions".equals(path)) {
            response.json(200, PAUSED_PLAN_ACTIONS);
        } else if ("/api/v1/plans/relay-metrics/holds".equals(path)) {
            response.json(200, "[]");
        } else if ("/api/v1/plans/relay-cursor-order/actions".equals(path)) {
            response.json(200, HELD_PLAN_ACTIONS);
        } else if ("/api/v1/plans/relay-cursor-order/holds".equals(path)) {
            response.json(200, HOLD_JSON);
        } else if ("/api/v1/plans/fix-relay/process".equals(path)) {
            response.json(200, FIX_RELAY_TREE);
        } else if ("/api/v1/reports".equals(path)) {
            response.json(200, "{\"unread_count\":0,\"items\":[]}");
        } else if ("/api/v1/events".equals(path)) {
            activeStream.set(response.events(200));
        } else {
            response.send(404, null, "{\"error\":\"not_found\"}");
        }
    }

    Clock clock() {
        return clock;
    }

    List<String> requests() {
        synchronized (requests) {
            return List.copyOf(requests);
        }
    }

    List<String> bodies() {
        synchronized (bodies) {
            return List.copyOf(bodies);
        }
    }

    OperatorContext context(VirtualScreenTerminal virtual) {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        return new OperatorContext(env, fixture.base().getParent(), true,
                            ClientEnvironment.capture(env), virtual::terminal, null);
    }

    OverviewFullScreenApp app(OperatorContext context, VirtualScreenTerminal virtual,
                        OverviewFullScreenApp.InitialSelection selection) {
        return new OverviewFullScreenApp(context, virtual.terminal(), selection, clock,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
    }

    static Thread start(OverviewFullScreenApp app, StringWriter out) {
        var thread = new Thread(() -> {
            try {
                app.run(new PrintWriter(out));
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
        // A failing screen test must not keep the forked test JVM alive.
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    static void delay(long millis) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
    }

    static void await(BooleanSupplier condition) throws InterruptedException {
        var deadline = System.nanoTime() + 8_000_000_000L;
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

    static void join(Thread thread) throws InterruptedException {
        thread.join(8_000L);
        assertFalse(thread.isAlive(), "the driver did not leave the view");
    }

    static boolean shows(VirtualScreenTerminal virtual, String text) {
        return virtual.screen().lines().stream().anyMatch(line -> line.contains(text));
    }

    @Override
    public void close() throws IOException {
        fixture.close();
    }
}
