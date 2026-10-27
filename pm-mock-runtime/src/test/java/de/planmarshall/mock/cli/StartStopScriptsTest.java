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

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;


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

    private Path mockRuntimeDir() {
        if (Files.exists(Path.of("start.sh"))) {
            return Path.of(".").toAbsolutePath().normalize();
        }
        return Path.of("pm-mock-runtime").toAbsolutePath().normalize();
    }

    private Path startScript() {
        return mockRuntimeDir().resolve("start.sh");
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
            pb.directory(mockRuntimeDir().toFile());
            var process = pb.start();
            var exited = process.waitFor(60, TimeUnit.SECONDS);
            assertTrue(exited, "start.sh should exit within 60 seconds");
            var exitCode = process.exitValue();
            assertNotEquals(0, exitCode, "start.sh must fail when assigned port is in use");

            var stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            var combinedOutput = stdout + "\n" + stderr;

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
            pb.directory(mockRuntimeDir().toFile());
            pb.environment().put("PM_MOCK_WEB_PORT", String.valueOf(occupiedPort));
            var process = pb.start();
            var exited = process.waitFor(60, TimeUnit.SECONDS);
            assertTrue(exited, "start.sh should exit within 60 seconds");
            var exitCode = process.exitValue();
            assertNotEquals(0, exitCode, "start.sh must fail when assigned env port is in use");

            var stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            var combinedOutput = stdout + "\n" + stderr;

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

        // Start a mock runtime process simulating worktree A
        var cpFile = mockRuntimeDir().resolve("target/classpath.txt");
        var cp = (Files.exists(cpFile) ? Files.readString(cpFile).trim() + ":" : "")
                + mockRuntimeDir().resolve("target/classes") + ":"
                + mockRuntimeDir().getParent().resolve("pm-api/target/classes");
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
