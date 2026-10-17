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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nayuki.qrcodegen.QrCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupItemEntry;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupJobState;
import de.planmarshall.api.system.CleanupProjectItem;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.ModelMetric;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.api.system.WorkerState;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.api.testsupport.CliProcess;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.api.testsupport.StagedDaemon;
import de.planmarshall.api.testsupport.VerificationResults;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * {@code pm-operator} as a real process (the native image when built, else the packaged JAR) against
 * the fake runtime: the start-up time to the first request (PM-TECH-3), the detached on-demand start
 * of {@code runtime start} (gate 8), and a status without a runtime that writes nothing.
 */
@DisplayName("pm-operator as a process")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PmOperatorProcessIT {

    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final int STARTUP_RUNS = 15;
    private static final long BUDGET_MS = 50;

    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class,
                        QrCode.class);
    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private Map<String, String> environment() {
        var environment = new HashMap<>(fixture.environment());
        environment.put("PATH", "/usr/bin:/bin");
        return environment;
    }

    private CliProcess operator(Map<String, String> environment, String... args) throws IOException {
        return CliProcess.start(binary.with(args), environment, fixture.base().getParent());
    }

    @Test
    @DisplayName("sends the first request of status within the start budget (PM-TECH-3)")
    void startupToFirstRequest() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var server = fixture.start((request, response) -> response.json(200, PmOperatorTest.STATUS));
        var samples = new ArrayList<Long>();
        for (var run = 0; run < STARTUP_RUNS; run++) {
            try (var process = operator(environment(), "status")) {
                var request = server.awaitRequest(WAIT);
                assertNotNull(request, "no request; stderr: " + process.stderr());
                samples.add((request.receivedNanos() - process.startNanos()) / 1000);
                assertEquals(0, process.awaitExit(WAIT));
            }
        }
        Collections.sort(samples);
        var median = samples.get(samples.size() / 2);
        var pass = median <= BUDGET_MS * 1000;
        var values = new LinkedHashMap<String, Object>();
        values.put("mode", binary.mode());
        values.put("runs", (long) samples.size());
        values.put("min_ms", samples.getFirst() / 1000.0);
        values.put("median_ms", median / 1000.0);
        values.put("max_ms", samples.getLast() / 1000.0);
        values.put("budget_ms", BUDGET_MS);
        VerificationResults.write("pm-tech-3-operator-first-request-" + binary.mode(), values, pass);
        if (binary.nativeImage()) {
            assertTrue(pass, "median start-up to first request " + median / 1000.0 + " ms");
        }
    }

    @Test
    @DisplayName("status without a runtime starts nothing and writes nothing")
    void statusWithoutRuntime() throws Exception {
        try (var process = operator(environment(), "status")) {
            assertEquals(0, process.awaitExit(WAIT));
            assertEquals("runtime   not running\n", new String(process.stdout(), StandardCharsets.UTF_8));
            assertTrue(Files.notExists(fixture.base()), "status created nothing below PM_MCP_BASE");
        }
    }

    @Test
    @DisplayName("runtime start starts pm-mcpd detached and reports it only after the round trip (gate 8)")
    void detachedStart() throws Exception {
        var staged = StagedDaemon.stage(fixture.base().getParent());
        var environment = environment();
        environment.putAll(staged.environment());
        try (var process = operator(environment, "runtime", "start")) {
            var started = staged.awaitStart(WAIT);
            assertNotNull(started, "pm-mcpd was not started; stderr: " + process.stderr());
            Thread.sleep(100);
            assertEquals(0, process.stdout().length, "running is reported only after the round trip");
            fixture.start((request, response) -> response.json(200, PmOperatorTest.STATUS));

            assertEquals(0, process.awaitExit(WAIT), process.stderr());
            try {
                assertEquals("runtime   running (pid 42)\n", new String(process.stdout(), StandardCharsets.UTF_8));
                assertEquals(started[0], started[1], "pm-mcpd leads its own process group (setsid)");
                assertNotEquals(ProcessHandle.current().pid(), started[1]);
                assertTrue(staged.alive(), "pm-mcpd outlives pm-operator");
                assertEquals("rwx------", PosixFilePermissions
                                    .toString(Files.getPosixFilePermissions(fixture.paths().locksDir())));
                var values = new LinkedHashMap<String, Object>();
                values.put("mode", binary.mode());
                values.put("daemon_pid", started[0]);
                values.put("daemon_pgid", started[1]);
                values.put("survived_client_exit", staged.alive());
                VerificationResults.write("gate8-detached-start-pm-operator-" + binary.mode(), values, true);
            } finally {
                staged.kill();
            }
        }
    }

    @Test
    @DisplayName("status against runtime reports hierarchical project overview (UC-PLN-4)")
    void statusWithProjectsOverview() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            var target = request.target();
            if ("/api/v1/status".equals(target)) {
                response.json(200, PmOperatorTest.STATUS);
            } else if ("/api/v1/projects".equals(target)) {
                response.json(200, "{\"projects\":[{\"project_id\":\"pm-mcp-clients\",\"display_name\":\"pm-mcp-clients\",\"plans_count\":1,\"epics_count\":0,\"needs_operator_count\":1}]}");
            } else if ("/api/v1/projects/pm-mcp-clients".equals(target)) {
                response.json(200, "{\"project_id\":\"pm-mcp-clients\",\"display_name\":\"pm-mcp-clients\",\"plans\":[{\"plan_id\":\"pln-console-history\",\"title\":\"Console history of answers\",\"current_phase\":\"refine\",\"phase_state\":\"waiting_for_you\",\"needs_operator_count\":1,\"needs_operator_kind\":\"decision\"}],\"epics\":[],\"alongside_landings\":[]}");
            } else {
                response.json(404, "{\"error\":\"not_found\"}");
            }
        });

        try (var process = operator(environment(), "status")) {
            assertEquals(0, process.awaitExit(WAIT), process.stderr());
            var stdout = new String(process.stdout(), StandardCharsets.UTF_8);
            assertTrue(stdout.contains("Runtime pm-mcpd"), "expected runtime status header; got: " + stdout);
            assertTrue(stdout.contains("pm-mcp-clients"), "expected project in overview; got: " + stdout);
            assertTrue(stdout.contains("Console history of answers"), "expected plan in overview; got: " + stdout);
            assertTrue(stdout.contains("! decision"), "expected attention reason in overview; got: " + stdout);
            assertTrue(stdout.contains("waiting for you"), "expected localized state in overview; got: " + stdout);
        }
    }

    @Test
    @DisplayName("reports refuses execution without an interactive terminal (PM-SEC-3)")
    void reportsRefusesWithoutTerminal() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        try (var process = operator(environment(), "reports")) {
            assertEquals(4, process.awaitExit(WAIT));
            var stderr = process.stderr();
            assertTrue(stderr.contains("operator refused: interactive terminal required"), "stderr: " + stderr);
        }

        try (var process = operator(environment(), "reports", "read", "--all")) {
            assertEquals(4, process.awaitExit(WAIT));
            var stderr = process.stderr();
            assertTrue(stderr.contains("operator refused: interactive terminal required"), "stderr: " + stderr);
        }
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
                            Map.of("running", 3, "waiting_for_you", 1),
                            Map.of("running", 1),
                            4,
                            6,
                            3,
                            List.of()
        );
        var workers = List.of(
                            new WorkerMetric("planner", "claude", "opus-4.5", WorkerState.BUSY, "fix-relay"),
                            new WorkerMetric("implementer", "codex", "gpt-5", WorkerState.IDLE, null)
        );
        var jobs = Map.<String, Object>of(
                            "build", Map.of("slots_used", 1, "slots_total", 2, "queued", 1),
                            "locks_held", 3
        );
        var models = List.of(
                            new ModelMetric("claude:opus-4.5", "planner", 42, 2020000L, 1900000L, 120000L, 0, 1, false, 0)
        );
        var cleanup = new CleanupSummary(
                            12, 2, 40, 5, 1, 1932735283L, 4, "today 03:00", "tomorrow 03:00"
        );
        return new SystemSnapshot(runtimeMetrics, workMetrics, workers, jobs, models, cleanup, SystemWindow.HOUR, null);
    }

    private CleanupJob sampleCleanupJob() {
        var items1 = List.of(
                            new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged (#214)", "12 Sep", 88080384L, 6, 0, null)
        );
        var prj1 = new CleanupProjectItem("pm-mcp-clients", 1, 0, 6, 0, 88080384L, items1);
        var summary = new CleanupSummary(1, 0, 6, 0, 1, 88080384L, 1, "today 03:00", "tomorrow 03:00");
        return new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary, List.of(prj1), Map.of());
    }

    @Test
    @DisplayName("system against runtime reports 6-part dashboard (UC-SYS-1)")
    void systemAgainstRuntime() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var snapshotJson = sampleSnapshot().toJson();
        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/system")) {
                response.json(200, snapshotJson);
            } else {
                response.json(404, "{\"error\":\"not_found\"}");
            }
        });

        try (var process = operator(environment(), "system")) {
            assertEquals(0, process.awaitExit(WAIT), process.stderr());
            var stdout = new String(process.stdout(), StandardCharsets.UTF_8);
            assertTrue(stdout.contains("Runtime   pm-mcpd 0.9.1"), "expected runtime; got: " + stdout);
            assertTrue(stdout.contains("Work      plans"), "expected work; got: " + stdout);
            assertTrue(stdout.contains("Workers   2 · 1 busy · 1 idle"), "expected workers; got: " + stdout);
            assertTrue(stdout.contains("Jobs      build"), "expected jobs; got: " + stdout);
            assertTrue(stdout.contains("Models"), "expected models; got: " + stdout);
            assertTrue(stdout.contains("Cleanup   due:"), "expected cleanup; got: " + stdout);
        }
    }

    @Test
    @DisplayName("system with --json outputs valid snapshot json")
    void systemWithJson() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var snapshotJson = sampleSnapshot().toJson();
        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/system")) {
                response.json(200, snapshotJson);
            } else {
                response.json(404, "{\"error\":\"not_found\"}");
            }
        });

        try (var process = operator(environment(), "system", "--json")) {
            assertEquals(0, process.awaitExit(WAIT), process.stderr());
            var stdout = new String(process.stdout(), StandardCharsets.UTF_8).trim();
            assertTrue(stdout.startsWith("{"), "expected json object; got: " + stdout);
            assertTrue(stdout.contains("\"runtime\""), "expected runtime key in json; got: " + stdout);
            assertTrue(stdout.contains("0.9.1"), "expected version in json; got: " + stdout);
        }
    }

    @Test
    @DisplayName("cleanup against runtime reports job summary and items (UC-SYS-3)")
    void cleanupAgainstRuntime() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var jobJson = JsonTree.write(sampleCleanupJob().toMap());
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target())) {
                response.json(200, jobJson);
            } else if ("/api/v1/system/cleanup/residue".equals(request.target())) {
                response.json(200, "{\"step\":\"workers\",\"last_sweep\":\"14:00\"}");
            } else {
                response.json(404, "{\"error\":\"not_found\"}");
            }
        });

        try (var process = operator(environment(), "cleanup")) {
            assertEquals(0, process.awaitExit(WAIT), process.stderr());
            var stdout = new String(process.stdout(), StandardCharsets.UTF_8);
            assertTrue(stdout.contains("Cleanup cln-0007 · due since today 03:00"), "expected cleanup header; got: " + stdout);
            assertTrue(stdout.contains("Summary   1 plans"), "expected summary; got: " + stdout);
            assertTrue(stdout.contains("fix-relay"), "expected item; got: " + stdout);
            assertTrue(stdout.contains("Kept for good"), "expected kept footer; got: " + stdout);
        }
    }

    @Test
    @DisplayName("projects refuses execution without an interactive terminal (PM-SEC-3)")
    void projectsRefusesWithoutTerminal() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        try (var process = operator(environment(), "projects")) {
            assertEquals(4, process.awaitExit(WAIT));
            var stderr = process.stderr();
            assertTrue(stderr.contains("operator refused: interactive terminal required"), "stderr: " + stderr);
        }
    }

    @Test
    @DisplayName("project release refuses execution without an interactive terminal (PM-SEC-3)")
    void projectReleaseRefusesWithoutTerminal() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        try (var process = operator(environment(), "project", "release", "my-service", "--reason", "archived", "--yes")) {
            assertEquals(4, process.awaitExit(WAIT));
            var stderr = process.stderr();
            assertTrue(stderr.contains("operator refused: interactive terminal required"), "stderr: " + stderr);
        }
    }

    @Test
    @DisplayName("plan refuses execution without an interactive terminal (PM-SEC-3)")
    void planRefusesWithoutTerminal() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        try (var process = operator(environment(), "plan", "fix-relay")) {
            assertEquals(4, process.awaitExit(WAIT));
            var stderr = process.stderr();
            assertTrue(stderr.contains("operator refused: interactive terminal required"), "stderr: " + stderr);
        }

        try (var process = operator(environment(), "plan", "fix-relay", "--task", "TASK-003")) {
            assertEquals(4, process.awaitExit(WAIT));
            var stderr = process.stderr();
            assertTrue(stderr.contains("operator refused: interactive terminal required"), "stderr: " + stderr);
        }
    }

    @Test
    @DisplayName("epic refuses execution without an interactive terminal (PM-SEC-3)")
    void epicRefusesWithoutTerminal() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        try (var process = operator(environment(), "epic", "relay-hardening")) {
            assertEquals(4, process.awaitExit(WAIT));
            var stderr = process.stderr();
            assertTrue(stderr.contains("operator refused: interactive terminal required"), "stderr: " + stderr);
        }
    }

    @Test
    @DisplayName("start refuses execution without an interactive terminal (PM-SEC-3)")
    void startRefusesWithoutTerminal() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        try (var process = operator(environment(), "start", "plan", "pm-mcp-clients", "--prompt", "x")) {
            assertEquals(4, process.awaitExit(WAIT));
            var stderr = process.stderr();
            assertTrue(stderr.contains("operator refused: interactive terminal required"), "stderr: " + stderr);
        }

        try (var process = operator(environment(), "start", "epic", "pm-mcp-clients", "--prompt", "x")) {
            assertEquals(4, process.awaitExit(WAIT));
            var stderr = process.stderr();
            assertTrue(stderr.contains("operator refused: interactive terminal required"), "stderr: " + stderr);
        }
    }
}
