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

@EnableGeneratorController
class ConsoleBadgeTest {

    @Test
    @DisplayName("machineSetup creates valid MACHINE_SETUP badge")
    void machineSetupBadge() {
        var text = Generators.nonEmptyStrings().next();
        var badge = ConsoleBadge.machineSetup(text);
        assertAll(
                            () -> assertEquals(ConsoleBadge.Kind.MACHINE_SETUP, badge.kind()),
                            () -> assertEquals(text, badge.text()),
                            () -> assertNull(badge.itemKind()),
                            () -> assertEquals(0, badge.count()),
                            () -> assertNull(badge.projectId()),
                            () -> assertFalse(badge.isQuestion())
        );
    }

    @Test
    @DisplayName("projectSetup creates valid PROJECT_SETUP badge")
    void projectSetupBadge() {
        var projectId = Generators.nonEmptyStrings().next();
        var text = Generators.nonEmptyStrings().next();
        var badge = ConsoleBadge.projectSetup(projectId, text);
        assertAll(
                            () -> assertEquals(ConsoleBadge.Kind.PROJECT_SETUP, badge.kind()),
                            () -> assertEquals(text, badge.text()),
                            () -> assertEquals(projectId, badge.projectId()),
                            () -> assertNull(badge.itemKind()),
                            () -> assertEquals(0, badge.count()),
                            () -> assertFalse(badge.isQuestion())
        );
    }

    @Test
    @DisplayName("waiting creates valid WAITING badge and identifies question kinds")
    void waitingBadge() {
        var count = Generators.integers(1, 9).next();
        var qBadge = ConsoleBadge.waiting("question", count, count + " questions");
        var dBadge = ConsoleBadge.waiting("decision", 1, "1 decision");
        var wBadge = ConsoleBadge.waiting("waiver_request", 2, "2 waiver requests");
        var oBadge = ConsoleBadge.waiting("integrity_pause", 1, "1 integrity pause");

        assertAll(
                            () -> assertEquals(ConsoleBadge.Kind.WAITING, qBadge.kind()),
                            () -> assertEquals(count + " questions", qBadge.text()),
                            () -> assertEquals("question", qBadge.itemKind()),
                            () -> assertEquals(count, qBadge.count()),
                            () -> assertNull(qBadge.projectId()),
                            () -> assertTrue(qBadge.isQuestion()),
                            () -> assertTrue(dBadge.isQuestion()),
                            () -> assertTrue(wBadge.isQuestion()),
                            () -> assertFalse(oBadge.isQuestion())
        );
    }

    @Test
    @DisplayName("enforces non-null kind and text")
    void nullArgumentsThrow() {
        var text = Generators.nonEmptyStrings().next();
        assertAll(
                            () -> assertThrows(NullPointerException.class, () -> new ConsoleBadge(null, text, null, 0, null)),
                            () -> assertThrows(NullPointerException.class, () -> new ConsoleBadge(ConsoleBadge.Kind.MACHINE_SETUP, null, null, 0, null))
        );
    }
}
