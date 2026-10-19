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
import java.util.HashMap;
import java.util.Map;

import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
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
    }

    /**
     * @return the {@code status} verb against the mock runtime's overview scenario
     * @throws Exception if the runtime or the verb fails
     */
    static Dialogue status() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.RUNNING_WORK).build());
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
                                 .scenario(StandardScenarios.WEB_PAIRING).build());
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
     * @return the console with {@code status} and {@code exit} typed at its prompt
     * @throws Exception if the runtime or the console fails
     */
    static Dialogue consoleStatus() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.STATUS_ONLY).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime), runtime.base().getParent());
            driver.typeLine("status");
            driver.typeLine("exit");

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
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.ONBOARDING).build());
             var driver = TerminalTestDriver.create()) {
            MockMachine.recordLiveRuntime(runtime);
            driver.project(environment(runtime), runtime.base().getParent());
            driver.typeLine("config");
            driver.typeLine("q");
            driver.typeLine("web devices");
            driver.typeLine("config consents");
            driver.typeLine("exit");

            var exitCode = new PmConsole(driver.context()).run();
            if (exitCode != 0) {
                throw new IllegalStateException("console exited with " + exitCode);
            }
            return new Dialogue("console-settings", driver.render(), runtime.base());
        }
    }

    private static Map<String, String> environment(MockRuntime runtime) {
        var environment = new HashMap<>(runtime.environment());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "transcript-host");
        environment.put("LANG", "en_US.UTF-8");
        return environment;
    }
}
