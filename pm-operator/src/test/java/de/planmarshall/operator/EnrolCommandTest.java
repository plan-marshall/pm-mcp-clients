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
import de.planmarshall.api.resource.EnrolResult;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("EnrolCommand and UnenrolCommand")
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

    private static String enrolResult(String id, boolean pilot) {
        return new EnrolResult(EnrolResult.STATUS_ENROLLED, id, pilot).toJson();
    }

    @Test
    @DisplayName("enrol posts the checkout path and reports the enrolled project")
    void enrolProject() throws Exception {
        var server = runtime((request, response) -> {
            if ("/api/v1/enrol".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, enrolResult("new-service", false));
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, execute("enrol", "/home/oliver/git/new-service"));
        assertTrue(out.toString().contains("✓ Enrolled new-service (standard mode)"), out.toString());

        var posted = server.awaitRequest(Duration.ofSeconds(1));
        assertFalse(posted.bodyText().contains("\"pilot\":true"), posted.bodyText());
        assertTrue(posted.bodyText().contains("/home/oliver/git/new-service"), posted.bodyText());
    }

    @Test
    @DisplayName("enrol --pilot marks the enrolment as pilot mode")
    void enrolPilot() throws Exception {
        var server = runtime((request, response) -> {
            if ("/api/v1/enrol".equals(request.target())) {
                response.json(200, enrolResult("new-service", true));
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, execute("enrol", "--pilot", "."));
        assertTrue(out.toString().contains("✓ Enrolled new-service (pilot mode)"), out.toString());
        assertTrue(server.awaitRequest(Duration.ofSeconds(1)).bodyText().contains("\"pilot\":true"));
    }

    @Test
    @DisplayName("enrol reports a root conflict refusal (PM-EXEC-2)")
    void enrolRootConflict() throws Exception {
        runtime((request, response) -> response.json(409,
                            JsonTree.write(Map.of("error", "enrolment_root_conflict"))));

        assertEquals(1, execute("enrol", "/home/oliver/git/taken"));
        assertTrue(out.toString().contains("Another enrolment already names /home/oliver/git/taken"),
                            out.toString());
    }

    @Test
    @DisplayName("enrol reports a store root mismatch refusal (PM-EXEC-2)")
    void enrolStoreRootMismatch() throws Exception {
        runtime((request, response) -> response.json(409,
                            JsonTree.write(Map.of("error", "project_store_root_mismatch"))));

        assertEquals(1, execute("enrol", "/home/oliver/git/other"));
        assertTrue(out.toString().contains("names a different checkout root"), out.toString());
    }

    @Test
    @DisplayName("enrol strips terminal control sequences from the path it reports")
    void enrolStripsControlSequences() throws Exception {
        runtime((request, response) -> response.json(200, enrolResult("evil", false)));

        assertEquals(0, execute("enrol", "\u001B[31m/x\u001B[0m"));
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

        assertEquals(0, execute("unenrol", "old-app", "--yes"));
        assertTrue(out.toString().contains("✓ old-app unenrolled"), out.toString());
        assertEquals("/api/v1/projects/old-app/unenrol", server.awaitRequest(Duration.ofSeconds(1)).target());
    }

    @Test
    @DisplayName("unenrol refuses a missing confirmation in --no-input mode without sending")
    void unenrolRequiresConfirmation() throws Exception {
        var server = runtime((request, response) -> response.json(200, "{}"));

        assertAll(
                            () -> assertEquals(3, execute("unenrol", "old-app", "--no-input")),
                            () -> assertTrue(err.toString().contains("Unenrol confirmation required (--yes)"),
                                                err.toString()),
                            () -> assertTrue(server.requests().isEmpty(), "no request may be sent")
        );
    }

    @Test
    @DisplayName("unenrol reports the scope_active refusal while work runs")
    void unenrolScopeActive() throws Exception {
        runtime((request, response) -> response.json(409, JsonTree.write(Map.of("error", "scope_active"))));

        assertEquals(1, execute("unenrol", "busy-app", "--yes"));
        assertTrue(err.toString().contains("busy-app still runs work"), err.toString());
    }

    @Test
    @DisplayName("unenrol reports a missing project")
    void unenrolNotFound() throws Exception {
        runtime((request, response) -> response.json(404, "{\"error\":\"project_not_found\"}"));

        assertEquals(1, execute("unenrol", "missing", "--yes"));
        assertTrue(err.toString().contains("missing not found."), err.toString());
    }
}
