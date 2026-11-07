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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

/**
 * {@code pm-operator drain} is the signal of the specification (01-cli-and-installer.adoc#cli-without-runtime):
 * it signals the process recorded in {@code state/runtime.json}, waits at most its bound for the
 * exit, does nothing without a record, and sends no request to the runtime's API.
 */
@DisplayName("DrainCommand")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class DrainCommandTest {

    /** A bound the fixture's exit stays far inside, so a slow machine does not reach it. */
    private static final Duration GENEROUS = Duration.ofSeconds(30);

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private final List<Process> children = new ArrayList<>();

    /** Where the fixtures write their ready and witness files; a test may move it below a directory with a space. */
    private Path workDir;

    @BeforeEach
    void setUp() throws IOException {
        workDir = tempDir;
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws Exception {
        for (var child : children) {
            child.destroyForcibly();
            child.waitFor(5, TimeUnit.SECONDS);
        }
        if (fixture != null) {
            fixture.close();
        }
    }

    private OperatorContext context() {
        return new OperatorContext(fixture.environment(), tempDir, true);
    }

    private int execute(String... args) {
        return PmOperator.execute(context(), new PrintWriter(out), new PrintWriter(err), args);
    }

    private int drainWithin(Duration bound) {
        var commandLine = new CommandLine(new DrainCommand(context(), bound));
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        return commandLine.execute();
    }

    private int drainWith(LongFunction<Optional<ProcessHandle>> processes) {
        var commandLine = new CommandLine(new DrainCommand(context(), GENEROUS, processes));
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        return commandLine.execute();
    }

    /**
     * Starts a shell fixture that sets its trap, says so, and loops, and records it as the runtime in
     * {@code state/runtime.json} once its trap is set: a signal sent earlier would end the shell
     * untrapped. The ready file is the script's {@code "$1"} and the witness its {@code "$2"}, passed as
     * arguments and quoted where they expand, so a path with a space names the right file.
     *
     * @param trap the trap command the fixture sets first; it names the witness as {@code "$2"}
     * @return the fixture process
     */
    private Process recordedRuntime(String trap) throws IOException {
        return recordedRuntime(trap, workDir.resolve("witness-" + children.size()));
    }

    private Process recordedRuntime(String trap, Path witness) throws IOException {
        var ready = workDir.resolve("ready-" + children.size());
        var child = start(trap + "; : > \"$1\"; while :; do sleep 0.05; done", ready.toString(), witness.toString());
        var deadline = System.nanoTime() + GENEROUS.toNanos();
        while (!Files.exists(ready)) {
            assertTrue(child.isAlive(), "the fixture ended before its trap was set");
            assertTrue(System.nanoTime() < deadline, "the fixture did not set its trap");
            Thread.onSpinWait();
        }
        recordRuntime(child);
        return child;
    }

    private Process start(String script, String... arguments) throws IOException {
        var command = new ArrayList<>(List.of("/bin/sh", "-c", script, "drain-fixture"));
        command.addAll(List.of(arguments));
        var child = new ProcessBuilder(command).start();
        children.add(child);
        return child;
    }

    private void recordRuntime(Process child) throws IOException {
        var handle = child.toHandle();
        var start = handle.info().startInstant();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + handle.pid()
                            + start.map(instant -> ",\"start_instant\":\"" + instant + "\"").orElse("") + "}");
    }

    @Test
    @DisplayName("the signal reaches the recorded process")
    void signalReachesTheProcess() throws Exception {
        var marker = Generators.letterStrings(8, 16).next();
        var witness = workDir.resolve("signalled");
        recordedRuntime("trap 'echo " + marker + " > \"$2\"; exit 0' TERM", witness);

        var code = drainWithin(GENEROUS);

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(Files.exists(witness), "the fixture never saw the signal"),
                            () -> assertEquals(marker, Files.readString(witness).strip()));
    }

    @Test
    @DisplayName("the fixture paths hold in a directory whose name has a space")
    void signalReachesTheProcessFromADirectoryWithASpace() throws Exception {
        workDir = Files.createDirectories(tempDir.resolve("drain " + Generators.letterStrings(3, 8).next()));
        var marker = Generators.letterStrings(8, 16).next();
        var witness = workDir.resolve("signalled");
        recordedRuntime("trap 'echo " + marker + " > \"$2\"; exit 0' TERM", witness);

        var code = drainWithin(GENEROUS);

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(Files.exists(witness), "the fixture never saw the signal"),
                            () -> assertEquals(marker, Files.readString(witness).strip()));
    }

    @Test
    @DisplayName("a runtime that exited before the signal is reported not running with exit 0")
    void exitedBeforeTheSignalIsNotRunning() throws Exception {
        var child = recordedRuntime("trap 'exit 0' TERM");

        var code = drainWith(_ -> Optional.of(new FailedSignal(child.pid(), false)));

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertEquals("The runtime is not running; there is nothing to drain.",
                                                out.toString().strip()),
                            () -> assertFalse(err.toString().contains("could not be signalled"), err.toString()));
    }

    @Test
    @DisplayName("a live runtime the signal does not reach is reported with exit 1")
    void liveRuntimeTheSignalMissesFails() throws Exception {
        var child = recordedRuntime("trap 'exit 0' TERM");

        var code = drainWith(_ -> Optional.of(new FailedSignal(child.pid(), true)));

        assertAll(
                            () -> assertEquals(PmOperator.EXIT_FAILURE, code),
                            () -> assertTrue(err.toString().contains(
                                                                    "The runtime (PID " + child.pid() + ") could not be signalled."),
                                                err.toString()));
    }

    /**
     * A process whose signal fails: {@code destroy()} answers {@code false}, and the process is still
     * alive or has exited, as the test needs.
     *
     * @param pid   the process id it reports
     * @param alive whether it is still alive after the failed signal
     */
    private record FailedSignal(long pid, boolean alive) implements ProcessHandle {

        @Override
        public Optional<ProcessHandle> parent() {
            return Optional.empty();
        }

        @Override
        public Stream<ProcessHandle> children() {
            return Stream.empty();
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return Stream.empty();
        }

        @Override
        public Info info() {
            return ProcessHandle.current().info();
        }

        @Override
        public CompletableFuture<ProcessHandle> onExit() {
            return new CompletableFuture<>();
        }

        @Override
        public boolean supportsNormalTermination() {
            return true;
        }

        @Override
        public boolean destroy() {
            return false;
        }

        @Override
        public boolean destroyForcibly() {
            return false;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public int compareTo(ProcessHandle other) {
            return Long.compare(pid, other.pid());
        }
    }

    @Test
    @DisplayName("the command returns once the process exits")
    void returnsOnceTheProcessExits() throws Exception {
        var child = recordedRuntime("trap 'exit 0' TERM");

        var code = drainWithin(GENEROUS);

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertFalse(child.isAlive(), "the command returned before the exit"),
                            () -> assertTrue(out.toString().contains("The runtime has drained and stopped."),
                                                out.toString()),
                            () -> assertFalse(out.toString().contains("still draining"), out.toString()));
    }

    @Test
    @DisplayName("the command returns at the bound when the process does not exit")
    void returnsAtTheBound() throws Exception {
        var child = recordedRuntime("trap '' TERM");
        var bound = Duration.ofMillis(Generators.integers(200, 600).next());

        var started = System.nanoTime();
        var code = drainWithin(bound);
        var waited = Duration.ofNanos(System.nanoTime() - started);

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(child.isAlive(), "the fixture ignores the signal and runs on"),
                            () -> assertTrue(waited.compareTo(bound) >= 0, "returned before the bound: " + waited),
                            () -> assertTrue(waited.compareTo(GENEROUS) < 0, "waited past the bound: " + waited),
                            () -> assertTrue(out.toString().contains(
                                                "The runtime is still draining (PID " + child.pid() + ")."), out.toString()));
    }

    @Test
    @DisplayName("no record means no signal and exit 0")
    void noRecordNoSignal() {
        var code = execute("drain");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertEquals("The runtime is not running; there is nothing to drain.",
                                                out.toString().strip()));
    }

    @Test
    @DisplayName("a record whose process is gone means no signal and exit 0")
    void deadRecordNoSignal() throws Exception {
        var child = start("exit 0");
        assertTrue(child.waitFor(GENEROUS.toSeconds(), TimeUnit.SECONDS), "the fixture did not end");
        recordRuntime(child);

        var code = execute("drain");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertEquals("The runtime is not running; there is nothing to drain.",
                                                out.toString().strip()));
    }

    @Test
    @DisplayName("no HTTP request reaches the runtime")
    void noHttpRequest() throws Exception {
        var server = fixture.start((request, response) -> response.send(200, null, ""));
        var child = recordedRuntime("trap 'exit 0' TERM");

        var code = execute("drain");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertFalse(child.isAlive(), "the drain did not end the fixture"),
                            () -> assertEquals(0, server.requests().size(), "requests: " + server.requests()));
    }

    @Test
    @DisplayName("drain --withdraw is a usage error")
    void withdrawIsAUsageError() throws Exception {
        var server = fixture.start((request, response) -> response.send(200, null, ""));

        var code = execute("drain", "--withdraw");

        assertAll(
                            () -> assertEquals(2, code),
                            () -> assertTrue(err.toString().contains("--withdraw"), err.toString()),
                            () -> assertEquals(0, server.requests().size(), "requests: " + server.requests()));
    }
}
