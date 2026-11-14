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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The starting dialogue against the contract's mock runtime, for the inputs its states do not offer
 * (design <em>Dialogues</em>, links as keys): each is answered with one line and the same prompt,
 * nothing is sent, and {@code q} leaves.
 */
@EnableGeneratorController
@DisplayName("starting dialogue: an input a state does not offer is answered with one line, and nothing is sent")
class StartDialogueNotOfferedTest {

    private static final String PROJECT = "pm-mcp-clients";

    @Test
    @DisplayName("the source list answers an unknown key, and x without a source, with the keys it offers")
    void sourceListKeys() throws Exception {
        var input = Generators.letterStrings(8, 12).next();
        var rendered = run(StandardScenarios.START_SOURCES, input + "\nx\n?\nq\n",
                            "plan", "add", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains(input + " is not offered here: d, f"), rendered),
                            () -> assertTrue(rendered.contains("x is not offered here: d, f"), rendered),
                            () -> assertTrue(rendered.contains("q, ?"), rendered),
                            () -> assertTrue(rendered.contains("Type a key of the list"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("Enter on a source list that cannot continue yet says so instead of drawing the list again silently")
    void enterWithoutASource() throws Exception {
        var rendered = run(StandardScenarios.START_SOURCES, "\nq\n",
                            "plan", "add", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains("Enter does nothing here; type one of the keys."), rendered),
                            () -> assertFalse(rendered.contains("has started"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("copy or move takes only 1, 2, a and Enter: the wire word is not a key and q copies nothing")
    void documentsMode() throws Exception {
        var rendered = run(StandardScenarios.START_SOURCES, "f\ndoc.md\nmove\n1\nq\n",
                            "plan", "add", "--project", PROJECT);
        var left = run(StandardScenarios.START_SOURCES, "f\ndoc.md\nq\n",
                            "plan", "add", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains("move is not offered here: 1-2, a, q"), rendered),
                            () -> assertTrue(rendered.contains("copied"), rendered),
                            () -> assertFalse(rendered.contains("moved"), rendered),
                            () -> assertFalse(left.contains("copied"), left),
                            () -> assertTrue(left.contains("Left the start"), left));
    }

    @Test
    @DisplayName("a failed source is not used anyway and a flagged one not accepted as unscreened")
    void pendingSourceKeys() throws Exception {
        var rendered = run(StandardScenarios.START_ISSUE_FAILED, "u\nr\na\nq\n",
                            "plan", "add", "--project", PROJECT, "--entry", "ent-3f9a2c41");
        assertAll(
                            () -> assertTrue(rendered.contains("u is not offered here: t, r, q"), rendered),
                            () -> assertTrue(rendered.contains("a is not offered here: u, r, q"), rendered),
                            () -> assertFalse(rendered.contains("Reason"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("a number beyond the sources removes nothing and asks again")
    void removeBeyondTheList() throws Exception {
        var number = Generators.integers(2, 99).next();
        var rendered = run(StandardScenarios.START_SOURCES,
                            "d\nA change\n\nx\n" + number + "\n1\nq\n",
                            "plan", "add", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains(number + " is not offered here: 1, q"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("the ready step answers an unknown key and leaves on q without starting")
    void readyKeys() throws Exception {
        var input = Generators.letterStrings(8, 12).next();
        var rendered = run(StandardScenarios.START_SOURCES, "d\nA change\n\n\n" + input + "\nq\n",
                            "plan", "add", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains(input + " is not offered here: e, q, ?"), rendered),
                            () -> assertFalse(rendered.contains("has started"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("a number beyond the recipes chooses nothing")
    void recipeBeyondTheList() throws Exception {
        var number = Generators.integers(6, 99).next();
        var rendered = run(StandardScenarios.START_SOURCES, number + "\nq\n",
                            "recipe", "run", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains(number + " is not offered here: 1-5, q"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("q at a recipe's parameters leaves without sending them, and an unknown answer sends nothing")
    void recipeParameterKeys() throws Exception {
        var input = Generators.letterStrings(8, 12).next();
        var rendered = run(StandardScenarios.START_SOURCES, "2\n" + input + "\nq\n",
                            "recipe", "run", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains(input + " is not offered here: 1"), rendered),
                            () -> assertTrue(rendered.contains("b, q, ?"), rendered),
                            () -> assertFalse(rendered.contains("e) edit"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("a recipe that cannot run offers only b and q")
    void blockedRecipeKeys() throws Exception {
        var input = Generators.letterStrings(8, 12).next();
        var rendered = run(StandardScenarios.START_SOURCES, input + "\nq\n",
                            "recipe", "run", "no-modules", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains(input + " is not offered here: b, q"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("a number beyond the adoptable epics adopts nothing")
    void adoptionBeyondTheList() throws Exception {
        var number = Generators.integers(3, 99).next();
        var rendered = run(StandardScenarios.START_EPIC_ADOPT, number + "\nq\n",
                            "epic", "add", "--project", PROJECT);
        assertAll(
                            () -> assertTrue(rendered.contains(number + " is not offered here: 1-2, n, q, ?"),
                                                rendered),
                            () -> assertFalse(rendered.contains("adopted from the ledger"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }

    @Test
    @DisplayName("a failed adoption answers an unknown key and leaves on q")
    void adoptionFailedKeys() throws Exception {
        var input = Generators.letterStrings(8, 12).next();
        var rendered = run(StandardScenarios.START_EPIC_ADOPT, input + "\nq\n",
                            "epic", "add", "--project", PROJECT, "--entry", "ent-7a2e4f01",
                            "--adopt", "storage-migration");
        assertAll(
                            () -> assertTrue(rendered.contains(input + " is not offered here: t, b, x, q"), rendered),
                            () -> assertFalse(rendered.contains("Abandoned"), rendered),
                            () -> assertTrue(rendered.contains("Left the start"), rendered));
    }
}
