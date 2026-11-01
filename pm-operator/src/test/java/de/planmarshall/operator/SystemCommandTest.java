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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.ModelMetric;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.api.system.WorkerState;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("SystemCommand")
class SystemCommandTest {

    @TempDir
    Path tempDir;

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
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private int execute(String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private SystemSnapshot sampleSnapshot() {
        var runtimeMetrics = new RuntimeMetrics(
                            "0.9.1",
                            "2026-10-02 (a1b2c3d)",
                            "today 08:14",
                            22320L,
                            432013312L,
                            1073741824L,
                            713031680L,
                            4.0,
                            2469606195L,
                            40802189312L,
                            "13:40",
                            true,
                            "127.0.0.1:7420",
                            "192.168.1.20:7421",
                            2,
                            0,
                            null,
                            "0.10.0",
                            "2026-10-16",
                            "today 08:14",
                            "/api/v1/system/release-notes?version=0.10.0",
                            true,
                            null
        );
        var workMetrics = new WorkMetrics(
                            Map.of("running", 3, "waiting_for_you", 1, "held", 1, "paused", 2),
                            Map.of("running", 1, "waiting_for_you", 1),
                            4,
                            6,
                            3,
                            List.of()
        );
        var workers = List.of(
                            new WorkerMetric("planner", "claude", "opus-4.5", WorkerState.BUSY, "fix-relay"),
                            new WorkerMetric("implementer", "claude", "sonnet-4.5", WorkerState.BUSY, "relay-docs"),
                            new WorkerMetric("implementer", "codex", "gpt-5", WorkerState.IDLE, null)
        );
        var jobs = Map.<String, Object>of(
                            "build", Map.of("slots_used", 1, "slots_total", 2, "queued", 1),
                            "test", Map.of("slots_used", 0, "slots_total", 1, "queued", 0),
                            "locks_held", 3
        );
        var models = List.of(
                            new ModelMetric("claude:opus-4.5", "planner", 42, 2020000L, 1900000L, 120000L, 0, 1, false, 0),
                            new ModelMetric("claude:sonnet-4.5", "implementer", 118, 3610000L, 3200000L, 410000L, 0, 0, false, 0),
                            new ModelMetric("codex:gpt-5", "implementer", 12, 0L, 0L, 0L, 1, 0, true, 12)
        );
        var cleanup = new CleanupSummary(
                            12, 2, 40, 5, 1, 1932735283L, 4, "today 03:00", "tomorrow 03:00"
        );
        return new SystemSnapshot(runtimeMetrics, workMetrics, workers, jobs, models, cleanup, SystemWindow.HOUR, null);
    }

    @Test
    @DisplayName("system reports not running when runtime record is absent")
    void systemNotRunning() {
        int code = execute("system");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains(
                                                                    "The runtime is not running. pm-operator runtime start starts it."),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("system prints 6-part system view when running")
    void systemRunning() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var snapshot = sampleSnapshot();
        var jsonText = JsonTree.write(snapshot.toMap());

        fixture.start((request, response) -> {
            if ("/api/v1/system?window=hour".equals(request.target()) || "/api/v1/system".equals(request.target())) {
                response.json(200, jsonText);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("system");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, "code"),
                            () -> assertTrue(output.contains("System"), "System header"),
                            () -> assertTrue(output.contains("Runtime   pm-mcpd 0.9.1 · build 2026-10-02 (a1b2c3d) · running since today 08:14 · 6 h 12 min · 0.10.0 available"), "runtime header"),
                            () -> assertTrue(output.contains("memory 412 MB in use of 1 GB · 680 MB resident · CPU 4 %"), "memory"),
                            () -> assertTrue(output.contains("machine store 2.3 GB · 38 GB free on its disk · measured 13:40"), "machine store"),
                            () -> assertTrue(output.contains("listeners  socket · loopback 127.0.0.1:7420 · network 192.168.1.20:7421 (2 devices)"), "listeners"),
                            () -> assertTrue(output.contains("Work      plans  3 running · 1 waiting for you · 1 held · 2 paused"), "work plans"),
                            () -> assertTrue(output.contains("tasks  4 running · 6 queued"), "work tasks"),
                            () -> assertTrue(output.contains("3 projects with running work"), "work projects"),
                            () -> assertTrue(output.contains("Workers   3 · 2 busy · 1 idle"), "workers header"),
                            () -> assertTrue(output.contains("planner       claude    opus-4.5    busy        fix-relay"), "worker planner"),
                            () -> assertTrue(output.contains("Jobs      locks  3 held"), "jobs locks"),
                            () -> assertTrue(output.contains("build  1 of 2 slots · 1 queued"), "jobs build"),
                            () -> assertTrue(output.contains("test  0 of 1 slot"), "jobs test"),
                            () -> assertTrue(output.contains("Models    Last hour · per model · 172 calls · ≥ 5.63 M tokens (≥ 5.10 M in · ≥ 530 k out)"), "models header"),
                            () -> assertTrue(output.contains("42 calls   2.02 M tokens (1.90 M in · 120 k out)   1 retry"), "model claude"),
                            () -> assertTrue(output.contains("12 calls   tokens not measured: not reported by codex   1 error"), "model codex"),
                            () -> assertTrue(output.contains("Cleanup   due: 12 plans · 2 epics · 40 logs · 5 read reports · 1 unenrolled project · frees 1.8 GB"), "cleanup header"),
                            () -> assertTrue(output.contains("4 kept for good · last sweep today 03:00"), "cleanup kept"),
                            () -> assertTrue(output.contains("plans · cleanup · system --window today · system --by role"), "footer")
        );
    }

    @Test
    @DisplayName("system passes --window and --project query parameters")
    void systemWithWindowAndProject() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var snapshot = sampleSnapshot();
        var jsonText = JsonTree.write(snapshot.toMap());

        fixture.start((request, response) -> {
            if ("/api/v1/system?window=today&project=pm-mcp-clients".equals(request.target())) {
                response.json(200, jsonText);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("system", "--window", "today", "--project", "pm-mcp-clients");
        assertEquals(0, code);
        assertTrue(out.toString().contains("System"));
    }

    @Test
    @DisplayName("system --json outputs raw JSON")
    void systemJson() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var snapshot = sampleSnapshot();
        var jsonText = JsonTree.write(snapshot.toMap());

        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/system")) {
                response.json(200, jsonText);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("system", "--json");
        assertEquals(0, code);
        assertTrue(out.toString().contains("\"version\":\"0.9.1\""));
    }

    @Test
    @DisplayName("formatBytes correctly formats byte sizes")
    void formatBytesTest() {
        assertEquals("500 B", SystemCommand.formatBytes(500L));
        assertEquals("412 MB", SystemCommand.formatBytes(432013312L));
        assertEquals("1 GB", SystemCommand.formatBytes(1073741824L));
        assertEquals("680 MB", SystemCommand.formatBytes(713031680L));
        assertEquals("2.3 GB", SystemCommand.formatBytes(2469606195L));
        assertEquals("38 GB", SystemCommand.formatBytes(40802189312L));
        assertEquals("1.8 GB", SystemCommand.formatBytes(1932735283L));
    }

    @Test
    @DisplayName("system --by role groups models by role and prints 7-day window")
    void systemByRoleAndVariations() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var runtimeMetrics = new RuntimeMetrics(
                            "0.9.1",
                            "commit-123",
                            "today 08:00",
                            120L,
                            1000L,
                            2000L,
                            3000L,
                            -1.0,
                            4000L,
                            5000L,
                            "12:00",
                            true,
                            "off",
                            "off",
                            0,
                            3,
                            "0.8.0",
                            null,
                            null,
                            null,
                            null,
                            false,
                            "managed installation"
        );
        var workMetrics = new WorkMetrics(
                            Map.of(),
                            Map.of(),
                            0,
                            0,
                            1,
                            List.of("recovering-repo")
        );
        var workers = List.of(
                            new WorkerMetric("planner", "claude", "opus-4.5", WorkerState.IDLE, null)
        );
        var models = List.of(
                            new ModelMetric("claude:opus-4.5", "planner", 10, 100000L, 80000L, 20000L, 0, 0, false, 0)
        );
        var cleanup = new CleanupSummary(0, 0, 0, 0, 0, 0L, 0, null, null);
        var snapshot = new SystemSnapshot(runtimeMetrics, workMetrics, workers, Map.of(), models, cleanup, SystemWindow.SEVEN_DAYS, null);

        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/system")) {
                response.json(200, JsonTree.write(snapshot.toMap()));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("system", "--by", "role", "--window", "7d");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("network off"), output),
                            () -> assertTrue(output.contains("3 host sessions run pm-mcp 0.8.0"), output),
                            () -> assertTrue(output.contains("1 project with running work"), output),
                            () -> assertTrue(output.contains("recovering-repo recovering"), output),
                            () -> assertTrue(output.contains("Models    7 days · per role · 10 calls"), output),
                            () -> assertTrue(output.contains("CPU not available (not measured on this platform)"), output)
        );
    }
}
