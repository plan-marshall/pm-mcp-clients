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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Tests for Picocli in the operator's language and custom exception handling (D3).
 */
@DisplayName("Picocli in the operator's language (D3)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PicocliLanguageTest {

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        System.setProperty("pm.test.no.browser", "true");
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        System.clearProperty("pm.test.no.browser");
        fixture.close();
    }

    private int runWithEnv(Map<String, String> env, String... args) {
        var merged = new HashMap<>(fixture.environment());
        merged.putAll(env);
        var clientEnv = ClientEnvironment.capture(merged);
        var context = new OperatorContext(
                            merged,
                            fixture.base().getParent(),
                            true,
                            clientEnv,
                            null,
                            _ -> {
                            }
        );
        return PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), args);
    }

    @Test
    @DisplayName("bogus in German prints localized refusal with exit code 2 and no English words outside glossary")
    void bogusInGerman() {
        int code = runWithEnv(Map.of("LC_ALL", "de_DE.UTF-8"), "bogus");
        assertEquals(2, code);
        String errorOutput = err.toString();
        assertTrue(errorOutput.contains("✗ bogus ist kein Verb. → pm-operator --help listet sie auf."), "Expected refusal line in: " + errorOutput);
        assertFalse(errorOutput.contains("Unmatched argument"), errorOutput);
        assertFalse(errorOutput.contains("Did you mean"), errorOutput);
    }

    @Test
    @DisplayName("bogus in English prints localized refusal with exit code 2")
    void bogusInEnglish() {
        int code = runWithEnv(Map.of("LC_ALL", "en_US.UTF-8"), "bogus");
        assertEquals(2, code);
        String errorOutput = err.toString();
        assertTrue(errorOutput.contains("✗ bogus is not a verb. → pm-operator --help lists them."), "Expected refusal line in: " + errorOutput);
        assertFalse(errorOutput.contains("Unmatched argument"), errorOutput);
        assertFalse(errorOutput.contains("Did you mean"), errorOutput);
    }

    @Test
    @DisplayName("pm-operator -h in German prints German headings and descriptions with no English words outside glossary")
    void helpInGerman() {
        int code = runWithEnv(Map.of("LC_ALL", "de_DE.UTF-8"), "-h");
        assertEquals(0, code);
        String help = out.toString();

        assertTrue(help.contains("Befehle:"), "Expected German heading 'Befehle:' in: " + help);
        assertFalse(help.contains("Commands:"), "Should not contain English 'Commands:' in: " + help);
        assertFalse(help.contains("Usage:"), "Should not contain English 'Usage:' in: " + help);

        assertTrue(help.contains("Zeigt den Runtime-Status; startet die Runtime nie."), help);
        assertFalse(help.contains("Shows the runtime status; never starts the runtime."), help);
        assertFalse(help.contains("Shows the overview of running work"), help);
        assertFalse(help.contains("Enables, pairs, or configures"), help);
        assertFalse(help.contains("Starts, stops, restarts, or upgrades"), help);
        assertFalse(help.contains("Signals the runtime to drain"), help);
        assertFalse(help.contains("Manages machine and project"), help);
        assertFalse(help.contains("The operator's credential acts."), help);
        assertFalse(help.contains("Manages project enrolments"), help);
        assertFalse(help.contains("Explores and lists projects"), help);
        assertFalse(help.contains("Manages client bundle installations"), help);
        assertFalse(help.contains("Inspects or controls plans."), help);
        assertFalse(help.contains("Inspects or controls epics."), help);
        assertFalse(help.contains("Recipe operations."), help);
        assertFalse(help.contains("Verify a lesson's premise"), help);
        assertFalse(help.contains("Lists open decisions and questions."), help);
        assertFalse(help.contains("Answers an open question or decision."), help);
        assertFalse(help.contains("Reports operations."), help);
        assertFalse(help.contains("Shows the system view; never starts the runtime."), help);
        assertFalse(help.contains("Cleanup operations:"), help);
        assertFalse(help.contains("Enrols the headless CLI"), help);
        assertFalse(help.contains("Reads the effective role set"), help);
        assertFalse(help.contains("Accepts a manual edit"), help);
        assertFalse(help.contains("Shows what needs operator attention"), help);
        assertFalse(help.contains("Goes to work by name:"), help);
    }

    @Test
    @DisplayName("pm-operator -h in English prints English headings and descriptions")
    void helpInEnglish() {
        int code = runWithEnv(Map.of("LC_ALL", "en_US.UTF-8"), "-h");
        assertEquals(0, code);
        String help = out.toString();

        assertTrue(help.contains("Commands:"), help);
        assertTrue(help.contains("Shows the runtime status; never starts the runtime."), help);
    }
}
