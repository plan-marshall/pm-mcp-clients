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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nayuki.qrcodegen.QrCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.PmOperator;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * The line a full-screen view leaves behind (CLI-CC-S-05, CLI-CC-P-06): {@code q} prints one line
 * naming where the operator was and what arrived meanwhile, also when nothing did
 * ({@code doc/design/terminal-client/15-overview.adoc#full-screen-live}). It runs the packaged
 * operator on a real pseudo-terminal, because the line is printed after the alternate screen is
 * gone and only a terminal shows what the operator is left with.
 */
@DisplayName("pm-operator summary line after q on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class SummaryLineIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String SCRIPT = "/usr/bin/script";
    private static final String OVERVIEW_PREFIX = "While the overview was open: ";

    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class, QrCode.class);

    @Test
    @DisplayName("plans then q prints the line for the overview")
    void plansThenQPrintsTheLine() throws Exception {
        try (var runtime = start(StandardScenarios.RUNNING_WORK);
                            var session = open(runtime, "plans")) {
            assertTrue(session.awaitText("Running work", TIMEOUT), session.rawOutput());

            session.send("q");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());

            assertEquals(0, session.exitCode(), session.rawOutput());
            var leaving = leavingLine(session.rawOutput());
            assertTrue(leaving.startsWith(OVERVIEW_PREFIX), leaving);
            assertTrue(leaving.endsWith("."), leaving);
        }
    }

    @Test
    @DisplayName("plan fix-relay then q names the plan view, not the overview")
    void planViewThenQNamesItsOwnPlace() throws Exception {
        try (var runtime = start(StandardScenarios.RUNNING_WORK);
                            var session = open(runtime, "plan", "fix-relay")) {
            assertTrue(session.awaitText("fix-relay", TIMEOUT), session.rawOutput());

            session.send("q");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());

            var leaving = leavingLine(session.rawOutput());
            assertTrue(leaving.contains("fix-relay"), leaving);
            assertTrue(leaving.contains("nothing arrived"), leaving);
            assertFalse(leaving.contains("the overview"), leaving);
        }
    }

    /**
     * @param output the terminal output
     * @return the line the view left behind, from the plain text it printed after the view closed
     */
    private static String leavingLine(String output) {
        var start = output.lastIndexOf("While ");
        if (start < 0) {
            throw new AssertionError("the view left no line behind");
        }
        var end = start;
        while (end < output.length() && "\r\n\u001b".indexOf(output.charAt(end)) < 0) {
            end++;
        }
        return output.substring(start, end).strip();
    }

    private static MockRuntime start(String scenario) throws IOException {
        Assumptions.assumeTrue(Files.isExecutable(Path.of(SCRIPT)), SCRIPT + " is not available");
        var runtime = MockRuntime.start(MockRuntimeConfig.builder().scenario(scenario).build());
        MockMachine.recordLiveRuntime(runtime);
        return runtime;
    }

    private PtySession open(MockRuntime runtime, String... arguments) throws IOException {
        var environment = new HashMap<>(runtime.environment());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "summary-host");
        environment.put("LANG", "en_US.UTF-8");
        environment.put("TERM", "xterm-256color");
        environment.put("PATH", "/usr/bin:/bin");
        // The view's saved selection and the history live outside the machine base.
        environment.put("XDG_STATE_HOME", runtime.base().resolve("xdg-state").toString());
        return PtySession.start(binary.with(arguments), environment,
                            Path.of("").toAbsolutePath(), 120, 30);
    }
}
