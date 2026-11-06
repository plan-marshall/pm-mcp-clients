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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nayuki.qrcodegen.QrCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.PmOperator;
import de.planmarshall.operator.tui.AnsiScreen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * The console's badge line while a read is still running (CLI-CC-S-01): the console runs on a real
 * pseudo-terminal against a runtime it reaches through {@link StallingSocketProxy}, which holds the
 * {@code /api/v1/project-onboardings} read. Both screens come from the same runtime scenario and
 * the same session, so the badge line alone tells the two apart: the stalled read leaves the
 * project badge out, the recovered read adds it.
 *
 * <p>The two screens are written below {@code target/screens/104-cli-console-third-residue}, or below
 * {@code -Dpm.screens.out} when a plan's terminal check collects them under
 * {@code .plan/temp/terminal/{plan}/}, for the change's evidence.
 */
@DisplayName("pm-operator console badge line on a stalled project-onboardings read")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ConsoleBadgeStallIT {

    private static final String STALLED_PATH = "/api/v1/project-onboardings";
    private static final String PROJECT_BADGE = "setup cui-parent";
    private static final String CONSOLE_PROMPT = "pm ";
    private static final String PROMPT_SUFFIX = "\u203a";
    private static final String OUTPUT_SET = "104-cli-console-third-residue";

    /** The system property that moves the two screens to an absolute directory, as a plan's terminal check does. */
    private static final String OUTPUT_PROPERTY = "pm.screens.out";
    private static final Duration READY_TIMEOUT = Duration.ofSeconds(60);

    @TempDir
    Path temp;

    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class, QrCode.class);

    @Test
    @DisplayName("the stalled read leaves the project badge out and the recovered read adds it")
    void stalledReadThenRecoveredRead() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.FULLY_ONBOARDED).build())) {
            MockMachine.recordLiveRuntime(runtime);
            var machineRoot = StallingSocketProxy.copyMachineRoot(runtime.base(), temp.resolve("machine"));
            try (var proxy = StallingSocketProxy.start(runtime.paths().socket(),
                                     machineRoot.resolve("run").resolve("runtime.sock"),
                                     StallingSocketProxy.isPath(STALLED_PATH));
                 var session = PtySession.start(binary.with(), environment(machineRoot),
                                     Path.of("").toAbsolutePath(), 120, 30)) {
                assertTrue(session.awaitConsolePrompt(READY_TIMEOUT),
                                    () -> "the console did not draw its prompt within " + READY_TIMEOUT
                                                        + ": " + session.rawOutput());
                var stalled = session.screen();
                var stalledBadge = consolePromptLine(stalled);

                proxy.release();
                DialogueHarness.type(session, "status\n");
                assertTrue(session.awaitText(PROJECT_BADGE, READY_TIMEOUT),
                                    "the recovered read did not add the project badge within " + READY_TIMEOUT);
                var recovered = session.screen();
                var recoveredBadge = consolePromptLine(recovered);

                write("stalled", stalled);
                write("recovered", recovered);

                assertAll(
                                    () -> assertFalse(stalledBadge.contains(PROJECT_BADGE),
                                                        "the stalled read must not show the project badge: " + stalledBadge),
                                    () -> assertTrue(recoveredBadge.contains(PROJECT_BADGE),
                                                        "the recovered read must show the project badge: " + recoveredBadge),
                                    () -> assertNotEquals(stalledBadge, recoveredBadge,
                                                        "both reads must not print the same badge line"));
            }
        }
    }

    private static String consolePromptLine(AnsiScreen screen) {
        return screen.lines().stream()
                            .filter(line -> line.startsWith(CONSOLE_PROMPT) && line.contains(PROMPT_SUFFIX))
                            .reduce((_, last) -> last)
                            .orElseThrow(() -> new AssertionError("no console prompt on the screen"));
    }

    private static Map<String, String> environment(Path machineRoot) {
        var environment = new HashMap<String, String>();
        environment.put(MachinePaths.ENV_BASE, machineRoot.toString());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "capture-host");
        environment.put("LANG", "en_US.UTF-8");
        environment.put("TERM", "xterm-256color");
        environment.put("PATH", "/usr/bin:/bin");
        environment.put("XDG_STATE_HOME", machineRoot.resolve("xdg-state").toString());
        return Map.copyOf(environment);
    }

    private static void write(String name, AnsiScreen screen) throws IOException {
        var selected = System.getProperty(OUTPUT_PROPERTY);
        var output = selected != null && !selected.isBlank()
                            ? Path.of(selected)
                            : Path.of("target", "screens", OUTPUT_SET);
        Files.createDirectories(output);
        Files.writeString(output.resolve(name + ".txt"),
                            String.join("\n", screen.lines()) + "\n", StandardCharsets.UTF_8);
    }
}
