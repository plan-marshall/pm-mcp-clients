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
import java.time.Duration;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The acts of the folded verb {@code abort} through their design verbs: work is ended as one shell
 * command only by {@code plan cancel}, {@code plan abandon} and {@code epic cancel}
 * (19-actions.adoc#commands).
 */
@EnableGeneratorController
@DisplayName("Ending work through plan cancel, plan abandon and epic cancel")
class AbortCommandTest {

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

    private FakeRuntimeServer acceptPost(String path) throws IOException {
        return fixture.start((request, response) -> {
            if (path.equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
    }

    @Test
    @DisplayName("plan cancel ends a running plan with its reason and archives it")
    void planCancel() throws Exception {
        var planId = Generators.letterStrings(4, 12).next();
        var reason = Generators.letterStrings(4, 24).next();
        var server = acceptPost("/api/v1/plans/" + planId + "/cancel");

        int code = execute("plan", "cancel", planId, "--reason", reason);

        var request = server.awaitRequest(Duration.ofSeconds(1));
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(request.bodyText().contains("\"reason\":\"" + reason + "\""),
                                                request.bodyText()),
                            () -> assertTrue(out.toString().contains("✓ " + planId + " cancelled and archived"),
                                                out.toString()));
    }

    @Test
    @DisplayName("plan abandon ends a paused plan and keeps its pull request with --keep-pr")
    void planAbandon() throws Exception {
        var planId = Generators.letterStrings(4, 12).next();
        var server = acceptPost("/api/v1/plans/" + planId + "/abandon");

        int code = execute("plan", "abandon", planId, "--reason", Generators.letterStrings(4, 24).next(),
                            "--keep-pr");

        var request = server.awaitRequest(Duration.ofSeconds(1));
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(request.bodyText().contains("\"close_pr\":false"), request.bodyText()),
                            () -> assertTrue(out.toString().contains("✓ " + planId + " abandoned and archived"),
                                                out.toString()));
    }

    @Test
    @DisplayName("epic cancel ends an epic and leaves its plans running with --without-plans")
    void epicCancel() throws Exception {
        var epicId = Generators.letterStrings(4, 12).next();
        var server = acceptPost("/api/v1/epics/" + epicId + "/cancel");

        int code = execute("epic", "cancel", epicId, "--reason", Generators.letterStrings(4, 24).next(),
                            "--without-plans");

        var request = server.awaitRequest(Duration.ofSeconds(1));
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(request.bodyText().contains("\"with_plans\":false"), request.bodyText()),
                            () -> assertTrue(out.toString().contains("✓ " + epicId + " cancelled"), out.toString()));
    }

    @Test
    @DisplayName("plan cancel --no-input without a reason stops with exit 3 and sends nothing")
    void planCancelWithoutReason() throws Exception {
        var planId = Generators.letterStrings(4, 12).next();
        var server = acceptPost("/api/v1/plans/" + planId + "/cancel");

        int code = execute("plan", "cancel", planId, "--no-input");

        assertAll(
                            () -> assertEquals(3, code),
                            () -> assertTrue(out.toString().contains("Cancellation reason required (--reason)"),
                                                out.toString()),
                            () -> assertTrue(server.requests().isEmpty(), "no request was sent"));
    }

    @Test
    @DisplayName("abort is no verb: it is reported with the usage exit code")
    void abortIsUnknown() {
        int code = execute("abort", "plan", Generators.letterStrings(4, 12).next());

        assertEquals(2, code);
    }
}
