/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.operator.PmConsole;
import de.planmarshall.operator.TranscriptVerbRunner;

/**
 * The dialogues a transcript covers (PM-TEST-7), each against its mock runtime scenario. A test
 * class and its integration-test twin share these scenarios, so both runs compare with the same
 * recorded transcript.
 */
final class TranscriptDialogues {

    private TranscriptDialogues() {
    }

    /**
     * One rendered dialogue.
     *
     * @param name        the transcript name below {@code src/test/resources/transcript}
     * @param rendered    the rendered terminal buffer
     * @param machineRoot the throw-away machine root to replace in the transcript
     */
    record Dialogue(String name, String rendered, Path machineRoot) {

        /** Compares the rendered buffer with the recorded transcript. */
        void assertRecorded() {
            TranscriptAssert.assertMatches(name, rendered,
                                new TranscriptNormalizer().replacing(machineRoot, "<machine-root>"));
        }

        /**
         * @return the rendered buffer with the throw-away machine root replaced and the texts the
         *         clock moves left as they are, so two runs with different clocks can be compared
         */
        String withoutMachineRoot() {
            return rendered.replace(machineRoot.toString(), "<machine-root>");
        }
    }

    /**
     * @return the {@code status} verb against the mock runtime's overview scenario
     * @throws Exception if the runtime or the verb fails
     */
    static Dialogue status() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.RUNNING_WORK).timing(MockTiming.ORDERED).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime), runtime.base().getParent());
            driver.endOfInput();

            var exitCode = TranscriptVerbRunner.run(driver.context(),
                                driver.writer(), driver.writer(), "status");
            if (exitCode != 0) {
                throw new IllegalStateException("status exited with " + exitCode);
            }
            return new Dialogue("status", driver.render(), runtime.base());
        }
    }

    /**
     * @return the {@code web pair} verb against the mock runtime's pairing scenario
     * @throws Exception if the runtime or the verb fails
     */
    static Dialogue webPair() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.WEB_PAIRING).timing(MockTiming.ORDERED).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime), runtime.base().getParent());
            driver.endOfInput();

            var exitCode = TranscriptVerbRunner.run(driver.context(),
                                driver.writer(), driver.writer(), "web", "pair");
            if (exitCode != 0) {
                throw new IllegalStateException("web pair exited with " + exitCode);
            }
            return new Dialogue("web-pair", driver.render(), runtime.base());
        }
    }

    /**
     * @return {@code plan fix-relay --all} against the mock runtime's plan-view scenario
     * @throws Exception if the runtime or the verb fails
     */
    static Dialogue planView() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.PLAN_VIEW).timing(MockTiming.ORDERED).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime), runtime.base().getParent());
            driver.endOfInput();

            var exitCode = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(),
                                "plan", "fix-relay", "--all");
            if (exitCode != 0) {
                throw new IllegalStateException("plan fix-relay exited with " + exitCode);
            }
            return new Dialogue("plan-view", driver.render(), runtime.base());
        }
    }


    /**
     * @return {@code epic relay-hardening} against the mock runtime's running-work scenario
     * @throws Exception if the runtime or the verb fails
     */
    static Dialogue controlEpic() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.RUNNING_WORK).timing(MockTiming.ORDERED).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime), runtime.base().getParent());
            driver.endOfInput();

            var exitCode = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(),
                                "epic", "relay-hardening");
            if (exitCode != 0) {
                throw new IllegalStateException("epic relay-hardening exited with " + exitCode);
            }
            return new Dialogue("control-epic", driver.render(), runtime.base());
        }
    }

    /**
     * @return the console with {@code status} and {@code exit} typed at its prompt
     * @throws Exception if the runtime or the console fails
     */
    static Dialogue consoleStatus() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.STATUS_ONLY).timing(MockTiming.ORDERED).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime), runtime.base().getParent());
            driver.typeLine("status");
            driver.typeLine("exit");
            driver.endOfInput();

            var exitCode = new PmConsole(driver.context()).run();
            if (exitCode != 0) {
                throw new IllegalStateException("console exited with " + exitCode);
            }
            return new Dialogue("console-status", driver.render(), runtime.base());
        }
    }

    /**
     * @return the console with the machine settings and the paired devices typed at its prompt
     * @throws Exception if the runtime or the console fails
     */
    static Dialogue consoleSettings() throws Exception {
        return consoleSettings(Clock.systemUTC());
    }

    /**
     * @param clock the clock to inject into the console context
     * @return the console with the machine settings and the paired devices typed at its prompt
     * @throws Exception if the runtime or the console fails
     */
    static Dialogue consoleSettings(Clock clock) throws Exception {
        return consoleSettings(clock, "en_US.UTF-8", "console-settings");
    }

    /**
     * @param clock the clock to inject into the console context
     * @return the same console, run with the German catalogue
     * @throws Exception if the runtime or the console fails
     */
    static Dialogue consoleSettingsGerman(Clock clock) throws Exception {
        return consoleSettings(clock, "de_DE.UTF-8", "console-settings-de");
    }

    private static Dialogue consoleSettings(Clock clock, String language, String name) throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.ONBOARDING).timing(MockTiming.ORDERED).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime, language), runtime.base().getParent(), clock);
            driver.typeLine("config");
            driver.typeLine("q");
            driver.typeLine("web devices");
            driver.typeLine("config consents");
            driver.typeLine("exit");
            driver.endOfInput();

            var exitCode = new PmConsole(driver.context()).run();
            if (exitCode != 0) {
                throw new IllegalStateException("console exited with " + exitCode);
            }
            return new Dialogue(name, driver.render(), runtime.base());
        }
    }

    /**
     * @return {@code plan add} typed at the source list, with the ready step and the start
     * @throws Exception if the runtime or the verb fails
     */
    static Dialogue startPlan() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.START_SOURCES).timing(MockTiming.ORDERED).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime), runtime.base().getParent());
            driver.type("d\nAdd a Codeberg provider\n\n\n\n");
            driver.endOfInput();

            var exitCode = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(),
                                "plan", "add", "--project", "pm-mcp-clients");
            if (exitCode != 0) {
                throw new IllegalStateException("plan add exited with " + exitCode);
            }
            return new Dialogue("start-plan", driver.render(), runtime.base());
        }
    }

    /**
     * @return {@code plan add --no-input} as one shell command
     * @throws Exception if the runtime or the verb fails
     */
    static Dialogue startPlanNoInput() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.START_SOURCES).timing(MockTiming.ORDERED).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime), runtime.base().getParent());
            driver.endOfInput();

            var exitCode = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(),
                                "plan", "add", "--project", "pm-mcp-clients",
                                "--description", "Add a Codeberg provider", "--no-input");
            if (exitCode != 0) {
                throw new IllegalStateException("plan add --no-input exited with " + exitCode);
            }
            return new Dialogue("start-plan-no-input", driver.render(), runtime.base());
        }
    }

    private static Map<String, String> environment(MockRuntime runtime) {
        return environment(runtime, "en_US.UTF-8");
    }

    private static Map<String, String> environment(MockRuntime runtime, String language) {
        var environment = new HashMap<>(runtime.environment());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "transcript-host");
        environment.put("LANG", language);
        return environment;
    }
}
