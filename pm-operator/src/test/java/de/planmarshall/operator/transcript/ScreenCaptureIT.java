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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.testsupport.BinaryUnderTest;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.operator.PmOperator;
import de.planmarshall.operator.StartPrompt;
import de.planmarshall.operator.tui.AnsiScreen;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fasterxml.jackson.core.JsonFactory;

import picocli.CommandLine;

/**
 * Runs a named screen-capture set on a real pseudo-terminal (CLI-CC-S-01): every non-blank,
 * non-comment line of {@code src/test/resources/screen-captures/{set}.txt} is one capture, and the
 * test writes the screen text, the cell attributes and the raw output below
 * {@code target/screens/{set}/} or, with {@code -Dpm.screens.out}, below that directory.
 *
 * <p>It is selected by {@code -Dpm.screens={set}} and skipped without it, so the ordinary
 * integration-test lane pays nothing for it. The command line is documented in
 * {@code doc/design/terminal-client/27-tui-testing.adoc}.
 */
@DisplayName("pm-operator screen captures on a real PTY")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnabledIfSystemProperty(named = "pm.screens", matches = "(?s).*\\P{javaWhitespace}.*")
class ScreenCaptureIT {

    private static final String SCRIPT = "/usr/bin/script";
    private static final String PROPERTY = "pm.screens";
    private static final String OUTPUT_PROPERTY = "pm.screens.out";
    private static final String RESOURCE_DIR = "screen-captures";
    private static final String OUTPUT_DIR = "screens";
    private static final Duration SETTLE = Duration.ofMillis(600);
    private static final Duration MIN_WAIT = Duration.ofSeconds(2);
    private static final Duration READY_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration POLL = Duration.ofMillis(50);

    /** One capture line of the set. */
    private record Capture(String name, String scenario, List<String> states, List<String> command,
    String keys, int columns, int rows, String locale, Map<String, String> environment) {
    }

    private final BinaryUnderTest binary = BinaryUnderTest.locate("pm-operator", PmOperator.class,
                        MachinePaths.class, CommandLine.class, JsonFactory.class,
                        Terminal.class, LineReader.class, QrCode.class);

    @Test
    @DisplayName("the selected capture set writes its screen, attribute, and stdout files")
    void captureSet() throws Exception {
        var setName = System.getProperty(PROPERTY);
        Assumptions.assumeTrue(setName != null && !setName.isBlank(),
                            "-D" + PROPERTY + "={set} selects the screen-capture set");
        Assumptions.assumeTrue(Files.isExecutable(Path.of(SCRIPT)), SCRIPT + " is not available");

        var captures = readSet(setName);
        assertFalse(captures.isEmpty(), "the capture set " + setName + " is empty");
        var selected = System.getProperty(OUTPUT_PROPERTY);
        var output = selected != null && !selected.isBlank()
                            ? Path.of(selected)
                            : Path.of("target", OUTPUT_DIR, setName);
        Files.createDirectories(output);

        for (var capture : captures) {
            capture(capture, output);
        }
    }

