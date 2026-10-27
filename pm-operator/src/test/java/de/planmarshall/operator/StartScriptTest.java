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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code pm-operator/start.sh} as a shell contract (CLI-CC-P-08): a foreground run writes no PID
 * file, so it never blocks another foreground run, while {@code -d} writes it so that
 * {@code stop.sh} can track the background process.
 *
 * <p>The script is copied into a throw-away tree whose build stamp is fresh and whose {@code java}
 * is a stub, so the test exercises the script's own decisions — the "already running" guard, the
 * PID file, the background start — without building the operator or starting it.
 */
@DisplayName("pm-operator/start.sh")
@EnabledOnOs({OS.MAC, OS.LINUX})
class StartScriptTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @TempDir
    Path temp;

    private Path script;
    private Path markerDirectory;

    @BeforeEach
    void copyScript() throws IOException {
        var module = temp.resolve("pm-operator");
        Files.createDirectories(module.resolve("target/classes"));
        var scriptPath = module.resolve("start.sh");
        Files.copy(Path.of("start.sh"), scriptPath);
        // A fresh build stamp and an existing classpath file keep the script from running Maven.
        Files.writeString(module.resolve("target/classpath.txt"), "");
        Files.writeString(module.resolve("target/.build-stamp"), "");
        script = scriptPath;

        markerDirectory = temp.resolve("markers");
        Files.createDirectories(markerDirectory);
        var bin = temp.resolve("bin");
        Files.createDirectories(bin);
        var java = bin.resolve("java");
        Files.writeString(java, "#!/bin/sh\nprintf '%s\\n' \"$*\" > \"$PM_TEST_MARKER\"\nsleep 2\n");
        Files.setPosixFilePermissions(java, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    @Test
    @DisplayName("two foreground runs at the same time both start")
    void twoForegroundRunsBothStart() throws Exception {
        var first = spawn("first", "--help");
        awaitMarker("first");
        var second = spawn("second", "--help");

        var firstRun = await(first);
        var secondRun = await(second);

        assertAll(
                            () -> assertFalse(firstRun.contains("already running"), firstRun),
                            () -> assertFalse(secondRun.contains("already running"), secondRun),
                            () -> assertTrue(Files.exists(marker("first")), "the first run reached java"),
                            () -> assertTrue(Files.exists(marker("second")), "the second run reached java"),
                            () -> assertFalse(Files.exists(pidFile()),
                                                "a foreground run writes no PID file")
        );
    }

    @Test
    @DisplayName("a background run writes the PID file stop.sh tracks")
    void backgroundRunWritesPidFile() throws Exception {
        var output = await(spawn("daemon", "-d", "--help"));

        assertAll(
                            () -> assertTrue(output.contains("pm-operator started in background"),
                                                output),
                            () -> assertTrue(Files.exists(pidFile()), "a background run writes the PID file"),
                            () -> assertTrue(Files.readString(pidFile()).strip().matches("\\d+"),
                                                Files.readString(pidFile()))
        );
        ProcessHandle.of(Long.parseLong(Files.readString(pidFile()).strip()))
                            .filter(handle -> handle.info().commandLine()
                                                .map(line -> line.contains("PmOperator")).orElse(false))
                            .ifPresent(ProcessHandle::destroy);
    }

    /**
     * @param marker     the name of the run's marker file
     * @param arguments  the arguments after the script
     * @return the running process
     * @throws IOException if the script cannot be started
     */
    private Process spawn(String marker, String... arguments) throws IOException {
        var command = new ArrayList<String>();
        command.add("bash");
        command.add(script.toString());
        command.addAll(List.of(arguments));
        var builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        var environment = builder.environment();
        environment.clear();
        environment.putAll(environment(marker));
        return builder.start();
    }

    /**
     * @param process the running script
     * @return everything the script printed
     * @throws IOException          if the output cannot be read
     * @throws InterruptedException if interrupted while waiting
     */
    private static String await(Process process) throws IOException, InterruptedException {
        var output = new String(process.getInputStream().readAllBytes(), UTF_8);
        assertTrue(process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS), "start.sh did not finish");
        assertEquals(0, process.exitValue(), output);
        return output;
    }

    /**
     * @param marker the name of the run's marker file
     * @throws InterruptedException if interrupted while waiting
     */
    private void awaitMarker(String marker) throws InterruptedException {
        var deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!Files.exists(marker(marker))) {
            assertTrue(System.nanoTime() < deadline, "the first run never reached java");
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20L));
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    private Map<String, String> environment(String marker) {
        var environment = new HashMap<String, String>();
        environment.put("PM_TEST_MARKER", marker(marker).toString());
        // The mock runtime is never detected: no PM_MCP_BASE means the script keeps its own.
        environment.put("HOME", temp.resolve("home").toString());
        environment.put("PATH", temp.resolve("bin") + ":/usr/bin:/bin");
        return environment;
    }

    private Path marker(String name) {
        return markerDirectory.resolve(name + ".txt");
    }

    private Path pidFile() {
        return script.getParent().resolve("target/pm-operator.pid");
    }
}
