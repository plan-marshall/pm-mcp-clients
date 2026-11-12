/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Shell tests for {@code start.sh} and {@code stop.sh} enforcing worktree isolation and port assignment.
 */
@DisplayName("start.sh and stop.sh shell tests")
@EnabledOnOs({OS.MAC, OS.LINUX})
class StartStopScriptsTest {

    /** The line start.sh prints before it compiles the module again with a nested Maven build. */
    private static final String NESTED_BUILD_LINE = "Building pm-mock-runtime";

    @TempDir
    Path temp;

    /** The copy of start.sh in a throw-away module tree that the script treats as built. */
    private Path startScript;

    /**
     * Copies {@code start.sh} into a throw-away module tree whose classpath file and build stamp
     * say "up to date" and that holds no class file newer than the stamp. Run from the real module
     * inside the reactor, the script finds the classes the reactor has just compiled newer than its
     * stamp, deletes {@code target/classes} of the module and of pm-api and compiles both again with
     * a nested {@code ./mvnw}; the copy keeps it from building, so the tests exercise the port checks
     * they are about.
     *
     * @throws IOException if the tree cannot be written
     */
    @BeforeEach
    void copyStartScript() throws IOException {
        var module = temp.resolve("pm-mock-runtime");
        Files.createDirectories(module.resolve("target/classes"));
        startScript = module.resolve("start.sh");
        Files.copy(mockRuntimeDir().resolve("start.sh"), startScript);
        Files.writeString(module.resolve("target/classpath.txt"), "");
        Files.writeString(module.resolve("target/.build-stamp"), "");
    }

    private Path mockRuntimeDir() {
        if (Files.exists(Path.of("start.sh"))) {
            return Path.of(".").toAbsolutePath().normalize();
        }
        return Path.of("pm-mock-runtime").toAbsolutePath().normalize();
    }

    private Path startScript() {
        return startScript;
    }

    private Path stopScript() {
        return mockRuntimeDir().resolve("stop.sh");
    }

