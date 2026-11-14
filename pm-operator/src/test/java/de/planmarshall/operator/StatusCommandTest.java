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

import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.overview.ActivePlanSummary;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnableGeneratorController
@DisplayName("StatusCommand")
@EnabledOnOs({OS.MAC, OS.LINUX})
class StatusCommandTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private int executeWithEnv(Map<String, String> extraEnv, String... args) {
        var env = new HashMap<>(fixture.environment());
        env.putAll(extraEnv);
        var context = new OperatorContext(
                            env,
                            tempDir,
                            true,
                            ClientEnvironment.capture(env),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                            _ -> {
                            }
        );
        return PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), args);
    }

    @Test
    @DisplayName("in German, status output contains no English attention literal when needs kind is null")
    void noEnglishLiteralInGerman() throws Exception {
        var project = new ProjectSummary("prj-1", "Project One", 1, 0, 1, null, false, Instant.now());
        var overview = new ProjectOverviewResponse(List.of(project));
        var plan = new ActivePlanSummary(
                            "plan-1",
                            "Plan One",
                            "execute",
                            "running",
                            null,
                            Instant.now(),
                            Instant.now(),
                            1,
                            2,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            1,
                            null,
                            null
        );
        var detail = new ProjectDetailResponse("prj-1", "Project One", List.of(plan), List.of(), List.of());

        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target())) {
                response.json(200, overview.toJson());
            } else if ("/api/v1/projects/prj-1".equals(request.target())) {
                response.json(200, detail.toJson());
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"version\":\"0.1.0\",\"running_since\":\"2026-11-14T10:00:00Z\",\"workers_count\":1,\"build_slots_used\":0,\"build_slots_total\":4}");
            } else {
                response.send(404, null, "");
            }
        });

        int exitCode = executeWithEnv(Map.of("LANG", "de_DE.UTF-8"), "status");
        assertEquals(0, exitCode, err.toString());
        var output = out.toString();
        assertFalse(output.contains("attention"), output);
        assertTrue(output.contains("unbekannt"), output);
    }
}
