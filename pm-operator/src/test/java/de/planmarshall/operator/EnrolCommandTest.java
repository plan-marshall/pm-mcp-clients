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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The acts of the folded verbs {@code enrol} and {@code unenrol} through their design verbs: a
 * project is enrolled by {@code project add [<path>] [--pilot]} and its enrolment removed by
 * {@code project unenrol <project_id>} (17-projects.adoc#commands).
 */
@DisplayName("Enrolling through project add and unenrolling through project unenrol")
class EnrolCommandTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws Exception {
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

    private FakeRuntimeServer runtime(FakeRuntimeServer.Handler handler) throws IOException {
        return fixture.start(handler);
    }

    @Test
    @DisplayName("project add posts the checkout path and reports the enrolled project")
    void enrolProject() throws Exception {
        var server = runtime((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, """
                                    {"status":"started","onboarding_id":"onb-new-service","checkout_path":"/home/oliver/git/new-service",
                                     "project_id":"new-service","current_step":"Identity","step_number":2,"total_steps":10}
                                    """);
            } else if ("/api/v1/projects/new-service/setup".equals(request.target())) {
                response.json(200, """
                                    {"project_id":"new-service","state":"project.completed","step":10,
                                     "total_steps":10,"summary":"new-service is ready","links":{}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, execute("project", "add", "/home/oliver/git/new-service", "--pilot"));
        assertTrue(out.toString().contains("new-service is ready"), out.toString());

        var posted = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(posted.bodyText().contains("/home/oliver/git/new-service"), posted.bodyText());
    }

    @Test
    @DisplayName("project add --pilot marks the enrolment as pilot mode")
    void enrolPilot() throws Exception {
        var server = runtime((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, """
                                    {"status":"started","onboarding_id":"onb-new-service","checkout_path":".",
                                     "project_id":"new-service","current_step":"Identity","step_number":2,"total_steps":10}
                                    """);
            } else if ("/api/v1/projects/new-service/setup".equals(request.target())) {
                response.json(200, """
                                    {"project_id":"new-service","state":"project.completed","step":10,
                                     "total_steps":10,"summary":"new-service is ready","links":{}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, execute("project", "add", "--pilot", "."));
        assertTrue(out.toString().contains("new-service is ready"), out.toString());
        assertTrue(server.awaitRequest(Duration.ofSeconds(1)).bodyText().contains("\"pilot\":true"));
    }

    @Test
    @DisplayName("enrol reports a root conflict refusal (PM-EXEC-2)")
    void enrolRootConflict() throws Exception {
        runtime((request, response) -> response.json(409,
                            JsonTree.write(Map.of("error", "enrolment_root_conflict"))));

        assertEquals(1, execute("project", "add", "/home/oliver/git/taken"));
        assertTrue(out.toString().contains("Another enrolment already names /home/oliver/git/taken"),
                            out.toString());
    }

    @Test
    @DisplayName("enrol reports a store root mismatch refusal (PM-EXEC-2)")
    void enrolStoreRootMismatch() throws Exception {
        runtime((request, response) -> response.json(409,
                            JsonTree.write(Map.of("error", "project_store_root_mismatch"))));

        assertEquals(1, execute("project", "add", "/home/oliver/git/other"));
        assertTrue(out.toString().contains("names a different checkout root"), out.toString());
    }

    @Test
    @DisplayName("enrol strips terminal control sequences from the path it reports")
    void enrolStripsControlSequences() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/project-onboardings".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, """
                                    {"status":"started","onboarding_id":"onb-x","checkout_path":"/x",
                                     "project_id":"x","current_step":"Identity","step_number":2,"total_steps":10}
                                    """);
            } else if ("/api/v1/projects/x/setup".equals(request.target())) {
                response.json(200, """
                                    {"project_id":"x","state":"project.completed","step":10,
                                     "total_steps":10,"summary":"x is ready","links":{}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, execute("project", "add", "\u001B[31m/x\u001B[0m", "--pilot"));
        assertFalse(out.toString().contains("\u001B[31m"), out.toString());
    }

    @Test
    @DisplayName("unenrol posts the project id and reports the removal")
    void unenrolProject() throws Exception {
        var server = runtime((request, response) -> {
            if (request.target().endsWith("/unenrol")) {
                response.json(200, "{\"status\":\"unenrolled\",\"project_id\":\"old-app\"}");
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, execute("project", "unenrol", "old-app", "--yes"));
        assertTrue(out.toString().contains("✓ old-app unenrolled"), out.toString());
        assertEquals("/api/v1/projects/old-app/unenrol", server.awaitRequest(Duration.ofSeconds(1)).target());
    }

    @Test
    @DisplayName("unenrol refuses a missing confirmation in --no-input mode without sending")
    void unenrolRequiresConfirmation() throws Exception {
        var server = runtime((request, response) -> response.json(200, "{}"));

        assertAll(
                            () -> assertEquals(3, execute("project", "unenrol", "old-app", "--no-input")),
                            () -> assertTrue(err.toString().contains("Unenrol confirmation required (--yes)"),
                                                err.toString()),
                            () -> assertTrue(server.requests().isEmpty(), "no request may be sent")
        );
    }

    @Test
    @DisplayName("unenrol reports the scope_active refusal while work runs")
    void unenrolScopeActive() throws Exception {
        runtime((request, response) -> response.json(409, JsonTree.write(Map.of("error", "scope_active"))));

        assertEquals(1, execute("project", "unenrol", "busy-app", "--yes"));
        assertTrue(err.toString().contains("busy-app still runs work"), err.toString());
    }

    @Test
    @DisplayName("unenrol reports a missing project")
    void unenrolNotFound() throws Exception {
        runtime((request, response) -> response.json(404, "{\"error\":\"project_not_found\"}"));

        assertEquals(1, execute("project", "unenrol", "missing", "--yes"));
        assertTrue(err.toString().contains("missing not found."), err.toString());
    }

    @Test
    @DisplayName("enrol and unenrol are no verbs: they are reported with the usage exit code")
    void foldedVerbsAreUnknown() throws Exception {
        var server = runtime((request, response) -> response.json(200, "{}"));

        assertAll(
                            () -> assertEquals(2, execute("enrol", ".")),
                            () -> assertEquals(2, execute("unenrol", "old-app", "--yes")),
                            () -> assertTrue(server.requests().isEmpty(), "no request may be sent"));
    }
}
