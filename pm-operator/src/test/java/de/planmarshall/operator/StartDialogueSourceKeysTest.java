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

import static de.planmarshall.operator.StartDialogueTest.run;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.planmarshall.mock.StandardScenarios;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The keys of the source list against the contract's mock runtime (design <em>Terminal Client:
 * Starting Work</em>, the keys that still apply): each kind's key asks what its source needs and adds
 * it, an empty answer adds nothing, and a kind that no longer combines with the list is not offered.
 */
@EnableGeneratorController
@DisplayName("starting dialogue: the keys of the source list")
class StartDialogueSourceKeysTest {

    private static final String PROJECT = "pm-mcp-clients";

    @ParameterizedTest(name = "{0} asks \"{1}\" and adds the {2}")
    @CsvSource(delimiter = '|', textBlock = """
            i | Number or URL of the issue        | Issue comments API
            l | Lesson                            | Retire lessons when a plan lands
            p | Number or URL of the pull request | Add the Codeberg provider
            """)
    @DisplayName("a key of an outside source asks its reference and adds the source")
    void externalSourceKeyAddsTheSource(String key, String question, String stored) throws Exception {
        var reference = Generators.letterStrings(4, 12).next();
        var rendered = run(StandardScenarios.START_SOURCES, key + "\n" + reference + "\nq\n",
                            "plan", "add", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains(question), rendered),
                            () -> assertTrue(rendered.contains(stored), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @ParameterizedTest(name = "an empty answer to {0} adds no {1}")
    @CsvSource(delimiter = '|', textBlock = """
            i | Issue comments API
            l | Retire lessons when a plan lands
            p | Add the Codeberg provider
            """)
    @DisplayName("an empty answer to the reference adds nothing")
    void emptyReferenceAddsNothing(String key, String stored) throws Exception {
        var rendered = run(StandardScenarios.START_SOURCES, key + "\n\nq\n",
                            "plan", "add", "--project", PROJECT);
        assertAll(
                            () -> assertFalse(rendered.contains(stored), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("c adds the local changes without asking for a reference")
    void localChangesKeyAddsTheChanges() throws Exception {
        var rendered = run(StandardScenarios.START_SOURCES, "c\nq\n",
                            "plan", "add", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains("2 files changed"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("once an issue is added, the local changes and a pull request are no longer offered")
    void issueWithdrawsLocalChangesAndPullRequest() throws Exception {
        var reference = Generators.letterStrings(4, 12).next();
        var rendered = run(StandardScenarios.START_SOURCES, "i\n" + reference + "\nc\np\nq\n",
                            "plan", "add", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains("c is not offered here: "), rendered),
                            () -> assertTrue(rendered.contains("p is not offered here: "), rendered),
                            () -> assertFalse(rendered.contains("2 files changed"), rendered),
                            () -> assertFalse(rendered.contains("Add the Codeberg provider"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }
}
