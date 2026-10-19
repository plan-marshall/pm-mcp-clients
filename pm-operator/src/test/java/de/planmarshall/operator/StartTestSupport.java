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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.operator.transcript.TerminalTestDriver;

/**
 * Runs a starting verb against one scenario of the mock runtime, with the scripted terminal the
 * transcript tests use. The tests of the three direct verbs share it, so a verb is assembled exactly
 * as the binary assembles it.
 */
final class StartTestSupport {

    private StartTestSupport() {
    }

    /**
     * @param scenario the mock scenario
     * @param input    the scripted keys, or {@code null} for end of input
     * @param args     the verb and its arguments
     * @return the exit code and everything the verb rendered
     * @throws Exception if the runtime or the verb fails
     */
    static Outcome run(String scenario, String input, String... args) throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder().scenario(scenario).build());
             var driver = TerminalTestDriver.create()) {
            recordLiveRuntime(runtime);
            var environment = new HashMap<>(runtime.environment());
            environment.put("LANG", "en_US.UTF-8");
            environment.put("USER", "operator");
            environment.put("PM_PROJECT", "pm-mcp-clients");
            driver.project(environment, runtime.base().getParent());
            if (input == null) {
                driver.endOfInput();
            } else {
                driver.type(input);
            }
            var code = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(), args);
            return new Outcome(code, driver.render());
        }
    }

    /**
     * Records the running mock runtime with the start instant of the host process.
     *
     * @param runtime the started mock runtime
     * @throws IOException if the record cannot be written
     */
    static void recordLiveRuntime(MockRuntime runtime) throws IOException {
        var self = ProcessHandle.current();
        var start = self.info().startInstant().orElseThrow();
        Files.createDirectories(runtime.paths().stateDir());
        Files.writeString(runtime.paths().runtimeRecord(),
                            "{\"pid\":" + self.pid() + ",\"start_instant\":\"" + start + "\"}",
                            StandardCharsets.UTF_8);
    }

    /**
     * @param code     the exit code
     * @param rendered everything the verb rendered
     */
    record Outcome(int code, String rendered) {
    }
}
