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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.api.system.WorkerState;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.RuntimeActDialogue.Act;
import de.planmarshall.operator.RuntimeActDialogue.Answer;
import de.planmarshall.operator.RuntimeActDialogue.When;
import de.planmarshall.operator.system.SystemView;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The confirmation of a runtime act ({@code 25-system.adoc#t-stop}, CLI-UC-S-02, CLI-UC-P-05): what
 * runs, the choice of when, and {@code k} keeping the runtime running. It is driven over an
 * in-memory terminal, so the screen the operator reads is the assertion and no process starts.
 */
@DisplayName("RuntimeActDialogue")
@EnableGeneratorController
class RuntimeActDialogueTest {

    private static final String ENGLISH = "en_US.UTF-8";
    private static final String GERMAN = "de_DE.UTF-8";

    /** The short snapshot bound the test that waits it out passes instead of the production 5 s. */
    private static final Duration SNAPSHOT_BOUND = Duration.ofMillis(300);

    /** The one deploy slot the snapshot serves, and the one slot the expected line counts. */
    private static final int DEPLOY_SLOTS = 1;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private OperatorContext context(VirtualScreenTerminal virtual, String locale) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", locale);
        return new OperatorContext(environment, fixture.base().getParent(), true,
                            ClientEnvironment.capture(environment), virtual::terminal, null);
    }

    private FakeRuntimeServer serve(int status, String body) throws IOException {
        PmOperatorTest.writeLiveRecord(fixture);
        return fixture.start((request, response) -> {
            if ("/api/v1/system".equals(request.target())) {
                response.send(status, null, body);
            } else {
                response.send(202, null, "");
            }
        });
    }

    private Answer ask(String keys, Act act, String version, boolean noInput) throws IOException {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            virtual.type(keys);
            return RuntimeActDialogue.confirm(context(virtual, ENGLISH), new PrintWriter(out), new PrintWriter(err),
                                act, null, noInput, version);
        }
    }

    @Test
    @DisplayName("the confirmation's counts read in the singular and with a zero group, in both languages")
    void runningNowSingularAndZero() throws Exception {
        var one = SystemSnapshot.fromJson(snapshotJson(1, 1, 1, 0, 0, 1));
        var zero = SystemSnapshot.fromJson(snapshotJson(0, 1, 1, 0, 0, 1));

        assertAll(
                            () -> assertEquals("1 plan \u00b7 1 epic \u00b7 1 task \u00b7 1 job \u00b7 1 worker",
                                                SystemView.runningNow(ENGLISH, one)),
                            () -> assertEquals("1 epic \u00b7 1 task \u00b7 1 job \u00b7 1 worker",
                                                SystemView.runningNow(ENGLISH, zero)),
                            () -> assertEquals("1 Plan \u00b7 1 Epic \u00b7 1 Task \u00b7 1 Job \u00b7 1 Worker",
                                                SystemView.runningNow("de_DE.UTF-8", one)),
                            () -> assertEquals("1 Epic \u00b7 1 Task \u00b7 1 Job \u00b7 1 Worker",
                                                SystemView.runningNow("de_DE.UTF-8", zero)));
    }

    @Test
    @DisplayName("the stop names what runs and Enter takes the drain")
    void stopTakesTheDrain() throws Exception {
        int plans = Generators.integers(2, 9).next();
        int epics = Generators.integers(2, 9).next();
        int tasks = Generators.integers(2, 9).next();
        int build = Generators.integers(2, 9).next();
        int test = Generators.integers(2, 9).next();
        int workers = Generators.integers(2, 9).next();
        serve(200, snapshotJson(plans, epics, tasks, build, test, workers));

        var answer = ask("\n", Act.STOP, null, false);

        // The counts are drawn, so the line is derived from them: the deploy slot of the snapshot
        // is the one slot the signature does not carry.
        var jobs = build + test + DEPLOY_SLOTS;
        var running = plans + " plans \u00b7 " + epics + " epics \u00b7 " + tasks + " tasks \u00b7 "
                            + jobs + " jobs \u00b7 " + workers + " workers";
        var screen = out.toString();
        assertAll(
                            () -> assertEquals(When.WHEN_DONE, answer.when()),
                            () -> assertEquals(0, answer.exitCode()),
                            () -> assertTrue(screen.contains("Stop the runtime?"), screen),
                            () -> assertTrue(screen.contains("Running now: " + running), screen),
                            () -> assertTrue(screen.contains("Every client sees that it stops."), screen),
                            () -> assertTrue(screen.contains("When"), screen),
                            () -> assertTrue(screen.contains("1) Stop when done"), screen),
                            () -> assertTrue(screen.contains("2) Stop now"), screen),
                            () -> assertTrue(screen.contains("k) keep it running"), screen)
        );
    }

    @Test
    @DisplayName("a runtime that accepts and never answers leaves stop with a refusal within the bound")
    void aSilentRuntimeDoesNotHangTheStop() throws Exception {
        var release = new CountDownLatch(1);
        var stream = new AtomicReference<FakeRuntimeServer.EventStream>();
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/system".equals(request.target())) {
                try {
                    // The head is written and the body never starts: the client waits for a chunk.
                    stream.set(response.events(200));
                    release.await(30, TimeUnit.SECONDS);
                } catch (IOException | InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            } else {
                response.send(202, null, "");
            }
        });

        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var answer = new AtomicReference<RuntimeActDialogue.Answer>();
            var thread = new Thread(() -> {
                try {
                    answer.set(RuntimeActDialogue.confirm(context(virtual, ENGLISH), new PrintWriter(out),
                                        new PrintWriter(err), new RuntimeActDialogue.Request(Act.STOP, null, false, null),
                                        SNAPSHOT_BOUND));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }, "stop-on-a-silent-runtime");
            thread.start();
            thread.join(SNAPSHOT_BOUND.plusSeconds(2).toMillis());
            var hung = thread.isAlive();
            release.countDown();
            var open = stream.get();
            if (open != null) {
                try {
                    open.close();
                } catch (IOException _) {
                    // the client already closed its side after the bound expired
                }
            }
            thread.join(5_000L);

            assertAll(
                                () -> assertFalse(hung, "stop hung on a runtime that never answers"),
                                () -> assertEquals(1, answer.get() != null ? answer.get().exitCode() : Integer.MIN_VALUE,
                                                    err.toString()),
                                () -> assertTrue(err.toString().contains("The runtime is not answering"),
                                                    err.toString()),
                                () -> assertFalse(out.toString().contains("The runtime is not answering"),
                                                    out.toString())
            );
        }
    }

    @Test
    @DisplayName("2 answers the choice of when with now")
    void nowIsTheSecondChoice() throws Exception {
        serve(200, snapshotJson(1, 0, 1, 0, 1, 2));

        assertEquals(When.NOW, ask("2\n", Act.STOP, null, false).when());
    }

    @Test
    @DisplayName("k keeps the runtime running and sends nothing")
    void keepLeavesTheDialogue() throws Exception {
        serve(200, snapshotJson(1, 0, 0, 0, 0, 1));

        var answer = ask("k\n", Act.STOP, null, false);

        assertAll(
                            () -> assertNull(answer.when()),
                            () -> assertEquals(0, answer.exitCode())
        );
    }

    @Test
    @DisplayName("q leaves the dialogue with the not-finished exit code")
    void qLeavesTheDialogue() throws Exception {
        serve(200, snapshotJson(1, 0, 0, 0, 0, 1));

        var answer = ask("q\n", Act.STOP, null, false);

        assertAll(
                            () -> assertNull(answer.when()),
                            () -> assertEquals(3, answer.exitCode())
        );
        assertTrue(out.toString().contains("Left the stop. Nothing was sent."), out.toString());
    }

    @Test
    @DisplayName("a restart names itself and its note")
    void restartNamesItsNote() throws Exception {
        serve(200, snapshotJson(1, 1, 1, 1, 0, 3));

        assertEquals(When.WHEN_DONE, ask("1\n", Act.RESTART, null, false).when());

        var screen = out.toString();
        assertAll(
                            () -> assertTrue(screen.contains("Restart the runtime?"), screen),
                            () -> assertTrue(screen.contains("Every client connects again by itself."), screen),
                            () -> assertTrue(screen.contains("1) Restart when done"), screen),
                            () -> assertTrue(screen.contains("2) Restart now"), screen)
        );
    }

    @Test
    @DisplayName("an upgrade names the version it goes to")
    void upgradeNamesTheVersion() throws Exception {
        serve(200, snapshotJson(0, 0, 0, 0, 0, 1));

        assertEquals(When.NOW, ask("2\n", Act.UPGRADE, "1.5.0", false).when());

        assertTrue(out.toString().contains("Upgrade to 1.5.0?"), out.toString());
    }

    @Test
    @DisplayName("an upgrade without a known version asks without one")
    void upgradeWithoutAVersion() throws Exception {
        serve(200, snapshotJsonWithoutNewerVersion());

        ask("k\n", Act.UPGRADE, null, false);

        assertTrue(out.toString().contains("Upgrade the runtime?"), out.toString());
    }

    @Test
    @DisplayName("a runtime with nothing running says so")
    void nothingRuns() throws Exception {
        serve(200, bareSnapshotJson());

        ask("k\n", Act.STOP, null, false);

        assertTrue(out.toString().contains("Nothing runs."), out.toString());
    }

    @Test
    @DisplayName("a snapshot the runtime refuses still asks, and says the counts are unavailable")
    void refusedSnapshotStillAsks() throws Exception {
        serve(500, "");

        assertEquals(When.WHEN_DONE, ask("1\n", Act.RESTART, null, false).when());

        assertTrue(out.toString().contains("Running now: not available"), out.toString());
    }

    @Test
    @DisplayName("a live runtime that refuses its socket is not answering, not absent")
    void refusedSocketOfALiveRuntime() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);

        var answer = ask("", Act.STOP, null, false);

        assertAll(
                            () -> assertNull(answer.when()),
                            () -> assertEquals(1, answer.exitCode()),
                            () -> assertTrue(err.toString().contains("The runtime is not answering."), err.toString()),
                            () -> assertFalse(out.toString().contains("The runtime is not answering."), out.toString())
        );
    }

    @Test
    @DisplayName("a stopped runtime is told there is nothing to stop")
    void stoppedRuntimeSaysSo() throws Exception {
        var answer = ask("", Act.STOP, null, false);

        assertAll(
                            () -> assertNull(answer.when()),
                            () -> assertEquals(0, answer.exitCode()),
                            () -> assertEquals("The runtime is not running; there is nothing to stop.",
                                                out.toString().strip())
        );
    }

    @Test
    @DisplayName("a restart of a stopped runtime still asks and starts it")
    void stoppedRuntimeStillAsksARestart() throws Exception {
        assertEquals(When.WHEN_DONE, ask("1\n", Act.RESTART, null, false).when());
    }

    @Test
    @DisplayName("--no-input without an answer stops at the choice with exit 3")
    void noInputStopsAtTheChoice() throws Exception {
        serve(200, snapshotJson(1, 0, 1, 1, 0, 1));

        var answer = ask("", Act.STOP, null, true);

        assertAll(
                            () -> assertNull(answer.when()),
                            () -> assertEquals(3, answer.exitCode()),
                            () -> assertTrue(out.toString().contains(
                                                                    "runtime stop needs the choice of when; answer it with --when-done or --now."),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("the German dialogue says l instead of k")
    void germanKeepsWithL() throws Exception {
        serve(200, snapshotJson(1, 0, 1, 1, 0, 1));

        Answer answer;
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            virtual.type("l\n");
            answer = RuntimeActDialogue.confirm(context(virtual, GERMAN), new PrintWriter(out), new PrintWriter(err),
                                Act.STOP, null, false, null);
        }

        var screen = out.toString();
        assertAll(
                            () -> assertNull(answer.when()),
                            () -> assertTrue(screen.contains("Runtime stoppen?"), screen),
                            () -> assertTrue(screen.contains("l) lass sie laufen"), screen)
        );
    }

    @Test
    @DisplayName("an unreadable snapshot body still asks, and says the counts are unavailable")
    void unreadableSnapshotStillAsks() throws Exception {
        serve(200, "not json");

        assertEquals(When.WHEN_DONE, ask("1\n", Act.STOP, null, false).when());

        assertTrue(out.toString().contains("Running now: not available"), out.toString());
    }

    @Test
    @DisplayName("an explicit when is taken without asking")
    void explicitWhenSkipsTheDialogue() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var answer = RuntimeActDialogue.confirm(context(virtual, ENGLISH), new PrintWriter(out),
                                new PrintWriter(err), Act.STOP, When.NOW, false, null);
            assertAll(
                                () -> assertEquals(When.NOW, answer.when()),
                                () -> assertEquals(0, answer.exitCode()),
                                () -> assertTrue(out.toString().isEmpty(), out.toString())
            );
        }
    }

    /**
     * @return the snapshot of a runtime that reports no state counts and no test slots
     * @throws IOException when the snapshot cannot be written
     */
    private static String bareSnapshotJson() throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(snapshotJson(0, 0, 0, 0, 0, 0)));
        var work = JsonTree.asObject(map.get("work"));
        work.remove("plans_by_state");
        work.remove("epics_by_state");
        var jobs = JsonTree.asObject(map.get("jobs"));
        jobs.remove("test");
        jobs.remove("deploy");
        return JsonTree.write(map);
    }

    /**
     * @return the snapshot of a machine that knows no newer version
     */
    private static String snapshotJsonWithoutNewerVersion() throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(snapshotJson(0, 0, 0, 0, 0, 1)));
        var runtime = JsonTree.asObject(map.get("runtime"));
        runtime.remove("newer_version");
        return JsonTree.write(map);
    }

    /**
     * @param plans  the running plans
     * @param epics  the running epics
     * @param tasks  the running tasks
     * @param build  the build slots in use
     * @param test   the test slots in use
     * @param workers the workers
     * @return the snapshot the runtime serves, as JSON
     */
    private static String snapshotJson(int plans, int epics, int tasks, int build, int test, int workers) {
        var runtime = new RuntimeMetrics("0.9.1", "2026-10-02 (a1b2c3d)", "today 08:14", 22320L,
                            432013312L, 1073741824L, 713031680L, 4.0, 2469606195L, 40802189312L,
                            "13:40", true, "127.0.0.1:7420", "192.168.1.20:7421", 2, 0, null,
                            "0.10.0", "2026-10-16", "today 08:14",
                            "/api/v1/system/release-notes?version=0.10.0", true, null);
        var work = new WorkMetrics(Map.of("running", plans), Map.of("running", epics), tasks, 0, 1, List.of());
        var roster = new ArrayList<WorkerMetric>();
        for (int index = 0; index < workers; index++) {
            roster.add(new WorkerMetric("implementer", "claude", "sonnet-4.5", WorkerState.BUSY, null));
        }
        var jobs = Map.<String, Object>of(
                            "build", Map.of("slots_used", build, "slots_total", build + 1, "queued", 0),
                            "test", Map.of("slots_used", test, "slots_total", test + 1, "queued", 0),
                            "deploy", Map.of("slots_used", DEPLOY_SLOTS, "slots_total", DEPLOY_SLOTS + 1, "queued", 0),
                            "locks_held", 0);
        var cleanup = new CleanupSummary(0, 0, 0, 0, 0, 0L, 0, "today 03:00", "tomorrow 03:00");
        var snapshot = new SystemSnapshot(runtime, work, roster, jobs, List.of(), cleanup,
                            SystemWindow.HOUR, null);
        return JsonTree.write(snapshot.toMap());
    }
}
