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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
class RuntimeActDialogueTest {

    private static final String ENGLISH = "en_US.UTF-8";
    private static final String GERMAN = "de_DE.UTF-8";

    private RuntimeFixture fixture;
    private StringWriter out;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
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
            return RuntimeActDialogue.confirm(context(virtual, ENGLISH), new PrintWriter(out), act, null,
                                noInput, version);
        }
    }

    @Test
    @DisplayName("the stop names what runs and Enter takes the drain")
    void stopTakesTheDrain() throws Exception {
        serve(200, snapshotJson(3, 1, 4, 1, 0, 5));

        var answer = ask("\n", Act.STOP, null, false);

        var screen = out.toString();
        assertAll(
                            () -> assertEquals(When.WHEN_DONE, answer.when()),
                            () -> assertEquals(0, answer.exitCode()),
                            () -> assertTrue(screen.contains("Stop the runtime?"), screen),
                            () -> assertTrue(screen.contains(
                                                "Running now: 3 plans \u00b7 1 epic \u00b7 4 tasks \u00b7 1 job \u00b7 5 workers"),
                                                screen),
                            () -> assertTrue(screen.contains("Every client sees that it stops."), screen),
                            () -> assertTrue(screen.contains("When"), screen),
                            () -> assertTrue(screen.contains("1) Stop when done"), screen),
                            () -> assertTrue(screen.contains("2) Stop now"), screen),
                            () -> assertTrue(screen.contains("k) keep it running"), screen)
        );
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
    @DisplayName("q leaves the dialogue and sends nothing")
    void qLeavesTheDialogue() throws Exception {
        serve(200, snapshotJson(1, 0, 0, 0, 0, 1));

        var answer = ask("q\n", Act.STOP, null, false);

        assertNull(answer.when());
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
    @DisplayName("a snapshot the runtime refuses still asks, without counts")
    void refusedSnapshotStillAsks() throws Exception {
        serve(500, "");

        assertEquals(When.WHEN_DONE, ask("1\n", Act.RESTART, null, false).when());

        assertTrue(out.toString().contains("Nothing runs."), out.toString());
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
            answer = RuntimeActDialogue.confirm(context(virtual, GERMAN), new PrintWriter(out), Act.STOP, null,
                                false, null);
        }

        var screen = out.toString();
        assertAll(
                            () -> assertNull(answer.when()),
                            () -> assertTrue(screen.contains("Runtime stoppen?"), screen),
                            () -> assertTrue(screen.contains("l) lass sie laufen"), screen)
        );
    }

    @Test
    @DisplayName("an unreadable snapshot body still asks")
    void unreadableSnapshotStillAsks() throws Exception {
        serve(200, "not json");

        assertEquals(When.WHEN_DONE, ask("1\n", Act.STOP, null, false).when());

        assertTrue(out.toString().contains("Nothing runs."), out.toString());
    }

    @Test
    @DisplayName("an explicit when is taken without asking")
    void explicitWhenSkipsTheDialogue() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(120, 30)) {
            var answer = RuntimeActDialogue.confirm(context(virtual, ENGLISH), new PrintWriter(out), Act.STOP,
                                When.NOW, false, null);
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
        JsonTree.asObject(map.get("jobs")).remove("test");
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
        var roster = new java.util.ArrayList<WorkerMetric>();
        for (int index = 0; index < workers; index++) {
            roster.add(new WorkerMetric("implementer", "claude", "sonnet-4.5", WorkerState.BUSY, null));
        }
        var jobs = Map.<String, Object>of(
                            "build", Map.of("slots_used", build, "slots_total", 2, "queued", 0),
                            "test", Map.of("slots_used", test, "slots_total", 1, "queued", 0),
                            "locks_held", 0);
        var cleanup = new CleanupSummary(0, 0, 0, 0, 0, 0L, 0, "today 03:00", "tomorrow 03:00");
        var snapshot = new SystemSnapshot(runtime, work, roster, jobs, List.of(), cleanup,
                            SystemWindow.HOUR, null);
        return JsonTree.write(snapshot.toMap());
    }
}
