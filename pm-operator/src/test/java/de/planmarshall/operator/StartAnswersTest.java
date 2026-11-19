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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The answers of a direct starting verb. */
@EnableGeneratorController
@DisplayName("start answers")
class StartAnswersTest {

    @Test
    @DisplayName("an empty answer set has no source, and the builder keeps the answers")
    void buildsAnswers() {
        var description = Generators.nonBlankStrings().next();
        var project = Generators.letterStrings(3, 10).next();
        var recipe = Generators.letterStrings(3, 10).next();
        var paramKey = Generators.letterStrings(3, 8).next();
        var paramValue = Generators.letterStrings(3, 8).next();
        var doc = Generators.letterStrings(3, 8).next() + ".md";

        var answers = new StartAnswers();
        assertTrue(answers.noSources());
        answers.withDescription(description).withProject(project).withRecipe(recipe)
                            .parameter(paramKey, paramValue);
        answers.documents.add(doc);
        assertAll(
                            () -> assertFalse(answers.noSources()),
                            () -> assertEquals(description, answers.description),
                            () -> assertEquals(project, answers.project),
                            () -> assertEquals(recipe, answers.recipeKey),
                            () -> assertEquals(paramValue, answers.parameters.get(paramKey)),
                            () -> assertEquals("-", StartAnswers.STDIN));
    }

    @Test
    @DisplayName("copy and move are one answer, or neither")
    void documentsMode() {
        var answers = new StartAnswers();
        assertNull(answers.documentsMode());
        answers.copy = true;
        assertEquals("copy", answers.documentsMode());
        answers.copy = false;
        answers.move = true;
        assertEquals("move", answers.documentsMode());
        answers.copy = true;
        assertThrows(IllegalArgumentException.class, answers::documentsMode);
    }
}
