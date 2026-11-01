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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.mock.StandardScenarios;

@DisplayName("MockRuntimeCli")
@EnableGeneratorController
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
        var envPort = Generators.integers(7421, 65535).next();
        var result = MockRuntimeCli.parse(new String[0], out, err, Map.of("PM_MOCK_WEB_PORT", String.valueOf(envPort)));
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        assertEquals(envPort.intValue(), result.config().getWebPort());
    }

    @Test
    @DisplayName("reports invalid port error in PM_MOCK_WEB_PORT")
    void invalidEnvPort() {
        var result = MockRuntimeCli.parse(new String[0], out, err, Map.of("PM_MOCK_WEB_PORT", "invalid"));
        assertEquals(2, result.exitCode());
        assertTrue(stderr().contains("invalid port in PM_MOCK_WEB_PORT: invalid"));

        var outOfRange = MockRuntimeCli.parse(new String[0], out, err, Map.of("PM_MOCK_WEB_PORT", "70000"));
        assertEquals(2, outOfRange.exitCode());
    }

    @Test
    @DisplayName("reports invalid port error when CLI --web-port is out of range")
    void outOfRangeCliPort() {
        var result = MockRuntimeCli.parse(new String[]{"--web-port", "70000"}, out, err, Map.of());
        assertEquals(2, result.exitCode());
        assertTrue(stderr().contains("invalid port: 70000"));

        var belowMin = MockRuntimeCli.parse(new String[]{"--web-port", "-2"}, out, err, Map.of());
        assertEquals(2, belowMin.exitCode());
        assertTrue(stderr().contains("invalid port: -2"));
    }

    @Test
    @DisplayName("falls back to default 7420 when env is null or PM_MOCK_WEB_PORT is blank")
    void nullOrBlankEnvFallback() {
        var nullEnv = MockRuntimeCli.parse(new String[0], out, err, null);
        assertEquals(7420, nullEnv.config().getWebPort());

        var blankEnv = MockRuntimeCli.parse(new String[0], out, err, Map.of("PM_MOCK_WEB_PORT", "   "));
        assertEquals(7420, blankEnv.config().getWebPort());
    }

    @Test
    @DisplayName("CLI --web-port overrides PM_MOCK_WEB_PORT")
    void cliPortOverridesEnv() {
        var cliPort = Generators.integers(1024, 30000).next();
        var envPort = Generators.integers(30001, 65535).next();
        var result = MockRuntimeCli.parse(new String[]{"--web-port", String.valueOf(cliPort)}, out, err,
                Map.of("PM_MOCK_WEB_PORT", String.valueOf(envPort)));
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        assertEquals(cliPort.intValue(), result.config().getWebPort());
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
        var base = "/tmp/" + Generators.letterStrings(4, 10).next().toLowerCase();
        var port = Generators.integers(1024, 65535).next();
        var token = Generators.letterStrings(8, 20).next();
        var args = new String[]{
                "--scenario", StandardScenarios.ONBOARDING_HAPPY,
                "--base", base,
                "--web-port", String.valueOf(port),
                "--token", token
        };
        var result = MockRuntimeCli.parse(args, out, err);
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        var config = result.config();
        assertNotNull(config);
        assertEquals(StandardScenarios.ONBOARDING_HAPPY, config.getScenario());
        assertEquals(Path.of(base), config.getBase());
        assertEquals(port.intValue(), config.getWebPort());
        assertEquals(token, config.effectiveRuntimeToken());
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
        var base = "/tmp/" + Generators.letterStrings(4, 10).next().toLowerCase();
        var port = Generators.integers(7421, 65535).next();
        var token = Generators.letterStrings(4, 12).next();
        var secret = "pmw_" + Generators.letterStrings(4, 12).next();
        var args = new String[]{
                "-s", StandardScenarios.WEB_PAIRING,
                "-b", base,
                "-p", String.valueOf(port),
                "-t", token,
                "-d", secret
        };
        var result = MockRuntimeCli.parse(args, out, err);
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        assertEquals(StandardScenarios.WEB_PAIRING, result.config().getScenario());
        assertEquals(Path.of(base), result.config().getBase());
        assertEquals(port.intValue(), result.config().getWebPort());
        assertEquals(token, result.config().effectiveRuntimeToken());
        assertEquals(secret, result.config().effectiveDeviceSecret());
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
        var missingScenario = "non-existent-" + Generators.letterStrings(4, 10).next().toLowerCase();
        var exitCode = MockRuntimeCli.run(new String[]{"-s", missingScenario}, out, err);
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
