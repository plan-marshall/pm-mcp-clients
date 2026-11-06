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
import java.util.concurrent.TimeUnit;

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

    @BeforeEach
    void setUp() throws IOException {
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

    /**
     * Starts a shell fixture that sets its trap, says so, and loops, and records it as the runtime in
     * {@code state/runtime.json} once its trap is set: a signal sent earlier would end the shell
     * untrapped.
     *
     * @param trap the trap command the fixture sets first
     * @return the fixture process
     */
    private Process recordedRuntime(String trap) throws IOException {
        var ready = tempDir.resolve("ready-" + children.size());
        var child = start(trap + "; : > " + ready + "; while :; do sleep 0.05; done");
        var deadline = System.nanoTime() + GENEROUS.toNanos();
        while (!Files.exists(ready)) {
            assertTrue(child.isAlive(), "the fixture ended before its trap was set");
            assertTrue(System.nanoTime() < deadline, "the fixture did not set its trap");
            Thread.onSpinWait();
        }
        recordRuntime(child);
        return child;
    }

    private Process start(String script) throws IOException {
        var child = new ProcessBuilder("/bin/sh", "-c", script).start();
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
        var witness = tempDir.resolve("signalled");
        recordedRuntime("trap 'echo " + marker + " > " + witness + "; exit 0' TERM");

        var code = drainWithin(GENEROUS);

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(Files.exists(witness), "the fixture never saw the signal"),
                            () -> assertEquals(marker, Files.readString(witness).strip()));
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
