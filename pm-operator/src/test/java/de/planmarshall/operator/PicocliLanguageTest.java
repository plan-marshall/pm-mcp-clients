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
import java.util.List;
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

    private static final List<String> FORBIDDEN_ENGLISH_HELP_PHRASES = List.of(
                        "Commands:",
                        "Usage:",
                        "Shows the runtime status; never starts the runtime.",
                        "Shows the overview of running work",
                        "Enables, pairs, or configures",
                        "Starts, stops, restarts, or upgrades",
                        "Signals the runtime to drain",
                        "Manages machine and project",
                        "The operator's credential acts.",
                        "Manages project enrolments",
                        "Explores and lists projects",
                        "Manages client bundle installations",
                        "Inspects or controls plans.",
                        "Inspects or controls epics.",
                        "Recipe operations.",
                        "Verify a lesson's premise",
                        "Lists open decisions and questions.",
                        "Answers an open question or decision.",
                        "Reports operations.",
                        "Shows the system view; never starts the runtime.",
                        "Cleanup operations:",
                        "Enrols the headless CLI",
                        "Reads the effective role set",
                        "Accepts a manual edit",
                        "Shows what needs operator attention",
                        "Goes to work by name:"
    );

    @Test
    @DisplayName("pm-operator -h in German prints German headings and descriptions with no English words outside glossary")
    void helpInGerman() {
        int code = runWithEnv(Map.of("LC_ALL", "de_DE.UTF-8"), "-h");
        assertEquals(0, code);
        String help = out.toString();

        assertTrue(help.contains("Befehle:"), "Expected German heading 'Befehle:' in: " + help);
        assertTrue(help.contains("Zeigt den Runtime-Status; startet die Runtime nie."), help);
        assertContainsNone(help, FORBIDDEN_ENGLISH_HELP_PHRASES);
    }

    private static void assertContainsNone(String text, List<String> forbidden) {
        for (var item : forbidden) {
            assertFalse(text.contains(item), "Should not contain English '" + item + "' in: " + text);
        }
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
