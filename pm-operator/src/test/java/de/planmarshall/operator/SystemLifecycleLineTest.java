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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
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

@EnableGeneratorController
@DisplayName("The system view states the lifecycle on its first line")
class SystemLifecycleLineTest {

    private final String version = Generators.letterStrings(3, 6).next();
    private final String build = Generators.letterStrings(6, 12).next();
    private final String startedAt = Generators.letterStrings(4, 8).next();
    private final String measuredAt = Generators.letterStrings(3, 5).next();
    private final String plannerRole = Generators.letterStrings(4, 8).next();
    private final String harness = Generators.letterStrings(3, 6).next();
    private final String model = Generators.letterStrings(3, 6).next();
    private final String planTitle = Generators.letterStrings(4, 10).next();
    private final int remaining = Generators.integers(1, 20).next();

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private String statusJson;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
        statusJson = "{\"status\":\"ready\",\"version\":\"" + version
                            + "\",\"lifecycle_state\":\"running\"}";
    }

    @AfterEach
    void tearDown() throws IOException {
        if (fixture != null) {
            fixture.close();
        }
    }

    private String drainingStatus() {
        return "{\"status\":\"ready\",\"version\":\"" + version + "\",\"lifecycle_state\":\"draining\","
                            + "\"draining_by\":\"the console\",\"can_withdraw\":true,"
                            + "\"drain_remaining_count\":" + remaining + "}";
    }

    private String stoppedStatus() {
        return "{\"status\":\"ready\",\"version\":\"" + version + "\",\"lifecycle_state\":\"stopped\"}";
    }

    private SystemSnapshot sampleSnapshot() {
        var uptimeSeconds = Generators.integers(60, 100_000).next().longValue();
        var heapUsed = Generators.integers(1_000_000, 1_000_000_000).next().longValue();
        var heapMax = heapUsed * 2;
        var resident = Generators.integers(1_000_000, 1_000_000_000).next().longValue();
        var cpuPercent = Generators.integers(1, 100).next().doubleValue();
        var storeBytes = Generators.integers(1_000_000, 1_000_000_000).next().longValue();
        var diskFree = Generators.integers(1_000_000, 1_000_000_000).next().longValue();
        var sweepAt = Generators.letterStrings(4, 8).next();
        var runtime = new RuntimeMetrics(version, build, startedAt, uptimeSeconds, heapUsed,
                            heapMax, resident, cpuPercent, storeBytes, diskFree, measuredAt, true,
                            "127.0.0.1:7420", "192.168.1.20:7421", 2, 0, null, null, null, null, null, false, null);
        var work = new WorkMetrics(Map.of("running", 3), Map.of(), 2, 0, 1, List.of());
        var workers = List.of(new WorkerMetric(plannerRole, harness, model, WorkerState.BUSY, planTitle));
        var jobs = Map.<String, Object>of("build", Map.of("slots_used", 1, "slots_total", 2, "queued", 0),
                            "locks_held", 1);
        var models = List.of(new ModelMetric(harness + ":" + model, plannerRole, 42, 2020000L, 1900000L, 120000L,
                            0, 0, false, 0));
        var cleanup = new CleanupSummary(0, 0, 0, 0, 0, 0L, 0, sweepAt, sweepAt);
        return new SystemSnapshot(runtime, work, workers, jobs, models, cleanup, SystemWindow.HOUR, null);
    }

    private void startRuntime() throws IOException {
        PmOperatorTest.writeLiveRecord(fixture);
        var jsonText = JsonTree.write(sampleSnapshot().toMap());
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, statusJson);
            } else if (request.target().startsWith("/api/v1/system")) {
                response.json(200, jsonText);
            } else {
                response.send(404, null, "");
            }
        });
    }

    private OperatorContext context(String lang) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", lang);
        environment.remove("LC_ALL");
        environment.remove("LC_MESSAGES");
        return new OperatorContext(environment, tempDir, true, ClientEnvironment.capture(environment));
    }

    private int execute(String lang, String... args) {
        return PmOperator.execute(context(lang), new PrintWriter(out), new PrintWriter(err), args);
    }

    @Test
    @DisplayName("a draining runtime says so on the first line, with what is left")
    void saysDrainingFirst() throws Exception {
        statusJson = drainingStatus();
        startRuntime();

        int code = execute("en_US.UTF-8", "system");

        assertEquals(0, code, err.toString());
        assertEquals("draining \u00b7 " + remaining + " left", out.toString().lines().findFirst().orElse(""),
                            out.toString());
    }

    @Test
    @DisplayName("a stopped runtime is the first line, as the design words it")
    void saysStoppedFirst() throws Exception {
        statusJson = stoppedStatus();
        startRuntime();

        int code = execute("en_US.UTF-8", "system");

        assertEquals(0, code, err.toString());
        assertEquals("stopped", out.toString().lines().findFirst().orElse(""), out.toString());
    }

    @Test
    @DisplayName("while the runtime runs, the view starts with its title")
    void saysNothingExtraWhileRunning() throws Exception {
        startRuntime();

        int code = execute("en_US.UTF-8", "system");

        assertEquals(0, code, err.toString());
        assertEquals("System", out.toString().lines().findFirst().orElse(""), out.toString());
    }

    @Test
    @DisplayName("the line and the state words are the catalogue's, in both languages")
    void statesTheLineInBothLanguages() throws Exception {
        statusJson = drainingStatus();
        startRuntime();

        int code = execute("de_DE.UTF-8", "system");

        assertEquals(0, code, err.toString());
        assertTrue(out.toString().lines().findFirst().orElse("")
                                                .startsWith("leert sich \u00b7 " + remaining + " offen"),
                            out.toString());
    }
}
