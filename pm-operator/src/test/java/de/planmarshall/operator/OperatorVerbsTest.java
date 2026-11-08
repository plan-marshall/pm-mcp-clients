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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OperatorVerbs")
class OperatorVerbsTest {

    @Test
    @DisplayName("the completer carries the registered verbs and the console's own commands, each once")
    void completerCoversRegisteredAndConsoleCommands() {
        var completer = OperatorVerbs.completer();

        assertTrue(completer.containsAll(OperatorVerbs.registered()), completer.toString());
        assertTrue(completer.containsAll(List.of("exit", "quit", "help", "tui")), completer.toString());
        assertEquals(Set.copyOf(completer).size(), completer.size(), "a word appears twice: " + completer);
    }

    @Test
    @DisplayName("the help sentence names every registered verb, in both languages")
    void helpMoreNamesEveryVerb() {
        for (var locale : List.of("en", "de")) {
            var words = words(OperatorVerbs.helpMore(locale));

            for (var verb : OperatorVerbs.registered()) {
                assertTrue(words.contains(verb), locale + " omits " + verb + ": " + words);
            }
        }
    }

    @Test
    @DisplayName("the help sentence carries no literal placeholder")
    void helpMoreCarriesNoPlaceholder() {
        assertFalse(OperatorVerbs.helpMore("en").contains("{verbs}"), OperatorVerbs.helpMore("en"));
    }

    private static List<String> words(String text) {
        return Arrays.stream(text.split("[^a-z-]+"))
                            .filter(word -> !word.isBlank())
                            .toList();
    }
}
