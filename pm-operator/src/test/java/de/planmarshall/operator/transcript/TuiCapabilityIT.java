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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nayuki.qrcodegen.QrCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.api.testsupport.VerificationResults;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.PmOperator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * The full-screen views on a real pseudo-terminal (PM-TEST-7): the process is started on a
 * {@code script(1)} PTY, keys are injected mid-run, the ANSI output is parsed, and the visible
 * result, the exit, and the restoration are asserted. It proves the console hand-off
 * ({@code plans}/{@code tui}) and the direct entry points on the same terminal shape an operator
 * uses, including the documented fallbacks.
 */
@DisplayName("pm-operator TUI capability on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
class TuiCapabilityIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String SCRIPT = "/usr/bin/script";
    private static final String TERM = "TERM";
    private static final String TERM_PROGRAM = "TERM_PROGRAM";
    private static final String PATH = "PATH";
    private static final String PROMPT = "pm \u203a";

    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class, QrCode.class);

    private MockRuntime runtime;

    @BeforeEach
    void startRuntime() throws IOException {
        Assumptions.assumeTrue(Files.isExecutable(Path.of(SCRIPT)), SCRIPT + " is not available");
        startScenario(StandardScenarios.FULLY_ONBOARDED);
    }

    private void startScenario(String scenario) throws IOException {
        if (runtime != null) {
            runtime.close();
        }
        runtime = MockRuntime.start(MockRuntimeConfig.builder().scenario(scenario).build());
        MockMachine.recordLiveRuntime(runtime);
    }

    @AfterEach
    void stopRuntime() throws IOException {
        if (runtime != null) {
            runtime.close();
        }
    }

    @Test
    @DisplayName("plans at 120x30 opens the alternate screen and q restores the terminal")
    void plansAtOneHundredTwentyByThirty() throws Exception {
        try (var session = start(List.of("plans"), Map.of(), 120, 30)) {
            assertTrue(session.awaitText("Running work", TIMEOUT), session.rawOutput());

            assertTrue(session.alternateScreenActive(), session.screenText());
            assertEquals(1, session.alternateScreenEntries(), session.rawOutput());

            session.send("q");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());

            assertEquals(0, session.exitCode(), session.rawOutput());
            assertTrue(session.alternateScreenExits() >= 1, session.screenText());
            assertTrue(session.cursorVisible(), session.screenText());
        }
        record("tui-capability-" + binary.mode(), Map.of(
                            "mode", binary.mode(),
                            "columns", 120L,
                            "rows", 30L,
                            "alternate_screen", true));
    }

    @Test
    @DisplayName("plans at 80x24 opens the alternate screen and q restores the terminal")
    void plansAtEightyByTwentyFour() throws Exception {
        try (var session = start(List.of("plans"), Map.of(), 80, 24)) {
            assertTrue(session.awaitText("Running work", TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenActive(), session.screenText());

            session.send("q");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
            assertTrue(session.cursorVisible(), session.screenText());
        }
    }

    @Test
    @DisplayName("--tui opens the overview directly")
    void tuiFlagOpens() throws Exception {
        try (var session = start(List.of("--tui"), Map.of(), 120, 30)) {
            assertTrue(session.awaitText("Running work", TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenActive(), session.screenText());

            session.send("q");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
        }
    }

    @Test
    @DisplayName("plan fix-relay opens the plan view and q restores the terminal")
    void planViewOpens() throws Exception {
        try (var session = start(List.of("plan", "fix-relay"), Map.of(), 120, 30)) {
            assertTrue(session.awaitText("fix-relay", TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenActive(), session.screenText());

            session.send("q");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
            assertTrue(session.cursorVisible(), session.screenText());
        }
    }

    @Test
    @DisplayName("projects opens the explorer and q restores the terminal")
    void projectsOpens() throws Exception {
        try (var session = start(List.of("projects"), Map.of(), 120, 30)) {
            assertTrue(session.awaitText("All projects", TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenActive(), session.screenText());

            session.send("q");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
        }
    }

    @Test
    @DisplayName("system in the console opens the system view and q returns to the prompt")
    void systemInConsoleOpens() throws Exception {
        startScenario(StandardScenarios.REPORTS_AND_SYSTEM);
        try (var session = start(List.of(), Map.of(), 120, 30)) {
            assertTrue(session.awaitText(PROMPT, TIMEOUT), session.rawOutput());

            session.send("system\n");
            assertTrue(session.awaitText("System", TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenActive(), session.screenText());

            session.send("q");
            assertTrue(session.awaitText(PROMPT, TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenExits() >= 1, session.screenText());

            session.send("exit\n");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
        }
    }

    @Test
    @DisplayName("plans --json prints JSON and never enters the alternate screen")
    void plansJsonStaysInline() throws Exception {
        try (var session = start(List.of("plans", "--json"), Map.of(), 120, 30)) {
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());

            assertEquals(0, session.exitCode(), session.rawOutput());
            assertEquals(0, session.alternateScreenEntries(), session.rawOutput());
            assertTrue(session.rawOutput().contains("\"status\""), session.rawOutput());
        }
    }

    @Test
    @DisplayName("50x20 falls back to the tree with the too-narrow line")
    void tooNarrowFallsBack() throws Exception {
        try (var session = start(List.of("plans"), Map.of(), 50, 20)) {
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());

            assertEquals(0, session.exitCode(), session.rawOutput());
            assertEquals(0, session.alternateScreenEntries(), session.rawOutput());
            assertTrue(session.rawOutput().contains("too narrow"), session.rawOutput());
            assertTrue(session.rawOutput().contains("pm-mcp-clients"), session.rawOutput());
        }
    }

    @Test
    @DisplayName("TERM=dumb names the reason and prints the tree")
    void dumbTerminalFallsBack() throws Exception {
        try (var session = start(List.of("plans"), Map.of(TERM, "dumb"), 120, 30)) {
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());

            assertEquals(0, session.exitCode(), session.rawOutput());
            assertEquals(0, session.alternateScreenEntries(), session.rawOutput());
            assertTrue(session.rawOutput().contains("dumb terminal"), session.rawOutput());
            assertTrue(session.rawOutput().contains("Runtime"), session.rawOutput());
        }
    }

    @Test
    @DisplayName("console.full_screen=false names the reason and prints the tree")
    void configuredOffFallsBack() throws Exception {
        Files.writeString(runtime.base().resolve("machine-config.json"),
                            "{\"format_version\":1,\"console\":{\"full_screen\":false}}");
        try (var session = start(List.of("plans"), Map.of(), 120, 30)) {
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());

            assertEquals(0, session.exitCode(), session.rawOutput());
            assertEquals(0, session.alternateScreenEntries(), session.rawOutput());
            assertTrue(session.rawOutput().contains("machine-config.json"), session.rawOutput());
            assertTrue(session.rawOutput().contains("Runtime"), session.rawOutput());
        }
    }

    @Test
    @DisplayName("the VS Code-shaped terminal (vscode program, xterm type, 80x24) opens the view")
    void vscodeShapedTerminalOpens() throws Exception {
        try (var session = start(List.of("plans"),
                                 Map.of(TERM_PROGRAM, "vscode", TERM, "xterm-256color"), 80, 24)) {
            assertTrue(session.awaitText("Running work", TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenActive(), session.screenText());

            session.send("q");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
            assertTrue(session.cursorVisible(), session.screenText());
        }
    }

    @Test
    @DisplayName("typing plans in the console opens the same view and returns to the prompt")
    void consolePlansHandsOff() throws Exception {
        try (var session = start(List.of(), Map.of(), 120, 30)) {
            assertTrue(session.awaitText(PROMPT, TIMEOUT), session.rawOutput());

            session.send("plans\n");
            assertTrue(session.awaitText("Running work", TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenActive(), session.screenText());
            assertEquals(1, session.alternateScreenEntries(), session.rawOutput());

            session.send("q");
            assertTrue(session.awaitText(PROMPT, TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenExits() >= 1, session.screenText());
            assertTrue(session.cursorVisible(), session.screenText());

            session.send("exit\n");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
        }
    }

    @Test
    @DisplayName("typing tui in the console opens the same view and returns to the prompt")
    void consoleTuiHandsOff() throws Exception {
        try (var session = start(List.of(), Map.of(), 120, 30)) {
            assertTrue(session.awaitText(PROMPT, TIMEOUT), session.rawOutput());

            session.send("tui\n");
            assertTrue(session.awaitText("Running work", TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenActive(), session.screenText());

            session.send("q");
            assertTrue(session.awaitText(PROMPT, TIMEOUT), session.rawOutput());
            assertTrue(session.alternateScreenExits() >= 1, session.screenText());

            session.send("exit\n");
            assertTrue(session.awaitExit(TIMEOUT), session.rawOutput());
            assertEquals(0, session.exitCode(), session.rawOutput());
        }
    }

    private PtySession start(List<String> arguments, Map<String, String> extraEnvironment,
                        int columns, int rows) throws IOException {
        var environment = new HashMap<>(runtime.environment());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "pty-host");
        environment.put("LANG", "en_US.UTF-8");
        environment.put(TERM, "xterm-256color");
        environment.put(PATH, "/usr/bin:/bin");
        // The view's saved selection and the history live outside the machine base; keep them in
        // the test's own tree so one case cannot open where another left off.
        environment.put("XDG_STATE_HOME", runtime.base().resolve("xdg-state").toString());
        environment.putAll(extraEnvironment);
        return PtySession.start(binary.with(arguments.toArray(String[]::new)), environment,
                            Path.of("").toAbsolutePath(), columns, rows);
    }

    /**
     * Records the mode the capability matrix ran in, so the report can name the native run when the
     * binary exists.
     *
     * @param item   the verification item
     * @param values the measured values
     */
    static void record(String item, Map<String, Object> values) {
        try {
            var all = new LinkedHashMap<String, Object>(values);
            VerificationResults.write(item, all, true);
        } catch (IOException _) {
            // The verification record is best effort; the assertions are the gate.
        }
    }
}
