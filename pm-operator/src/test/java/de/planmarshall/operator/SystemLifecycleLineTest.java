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

@DisplayName("The system view states the lifecycle on its first line")
class SystemLifecycleLineTest {

    private static final String DRAINING_STATUS = """
                        {"status":"ready","version":"0.9.1","lifecycle_state":"draining",
                         "draining_by":"the console","can_withdraw":true,"drain_remaining_count":2}
                        """;

    private static final String STOPPED_STATUS = """
                        {"status":"ready","version":"0.9.1","lifecycle_state":"stopped"}
                        """;

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
        statusJson = "{\"status\":\"ready\",\"version\":\"0.9.1\",\"lifecycle_state\":\"running\"}";
    }

    @AfterEach
    void tearDown() throws IOException {
        if (fixture != null) {
            fixture.close();
        }
    }

    private static SystemSnapshot sampleSnapshot() {
        var runtime = new RuntimeMetrics("0.9.1", "2026-10-02 (a1b2c3d)", "today 08:14", 22320L, 432013312L,
                            1073741824L, 713031680L, 4.0, 2469606195L, 40802189312L, "13:40", true,
                            "127.0.0.1:7420", "192.168.1.20:7421", 2, 0, null, null, null, null, null, false, null);
        var work = new WorkMetrics(Map.of("running", 3), Map.of(), 2, 0, 1, List.of());
        var workers = List.of(new WorkerMetric("planner", "claude", "opus-4.5", WorkerState.BUSY, "fix-relay"));
        var jobs = Map.<String, Object>of("build", Map.of("slots_used", 1, "slots_total", 2, "queued", 0),
                            "locks_held", 1);
        var models = List.of(new ModelMetric("claude:opus-4.5", "planner", 42, 2020000L, 1900000L, 120000L,
                            0, 0, false, 0));
        var cleanup = new CleanupSummary(0, 0, 0, 0, 0, 0L, 0, "today 03:00", "tomorrow 03:00");
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
    void saysDrainingFirst() throws IOException {
        statusJson = DRAINING_STATUS;
        startRuntime();

        int code = execute("en_US.UTF-8", "system");

        assertEquals(0, code, err.toString());
        assertEquals("draining \u00b7 2 plans finish", out.toString().lines().findFirst().orElse(""),
                            out.toString());
    }

    @Test
    @DisplayName("a stopped runtime is the first line, as the design words it")
    void saysStoppedFirst() throws IOException {
        statusJson = STOPPED_STATUS;
        startRuntime();

        int code = execute("en_US.UTF-8", "system");

        assertEquals(0, code, err.toString());
        assertEquals("stopped", out.toString().lines().findFirst().orElse(""), out.toString());
    }

    @Test
    @DisplayName("while the runtime runs, the view starts with its title")
    void saysNothingExtraWhileRunning() throws IOException {
        startRuntime();

        int code = execute("en_US.UTF-8", "system");

        assertEquals(0, code, err.toString());
        assertEquals("System", out.toString().lines().findFirst().orElse(""), out.toString());
    }

    @Test
    @DisplayName("the line and the state words are the catalogue's, in both languages")
    void statesTheLineInBothLanguages() throws IOException {
        statusJson = DRAINING_STATUS;
        startRuntime();

        int code = execute("de_DE.UTF-8", "system");

        assertEquals(0, code, err.toString());
        assertTrue(out.toString().lines().findFirst().orElse("").startsWith("leert sich \u00b7 2 Pl"),
                            out.toString());
    }
}
