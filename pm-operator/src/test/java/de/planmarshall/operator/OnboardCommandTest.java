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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.onboarding.ConsentKind;
import de.planmarshall.api.onboarding.ConsentRecord;
import de.planmarshall.api.onboarding.ConsentStatus;
import de.planmarshall.api.onboarding.DocumentsMode;
import de.planmarshall.api.onboarding.MachineSettingsAspectSummary;
import de.planmarshall.api.onboarding.MachineSettingsRecord;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.api.onboarding.PlanDefaults;
import de.planmarshall.api.system.CleanupDurations;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.dialogue.OnboardingSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The machine's onboarding through its two entry points, now that it has no verb of its own: the
 * bare {@code pm-operator}, which runs it from where it stands, and {@code config <aspect>}, which
 * runs one of its steps again as a later change (01-commands.adoc#command-tree).
 */
@DisplayName("The onboarding through the bare pm-operator and config <aspect>")
class OnboardCommandTest {

    private static final String HARNESS_CONSENT = """
                        {"scope":"machine","state":"harness.consent","summary":"Look for harnesses on this machine?",
                         "completed_steps":1,"auto":false,
                         "links":{"yes":{"href":"/api/v1/onboarding/harness/scan","method":"POST","next":"harness.choose","preselected":true},
                                  "no":{"href":"/api/v1/onboarding/harness/decline","method":"POST","next":"harness.consent"}}}
                        """;

    private static final String HARNESS_CHOOSE = """
                        {"scope":"machine","state":"harness.choose","summary":"Choose the harnesses to enrol",
                         "completed_steps":1,"auto":false,
                         "links":{"choose":{"href":"/api/v1/onboarding/harness/choose","method":"POST","next":"harness.probe-consent"}}}
                        """;

    private static final String UNONBOARDED_STATUS = """
                        {"status":"onboarding","onboarded":false,"version":"0.9.0","onboard":{"step":1},
                         "web":{"enabled":true,"port":7420}}
                        """;

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private FakeRuntimeServer seedOnboardingRoutes() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        return fixture.start((request, response) -> {
            var path = request.target();
            if ("/api/v1/status".equals(path)) {
                response.json(200, UNONBOARDED_STATUS);
            } else if (OnboardingSource.STATE_PATH.equals(path)) {
                response.json(200, HARNESS_CONSENT);
            } else if (OnboardingSource.STEP_PATH.equals(path) && "POST".equals(request.method())) {
                response.json(200, HARNESS_CHOOSE);
            } else {
                response.send(404, null, "");
            }
        });
    }

    /**
     * Runs the bare {@code pm-operator} on a terminal that reads the given input.
     *
     * @return the exit code and what the terminal showed
     */
    private Outcome runBare(String input) {
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var screen = new ByteArrayOutputStream();
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(inStream, screen), null);
        var code = PmOperator.execute(context, new PrintWriter(new StringWriter()),
                            new PrintWriter(new StringWriter()));
        return new Outcome(code, screen.toString(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("the bare pm-operator")
    class Bare {

        @Test
        @DisplayName("runs the onboarding here from where it stands, as the dialogue's first frame")
        void showsTheCurrentState() throws Exception {
            seedOnboardingRoutes();

            var outcome = runBare("h\nq\nexit\n");

            assertAll(
                                () -> assertEquals(0, outcome.code()),
                                () -> assertTrue(outcome.screen().contains("Look for harnesses on this machine?"),
                                                    outcome.screen()),
                                () -> assertTrue(outcome.screen().contains("Nothing was sent."), outcome.screen()));
        }

        @Test
        @DisplayName("follows the link the operator chooses to the next state")
        void followsAStep() throws Exception {
            var server = seedOnboardingRoutes();

            var outcome = runBare("h\n1\nq\nexit\n");

            assertAll(
                                () -> assertEquals(0, outcome.code()),
                                () -> assertTrue(outcome.screen().contains("Choose the harnesses to enrol"),
                                                    outcome.screen()),
                                () -> assertTrue(server.requests().stream().anyMatch(recorded ->
                                                                        OnboardingSource.STEP_PATH.equals(recorded.target())
                                                                                            && recorded.bodyText().contains("yes")),
                                                    "the chosen link was sent"));
        }

        @Test
        @DisplayName("sends no step while the operator leaves the dialogue at once")
        void leavingSendsNothing() throws Exception {
            var server = seedOnboardingRoutes();

            var outcome = runBare("h\nq\nexit\n");

            assertAll(
                                () -> assertEquals(0, outcome.code()),
                                () -> assertFalse(server.requests().stream().anyMatch(recorded ->
                                                                        OnboardingSource.STEP_PATH.equals(recorded.target())),
                                                    "no step was sent"));
        }
    }

    @Nested
    @DisplayName("config <aspect>")
    class ConfigAspect {

        @Test
        @DisplayName("runs one step again as a later change and reports it")
        void runsOneStepAgain() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var server = fixture.start((request, response) -> {
                if ("/api/v1/config/language".equals(request.target()) && "POST".equals(request.method())) {
                    response.json(200, "{}");
                } else {
                    response.send(404, null, "");
                }
            });
            var out = new StringWriter();
            var context = new OperatorContext(fixture.environment(), tempDir, true);

            var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()),
                                "config", "language", "--interface", "de");

            var request = server.awaitRequest(Duration.ofSeconds(1));
            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertTrue(request.bodyText().contains("\"interface\":\"de\""), request.bodyText()),
                                () -> assertTrue(out.toString().contains("Language settings updated"), out.toString()));
        }

        @Test
        @DisplayName("reports a step the runtime refuses")
        void reportsARefusedStep() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> response.send(409, null, ""));
            var err = new StringWriter();
            var context = new OperatorContext(fixture.environment(), tempDir, true);

            var code = PmOperator.execute(context, new PrintWriter(new StringWriter()), new PrintWriter(err),
                                "config", "language", "--interface", "de");

            assertAll(
                                () -> assertEquals(1, code),
                                () -> assertTrue(err.toString().contains("HTTP 409"), err.toString()));
        }
    }

    @Test
    @DisplayName("round-trips the machine settings contract through its own record")
    void contractRoundTrip() throws Exception {
        var settings = new MachineSettingsRecord(3,
                            List.of(new MachineSettingsAspectSummary(OnboardingAspect.PLANS, "Pull Request", "landing.pull-request")),
                            "en", "same", DocumentsMode.ASK, CleanupDurations.defaults(), PlanDefaults.shipped(),
                            List.of(new ConsentRecord(ConsentKind.SCAN_HARNESSES, ConsentStatus.REMEMBERED, true,
                                                null, "terminal", null, null)),
                            List.of());

        var parsed = MachineSettingsRecord.fromJson(settings.toJson());

        assertAll(
                            () -> assertEquals(3, parsed.revision()),
                            () -> assertFalse(parsed.aspects().isEmpty()));
    }

    private record Outcome(int code, String screen) {
    }
}
