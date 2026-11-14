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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        fixture.close();
    }

    private int execute(String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
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
