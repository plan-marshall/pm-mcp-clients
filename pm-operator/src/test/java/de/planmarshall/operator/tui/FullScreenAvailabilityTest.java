/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.tui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import de.planmarshall.api.message.MessageCatalogue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The availability verdict: one reason per refusal, each with its catalogue line in both
 * languages, and no reason on an available verdict.
 */
@DisplayName("FullScreenAvailability")
class FullScreenAvailabilityTest {

    @Test
    @DisplayName("an available verdict carries no reason and no refusal text")
    void availableCarriesNoReason() {
        var verdict = FullScreenAvailability.open();

        assertAll(
                            () -> assertTrue(verdict.available()),
                            () -> assertNull(verdict.reason()),
                            () -> assertThrows(IllegalStateException.class, verdict::messageKey)
        );
    }

    @Test
    @DisplayName("every refusal renders its own catalogue line in English and German")
    void everyRefusalRenders() {
        for (var reason : FullScreenAvailability.Reason.values()) {
            var verdict = FullScreenAvailability.refused(reason);
            var en = MessageCatalogue.format("en", verdict.messageKey(), Map.of());
            var de = MessageCatalogue.format("de", verdict.messageKey(), Map.of());

            assertAll(
                                () -> assertFalse(verdict.available()),
                                () -> assertEquals(reason, verdict.reason()),
                                () -> assertNotEquals(verdict.messageKey(), en),
                                () -> assertNotEquals(verdict.messageKey(), de),
                                () -> assertTrue(de.contains("Terminal") || de.contains("Vollbild"),
                                                    verdict.messageKey() + ": " + de)
            );
        }
    }

    @Test
    @DisplayName("an inconsistent verdict is refused")
    void refusesInconsistentVerdicts() {
        assertAll(
                            () -> assertThrows(IllegalArgumentException.class,
                                                () -> new FullScreenAvailability(true,
                                                                    FullScreenAvailability.Reason.TOO_NARROW)),
                            () -> assertThrows(IllegalArgumentException.class,
                                                () -> new FullScreenAvailability(false, null))
        );
    }

}
