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
@EnableGeneratorController
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

    /** @return a plan id the test owns, drawn so nothing pins one name */
    private static String planId() {
        return "plan-" + Generators.integers(100, 999).next();
    }

    /** @return a hold id the test owns, drawn so nothing pins one name */
    private static String holdId() {
        return "H-" + String.format("%04d", Generators.integers(1, 9_999).next());
    }

    private static String settingsJson(String plan, int revision, PlanSettingsValue... values) throws IOException {
        return new PlanSettingsRecord(plan, revision, List.of(values)).toJson();
    }

    private static PlanSettingsValue landing() {
        return new PlanSettingsValue("landing", "landing.pull-request", "project",
                            List.of("landing.pull-request", "landing.main", "landing.files"));
    }

    @Test
    @DisplayName("plan resolve releases the plan when the act resolves the hold")
    void resolveReleases() throws Exception {
        var plan = planId();
        var hold = holdId();
        var server = fixture.start((request, response) -> {
            if (("/api/v1/plans/" + plan + "/holds/" + hold + "/resolve").equals(request.target())
                                && "POST".equals(request.method())) {
                response.json(200, "{\"hold_id\":\"" + hold + "\",\"released\":true,\"subject\":\"" + plan + "\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "resolve", plan, hold, "--act", "accept");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains(plan + " goes on"))
        );

        var req = server.awaitRequest(Duration.ofSeconds(1));
        assertTrue(req.bodyText().contains("\"act\":\"accept\""));
    }

    @Test
    @DisplayName("plan resolve reports what still holds when the act did not release it")
    void resolveStillHeld() throws Exception {
        var plan = planId();
        var hold = holdId();
        fixture.start((request, response) -> {
            if (("/api/v1/plans/" + plan + "/holds/" + hold + "/resolve").equals(request.target())) {
                response.json(200, """
                                    {"hold_id":"%s","released":false,
                                     "subject":"%s","cause":"the plan reads cui-http, which this project may not read"}
                                    """.formatted(hold, plan));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "resolve", plan, hold, "--act", "retry");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains(
                                                "Still held: the plan reads cui-http, which this project may not read"))
        );
    }

    @Test
    @DisplayName("plan resolve fails when the runtime refuses the hold")
    void resolveRefused() throws Exception {
        var hold = holdId();
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("plan", "resolve", planId(), hold, "--act", "retry");
        assertEquals(1, code);
        assertTrue(err.toString().contains("This entry does not exist"), err.toString());
    }

    @Test
    @DisplayName("plan settings shows the plan's values with their origin")
    void settingsShow() throws Exception {
        var plan = planId();
        var revision = Generators.integers(2, 90).next();
        fixture.start((request, response) -> {
            if (("/api/v1/plans/" + plan + "/settings").equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(plan, revision, landing()));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", plan);
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("Settings \u00b7 " + plan)),
                            () -> assertTrue(out.toString().contains("landing.pull-request")),
                            () -> assertTrue(out.toString().contains("from the project"))
        );
    }

    @Test
    @DisplayName("plan settings applies a value against the current revision")
    void settingsApply() throws Exception {
        var plan = planId();
        var revision = Generators.integers(2, 90).next();
        var server = fixture.start((request, response) -> {
            if (("/api/v1/plans/" + plan + "/settings").equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(plan, revision, landing()));
            } else if (("/api/v1/plans/" + plan + "/settings").equals(request.target())) {
                response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":" + (revision + 1) + "}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", plan, "--set", "method=method.tests-after");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("Applies where " + plan
                                                + " next reads it; the plan's version becomes " + (revision + 1) + "."))
        );

        server.awaitRequest(Duration.ofSeconds(1));
        var put = server.awaitRequest(Duration.ofSeconds(1));
        assertAll(
                            () -> assertTrue(put.bodyText().contains("\"aspect\":\"method\"")),
                            () -> assertTrue(put.bodyText().contains("\"revision\":" + revision))
        );
    }

    @Test
    @DisplayName("plan settings applies again with --yes after a conflict")
    void settingsConflictAppliedAgain() throws Exception {
        var plan = planId();
        var revision = Generators.integers(2, 90).next();
        var puts = new AtomicInteger();
        fixture.start((request, response) -> {
            if (("/api/v1/plans/" + plan + "/settings").equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(plan, revision, landing()));
            } else if (("/api/v1/plans/" + plan + "/settings").equals(request.target())) {
                if (puts.incrementAndGet() == 1) {
                    response.json(409, """
                                        {"applied":false,"conflict":true,"revision":%d,"current_value":"method.test-first"}
                                        """.formatted(revision + 1));
                } else {
                    response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":" + (revision + 2) + "}");
                }
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", plan, "--set", "method=method.tests-after", "--yes");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("the plan's version becomes " + (revision + 2))),
                            () -> assertEquals(2, puts.get())
        );
    }

    @Test
    @DisplayName("plan settings stops on a conflict with --no-input")
    void settingsConflictNoInput() throws Exception {
        var plan = planId();
        var revision = Generators.integers(2, 90).next();
        fixture.start((request, response) -> {
            if (("/api/v1/plans/" + plan + "/settings").equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(plan, revision, landing()));
            } else if (("/api/v1/plans/" + plan + "/settings").equals(request.target())) {
                response.json(409, """
                                    {"applied":false,"conflict":true,"revision":%d,"current_value":"method.test-first"}
                                    """.formatted(revision + 1));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", plan, "--set", "method=method.tests-after", "--no-input");
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
        var plan = planId();
        var revision = Generators.integers(2, 90).next();
        fixture.start((request, response) -> {
            if (("/api/v1/plans/" + plan + "/settings").equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, settingsJson(plan, revision, landing()));
            } else if (("/api/v1/plans/" + plan + "/settings").equals(request.target())) {
                response.send(400, "application/json", "{\"error\":\"refused\",\"reason\":\"protected\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", plan, "--set", "landing=landing.main");
        assertEquals(1, code);
        assertTrue(out.toString().contains("landing.main is refused:"));
    }

    @Test
    @DisplayName("plan settings reports a missing plan")
    void settingsMissingPlan() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("plan", "settings", planId());
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("This entry does not exist"))
        );
    }
}
