/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decision;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.message.MessageCatalogue;

@DisplayName("Decision Message Catalogue Tests (UC-PLN-2)")
class DecisionCatalogueTest {

    @Test
    @DisplayName("decisions area contains expected keys in en and de")
    void decisionsArea() {
        var en = MessageCatalogue.getMessages("en", "decisions");
        var de = MessageCatalogue.getMessages("de", "decisions");

        assertFalse(en.isEmpty(), "English decisions messages should not be empty");
        assertFalse(de.isEmpty(), "German decisions messages should not be empty");
        assertEquals(en.keySet(), de.keySet(), "Keys must match identically between en and de");

        // Verify key design anchors
        assertAll(
                () -> assertTrue(en.containsKey("decision.blocks.plan")),
                () -> assertTrue(en.containsKey("decision.reversibility.reversible")),
                () -> assertTrue(en.containsKey("decision.waiting")),
                () -> assertTrue(en.containsKey("decision.support.votes")),
                () -> assertTrue(en.containsKey("decision.support.band.clear_majority")),
                () -> assertTrue(en.containsKey("decision.recommendation.by")),
                () -> assertTrue(en.containsKey("decision.readback.understood")),
                () -> assertTrue(en.containsKey("decision.waiver.title")),
                () -> assertTrue(en.containsKey("decision.waiver.waive")),
                () -> assertTrue(en.containsKey("decision.waiver.decline")),
                () -> assertTrue(en.containsKey("operator.decision.own_words_prompt")),
                () -> assertTrue(en.containsKey("operator.decision.waiver_prompt"))
        );
    }

    @Test
    @DisplayName("parameterized formatting works across English and German for decisions")
    void parameterizedFormatting() {
        var enVotes = MessageCatalogue.format("en", "decision.support.votes", Map.of("votes", 2, "total", 3, "models", 2));
        assertEquals("2 of 3 votes · 2 models", enVotes);

        var deVotes = MessageCatalogue.format("de", "decision.support.votes", Map.of("votes", 2, "total", 3, "models", 2));
        assertEquals("2 von 3 Stimmen · 2 Modelle", deVotes);

        var enRec = MessageCatalogue.format("en", "decision.recommendation.by", Map.of("role", "implement", "option", "Rebase"));
        assertEquals("Recommended by implement: Rebase", enRec);

        var deRec = MessageCatalogue.format("de", "decision.recommendation.by", Map.of("role", "implement", "option", "Rebase"));
        assertEquals("Empfohlen von implement: Rebase", deRec);
    }
}
