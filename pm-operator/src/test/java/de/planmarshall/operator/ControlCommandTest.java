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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsValue;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Controls a plan: resolves what holds it (UC-CTL-4) and changes its own settings (UC-CTL-5).
 *
 * <p>The verbs live in {@link PlanCommand} ({@code plan resolve}, {@code plan settings}), which is
 * the terminal client's home for the controlling-work actions (19-actions.adoc).
 */
@DisplayName("ControlCommand")
class ControlCommandTest {

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

    private static String settingsJson(int revision, PlanSettingsValue... values) throws IOException {
        return new PlanSettingsRecord("fix-relay", revision, List.of(values)).toJson();
    }

    private static PlanSettingsValue landing() {
        return new PlanSettingsValue("landing", "landing.pull-request", "project",
                            List.of("landing.pull-request", "landing.main", "landing.files"));
    }

    @Test
    @DisplayName("plan resolve releases the plan when the act resolves the hold")
    void resolveReleases() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/plans/relay-cursor-order/holds/H-0001/resolve".equals(request.target())
                                && "POST".equals(request.method())) {
                response.json(200, "{\"hold_id\":\"H-0001\",\"released\":true,\"subject\":\"relay-cursor-order\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "resolve", "relay-cursor-order", "H-0001", "--act", "accept");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("relay-cursor-order goes on"))
        );

        var req = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(req.bodyText().contains("\"act\":\"accept\""));
    }

    @Test
    @DisplayName("plan resolve reports what still holds when the act did not release it")
    void resolveStillHeld() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/holds/H-0001/resolve".equals(request.target())) {
                response.json(200, """
                                    {"hold_id":"H-0001","released":false,
                                     "subject":"fix-relay","cause":"the plan reads cui-http, which this project may not read"}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "resolve", "fix-relay", "H-0001", "--act", "retry");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains(
                                                "Still held: the plan reads cui-http, which this project may not read"))
        );
    }

    @Test
    @DisplayName("plan resolve fails when the runtime refuses the hold")
    void resolveRefused() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("plan", "resolve", "fix-relay", "H-9999", "--act", "retry");
        assertEquals(1, code);
        assertTrue(err.toString().contains("refused by the runtime"));
    }

    @Test
    @DisplayName("plan settings shows the plan's values with their origin")
    void settingsShow() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/settings".equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(7, landing()));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", "fix-relay");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("Settings \u00b7 fix-relay")),
                            () -> assertTrue(out.toString().contains("landing.pull-request")),
                            () -> assertTrue(out.toString().contains("from the project"))
        );
    }

    @Test
    @DisplayName("plan settings applies a value against the current revision")
    void settingsApply() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/settings".equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(7, landing()));
            } else if ("/api/v1/plans/fix-relay/settings".equals(request.target())) {
                response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":8}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", "fix-relay", "--set", "method=method.tests-after");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains(
                                                "Applies where fix-relay next reads it; the plan's version becomes 8."))
        );

        server.awaitRequest(Duration.ofSeconds(1));
        var put = server.awaitRequest(Duration.ofSeconds(1));
        assertAll(
                            () -> assertTrue(put.bodyText().contains("\"aspect\":\"method\"")),
                            () -> assertTrue(put.bodyText().contains("\"revision\":7"))
        );
    }

    @Test
    @DisplayName("plan settings applies again with --yes after a conflict")
    void settingsConflictAppliedAgain() throws Exception {
        var puts = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/settings".equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(7, landing()));
            } else if ("/api/v1/plans/fix-relay/settings".equals(request.target())) {
                if (puts.incrementAndGet() == 1) {
                    response.json(409, """
                                        {"applied":false,"conflict":true,"revision":8,"current_value":"method.test-first"}
                                        """);
                } else {
                    response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":9}");
                }
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", "fix-relay", "--set", "method=method.tests-after", "--yes");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("the plan's version becomes 9")),
                            () -> assertEquals(2, puts.get())
        );
    }

    @Test
    @DisplayName("plan settings stops on a conflict with --no-input")
    void settingsConflictNoInput() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/settings".equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(7, landing()));
            } else if ("/api/v1/plans/fix-relay/settings".equals(request.target())) {
                response.json(409, """
                                    {"applied":false,"conflict":true,"revision":8,"current_value":"method.test-first"}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", "fix-relay", "--set", "method=method.tests-after", "--no-input");
        assertAll(
                            () -> assertEquals(3, code),
                            () -> assertTrue(out.toString().contains(
                                                "Changed meanwhile: it is now method.test-first")),
                            () -> assertTrue(out.toString().contains("--no-input stops here"))
        );
    }

    @Test
    @DisplayName("plan settings reports a refused value with its reason")
    void settingsRefused() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/plans/fix-relay/settings".equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(7, landing()));
            } else if ("/api/v1/plans/fix-relay/settings".equals(request.target())) {
                response.send(400, "application/json", "{\"error\":\"refused\",\"reason\":\"protected\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", "fix-relay", "--set", "landing=landing.main");
        assertEquals(1, code);
        assertTrue(out.toString().contains("landing.main is refused:"));
    }

    @Test
    @DisplayName("plan settings reports a missing plan")
    void settingsMissingPlan() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("plan", "settings", "p-missing");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("Plan p-missing not found."))
        );
    }
}