    private void capture(Capture capture, Path output) throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(capture.scenario()).build())) {
            MockMachine.recordLiveRuntime(runtime);
            for (var state : capture.states()) {
                runtime.goToState(state);
            }
            try (var session = start(runtime, capture)) {
                assertTrue(awaitQuiet(session, !capture.keys().isEmpty()),
                                    capture.name() + " did not settle within " + READY_TIMEOUT);
                var screen = session.screen();
                var raw = session.rawOutput();
                assertFalse(raw.isEmpty(), capture.name() + " wrote nothing to the terminal");
                Files.writeString(output.resolve(capture.name() + ".txt"),
                                    String.join("\n", screen.lines()) + "\n", StandardCharsets.UTF_8);
                Files.writeString(output.resolve(capture.name() + ".attrs.txt"),
                                    attributes(screen), StandardCharsets.UTF_8);
                if (capture.keys().isEmpty()) {
                    Files.writeString(output.resolve(capture.name() + ".stdout.txt"),
                                        raw, StandardCharsets.UTF_8);
                } else {
                    DialogueHarness.type(session, capture.keys());
                    assertTrue(session.awaitExit(READY_TIMEOUT),
                                        capture.name() + " did not exit within " + READY_TIMEOUT);
                    assertTrue(session.awaitReader(READY_TIMEOUT),
                                        capture.name() + "'s output reader did not finish");
                    // The whole output, the keys' part included: what a verb prints on leaving a
                    // view (`q`) is read here, the three files above are the view itself.
                    Files.writeString(output.resolve(capture.name() + ".stdout.txt"),
                                        session.rawOutput(), StandardCharsets.UTF_8);
                }
            }
        }
    }

    private PtySession start(MockRuntime runtime, Capture capture) throws IOException {
        var environment = new HashMap<String, String>();
        // A PTY test clears the environment: the operator gate refuses a coding agent by design
        // (PM-SEC-3), and the harness is the only thing allowed to run the operator here.
        environment.put(MachinePaths.ENV_BASE, runtime.base().toString());
        environment.put("USER", "operator");
        environment.put("HOSTNAME", "capture-host");
        environment.put("LANG", capture.locale());
        environment.put("TERM", "xterm-256color");
        environment.put("PATH", "/usr/bin:/bin");
        environment.put("XDG_STATE_HOME", runtime.base().resolve("xdg-state").toString());
        environment.putAll(capture.environment());
        return PtySession.start(binary.with(capture.command().toArray(String[]::new)), environment,
                            Path.of("").toAbsolutePath(), capture.columns(), capture.rows());
    }

    /**
     * Waits until the process has written nothing new for the settle window and the screen can be
     * captured. A capture that types keys expects something to answer: a full-screen view enters the
     * alternate screen only after JLine's terminal probe is answered, and a quiet stretch before that
     * is not a drawn view, so the wait holds until the alternate screen is active. A line dialogue
     * never enters the alternate screen; it is recognised by the prompt suffix it printed, and the
     * quiet window decides once {@link #MIN_WAIT} has passed. A capture whose verb prints and types
     * nothing is settled by the quiet window alone.
     *
     * @param session     the running session
     * @param expectsView whether the capture types keys and therefore expects an answer
     * @return {@code true} when the screen settled before the deadline
     * @throws InterruptedException if interrupted
     */
    private static boolean awaitQuiet(PtySession session, boolean expectsView) throws InterruptedException {
        var start = System.nanoTime();
        var deadline = start + READY_TIMEOUT.toNanos();
        var lastLength = 0;
        var quietSince = 0L;
        while (System.nanoTime() < deadline) {
            var raw = session.rawOutput();
            var length = raw.length();
            if (length > 0 && length == lastLength) {
                if (quietSince == 0L) {
                    quietSince = System.nanoTime();
                } else if (System.nanoTime() - quietSince >= SETTLE.toNanos()
                                    && (!expectsView || settled(session, raw, start))) {
                    return true;
                }
            } else {
                lastLength = length;
                quietSince = 0L;
            }
            LockSupport.parkNanos(POLL.toNanos());
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        return false;
    }

    /**
     * @param session the running session
     * @param raw     the raw output so far
     * @param start   when the wait started, in {@link System#nanoTime()} terms
     * @return whether what the capture waits for has happened: a line dialogue answered by its
     *         prompt suffix and past {@link #MIN_WAIT}, or a full-screen view that entered the
     *         alternate screen
     */
    private static boolean settled(PtySession session, String raw, long start) {
        if (raw.contains(StartPrompt.SUFFIX)) {
            return System.nanoTime() - start >= MIN_WAIT.toNanos();
        }
        return session.screen().alternateScreenActive();
    }

    private static String attributes(AnsiScreen screen) {
        var text = new StringBuilder();
        for (int row = 0; row < screen.cellStyles().size(); row++) {
            text.append("row ").append(row + 1).append(':');
            var styles = screen.cellStyles().get(row);
            int start = 0;
            while (start < styles.size()) {
                var style = styles.get(start).text();
                int end = start;
                while (end + 1 < styles.size() && styles.get(end + 1).text().equals(style)) {
                    end++;
                }
                text.append(' ').append(start).append('-').append(end).append(' ').append(style);
                start = end + 1;
            }
            text.append('\n');
        }
        return text.toString();
    }

    /**
     * @param setName the capture set name
     * @return the captures of the set, in file order
     * @throws IOException if the set cannot be read
     */
    private static List<Capture> readSet(String setName) throws IOException {
        var resource = RESOURCE_DIR + "/" + setName + ".txt";
        try (var in = ScreenCaptureIT.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing capture set " + resource);
            }
            var captures = new ArrayList<Capture>();
            for (var line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                var trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                captures.add(parse(trimmed));
            }
            return captures;
        }
    }

    private static Capture parse(String line) {
        var fields = line.split("\\|", -1);
        if (fields.length != 8) {
            throw new IllegalArgumentException("A capture line names 8 fields, got " + fields.length + ": " + line);
        }
        var size = fields[5].strip().split("x");
        if (size.length != 2) {
            throw new IllegalArgumentException("A capture size is COLUMNSxROWS: " + fields[5].strip());
        }
        var command = new ArrayList<String>();
        for (var token : fields[3].strip().split("\\s+")) {
            if (!token.isEmpty()) {
                command.add(token);
            }
        }
        var states = new ArrayList<String>();
        for (var state : fields[2].strip().split(",")) {
            if (!state.isBlank() && !"-".equals(state.strip())) {
                states.add(state.strip());
            }
        }
        var environment = new HashMap<String, String>();
        for (var entry : fields[7].strip().split(",")) {
            var pair = entry.strip();
            if (pair.isEmpty() || "-".equals(pair)) {
                continue;
            }
            var split = pair.indexOf('=');
            if (split <= 0) {
                throw new IllegalArgumentException("A capture environment entry is KEY=VALUE: " + pair);
            }
            environment.put(pair.substring(0, split), pair.substring(split + 1));
        }
        return new Capture(fields[0].strip(), fields[1].strip(), List.copyOf(states), List.copyOf(command),
                            keys(fields[4].strip()), Integer.parseInt(size[0].strip()),
                            Integer.parseInt(size[1].strip()), fields[6].strip(), Map.copyOf(environment));
    }

    private static String keys(String raw) {
        if (raw.isEmpty() || "-".equals(raw)) {
            return "";
        }
        return raw.replace("\\n", "\n").replace("\\t", "\t");
    }
}
