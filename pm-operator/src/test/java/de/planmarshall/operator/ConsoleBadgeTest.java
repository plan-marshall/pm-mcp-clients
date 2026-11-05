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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConsoleBadgeTest {

    @Test
    @DisplayName("machineSetup creates valid MACHINE_SETUP badge")
    void machineSetupBadge() {
        var badge = ConsoleBadge.machineSetup("setup 1/7");
        assertAll(
                            () -> assertEquals(ConsoleBadge.Kind.MACHINE_SETUP, badge.kind()),
                            () -> assertEquals("setup 1/7", badge.text()),
                            () -> assertNull(badge.itemKind()),
                            () -> assertEquals(0, badge.count()),
                            () -> assertNull(badge.projectId()),
                            () -> assertFalse(badge.isQuestion())
        );
    }

    @Test
    @DisplayName("projectSetup creates valid PROJECT_SETUP badge")
    void projectSetupBadge() {
        var badge = ConsoleBadge.projectSetup("pm-mcp-clients", "setup pm-mcp-clients 2/10");
        assertAll(
                            () -> assertEquals(ConsoleBadge.Kind.PROJECT_SETUP, badge.kind()),
                            () -> assertEquals("setup pm-mcp-clients 2/10", badge.text()),
                            () -> assertEquals("pm-mcp-clients", badge.projectId()),
                            () -> assertNull(badge.itemKind()),
                            () -> assertEquals(0, badge.count()),
                            () -> assertFalse(badge.isQuestion())
        );
    }

    @Test
    @DisplayName("waiting creates valid WAITING badge and identifies question kinds")
    void waitingBadge() {
        var qBadge = ConsoleBadge.waiting("question", 3, "3 questions");
        var dBadge = ConsoleBadge.waiting("decision", 1, "1 decision");
        var wBadge = ConsoleBadge.waiting("waiver_request", 2, "2 waiver requests");
        var oBadge = ConsoleBadge.waiting("integrity_pause", 1, "1 integrity pause");

        assertAll(
                            () -> assertEquals(ConsoleBadge.Kind.WAITING, qBadge.kind()),
                            () -> assertEquals("3 questions", qBadge.text()),
                            () -> assertEquals("question", qBadge.itemKind()),
                            () -> assertEquals(3, qBadge.count()),
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
        assertAll(
                            () -> assertThrows(NullPointerException.class, () -> new ConsoleBadge(null, "text", null, 0, null)),
                            () -> assertThrows(NullPointerException.class, () -> new ConsoleBadge(ConsoleBadge.Kind.MACHINE_SETUP, null, null, 0, null))
        );
    }
}
