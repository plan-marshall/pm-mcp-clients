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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.mock.StandardScenarios;

@DisplayName("MockRuntimeCli")
class MockRuntimeCliTest {

    private ByteArrayOutputStream outBytes;
    private ByteArrayOutputStream errBytes;
    private PrintStream out;
    private PrintStream err;

    @BeforeEach
    void setUp() {
        outBytes = new ByteArrayOutputStream();
        errBytes = new ByteArrayOutputStream();
        out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
    }

    private String stdout() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("--help prints usage and exits with 0")
    void helpFlag() {
        var result = MockRuntimeCli.parse(new String[]{"--help"}, out, err);
        assertEquals(0, result.exitCode());
        assertNull(result.config());
        assertFalse(result.isSuccess());
        assertTrue(stdout().contains("Usage: pm-mock-runtime"));
    }

    @Test
    @DisplayName("-h prints usage and exits with 0")
    void shortHelpFlag() {
        var result = MockRuntimeCli.parse(new String[]{"-h"}, out, err);
        assertEquals(0, result.exitCode());
        assertNull(result.config());
        assertTrue(stdout().contains("Usage: pm-mock-runtime"));
    }

    @Test
    @DisplayName("parses default options without arguments, defaulting webPort to 7420")
    void defaultOptions() {
        var result = MockRuntimeCli.parse(new String[0], out, err, Map.of());
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        var config = result.config();
        assertNotNull(config);
        assertEquals(StandardScenarios.STATUS_ONLY, config.getScenario());
        assertNull(config.getBase());
        assertEquals(7420, config.getWebPort());
    }

    @Test
    @DisplayName("falls back to PM_MOCK_WEB_PORT when CLI argument is not specified")
    void webPortEnvFallback() {
        var result = MockRuntimeCli.parse(new String[0], out, err, Map.of("PM_MOCK_WEB_PORT", "9099"));
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        assertEquals(9099, result.config().getWebPort());
    }

    @Test
    @DisplayName("CLI --web-port overrides PM_MOCK_WEB_PORT")
    void cliPortOverridesEnv() {
        var result = MockRuntimeCli.parse(new String[]{"--web-port", "8088"}, out, err, Map.of("PM_MOCK_WEB_PORT", "9099"));
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        assertEquals(8088, result.config().getWebPort());
    }

    @Test
    @DisplayName("CLI --web-port -1 explicitly disables web listener")
    void cliPortDisabledExplicit() {
        var result = MockRuntimeCli.parse(new String[]{"--web-port", "-1"}, out, err, Map.of());
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        assertEquals(-1, result.config().getWebPort());
    }

    @Test
    @DisplayName("parses customized CLI arguments")
    void customOptions() {
        var args = new String[]{
                "--scenario", StandardScenarios.ONBOARDING_HAPPY,
                "--base", "/tmp/pm-test-base",
                "--web-port", "8088",
                "--token", "secret-token-123"
        };
        var result = MockRuntimeCli.parse(args, out, err);
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        var config = result.config();
        assertNotNull(config);
        assertEquals(StandardScenarios.ONBOARDING_HAPPY, config.getScenario());
        assertEquals(Path.of("/tmp/pm-test-base"), config.getBase());
        assertEquals(8088, config.getWebPort());
        assertEquals("secret-token-123", config.effectiveRuntimeToken());
    }

    @Test
    @DisplayName("reports error on missing argument for options")
    void missingOptionArgs() {
        assertEquals(2, MockRuntimeCli.parse(new String[]{"-s"}, out, err).exitCode());
        assertEquals(2, MockRuntimeCli.parse(new String[]{"--base"}, out, err).exitCode());
        assertEquals(2, MockRuntimeCli.parse(new String[]{"-b"}, out, err).exitCode());
        assertEquals(2, MockRuntimeCli.parse(new String[]{"--web-port"}, out, err).exitCode());
        assertEquals(2, MockRuntimeCli.parse(new String[]{"-p"}, out, err).exitCode());
        assertEquals(2, MockRuntimeCli.parse(new String[]{"--token"}, out, err).exitCode());
        assertEquals(2, MockRuntimeCli.parse(new String[]{"-t"}, out, err).exitCode());
        assertEquals(2, MockRuntimeCli.parse(new String[]{"--device-secret"}, out, err).exitCode());
        assertEquals(2, MockRuntimeCli.parse(new String[]{"-d"}, out, err).exitCode());
    }

