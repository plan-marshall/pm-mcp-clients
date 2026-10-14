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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("parses default options without arguments")
    void defaultOptions() {
        var result = MockRuntimeCli.parse(new String[0], out, err);
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        var config = result.config();
        assertNotNull(config);
        assertEquals("status-only", config.getScenario());
        assertNull(config.getBase());
        assertEquals(-1, config.getWebPort());
    }

    @Test
    @DisplayName("parses customized CLI arguments")
    void customOptions() {
        var args = new String[]{
                "--scenario", "onboarding-happy",
                "--base", "/tmp/pm-test-base",
                "--web-port", "8088",
                "--token", "secret-token-123"
        };
        var result = MockRuntimeCli.parse(args, out, err);
        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        var config = result.config();
        assertNotNull(config);
        assertEquals("onboarding-happy", config.getScenario());
        assertEquals(Path.of("/tmp/pm-test-base"), config.getBase());
        assertEquals(8088, config.getWebPort());
        assertEquals("secret-token-123", config.effectiveRuntimeToken());
    }

    @Test
    @DisplayName("reports error on missing argument for --scenario")
    void missingScenarioArg() {
        var result = MockRuntimeCli.parse(new String[]{"--scenario"}, out, err);
        assertEquals(2, result.exitCode());
        assertFalse(result.isSuccess());
        assertTrue(stderr().contains("missing argument"));
    }

    @Test
    @DisplayName("reports error on invalid web port")
    void invalidWebPort() {
        var result = MockRuntimeCli.parse(new String[]{"--web-port", "not-a-number"}, out, err);
        assertEquals(2, result.exitCode());
        assertFalse(result.isSuccess());
        assertTrue(stderr().contains("invalid port"));
    }

    @Test
    @DisplayName("reports error on unrecognized option")
    void unrecognizedOption() {
        var result = MockRuntimeCli.parse(new String[]{"--unknown-flag"}, out, err);
        assertEquals(2, result.exitCode());
        assertFalse(result.isSuccess());
        assertTrue(stderr().contains("unrecognized argument"));
    }
}