    @Test
    @DisplayName("start.sh with assigned port (-p) held by another process fails, names holder and leaves it running")
    void startWithOccupiedAssignedPortFlagFails() throws Exception {
        try (var socket = new ServerSocket(0)) {
            var occupiedPort = socket.getLocalPort();
            var pb = new ProcessBuilder(
                    "bash", startScript().toString(),
                    "-p", String.valueOf(occupiedPort),
                    "-s", "status-only"
            );
            pb.directory(startScript().getParent().toFile());
            pb.redirectErrorStream(true);
            var process = pb.start();
            var outputFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException _) {
                    return "";
                }
            });
            var exited = process.waitFor(60, TimeUnit.SECONDS);
            assertTrue(exited, "start.sh should exit within 60 seconds");
            var exitCode = process.exitValue();
            assertNotEquals(0, exitCode, "start.sh must fail when assigned port is in use");

            var combinedOutput = outputFuture.get(5, TimeUnit.SECONDS);

            assertFalse(combinedOutput.contains(NESTED_BUILD_LINE),
                    "start.sh must not run its own Maven build; got:\n" + combinedOutput);
            assertTrue(combinedOutput.contains(String.valueOf(occupiedPort)),
                    "Output must name the occupied port " + occupiedPort + "; got:\n" + combinedOutput);
            var currentPid = String.valueOf(ProcessHandle.current().pid());
            assertTrue(combinedOutput.contains(currentPid),
                    "Output must name the holder process PID " + currentPid + "; got:\n" + combinedOutput);

            assertFalse(socket.isClosed(), "Holder socket must remain open and process left running");
        }
    }

    @Test
    @DisplayName("start.sh with assigned port (PM_MOCK_WEB_PORT) held by another process fails, names holder and leaves it running")
    void startWithOccupiedAssignedEnvPortFails() throws Exception {
        try (var socket = new ServerSocket(0)) {
            var occupiedPort = socket.getLocalPort();
            var pb = new ProcessBuilder(
                    "bash", startScript().toString(),
                    "-s", "status-only"
            );
            pb.directory(startScript().getParent().toFile());
            pb.environment().put("PM_MOCK_WEB_PORT", String.valueOf(occupiedPort));
            pb.redirectErrorStream(true);
            var process = pb.start();
            var outputFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException _) {
                    return "";
                }
            });
            var exited = process.waitFor(60, TimeUnit.SECONDS);
            assertTrue(exited, "start.sh should exit within 60 seconds");
            var exitCode = process.exitValue();
            assertNotEquals(0, exitCode, "start.sh must fail when assigned env port is in use");

            var combinedOutput = outputFuture.get(5, TimeUnit.SECONDS);

            assertFalse(combinedOutput.contains(NESTED_BUILD_LINE),
                    "start.sh must not run its own Maven build; got:\n" + combinedOutput);
            assertTrue(combinedOutput.contains(String.valueOf(occupiedPort)),
                    "Output must name the occupied port " + occupiedPort + "; got:\n" + combinedOutput);
            var currentPid = String.valueOf(ProcessHandle.current().pid());
            assertTrue(combinedOutput.contains(currentPid),
                    "Output must name the holder process PID " + currentPid + "; got:\n" + combinedOutput);

            assertFalse(socket.isClosed(), "Holder socket must remain open and process left running");
        }
    }

    @Test
    @DisplayName("stop.sh in one worktree leaves a mock started from another worktree running")
    void stopInOneWorktreeLeavesOtherWorktreeMockRunning(@TempDir Path tempDir) throws Exception {
        var worktreeA = tempDir.resolve("worktreeA");
        var worktreeB = tempDir.resolve("worktreeB");
        Files.createDirectories(worktreeA.resolve("target"));
        Files.createDirectories(worktreeB.resolve("target"));

        // Copy stop.sh into both simulated worktree directories
        var stopScriptA = worktreeA.resolve("stop.sh");
        var stopScriptB = worktreeB.resolve("stop.sh");
        Files.copy(stopScript(), stopScriptA);
        Files.copy(stopScript(), stopScriptB);
        stopScriptA.toFile().setExecutable(true);
        stopScriptB.toFile().setExecutable(true);

        // Find an open port for worktree A's mock
        int portA;
        try (var s = new ServerSocket(0)) {
            portA = s.getLocalPort();
        }

        // Start a mock runtime process simulating worktree A, on the classpath this test runs with:
        // it holds the module, pm-api and every dependency without a classpath file of start.sh.
        var cp = System.getProperty("java.class.path");
        var pbA = new ProcessBuilder(
                "java", "-cp", cp,
                "de.planmarshall.mock.cli.MockRuntimeCli",
                "--scenario", "status-only",
                "--web-port", String.valueOf(portA)
        );
        pbA.directory(worktreeA.toFile());
        var processA = pbA.start();
        try {
            // Write worktree A's PID file and log file
            Files.writeString(worktreeA.resolve("target/pm-mock-runtime.pid"), String.valueOf(processA.pid()));
            Files.writeString(worktreeA.resolve("target/pm-mock-runtime.log"), "Loopback Web Port:  " + portA + "\n");

            // Wait until processA is alive and listening
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline && processA.isAlive()) {
                try {
                    new Socket("127.0.0.1", portA).close();
                    break;
                } catch (Exception _) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
                }
            }
            if (!processA.isAlive()) {
                var errOutput = new String(processA.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                var stdOutput = new String(processA.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new AssertionError("processA exited early (code " + processA.exitValue() + "):\nSTDOUT: " + stdOutput + "\nSTDERR: " + errOutput);
            }

            // In worktree B, log points to portA or 7420, but worktree B has NO PID file (or its own PID)
            Files.writeString(worktreeB.resolve("target/pm-mock-runtime.log"), "Loopback Web Port:  " + portA + "\n");

            // Execute stop.sh in worktree B
            var pbStopB = new ProcessBuilder("bash", stopScriptB.toString());
            pbStopB.directory(worktreeB.toFile());
            var stopB = pbStopB.start();
            assertTrue(stopB.waitFor(10, TimeUnit.SECONDS), "stop.sh in worktree B should finish quickly");

            // Verify that processA in worktree A is STILL running!
            assertTrue(processA.isAlive(), "Mock in worktree A must NOT be stopped by stop.sh in worktree B");
        } finally {
            processA.destroy();
            processA.waitFor(5, TimeUnit.SECONDS);
            if (processA.isAlive()) {
                processA.destroyForcibly();
            }
        }
    }
}
