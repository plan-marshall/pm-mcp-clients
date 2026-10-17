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
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

@DisplayName("OnboardCommand and SettingsCommand")
class OnboardCommandTest {

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

    private OperatorContext createContext(String input) throws IOException {
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();
        var terminal = new DumbTerminal(inStream, outStream);
        var clientEnv = ClientEnvironment.capture(fixture.environment());
        return new OperatorContext(fixture.environment(), tempDir, true, clientEnv, () -> terminal, null);
    }

    private void seedOnboardingRoutes() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            var path = request.target();
            var method = request.method();
            if (OnboardCommand.STATE_PATH.equals(path)) {
                response.json(200, """
                                    {"scope":"machine","state":"harness.consent","summary":"Look for harnesses on this machine?",
                                     "completed_steps":1,"auto":false,
                                     "links":{"yes":{"href":"/api/v1/onboarding/harness/scan","method":"POST","next":"harness.choose"},
                                              "no":{"href":"/api/v1/onboarding/harness/decline","method":"POST","next":"harness.consent"}}}
                                    """);
            } else if (OnboardCommand.STEP_PATH.equals(path) && "POST".equals(method)) {
                response.json(200, """
                                    {"scope":"machine","state":"harness.choose","summary":"Choose the harnesses to enrol",
                                     "completed_steps":1,"auto":false,
                                     "links":{"choose":{"href":"/api/v1/onboarding/harness/choose","method":"POST","next":"harness.probe-consent"}}}
                                    """);
            } else if (OnboardCommand.AUTO_PATH.equals(path) && "POST".equals(method)) {
                response.json(200, """
                                    {"scope":"machine","state":"auto.consent","summary":"Decide everything for me?",
                                     "completed_steps":0,"auto":true}
                                    """);
            } else {
                response.send(404, null, "Not found: " + path);
            }
        });
    }

    @Nested
    @DisplayName("onboard")
    class Onboard {

        @Test
        @DisplayName("shows the current state and its offered steps")
        void showsCurrentState() throws Exception {
            seedOnboardingRoutes();

            var writer = new StringWriter();
            var cmd = new CommandLine(new OnboardCommand(createContext("")));
            cmd.setOut(new PrintWriter(writer));

            int code = cmd.execute();

            var output = writer.toString();
            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertTrue(output.contains("Machine onboarding")),
                                () -> assertTrue(output.contains("Look for harnesses on this machine?")),
                                () -> assertTrue(output.contains("Step 2 of 7")),
                                () -> assertTrue(output.contains("yes")),
                                () -> assertTrue(output.contains("no")));
        }

        @Test
        @DisplayName("follows a named step")
        void followsStep() throws Exception {
            seedOnboardingRoutes();

            var writer = new StringWriter();
            var cmd = new CommandLine(new OnboardCommand(createContext("")));
            cmd.setOut(new PrintWriter(writer));

            int code = cmd.execute("--step", "yes");

            var output = writer.toString();
            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertTrue(output.contains("Choose the harnesses to enrol")));
        }

        @Test
        @DisplayName("starts the automatic run with a confirmed consent")
        void startsAutomaticRun() throws Exception {
            seedOnboardingRoutes();

            var writer = new StringWriter();
            var cmd = new CommandLine(new OnboardCommand(createContext("")));
            cmd.setOut(new PrintWriter(writer));

            int code = cmd.execute("--auto", "--yes");

            var output = writer.toString();
            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertTrue(output.contains("Decide everything for me?")));
        }

        @Test
        @DisplayName("declines the automatic run when the confirmation is not given")
        void declinesAutomaticRun() throws Exception {
            seedOnboardingRoutes();

            var writer = new StringWriter();
            var cmd = new CommandLine(new OnboardCommand(createContext("n\n")));
            cmd.setOut(new PrintWriter(writer));

            int code = cmd.execute("--auto");

            var output = writer.toString();
            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertTrue(output.contains("Nothing was done.")));
        }

        @Test
        @DisplayName("asks for input instead of running non-interactively")
        void failsWithoutInput() throws Exception {
            seedOnboardingRoutes();

            var writer = new StringWriter();
            var cmd = new CommandLine(new OnboardCommand(createContext("")));
            cmd.setOut(new PrintWriter(writer));

            int code = cmd.execute("--auto", "--no-input");

            var output = writer.toString();
            assertAll(
                                () -> assertEquals(3, code),
                                () -> assertTrue(output.contains("Decide everything for me?")));
        }
    }

    @Nested
    @DisplayName("settings machine")
    class SettingsMachine {

        private static final String OVERVIEW = """
                            {"revision":4,
                             "aspects":[
                               {"aspect":"language","step":1,"summary":"Deutsch, models English","current":"de"},
                               {"aspect":"harness","step":2,"summary":"Claude Code 2.4.1 (default)","current":"claude"}],
                             "language_interface":"de","language_model":"en","documents_mode":"ask",
                             "cleanup_durations":{"plans_after_days":30,"epics_after_days":30,"logs_after_days":30,"reports_after_days":30,"unenrolled_after_days":30,"remind_after_days":3},
                             "plan_defaults":{"landing":"landing.pull-request","method":"method.test-first","asking":"ask.key-points"},
                             "consents":[{"kind":"scan_harnesses","status":"remembered","remember":true,"channel":"terminal"}],
                             "credentials":[]}
                            """;

        @Test
        @DisplayName("shows the revision, the aspects and the remembered consents")
        void showsOverview() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                if (SettingsCommand.MachineCommand.MACHINE_PATH.equals(request.target())) {
                    response.json(200, OVERVIEW);
                } else {
                    response.send(404, null, "");
                }
            });

            var writer = new StringWriter();
            var cmd = new CommandLine(new SettingsCommand.MachineCommand(createContext("")));
            cmd.setOut(new PrintWriter(writer));

            int code = cmd.execute();

            var output = writer.toString();
            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertTrue(output.contains("Machine settings")),
                                () -> assertTrue(output.contains("Settings revision 4")),
                                () -> assertTrue(output.contains("1. Language: Deutsch, models English")),
                                () -> assertTrue(output.contains("Remembered consents")),
                                () -> assertTrue(output.contains("look for harnesses")));
        }

        @Test
        @DisplayName("applies a change against the current revision")
        void appliesChange() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                if (SettingsCommand.MachineCommand.MACHINE_PATH.equals(request.target())
                                    && "GET".equals(request.method())) {
                    response.json(200, OVERVIEW);
                } else if (SettingsCommand.MachineCommand.MACHINE_PATH.equals(request.target())
                                    && "PUT".equals(request.method())) {
                    response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":5}");
                } else {
                    response.send(404, null, "");
                }
            });

            var writer = new StringWriter();
            var cmd = new CommandLine(new SettingsCommand.MachineCommand(createContext("")));
            cmd.setOut(new PrintWriter(writer));

            int code = cmd.execute("--set", "documents_mode=copy");

            var output = writer.toString();
            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertTrue(output.contains("Saved.")));
        }

        @Test
        @DisplayName("keeps going through a conflict on --yes and applies the change again")
        void recoversFromConflict() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            final int[] putCount = {0};
            fixture.start((request, response) -> {
                if (SettingsCommand.MachineCommand.MACHINE_PATH.equals(request.target())
                                    && "GET".equals(request.method())) {
                    response.json(200, OVERVIEW);
                } else if (SettingsCommand.MachineCommand.MACHINE_PATH.equals(request.target())
                                    && "PUT".equals(request.method())) {
                    putCount[0]++;
                    if (putCount[0] == 1) {
                        response.json(409, "{\"applied\":false,\"conflict\":true,\"revision\":9,\"current_value\":\"copy\"}");
                    } else {
                        response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":10}");
                    }
                } else {
                    response.send(404, null, "");
                }
            });

            var writer = new StringWriter();
            var cmd = new CommandLine(new SettingsCommand.MachineCommand(createContext("")));
            cmd.setOut(new PrintWriter(writer));

            int code = cmd.execute("--set", "documents_mode=move", "--yes");

            var output = writer.toString();
            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertEquals(2, putCount[0]),
                                () -> assertTrue(output.contains("Changed meanwhile")),
                                () -> assertTrue(output.contains("Saved.")));
        }

        @Test
        @DisplayName("withdraws a remembered consent")
        void withdrawsConsent() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                if (SettingsCommand.MachineCommand.MACHINE_PATH.equals(request.target())
                                    && "GET".equals(request.method())) {
                    response.json(200, OVERVIEW);
                } else if (SettingsCommand.MachineCommand.MACHINE_PATH.equals(request.target())
                                    && "PUT".equals(request.method())) {
                    response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":5}");
                } else {
                    response.send(404, null, "");
                }
            });

            var writer = new StringWriter();
            var cmd = new CommandLine(new SettingsCommand.MachineCommand(createContext("")));
            cmd.setOut(new PrintWriter(writer));

            int code = cmd.execute("--withdraw", ConsentKind.SCAN_HARNESSES.wireValue());

            var output = writer.toString();
            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertTrue(output.contains("Consent withdrawn")));
        }
    }

    @Test
    @DisplayName("round-trips the machine settings contract through its own record")
    void contractRoundTrip() throws Exception {
        var record = new MachineSettingsRecord(3,
                            List.of(new MachineSettingsAspectSummary(OnboardingAspect.PLANS, "Pull Request", "landing.pull-request")),
                            "en", "same", DocumentsMode.ASK, CleanupDurations.defaults(), PlanDefaults.shipped(),
                            List.of(new ConsentRecord(ConsentKind.SCAN_HARNESSES, ConsentStatus.REMEMBERED, true,
                                                null, "terminal", null, null)),
                            List.of());
        var parsed = MachineSettingsRecord.fromJson(record.toJson());
        assertEquals(3, parsed.revision());
        assertFalse(parsed.aspects().isEmpty());
    }
}
