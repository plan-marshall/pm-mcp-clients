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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.overview.ActivePlanSummary;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.api.testsupport.RuntimeFixture;

/**
 * {@code pm-operator status}: the requests it sends for a project the operator names.
 */
@DisplayName("StatusCommand")
@EnableGeneratorController
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

    private int execute(String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
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

    @Test
    @DisplayName("a project id is one encoded query value in the preliminary status read too")
    void projectIdIsOneEncodedQueryValue() throws Exception {
        var project = Generators.letterStrings(3, 8).next() + "&" + Generators.letterStrings(3, 8).next() + "+"
                            + Generators.letterStrings(3, 8).next();
        var targets = new CopyOnWriteArrayList<String>();
        fixture.start((request, response) -> {
            targets.add(request.target());
            response.json(200, "{\"project_id\":\"p\"}");
        });

        int code = execute("status", "--json", "--project", project);

        var status = targets.stream().filter(target -> target.startsWith("/api/v1/status")).toList();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertEquals(List.of("/api/v1/status?project=" + OperatorUrlUtil.encodeQueryValue(project)),
                                                status, targets.toString()));
    }

    @ParameterizedTest(name = "status {0} names the project as one path segment")
    @ValueSource(strings = {"--ended --json", "--ended", "--json"})
    @DisplayName("a project id is one encoded path segment, never read as path or query syntax")
    void projectIdIsOneEncodedSegment(String options) throws Exception {
        var project = Generators.letterStrings(3, 8).next() + "/" + Generators.letterStrings(3, 8).next();
        var targets = new CopyOnWriteArrayList<String>();
        fixture.start((request, response) -> {
            targets.add(request.target());
            if (request.target().contains("/ended")) {
                response.json(200, "{\"items\":[],\"has_older\":false}");
            } else {
                response.json(200, "{\"project_id\":\"p\"}");
            }
        });
        var args = new ArrayList<String>();
        args.add("status");
        args.addAll(List.of(options.split(" ")));
        args.add("--project");
        args.add(project);

        int code = execute(args.toArray(String[]::new));

        var encoded = "/api/v1/projects/" + OperatorUrlUtil.encodePathSegment(project);
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(targets.stream().anyMatch(target -> target.startsWith(encoded)), targets.toString()),
                            () -> assertTrue(targets.stream().noneMatch(target -> target.startsWith("/api/v1/projects/" + project)),
                                                targets.toString())
        );
    }
}
