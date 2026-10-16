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

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ProjectCommand and ProjectsCommand")
class ProjectCommandTest {

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

    @Test
    @DisplayName("projects lists enrolled projects with favourite indicators")
    void projectsList() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target())) {
                response.json(200, """
                                    [
                                      {"id":"pm-mcp-clients","state":"ready","favourite":true,"plans_count":3},
                                      {"id":"cui-http","state":"ready","favourite":false,"plans_count":0}
                                    ]
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("projects");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Projects")),
                            () -> assertTrue(output.contains("★ pm-mcp-clients")),
                            () -> assertTrue(output.contains("3 plans")),
                            () -> assertTrue(output.contains("cui-http"))
        );
    }

    @Test
    @DisplayName("project add starts onboarding for a path")
    void projectAdd() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "add", "/git/new-app");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Project onboarding started for /git/new-app"))
        );
    }

    @Test
    @DisplayName("project favourite toggles favourite status")
    void projectFavourite() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/my-app/favourite".equals(request.target())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "favourite", "my-app");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Project my-app favourite: on (★)"))
        );

        out.getBuffer().setLength(0);
        int code2 = execute("project", "favourite", "my-app", "--off");
        assertAll(
                            () -> assertEquals(0, code2),
                            () -> assertTrue(out.toString().contains("✓ Project my-app favourite: off"))
        );
    }

    @Test
    @DisplayName("project release marks project released")
    void projectRelease() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/old-app/release".equals(request.target())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "release", "old-app", "--reason", "archived");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Project old-app released"))
        );
    }

    @Test
    @DisplayName("project unenrol un-enrols project")
    void projectUnenrol() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects/old-app/unenrol".equals(request.target())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("project", "unenrol", "old-app");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Project old-app unenrolled"))
        );
    }
}