    @Test
    @DisplayName("parses short flags correctly")
    void shortFlags() {
        var args = new String[]{
                "-s", StandardScenarios.WEB_PAIRING,
                "-b", "/tmp/base-short",
                "-p", "9999",
                "-t", "tok",
                "-d", "pmw_explicit"
        };
        var result = MockRuntimeCli.parse(args, out, err);
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        assertEquals(StandardScenarios.WEB_PAIRING, result.config().getScenario());
        assertEquals(Path.of("/tmp/base-short"), result.config().getBase());
        assertEquals(9999, result.config().getWebPort());
        assertEquals("tok", result.config().effectiveRuntimeToken());
        assertEquals("pmw_explicit", result.config().effectiveDeviceSecret());
    }

    @Test
    @DisplayName("run returns exit code of failed parse")
    void runParseFailure() {
        var exitCode = MockRuntimeCli.run(new String[]{"--unknown"}, out, err);
        assertEquals(2, exitCode);
    }

    @Test
    @DisplayName("run returns 1 on startup failure (e.g. non-existent scenario)")
    void runStartupFailure() {
        var exitCode = MockRuntimeCli.run(new String[]{"-s", "non-existent-scenario-file-1234"}, out, err);
        assertEquals(1, exitCode);
        assertTrue(stderr().contains("Error starting mock runtime"));
    }

    @Test
    @DisplayName("run returns 0 on --help")
    void runHelp() {
        var exitCode = MockRuntimeCli.run(new String[]{"--help"}, out, err);
        assertEquals(0, exitCode);
        assertTrue(stdout().contains("Usage: pm-mock-runtime"));
    }

    @Test
    @DisplayName("reports invalid port error")
    void invalidPort() {
        var result = MockRuntimeCli.parse(new String[]{"--web-port", "abc"}, out, err);
        assertEquals(2, result.exitCode());
        assertTrue(stderr().contains("invalid port"));
    }

    @Test
    @DisplayName("starts runtime and exits cleanly when interrupted, printing pairing code and web URL")
    void runInterrupted() {
        var currentThread = Thread.currentThread();
        Thread.ofVirtual().start(() -> {
            LockSupport.parkNanos(100_000_000L);
            currentThread.interrupt();
        });
        var exitCode = MockRuntimeCli.run(new String[]{"-s", StandardScenarios.STATUS_ONLY, "-p", "0"}, out, err);
        assertEquals(0, exitCode);
        assertTrue(stdout().contains("Mock runtime started successfully"));
        assertTrue(stdout().contains("Pairing Code:       KXQB-MRTN"));
        // The development URL names the machine, not localhost, so a phone can reach it.
        assertTrue(
                Pattern.compile("Web App URL:\\s+http://[^/\\s]+:5173/pair#code=KXQB-MRTN")
                        .matcher(stdout())
                        .find(),
                () -> "Web App URL with the machine host expected, got: " + stdout());
    }

    @Test
    @DisplayName("omits pairing code and web app URL when loopback web port is disabled")
    void runWithoutWebPort() {
        var currentThread = Thread.currentThread();
        Thread.ofVirtual().start(() -> {
            LockSupport.parkNanos(100_000_000L);
            currentThread.interrupt();
        });
        var exitCode = MockRuntimeCli.run(new String[]{"-s", StandardScenarios.RUNNING_WORK, "--web-port", "-1"}, out, err);
        assertEquals(0, exitCode);
        assertTrue(stdout().contains("Mock runtime started successfully"));
        assertFalse(stdout().contains("Pairing Code:"));
        assertFalse(stdout().contains("Web App URL:"));
    }
}
