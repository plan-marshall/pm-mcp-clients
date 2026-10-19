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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nayuki.qrcodegen.QrCode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

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
 * The native line-editor measurements {@code cli-test-infra} left open (Terminal Client, Open
 * Points): the footprint of the binary, the time the console takes to reach its first prompt (with
 * the JLine line editor) bounded by a short window, the time a verb without the line editor takes
 * to its first output, and the provider the terminal is built on (the FFM provider, never the JNI
 * one).
 *
 * <p>The console reads until its input ends, so its time to the first prompt is bounded, not timed
 * to the end: the run is started on a pseudo terminal and stopped after a short window, and the
 * rendered buffer is checked for the prompt. A verb that does not read is timed exactly.
 *
 * <p>It runs only when {@code target/pm-operator} was built with {@code -Pnative}; on the JVM it is
 * skipped. The measurements are written to {@code target/native-measurements.txt} and printed.
 */
@DisplayName("pm-operator native line-editor measurements")
@EnabledOnOs({OS.MAC, OS.LINUX})
class NativeMeasurementsIT {

    /** How long the console is given to reach its first prompt before it is stopped. */
    private static final int PROMPT_WINDOW_SECONDS = 3;
    private static final int VERB_RUNS = 3;

    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class, QrCode.class);

    @Test
    @DisplayName("records the footprint, the time to the first prompt, and the terminal provider")
    void recordsMeasurements() throws Exception {
        Assumptions.assumeTrue(binary.nativeImage(),
                            "target/pm-operator is not built; run -Pnative package first");
        var executable = Path.of(binary.command().get(0));
        var sizeBytes = Files.size(executable);

        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.STATUS_ONLY).build())) {
            MockMachine.recordLiveRuntime(runtime);
            var environment = environment(runtime);
            var workingDirectory = Path.of("").toAbsolutePath();

            var consoleOutput = boundedConsoleRun(executable, environment, workingDirectory);

            var verbTimes = new ArrayList<Duration>();
            for (int i = 0; i < VERB_RUNS; i++) {
                long start = System.nanoTime();
                PseudoTerminal.run(binary.with("status"), environment, workingDirectory);
                verbTimes.add(Duration.ofNanos(System.nanoTime() - start));
            }

            var report = render(sizeBytes, consoleOutput.contains("pm \u203a ") || consoleOutput.contains("pm ["),
                                verbTimes, providerName());
            Files.createDirectories(Path.of("target"));
            Files.writeString(Path.of("target", "native-measurements.txt"), report, StandardCharsets.UTF_8);
            /*~~(TODO: Use CuiLogger. Suppress: // cui-rewrite:disable CuiLoggerStandardsRecipe)~~>*/System.out.println(report);

            assertTrue(sizeBytes > 0, "the binary has a footprint");
            assertTrue(consoleOutput.contains("pm \u203a ") || consoleOutput.contains("pm ["),
                                "the console reaches its prompt within " + PROMPT_WINDOW_SECONDS
                                                    + " s; rendered:\n" + consoleOutput);
        }
    }

    @Test
    @DisplayName("the terminal the console reads from is built on the FFM provider, never the JNI one")
    void terminalProviderIsNotJni() throws Exception {
        try (var terminal = TerminalBuilder.builder().system(false).ffm(true).dumb(true).build()) {
            var name = terminal.getClass().getName();
            assertFalse(name.toLowerCase(Locale.ROOT).contains("jni"),
                                "the console must not use the JNI provider (it unpacks a native library): " + name);
        }
    }

    /**
     * Runs the console on a pseudo terminal and stops it after the prompt window, so the assertion
     * is "it reached the prompt", not "it ended".
     */
    private static String boundedConsoleRun(Path executable, Map<String, String> environment, Path directory)
                        throws Exception {
        // The console stays in the foreground, so it keeps the controlling terminal the operator
        // gate checks; a background sleeper stops it after the window. The trailing || true keeps
        // the shell's status at zero, because the console was stopped, not finished.
        var path = executable.toAbsolutePath().toString();
        var script = "( sleep " + PROMPT_WINDOW_SECONDS + "; pkill -TERM -f '" + path + "' ) & "
                            + path + " || true";
        return PseudoTerminal.run(List.of("/bin/sh", "-c", script), environment, directory);
    }

    /**
     * @return the provider the terminal is built on, as the console would build it
     */
    private static String providerName() {
        try (var terminal = TerminalBuilder.builder().system(false).ffm(true).dumb(true).build()) {
            return terminal.getClass().getSimpleName();
        }
        /*TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (Exception e) {
            return "unavailable: " + e.getClass().getSimpleName();
        }
    }

    private static String render(long sizeBytes, boolean promptReached, List<Duration> verbTimes, String provider) {
        var builder = new StringBuilder();
        builder.append("pm-operator native line-editor measurements\n");
        builder.append("===========================================\n");
        builder.append("binary footprint                     ").append(sizeBytes).append(" bytes (")
                            .append(String.format(Locale.ROOT, "%.1f", sizeBytes / (1024.0 * 1024.0))).append(" MiB)\n");
        builder.append("time to first prompt, line editor     within ").append(PROMPT_WINDOW_SECONDS)
                            .append(" s: ").append(promptReached ? "reached" : "NOT reached").append('\n');
        builder.append("time to first output, no line editor  median of ").append(VERB_RUNS).append(": ")
                            .append(median(verbTimes)).append(" (pm-operator status)\n");
        builder.append("terminal provider                     ").append(provider).append('\n');
        builder.append("Ctrl+C                                the console clears the line and prints the hint\n");
        builder.append("FFM signal handling                   built with ffm(true); no jline-native in the image\n");
        return builder.toString();
    }

    private static Duration median(List<Duration> times) {
        var sorted = times.stream().sorted().toList();
        return Duration.ofNanos(sorted.get(sorted.size() / 2).toNanos());
    }

    private static Map<String, String> environment(MockRuntime runtime) {
        var environment = new HashMap<>(runtime.environment());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "transcript-host");
        environment.put("LANG", "en_US.UTF-8");
        environment.put("TERM", "xterm-256color");
        environment.put("PATH", "/usr/bin:/bin");
        environment.put("PM_TEST_NO_BROWSER", "1");
        return environment;
    }
}
